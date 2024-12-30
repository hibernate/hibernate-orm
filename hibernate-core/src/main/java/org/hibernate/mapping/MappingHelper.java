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
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.resource.beans.spi.ProvidedInstanceManagedBeanImpl;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CustomCollectionType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.MetaType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.SpecialOneToOneType;
import org.hibernate.type.Type;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserCollectionType;

import static org.hibernate.internal.util.PropertiesHelper.map;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * @author Steve Ebersole
 */
@Internal
public final class MappingHelper {
	private final static Properties EMPTY_PROPERTIES = new Properties();

	private MappingHelper() {
	}

	public static CollectionType customCollection(
			String typeName,
			Properties typeParameters,
			String role,
			String propertyRef,
			MetadataImplementor metadata) {
		final ManagedBean<? extends UserCollectionType> userTypeBean =
				createUserTypeBean( role, classForName( typeName, metadata ), map( typeParameters ), metadata );
		return new CustomCollectionType( userTypeBean, role, propertyRef );
	}

	private static ManagedBean<? extends UserCollectionType> createUserTypeBean(
			String role,
			Class<? extends UserCollectionType> userCollectionTypeClass,
			Map<String, ?> parameters,
			MetadataImplementor metadata) {
		return metadata.getMetadataBuildingOptions().isAllowExtensionsInCdi()
				? createSharedUserTypeBean( role, userCollectionTypeClass, parameters, metadata )
				: createLocalUserTypeBean( role, userCollectionTypeClass, parameters );
	}

	public static ManagedBean<? extends UserCollectionType> createCustomTypeBean(
			String role,
			Class<? extends UserCollectionType> implementation,
			Map<String, ?> parameters,
			MetadataBuildingContext context) {
		// if deferred container access is enabled, we locally create the user-type
		return context.getBuildingOptions().isAllowExtensionsInCdi()
				? createSharedUserTypeBean( role, implementation, parameters, context.getMetadataCollector() )
				: createLocalUserTypeBean( role, implementation, parameters );
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

	private static Class<UserCollectionType> classForName(String typeName, MetadataImplementor metadata) {
		return metadata.getMetadataBuildingOptions().getServiceRegistry()
				.requireService( ClassLoaderService.class )
				.classForName( typeName );
	}

	private static ManagedBeanRegistry getManagedBeanRegistry(MetadataImplementor metadata) {
		return metadata.getMetadataBuildingOptions().getServiceRegistry()
				.requireService( ManagedBeanRegistry.class );
	}

	public static void throwIgnoredCollectionTypeParameters(String role, Class<?> implementation) {
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

	public static AnyType anyMapping(
			Type discriminatorType,
			Type identifierType,
			Map<Object, String> explicitValeMappings,
			ImplicitDiscriminatorStrategy implicitValueStrategy,
			boolean lazy,
			MetadataBuildingContext buildingContext) {
		final MetaType metaType = new MetaType( discriminatorType, implicitValueStrategy, explicitValeMappings );
		return new AnyType( buildingContext.getBootstrapContext().getTypeConfiguration(), metaType, identifierType, lazy );
	}

	public static ManyToOneType manyToOne(
			String referencedEntityName,
			boolean referenceToPrimaryKey,
			String referencedPropertyName,
			String propertyName,
			boolean isLogicalOneToOne,
			boolean lazy,
			boolean unwrapProxy,
			boolean ignoreNotFound,
			MetadataBuildingContext buildingContext) {
		return new ManyToOneType(
				buildingContext.getBootstrapContext().getTypeConfiguration(),
				referencedEntityName,
				referenceToPrimaryKey,
				referencedPropertyName,
				propertyName,
				lazy,
				unwrapProxy,
				ignoreNotFound,
				isLogicalOneToOne
		);
	}

	public static SpecialOneToOneType specialOneToOne(
			String referencedEntityName,
			ForeignKeyDirection foreignKeyType,
			boolean referenceToPrimaryKey,
			String referencedPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String owningEntityName,
			String owningEntityPropertyName,
			boolean constrained,
			MetadataBuildingContext buildingContext) {
		return new SpecialOneToOneType(
				buildingContext.getBootstrapContext().getTypeConfiguration(),
				referencedEntityName,
				foreignKeyType,
				referenceToPrimaryKey,
				referencedPropertyName,
				lazy,
				unwrapProxy,
				owningEntityName,
				owningEntityPropertyName,
				constrained
		);
	}

	public static OneToOneType oneToOne(
			String referencedEntityName,
			ForeignKeyDirection foreignKeyType,
			boolean referenceToPrimaryKey,
			String referencedPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String owningEntityName,
			String owningEntityPropertyName,
			boolean constrained,
			MetadataBuildingContext buildingContext) {
		return new OneToOneType(
				buildingContext.getBootstrapContext().getTypeConfiguration(),
				referencedEntityName,
				foreignKeyType,
				referenceToPrimaryKey,
				referencedPropertyName,
				lazy,
				unwrapProxy,
				owningEntityName,
				owningEntityPropertyName,
				constrained
		);
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
}
