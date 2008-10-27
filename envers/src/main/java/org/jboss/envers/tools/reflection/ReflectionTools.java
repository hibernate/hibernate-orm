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
package org.jboss.envers.tools.reflection;

import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.BasicPropertyAccessor;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.util.ReflectHelper;
import org.jboss.envers.exception.VersionsException;
import org.jboss.envers.tools.Pair;
import org.jboss.envers.tools.ConcurrentReferenceHashMap;

import java.util.Map;

import static org.jboss.envers.tools.Pair.make;

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

    private static final PropertyAccessor BASIC_PROPERTY_ACCESSOR = new BasicPropertyAccessor();

    public static Class<?> loadClass(String name) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new VersionsException(e);
        }
    }

    public static Getter getGetter(Class cls, String propertyName) {
        Pair<Class, String> key = make(cls, propertyName);
        Getter value = getterCache.get(key);
        if (value == null) {
            value = ReflectHelper.getGetter(cls, propertyName);
            // It's ok if two getters are generated concurrently
            getterCache.put(key, value);
        }

        return value;
    }

    public static Setter getSetter(Class cls, String propertyName) {
        Pair<Class, String> key = make(cls, propertyName);
        Setter value = setterCache.get(key);
        if (value == null) {
            value = BASIC_PROPERTY_ACCESSOR.getSetter(cls, propertyName);
            // It's ok if two setters are generated concurrently
            setterCache.put(key, value);
        }

        return value;
    }
}
