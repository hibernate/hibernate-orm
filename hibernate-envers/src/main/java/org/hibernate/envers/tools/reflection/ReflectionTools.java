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

import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.tools.ConcurrentReferenceHashMap;
import org.hibernate.envers.tools.Pair;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hibernate.envers.tools.Pair.make;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
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

    /**
     * Returns property value by invoking getter method or directly retrieving field's reference.
     * @param member Field or getter method representation.
     * @param object Object instance.
     * @return Property value.
     */
    public static Object getPropertyValue(Member member, Object object) {
        try {
            if (member instanceof Field) {
                Field field = (Field) member;
                field.setAccessible(true);
                return field.get(object);
            } else if (member instanceof Method) {
                Method method = (Method) member;
                method.setAccessible(true);
                return method.invoke(object);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new IllegalArgumentException("Unsupported member type: " + member + ".");
    }

    /** 
     * @param clazz Class type.
     * @param annotation Searched annotation.
     * @return Set of annotated members ({@link Field} and/or {@link Method}).
     */
    public static Set<Member> getAnnotatedMembers(Class clazz, Class<? extends Annotation> annotation) {
        try {
            Set<Field> annotatedFields = new HashSet<Field>();
            Set<Method> annotatedMethods = new HashSet<Method>();
            doGetAnnotatedFields(clazz, annotation, annotatedFields);
            doGetAnnotatedMethods(clazz, annotation, annotatedMethods);
            Set<Member> result = new HashSet<Member>(annotatedFields.size() + annotatedMethods.size());
            for (Field field : annotatedFields) {
                field.setAccessible(true);
                result.add(field);
            }
            for (Method method : annotatedMethods) {
                method.setAccessible(true);
                result.add(method);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Populates {@code fields} set with all properties marked with a given annotation.
     * @param clazz Class type.
     * @param annotation Annotation.
     * @param fields Set of annotated fields. Shall be initialized externally.
     */
    private static void doGetAnnotatedFields(Class clazz, Class<? extends Annotation> annotation, Set<Field> fields) {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(annotation)) {
                fields.add(field);
            }
        }
        Class superClass = clazz.getSuperclass();
        if (!Object.class.equals(superClass)) {
            doGetAnnotatedFields(superClass, annotation, fields);
        }
    }

    /**
     * Populates {@code methods} set with all functions marked with a given annotation.
     * @param clazz Class type.
     * @param annotation Annotation.
     * @param methods Set of annotated methods. Shall be initialized externally.
     */
    private static void doGetAnnotatedMethods(Class clazz, Class<? extends Annotation> annotation, Set<Method> methods) {
        for (Method method : clazz.getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.isAnnotationPresent(annotation)) {
                methods.add(method);
            }
        }
        Class superClass = clazz.getSuperclass();
        if (!Object.class.equals(superClass)) {
            doGetAnnotatedMethods(superClass, annotation, methods);
        }
    }
}
