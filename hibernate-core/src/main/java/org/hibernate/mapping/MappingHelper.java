/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.BootLogging;
import org.hibernate.boot.model.internal.DelayedParameterizedTypeBean;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
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

import static org.hibernate.metamodel.mapping.MappingModelCreationLogging.MAPPING_MODEL_CREATION_MESSAGE_LOGGER;

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
		final Class<? extends UserCollectionType> userCollectionTypeClass =
				metadata.getMetadataBuildingOptions().getServiceRegistry()
						.requireService( ClassLoaderService.class )
						.classForName( typeName );

		final boolean hasParameters = CollectionHelper.isNotEmpty( typeParameters );
		final ManagedBean<? extends UserCollectionType> userTypeBean;

		if ( !metadata.getMetadataBuildingOptions().isAllowExtensionsInCdi() ) {
			//noinspection unchecked,rawtypes
			userTypeBean = createLocalUserCollectionTypeBean(
					role,
					userCollectionTypeClass,
					hasParameters,
					(Map) typeParameters
			);
		}
		else {
			final ManagedBean<? extends UserCollectionType> userCollectionTypeBean =
					metadata.getMetadataBuildingOptions()
							.getServiceRegistry()
							.requireService( ManagedBeanRegistry.class )
							.getBean( userCollectionTypeClass );

			if ( hasParameters ) {
				if ( ParameterizedType.class.isAssignableFrom( userCollectionTypeBean.getBeanClass() ) ) {
					// create a copy of the parameters and create a bean wrapper to delay injecting
					// the parameters, thereby delaying the need to resolve the instance from the
					// wrapped bean
					final Properties copy = new Properties();
					copy.putAll( typeParameters );
					userTypeBean = new DelayedParameterizedTypeBean<>( userCollectionTypeBean, copy );
				}
				else {
					// there were parameters, but the custom-type does not implement the interface
					// used to inject them - log a "warning"
					BootLogging.BOOT_LOGGER.debugf(
							"`@CollectionType` (%s) specified parameters, but the" +
									" implementation does not implement `%s` which is used to inject them - `%s`",
							role,
							ParameterizedType.class.getName(),
							userCollectionTypeClass.getName()
					);
					userTypeBean = userCollectionTypeBean;
				}
			}
			else {
				userTypeBean = userCollectionTypeBean;
			}
		}

		return new CustomCollectionType( userTypeBean, role, propertyRef );
	}

	public static void injectParameters(Object type, Properties parameters) {
		if ( type instanceof ParameterizedType ) {
			( (ParameterizedType) type ).setParameterValues( parameters == null ? EMPTY_PROPERTIES : parameters );
		}
		else if ( parameters != null && !parameters.isEmpty() ) {
			MAPPING_MODEL_CREATION_MESSAGE_LOGGER.debugf(
					"UserCollectionType impl does not implement ParameterizedType but parameters were present : `%s`",
					type.getClass().getName()
			);
		}
	}

	public static void injectParameters(Object type, Supplier<Properties> parameterAccess) {
		injectParameters( type, parameterAccess.get() );
	}

	public static AnyType anyMapping(
			Type metaType,
			Type identifierType,
			Map<Object, String> metaValueToEntityNameMap,
			boolean lazy,
			MetadataBuildingContext buildingContext) {
		if ( metaValueToEntityNameMap != null ) {
			metaType = new MetaType( metaValueToEntityNameMap, metaType );
		}

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

	public static ManagedBean<UserCollectionType> createLocalUserCollectionTypeBean(
			String role,
			Class<? extends UserCollectionType> implementation,
			boolean hasParameters,
			Map<String, String> parameters) {
		final UserCollectionType userCollectionType = FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( implementation );

		if ( hasParameters ) {
			// `@CollectionType` declared parameters - inject them
			if ( userCollectionType instanceof ParameterizedType ) {
				final Properties properties = new Properties();
				properties.putAll( parameters );
				( (ParameterizedType) userCollectionType ).setParameterValues( properties );
			}
			else {
				// there were parameters, but the custom-type does not implement the interface
				// used to inject them - log a "warning"
				BootLogging.BOOT_LOGGER.debugf(
						"`@CollectionType` (%s) specified parameters, but the" +
								" implementation does not implement `%s` which is used to inject them - `%s`",
						role,
						ParameterizedType.class.getName(),
						implementation.getName()
				);

				// use the un-configured instance
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
