package org.rpc.utils;

import java.util.Collection;

public final class CollectionUtil {
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}
