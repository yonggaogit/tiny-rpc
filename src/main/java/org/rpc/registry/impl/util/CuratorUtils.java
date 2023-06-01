package org.rpc.registry.impl.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.rpc.enums.RpcConfigEnum;
import org.rpc.utils.PropertiesFileUtil;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class CuratorUtils {

    private static final int BASE_SLEEP_TIME = 1000;
    private static final int MAX_RETRIES = 3;

    private static CuratorFramework zkClient;
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";

    public static final String ZOOKEEPER_REGISTER_ROOT_PATH = "/my_rpc";

    private static final Set<String> REGISTED_PATH_SET = new HashSet<>();
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private CuratorUtils() {}

    public static void createPersistentNode( CuratorFramework zkClient, String path ) {
        try {
            if ( REGISTED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("[{}] path have been exists", path);
            } else {
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("[{}] create successful", path);
            }
            REGISTED_PATH_SET.add( path );
        } catch ( Exception e ) {
            log.error("[{}] create fail", path);
        }
    }

    public static List<String> getChildrenNodes( CuratorFramework zkClient, String rpcServiceName ) {
        if ( SERVICE_ADDRESS_MAP.containsKey(rpcServiceName) ) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }
        String servicePath = ZOOKEEPER_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        List<String> result  = null;
        try {
            result = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        registerWatcher(rpcServiceName, zkClient);
        return result;
    }

    public static CuratorFramework getZkClient() {
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        String zookeeperAddress = properties != null && properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null ? properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) : DEFAULT_ZOOKEEPER_ADDRESS;
        if ( zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED ) {
            return zkClient;
        }

        RetryPolicy retryPolicy = new ExponentialBackoffRetry( BASE_SLEEP_TIME, MAX_RETRIES );
        zkClient = CuratorFrameworkFactory.builder().connectString( zookeeperAddress ).retryPolicy(retryPolicy).build();

        zkClient.start();
        try {
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("connect to zookeeper fail");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return zkClient;
    }

    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        for (String path : REGISTED_PATH_SET) {
            if (path.endsWith(inetSocketAddress.toString())) {
                try {
                    zkClient.delete().forPath(path);
                } catch (Exception e) {
                    log.error("clear registry for path [{}] fail", path);
                }
            }
        }
        log.info( "delete all path success:[{}]" );
    }

    public static void registerWatcher( String rpcServiceName, CuratorFramework zkClient ) {
        String servicePath = ZOOKEEPER_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        PathChildrenCache pathChildrenCache = new PathChildrenCache( zkClient, servicePath, true );
        PathChildrenCacheListener pathChildrenCacheListener = ( curatorFramework, pathChildrenCacheEvent ) -> {
            List<String> serviceAddress = curatorFramework.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(servicePath, serviceAddress);
        };
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        try {
            pathChildrenCache.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
