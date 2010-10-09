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
package org.hibernate.envers.tools.reflection;

import java.util.Map;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.tools.ConcurrentReferenceHashMap;
import org.hibernate.envers.tools.Pair;
import static org.hibernate.envers.tools.Pair.make;
import org.hibernate.envers.entities.PropertyData;

import org.hibernate.property.*;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ReflectionTools {
    private static final Map<Pair<Class, String>, Getter> getterCache =
            new ConcurrentReferenceHashMap<Pair<Class, String>, Getter>(10,
                ConcurrentReferenceHashMap.ReferenceType.SOFT,
                ConcurrentReferenceHashMap.ReferenceType.SOFT);
    private static final Map<Pair<Class, String>, Setter> setterCache =
            new ConcurrentReferenceHashMap<Pair<Class, String>, Setter>(10,
                ConcurrentReferenceHashMap.ReferenceType.SOFT,
                ConcurrentReferenceHashMap.ReferenceType.SOFT);

    public static Class<?> loadClass(String name) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new AuditException(e);
        }
    }

    private static PropertyAccessor getAccessor(String accessorType) {
        return PropertyAccessorFactory.getPropertyAccessor(accessorType);
    }

    public static Getter getGetter(Class cls, PropertyData propertyData) {
        return getGetter(cls, propertyData.getBeanName(), propertyData.getAccessType());
    }

    public static Getter getGetter(Class cls, String propertyName, String accessorType) {
        Pair<Class, String> key = make(cls, propertyName);
        Getter value = getterCache.get(key);
        if (value == null) {
            value = getAccessor(accessorType).getGetter(cls, propertyName);
            // It's ok if two getters are generated concurrently
            getterCache.put(key, value);
        }

        return value;
    }

    public static Setter getSetter(Class cls, PropertyData propertyData) {
        return getSetter(cls, propertyData.getBeanName(), propertyData.getAccessType());
    }

    private static Setter getSetter(Class cls, String propertyName, String accessorType) {
        Pair<Class, String> key = make(cls, propertyName);
        Setter value = setterCache.get(key);
        if (value == null) {
            value = getAccessor(accessorType).getSetter(cls, propertyName);
            // It's ok if two setters are generated concurrently
            setterCache.put(key, value);
        }

        return value;
    }
}
