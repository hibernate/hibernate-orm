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
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ProvidedInstanceManagedBeanImpl;
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

	public static ManagedBean<? extends UserCollectionType> createUserTypeBean(
			String role,
			Class<? extends UserCollectionType> userCollectionTypeClass,
			Map<String, ?> parameters,
			BootstrapContext bootstrapContext,
			boolean allowExtensionsInCdi) {
		return allowExtensionsInCdi
				? createSharedUserTypeBean( role, userCollectionTypeClass, parameters, bootstrapContext )
				: createLocalUserTypeBean( role, userCollectionTypeClass, parameters );
	}

	private static ManagedBean<? extends UserCollectionType> createSharedUserTypeBean(
			String role,
			Class<? extends UserCollectionType> userCollectionTypeClass,
			Map<String, ?> parameters,
			BootstrapContext bootstrapContext) {
		final ManagedBean<? extends UserCollectionType> managedBean =
				bootstrapContext.getManagedBeanRegistry().getBean( userCollectionTypeClass );
		if ( isNotEmpty( parameters ) ) {
			if ( ParameterizedType.class.isAssignableFrom( managedBean.getBeanClass() ) ) {
				// create a copy of the parameters and create a bean wrapper to delay injecting
				// the parameters, thereby delaying the need to resolve the instance from the
				// wrapped bean
				final Properties copy = new Properties();
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
		else if ( parameters != null && !parameters.isEmpty() ) {
			throw new MappingException( "'UserType' implementation '" + type.getClass().getName()
					+ "' does not implement 'ParameterizedType' but parameters were provided" );
		}
	}

	private static ManagedBean<UserCollectionType> createLocalUserTypeBean(
			String role,
			Class<? extends UserCollectionType> implementation,
			Map<String, ?> parameters) {
		final UserCollectionType userCollectionType =
				FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( implementation );
		if ( isNotEmpty( parameters ) ) {
			// CollectionType declared parameters - inject them
			if ( userCollectionType instanceof ParameterizedType parameterizedType ) {
				final Properties properties = new Properties();
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
			Set<String> distinctColumns,
			List<Property> properties,
			String owner) throws MappingException {
		for ( Property prop : properties ) {
			if ( prop.isUpdatable() || prop.isInsertable() ) {
				prop.getValue().checkColumnDuplication( distinctColumns, owner );
			}
		}
	}

	static Class<?> classForName(String typeName, BootstrapContext bootstrapContext) {
		return bootstrapContext.getClassLoaderAccess().classForName( typeName );
	}

	static <T> Class<? extends T> classForName(Class<T> supertype, String typeName, BootstrapContext bootstrapContext) {
		final Class<?> clazz = classForName( typeName, bootstrapContext );
		if ( supertype.isAssignableFrom( clazz ) ) {
			//noinspection unchecked
			return (Class<? extends T>) clazz;
		}
		else {
			throw new MappingException( "Class '" + typeName + "' does not implement '" + supertype.getName() + "'" );
		}
	}
}
