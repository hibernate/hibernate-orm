/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.model.internal.DelayedParameterizedTypeBean;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.resource.beans.spi.ProvidedInstanceManagedBeanImpl;
import org.hibernate.usertype.AnnotationBasedUserType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserCollectionType;

import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * @author Steve Ebersole
 */
@Internal
public final class MappingHelper {
	private final static Properties EMPTY_PROPERTIES = new Properties();

	private MappingHelper() {
	}

	/**
	 * Creates the live extension instance for a declaratively specified
	 * {@link CompositeUserType}, consistently for normal boot and archive
	 * restoration.
	 */
	public static CompositeUserType<?> createCompositeUserType(
			Class<? extends CompositeUserType<?>> implementation,
			ManagedBeanRegistry managedBeanRegistry,
			boolean allowExtensionsInCdi) {
		return allowExtensionsInCdi
				? managedBeanRegistry.getBean( implementation ).getBeanInstance()
				: FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( implementation );
	}

	public static ManagedBean<? extends UserCollectionType> createUserTypeBean(
			String role,
			Class<? extends UserCollectionType> userCollectionTypeClass,
			Map<String, ?> parameters,
			ManagedBeanRegistry managedBeanRegistry,
			boolean allowExtensionsInCdi) {
		return allowExtensionsInCdi
				? createSharedUserTypeBean( role, userCollectionTypeClass, parameters, managedBeanRegistry )
				: createLocalUserTypeBean( role, userCollectionTypeClass, parameters );
	}

	private static ManagedBean<? extends UserCollectionType> createSharedUserTypeBean(
			String role,
			Class<? extends UserCollectionType> userCollectionTypeClass,
			Map<String, ?> parameters,
			ManagedBeanRegistry managedBeanRegistry) {
		final var managedBean =
				managedBeanRegistry.getBean( userCollectionTypeClass );
		if ( isNotEmpty( parameters ) ) {
			if ( ParameterizedType.class.isAssignableFrom( managedBean.getBeanClass() ) ) {
				// create a copy of the parameters and create a bean wrapper to delay injecting
				// the parameters, thereby delaying the need to resolve the instance from the
				// wrapped bean
				final var copy = new Properties();
				copy.putAll( parameters );
				return new DelayedParameterizedTypeBean<>( managedBean, copy );
			}
			else {
				throwIgnoredCollectionTypeParameters( role, userCollectionTypeClass );
			}
		}
		return managedBean;
	}

	private static void throwIgnoredCollectionTypeParameters(String role, Class<?> implementation) {
		throw new MappingException( "'@CollectionType' [" + role + "] specified parameters, but the implementation '"
				+ implementation.getName() + "' does not implement 'ParameterizedType' which is used to inject them" );
	}

	public static void injectParameters(Object type, Properties parameters) {
		if ( type instanceof ParameterizedType parameterizedType ) {
			parameterizedType.setParameterValues( parameters == null ? EMPTY_PROPERTIES : parameters );
		}
		else if ( parameters != null && !parameters.isEmpty()
				&& !( type instanceof AnnotationBasedUserType<?,?> ) ) {
			throw new MappingException( "'UserType' implementation '" + type.getClass().getName()
					+ "' does not implement 'ParameterizedType' or 'AnnotationBasedUserType' but parameters were provided" );
		}
	}

	private static ManagedBean<UserCollectionType> createLocalUserTypeBean(
			String role,
			Class<? extends UserCollectionType> implementation,
			Map<String, ?> parameters) {
		final var userCollectionType =
				FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( implementation );
		if ( isNotEmpty( parameters ) ) {
			// CollectionType declared parameters - inject them
			if ( userCollectionType instanceof ParameterizedType parameterizedType ) {
				final var properties = new Properties();
				properties.putAll( parameters );
				parameterizedType.setParameterValues( properties );
			}
			else {
				throwIgnoredCollectionTypeParameters( role, implementation );
			}
		}
		return new ProvidedInstanceManagedBeanImpl<>( userCollectionType );
	}

	public static void checkPropertyColumnDuplication(
			Set<QualifiedColumnName> distinctColumns,
			List<Property> properties,
			String owner,
			Database database) throws MappingException {
		for ( var property : properties ) {
			if ( ( property.isUpdatable() || property.isInsertable() ) && !property.isGenericSpecialization() ) {
				property.getValue().checkColumnDuplication( distinctColumns, owner, database );
			}
		}
	}

	static Class<?> classForName(String typeName, ClassLoaderAccess classLoaderAccess) {
		if ( classLoaderAccess != null ) {
			return classLoaderAccess.classForName( typeName );
		}
		return classForName( typeName );
	}

	static Class<?> classForName(String typeName) {
		try {
			return Class.forName( typeName );
		}
		catch (ClassNotFoundException e) {
			throw new MappingException( "Class '" + typeName + "' could not be loaded", e );
		}
	}

	static <T> Class<? extends T> classForName(Class<T> supertype, String typeName, ClassLoaderAccess classLoaderAccess) {
		final var clazz = classForName( typeName, classLoaderAccess );
		if ( supertype.isAssignableFrom( clazz ) ) {
			//noinspection unchecked
			return (Class<? extends T>) clazz;
		}
		else {
			throw new MappingException( "Class '" + typeName + "' does not implement '" + supertype.getName() + "'" );
		}
	}
}
