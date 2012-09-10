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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javassist.util.proxy.ProxyFactory;

import org.hibernate.Session;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
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

	public static boolean entitiesEqual(SessionImplementor session, String entityName, Object obj1, Object obj2) {
        Object id1 = getIdentifier(session, entityName, obj1);
		Object id2 = getIdentifier(session, entityName, obj2);

        return objectsEqual(id1, id2);
    }	

	public static Object getIdentifier(SessionImplementor session, String entityName, Object obj) {
		if (obj == null) {
			return null;
		}

		if (obj instanceof HibernateProxy) {
			HibernateProxy hibernateProxy = (HibernateProxy) obj;
			return hibernateProxy.getHibernateLazyInitializer().getIdentifier();
		}

		return session.getEntityPersister(entityName, obj).getIdentifier(obj, session);
	}	    


    public static Object getTargetFromProxy(SessionFactoryImplementor sessionFactoryImplementor, HibernateProxy proxy) {
        if (!proxy.getHibernateLazyInitializer().isUninitialized() || activeProxySession(proxy)) {
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
			return target;
        }
		finally {
            tempSession.close();
        }
    }

    private static boolean activeProxySession(HibernateProxy proxy) {
        Session session = (Session) proxy.getHibernateLazyInitializer().getSession();
        return session != null && session.isOpen() && session.isConnected();
    }

    /**
     * @param clazz Class wrapped with a proxy or not.
     * @param <T> Class type.
     * @return Returns target class in case it has been wrapped with a proxy. If {@code null} reference is passed,
     *         method returns {@code null}. 
     */
    @SuppressWarnings({"unchecked"})
    public static <T> Class<T> getTargetClassIfProxied(Class<T> clazz) {
        if (clazz == null) {
            return null;
        } else if (ProxyFactory.isProxyClass(clazz)) {
            // Get the source class of Javassist proxy instance.
            return (Class<T>) clazz.getSuperclass();
        }
        return clazz;
    }

    public static boolean objectsEqual(Object obj1, Object obj2) {
        if (obj1 == null) {
            return obj2 == null;
        }

        return obj1.equals(obj2);
    }

    public static boolean arraysEqual(Object[] array1, Object[] array2) {
        if (array1 == null) return array2 == null;
        if (array2 == null || array1.length != array2.length) return false;
        for (int i = 0; i < array1.length; ++i) {
            if (array1[i] != null ? !array1[i].equals(array2[i]) : array2[i] != null) {
                return false;
            }
        }
        return true;
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

    /**
     * @return Java class mapped to specified entity name.
     */
    public static Class getEntityClass(SessionImplementor sessionImplementor, Session session, String entityName) {
        EntityPersister entityPersister = sessionImplementor.getFactory().getEntityPersister(entityName);
        return entityPersister.getMappedClass();
    }

    /**
     * Converts map's value set to an array. {@code keys} parameter specifies requested elements and their order. 
     * @param data Source map.
     * @param keys Array of keys that represent requested map values.
     * @return Array of values stored in the map under specified keys. If map does not contain requested key,
     *         {@code null} is inserted.
     */
    public static Object[] mapToArray(Map<String, Object> data, String[] keys) {
        Object[] ret = new Object[keys.length];
        for (int i = 0; i < keys.length; ++i) {
            ret[i] = data.get(keys[i]);
        }
        return ret;
    }

    /**
     * @param clazz Source class.
     * @param propertyName Property name.
     * @return Property object or {@code null} if none with expected name has been found.
     */
    public static XProperty getProperty(XClass clazz, String propertyName) {
        XProperty property = getProperty(clazz, propertyName, "field");
        if (property == null) {
            property = getProperty(clazz, propertyName, "property");
        }
        return property;
    }

    /**
     * @param clazz Source class.
     * @param propertyName Property name.
     * @param accessType Expected access type. Legal values are <i>field</i> and <i>property</i>.
     * @return Property object or {@code null} if none with expected name and access type has been found.
     */
    public static XProperty getProperty(XClass clazz, String propertyName, String accessType) {
        for (XProperty property : clazz.getDeclaredProperties(accessType)) {
            if (propertyName.equals(property.getName())) {
                return property;
            }
        }
        return null;
    }
}
