/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxy.pojo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Subclass;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.HibernateProxy;

/**
 * Most of this code was originally an internal detail of {@code PojoEntityTuplizer},
 * then extracted to make it easier for integrators to initialize a custom
 * {@link org.hibernate.proxy.ProxyFactory}.
 *
 * @deprecated No longer used. Will be removed.
 */
@Deprecated(since = "7.2", forRemoval = true)
public final class ProxyFactoryHelper {

	private ProxyFactoryHelper() {
		//not meant to be instantiated
	}

	public static Set<Class<?>> extractProxyInterfaces(final PersistentClass persistentClass, final String entityName) {
		final Set<Class<?>> proxyInterfaces = new java.util.HashSet<>();
		final Class<?> mappedClass = persistentClass.getMappedClass();
		final Class<?> proxyInterface = persistentClass.getProxyInterface();

		if ( proxyInterface != null && !mappedClass.equals( proxyInterface ) ) {
			if ( !proxyInterface.isInterface() ) {
				throw new MappingException(
						"proxy must be either an interface, or the class itself: " + entityName
				);
			}
			proxyInterfaces.add( proxyInterface );
		}

		if ( mappedClass.isInterface() ) {
			proxyInterfaces.add( mappedClass );
		}

		for ( Subclass subclass : persistentClass.getSubclasses() ) {
			final Class<?> subclassProxy = subclass.getProxyInterface();
			final Class<?> subclassClass = subclass.getMappedClass();
			if ( subclassProxy != null && !subclassClass.equals( subclassProxy ) ) {
				if ( !subclassProxy.isInterface() ) {
					throw new MappingException(
							"proxy must be either an interface, or the class itself: " + subclass.getEntityName()
					);
				}
				proxyInterfaces.add( subclassProxy );
			}
		}

		proxyInterfaces.add( HibernateProxy.class );
		return proxyInterfaces;
	}

	public static void validateProxyability(final PersistentClass persistentClass) {
		Class<?> clazz = persistentClass.getMappedClass();
		for ( Property property : persistentClass.getProperties() ) {
			validateGetterSetterMethodProxyability( "Getter", property.getGetter( clazz ).getMethod() );
			validateGetterSetterMethodProxyability( "Setter", property.getSetter( clazz ).getMethod() );
		}
	}

	public static void validateGetterSetterMethodProxyability(String getterOrSetter, Method method ) {
		if ( method != null && Modifier.isFinal( method.getModifiers() ) ) {
			throw new HibernateException(
					String.format(
							"%s methods of lazy classes cannot be final: %s#%s",
							getterOrSetter,
							method.getDeclaringClass().getName(),
							method.getName()
					)
			);
		}
	}

	public static Method extractProxySetIdentifierMethod(final Setter idSetter, final Class<?> proxyInterface) {
		Method idSetterMethod = idSetter == null ? null : idSetter.getMethod();

		return idSetterMethod == null || proxyInterface == null
				? null
				: ReflectHelper.getMethod( proxyInterface, idSetterMethod );
	}

	public static Method extractProxyGetIdentifierMethod(final Getter idGetter, final Class<?> proxyInterface) {
		Method idGetterMethod = idGetter == null ? null : idGetter.getMethod();

		return idGetterMethod == null || proxyInterface == null
				? null
				: ReflectHelper.getMethod( proxyInterface, idGetterMethod );
	}
}
