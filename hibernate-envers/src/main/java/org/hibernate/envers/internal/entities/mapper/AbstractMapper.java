/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;

/**
 * A base class for all entity mapper implementations.
 *
 * @author Chris Cranford
 */
public abstract class AbstractMapper {

	/**
	 * Get a value from the specified object.
	 *
	 * @param propertyData the property data, should not be {@literal null}
	 * @param object the object for which the value should be read, should not be {@literal null}
	 * @param serviceRegistry the service registry, should not be {@literal null}
	 * @param <T> the return type
	 * @return the value read from the object, may be {@literal null}
	 */
	@SuppressWarnings("unchecked")
	protected <T> T getValueFromObject(PropertyData propertyData, Object object, ServiceRegistry serviceRegistry) {
		final Getter getter = ReflectionTools.getGetter( object.getClass(), propertyData, serviceRegistry );
		return (T) getter.get( object );
	}

	/**
	 * Get a value from the specified object.
	 *
	 * @param propertyName the property name, should not be {@literal null}
	 * @param accessType the property access type, should not be {@literal null}
	 * @param object the object for which the value should be read, should not be {@literal null}
	 * @param serviceRegistry the service registry, should not be {@literal null}
	 * @param <T> the return type
	 * @return the value read from the object, may be {@literal null}
	 */
	@SuppressWarnings("unchecked")
	protected <T> T getValueFromObject(String propertyName, String accessType, Object object, ServiceRegistry serviceRegistry) {
		final Getter getter = ReflectionTools.getGetter( object.getClass(), propertyName, accessType, serviceRegistry );
		return (T) getter.get( object );
	}

	/**
	 * Set the specified value on the object.
	 *
	 * @param propertyData the property data, should not be {@literal null}
	 * @param object the object for which the value should be set, should not be {@literal null}
	 * @param value the value ot be set, may be {@literal null}
	 * @param serviceRegistry the service registry, should not be {@literal null}
	 */
	protected void setValueOnObject(PropertyData propertyData, Object object, Object value, ServiceRegistry serviceRegistry) {
		final Setter setter = ReflectionTools.getSetter(object.getClass(), propertyData, serviceRegistry );
		setter.set( object, value );
	}

	/**
	 * Gets the value from the source object and sets the value in the destination object.
	 *
	 * @param propertyData the property data, should not be {@literal null}
	 * @param source the source object, should not be {@literal null}
	 * @param destination the destination object, should not be {@literal null}
	 * @param serviceRegistry the service registry, should not be {@literal null}
	 */
	protected void getAndSetValue(PropertyData propertyData, Object source, Object destination, ServiceRegistry serviceRegistry) {
		final Getter getter = ReflectionTools.getGetter( source.getClass(), propertyData, serviceRegistry );
		final Setter setter = ReflectionTools.getSetter( destination.getClass(), propertyData, serviceRegistry );
		setter.set( destination, getter.get( source ) );
	}

	/**
	 * Creates a new object based on the specified class with the given constructor arguments.
	 *
	 * @param clazz the class, must not be {@literal null}
	 * @param args the variadic constructor arguments, may be omitted.
	 * @param <T> the return class type
	 * @return a new instance of the class
	 */
	protected <T> T newObjectInstance(Class<T> clazz, Object... args) {
		try {
			final Constructor<T> constructor = ReflectHelper.getDefaultConstructor( clazz );
			if ( constructor == null ) {
				throw new AuditException( "Failed to locate default constructor for class: " + clazz.getName() );
			}
			return constructor.newInstance( args );
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new AuditException( e );
		}
	}
}
