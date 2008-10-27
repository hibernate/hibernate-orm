package org.jboss.envers.test.tools;

import java.util.*;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class TestTools {
    public static <T> Set<T> makeSet(T... objects) {
        Set<T> ret = new HashSet<T>();
        //noinspection ManualArrayToCollectionCopy
        for (T o : objects) {
            ret.add(o);
        }

        return ret;
    }

    public static <T> List<T> makeList(T... objects) {
        return Arrays.asList(objects);
    }

    public static Map<Object, Object> makeMap(Object... objects) {
        Map<Object, Object> ret = new HashMap<Object, Object>();
        // The number of objects must be divisable by 2.
        //noinspection ManualArrayToCollectionCopy
        for (int i=0; i<objects.length; i+=2) {
            ret.put(objects[i], objects[i+1]);
        }

        return ret;
    }

    public static <T> boolean checkList(List<T> list, T... objects) {
        if (list.size() != objects.length) {
            return false;
        }

        for (T obj : objects) {
            if (!list.contains(obj)) {
                return false;
            }
        }

        return true;
    }
}
