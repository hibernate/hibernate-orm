/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.query.results.dynamic.DynamicResultBuilderAttribute;
import org.hibernate.query.results.dynamic.DynamicResultBuilderBasic;
import org.hibernate.query.results.dynamic.DynamicResultBuilderBasicConverted;
import org.hibernate.query.results.dynamic.DynamicResultBuilderBasicStandard;
import org.hibernate.query.results.dynamic.DynamicResultBuilderEntityCalculated;
import org.hibernate.query.results.dynamic.DynamicResultBuilderEntityStandard;
import org.hibernate.query.results.dynamic.DynamicResultBuilderInstantiation;
import org.hibernate.query.results.implicit.ImplicitFetchBuilder;
import org.hibernate.query.results.implicit.ImplicitFetchBuilderBasic;
import org.hibernate.query.results.implicit.ImplicitFetchBuilderDiscriminatedAssociation;
import org.hibernate.query.results.implicit.ImplicitFetchBuilderEmbeddable;
import org.hibernate.query.results.implicit.ImplicitFetchBuilderEntity;
import org.hibernate.query.results.implicit.ImplicitFetchBuilderEntityPart;
import org.hibernate.query.results.implicit.ImplicitFetchBuilderPlural;
import org.hibernate.query.results.implicit.ImplicitModelPartResultBuilderEntity;
import org.hibernate.query.results.implicit.ImplicitResultClassBuilder;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * @author Steve Ebersole
 */
public class Builders {
	public static DynamicResultBuilderBasic scalar(String columnAlias) {
		return scalar( columnAlias, columnAlias );
	}

	public static DynamicResultBuilderBasic scalar(String columnAlias, String resultAlias) {
		return new DynamicResultBuilderBasicStandard( columnAlias, resultAlias );
	}

	public static DynamicResultBuilderBasic scalar(String columnAlias, BasicType<?> type) {
		return scalar( columnAlias, columnAlias, type );
	}

	public static DynamicResultBuilderBasic scalar(String columnAlias, String resultAlias, BasicType<?> type) {
		return new DynamicResultBuilderBasicStandard( columnAlias, resultAlias, type );
	}

	public static DynamicResultBuilderBasic scalar(
			String columnAlias,
			Class<?> javaType,
			SessionFactoryImplementor factory) {
		return scalar( columnAlias, columnAlias, javaType, factory );
	}

	public static DynamicResultBuilderBasic scalar(
			String columnAlias,
			String resultAlias,
			Class<?> javaTypeClass,
			SessionFactoryImplementor factory) {
		final JavaType<?> javaType = factory.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( javaTypeClass );

		return new DynamicResultBuilderBasicStandard( columnAlias, resultAlias, javaType );
	}

	public static <R> ResultBuilder converted(
			String columnAlias,
			Class<R> jdbcJavaType,
			AttributeConverter<?, R> converter,
			SessionFactoryImplementor sessionFactory) {
		return converted( columnAlias, null, jdbcJavaType, converter, sessionFactory );
	}

	public static <O,R> ResultBuilder converted(
			String columnAlias,
			Class<O> domainJavaType,
			Class<R> jdbcJavaType,
			AttributeConverter<O, R> converter,
			SessionFactoryImplementor sessionFactory) {
		return new DynamicResultBuilderBasicConverted<>( columnAlias, domainJavaType, jdbcJavaType, converter, sessionFactory );
	}

	public static <R> ResultBuilder converted(
			String columnAlias,
			Class<R> jdbcJavaType,
			Class<? extends AttributeConverter<?, R>> converterJavaType,
			SessionFactoryImplementor sessionFactory) {
		return converted( columnAlias, null, jdbcJavaType, (Class) converterJavaType, sessionFactory );
	}

	public static <O,R> ResultBuilder converted(
			String columnAlias,
			Class<O> domainJavaType,
			Class<R> jdbcJavaType,
			Class<? extends AttributeConverter<O,R>> converterJavaType,
			SessionFactoryImplementor sessionFactory) {
		return new DynamicResultBuilderBasicConverted<>( columnAlias, domainJavaType, jdbcJavaType, converterJavaType, sessionFactory );
	}

	public static ResultBuilderBasicValued scalar(int position) {
		// will be needed for interpreting legacy HBM <resultset/> mappings
		return new DynamicResultBuilderBasicStandard( position );
	}

	public static ResultBuilderBasicValued scalar(int position, BasicType<?> type) {
		// will be needed for interpreting legacy HBM <resultset/> mappings
		return new DynamicResultBuilderBasicStandard( position, type );
	}

	public static <J> DynamicResultBuilderInstantiation<J> instantiation(Class<J> targetJavaType, SessionFactoryImplementor factory) {
		final JavaType<J> targetJtd = factory.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( targetJavaType );
		return new DynamicResultBuilderInstantiation<>( targetJtd );
	}

