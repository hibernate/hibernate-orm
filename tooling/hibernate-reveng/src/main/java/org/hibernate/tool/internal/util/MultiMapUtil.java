package org.hibernate.tool.internal.util;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections.MultiMap;

public class MultiMapUtil {

    /**
     * Copies all the values from one MultiMap to another.
     * This method is needed because the (undocumented) behaviour of 
     * MultiHashMap.putAll in versions of Commons Collections prior to 3.0
     * was to replace the collection in the destination, whereas in 3.0
     * it adds the collection from the source as an _element_ of the collection
     * in the destination.  This method makes no assumptions about the implementation
     * of the MultiMap, and should work with all versions.
     * 
     * @param destination
     * @param specific
     */
    public static void copyMultiMap(MultiMap destination, MultiMap specific) {
        for (Iterator<?> keyIterator = specific.keySet().iterator(); keyIterator.hasNext(); ) {
            Object key = keyIterator.next();
            Collection<?> c = (Collection<?>)specific.get(key);
            for (Iterator<?> valueIterator = c.iterator(); valueIterator.hasNext(); ) 
                destination.put(key, valueIterator.next() );
        }
    }

}
