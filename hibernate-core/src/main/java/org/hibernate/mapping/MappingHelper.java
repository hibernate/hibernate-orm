/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
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
			MetadataImplementor metadata) {
		return metadata.getMetadataBuildingOptions().isAllowExtensionsInCdi()
				? createSharedUserTypeBean( role, userCollectionTypeClass, parameters, metadata )
				: createLocalUserTypeBean( role, userCollectionTypeClass, parameters );
	}

	private static ManagedBean<? extends UserCollectionType> createSharedUserTypeBean(
			String role,
			Class<? extends UserCollectionType> userCollectionTypeClass,
			Map<String, ?> parameters,
			MetadataImplementor metadata) {
		final ManagedBean<? extends UserCollectionType> managedBean =
				getManagedBeanRegistry( metadata ).getBean( userCollectionTypeClass );
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

	private static ManagedBeanRegistry getManagedBeanRegistry(MetadataImplementor metadata) {
		return metadata.getMetadataBuildingOptions().getServiceRegistry()
				.requireService( ManagedBeanRegistry.class );
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
			if ( prop.isUpdateable() || prop.isInsertable() ) {
				prop.getValue().checkColumnDuplication( distinctColumns, owner );
			}
		}
	}

	static Class<?> classForName(String typeName, MetadataImplementor metadata) {
		return metadata.getMetadataBuildingOptions().getServiceRegistry()
				.requireService( ClassLoaderService.class )
				.classForName( typeName );
	}

	static <T> Class<? extends T> classForName(Class<T> supertype, String typeName, MetadataImplementor metadata) {
		final Class<?> clazz = classForName( typeName, metadata );
		if ( supertype.isAssignableFrom( clazz ) ) {
			//noinspection unchecked
			return (Class<? extends T>) clazz;
		}
		else {
			throw new MappingException( "Class '" + typeName + "' does not implement '" + supertype.getName() + "'" );
		}
	}
}
