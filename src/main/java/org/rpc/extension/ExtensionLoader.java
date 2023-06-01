package org.rpc.extension;

import lombok.extern.slf4j.Slf4j;
import org.rpc.utils.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class ExtensionLoader<T> {
    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();
    private final Class<?> type;

    private ExtensionLoader( Class<?> type ) {
        this.type = type;
    }

    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type) {
        if ( type == null ) {
            throw new IllegalArgumentException("Extension type should not be null");
        }
        if ( ! type.isInterface() ) {
            throw new IllegalArgumentException("Extension type must be an interface");
        }
        if ( type.getAnnotation(SPI.class) == null ) {
            throw new IllegalArgumentException("Extension type must be annotated by @SPI");
        }

        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        if ( extensionLoader == null ) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<S>(type));
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }

        return extensionLoader;
    }

    public T getExtension( String name ) {
        if (StringUtil.isBlank( name )) {
            throw new IllegalArgumentException("Extension name should not be null or empty.");
        }

        Holder<Object> holder = cachedInstances.get(name);
        if ( holder == null ) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }

        Object instance = holder.get();
        if ( instance == null ) {
            synchronized ( holder ) {
                instance = holder.get();
                if ( instance == null ) {
                    instance = createExtension ( name );
                    holder.set( instance );
                }
            }
        }

        return (T) instance;
    }

    private T createExtension( String name ) {
        Class<?> aClass = getExtensionClasses().get(name);
        if ( aClass == null ) {
            throw new RuntimeException("No such extension of name " + name);
        }

        T instance = (T) EXTENSION_INSTANCES.get( aClass );
        if ( instance == null ) {
            try {
                EXTENSION_INSTANCES.putIfAbsent(aClass, aClass.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(aClass);
            } catch ( Exception e ) {
                log.error(e.getMessage());
            }
        }
        return instance;
    }

    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if ( classes == null ) {
            synchronized ( cachedClasses ) {
                classes = cachedClasses.get();
                if ( classes == null ) {
                    classes = new HashMap<>();
                    loadDirectory( classes );
                    cachedClasses.set( classes );
                }
            }
        }
        return classes;
    }

    private void loadDirectory( Map<String, Class<?>> extensionClasses ) {
        String fileName = ExtensionLoader.SERVICE_DIRECTORY + type.getName();
        ClassLoader classLoader = ExtensionLoader.class.getClassLoader();

        Enumeration<URL> urls;
        try {
            urls = classLoader.getResources(fileName);
            if ( urls != null ) {
                while (urls.hasMoreElements() ) {
                    URL resourceUrl = urls.nextElement();
                    loadResource( extensionClasses, classLoader, resourceUrl );
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void loadResource( Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL resourceUrl ) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), UTF_8))) {
            String line;
            // read every line
            while ((line = reader.readLine()) != null) {
                // get index of comment
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                    // string after # is comment so we ignore it
                    line = line.substring(0, ci);
                }
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        final int ei = line.indexOf('=');
                        String name = line.substring(0, ei).trim();
                        String clazzName = line.substring(ei + 1).trim();
                        // our SPI use key-value pair so both of them must not be empty
                        if (name.length() > 0 && clazzName.length() > 0) {
                            Class<?> clazz = classLoader.loadClass(clazzName);
                            extensionClasses.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage());
                    }
                }

            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
