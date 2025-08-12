/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.Discriminatable;
import org.hibernate.boot.jaxb.hbm.spi.EntityInfo;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyAssociationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAuxiliaryDatabaseObjectType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCacheInclusionEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmClassRenameType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmConfigParameterContainer;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmConfigParameterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCustomSqlDmlType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDiscriminatorSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDynamicComponentType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityBaseDefinition;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchStyleEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchStyleWithSubselectEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterAliasMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterParameterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmGeneratorSpecificationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIndexType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmJoinedSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLazyEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLazyWithExtraEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLazyWithNoProxyEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListIndexType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToAnyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapKeyBasicType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryPropertyReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNotFoundEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOnDeleteEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOuterJoinEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPrimitiveArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPropertiesType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmQueryParamType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmResultSetMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSecondaryTableType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSimpleIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSynchronizeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTimestampAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTypeDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTypeSpecificationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmUnionSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmVersionAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.PluralAttributeInfo;
import org.hibernate.boot.jaxb.hbm.spi.ResultSetMappingContainer;
import org.hibernate.boot.jaxb.hbm.spi.ToolingHintContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyDiscriminatorValueMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingDiscriminatorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCachingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCascadeTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCheckConstraintImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConfigurationParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConvertImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCustomSqlImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDatabaseObjectImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDatabaseObjectScopeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDiscriminatorColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDiscriminatorFormulaImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableAttributesContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmptyTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFetchProfileImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFieldResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFilterDefImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFilterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbForeignKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGeneratedValueImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGenericIdGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbHqlImportImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbInheritanceImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedHqlQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNaturalIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOrderColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralFetchModeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPrimaryKeyJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPropertyRefImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbQueryParamTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSecondaryTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSingularAssociationAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSingularFetchModeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSqlResultSetMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSynchronizedTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTransientImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbVersionImpl;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbTableMapping;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.type.BasicType;
import org.hibernate.type.ConvertedBasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.internal.BasicTypeImpl;

import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import static org.hibernate.boot.jaxb.hbm.transform.HbmTransformationLogging.TRANSFORMATION_LOGGER;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;

