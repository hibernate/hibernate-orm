/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.tools;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.Session;

import java.util.*;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Tools {
    public static <K,V> Map<K,V> newHashMap() {
        return new HashMap<K,V>();
    }

    public static <E> Set<E> newHashSet() {
        return new HashSet<E>();
    }

    public static <K,V> Map<K,V> newLinkedHashMap() {
        return new LinkedHashMap<K,V>();
    }

	public static boolean entitiesEqual(SessionImplementor session, Object obj1, Object obj2) {
        Object id1 = getIdentifier(session, obj1);
		Object id2 = getIdentifier(session, obj2);

        return objectsEqual(id1, id2);
    }

	public static Object getIdentifier(SessionImplementor session, Object obj) {
		if (obj == null) {
			return null;
		}

		if (obj instanceof HibernateProxy) {
			HibernateProxy hibernateProxy = (HibernateProxy) obj;
			return hibernateProxy.getHibernateLazyInitializer().getIdentifier();
		}


		return session.getEntityPersister( null, obj ).getIdentifier( obj, session );
	}

    public static Object getTargetFromProxy(SessionFactoryImplementor sessionFactoryImplementor, HibernateProxy proxy) {
        if (!proxy.getHibernateLazyInitializer().isUninitialized()) {
            return proxy.getHibernateLazyInitializer().getImplementation();
        }

        SessionImplementor sessionImplementor = proxy.getHibernateLazyInitializer().getSession();
        Session tempSession = sessionImplementor==null
				? sessionFactoryImplementor.openTemporarySession()
				: sessionImplementor.getFactory().openTemporarySession();
        try {
			Object target = tempSession.get(
					proxy.getHibernateLazyInitializer().getEntityName(),
					proxy.getHibernateLazyInitializer().getIdentifier()
			);
			proxy.getHibernateLazyInitializer().setImplementation( target );
			return target;
        }
		finally {
            tempSession.close();
        }
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

        //noinspection RedundantIfStatement
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

    /**
     * @param properties Properties from which to read.
     * @param propertyName The name of the property.
     * @param legacyPropertyName Legacy name of the property. The value of this property is read if value for
     * {@code propertyName} is not set.
     * @param defaultValue Default value returned if a value neither for {@code propertyName} or
     * {@code legacyPropertyName} is set.
     * @return The value of the property, legacy proparty or the default value, if neither of the values are not set.
     */
    public static String getProperty(Properties properties, String propertyName, String legacyPropertyName, String defaultValue) {
        String value = properties.getProperty(propertyName, null);
        if (value == null) {
            return properties.getProperty(legacyPropertyName, defaultValue);
        } else {
            return value;
        }
    }
}
