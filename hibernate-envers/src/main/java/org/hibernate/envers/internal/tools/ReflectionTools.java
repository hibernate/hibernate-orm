/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.tools;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.tools.Pair;
import org.hibernate.internal.util.collections.ConcurrentReferenceHashMap;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public abstract class ReflectionTools {
	private static final Map<Pair<Class, String>, Getter> GETTER_CACHE = new ConcurrentReferenceHashMap<>(
			10,
			ConcurrentReferenceHashMap.ReferenceType.SOFT,
			ConcurrentReferenceHashMap.ReferenceType.SOFT
	);
	private static final Map<Pair<Class, String>, Setter> SETTER_CACHE = new ConcurrentReferenceHashMap<>(
			10,
			ConcurrentReferenceHashMap.ReferenceType.SOFT,
			ConcurrentReferenceHashMap.ReferenceType.SOFT
	);

	private static PropertyAccessStrategy getAccessStrategy(Class<?> cls, ServiceRegistry serviceRegistry, String accessorType) {
		return serviceRegistry.getService( PropertyAccessStrategyResolver.class )
				.resolvePropertyAccessStrategy( cls, accessorType, null );
	}

	public static Getter getGetter(Class cls, PropertyData propertyData, ServiceRegistry serviceRegistry) {
		if ( propertyData.getPropertyAccessStrategy() == null ) {
			return getGetter( cls, propertyData.getBeanName(), propertyData.getAccessType(), serviceRegistry );
		}
		else {
			final String propertyName = propertyData.getName();
			final Pair<Class, String> key = Pair.make( cls, propertyName );
			Getter value = GETTER_CACHE.get( key );
			if ( value == null ) {
				value = propertyData.getPropertyAccessStrategy().buildPropertyAccess(
						cls,
						propertyData.getBeanName(), false
				).getGetter();
				// It's ok if two getters are generated concurrently
				GETTER_CACHE.put( key, value );
			}
			return value;
		}
	}

	public static Getter getGetter(Class cls, String propertyName, String accessorType, ServiceRegistry serviceRegistry) {
		final Pair<Class, String> key = Pair.make( cls, propertyName );
		Getter value = GETTER_CACHE.get( key );
		if ( value == null ) {
			value = getAccessStrategy( cls, serviceRegistry, accessorType ).buildPropertyAccess( cls, propertyName, true ).getGetter();
			// It's ok if two getters are generated concurrently
			GETTER_CACHE.put( key, value );
		}

		return value;
	}

	public static Setter getSetter(Class cls, PropertyData propertyData, ServiceRegistry serviceRegistry) {
		return getSetter( cls, propertyData.getBeanName(), propertyData.getAccessType(), serviceRegistry );
	}

	public static Setter getSetter(Class cls, String propertyName, String accessorType, ServiceRegistry serviceRegistry) {
		final Pair<Class, String> key = Pair.make( cls, propertyName );
		Setter value = SETTER_CACHE.get( key );
		if ( value == null ) {
			value = getAccessStrategy( cls, serviceRegistry, accessorType ).buildPropertyAccess( cls, propertyName, true ).getSetter();
			// It's ok if two setters are generated concurrently
			SETTER_CACHE.put( key, value );
		}

		return value;
	}

	public static Field getField(Class cls, PropertyData propertyData) {
		Field field = null;
		Class<?> clazz = cls;
		while ( clazz != null && field == null ) {
			try {
				field = clazz.getDeclaredField( propertyData.getName() );
			}
			catch ( Exception e ) {
				// ignore
			}
			clazz = clazz.getSuperclass();
		}
		return field;
	}

	public static Class<?> getType(Class cls, PropertyData propertyData, ServiceRegistry serviceRegistry) {
		final Setter setter = getSetter( cls, propertyData, serviceRegistry );
		if ( setter.getMethod() != null && setter.getMethod().getParameterCount() > 0 ) {
			return setter.getMethod().getParameterTypes()[0];
		}

		final Field field = getField( cls, propertyData );
		if ( field != null ) {
			return field.getType();
		}

		throw new AuditException(
				String.format(
						Locale.ROOT,
						"Failed to determine type for field [%s] on class [%s].",
						propertyData.getName(),
						cls.getName()
				)
		);
	}

	/**
	 * Locate class with a given name.
	 *
	 * @param name Fully qualified class name.
	 * @param classLoaderService Class loading service. Passing {@code null} is "allowed", but will result in
	 * TCCL usage.
	 *
	 * @return The cass reference.
	 *
	 * @throws ClassLoadingException Indicates the class could not be found.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> loadClass(String name, ClassLoaderService classLoaderService)
			throws ClassLoadingException {
		try {
			if ( classLoaderService != null ) {
				return classLoaderService.classForName( name );
			}
			else {
				return (Class<T>) Thread.currentThread().getContextClassLoader().loadClass( name );
			}
		}
		catch (Exception e) {
			throw new ClassLoadingException( "Unable to load class [" + name + "]", e );
		}
	}

	public static void reset() {
		SETTER_CACHE.clear();
		GETTER_CACHE.clear();
	}
}