/**
 * Transforms {@code hbm.xml} {@linkplain JaxbHbmHibernateMapping JAXB} bindings into
 * {@code mapping.xml} {@linkplain JaxbEntityMappingsImpl JAXB} bindings
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class HbmXmlTransformer {
	/**
	 * Transforms a list of {@code hbm.xml} JAXB bindings into a list of {@code mapping.xml} JAXB bindings
	 *
	 * @param hbmXmlBindings The list of {@code hbm.xml} JAXB bindings
	 * @param unsupportedFeatureHandling How to handle {@code hbm.xml} features we don't transform
	 *
	 * @return The list of {@code mapping.xml} JAXB bindings
	 */
	public static List<Binding<JaxbEntityMappingsImpl>> transform(
			List<Binding<JaxbHbmHibernateMapping>> hbmXmlBindings,
			MetadataImplementor bootModel,
			UnsupportedFeatureHandling unsupportedFeatureHandling) {
		// perform a first pass over the hbm.xml bindings building much of the transformation-state
		final var transformationState = new TransformationState();
		final var transformations = XmlPreprocessor.preprocessHbmXml( hbmXmlBindings, transformationState );

		// build and perform a pass over the boot model building the rest of the transformation-state
		BootModelPreprocessor.preprocessBooModel( bootModel, transformationState );

		// now we are ready to fully build the mapping.xml transformations
		for ( int i = 0; i < hbmXmlBindings.size(); i++ ) {
			final var hbmXmlTransformer = new HbmXmlTransformer(
					hbmXmlBindings.get( i ),
					transformations.get( i ),
					transformationState,
					unsupportedFeatureHandling
			);

			hbmXmlTransformer.performTransformation();
		}

		return transformations;
	}


	private final Binding<JaxbHbmHibernateMapping> hbmXmlBinding;
	private final Binding<JaxbEntityMappingsImpl> mappingXmlBinding;

	private final TransformationState transformationState;

	private final UnsupportedFeatureHandling unsupportedFeatureHandling;

	// todo (7.0) : use transformation-state instead
	private final Map<String,JaxbEmbeddableImpl> jaxbEmbeddableByClassName = new HashMap<>();

	private Table currentBaseTable;

	private HbmXmlTransformer(
			Binding<JaxbHbmHibernateMapping> hbmXmlBinding,
			Binding<JaxbEntityMappingsImpl> mappingXmlBinding,
			TransformationState transformationState,
			UnsupportedFeatureHandling unsupportedFeatureHandling) {
		this.hbmXmlBinding = hbmXmlBinding;
		this.mappingXmlBinding = mappingXmlBinding;
		this.transformationState = transformationState;
		this.unsupportedFeatureHandling = unsupportedFeatureHandling;
	}


	private void performTransformation() {
		final var hbmXmlRoot = hbmXmlBinding.getRoot();
		final var mappingXmlRoot = mappingXmlBinding.getRoot();

		TransformationHelper.transfer( hbmXmlRoot::getPackage, mappingXmlRoot::setPackage );
		TransformationHelper.transfer( hbmXmlRoot::getCatalog, mappingXmlRoot::setCatalog );
		TransformationHelper.transfer( hbmXmlRoot::getSchema, mappingXmlRoot::setSchema );
		TransformationHelper.transfer( hbmXmlRoot::getDefaultAccess, mappingXmlRoot::setAttributeAccessor );
		TransformationHelper.transfer( hbmXmlRoot::getDefaultCascade, mappingXmlRoot::setDefaultCascade );
		TransformationHelper.transfer( hbmXmlRoot::isDefaultLazy, mappingXmlRoot::setDefaultLazy );

		transferFilterDefinitions();
		transferImports();
		transferResultSetMappings();
		transferNamedQueries();
		transferNamedNativeQueries();
		transferFetchProfiles();
		transferDatabaseObjects();

		hbmXmlRoot.getClazz().forEach( (hbmEntity) -> {
			final String entityName = TransformationHelper.determineEntityName( hbmEntity, hbmXmlRoot );
			final var mappingEntity = transformationState.getMappingEntityByName().get( entityName );
			final var bootEntityInfo = transformationState.getEntityInfoByName().get( entityName );
			assert mappingEntity != null : "Unable to locate JaxbEntityImpl for " + entityName;
			assert bootEntityInfo != null : "Unable to locate EntityTypeInfo for " + entityName;

			transferRootEntity( hbmEntity, mappingEntity, bootEntityInfo );
		} );

		hbmXmlRoot.getSubclass().forEach( (hbmSubclass) -> {
			final String entityName = TransformationHelper.determineEntityName( hbmSubclass, hbmXmlRoot );
			final var mappingEntity = transformationState.getMappingEntityByName().get( entityName );
			final var bootEntityInfo = transformationState.getEntityInfoByName().get( entityName );
			assert mappingEntity != null : "Unable to locate JaxbEntityImpl for " + entityName;
			assert bootEntityInfo != null : "Unable to locate EntityTypeInfo for " + entityName;

			transferDiscriminatorSubclass( hbmSubclass, mappingEntity, bootEntityInfo );

			final String rootEntityName = bootEntityInfo.getPersistentClass().getRootClass().getEntityName();
			final var rootMappingEntity = transformationState.getMappingEntityByName().get( rootEntityName );
			defineInheritance( rootMappingEntity, InheritanceType.SINGLE_TABLE );
		} );

		hbmXmlRoot.getJoinedSubclass().forEach( (hbmSubclass) -> {
			final String entityName = TransformationHelper.determineEntityName( hbmSubclass, hbmXmlRoot );
			final var mappingEntity = transformationState.getMappingEntityByName().get( entityName );
			final var bootEntityInfo = transformationState.getEntityInfoByName().get( entityName );
			assert mappingEntity != null : "Unable to locate JaxbEntityImpl for " + entityName;
			assert bootEntityInfo != null : "Unable to locate EntityTypeInfo for " + entityName;

			transferJoinedSubclass( hbmSubclass, mappingEntity, bootEntityInfo );

			final String rootEntityName = bootEntityInfo.getPersistentClass().getRootClass().getEntityName();
			final var rootMappingEntity = transformationState.getMappingEntityByName().get( rootEntityName );
			defineInheritance( rootMappingEntity, InheritanceType.JOINED );
		} );

		hbmXmlRoot.getUnionSubclass().forEach( (hbmSubclass) -> {
			final String entityName = TransformationHelper.determineEntityName( hbmSubclass, hbmXmlRoot );
			final var mappingEntity = transformationState.getMappingEntityByName().get( entityName );
			final var bootEntityInfo = transformationState.getEntityInfoByName().get( entityName );
			assert mappingEntity != null : "Unable to locate JaxbEntityImpl for " + entityName;
			assert bootEntityInfo != null : "Unable to locate EntityTypeInfo for " + entityName;

			transferUnionSubclass( hbmSubclass, mappingEntity, bootEntityInfo );

			final String rootEntityName = bootEntityInfo.getPersistentClass().getRootClass().getEntityName();
			final var rootMappingEntity = transformationState.getMappingEntityByName().get( rootEntityName );
			defineInheritance( rootMappingEntity, InheritanceType.TABLE_PER_CLASS );
		} );

		if ( TRANSFORMATION_LOGGER.isDebugEnabled() ) {
			dumpTransformed( origin(), mappingXmlRoot );
		}
	}

	private static void dumpTransformed(Origin origin, JaxbEntityMappingsImpl ormRoot) {
		try {
			var ctx = JAXBContext.newInstance( JaxbEntityMappingsImpl.class );
			var marshaller = ctx.createMarshaller();
			marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
			final var stringWriter = new StringWriter();
			marshaller.marshal( ormRoot, stringWriter );
			TRANSFORMATION_LOGGER.debugf( "Transformed hbm.xml (%s):\n%s", origin, stringWriter.toString() );
		}
		catch (JAXBException e) {
			throw new RuntimeException( e );
		}
	}

	private void transferRootEntity(
			JaxbHbmRootEntityType hbmClass,
			JaxbEntityImpl mappingEntity,
			EntityTypeInfo entityInfo) {
		currentBaseTable = entityInfo.table();

		transferBaseEntityInformation( hbmClass, mappingEntity, entityInfo );

		mappingEntity.setMutable( hbmClass.isMutable() );

		applyTable( entityInfo.getPersistentClass(), mappingEntity );

		for ( var hbmSync : hbmClass.getSynchronize() ) {
			final var sync = new JaxbSynchronizedTableImpl();
			sync.setTable( hbmSync.getTable() );
			mappingEntity.getSynchronizeTables().add( sync );
		}

		if ( hbmClass.getLoader() != null ) {
			handleUnsupported( "<loader/> is not supported in mapping.xsd - use <sql-select/> or <hql-select/> instead" );
		}

		if ( !hbmClass.getTuplizer().isEmpty() ) {
			handleUnsupported( "<tuplizer/> is not supported" );
		}

		if ( hbmClass.getSqlInsert() != null ) {
			mappingEntity.setSqlInsert( new JaxbCustomSqlImpl() );
			mappingEntity.getSqlInsert().setValue( hbmClass.getSqlInsert().getValue() );
			mappingEntity.getSqlInsert().setResultCheck( hbmClass.getSqlInsert().getCheck() );
			mappingEntity.getSqlInsert().setValue( hbmClass.getSqlInsert().getValue() );
		}
		if ( hbmClass.getSqlUpdate() != null ) {
			mappingEntity.setSqlUpdate( new JaxbCustomSqlImpl() );
			mappingEntity.getSqlUpdate().setValue( hbmClass.getSqlUpdate().getValue() );
			mappingEntity.getSqlUpdate().setResultCheck( hbmClass.getSqlUpdate().getCheck() );
			mappingEntity.getSqlUpdate().setValue( hbmClass.getSqlUpdate().getValue() );
		}
		if ( hbmClass.getSqlDelete() != null ) {
			mappingEntity.setSqlDelete( new JaxbCustomSqlImpl() );
			mappingEntity.getSqlDelete().setValue( hbmClass.getSqlDelete().getValue() );
			mappingEntity.getSqlDelete().setResultCheck( hbmClass.getSqlDelete().getCheck() );
			mappingEntity.getSqlDelete().setValue( hbmClass.getSqlDelete().getValue() );
		}
		mappingEntity.setRowid( hbmClass.getRowid() );
		mappingEntity.setSqlRestriction( hbmClass.getWhere() );

		mappingEntity.setOptimisticLocking( hbmClass.getOptimisticLock() );

		mappingEntity.setDiscriminatorValue( hbmClass.getDiscriminatorValue() );

		transferDiscriminator( hbmClass, mappingEntity, entityInfo );
		transferEntityAttributes( hbmClass, mappingEntity, entityInfo );

		if ( hbmClass.getCache() != null ) {
			transformEntityCaching( hbmClass, mappingEntity );
		}

		for ( var hbmQuery : hbmClass.getQuery() ) {
			final String name = mappingEntity.getName() + "." + hbmQuery.getName();
			mappingEntity.getNamedQueries().add( transformNamedQuery( hbmQuery, name ) );
		}

		for ( var hbmQuery : hbmClass.getSqlQuery() ) {
			final String name = mappingEntity.getName() + "." + hbmQuery.getName();
			mappingEntity.getNamedNativeQueries().add( transformNamedNativeQuery( hbmQuery, name ) );
		}

		final var filters = mappingEntity.getFilters();
		for ( var hbmFilter : hbmClass.getFilter()) {
			filters.add( convert( hbmFilter ) );
		}

		final var fetchProfiles = mappingEntity.getFetchProfiles();
		for ( var hbmFetchProfile : hbmClass.getFetchProfile() ) {
			fetchProfiles.add( transferFetchProfile( hbmFetchProfile ) );
		}

		for ( var hbmSubclass : hbmClass.getSubclass() ) {
			final String subclassEntityName = TransformationHelper.determineEntityName( hbmSubclass, hbmXmlBinding.getRoot() );
			final var mappingSubclassEntity = transformationState.getMappingEntityByName().get( subclassEntityName );
			final var subclassEntityInfo = transformationState.getEntityInfoByName().get( subclassEntityName );
			transferDiscriminatorSubclass( hbmSubclass, mappingSubclassEntity, subclassEntityInfo );
			defineInheritance( mappingEntity, InheritanceType.SINGLE_TABLE );
		}

		for ( var hbmSubclass : hbmClass.getJoinedSubclass() ) {
			final String subclassEntityName = TransformationHelper.determineEntityName( hbmSubclass, hbmXmlBinding.getRoot() );
			final var mappingSubclassEntity = transformationState.getMappingEntityByName().get( subclassEntityName );
			final var subclassEntityInfo = transformationState.getEntityInfoByName().get( subclassEntityName );
			transferJoinedSubclass( hbmSubclass, mappingSubclassEntity, subclassEntityInfo );
			defineInheritance( mappingEntity, InheritanceType.TABLE_PER_CLASS );
		}

		for ( var hbmSubclass : hbmClass.getUnionSubclass() ) {
			final String subclassEntityName = TransformationHelper.determineEntityName( hbmSubclass, hbmXmlBinding.getRoot() );
			final var mappingSubclassEntity = transformationState.getMappingEntityByName().get( subclassEntityName );
			final var subclassEntityInfo = transformationState.getEntityInfoByName().get( subclassEntityName );
			transferUnionSubclass( hbmSubclass, mappingSubclassEntity, subclassEntityInfo );
			defineInheritance( mappingEntity, InheritanceType.JOINED );
		}

		for ( var hbmQuery : hbmClass.getQuery() ) {
			// Tests implied this was the case...
			final String name = hbmClass.getName() + "." + hbmQuery.getName();
			mappingXmlBinding.getRoot().getNamedQueries().add( transformNamedQuery( hbmQuery, name ) );
		}

		for ( var hbmQuery : hbmClass.getSqlQuery() ) {
			// Tests implied this was the case...
			final String name = hbmClass.getName() + "." + hbmQuery.getName();
			mappingXmlBinding.getRoot().getNamedNativeQueries().add( transformNamedNativeQuery( hbmQuery, name ) );
		}
	}

	private void defineInheritance(JaxbEntityImpl mappingEntity, InheritanceType strategy) {
		if ( mappingEntity.getInheritance() != null && mappingEntity.getInheritance().getStrategy() != strategy ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"Attempt to use multiple inheritance strategies : %s & %s",
							mappingEntity.getInheritance().getStrategy(),
							strategy
					),
					origin()
			);
		}
		else {
			mappingEntity.setInheritance( new JaxbInheritanceImpl() );
			mappingEntity.getInheritance().setStrategy( strategy );
		}
	}

	private void transferDiscriminatorSubclass(
			JaxbHbmDiscriminatorSubclassEntityType hbmSubclass,
			JaxbEntityImpl subclassEntity,
			EntityTypeInfo subclassEntityInfo) {
		transferBaseEntityInformation( hbmSubclass, subclassEntity, subclassEntityInfo );
		transferBaseEntityAttributes( hbmSubclass, subclassEntity, subclassEntityInfo );

		if ( !hbmSubclass.getSubclass().isEmpty() ) {
			for ( var nestedHbmSubclass : hbmSubclass.getSubclass() ) {
				final String nestedSubclassEntityName = TransformationHelper.determineEntityName( nestedHbmSubclass, hbmXmlBinding.getRoot() );
				final var nestedSubclassSubclassEntity = transformationState.getMappingEntityByName().get( nestedSubclassEntityName );
				final var nestedSubclassInfo = transformationState.getEntityInfoByName().get( nestedSubclassEntityName );
				transferDiscriminatorSubclass( nestedHbmSubclass, nestedSubclassSubclassEntity, nestedSubclassInfo );
			}
		}
	}

	private void transferJoinedSubclass(
			JaxbHbmJoinedSubclassEntityType hbmSubclass,
			JaxbEntityImpl subclassEntity,
			EntityTypeInfo subclassEntityInfo) {
		currentBaseTable = subclassEntityInfo.table();

		transferBaseEntityInformation( hbmSubclass, subclassEntity, subclassEntityInfo );
		transferEntityAttributes( hbmSubclass, subclassEntity, subclassEntityInfo );

		applyTable( subclassEntityInfo.getPersistentClass(), subclassEntity );

		final var key = hbmSubclass.getKey();
		if ( key != null ) {
			final var joinColumn = new JaxbPrimaryKeyJoinColumnImpl();
			// todo (7.0) : formula and multiple columns
			joinColumn.setName( key.getColumnAttribute() );
			subclassEntity.getPrimaryKeyJoinColumns().add( joinColumn );
			joinColumn.setForeignKey( transformForeignKey( key.getForeignKey() ) );
		}

		if ( !hbmSubclass.getJoinedSubclass().isEmpty() ) {
			for ( var nestedHbmSubclass : hbmSubclass.getJoinedSubclass() ) {
				final String nestedSubclassEntityName = TransformationHelper.determineEntityName( nestedHbmSubclass, hbmXmlBinding.getRoot() );
				final var nestedSubclassSubclassEntity = transformationState.getMappingEntityByName().get( nestedSubclassEntityName );
				final var nestedSubclassInfo = transformationState.getEntityInfoByName().get( nestedSubclassEntityName );
				transferJoinedSubclass( nestedHbmSubclass, nestedSubclassSubclassEntity, nestedSubclassInfo );
			}
		}
	}

	private void transferUnionSubclass(
			JaxbHbmUnionSubclassEntityType hbmSubclass,
			JaxbEntityImpl subclassEntity,
			EntityTypeInfo subclassEntityInfo) {
		currentBaseTable = subclassEntityInfo.table();

		subclassEntity.setProxy( hbmSubclass.getProxy() );
		transferBaseEntityInformation( hbmSubclass, subclassEntity, subclassEntityInfo );
		transferEntityAttributes( hbmSubclass, subclassEntity, subclassEntityInfo );

		applyTable( subclassEntityInfo.getPersistentClass(), subclassEntity );

		if ( !hbmSubclass.getUnionSubclass().isEmpty() ) {
			for ( var nestedHbmSubclass : hbmSubclass.getUnionSubclass() ) {
				final String nestedSubclassEntityName = TransformationHelper.determineEntityName( nestedHbmSubclass, hbmXmlBinding.getRoot() );
				final var nestedSubclassSubclassEntity = transformationState.getMappingEntityByName().get( nestedSubclassEntityName );
				final var nestedSubclassInfo = transformationState.getEntityInfoByName().get( nestedSubclassEntityName );
				transferUnionSubclass( nestedHbmSubclass, nestedSubclassSubclassEntity, nestedSubclassInfo );
			}
		}
	}

	private void transferBaseEntityInformation(
			JaxbHbmEntityBaseDefinition hbmEntity,
			JaxbEntityImpl mappingEntity,
			EntityTypeInfo bootEntityInfo) {
		mappingEntity.setMetadataComplete( true );

		final var persistentClass = bootEntityInfo.getPersistentClass();
		if ( persistentClass.getSuperclass() != null ) {
			mappingEntity.setExtends( persistentClass.getSuperclass().getEntityName() );
		}

		if ( hbmEntity instanceof Discriminatable discriminatable ) {
			TransformationHelper.transfer( discriminatable::getDiscriminatorValue, mappingEntity::setDiscriminatorValue );
		}

		if ( hbmEntity.isAbstract() != null ) {
			// todo : handle hbm abstract as mapping abstract or as mapped-superclass?
			mappingEntity.setAbstract( hbmEntity.isAbstract() );
		}

		if ( hbmEntity.getPersister() != null ) {
			handleUnsupported( "<persister/> mappings are not supported" );
			return;
		}

		TransformationHelper.transfer( hbmEntity::isLazy, mappingEntity::setLazy );
		TransformationHelper.transfer( hbmEntity::getProxy, mappingEntity::setProxy );

		TransformationHelper.transfer( hbmEntity::getBatchSize, mappingEntity::setBatchSize );

		TransformationHelper.transfer( hbmEntity::isDynamicInsert, mappingEntity::setDynamicInsert );
		TransformationHelper.transfer( hbmEntity::isDynamicUpdate, mappingEntity::setDynamicUpdate );
		TransformationHelper.transfer( hbmEntity::isSelectBeforeUpdate, mappingEntity::setSelectBeforeUpdate );

		transferToolingHints( hbmEntity );
		transferResultSetMappings( mappingEntity.getName(), hbmEntity );
	}

	private void applyBasicTypeMapping(
			BasicValue basicValue,
			JaxbBasicMapping jaxbBasicMapping,
			String hbmTypeAttribute,
			JaxbHbmTypeSpecificationType hbmType,
			Consumer<EnumType> enumTypeConsumer,
			Consumer<BasicValueConverter<?,?>> converterConsumer) {
		final BasicType<?> type = (BasicType<?>) basicValue.getType();
		if ( type instanceof BasicTypeImpl<?> standardBasicType ) {
			if ( enumTypeConsumer != null && type.getReturnedClass().isEnum() ) {
				enumTypeConsumer.accept( standardBasicType.getJdbcType().isString() ? EnumType.STRING : EnumType.ORDINAL );
			}
			else {
				jaxbBasicMapping.setJavaType( standardBasicType.getMappedJavaType().getClass().getName() );
				jaxbBasicMapping.setJdbcType( standardBasicType.getJdbcType().getClass().getName() );
			}
		}
		else if ( type instanceof CustomType<?> ) {
			if ( isNotEmpty( hbmTypeAttribute ) ) {
				jaxbBasicMapping.setType( interpretBasicType(
						hbmTypeAttribute,
						null,
						transformationState.getTypeDefMap().get( hbmTypeAttribute )
				) );
			}

			if ( hbmType != null ) {
				jaxbBasicMapping.setType( interpretBasicType(
						hbmType.getName(),
						hbmType,
						transformationState.getTypeDefMap().get( hbmType.getName() )
				) );
			}
		}
		else if ( type instanceof ConvertedBasicType<?> convertedType ) {
			if ( converterConsumer == null ) {
				throw new AssertionFailure( "Unexpected context for converted value" );
			}
			jaxbBasicMapping.setJavaType( convertedType.getMappedJavaType().getClass().getName() );
			jaxbBasicMapping.setJdbcTypeCode( convertedType.getJdbcType().getJdbcTypeCode() );
			converterConsumer.accept( convertedType.getValueConverter() );
		}
	}



	private static void applyTable(PersistentClass bootBinding, JaxbEntityImpl jaxbEntity) {
		final Table table = bootBinding.getTable();
		if ( table.isSubselect() ) {
			jaxbEntity.setTableExpression( table.getSubselect() );
		}
		else if ( table.isView() ) {
			jaxbEntity.setTableExpression( table.getViewQuery() );
		}
		else {
			final var jaxbTable = new JaxbTableImpl();
			jaxbEntity.setTable( jaxbTable );
			jaxbTable.setName( table.getName() );
			jaxbTable.setComment( table.getComment() );
			transferBaseTableInfo( table, jaxbTable );
		}
	}

	private static void transferBaseTableInfo(Table table, JaxbTableMapping jaxbTableMapping) {
		jaxbTableMapping.setCatalog( table.getCatalog() );
		jaxbTableMapping.setSchema( table.getSchema() );

		for ( var check : table.getChecks() ) {
			final var jaxbCheckConstraint = new JaxbCheckConstraintImpl();
			jaxbTableMapping.getCheckConstraints().add( jaxbCheckConstraint );
			jaxbCheckConstraint.setName( check.getName() );
			jaxbCheckConstraint.setConstraint( check.getConstraint() );
			jaxbCheckConstraint.setOptions( check.getOptions() );
		}

	}



















	@SuppressWarnings("unchecked")
	private void transferFilterDefinitions() {
		final List<JaxbHbmFilterDefinitionType> filterDefs = hbmXmlBinding.getRoot().getFilterDef();
		final JaxbEntityMappingsImpl ormRoot = mappingXmlBinding.getRoot();

		if ( filterDefs.isEmpty() ) {
			return;
		}

		for ( JaxbHbmFilterDefinitionType hbmFilterDef : filterDefs ) {
			final JaxbFilterDefImpl filterDef = new JaxbFilterDefImpl();
			ormRoot.getFilterDefinitions().add( filterDef );
			filterDef.setName( hbmFilterDef.getName() );

			boolean foundCondition = false;
			for ( Object content : hbmFilterDef.getContent() ) {
				if ( content instanceof String string ) {
					if ( !isBlank( string ) ) {
						foundCondition = true;
						filterDef.setDefaultCondition( string.trim() );
					}
				}
				else {
					final JaxbHbmFilterParameterType hbmFilterParam =
							( (JAXBElement<JaxbHbmFilterParameterType>) content ).getValue();
					final JaxbFilterDefImpl.JaxbFilterParamImpl param = new JaxbFilterDefImpl.JaxbFilterParamImpl();
					filterDef.getFilterParams().add( param );
					param.setName( hbmFilterParam.getParameterName() );
					param.setType( hbmFilterParam.getParameterValueTypeName() );
				}
			}

			if ( !foundCondition ) {
				filterDef.setDefaultCondition( hbmFilterDef.getCondition() );
			}
		}
	}

	private void transferImports() {
		final List<JaxbHbmClassRenameType> hbmImports = hbmXmlBinding.getRoot().getImport();
		if ( hbmImports.isEmpty() ) {
			return;
		}
		final JaxbEntityMappingsImpl ormRoot = mappingXmlBinding.getRoot();

		for ( JaxbHbmClassRenameType hbmImport : hbmImports ) {
			final JaxbHqlImportImpl ormImport = new JaxbHqlImportImpl();
			ormRoot.getHqlImports().add( ormImport );
			ormImport.setClazz( hbmImport.getClazz() );
			ormImport.setRename( hbmImport.getRename() );
		}
	}

	private void transferResultSetMappings() {
		final List<JaxbHbmResultSetMappingType> hbmResultMappings = hbmXmlBinding.getRoot().getResultset();
		if ( hbmResultMappings.isEmpty() ) {
			return;
		}

		final JaxbEntityMappingsImpl ormRoot = mappingXmlBinding.getRoot();

		for ( JaxbHbmResultSetMappingType hbmResultSet : hbmResultMappings ) {
			final JaxbSqlResultSetMappingImpl mapping = transformResultSetMapping( null, hbmResultSet );
			ormRoot.getSqlResultSetMappings().add( mapping );
		}
	}

	private JaxbSqlResultSetMappingImpl transformResultSetMapping(
			String namePrefix,
			JaxbHbmResultSetMappingType hbmResultSet) {
		final String resultMappingName = namePrefix == null
				? hbmResultSet.getName()
				: namePrefix + "." + hbmResultSet.getName();

		final JaxbSqlResultSetMappingImpl mapping = new JaxbSqlResultSetMappingImpl();
		mapping.setName( resultMappingName );
		mapping.setDescription( "SQL ResultSet mapping - " + resultMappingName );

		for ( Serializable hbmReturn : hbmResultSet.getValueMappingSources() ) {
			if ( hbmReturn instanceof JaxbHbmNativeQueryReturnType returnType ) {
				mapping.getEntityResult()
						.add( transferEntityReturnElement( resultMappingName, returnType ) );
			}
			else if ( hbmReturn instanceof JaxbHbmNativeQueryScalarReturnType scalarReturnType) {
				mapping.getColumnResult()
						.add( transferScalarReturnElement( resultMappingName, scalarReturnType ) );
			}
			else if ( hbmReturn instanceof JaxbHbmNativeQueryJoinReturnType ) {
				handleUnsupportedContent(
						String.format(
								"SQL ResultSet mapping [name=%s] contained a <return-join/> element, " +
										"which is not supported for transformation",
								resultMappingName
						)
				);
			}
			else if ( hbmReturn instanceof JaxbHbmNativeQueryCollectionLoadReturnType ) {
				handleUnsupportedContent(
						String.format(
								"SQL ResultSet mapping [name=%s] contained a <collection-load/> element, " +
										"which is not supported for transformation",
								resultMappingName
						)
				);
			}
			else {
				// should never happen thanks to XSD
				handleUnsupportedContent(
						String.format(
								"SQL ResultSet mapping [name=%s] contained an unexpected element type",
								resultMappingName
						)
				);
			}
		}
		return mapping;
	}

	private JaxbEntityResultImpl transferEntityReturnElement(
			String resultMappingName,
			JaxbHbmNativeQueryReturnType hbmReturn) {
		final JaxbEntityResultImpl entityResult = new JaxbEntityResultImpl();
		entityResult.setEntityClass( getFullyQualifiedClassName( hbmReturn.getClazz() ) );

		for ( JaxbHbmNativeQueryPropertyReturnType propertyReturn : hbmReturn.getReturnProperty() ) {
			final JaxbFieldResultImpl field = new JaxbFieldResultImpl();
			final List<String> columns = new ArrayList<>();
			if ( !isEmpty( propertyReturn.getColumn() ) ) {
				columns.add( propertyReturn.getColumn() );
			}

			for ( JaxbHbmNativeQueryPropertyReturnType.JaxbHbmReturnColumn returnColumn : propertyReturn.getReturnColumn() ) {
				columns.add( returnColumn.getName() );
			}

			if ( columns.size() > 1 ) {
				handleUnsupportedContent(
						String.format(
								"SQL ResultSet mapping [name=%s] contained a <return-property name='%s'/> element " +
										"declaring multiple 1 column mapping, which is not supported for transformation;" +
										"skipping that return-property mapping",
								resultMappingName,
								propertyReturn.getName()
						)
				);
				continue;
			}

			field.setColumn( columns.get( 0 ) );
			field.setName( propertyReturn.getName() );
			entityResult.getFieldResult().add( field );
		}
		return entityResult;
	}

	private JaxbColumnResultImpl transferScalarReturnElement(
			String resultMappingName,
			JaxbHbmNativeQueryScalarReturnType hbmReturn) {
		final JaxbColumnResultImpl columnResult = new JaxbColumnResultImpl();
		columnResult.setName( hbmReturn.getColumn() );
		columnResult.setClazz( hbmReturn.getType() );
		handleUnsupportedContent(
				String.format(
						"SQL ResultSet mapping [name=%s] contained a <return-scalar column='%s'/> element; " +
								"transforming type->class likely requires manual adjustment",
						resultMappingName,
						hbmReturn.getColumn()
				)
		);
		return columnResult;
	}

	private void transferFetchProfiles() {
		final List<JaxbHbmFetchProfileType> hbmFetchProfiles = hbmXmlBinding.getRoot().getFetchProfile();
		if ( hbmFetchProfiles.isEmpty() ) {
			return;
		}

		for ( JaxbHbmFetchProfileType hbmFetchProfile : hbmFetchProfiles ) {
			mappingXmlBinding.getRoot().getFetchProfiles().add( transferFetchProfile( hbmFetchProfile ) );
		}
	}

	private static JaxbFetchProfileImpl transferFetchProfile(JaxbHbmFetchProfileType hbmFetchProfile) {
		final JaxbFetchProfileImpl fetchProfile = new JaxbFetchProfileImpl();
		fetchProfile.setName( hbmFetchProfile.getName() );
		for ( JaxbHbmFetchProfileType.JaxbHbmFetch hbmFetch : hbmFetchProfile.getFetch() ) {
			final JaxbFetchProfileImpl.JaxbFetchImpl fetch = new JaxbFetchProfileImpl.JaxbFetchImpl();
			fetchProfile.getFetch().add( fetch );
			fetch.setEntity( hbmFetch.getEntity() );
			fetch.setAssociation( hbmFetch.getAssociation() );
			fetch.setStyle( hbmFetch.getStyle().value() );
		}
		return fetchProfile;
	}

	private void transferNamedQueries() {
		final List<JaxbHbmNamedQueryType> hbmHqlQueries = hbmXmlBinding.getRoot().getQuery();
		if ( hbmHqlQueries.isEmpty() ) {
			return;
		}

		for ( JaxbHbmNamedQueryType hbmHqlQuery : hbmHqlQueries ) {
			mappingXmlBinding.getRoot().getNamedQueries().add( transformNamedQuery( hbmHqlQuery, hbmHqlQuery.getName() ) );
		}
	}

	private static JaxbNamedHqlQueryImpl transformNamedQuery(JaxbHbmNamedQueryType hbmQuery, String name) {
		final JaxbNamedHqlQueryImpl query = new JaxbNamedHqlQueryImpl();
		query.setName( name );
		query.setCacheable( hbmQuery.isCacheable() );
		query.setCacheMode( hbmQuery.getCacheMode() );
		query.setCacheRegion( hbmQuery.getCacheRegion() );
		query.setComment( hbmQuery.getComment() );
		query.setFetchSize( hbmQuery.getFetchSize() );
		query.setFlushMode( hbmQuery.getFlushMode() );
		query.setFetchSize( hbmQuery.getFetchSize() );
		query.setReadOnly( hbmQuery.isReadOnly() );
		query.setTimeout( hbmQuery.getTimeout() );

		for ( Object content : hbmQuery.getContent() ) {
			if ( content instanceof String qryString ) {
				qryString = qryString.trim();
				query.setQuery( qryString );
			}
			else {
				@SuppressWarnings("unchecked") final JAXBElement<JaxbHbmQueryParamType> element = (JAXBElement<JaxbHbmQueryParamType>) content;
				final JaxbHbmQueryParamType hbmQueryParam = element.getValue();
				final JaxbQueryParamTypeImpl queryParam = new JaxbQueryParamTypeImpl();
				query.getQueryParam().add( queryParam );
				queryParam.setName( hbmQueryParam.getName() );
				queryParam.setType( hbmQueryParam.getType() );
			}
		}

		return query;
	}

	private void transferNamedNativeQueries() {
		final List<JaxbHbmNamedNativeQueryType> hbmNativeQueries = hbmXmlBinding.getRoot().getSqlQuery();
		if ( hbmNativeQueries.isEmpty() ) {
			return;
		}

		for ( JaxbHbmNamedNativeQueryType hbmQuery : hbmNativeQueries ) {
			mappingXmlBinding.getRoot().getNamedNativeQueries().add( transformNamedNativeQuery( hbmQuery, hbmQuery.getName() ) );
		}
	}

	private JaxbNamedNativeQueryImpl transformNamedNativeQuery(JaxbHbmNamedNativeQueryType hbmQuery, String queryName) {
		final String implicitResultSetMappingName = queryName + "-implicitResultSetMapping";

		final JaxbNamedNativeQueryImpl query = new JaxbNamedNativeQueryImpl();
		query.setName( queryName );
		query.setCacheable( hbmQuery.isCacheable() );
		query.setCacheMode( hbmQuery.getCacheMode() );
		query.setCacheRegion( hbmQuery.getCacheRegion() );
		query.setComment( hbmQuery.getComment() );
		query.setFetchSize( hbmQuery.getFetchSize() );
		query.setFlushMode( hbmQuery.getFlushMode() );
		query.setFetchSize( hbmQuery.getFetchSize() );
		query.setReadOnly( hbmQuery.isReadOnly() );
		query.setTimeout( hbmQuery.getTimeout() );

		JaxbSqlResultSetMappingImpl implicitResultSetMapping = null;

		// JaxbQueryElement#content elements can be either the query or parameters
		for ( Object content : hbmQuery.getContent() ) {
			if ( content instanceof String qryString ) {
				qryString = qryString.trim();
				query.setQuery( qryString );
			}
			else if ( content instanceof JAXBElement<?> contentElement ) {
				final Object element = contentElement.getValue();
				if ( element instanceof JaxbHbmQueryParamType hbmQueryParam ) {
					final JaxbQueryParamTypeImpl queryParam = new JaxbQueryParamTypeImpl();
					queryParam.setName( hbmQueryParam.getName() );
					queryParam.setType( hbmQueryParam.getType() );
					query.getQueryParam().add( queryParam );
				}
				else if ( element instanceof JaxbHbmNativeQueryScalarReturnType scalarReturnType ) {
					if ( implicitResultSetMapping == null ) {
						implicitResultSetMapping = new JaxbSqlResultSetMappingImpl();
						implicitResultSetMapping.setName( implicitResultSetMappingName );
						implicitResultSetMapping.setDescription(
								String.format(
										Locale.ROOT,
										"ResultSet mapping implicitly created for named native query `%s` during hbm.xml transformation",
										queryName
								)
						);
						mappingXmlBinding.getRoot().getSqlResultSetMappings().add( implicitResultSetMapping );
					}
					implicitResultSetMapping.getColumnResult()
							.add( transferScalarReturnElement( implicitResultSetMappingName, scalarReturnType ) );
				}
				else if ( element instanceof JaxbHbmNativeQueryReturnType returnType ) {
					if ( implicitResultSetMapping == null ) {
						implicitResultSetMapping = new JaxbSqlResultSetMappingImpl();
						implicitResultSetMapping.setName( implicitResultSetMappingName );
						implicitResultSetMapping.setDescription(
								String.format(
										Locale.ROOT,
										"ResultSet mapping implicitly created for named native query `%s` during hbm.xml transformation",
										queryName
								)
						);
						mappingXmlBinding.getRoot().getSqlResultSetMappings().add( implicitResultSetMapping );
					}
					implicitResultSetMapping.getEntityResult()
							.add( transferEntityReturnElement( implicitResultSetMappingName, returnType ) );
				}
				else if ( element instanceof JaxbHbmNativeQueryCollectionLoadReturnType ) {
					handleUnsupportedContent(
							String.format(
									"Named native query [name=%s] contained a <collection-load/> element, " +
											"which is not supported for transformation",
									queryName
							)
					);
				}
				else if ( element instanceof JaxbHbmNativeQueryJoinReturnType ) {
					handleUnsupportedContent(
							String.format(
									"Named native query [name=%s] contained a <return-join/> element, " +
											"which is not supported for transformation",
									queryName
							)
					);
				}
				else if ( element instanceof JaxbHbmSynchronizeType hbmSynchronize ) {
					final JaxbSynchronizedTableImpl synchronize = new JaxbSynchronizedTableImpl();
					synchronize.setTable( hbmSynchronize.getTable() );
					query.getSynchronizations().add( synchronize );
				}
				else {
					// should never happen thanks to XSD
					handleUnsupportedContent(
							String.format(
									"Named native query [name=%s] contained an unexpected element type",
									queryName
							)
					);
				}
			}
		}

		return query;
	}

	private void transferDatabaseObjects() {
		final List<JaxbHbmAuxiliaryDatabaseObjectType> hbmDatabaseObjects = hbmXmlBinding.getRoot().getDatabaseObject();
		if ( hbmDatabaseObjects.isEmpty() ) {
			return;
		}

		for ( JaxbHbmAuxiliaryDatabaseObjectType hbmDatabaseObject : hbmDatabaseObjects ) {
			// NOTE: database-object does not define a name nor a good "identifier" for logging (exportable)

			final JaxbDatabaseObjectImpl databaseObject = new JaxbDatabaseObjectImpl();
			mappingXmlBinding.getRoot().getDatabaseObjects().add( databaseObject );

			databaseObject.setCreate( hbmDatabaseObject.getCreate() );
			databaseObject.setDrop( hbmDatabaseObject.getDrop() );

			if ( ! hbmDatabaseObject.getDialectScope().isEmpty() ) {
				hbmDatabaseObject.getDialectScope().forEach( (hbmScope) -> {
					final JaxbDatabaseObjectScopeImpl scope = new JaxbDatabaseObjectScopeImpl();
					databaseObject.getDialectScopes().add( scope );

					scope.setName( hbmScope.getName() );
					// hbm.xml does not define min/max versions for its dialect-scope type
				} );
			}
		}
	}


	private void transformEntityCaching(JaxbHbmRootEntityType hbmClass, JaxbEntityImpl entity) {
		entity.setCaching( new JaxbCachingImpl() );
		entity.getCaching().setRegion( hbmClass.getCache().getRegion() );
		entity.getCaching().setAccess( hbmClass.getCache().getUsage() );
		entity.getCaching().setIncludeLazy( convert( hbmClass.getCache().getInclude() ) );
	}

	private boolean convert(JaxbHbmCacheInclusionEnum hbmInclusion) {
		if ( hbmInclusion == null ) {
			return true;
		}

		if ( hbmInclusion == JaxbHbmCacheInclusionEnum.NON_LAZY ) {
			return false;
		}

		if ( hbmInclusion == JaxbHbmCacheInclusionEnum.ALL ) {
			return true;
		}

		throw new IllegalArgumentException( "Unrecognized cache-inclusions value : " + hbmInclusion );
	}

	private void transferResultSetMappings(String namePrefix, ResultSetMappingContainer container) {
		final List<JaxbHbmResultSetMappingType> resultSetMappings = container.getResultset();
		resultSetMappings.forEach( (hbmMapping) -> {
					final JaxbSqlResultSetMappingImpl mapping = transformResultSetMapping( namePrefix, hbmMapping );
					mappingXmlBinding.getRoot().getSqlResultSetMappings().add( mapping );
		} );
	}

	private void transferToolingHints(ToolingHintContainer container) {
		if ( CollectionHelper.isNotEmpty( container.getToolingHints() ) ) {
			handleUnsupported(
					"Transformation of <meta/> (tooling hint) is not supported - `%s`",
					hbmXmlBinding.getOrigin()
			);
		}
	}

	private void transferColumnsAndFormulas(
			Value value,
			ColumnAndFormulaTarget target,
			ColumnDefaults columnDefaults,
			String table) {
		for ( int i = 0; i < value.getSelectables().size(); i++ ) {
			final Selectable selectable = value.getSelectables().get( i );
			if ( selectable instanceof Formula formula ) {
				target.addFormula( formula.getFormula() );
			}
			else if ( selectable instanceof Column column ) {
				final TargetColumnAdapter targetColumnAdapter = target.makeColumnAdapter( columnDefaults );
				targetColumnAdapter.setName( column.getQuotedName() );
				targetColumnAdapter.setTable( table );
				targetColumnAdapter.setLength( convertColumnLength( column.getLength() ) );
				targetColumnAdapter.setPrecision( column.getPrecision() );
				targetColumnAdapter.setScale( column.getScale() );
				target.addColumn( targetColumnAdapter );
			}
		}
	}

	private Integer convertColumnLength(Long length) {
		return length == null ? null : length.intValue();
	}

	private void transferColumnsAndFormulas(
			ColumnAndFormulaSource source,
			ColumnAndFormulaTarget target,
			ColumnDefaults columnDefaults,
			String tableName) {
		if ( tableName != null
				&& currentBaseTable != null
				&& currentBaseTable.isPhysicalTable()
				&& currentBaseTable.getName().equals( tableName ) ) {
			tableName = null;
		}

		if ( isNotEmpty( source.getFormulaAttribute() ) ) {
			target.addFormula( source.getFormulaAttribute() );
		}
		else if ( isNotEmpty( source.getColumnAttribute() ) ) {
			final TargetColumnAdapter column = target.makeColumnAdapter( columnDefaults );
			column.setName( source.getColumnAttribute() );
			column.setTable( tableName );
			target.addColumn( column );
		}
		else if ( !source.getColumnOrFormula().isEmpty() ) {
			for ( Serializable columnOrFormula : source.getColumnOrFormula() ) {
				if ( columnOrFormula instanceof String string ) {
					target.addFormula( string );
				}
				else {
					final JaxbHbmColumnType hbmColumn = (JaxbHbmColumnType) columnOrFormula;
					final TargetColumnAdapter column = target.makeColumnAdapter( columnDefaults );
					column.setTable( tableName );
					transferColumn( source.wrap( hbmColumn ), column );
					target.addColumn( column );
				}
			}
		}
		else if ( isNotEmpty( tableName ) ) {
			// this is the case of transforming a <join/> where the property did not specify columns or formula.
			// we need to create a column still to pass along the secondary table name
			final TargetColumnAdapter column = target.makeColumnAdapter( columnDefaults );
			column.setTable( tableName );
			target.addColumn( column );
		}
	}

	private void transferColumn(
			SourceColumnAdapter source,
			TargetColumnAdapter target) {
		target.setName( source.getName() );

		target.setNullable( invert( source.isNotNull() ) );
		target.setUnique( source.isUnique() );

		target.setLength( source.getLength() );
		target.setScale( source.getScale() );
		target.setPrecision( source.getPrecision() );

		target.setComment( source.getComment() );

		target.setCheck( source.getCheck() );
		target.setDefault( source.getDefault() );

		target.setColumnDefinition( source.getSqlType() );

		target.setRead( source.getRead() );
		target.setWrite( source.getWrite() );

	}

	private void transferColumn(
			Column source,
			TargetColumnAdapter target) {
		target.setName( source.getQuotedName() );

		target.setNullable( source.isNullable() );
		target.setUnique( source.isUnique() );

		if ( source.getLength() != null ) {
			target.setLength( source.getLength().intValue() );
		}
		target.setScale( source.getScale() );
		target.setPrecision( source.getPrecision() );

		target.setComment( source.getComment() );

		if ( source.hasCheckConstraint() ) {
			if ( source.getCheckConstraints().size() > 1 ) {
				handleUnsupported( "Cannot transform multiple column-level check-constraints : " + source.getName() );
			}
			target.setCheck( source.getCheckConstraints().get( 0 ).getConstraint() );
		}
		target.setDefault( source.getDefaultValue() );

		target.setColumnDefinition( source.getSqlType() );

		target.setRead( source.getCustomRead() );
		target.setWrite( source.getCustomWrite() );

	}

	private void transferDiscriminator(
			JaxbHbmRootEntityType hbmClass,
			JaxbEntityImpl mappingEntity,
			EntityTypeInfo bootEntityInfo) {
		if ( hbmClass.getDiscriminator() == null ) {
			return;
		}

		final Value discriminatorValue = bootEntityInfo.getPersistentClass().getDiscriminator();
		assert discriminatorValue.getSelectables().size() == 1;

		final boolean forceDiscriminator = bootEntityInfo.getPersistentClass().isForceDiscriminator();
		final DiscriminatorType discriminatorType = determineDiscriminatorType( discriminatorValue );
		if ( discriminatorValue.hasFormula() ) {
			final JaxbDiscriminatorFormulaImpl jaxbFormula = new JaxbDiscriminatorFormulaImpl();
			mappingEntity.setDiscriminatorFormula( jaxbFormula );
			jaxbFormula.setFragment( ( (Formula) discriminatorValue.getSelectables().get( 0 ) ).getFormula() );
			jaxbFormula.setDiscriminatorType( discriminatorType );
			jaxbFormula.setForceSelection( forceDiscriminator );
		}
		else {
			assert discriminatorValue.getColumns().size() == 1;
			final Column column = discriminatorValue.getColumns().get( 0 );
			final JaxbDiscriminatorColumnImpl jaxbColumn = new JaxbDiscriminatorColumnImpl();
			mappingEntity.setDiscriminatorColumn( jaxbColumn );
			jaxbColumn.setName( column.getName() );
			jaxbColumn.setDiscriminatorType( discriminatorType );
			jaxbColumn.setForceSelection( forceDiscriminator );
			jaxbColumn.setOptions( column.getOptions() );
			if ( column.getLength() != null ) {
				jaxbColumn.setLength( column.getLength().intValue() );
			}
		}
	}

	private static DiscriminatorType determineDiscriminatorType(Value discriminatorBinding) {
		final Class<?> returnedClass = discriminatorBinding.getType().getReturnedClass();
		if ( Character.class.equals( returnedClass )
				|| char.class.equals( returnedClass ) ) {
			return DiscriminatorType.CHAR;
		}
		if ( Integer.class.equals( returnedClass )
				|| int.class.equals( returnedClass ) ) {
			return DiscriminatorType.INTEGER;
		}

		return DiscriminatorType.STRING;
	}

	private void transferEntityAttributes(
			JaxbHbmEntityBaseDefinition hbmEntity,
			JaxbEntityImpl mappingEntity,
			EntityTypeInfo bootEntityInfo) {
		transferBaseEntityAttributes( hbmEntity, mappingEntity, bootEntityInfo );

		if ( bootEntityInfo.getPersistentClass() instanceof RootClass rootClass ) {
			assert hbmEntity instanceof JaxbHbmRootEntityType;

			transferIdentifier( (JaxbHbmRootEntityType) hbmEntity, mappingEntity, bootEntityInfo, rootClass );
			transferNaturalIdentifiers( (JaxbHbmRootEntityType) hbmEntity, mappingEntity, bootEntityInfo, rootClass );
			transferVersion( (JaxbHbmRootEntityType) hbmEntity, mappingEntity, bootEntityInfo, rootClass );

			transferJoins( (JaxbHbmRootEntityType) hbmEntity, mappingEntity, bootEntityInfo );
		}
	}


	private void transferBaseEntityAttributes(
			EntityInfo hbmEntity,
			JaxbEntityImpl mappingEntity,
			EntityTypeInfo entityTypeInfo) {
		try {
			mappingEntity.setAttributes( new JaxbAttributesContainerImpl() );
			transferBaseAttributes(
					entityTypeInfo.getPersistentClass().getEntityName(),
					hbmEntity.getAttributes(),
					entityTypeInfo,
					mappingEntity.getAttributes()
			);
		}
		catch (Exception e) {
			throw new TransformationException( "Error processing entity attributes : " + entityTypeInfo.getPersistentClass().getEntityName(), e, origin() );
		}
	}

	private void transferBaseAttributes(
			String roleBase,
			List<?> hbmAttributeMappings,
			ManagedTypeInfo managedTypeInfo,
			JaxbAttributesContainer attributes) {
		for ( Object hbmAttributeMapping : hbmAttributeMappings ) {
			if ( hbmAttributeMapping instanceof JaxbHbmBasicAttributeType basic ) {
				try {
					final PropertyInfo propertyInfo = managedTypeInfo.propertyInfoMap().get( basic.getName() );
					attributes.getBasicAttributes().add( transformBasicAttribute( basic, propertyInfo ) );
				}
				catch (Exception e) {
					throw new TransformationException( "Error transforming <property/> : " + basic.getName(), e, origin() );
				}
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmCompositeAttributeType hbmComponent ) {
				try {
					final String componentRole = roleBase + "." + hbmComponent.getName();
					final ComponentTypeInfo componentTypeInfo = transformationState.getEmbeddableInfoByRole().get( componentRole );
					final JaxbEmbeddableImpl jaxbEmbeddable = applyEmbeddable(
							roleBase,
							hbmComponent,
							componentTypeInfo
					);
					attributes.getEmbeddedAttributes().add( transformEmbedded( jaxbEmbeddable, hbmComponent ) );
				}
				catch (Exception e) {
					throw new TransformationException( "Error transforming <component/> : " + hbmComponent.getName(), e, origin() );
				}
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmPropertiesType hbmProperties ) {
				// while we could simply "unwrap" the <properties/> itself and inline the attributes,
				// <properties/> is most often used to create a target for property-ref mappings - that
				// we could not support without a new sort of annotation - e.g.
				//
				// @interface PropertyGroup {
				// 		String name();
				//		String[] propertyNames();
				// }
				//
				// where the `name` could then be used for `@PropertyRef`
				handleUnsupported( "<properties/> mappings not supported for transformation [name=%s]", hbmProperties.getName() );
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmDynamicComponentType dynamicComponentType) {
				final String name = dynamicComponentType.getName();
				handleUnsupported(
						"<dynamic-component/> mappings not supported for transformation [name=%s]",
						name
				);
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmOneToOneType hbmOneToOne ) {
				try {
					final PropertyInfo propertyInfo = managedTypeInfo.propertyInfoMap().get( hbmOneToOne.getName() );
					transferOneToOne( hbmOneToOne, propertyInfo, attributes );
				}
				catch (Exception e) {
					throw new TransformationException( "Error transforming <one-to-one/> : " + hbmOneToOne.getName(), e, origin() );
				}
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmManyToOneType hbmManyToOne ) {
				try {
					transferManyToOne( managedTypeInfo, attributes, hbmManyToOne );
				}
				catch (Exception e) {
					throw new TransformationException( "Error transforming <many-to-one/> : " + hbmManyToOne.getName(), e, origin() );
				}
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmAnyAssociationType any ) {
				try {
					final PropertyInfo propertyInfo = managedTypeInfo.propertyInfoMap().get( any.getName() );
					attributes.getAnyMappingAttributes().add( transformAnyAttribute( any, propertyInfo ) );
				}
				catch (Exception e) {
					throw new TransformationException( "Error transforming <any/> : " + any.getName(), e, origin() );
				}
			}
			else if ( hbmAttributeMapping instanceof PluralAttributeInfo hbmCollection ) {
				final PropertyInfo propertyInfo = managedTypeInfo.propertyInfoMap().get( hbmCollection.getName() );
				if ( hbmCollection.getElement() != null || hbmCollection.getCompositeElement() != null ) {
					try {
						attributes.getElementCollectionAttributes().add( transformElementCollection( roleBase, hbmCollection, propertyInfo ) );
					}
					catch (Exception e) {
						throw new TransformationException( "Error transforming element-collection : " + hbmCollection.getName(), e, origin() );
					}
				}
				else if ( hbmCollection.getOneToMany() != null ) {
					try {
						attributes.getOneToManyAttributes().add( transformOneToMany( hbmCollection, propertyInfo ) );
					}
					catch (Exception e) {
						throw new TransformationException( "Error transforming one-to-many : " + hbmCollection.getName(), e, origin() );
					}
				}
				else if ( hbmCollection.getManyToMany() != null ) {
					try {
						attributes.getManyToManyAttributes().add( transformManyToMany( hbmCollection, propertyInfo ) );
					}
					catch (Exception e) {
						throw new TransformationException( "Error transforming many-to-many : " + hbmCollection.getName(), e, origin() );
					}
				}
				else if ( hbmCollection.getManyToAny() != null ) {
					try {
						attributes.getPluralAnyMappingAttributes().add( transformPluralAny( hbmCollection ) );
					}
					catch (Exception e) {
						throw new TransformationException( "Error transforming many-to-any : " + hbmCollection.getName(), e, origin() );
					}
				}
				else {
					throw new UnsupportedOperationException( "Unexpected node type - " + hbmCollection );
				}
			}
		}
	}

	private JaxbBasicImpl transformBasicAttribute(final JaxbHbmBasicAttributeType hbmProp, PropertyInfo propertyInfo) {
		final JaxbBasicImpl basic = new JaxbBasicImpl();
		transferBasicAttribute( hbmProp, basic, propertyInfo );
		return basic;
	}

	private void transferBasicAttribute(
			JaxbHbmBasicAttributeType hbmProp,
			JaxbBasicImpl basic,
			PropertyInfo propertyInfo) {
		basic.setName( hbmProp.getName() );
		basic.setOptional( propertyInfo.bootModelProperty().isOptional() );
		basic.setFetch( FetchType.EAGER );
		basic.setAttributeAccessor( hbmProp.getAccess() );
		basic.setOptimisticLock( hbmProp.isOptimisticLock() );

		applyBasicTypeMapping(
				(BasicValue) propertyInfo.bootModelProperty().getValue(),
				basic,
				hbmProp.getTypeAttribute(),
				hbmProp.getType(),
				basic::setEnumerated,
				basicValueConverter -> {
					if ( basicValueConverter instanceof AttributeConverter<?,?> jpaAttributeConverter ) {
						final JaxbConvertImpl jaxbConvert = new JaxbConvertImpl();
						jaxbConvert.setConverter( jpaAttributeConverter.getClass().getName() );
						basic.setConvert( jaxbConvert );
					}
					else if ( basicValueConverter instanceof JpaAttributeConverter<?, ?> jpaAttributeConverter ) {
						final JaxbConvertImpl jaxbConvert = new JaxbConvertImpl();
						jaxbConvert.setConverter( jpaAttributeConverter.getConverterJavaType().getTypeName() );
						basic.setConvert( jaxbConvert );
					}
				}
		);

		transferColumnsAndFormulas(
				new ColumnAndFormulaSource() {
					@Override
					public String getColumnAttribute() {
						return hbmProp.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return hbmProp.getFormulaAttribute();
					}

					@Override
					public List<Serializable> getColumnOrFormula() {
						return hbmProp.getColumnOrFormula();
					}

					@Override
					public SourceColumnAdapter wrap(Serializable column) {
						return new SourceColumnAdapterJaxbHbmColumnType( (JaxbHbmColumnType) column );
					}
				},
				new ColumnAndFormulaTarget() {
					@Override
					public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
						return new TargetColumnAdapterJaxbColumn( columnDefaults );
					}

					@Override
					public void addColumn(TargetColumnAdapter column) {
						basic.setColumn( ( (TargetColumnAdapterJaxbColumn) column ).getTargetColumn() );
					}

					@Override
					public void addFormula(String formula) {
						basic.setFormula( formula );
					}
				},
				new ColumnDefaults() {
					@Override
					public Boolean isNullable() {
						return propertyInfo.bootModelProperty().isOptional();
					}

					@Override
					public Integer getLength() {
						return hbmProp.getLength();
					}

					@Override
					public Integer getScale() {
						return isNotEmpty( hbmProp.getScale() )
								? Integer.parseInt( hbmProp.getScale() )
								: null;
					}

					@Override
					public Integer getPrecision() {
						return isNotEmpty( hbmProp.getPrecision() )
								? Integer.parseInt( hbmProp.getPrecision() )
								: null;
					}

					@Override
					public Boolean isUnique() {
						return hbmProp.isUnique();
					}

					@Override
					public Boolean isInsertable() {
						return hbmProp.isInsert();
					}

					@Override
					public Boolean isUpdatable() {
						return hbmProp.isUpdate();
					}
				},
				propertyInfo.tableName()
		);
	}

	private JaxbUserTypeImpl interpretBasicType(String typeName, JaxbHbmConfigParameterContainer typeLocalParams, JaxbHbmTypeDefinitionType typeDef) {
		assert isNotEmpty( typeName );

		final JaxbUserTypeImpl typeNode = new JaxbUserTypeImpl();

		if ( typeDef == null ) {
			typeNode.setValue( typeName );
		}
		else {
			typeNode.setValue( typeDef.getClazz() );
			for ( JaxbHbmConfigParameterType hbmParam : typeDef.getConfigParameters() ) {
				final JaxbConfigurationParameterImpl param = new JaxbConfigurationParameterImpl();
				param.setName( hbmParam.getName() );
				param.setValue( hbmParam.getValue() );
				typeNode.getParameters().add( param );
			}
		}

		if ( typeLocalParams != null ) {
			for ( JaxbHbmConfigParameterType hbmParam : typeLocalParams.getConfigParameters() ) {
				final JaxbConfigurationParameterImpl param = new JaxbConfigurationParameterImpl();
				param.setName( hbmParam.getName() );
				param.setValue( hbmParam.getValue() );
				typeNode.getParameters().add( param );
			}
		}

		return typeNode;
	}

	private JaxbEmbeddableImpl applyEmbeddable(
			String roleBase,
			JaxbHbmCompositeAttributeType hbmComponent,
			ComponentTypeInfo componentTypeInfo) {
		final String embeddableClassName = componentTypeInfo.getComponent().getComponentClassName();
		if ( isNotEmpty( embeddableClassName ) ) {
			final JaxbEmbeddableImpl existing = jaxbEmbeddableByClassName.get( embeddableClassName );
			if ( existing != null ) {
				return existing;
			}
		}

		final String role = roleBase + "." + hbmComponent.getName();
		final String embeddableName = determineEmbeddableName( embeddableClassName, hbmComponent.getName() );
		final JaxbEmbeddableImpl jaxbEmbeddable = convertEmbeddable(
				role,
				embeddableName,
				embeddableClassName,
				hbmComponent
		);
		mappingXmlBinding.getRoot().getEmbeddables().add( jaxbEmbeddable );

		if ( isNotEmpty( embeddableClassName ) ) {
			jaxbEmbeddableByClassName.put( embeddableClassName, jaxbEmbeddable );
		}

		return jaxbEmbeddable;
	}


	private JaxbEmbeddableImpl convertEmbeddable(
			String role,
			String embeddableName,
			String embeddableClassName,
			JaxbHbmCompositeAttributeType hbmComponent) {
		final ComponentTypeInfo componentTypeInfo = transformationState.getEmbeddableInfoByRole().get( role );

		final JaxbEmbeddableImpl embeddable = new JaxbEmbeddableImpl();
		embeddable.setMetadataComplete( true );
		embeddable.setName( embeddableName );
		embeddable.setClazz( embeddableClassName );

		embeddable.setAttributes( new JaxbEmbeddableAttributesContainerImpl() );
		transferBaseAttributes( role, hbmComponent.getAttributes(), componentTypeInfo, embeddable.getAttributes() );
		return embeddable;
	}

	private int counter = 1;
	private String determineEmbeddableName(String componentClassName, String attributeName) {
		if ( isNotEmpty( componentClassName ) ) {
			return componentClassName;
		}
		return attributeName + "_" + counter++;
	}

	private JaxbEmbeddedImpl transformEmbedded(
			JaxbEmbeddableImpl jaxbEmbeddable,
			JaxbHbmCompositeAttributeType hbmComponent) {
		final JaxbEmbeddedImpl embedded = new JaxbEmbeddedImpl();
		embedded.setName( hbmComponent.getName() );
		embedded.setAttributeAccessor( hbmComponent.getAccess() );
		embedded.setTarget( jaxbEmbeddable.getName() );
		return embedded;
	}

	private void transferOneToOne(JaxbHbmOneToOneType hbmOneToOne, PropertyInfo propertyInfo, JaxbAttributesContainer attributes) {
		final JaxbOneToOneImpl oneToOne = new JaxbOneToOneImpl();
		oneToOne.setAttributeAccessor( hbmOneToOne.getAccess() );
		oneToOne.setOptional( propertyInfo.bootModelProperty().isOptional() );
		oneToOne.setCascade( convertCascadeType( hbmOneToOne.getCascade() ) );
		oneToOne.setOrphanRemoval( isOrphanRemoval( hbmOneToOne.getCascade() ) );
		oneToOne.setForeignKey( new JaxbForeignKeyImpl() );
		oneToOne.getForeignKey().setName( hbmOneToOne.getForeignKey() );
		if ( isNotEmpty( hbmOneToOne.getPropertyRef() ) ) {
			oneToOne.setPropertyRef( new JaxbPropertyRefImpl() );
			oneToOne.getPropertyRef().setName( hbmOneToOne.getPropertyRef() );
		}
		for ( String formula : hbmOneToOne.getFormula() ) {
			oneToOne.getJoinColumnOrJoinFormula().add( formula );
		}
		oneToOne.setName( hbmOneToOne.getName() );
		if ( isNotEmpty( hbmOneToOne.getEntityName() ) ) {
			oneToOne.setTargetEntity( hbmOneToOne.getEntityName() );
		}
		else {
			oneToOne.setTargetEntity( hbmOneToOne.getClazz() );
		}

		transferFetchable( hbmOneToOne.getLazy(), hbmOneToOne.getFetch(), hbmOneToOne.getOuterJoin(), hbmOneToOne.isConstrained(), oneToOne );

		attributes.getOneToOneAttributes().add( oneToOne );
	}

	private void transferManyToOne(
			ManagedTypeInfo managedTypeInfo,
			JaxbAttributesContainer attributes,
			JaxbHbmManyToOneType hbmManyToOne) {
		final PropertyInfo propertyInfo = managedTypeInfo.propertyInfoMap().get( hbmManyToOne.getName() );
		final JaxbManyToOneImpl jaxbManyToOne = transformManyToOne( hbmManyToOne, propertyInfo );
		attributes.getManyToOneAttributes().add( jaxbManyToOne );
	}

	private JaxbManyToOneImpl transformManyToOne(JaxbHbmManyToOneType hbmNode, PropertyInfo propertyInfo) {
		final JaxbManyToOneImpl jaxbManyToOne = new JaxbManyToOneImpl();
		jaxbManyToOne.setName( hbmNode.getName() );
		jaxbManyToOne.setOptional( propertyInfo.bootModelProperty().isOptional() );
		if ( isNotEmpty( hbmNode.getEntityName() ) ) {
			jaxbManyToOne.setTargetEntity( hbmNode.getEntityName() );
		}
		else {
			jaxbManyToOne.setTargetEntity( hbmNode.getClazz() );
		}
		transferFetchable( hbmNode.getLazy(), hbmNode.getFetch(), hbmNode.getOuterJoin(), null, jaxbManyToOne );

		jaxbManyToOne.setAttributeAccessor( hbmNode.getAccess() );
		jaxbManyToOne.setCascade( convertCascadeType( hbmNode.getCascade() ) );

		if ( isNotEmpty( hbmNode.getPropertyRef() ) ) {
			jaxbManyToOne.setPropertyRef( new JaxbPropertyRefImpl() );
			jaxbManyToOne.getPropertyRef().setName( hbmNode.getPropertyRef() );
		}

		final Property manyToOneProperty = propertyInfo.bootModelProperty();
		final ManyToOne manyToOne = (ManyToOne) manyToOneProperty.getValue();
		transferColumnsAndFormulas(
				manyToOne,
				new ColumnAndFormulaTarget() {
					@Override
					public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
						return new TargetColumnAdapterJaxbJoinColumn( columnDefaults );
					}

					@Override
					public void addColumn(TargetColumnAdapter column) {
						jaxbManyToOne.getJoinColumnOrJoinFormula()
								.add( ( (TargetColumnAdapterJaxbJoinColumn) column ).getTargetColumn() );
					}

					@Override
					public void addFormula(String formula) {
						jaxbManyToOne.getJoinColumnOrJoinFormula().add( formula );
					}
				},
				new ColumnDefaultsProperty( manyToOneProperty ),
				propertyInfo.tableName()
		);

		jaxbManyToOne.setForeignKey( transformForeignKey( hbmNode.getForeignKey() ) );

		if ( hbmNode.getNotFound() != null ) {
			jaxbManyToOne.setNotFound( interpretNotFoundAction( hbmNode.getNotFound() ) );
		}

		return jaxbManyToOne;
	}

	private NotFoundAction interpretNotFoundAction(JaxbHbmNotFoundEnum hbmNotFound) {
		return switch ( hbmNotFound ) {
			case EXCEPTION -> NotFoundAction.EXCEPTION;
			case IGNORE -> NotFoundAction.IGNORE;
		};
	}


	private JaxbAnyMappingImpl transformAnyAttribute(JaxbHbmAnyAssociationType source, PropertyInfo propertyInfo) {
		final JaxbAnyMappingImpl target = new JaxbAnyMappingImpl();

		target.setName( source.getName() );
		target.setAttributeAccessor( source.getAccess() );
		target.setOptimisticLock( source.isOptimisticLock() );
		target.setOptional( propertyInfo.bootModelProperty().isOptional() );

		// todo : cascade
		// todo : discriminator column
		// todo : key column

		target.setDiscriminator( new JaxbAnyMappingDiscriminatorImpl() );
		source.getMetaValue().forEach( (sourceMapping) -> {
			final JaxbAnyDiscriminatorValueMappingImpl mapping = new JaxbAnyDiscriminatorValueMappingImpl();
			mapping.setDiscriminatorValue( sourceMapping.getValue() );
			mapping.setCorrespondingEntityName( sourceMapping.getClazz() );
			target.getDiscriminator().getValueMappings().add( mapping );
		} );

		target.setKey( new JaxbAnyMappingKeyImpl() );

		return target;
	}

	private JaxbElementCollectionImpl transformElementCollection(
			String roleBase,
			PluralAttributeInfo source,
			PropertyInfo propertyInfo) {
		final JaxbElementCollectionImpl target = new JaxbElementCollectionImpl();
		transferCollectionCommonInfo( source, target );
		transferCollectionTable( source, target );

		if ( source.getElement() != null ) {
			transferElementInfo( source, source.getElement(), propertyInfo, target );
		}
		else {
			target.setTargetClass( source.getCompositeElement().getClazz() );
			transferElementInfo( roleBase, source, source.getCompositeElement(), target );
		}

		return target;
	}

	private void transferCollectionTable(
			final PluralAttributeInfo source,
			final JaxbElementCollectionImpl target) {
		target.setCollectionTable( new JaxbCollectionTableImpl() );

		final JaxbCollectionTableImpl collectionTable = target.getCollectionTable();
		if ( isNotEmpty( source.getTable() ) ) {
			collectionTable.setName( source.getTable() );
			collectionTable.setCatalog( source.getCatalog() );
			collectionTable.setSchema( source.getSchema() );
		}
		final JaxbHbmKeyType key = source.getKey();
		if ( key != null ) {
			collectionTable.setForeignKeys( transformForeignKey( key.getForeignKey() ) );
			transferColumnsAndFormulas(
					new ColumnAndFormulaSource() {
						@Override
						public String getColumnAttribute() {
							return key.getColumnAttribute();
						}

						@Override
						public String getFormulaAttribute() {
							return null;
						}

						@Override
						public List<Serializable> getColumnOrFormula() {
							return new ArrayList<>( key.getColumn() );
						}

						@Override
						public SourceColumnAdapter wrap(Serializable column) {
							return new SourceColumnAdapterJaxbHbmColumnType( (JaxbHbmColumnType) column );
						}
					},
					new ColumnAndFormulaTarget() {
						@Override
						public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
							return new TargetColumnAdapterJaxbJoinColumn( columnDefaults );
						}

						@Override
						public void addColumn(TargetColumnAdapter column) {
							collectionTable.getJoinColumns()
									.add( ( (TargetColumnAdapterJaxbJoinColumn) column ).getTargetColumn() );
						}

						@Override
						public void addFormula(String formula) {
							handleUnsupportedContent(
									"formula as part of element-collection key is not supported for transformation; skipping"
							);
						}
					},
					ColumnDefaultsBasicImpl.INSTANCE,
//					source.getTable()
					null

			);

			if ( isNotEmpty( key.getPropertyRef() ) ) {
				handleUnsupportedContent(
						"Foreign-key (<key/>) for persistent collection (name=" + source.getName() +
								") specified property-ref which is not supported for transformation; " +
								"transformed <join-column/> will need manual adjustment of referenced-column-name"
				);
			}
		}
	}


	private void transferCollectionCommonInfo(PluralAttributeInfo source, JaxbPluralAttribute target) {
		target.setName( source.getName() );
		target.setAttributeAccessor( source.getAccess() );
		target.setFetchMode( convert( source.getFetch() ) );
		target.setFetch( convert( source.getLazy() ) );

		if ( isNotEmpty( source.getCollectionType() ) ) {
			final JaxbCollectionUserTypeImpl jaxbCollectionUserType = new JaxbCollectionUserTypeImpl();
			target.setCollectionType( jaxbCollectionUserType );
			jaxbCollectionUserType.setType( source.getCollectionType() );
		}

		if ( source instanceof JaxbHbmSetType set ) {
			final String sort = set.getSort();
			if ( isNotEmpty( sort ) && !"unsorted".equals( sort ) ) {
				target.setSort( sort );
			}
			target.setOrderBy( set.getOrderBy() );
			target.setClassification( LimitedCollectionClassification.SET );
		}
		else if ( source instanceof JaxbHbmMapType map ) {
			final String sort = map.getSort();
			if ( isNotEmpty( sort ) && !"unsorted".equals( sort ) ) {
				target.setSort( sort );
			}
			target.setOrderBy( map.getOrderBy() );

			transferMapKey( map, target );
			target.setClassification( LimitedCollectionClassification.MAP );
		}
		else if ( source instanceof JaxbHbmIdBagCollectionType ) {
			handleUnsupported( "collection-id is not supported for transformation" );

		}
		else if ( source instanceof JaxbHbmBagCollectionType ) {
			target.setClassification( LimitedCollectionClassification.BAG );
		}
		else if ( source instanceof JaxbHbmListType listType ) {
			transferListIndex(
					listType.getIndex(),
					listType.getListIndex(),
					target
			);
			target.setClassification( LimitedCollectionClassification.LIST );
		}
		else if ( source instanceof JaxbHbmArrayType arrayType ) {
			transferListIndex(
					arrayType.getIndex(),
					arrayType.getListIndex(),
					target
			);
			target.setClassification( LimitedCollectionClassification.LIST );
		}
		else if ( source instanceof JaxbHbmPrimitiveArrayType primitiveArrayType ) {
			transferListIndex(
					primitiveArrayType.getIndex(),
					primitiveArrayType.getListIndex(),
					target
			);
			target.setClassification( LimitedCollectionClassification.LIST );
		}
	}

	private FetchType convert(JaxbHbmLazyWithExtraEnum lazy) {
		return lazy == null
			|| lazy == JaxbHbmLazyWithExtraEnum.TRUE
				? FetchType.LAZY
				: FetchType.EAGER;
	}

	private void transferListIndex(
			JaxbHbmIndexType index,
			JaxbHbmListIndexType listIndex,
			JaxbPluralAttribute target) {
		final JaxbOrderColumnImpl orderColumn = new JaxbOrderColumnImpl();
		target.setOrderColumn( orderColumn );

		if ( index != null ) {
			// todo : base on order-column
			if ( isNotEmpty( index.getColumnAttribute() ) ) {
				orderColumn.setName( index.getColumnAttribute() );
			}
			else if ( index.getColumn().size() == 1 ) {
				final JaxbHbmColumnType hbmColumn = index.getColumn().get( 0 );
				orderColumn.setName( hbmColumn.getName() );
				orderColumn.setNullable( invert( hbmColumn.isNotNull() ) );
				orderColumn.setColumnDefinition( hbmColumn.getSqlType() );
			}
		}
		else if ( listIndex != null ) {
			// todo : base on order-column
			if ( isNotEmpty( listIndex.getColumnAttribute() ) ) {
				orderColumn.setName( listIndex.getColumnAttribute() );
			}
			else if ( listIndex.getColumn() != null ) {
				orderColumn.setName( listIndex.getColumn().getName() );
				orderColumn.setNullable( invert( listIndex.getColumn().isNotNull() ) );
				orderColumn.setColumnDefinition( listIndex.getColumn().getSqlType() );
			}
		}
	}

	private void transferMapKey(JaxbHbmMapType source, JaxbPluralAttribute target) {
		if ( source.getIndex() != null ) {
			final JaxbMapKeyColumnImpl mapKey = new JaxbMapKeyColumnImpl();
			// TODO: multiple columns?
			mapKey.setName( source.getIndex().getColumnAttribute() );
			target.setMapKeyColumn( mapKey );
		}
		else if ( source.getMapKey() != null ) {
			if ( ! isEmpty( source.getMapKey().getFormulaAttribute() ) ) {
				handleUnsupported(
						"Transformation of formula attribute within map-keys is not supported - `%s`",
						origin()
				);
				return;
			}

			if ( CollectionHelper.isNotEmpty( source.getMapKey().getColumnOrFormula() ) ) {
				handleUnsupported(
						"Transformation of column/formula elements within map-keys is not supported - `%s`",
						origin()
				);
				return;
			}

			if ( isNotEmpty( source.getMapKey().getNode() ) ) {
				handleUnsupported(
						"Transformation of `node` attribute is not supported - %s",
						origin()
				);
				return;
			}

			final String mapKeyType = resolveMapKeyType( source.getMapKey() );
			if ( mapKeyType != null ) {
				final JaxbUserTypeImpl jaxbMapKeyType = new JaxbUserTypeImpl();
				target.setMapKeyType( jaxbMapKeyType );
				jaxbMapKeyType.setValue( mapKeyType );
			}

			if ( isNotEmpty( source.getMapKey().getColumnAttribute() ) ) {
				final JaxbMapKeyColumnImpl mapKeyColumn = new JaxbMapKeyColumnImpl();
				mapKeyColumn.setName( source.getMapKey().getColumnAttribute() );
				target.setMapKeyColumn( mapKeyColumn );
			}
		}
	}

	private String resolveMapKeyType(JaxbHbmMapKeyBasicType mapKey) {
		if ( isNotEmpty( mapKey.getTypeAttribute() ) ) {
			return mapKey.getTypeAttribute();
		}
		else if ( mapKey.getType() != null ) {
			return nullIfEmpty( mapKey.getType().getName() );
		}
		else {
			return null;
		}
	}

	private Boolean invert(Boolean value) {
		return value == null ? null : !value;
	}

	private JaxbPluralFetchModeImpl convert(JaxbHbmFetchStyleWithSubselectEnum fetch) {
		if ( fetch == null ) {
			return null;
		}
		else {
			return switch ( fetch ) {
				case SELECT -> JaxbPluralFetchModeImpl.SELECT;
				case JOIN -> JaxbPluralFetchModeImpl.JOIN;
				case SUBSELECT -> JaxbPluralFetchModeImpl.SUBSELECT;
			};
		}
	}


	private void transferElementInfo(
			PluralAttributeInfo hbmCollection,
			JaxbHbmBasicCollectionElementType element,
			PropertyInfo propertyInfo,
			JaxbElementCollectionImpl target) {
		transferCollectionCommonInfo( hbmCollection, target );
		transferCollectionTable( hbmCollection, target );

		transferElementTypeInfo( hbmCollection, element, propertyInfo, target );

		transferColumnsAndFormulas(
				new ColumnAndFormulaSource() {
					@Override
					public String getColumnAttribute() {
						return element.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return element.getFormulaAttribute();
					}

					@Override
					public List<Serializable> getColumnOrFormula() {
						return element.getColumnOrFormula();
					}

					@Override
					public SourceColumnAdapter wrap(Serializable column) {
						return new SourceColumnAdapterJaxbHbmColumnType( (JaxbHbmColumnType) column );
					}
				},
				new ColumnAndFormulaTarget() {
					@Override
					public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
						return new TargetColumnAdapterJaxbColumn( columnDefaults );
					}

					@Override
					public void addColumn(TargetColumnAdapter column) {
						target.setColumn( ( (TargetColumnAdapterJaxbColumn) column ).getTargetColumn() );
					}

					@Override
					public void addFormula(String formula) {
						target.setFormula( formula );
					}
				},
				ColumnDefaultsBasicImpl.INSTANCE,
				null
		);
	}

	private void transferElementTypeInfo(
			PluralAttributeInfo hbmCollection,
			JaxbHbmBasicCollectionElementType element,
			PropertyInfo propertyInfo,
			JaxbElementCollectionImpl target) {
		final Collection collectionValue = (Collection) propertyInfo.bootModelProperty().getValue();
		final BasicValue basicValue = (BasicValue) collectionValue.getElement();
		applyBasicTypeMapping(
				basicValue,
				target,
				element.getTypeAttribute(),
				element.getType(),
				target::setEnumerated,
				basicValueConverter -> {
					if ( basicValueConverter instanceof AttributeConverter<?,?> jpaAttributeConverter ) {
						final JaxbConvertImpl jaxbConvert = new JaxbConvertImpl();
						jaxbConvert.setConverter( jpaAttributeConverter.getClass().getName() );
						target.getConverts().add( jaxbConvert );
					}
					else if ( basicValueConverter instanceof JpaAttributeConverter<?, ?> jpaAttributeConverter ) {
						final JaxbConvertImpl jaxbConvert = new JaxbConvertImpl();
						jaxbConvert.setConverter( jpaAttributeConverter.getConverterJavaType().getTypeName() );
						target.getConverts().add( jaxbConvert );
					}
				}
		);
		target.setTargetClass( basicValue.resolve().getDomainJavaType().getTypeName() );
	}

	private void transferElementInfo(
			String roleBase,
			PluralAttributeInfo hbmCollection,
			JaxbHbmCompositeCollectionElementType compositeElement,
			JaxbElementCollectionImpl target) {
		transferCollectionCommonInfo( hbmCollection, target );
		transferCollectionTable( hbmCollection, target );

		final String embeddableClassName = compositeElement.getClazz();
		final String embeddableName = determineEmbeddableName( embeddableClassName, hbmCollection.getName() );

		final String partRole = roleBase + "." + hbmCollection.getName() + ".value";
		final ComponentTypeInfo componentTypeInfo = transformationState.getEmbeddableInfoByRole().get( partRole );

		target.setTarget( embeddableName );
		if ( isNotEmpty( embeddableClassName ) ) {
			target.setTargetClass( embeddableClassName );
		}

		final JaxbEmbeddableImpl embeddable = new JaxbEmbeddableImpl();
		embeddable.setClazz( embeddableClassName );
		embeddable.setName( embeddableName );
		embeddable.setAttributes( new JaxbEmbeddableAttributesContainerImpl() );
		transferBaseAttributes(
				partRole,
				compositeElement.getAttributes(),
				componentTypeInfo,
				embeddable.getAttributes()
		);
		mappingXmlBinding.getRoot().getEmbeddables().add( embeddable );
	}

	private JaxbOneToManyImpl transformOneToMany(PluralAttributeInfo hbmCollection, PropertyInfo propertyInfo) {
		final JaxbOneToManyImpl target = new JaxbOneToManyImpl();
		transferOneToManyInfo( hbmCollection, hbmCollection.getOneToMany(), target , propertyInfo);
		return target;
	}

	private void transferOneToManyInfo(
			PluralAttributeInfo hbmAttributeInfo,
			JaxbHbmOneToManyCollectionElementType hbmOneToMany,
			JaxbOneToManyImpl target,
			PropertyInfo propertyInfo) {
		if ( hbmOneToMany.isEmbedXml() != null ) {
			handleUnsupported( "`embed-xml` not supported" );
		}
		if ( !(hbmOneToMany.getNode() == null || hbmOneToMany.getNode().isBlank() ) ) {
			handleUnsupported( "`node` not supported" );
		}

		transferCollectionCommonInfo( hbmAttributeInfo, target );
		target.setTargetEntity( isNotEmpty( hbmOneToMany.getClazz() ) ? hbmOneToMany.getClazz() : hbmOneToMany.getEntityName() );

		final Property bootModelProperty = propertyInfo.bootModelProperty();
		final Collection bootModelValue = (Collection) bootModelProperty.getValue();

		final JaxbHbmKeyType key = hbmAttributeInfo.getKey();

		if ( bootModelValue.isInverse() ) {
			target.setMappedBy( resolveMappedBy( hbmAttributeInfo, bootModelProperty, bootModelValue ) );
		}
		else {

			// columns + formulas --> do we need similar for lists, sets, etc?
			// ~~> hbmListNode.getElement()
			//transferCollectionTable( source, oneToMany )

			if ( key != null ) {
				target.setForeignKey( transformForeignKey( key.getForeignKey() ) );
				transferColumnsAndFormulas(
						new ColumnAndFormulaSource() {
							@Override
							public String getColumnAttribute() {
								return key.getColumnAttribute();
							}

							@Override
							public String getFormulaAttribute() {
								return null;
							}

							@Override
							public List<Serializable> getColumnOrFormula() {
								return new ArrayList<>( key.getColumn() );
							}

							@Override
							public SourceColumnAdapter wrap(Serializable column) {
								return new SourceColumnAdapterJaxbHbmColumnType( (JaxbHbmColumnType) column );
							}
						},
						new ColumnAndFormulaTarget() {
							@Override
							public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
								return new TargetColumnAdapterJaxbJoinColumn( columnDefaults );
							}

							@Override
							public void addColumn(TargetColumnAdapter column) {
								target.getJoinColumn()
										.add( ( (TargetColumnAdapterJaxbJoinColumn) column ).getTargetColumn() );
							}

							@Override
							public void addFormula(String formula) {

							}
						},
						ColumnDefaultsBasicImpl.INSTANCE,
						null
				);
			}
		}

		if ( hbmOneToMany.getNotFound() != null ) {
			target.setNotFound( interpretNotFoundAction( hbmOneToMany.getNotFound() ) );
		}

		target.setOrphanRemoval( isOrphanRemoval( hbmAttributeInfo.getCascade() ) );
		target.setCascade( convertCascadeType( hbmAttributeInfo.getCascade() ) );

		for ( JaxbHbmFilterType hbmFilter : hbmAttributeInfo.getFilter() ) {
			target.getFilters().add( convert( hbmFilter ) );
		}

		if ( isNotEmpty( hbmAttributeInfo.getWhere() ) ) {
			target.setSqlRestriction( hbmAttributeInfo.getWhere() );
		}
		if ( hbmAttributeInfo.getSqlInsert() != null ) {
			final JaxbCustomSqlImpl jaxbCustomSql = new JaxbCustomSqlImpl();
			target.setSqlInsert( jaxbCustomSql );
			transferCustomSql( hbmAttributeInfo.getSqlInsert(), jaxbCustomSql );
		}
		if ( hbmAttributeInfo.getSqlUpdate() != null ) {
			final JaxbCustomSqlImpl jaxbCustomSql = new JaxbCustomSqlImpl();
			target.setSqlUpdate( jaxbCustomSql );
			transferCustomSql( hbmAttributeInfo.getSqlUpdate(), jaxbCustomSql );
		}
		if ( hbmAttributeInfo.getSqlDelete() != null ) {
			final JaxbCustomSqlImpl jaxbCustomSql = new JaxbCustomSqlImpl();
			target.setSqlDelete( jaxbCustomSql );
			transferCustomSql( hbmAttributeInfo.getSqlDelete(), jaxbCustomSql );
		}
		if ( hbmAttributeInfo.getSqlDeleteAll() != null ) {
			final JaxbCustomSqlImpl jaxbCustomSql = new JaxbCustomSqlImpl();
			target.setSqlDeleteAll( jaxbCustomSql );
			transferCustomSql( hbmAttributeInfo.getSqlDeleteAll(), jaxbCustomSql );
		}
	}

	private String resolveMappedBy(
			PluralAttributeInfo hbmAttributeInfo,
			Property bootModelProperty,
			Collection bootModelValue) {
		if ( isNotEmpty( bootModelValue.getMappedByProperty() ) ) {
			return bootModelValue.getMappedByProperty();
		}

		final OneToMany element = (OneToMany) bootModelValue.getElement();
		final String referencedEntityName = element.getReferencedEntityName();
		final Map<List<Selectable>, String> attributeMap = transformationState.getMappableAttributesByColumns( referencedEntityName );
		return resolveMappedBy( bootModelProperty, bootModelValue, attributeMap );
	}

	private String resolveMappedBy(
			Property bootModelProperty,
			Collection bootModelValue,
			Map<List<Selectable>, String> attributeMap) {
		if ( attributeMap != null ) {
			final KeyValue collectionKey = bootModelValue.getKey();
			for ( Map.Entry<List<Selectable>, String> attributeEntry : attributeMap.entrySet() ) {
				if ( matches( collectionKey, attributeEntry.getKey() ) ) {
					return attributeEntry.getValue();
				}
			}
		}

		throw new AssertionFailure(
				String.format(
						Locale.ROOT,
						"Unable to determine mapped-by name for inverse one-to-many : %s.%s",
						bootModelProperty.getPersistentClass().getEntityName(),
						bootModelProperty.getName()
				)
		);
	}

	private boolean matches(KeyValue collectionKey, List<Selectable> candidate) {
		final List<Selectable> collectionKeySelectables = collectionKey.getSelectables();
		if ( collectionKeySelectables.size() != candidate.size() ) {
			return false;
		}

		// opt-out checking -> looking for a non-match
		for ( int i = 0; i < collectionKeySelectables.size(); i++ ) {
			final Selectable collectionKeySelectable = collectionKeySelectables.get( i );
			final Selectable candidateSelectable = candidate.get( i );
			if ( collectionKeySelectable instanceof Formula || candidateSelectable instanceof Formula ) {
				continue;
			}

			final Column collectionKeyColumn = (Column) collectionKeySelectable;
			final Column candidateColumn = (Column) candidateSelectable;
			assert isNotEmpty( collectionKeyColumn.getCanonicalName() );
			assert isNotEmpty( candidateColumn.getCanonicalName() );
			if ( !collectionKeyColumn.getCanonicalName().equals( candidateColumn.getCanonicalName() ) ) {
				return false;
			}
		}

		// as best we can tell, this is the match
		return true;
	}

	private void transferCustomSql(JaxbHbmCustomSqlDmlType hbmCustomSql, JaxbCustomSqlImpl mappingCustomSql) {
		mappingCustomSql.setValue( hbmCustomSql.getValue() );
		mappingCustomSql.setCallable( hbmCustomSql.isCallable() );
		mappingCustomSql.setResultCheck( hbmCustomSql.getCheck() );
	}

	private JaxbManyToManyImpl transformManyToMany(PluralAttributeInfo hbmCollection, PropertyInfo propertyInfo) {
		final JaxbManyToManyImpl target = new JaxbManyToManyImpl();
		transferManyToManyInfo( hbmCollection, hbmCollection.getManyToMany(), propertyInfo, target );
		return target;
	}

	private void transferManyToManyInfo(
			PluralAttributeInfo hbmCollection,
			JaxbHbmManyToManyCollectionElementType manyToMany,
			PropertyInfo propertyInfo,
			JaxbManyToManyImpl target) {
		if ( manyToMany.isEmbedXml() != null ) {
			handleUnsupported( "`embed-xml` no longer supported" );
		}
		if ( isNotEmpty( manyToMany.getNode() ) ) {
			handleUnsupported( "`node` no longer supported" );
		}

		final Property bootModelProperty = propertyInfo.bootModelProperty();
		final Collection bootValue = (Collection) bootModelProperty.getValue();

		final JaxbJoinTableImpl joinTable = new JaxbJoinTableImpl();
		final String tableName = hbmCollection.getTable();
		if ( isNotEmpty( tableName ) ) {
			joinTable.setName( tableName );
		}
		target.setJoinTable( joinTable );

		final JaxbHbmKeyType key = hbmCollection.getKey();
		if ( key != null ) {
			joinTable.setForeignKey( transformForeignKey( key.getForeignKey() ) );
			transferColumnsAndFormulas(
					new ColumnAndFormulaSource() {
						@Override
						public String getColumnAttribute() {
							return key.getColumnAttribute();
						}

						@Override
						public String getFormulaAttribute() {
							return "";
						}

						@Override
						public List<Serializable> getColumnOrFormula() {
							return new ArrayList<>(key.getColumn());
						}

						@Override
						public SourceColumnAdapter wrap(Serializable column) {
							return new SourceColumnAdapterJaxbHbmColumnType( (JaxbHbmColumnType) column );
						}
					},
					new ColumnAndFormulaTarget() {
						@Override
						public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
							return new TargetColumnAdapterJaxbJoinColumn( columnDefaults );
						}

						@Override
						public void addColumn(TargetColumnAdapter column) {
							joinTable.getJoinColumn().add( ( (TargetColumnAdapterJaxbJoinColumn) column ).getTargetColumn() );
						}

						@Override
						public void addFormula(String formula) {

						}
					},
					ColumnDefaultsBasicImpl.INSTANCE,
					bootValue.getKey().getTable().getName()
			);
		}

		transferColumnsAndFormulas(
				new ColumnAndFormulaSource() {
					@Override
					public String getColumnAttribute() {
						return manyToMany.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return manyToMany.getFormulaAttribute();
					}

					@Override
					public List<Serializable> getColumnOrFormula() {
						return manyToMany.getColumnOrFormula();
					}

					@Override
					public SourceColumnAdapter wrap(Serializable column) {
						return new SourceColumnAdapterJaxbHbmColumnType( (JaxbHbmColumnType) column );
					}
				},
				new ColumnAndFormulaTarget() {
					@Override
					public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
						return new TargetColumnAdapterJaxbJoinColumn( columnDefaults );
					}

					@Override
					public void addColumn(TargetColumnAdapter column) {
						joinTable.getInverseJoinColumn().add( ( (TargetColumnAdapterJaxbJoinColumn) column ).getTargetColumn() );
					}

					@Override
					public void addFormula(String formula) {
						handleUnsupported( "<many-to-many formula> not supported skipping" );
					}
				},
				ColumnDefaultsBasicImpl.INSTANCE,
				joinTable.getName()
		);

		transferCollectionCommonInfo( hbmCollection, target );
		target.setTargetEntity( isNotEmpty( manyToMany.getClazz() ) ? manyToMany.getClazz() : manyToMany.getEntityName() );

		if ( manyToMany.getNotFound() == JaxbHbmNotFoundEnum.IGNORE ) {
			target.setNotFound( NotFoundAction.IGNORE );
		}

		for ( JaxbHbmFilterType hbmFilter : hbmCollection.getFilter() ) {
			target.getFilters().add( convert( hbmFilter ) );
		}

		if ( isNotEmpty( hbmCollection.getWhere() ) ) {
			target.setSqlRestriction( hbmCollection.getWhere() );
		}
		if ( hbmCollection.getSqlInsert() != null ) {
			final JaxbCustomSqlImpl jaxbCustomSql = new JaxbCustomSqlImpl();
			target.setSqlInsert( jaxbCustomSql );
			transferCustomSql( hbmCollection.getSqlInsert(), jaxbCustomSql );
		}
		if ( hbmCollection.getSqlUpdate() != null ) {
			final JaxbCustomSqlImpl jaxbCustomSql = new JaxbCustomSqlImpl();
			target.setSqlUpdate( jaxbCustomSql );
			transferCustomSql( hbmCollection.getSqlUpdate(), jaxbCustomSql );
		}
		if ( hbmCollection.getSqlDelete() != null ) {
			final JaxbCustomSqlImpl jaxbCustomSql = new JaxbCustomSqlImpl();
			target.setSqlDelete( jaxbCustomSql );
			transferCustomSql( hbmCollection.getSqlDelete(), jaxbCustomSql );
		}
		if ( hbmCollection.getSqlDeleteAll() != null ) {
			final JaxbCustomSqlImpl jaxbCustomSql = new JaxbCustomSqlImpl();
			target.setSqlDeleteAll( jaxbCustomSql );
			transferCustomSql( hbmCollection.getSqlDeleteAll(), jaxbCustomSql );
		}
	}

	private JaxbPluralAnyMappingImpl transformPluralAny(PluralAttributeInfo hbmCollection) {
		final JaxbPluralAnyMappingImpl target = new JaxbPluralAnyMappingImpl();
		transferPluralAny( hbmCollection, hbmCollection.getManyToAny(), target );
		return target;
	}

	private void transferPluralAny(
			PluralAttributeInfo hbmCollection,
			JaxbHbmManyToAnyCollectionElementType manyToAny,
			JaxbPluralAnyMappingImpl target) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	private void transferIdentifier(
			JaxbHbmRootEntityType hbmEntity,
			JaxbEntityImpl mappingXmlEntity,
			EntityTypeInfo bootEntityInfo,
			RootClass rootClass) {
		final Property identifierProperty = rootClass.getIdentifierProperty();
		if ( identifierProperty != null ) {
			// we have either a simple id or an embedded id
			transferSinglePropertyIdentifier( hbmEntity, mappingXmlEntity, bootEntityInfo, rootClass, identifierProperty );
		}
		else {
			transferNonAggregatedCompositeId( hbmEntity, mappingXmlEntity, bootEntityInfo, rootClass );
		}
	}

	private void transferSinglePropertyIdentifier(
			JaxbHbmRootEntityType hbmEntity,
			JaxbEntityImpl mappingXmlEntity,
			EntityTypeInfo bootEntityInfo,
			RootClass rootClass,
			Property identifierProperty) {
		if ( identifierProperty.getValue() instanceof BasicValue basicValue ) {
			final JaxbIdImpl simpleId = new JaxbIdImpl();
			transferSimpleId( hbmEntity.getId(), simpleId, identifierProperty, basicValue );
			mappingXmlEntity.getAttributes().getIdAttributes().add( simpleId );
		}
		else {
			final JaxbEmbeddedIdImpl embeddedId = new JaxbEmbeddedIdImpl();
			transferEmbeddedId( hbmEntity.getCompositeId(), embeddedId, bootEntityInfo, identifierProperty );
			mappingXmlEntity.getAttributes().setEmbeddedIdAttribute( embeddedId );
		}
	}

	private void transferSimpleId(
			JaxbHbmSimpleIdType source,
			JaxbIdImpl target,
			Property identifierProperty,
			BasicValue basicValue) {
		target.setName( source.getName() );
		target.setAttributeAccessor( source.getAccess() );

		applyBasicTypeMapping( basicValue, target, source.getTypeAttribute(), source.getType(), null,null );

		final JaxbHbmGeneratorSpecificationType hbmGenerator = source.getGenerator();
		if ( hbmGenerator != null && !"assigned".equals( hbmGenerator.getClazz() ) ) {
			final JaxbGeneratedValueImpl jaxbGeneratedValue = new JaxbGeneratedValueImpl();
			target.setGeneratedValue( jaxbGeneratedValue );

			final JaxbGenericIdGeneratorImpl generator = new JaxbGenericIdGeneratorImpl();
			target.setGenericGenerator( generator );
			generator.setClazz( hbmGenerator.getClazz() );

			final List<JaxbHbmConfigParameterType> hbmConfigParameters = hbmGenerator.getConfigParameters();
			for ( int i = 0; i < hbmConfigParameters.size(); i++ ) {
				final JaxbHbmConfigParameterType hbmConfigParameter = hbmConfigParameters.get( i );
				final JaxbConfigurationParameterImpl jaxbParam = new JaxbConfigurationParameterImpl();
				generator.getParameters().add( jaxbParam );
				jaxbParam.setName( hbmConfigParameter.getName() );
				jaxbParam.setValue( hbmConfigParameter.getValue() );
			}
		}

		target.setUnsavedValue( source.getUnsavedValue() );

		target.setColumn( new JaxbColumnImpl() );
		transferColumn(
				identifierProperty.getColumns().get( 0 ),
				new TargetColumnAdapterJaxbColumn( target.getColumn(), ColumnDefaultsInsertableNonUpdateableImpl.INSTANCE )
		);
	}

	private void transferEmbeddedId(
			JaxbHbmCompositeIdType hbmCompositeId,
			JaxbEmbeddedIdImpl jaxbEmbeddedId,
			EntityTypeInfo bootEntityInfo,
			Property identifierProperty) {
		final JaxbEmbeddableImpl jaxbEmbeddable = transformEmbeddedIdEmbeddable(
				hbmCompositeId,
				bootEntityInfo,
				identifierProperty
		);

		jaxbEmbeddedId.setName( identifierProperty.getName() );
		transferAccess(
				hbmCompositeId.getAccess(),
				jaxbEmbeddedId::setAccess,
				jaxbEmbeddedId::setAttributeAccessor
		);
		jaxbEmbeddedId.setTarget( jaxbEmbeddable.getName() );
	}

	private void transferAccess(
			String hbmAccess,
			Consumer<AccessType> accessTypeConsumer,
			Consumer<String> propertyAccessConsumer) {
		if ( AccessType.PROPERTY.name().equalsIgnoreCase( hbmAccess ) ) {
			accessTypeConsumer.accept( AccessType.PROPERTY );
		}
		else if ( AccessType.FIELD.name().equals( hbmAccess ) ) {
			accessTypeConsumer.accept( AccessType.FIELD );
		}
		else {
			if ( propertyAccessConsumer != null ) {
				propertyAccessConsumer.accept( hbmAccess );
			}
		}
	}

	private JaxbEmbeddableImpl transformEmbeddedIdEmbeddable(
			JaxbHbmCompositeIdType hbmCompositeId,
			EntityTypeInfo bootEntityInfo,
			Property idProperty) {
		final String embeddableClassName = hbmCompositeId.getClazz();
		if ( isNotEmpty( embeddableClassName ) ) {
			final JaxbEmbeddableImpl existing = jaxbEmbeddableByClassName.get( embeddableClassName );
			if ( existing != null ) {
				return existing;
			}
		}

		final String role = bootEntityInfo.getPersistentClass().getEntityName() + "." + hbmCompositeId.getName();
		final String embeddableName = determineEmbeddableName( embeddableClassName, hbmCompositeId.getName() );
		final ComponentTypeInfo componentTypeInfo = transformationState.getEmbeddableInfoByRole().get( role );
		final JaxbEmbeddableImpl created = transferEmbeddedIdEmbeddable(
				role,
				embeddableName,
				embeddableClassName,
				componentTypeInfo,
				hbmCompositeId
		);
		mappingXmlBinding.getRoot().getEmbeddables().add( created );
		return created;
	}

	private JaxbEmbeddableImpl transferEmbeddedIdEmbeddable(
			String role,
			String embeddableName,
			String embeddableClassName,
			ComponentTypeInfo componentTypeInfo,
			JaxbHbmCompositeIdType hbmCompositeId) {
		final JaxbEmbeddableImpl jaxbEmbeddable = new JaxbEmbeddableImpl();

		jaxbEmbeddable.setName( embeddableName );
		jaxbEmbeddable.setClazz( embeddableClassName );
		jaxbEmbeddable.setMetadataComplete( true );

		transferAccess(
				hbmCompositeId.getAccess(),
				jaxbEmbeddable::setAccess,
				null
		);

		if ( jaxbEmbeddable.getAttributes() == null ) {
			jaxbEmbeddable.setAttributes( new JaxbEmbeddableAttributesContainerImpl() );
		}

		hbmCompositeId.getKeyPropertyOrKeyManyToOne().forEach( (hbmIdProperty) -> {
			if ( hbmIdProperty instanceof JaxbHbmCompositeKeyBasicAttributeType hbmKeyProperty ) {
				final PropertyInfo keyPropertyInfo = componentTypeInfo.propertyInfoMap().get( hbmKeyProperty.getName() );
				jaxbEmbeddable.getAttributes().getBasicAttributes().add( transformCompositeKeyProperty(
						hbmKeyProperty,
						keyPropertyInfo
				) );
			}
			else if ( hbmIdProperty instanceof JaxbHbmCompositeKeyManyToOneType hbmKeyManyToOne ) {
				final PropertyInfo keyPropertyInfo = componentTypeInfo.propertyInfoMap().get( hbmKeyManyToOne.getName() );
				jaxbEmbeddable.getAttributes().getManyToOneAttributes().add( transformCompositeKeyManyToOne(
						hbmKeyManyToOne,
						keyPropertyInfo
				) );

			}
			else {
				throw new AssertionFailure( "Unexpected" );
			}
		} );

		return jaxbEmbeddable;
	}

	private JaxbBasicImpl transformCompositeKeyProperty(
			JaxbHbmCompositeKeyBasicAttributeType hbmKeyProperty,
			PropertyInfo keyPropertyInfo) {
		final JaxbBasicImpl jaxbBasic = new JaxbBasicImpl();

		jaxbBasic.setName( hbmKeyProperty.getName() );
		jaxbBasic.setOptional( false );
		jaxbBasic.setFetch( FetchType.EAGER );
		transferAccess(
				hbmKeyProperty.getAccess(),
				jaxbBasic::setAccess,
				jaxbBasic::setAttributeAccessor
		);

		applyBasicTypeMapping(
				(BasicValue) keyPropertyInfo.bootModelProperty().getValue(),
				jaxbBasic,
				hbmKeyProperty.getTypeAttribute(),
				hbmKeyProperty.getType(),
				jaxbBasic::setEnumerated,
				basicValueConverter -> {
					if ( basicValueConverter instanceof JpaAttributeConverter<?, ?> jpaAttributeConverter ) {
						final JaxbConvertImpl jaxbConvert = new JaxbConvertImpl();
						jaxbConvert.setConverter( jpaAttributeConverter.getConverterJavaType().getTypeName() );
						jaxbBasic.setConvert( jaxbConvert );
					}
				}
		);

		transferColumnsAndFormulas(
				new ColumnAndFormulaSource() {
					@Override
					public String getColumnAttribute() {
						return hbmKeyProperty.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return null;
					}

					@Override
					public List<Serializable> getColumnOrFormula() {
						return Collections.emptyList();
					}

					@Override
					public SourceColumnAdapter wrap(Serializable column) {
						return new SourceColumnAdapterJaxbHbmColumnType( (JaxbHbmColumnType) column );
					}
				},
				new ColumnAndFormulaTarget() {
					@Override
					public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
						return new TargetColumnAdapterJaxbColumn( columnDefaults );
					}

					@Override
					public void addColumn(TargetColumnAdapter column) {
						jaxbBasic.setColumn( ( (TargetColumnAdapterJaxbColumn) column ).getTargetColumn() );
					}

					@Override
					public void addFormula(String formula) {
						jaxbBasic.setFormula( formula );
					}
				},
				new ColumnDefaults() {
					@Override
					public Boolean isNullable() {
						return false;
					}

					@Override
					public Integer getLength() {
						return hbmKeyProperty.getLength();
					}

					@Override
					public Integer getScale() {
						return null;
					}

					@Override
					public Integer getPrecision() {
						return null;
					}

					@Override
					public Boolean isUnique() {
						return false;
					}

					@Override
					public Boolean isInsertable() {
						return true;
					}

					@Override
					public Boolean isUpdatable() {
						return true;
					}
				},
				null
		);

		return jaxbBasic;
	}

	private JaxbManyToOneImpl transformCompositeKeyManyToOne(
			JaxbHbmCompositeKeyManyToOneType hbmKeyManyToOne,
			PropertyInfo keyManyToOneInfo) {
		final JaxbManyToOneImpl jaxbKyManyToOne = new JaxbManyToOneImpl();

		jaxbKyManyToOne.setName( hbmKeyManyToOne.getName() );
		if ( isNotEmpty( hbmKeyManyToOne.getEntityName() ) ) {
			jaxbKyManyToOne.setTargetEntity( hbmKeyManyToOne.getEntityName() );
		}
		else {
			jaxbKyManyToOne.setTargetEntity( hbmKeyManyToOne.getClazz() );
		}

		jaxbKyManyToOne.setOptional( false );
		jaxbKyManyToOne.setFetch( FetchType.EAGER );
		jaxbKyManyToOne.setFetchMode( JaxbSingularFetchModeImpl.SELECT );
		jaxbKyManyToOne.setNotFound( NotFoundAction.EXCEPTION );

		transferAccess(
				hbmKeyManyToOne.getAccess(),
				jaxbKyManyToOne::setAccess,
				jaxbKyManyToOne::setAttributeAccessor
		);

		transferColumnsAndFormulas(
				new ColumnAndFormulaSource() {
					@Override
					public String getColumnAttribute() {
						return hbmKeyManyToOne.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return null;
					}

					@Override
					public List<Serializable> getColumnOrFormula() {
						return Collections.emptyList();
					}

					@Override
					public SourceColumnAdapter wrap(Serializable column) {
						return new SourceColumnAdapterJaxbHbmColumnType( (JaxbHbmColumnType) column );
					}
				},
				new ColumnAndFormulaTarget() {
					@Override
					public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
						return new TargetColumnAdapterJaxbJoinColumn( columnDefaults );
					}

					@Override
					public void addColumn(TargetColumnAdapter column) {
						jaxbKyManyToOne.getJoinColumnOrJoinFormula()
								.add( ( (TargetColumnAdapterJaxbJoinColumn) column ).getTargetColumn() );
					}

					@Override
					public void addFormula(String formula) {
						jaxbKyManyToOne.getJoinColumnOrJoinFormula().add( formula );
					}
				},
				ColumnDefaultsBasicImpl.INSTANCE,
				null
		);

		jaxbKyManyToOne.setForeignKey( transformForeignKey( hbmKeyManyToOne.getForeignKey() ) );

		return jaxbKyManyToOne;
	}

	private JaxbForeignKeyImpl transformForeignKey(String hbmForeignKeyName) {
		if ( isEmpty( hbmForeignKeyName ) ) {
			return null;
		}

		final JaxbForeignKeyImpl jaxbForeignKey = new JaxbForeignKeyImpl();
		if ( "none".equalsIgnoreCase( hbmForeignKeyName ) ) {
			jaxbForeignKey.setConstraintMode( ConstraintMode.NO_CONSTRAINT );
		}
		else {
			jaxbForeignKey.setName( hbmForeignKeyName );
		}
		return jaxbForeignKey;
	}

	private void transferNonAggregatedCompositeId(
			JaxbHbmRootEntityType hbmEntity,
			JaxbEntityImpl mappingXmlEntity,
			EntityTypeInfo bootEntityInfo,
			RootClass rootClass) {
		final JaxbHbmCompositeIdType hbmCompositeId = hbmEntity.getCompositeId();

		final Component idClassMapping = rootClass.getIdentifierMapper();
		if ( idClassMapping != null ) {
			transferIdClass( hbmCompositeId, idClassMapping, mappingXmlEntity );
		}

		final String idRole = rootClass.getEntityName() + ".id";
		final ComponentTypeInfo componentTypeInfo = transformationState.getEmbeddableInfoByRole().get( idRole );
		assert componentTypeInfo != null;

		hbmCompositeId.getKeyPropertyOrKeyManyToOne().forEach( (hbmIdProperty) -> {
			if ( hbmIdProperty instanceof JaxbHbmCompositeKeyBasicAttributeType hbmKeyProperty ) {
				final PropertyInfo keyPropertyInfo = componentTypeInfo.propertyInfoMap().get( hbmKeyProperty.getName() );
				mappingXmlEntity.getAttributes().getIdAttributes().add( transformNonAggregatedKeyProperty(
						hbmKeyProperty,
						keyPropertyInfo
				) );
			}
			else if ( hbmIdProperty instanceof JaxbHbmCompositeKeyManyToOneType ) {
				handleUnsupported( "Transformation of <key-many-to-one/> not supported" );
			}
			else {
				throw new AssertionFailure( "Unexpected non-aggregated composite id property kind : " + hbmIdProperty );
			}
		} );
	}

	private void transferIdClass(
			JaxbHbmCompositeIdType hbmCompositeId,
			Component idClassMapping,
			JaxbEntityImpl mappingXmlEntity) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	private JaxbIdImpl transformNonAggregatedKeyProperty(
			JaxbHbmCompositeKeyBasicAttributeType hbmIdProperty,
			PropertyInfo idPropertyInfo) {
		final JaxbIdImpl jaxbBasic = new JaxbIdImpl();
		jaxbBasic.setName( hbmIdProperty.getName() );
		transferAccess(
				hbmIdProperty.getAccess(),
				jaxbBasic::setAccess,
				jaxbBasic::setAttributeAccessor
		);

		applyBasicTypeMapping(
				(BasicValue) idPropertyInfo.bootModelProperty().getValue(),
				jaxbBasic,
				hbmIdProperty.getTypeAttribute(),
				hbmIdProperty.getType(),
				null,
				basicValueConverter -> {
					throw new UnsupportedOperationException( "Conversion of id attributes not supported" );
				}
		);

		transferColumnsAndFormulas(
				new ColumnAndFormulaSource() {
					@Override
					public String getColumnAttribute() {
						return hbmIdProperty.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return null;
					}

					@Override
					public List<Serializable> getColumnOrFormula() {
						//noinspection unchecked,rawtypes
						return (List) hbmIdProperty.getColumn();
					}

					@Override
					public SourceColumnAdapter wrap(Serializable column) {
						return new SourceColumnAdapterJaxbHbmColumnType( (JaxbHbmColumnType) column );
					}
				},
				new ColumnAndFormulaTarget() {
					@Override
					public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
						return new TargetColumnAdapterJaxbColumn( columnDefaults );
					}

					@Override
					public void addColumn(TargetColumnAdapter column) {
						jaxbBasic.setColumn( ( (TargetColumnAdapterJaxbColumn) column ).getTargetColumn() );
					}

					@Override
					public void addFormula(String formula) {
						handleUnsupported( "formula not supported in id mappings" );
					}
				},
				new ColumnDefaults() {
					@Override
					public Boolean isNullable() {
						return false;
					}

					@Override
					public Integer getLength() {
						return hbmIdProperty.getLength();
					}

					@Override
					public Integer getScale() {
						return null;
					}

					@Override
					public Integer getPrecision() {
						return null;
					}

					@Override
					public Boolean isUnique() {
						return true;
					}

					@Override
					public Boolean isInsertable() {
						return true;
					}

					@Override
					public Boolean isUpdatable() {
						return true;
					}
				},
				null
		);

		return jaxbBasic;
	}


	private void transferNaturalIdentifiers(
			JaxbHbmRootEntityType source,
			JaxbEntityImpl target,
			EntityTypeInfo bootEntityInfo,
			RootClass rootClass) {
		if ( source.getNaturalId() == null ) {
			return;
		}

		final JaxbNaturalIdImpl naturalId = new JaxbNaturalIdImpl();
		transferBaseAttributes(
				rootClass.getEntityName(),
				source.getNaturalId().getAttributes(),
				bootEntityInfo,
				new JaxbAttributesContainer() {
					@Override
					public List<JaxbBasicImpl> getBasicAttributes() {
						return naturalId.getBasicAttributes();
					}

					@Override
					public List<JaxbEmbeddedImpl> getEmbeddedAttributes() {
						return naturalId.getEmbeddedAttributes();
					}

					@Override
					public List<JaxbOneToOneImpl> getOneToOneAttributes() {
						return null;
					}

					@Override
					public List<JaxbManyToOneImpl> getManyToOneAttributes() {
						return naturalId.getManyToOneAttributes();
					}

					@Override
					public List<JaxbAnyMappingImpl> getAnyMappingAttributes() {
						return naturalId.getAnyMappingAttributes();
					}

					@Override
					public List<JaxbElementCollectionImpl> getElementCollectionAttributes() {
						return null;
					}

					@Override
					public List<JaxbOneToManyImpl> getOneToManyAttributes() {
						return null;
					}

					@Override
					public List<JaxbManyToManyImpl> getManyToManyAttributes() {
						return null;
					}

					@Override
					public List<JaxbPluralAnyMappingImpl> getPluralAnyMappingAttributes() {
						return null;
					}

					@Override
					public List<JaxbTransientImpl> getTransients() {
						return null;
					}
				}
		);

		naturalId.setMutable( source.getNaturalId().isMutable() );
		target.getAttributes().setNaturalId( naturalId );
	}

	private void transferVersion(
			JaxbHbmRootEntityType source,
			JaxbEntityImpl target,
			EntityTypeInfo bootEntityInfo,
			RootClass rootClass) {
		final JaxbHbmVersionAttributeType hbmVersion = source.getVersion();
		final JaxbHbmTimestampAttributeType hbmTimestamp = source.getTimestamp();

		if ( hbmVersion != null ) {
			final JaxbVersionImpl version = new JaxbVersionImpl();
			version.setName( hbmVersion.getName() );
			if ( isNotEmpty( hbmVersion.getColumnAttribute() ) ) {
				version.setColumn( new JaxbColumnImpl() );
				version.getColumn().setName( hbmVersion.getColumnAttribute() );
			}
			target.getAttributes().setVersion( version );
		}
		else if ( hbmTimestamp != null ) {
			final JaxbVersionImpl version = new JaxbVersionImpl();
			version.setName( hbmTimestamp.getName() );
			// TODO: multiple columns?
			if ( isNotEmpty( hbmTimestamp.getColumnAttribute() ) ) {
				version.setColumn( new JaxbColumnImpl() );
				version.getColumn().setName( hbmTimestamp.getColumnAttribute() );
			}
			//noinspection deprecation
			version.setTemporal( TemporalType.TIMESTAMP );
			target.getAttributes().setVersion( version );
		}
	}

	private void transferJoins(
			JaxbHbmRootEntityType hbmEntity,
			JaxbEntityImpl mappingEntity,
			EntityTypeInfo bootEntityInfo) {
		for ( JaxbHbmSecondaryTableType hbmJoin : hbmEntity.getJoin() ) {
			transferSecondaryTable( hbmJoin, mappingEntity );

			for ( Serializable hbmProperty : hbmJoin.getAttributes() ) {
				if ( hbmProperty instanceof JaxbHbmBasicAttributeType hbmBasicAttribute) {
					final PropertyInfo propertyInfo = bootEntityInfo.propertyInfoMap().get( hbmBasicAttribute.getName() );
					final JaxbBasicImpl prop = transformBasicAttribute( hbmBasicAttribute, propertyInfo );
					if ( prop.getColumn() == null && prop.getFormula() == null ) {
						prop.setColumn( new JaxbColumnImpl() );
						prop.getColumn().setTable( propertyInfo.bootModelProperty().getValue().getTable().getName() );
					}
					mappingEntity.getAttributes().getBasicAttributes().add( prop );
				}
				else if ( hbmProperty instanceof JaxbHbmCompositeAttributeType hbmComponent ) {
					final String componentRole = bootEntityInfo.getPersistentClass().getEntityName() + "." + hbmComponent.getName();
					final ComponentTypeInfo componentTypeInfo = transformationState.getEmbeddableInfoByRole().get( componentRole );
					final JaxbEmbeddableImpl jaxbEmbeddable = applyEmbeddable(
							bootEntityInfo.getPersistentClass().getEntityName(),
							hbmComponent,
							componentTypeInfo
					);
					mappingEntity.getAttributes().getEmbeddedAttributes().add( transformEmbedded( jaxbEmbeddable, hbmComponent ) );
				}
				else if ( hbmProperty instanceof JaxbHbmManyToOneType hbmManyToOne ) {
					final PropertyInfo propertyInfo = bootEntityInfo.propertyInfoMap().get( hbmManyToOne.getName() );
					final JaxbManyToOneImpl jaxbManyToOne = transformManyToOne( hbmManyToOne, propertyInfo );
					mappingEntity.getAttributes().getManyToOneAttributes().add( jaxbManyToOne );
				}
				else if ( hbmProperty instanceof JaxbHbmAnyAssociationType ) {
					throw new MappingException(
							"transformation of <any/> as part of <join/> (secondary-table) not yet implemented",
							origin()
					);
				}
				else if ( hbmProperty instanceof JaxbHbmDynamicComponentType ) {
					handleUnsupportedContent( "<dynamic-component/> mappings not supported" );
				}
			}
		}
	}

	private void transferSecondaryTable(JaxbHbmSecondaryTableType hbmJoin, JaxbEntityImpl mappingEntity) {
		final JaxbSecondaryTableImpl secondaryTable = new JaxbSecondaryTableImpl();
		secondaryTable.setCatalog( hbmJoin.getCatalog() );
		secondaryTable.setComment( hbmJoin.getComment() );
		secondaryTable.setName( hbmJoin.getTable() );
		secondaryTable.setSchema( hbmJoin.getSchema() );
		secondaryTable.setOptional( hbmJoin.isOptional() );
		secondaryTable.setOwned( !hbmJoin.isInverse() );
		final JaxbHbmKeyType key = hbmJoin.getKey();
		if ( key != null ) {
			final JaxbPrimaryKeyJoinColumnImpl joinColumn = new JaxbPrimaryKeyJoinColumnImpl();
			joinColumn.setName( key.getColumnAttribute() );
			secondaryTable.getPrimaryKeyJoinColumn().add( joinColumn );

			joinColumn.setForeignKey( transformForeignKey( key.getForeignKey() ) );
		}
		mappingEntity.getSecondaryTables().add( secondaryTable );
	}


	// ToOne
	private void transferFetchable(
			JaxbHbmLazyWithNoProxyEnum hbmLazy,
			JaxbHbmFetchStyleEnum hbmFetch,
			JaxbHbmOuterJoinEnum hbmOuterJoin,
			Boolean constrained,
			JaxbSingularAssociationAttribute fetchable) {
		FetchType laziness = FetchType.LAZY;
		JaxbSingularFetchModeImpl fetch = JaxbSingularFetchModeImpl.SELECT;

		if (hbmLazy != null) {
			if (hbmLazy.equals( JaxbHbmLazyWithNoProxyEnum.FALSE )) {
				laziness = FetchType.EAGER;
			}
			else if (hbmLazy.equals( JaxbHbmLazyWithNoProxyEnum.NO_PROXY )) {
				// TODO: @LazyToOne(LazyToOneOption.PROXY) or @LazyToOne(LazyToOneOption.NO_PROXY)
			}
		}

		// allow fetch style to override laziness, if necessary
		if (constrained != null && ! constrained) {
			// NOTE SPECIAL CASE: one-to-one constrained=false cannot be proxied, so default to join and non-lazy
			laziness = FetchType.EAGER;
			fetch = JaxbSingularFetchModeImpl.JOIN;
		}
		else {
			if (hbmFetch == null) {
				if (hbmOuterJoin != null && hbmOuterJoin.equals( JaxbHbmOuterJoinEnum.TRUE ) ) {
					laziness = FetchType.EAGER;
					fetch = JaxbSingularFetchModeImpl.JOIN;
				}
			}
			else {
				if (hbmFetch.equals( JaxbHbmFetchStyleEnum.JOIN ) ) {
					laziness = FetchType.EAGER;
					fetch = JaxbSingularFetchModeImpl.JOIN;
				}
			}
		}

		fetchable.setFetch( laziness );
		fetchable.setFetchMode( fetch );
	}

	// ToMany
	private void transferFetchable(
			JaxbHbmLazyWithExtraEnum hbmLazy,
			JaxbHbmFetchStyleWithSubselectEnum hbmFetch,
			JaxbHbmOuterJoinEnum hbmOuterJoin,
			JaxbPluralAttribute fetchable) {
		FetchType laziness = FetchType.LAZY;
		JaxbPluralFetchModeImpl fetch = JaxbPluralFetchModeImpl.SELECT;

		if (hbmLazy != null) {
			if (hbmLazy.equals( JaxbHbmLazyWithExtraEnum.EXTRA )) {
				throw new MappingException( "HBM transformation: extra lazy not yet supported.", origin() );
			}
			else if (hbmLazy.equals( JaxbHbmLazyWithExtraEnum.FALSE )) {
				laziness = FetchType.EAGER;
			}
		}

		// allow fetch style to override laziness, if necessary
		if (hbmFetch == null) {
			if (hbmOuterJoin != null && hbmOuterJoin.equals( JaxbHbmOuterJoinEnum.TRUE ) ) {
				laziness = FetchType.EAGER;
				fetch = JaxbPluralFetchModeImpl.JOIN;
			}
		}
		else {
			if (hbmFetch.equals( JaxbHbmFetchStyleWithSubselectEnum.JOIN ) ) {
				laziness = FetchType.EAGER;
				fetch = JaxbPluralFetchModeImpl.JOIN;
			}
			else if (hbmFetch.equals( JaxbHbmFetchStyleWithSubselectEnum.SUBSELECT ) ) {
				fetch = JaxbPluralFetchModeImpl.SUBSELECT;
			}
		}

		fetchable.setFetch( laziness );
		fetchable.setFetchMode( fetch );
	}

	// KeyManyToOne
	private static FetchType convert(JaxbHbmLazyEnum hbmLazy) {
		if ( hbmLazy != null && "false".equalsIgnoreCase( hbmLazy.value() ) ) {
			return FetchType.EAGER;
		}
		else {
			// proxy is HBM default
			return FetchType.LAZY;
		}
	}

	private static OnDeleteAction convert(JaxbHbmOnDeleteEnum hbmOnDelete) {
		return hbmOnDelete == JaxbHbmOnDeleteEnum.CASCADE ? OnDeleteAction.CASCADE : OnDeleteAction.NO_ACTION;
	}

	private static JaxbFilterImpl convert(JaxbHbmFilterType hbmFilter) {
		final JaxbFilterImpl filter = new JaxbFilterImpl();
		filter.setName( hbmFilter.getName() );

		final boolean shouldAutoInjectAliases = hbmFilter.getAutoAliasInjection() == null
				|| hbmFilter.getAutoAliasInjection().equalsIgnoreCase( "true" );

		filter.setAutoAliasInjection( shouldAutoInjectAliases );
		filter.setCondition( hbmFilter.getCondition() );

		for ( Serializable content : hbmFilter.getContent() ) {
			if ( content instanceof String string ) {
				filter.setCondition( string );
			}
			else {
				final JaxbHbmFilterAliasMappingType hbmAliasMapping = (JaxbHbmFilterAliasMappingType) content;
				final JaxbFilterImpl.JaxbAliasesImpl aliasMapping = new JaxbFilterImpl.JaxbAliasesImpl();
				aliasMapping.setAlias( hbmAliasMapping.getAlias() );
				aliasMapping.setEntity( hbmAliasMapping.getEntity() );
				aliasMapping.setTable( hbmAliasMapping.getTable() );
				filter.getAliases().add( aliasMapping );
			}
		}

		return filter;
	}

	private static JaxbCascadeTypeImpl convertCascadeType(String s) {
		final JaxbCascadeTypeImpl cascadeType = new JaxbCascadeTypeImpl();

		if ( isNotEmpty( s ) ) {
			s = s.toLowerCase( Locale.ROOT ).replaceAll( " ", "" );
			final String[] split = StringHelper.split( ",", s );
			for ( String hbmCascade : split ) {
				if ( hbmCascade.contains( "all" ) ) {
					cascadeType.setCascadeAll( new JaxbEmptyTypeImpl() );
				}
				if ( hbmCascade.contains( "persist" ) ) {
					cascadeType.setCascadePersist( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "merge" ) ) {
					cascadeType.setCascadeMerge( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "refresh" ) ) {
					cascadeType.setCascadeRefresh( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "save-update" ) ) {
					cascadeType.setCascadeMerge( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "evict" ) || hbmCascade.contains( "detach" ) ) {
					cascadeType.setCascadeDetach( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "replicate" ) ) {
					cascadeType.setCascadeReplicate( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "lock" ) ) {
					cascadeType.setCascadeLock( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "delete" ) ) {
					cascadeType.setCascadeRemove( new JaxbEmptyTypeImpl() );
				}
			}
		}
		return cascadeType;
	}

	private boolean isOrphanRemoval(String s) {
		return isNotEmpty( s )
				&& s.toLowerCase( Locale.ROOT ).contains( "orphan" );
	}

	private String getFullyQualifiedClassName(String className) {
		// todo : right now we do both, we set the package into the XML and qualify the names; pick one...
		//		1) pass the names through as-is and set the package into the XML; the orm.xml reader
		//			would apply the package as needed
		//		2) qualify the name that we write into the XML, but the do not set the package into the XML;
		//			if going this route, would be better to leverage the normal hierarchical lookup for package
		// 			names which would mean passing along MappingDefaults (or maybe even the full "binding context")

		final String defaultPackageName = mappingXmlBinding.getRoot().getPackage();
		if ( isNotEmpty( className )
				&& className.indexOf( '.' ) < 0
				&& isNotEmpty( defaultPackageName ) ) {
			className = StringHelper.qualify( defaultPackageName, className );
		}
		return className;
	}


	private void handleUnsupportedContent(String description) {
		handleUnsupported(
				"Transformation of hbm.xml `%s` encountered unsupported content : %s",
				origin().toString(),
				description
		);
	}

	private Origin origin() {
		return hbmXmlBinding.getOrigin();
	}

	private void handleUnsupported(String message, Object... messageArgs) {
		handleUnsupported(
				null,
				message,
				messageArgs
		);
	}

	@FunctionalInterface
	private interface PickHandler {
		void handlePick(String message, Object... messageArgs);
	}

	private void handleUnsupported(PickHandler pickHandler, String message, Object... messageArgs) {
		switch ( unsupportedFeatureHandling ) {
			case ERROR -> throw new UnsupportedOperationException(
					String.format(
							Locale.ROOT,
							message,
							messageArgs
					)  + " (" + origin().getName() + " [" + origin().getType() + "]" + ")"
			);
			case PICK -> {
				if ( pickHandler != null ) {
					pickHandler.handlePick( message, messageArgs );
				}
			}
			case IGNORE -> TRANSFORMATION_LOGGER.debugf( message, messageArgs );
			case WARN -> TRANSFORMATION_LOGGER.warnf( message, messageArgs );
		}
	}

}