	public static ResultBuilder attributeResult(
			String columnAlias,
			String entityName,
			String attributePath,
			SessionFactoryImplementor sessionFactory) {
		if ( attributePath.contains( "." ) ) {
			throw new UnsupportedOperationException(
					"Support for defining a NativeQuery attribute result based on a composite path is not yet implemented"
			);
		}

		final RuntimeMetamodels runtimeMetamodels = sessionFactory.getRuntimeMetamodels();
		final String fullEntityName = runtimeMetamodels.getMappingMetamodel().getImportedName( entityName );
		final EntityPersister entityMapping = runtimeMetamodels.getMappingMetamodel().findEntityDescriptor( fullEntityName );
		if ( entityMapping == null ) {
			throw new IllegalArgumentException( "Could not locate entity mapping : " + fullEntityName );
		}

		final AttributeMapping attributeMapping = entityMapping.findAttributeMapping( attributePath );
		if ( attributeMapping == null ) {
			throw new IllegalArgumentException( "Could not locate attribute mapping : " + fullEntityName + "." + attributePath );
		}

		if ( attributeMapping instanceof SingularAttributeMapping ) {
			final SingularAttributeMapping singularAttributeMapping = (SingularAttributeMapping) attributeMapping;
			return new DynamicResultBuilderAttribute( singularAttributeMapping, columnAlias, fullEntityName, attributePath );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Specified attribute mapping [%s.%s] not a basic attribute: %s",
						fullEntityName,
						attributePath,
						attributeMapping
				)
		);
	}

	public static ResultBuilder attributeResult(String columnAlias, SingularAttribute<?, ?> attribute) {
		if ( ! ( attribute.getDeclaringType() instanceof EntityType ) ) {
			throw new UnsupportedOperationException(
					"Support for defining a NativeQuery attribute result based on a composite path is not yet implemented"
			);
		}

		throw new UnsupportedOperationException();
	}

	/**
	 * Creates a EntityResultBuilder allowing for further configuring of the mapping.
	 *
	 */
	public static DynamicResultBuilderEntityStandard entity(
			String tableAlias,
			String entityName,
			SessionFactoryImplementor sessionFactory) {
		final RuntimeMetamodels runtimeMetamodels = sessionFactory.getRuntimeMetamodels();
		final EntityMappingType entityMapping = runtimeMetamodels.getEntityMappingType( entityName );

		return new DynamicResultBuilderEntityStandard( entityMapping, tableAlias );
	}

	/**
	 * Creates a EntityResultBuilder that does not allow any further configuring of the mapping.
	 *
	 * @see NativeQuery#addEntity(Class)
	 * @see NativeQuery#addEntity(String)
	 * @see NativeQuery#addEntity(String, Class)
	 * @see NativeQuery#addEntity(String, String)
	 */
	public static DynamicResultBuilderEntityCalculated entityCalculated(
			String tableAlias,
			String entityName,
			SessionFactoryImplementor sessionFactory) {
		return entityCalculated( tableAlias, entityName, null, sessionFactory );
	}

	/**
	 * Creates a EntityResultBuilder that does not allow any further configuring of the mapping.
	 *
	 * @see #entityCalculated(String, String, SessionFactoryImplementor)
	 * @see NativeQuery#addEntity(String, Class, LockMode)
	 * @see NativeQuery#addEntity(String, String, LockMode)
	 */
	public static DynamicResultBuilderEntityCalculated entityCalculated(
			String tableAlias,
			String entityName,
			LockMode explicitLockMode,
			SessionFactoryImplementor sessionFactory) {
		final EntityMappingType entityMapping = sessionFactory.getRuntimeMetamodels().getEntityMappingType( entityName );
		return new DynamicResultBuilderEntityCalculated( entityMapping, tableAlias, explicitLockMode );
	}

	public static DynamicFetchBuilderLegacy fetch(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		return new DynamicFetchBuilderLegacy( tableAlias, ownerTableAlias, joinPropertyName, new ArrayList<>(), new HashMap<>() );
	}

	public static ResultBuilder resultClassBuilder(
			Class<?> resultMappingClass,
			ResultSetMappingResolutionContext resolutionContext) {
		return resultClassBuilder( resultMappingClass, resolutionContext.getSessionFactory() );
	}

	public static ResultBuilder resultClassBuilder(
			Class<?> resultMappingClass,
			SessionFactoryImplementor sessionFactory) {
		final MappingMetamodelImplementor mappingMetamodel =
				sessionFactory.getRuntimeMetamodels()
						.getMappingMetamodel();
		final EntityMappingType entityMappingType = mappingMetamodel.findEntityDescriptor( resultMappingClass );
		if ( entityMappingType != null ) {
			// the resultClass is an entity
			return new ImplicitModelPartResultBuilderEntity( entityMappingType );
		}

		// todo : support for known embeddables might be nice

		// otherwise, assume it's a "basic" mapping
		return new ImplicitResultClassBuilder( resultMappingClass );
	}

	public static ImplicitFetchBuilder implicitFetchBuilder(
			NavigablePath fetchPath,
			Fetchable fetchable,
			DomainResultCreationState creationState) {
		final BasicValuedModelPart basicValuedFetchable = fetchable.asBasicValuedModelPart();
		if ( basicValuedFetchable != null ) {
			return new ImplicitFetchBuilderBasic( fetchPath, basicValuedFetchable, creationState );
		}

		if ( fetchable instanceof EmbeddableValuedFetchable ) {
			final EmbeddableValuedFetchable embeddableValuedFetchable = (EmbeddableValuedFetchable) fetchable;
			return new ImplicitFetchBuilderEmbeddable( fetchPath, embeddableValuedFetchable, creationState );
		}

		if ( fetchable instanceof ToOneAttributeMapping ) {
			final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) fetchable;
			return new ImplicitFetchBuilderEntity( fetchPath, toOneAttributeMapping, creationState );
		}

		if ( fetchable instanceof PluralAttributeMapping ) {
			return new ImplicitFetchBuilderPlural( fetchPath, (PluralAttributeMapping) fetchable, creationState );
		}

		if ( fetchable instanceof EntityCollectionPart ) {
			return new ImplicitFetchBuilderEntityPart( fetchPath, (EntityCollectionPart) fetchable );
		}

		if ( fetchable instanceof DiscriminatedAssociationAttributeMapping ) {
			return new ImplicitFetchBuilderDiscriminatedAssociation(
					fetchPath,
					(DiscriminatedAssociationAttributeMapping) fetchable,
					creationState
			);
		}


		throw new UnsupportedOperationException();
	}
}
