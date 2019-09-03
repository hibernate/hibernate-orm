/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

import java.util.Map;
import java.util.Properties;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CustomCollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.MetaType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.SpecialOneToOneType;
import org.hibernate.type.Type;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserCollectionType;

/**
 * @author Steve Ebersole
 */
@Internal
final class MappingHelper {
	private final static Properties EMPTY_PROPERTIES = new Properties();

	private MappingHelper() {
	}

	public static CollectionType customCollection(
			String typeName,
			Properties typeParameters,
			String role,
			String propertyRef,
			MetadataImplementor metadata) {
		final ClassLoaderService cls = metadata.getMetadataBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );

		try {
			final Class<? extends UserCollectionType> typeClass = cls.classForName( typeName );

			CustomCollectionType result = new CustomCollectionType( typeClass, role, propertyRef, metadata.getTypeConfiguration() );
			if ( typeParameters != null ) {
				injectParameters( result.getUserType(), typeParameters );
			}

			return result;
		}
		catch (ClassLoadingException e) {
			throw new MappingException( "user collection type class not found: " + typeName, e );
		}
	}

	public static void injectParameters(Object type, Properties parameters) {
		if ( type instanceof ParameterizedType ) {
			if ( parameters == null ) {
				( (ParameterizedType) type ).setParameterValues( EMPTY_PROPERTIES );
			}
			else {
				( (ParameterizedType) type ).setParameterValues( parameters );
			}
		}
		else if ( parameters != null && !parameters.isEmpty() ) {
			throw new MappingException( "UserCollectionType impl does not implement ParameterizedType but parameters were present : " + type.getClass().getName() );
		}
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
}
