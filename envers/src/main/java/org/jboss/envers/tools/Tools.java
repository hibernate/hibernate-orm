/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.tools;

import java.util.*;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Tools {
    public static <K,V> Map<K,V> newHashMap() {
        return new HashMap<K,V>();
    }

    public static boolean objectsEqual(Object obj1, Object obj2) {
        if (obj1 == null) {
            return obj2 == null;
        }

        return obj1.equals(obj2);
    }

    public static <T> List<T> iteratorToList(Iterator<T> iter) {
        List<T> ret = new ArrayList<T>();
        while (iter.hasNext()) {
            ret.add(iter.next());
        }

        return ret;
    }

    public static boolean iteratorsContentEqual(Iterator iter1, Iterator iter2) {
        while (iter1.hasNext() && iter2.hasNext()) {
            if (!iter1.next().equals(iter2.next())) {
                return false;
            }
        }

        if (iter1.hasNext() || iter2.hasNext()) {
            return false;
        }

        return true;
    }

    /**
     * Transforms a list of arbitrary elements to a list of index-element pairs. 
     * @param list List to transform.
     * @return A list of pairs: ((0, element_at_index_0), (1, element_at_index_1), ...)
     */
    public static <T> List<Pair<Integer, T>> listToIndexElementPairList(List<T> list) {
        List<Pair<Integer, T>> ret = new ArrayList<Pair<Integer, T>>();
        Iterator<T> listIter = list.iterator();
        for (int i=0; i<list.size(); i++) {
            ret.add(Pair.make(i, listIter.next()));
        }

        return ret;
    }
}
