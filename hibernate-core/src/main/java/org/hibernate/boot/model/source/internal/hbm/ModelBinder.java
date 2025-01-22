/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.sql.Types;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SourceType;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.internal.FkSecondPass;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitCollectionTableNameSource;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitIdentifierColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitIndexColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitMapKeyColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.source.internal.ImplicitColumnNamingSecondPass;
import org.hibernate.boot.model.source.spi.*;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.InFlightMetadataCollector.EntityTableXref;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.NaturalIdUniqueKeyBinder;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.generator.internal.GeneratedGeneration;
import org.hibernate.generator.internal.SourceGeneration;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.AttributeContainer;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.SortableValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.mapping.Value;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import static org.hibernate.boot.model.internal.GeneratorBinder.makeIdGenerator;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.boot.model.source.internal.hbm.Helper.reflectedPropertyClass;
import static org.hibernate.boot.model.source.internal.hbm.NamedQueryBinder.processNamedNativeQuery;
import static org.hibernate.boot.model.source.internal.hbm.NamedQueryBinder.processNamedQuery;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.StringHelper.getNonEmptyOrConjunctionIfBothNonEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.EMBEDDED;

/**
 * Responsible for coordinating the binding of all information inside entity tags ({@code <class/>}, etc).
 *
 * @author Steve Ebersole
 */
public class ModelBinder {

	private final MetadataBuildingContext metadataBuildingContext;

	private final Database database;
	private final ImplicitNamingStrategy implicitNamingStrategy;
	private final RelationalObjectBinder relationalObjectBinder;

	public static ModelBinder prepare(MetadataBuildingContext context) {
		return new ModelBinder( context );
	}

	public ModelBinder(MetadataBuildingContext context) {
		this.metadataBuildingContext = context;
		this.database = context.getMetadataCollector().getDatabase();
		this.implicitNamingStrategy = context.getBuildingOptions().getImplicitNamingStrategy();
		this.relationalObjectBinder = new RelationalObjectBinder( context );
	}

	public void bindEntityHierarchy(EntityHierarchySourceImpl hierarchySource) {
		final var rootEntityDescriptor =
				new RootClass( hierarchySource.getRootEntityMappingDocument() );
		bindRootEntity( hierarchySource, rootEntityDescriptor );
		final var root = hierarchySource.getRoot();
		root.getLocalMetadataBuildingContext().getMetadataCollector()
				.addEntityBinding( rootEntityDescriptor );

		switch ( hierarchySource.getHierarchyInheritanceType() ) {
			case NO_INHERITANCE:
				// nothing to do
				break;
			case DISCRIMINATED:
				bindDiscriminatorSubclassEntities( root, rootEntityDescriptor );
				break;
			case JOINED:
				bindJoinedSubclassEntities( root, rootEntityDescriptor );
				break;
			case UNION:
				bindUnionSubclassEntities( root, rootEntityDescriptor );
				break;
		}
	}

	private void bindRootEntity(EntityHierarchySourceImpl hierarchySource, RootClass rootEntityDescriptor) {
		final var root = hierarchySource.getRoot();
		final var mappingDocument = root.sourceMappingDocument();

		bindBasicEntityValues( mappingDocument, root, rootEntityDescriptor );

		final var primaryTable = bindEntityTableSpecification(
				mappingDocument,
				root.getPrimaryTable(),
				null,
				root,
				rootEntityDescriptor
		);

		rootEntityDescriptor.setTable( primaryTable );
		if ( BOOT_LOGGER.isTraceEnabled() ) {
			BOOT_LOGGER.mappingClassToTable(
					rootEntityDescriptor.getEntityName(),
					primaryTable.getName() );
		}

		rootEntityDescriptor.setOptimisticLockStyle( hierarchySource.getOptimisticLockStyle() );
		rootEntityDescriptor.setMutable( hierarchySource.isMutable() );
		rootEntityDescriptor.setWhere( hierarchySource.getWhere() );

		if ( hierarchySource.isExplicitPolymorphism() ) {
			DEPRECATION_LOGGER.warn( "Implicit/explicit polymorphism no longer supported" );
		}

		bindEntityIdentifier( mappingDocument, hierarchySource, rootEntityDescriptor );

		if ( hierarchySource.getVersionAttributeSource() != null ) {
			bindEntityVersion( mappingDocument, hierarchySource, rootEntityDescriptor );
		}

		if ( hierarchySource.getDiscriminatorSource() != null ) {
			bindEntityDiscriminator( mappingDocument, hierarchySource, rootEntityDescriptor );
		}

		applyCaching( mappingDocument, hierarchySource.getCaching(), rootEntityDescriptor );

		if ( rootEntityDescriptor.getIdentifier() instanceof SortableValue sortableValue ) {
			sortableValue.sortProperties();
		}
		// Primary key constraint
		rootEntityDescriptor.createPrimaryKey();

		bindAllEntityAttributes( mappingDocument, root, rootEntityDescriptor );

		final var naturalIdCaching = hierarchySource.getNaturalIdCaching();
		if ( naturalIdCaching != null && naturalIdCaching.isRequested() ) {
			rootEntityDescriptor.setNaturalIdCacheRegionName( naturalIdCaching.getRegion() );
		}
	}

	private void applyCaching(MappingDocument mappingDocument, Caching caching, RootClass rootEntityDescriptor) {
		if ( isCacheEnabled( mappingDocument, caching ) ) {
			rootEntityDescriptor.setCacheConcurrencyStrategy( getConcurrencyStrategy( mappingDocument, caching ).getExternalName() );
			rootEntityDescriptor.setCacheRegionName( caching == null ? null : caching.getRegion() );
			rootEntityDescriptor.setLazyPropertiesCacheable( caching == null || caching.isCacheLazyProperties() );
			rootEntityDescriptor.setCached( true );
		}
	}

	private static AccessType getConcurrencyStrategy(MappingDocument mappingDocument, Caching caching) {
		return caching == null || caching.getAccessType() == null
				? mappingDocument.getBuildingOptions().getImplicitCacheAccessType()
				: caching.getAccessType();
	}

	private static boolean isCacheEnabled(MappingDocument mappingDocument, Caching caching) {
		return switch ( mappingDocument.getBuildingOptions().getSharedCacheMode() ) {
			case UNSPECIFIED, ENABLE_SELECTIVE ->
				// this is the default behavior for hbm.xml
					caching != null && caching.isRequested(false);
			case NONE ->
				// this option is actually really useful
					false;
			case ALL ->
				// goes completely against the whole ideology we have for
				// caching, and so it hurts me to support it here
					true;
			case DISABLE_SELECTIVE ->
				// makes no sense for hbm.xml, and also goes against our
				// ideology, and so it hurts me to support it here
					caching == null || caching.isRequested(true);
		};
	}

	private void bindEntityIdentifier(
			MappingDocument mappingDocument,
			EntityHierarchySourceImpl hierarchySource,
			RootClass rootEntityDescriptor) {
		switch ( hierarchySource.getIdentifierSource().getNature() ) {
			case SIMPLE:
				bindSimpleEntityIdentifier( mappingDocument, hierarchySource, rootEntityDescriptor );
				break;
			case AGGREGATED_COMPOSITE:
				bindAggregatedCompositeEntityIdentifier( mappingDocument, hierarchySource, rootEntityDescriptor );
				break;
			case NON_AGGREGATED_COMPOSITE:
				bindNonAggregatedCompositeEntityIdentifier( mappingDocument, hierarchySource, rootEntityDescriptor );
				break;
			default:
				throw new MappingException(
						String.format(
								Locale.ENGLISH,
								"Unexpected entity identifier nature [%s] for entity %s",
								hierarchySource.getIdentifierSource().getNature(),
								hierarchySource.getRoot().getEntityNamingSource().getEntityName()
						),
						mappingDocument.getOrigin()
				);
		}
	}

	private void bindBasicEntityValues(
			MappingDocument sourceDocument,
			EntitySource entitySource,
			PersistentClass entityDescriptor) {
		final var entityNamingSource = entitySource.getEntityNamingSource();
		final String entityName = entityNamingSource.getEntityName();

		entityDescriptor.setEntityName( entityName );
		entityDescriptor.setJpaEntityName( entityNamingSource.getJpaEntityName() );
		entityDescriptor.setClassName( entityNamingSource.getClassName() );
		String jpaEntityName = entityDescriptor.getJpaEntityName();
		String className = entityDescriptor.getClassName();
		if ( jpaEntityName != null && className != null ) {
			metadataBuildingContext.getMetadataCollector().addImport( jpaEntityName, className );
		}

		entityDescriptor.setDiscriminatorValue( discriminatorValue( entitySource ) );

		setupProxying( sourceDocument, entitySource, entityDescriptor );

		entityDescriptor.setAbstract( entitySource.isAbstract() );

		setupImports( sourceDocument, entityName );

		entityDescriptor.setDynamicInsert( entitySource.isDynamicInsert() );
		entityDescriptor.setDynamicUpdate( entitySource.isDynamicUpdate() );
		entityDescriptor.setBatchSize( entitySource.getBatchSize() );
		entityDescriptor.setSelectBeforeUpdate( entitySource.isSelectBeforeUpdate() );

		bindCustomSql( entitySource, entityDescriptor );

		for ( String tableName : entitySource.getSynchronizedTableNames() ) {
			entityDescriptor.addSynchronizedTable( physicalTableName( sourceDocument, tableName ) );
		}

		for ( var filterSource : entitySource.getFilterSources() ) {
			entityDescriptor.addFilter(
					filterSource.getName(),
					filterCondition( filterSource, sourceDocument ),
					filterSource.shouldAutoInjectAliases(),
					filterSource.getAliasToTableMap(),
					filterSource.getAliasToEntityMap()
			);
		}

		for ( var namedQuery : entitySource.getNamedQueries() ) {
			processNamedQuery( sourceDocument, namedQuery, entityName + "." );
		}
		for ( var namedQuery : entitySource.getNamedNativeQueries() ) {
			processNamedNativeQuery( sourceDocument, namedQuery, entityName + "." );
		}

		entityDescriptor.setMetaAttributes( entitySource.getToolingHintContext().getMetaAttributeMap() );
	}

	private static String discriminatorValue(EntitySource entitySource) {
		final String discriminatorMatchValue = entitySource.getDiscriminatorMatchValue();
		return discriminatorMatchValue == null
				? entitySource.getEntityNamingSource().getEntityName()
				: discriminatorMatchValue;
	}

	private static void setupImports(MappingDocument sourceDocument, String entityName) {
		final var metadataCollector = sourceDocument.getMetadataCollector();
		metadataCollector.addImport( entityName, entityName );
		if ( sourceDocument.getEffectiveDefaults().isDefaultAutoImport()
				&& entityName.indexOf( '.' ) > 0 ) {
			metadataCollector.addImport( unqualify( entityName ), entityName );
		}
	}

	private static void setupProxying(
			MappingDocument sourceDocument,
			EntitySource entitySource,
			PersistentClass entityDescriptor) {
		// NOTE: entitySource#isLazy already accounts for MappingDefaults#areEntitiesImplicitlyLazy
		if ( isNotEmpty( entitySource.getProxy() ) ) {
			entityDescriptor.setProxyInterfaceName( sourceDocument.qualifyClassName( entitySource.getProxy() ) );
			entityDescriptor.setLazy( true );
		}
		else if ( entitySource.isLazy() ) {
			entityDescriptor.setProxyInterfaceName( entityDescriptor.getClassName() );
			entityDescriptor.setLazy( true );
		}
		else {
			entityDescriptor.setProxyInterfaceName( null );
			entityDescriptor.setLazy( false );
		}
	}

	private static String filterCondition(FilterSource filterSource, MappingDocument sourceDocument) {
		final String condition = filterSource.getCondition();
		if ( condition == null ) {
			final var filterDefinition =
					sourceDocument.getMetadataCollector()
							.getFilterDefinition( filterSource.getName() );
			if ( filterDefinition != null ) {
				return filterDefinition.getDefaultFilterCondition();
			}
			else {
				return null;
			}
		}
		else {
			return condition;
		}
	}

	private String physicalTableName(MappingDocument sourceDocument, String tableName) {
		final var jdbcEnvironment = sourceDocument.getMetadataCollector().getDatabase().getJdbcEnvironment();
		return sourceDocument.getBuildingOptions().getPhysicalNamingStrategy()
				.toPhysicalTableName( jdbcEnvironment.getIdentifierHelper().toIdentifier( tableName ), jdbcEnvironment )
				.render( jdbcEnvironment.getDialect() );
	}

	private void bindDiscriminatorSubclassEntities(
			AbstractEntitySourceImpl entitySource,
			PersistentClass superEntityDescriptor) {
		for ( var subType : entitySource.getSubTypes() ) {
			final var subEntityDescriptor =
					new SingleTableSubclass( superEntityDescriptor, metadataBuildingContext );
			subEntityDescriptor.setCached( superEntityDescriptor.isCached() );
			bindDiscriminatorSubclassEntity( (SubclassEntitySourceImpl) subType, subEntityDescriptor );
			superEntityDescriptor.addSubclass( subEntityDescriptor );
			entitySource.getLocalMetadataBuildingContext().getMetadataCollector()
					.addEntityBinding( subEntityDescriptor );
		}
	}

	private void bindDiscriminatorSubclassEntity(
			SubclassEntitySourceImpl entitySource,
			SingleTableSubclass entityDescriptor) {

		final var sourceDocument = entitySource.sourceMappingDocument();
		final var localMetadataCollector = entitySource.getLocalMetadataBuildingContext().getMetadataCollector();

		bindBasicEntityValues( sourceDocument, entitySource, entityDescriptor );

		final var superEntityTableXref =
				superEntityTableXref( sourceDocument, entitySource, entityDescriptor, localMetadataCollector );

		localMetadataCollector.addEntityTableXref(
				entitySource.getEntityNamingSource().getEntityName(),
				database.toIdentifier( localMetadataCollector.getLogicalTableName( entityDescriptor.getTable() ) ),
				entityDescriptor.getTable(),
				superEntityTableXref
		);

		bindAllEntityAttributes( sourceDocument, entitySource, entityDescriptor );

		bindDiscriminatorSubclassEntities( entitySource, entityDescriptor );
	}

	private void bindJoinedSubclassEntities(
			AbstractEntitySourceImpl entitySource,
			PersistentClass superEntityDescriptor) {
		for ( var subType : entitySource.getSubTypes() ) {
			final var subEntityDescriptor =
					new JoinedSubclass( superEntityDescriptor, metadataBuildingContext );
			subEntityDescriptor.setCached( superEntityDescriptor.isCached() );
			bindJoinedSubclassEntity( (JoinedSubclassEntitySourceImpl) subType, subEntityDescriptor );
			superEntityDescriptor.addSubclass( subEntityDescriptor );
			entitySource.getLocalMetadataBuildingContext().getMetadataCollector()
					.addEntityBinding( subEntityDescriptor );
		}
	}

	private void bindJoinedSubclassEntity(
			JoinedSubclassEntitySourceImpl entitySource,
			JoinedSubclass entityDescriptor) {
		final var mappingDocument = entitySource.sourceMappingDocument();

		bindBasicEntityValues( mappingDocument, entitySource, entityDescriptor );

		final var primaryTable = bindEntityTableSpecification(
				mappingDocument,
				entitySource.getPrimaryTable(),
				null,
				entitySource,
				entityDescriptor
		);

		entityDescriptor.setTable( primaryTable );
		if ( BOOT_LOGGER.isTraceEnabled() ) {
			BOOT_LOGGER.mappingJoinedSubclassToTable(
					entityDescriptor.getEntityName(),
					primaryTable.getName() );
		}

		// KEY
		final var keyBinding =
				new DependantValue( mappingDocument, primaryTable, entityDescriptor.getIdentifier() );
		if ( mappingDocument.getBuildingOptions().useNationalizedCharacterData() ) {
			keyBinding.makeNationalized();
		}
		entityDescriptor.setKey( keyBinding );
		keyBinding.setOnDeleteAction( getOnDeleteAction( entitySource ) );
		relationalObjectBinder.bindColumns(
				mappingDocument,
				entitySource.getPrimaryKeyColumnSources(),
				keyBinding,
				false,
				new RelationalObjectBinder.ColumnNamingDelegate() {
					int count = 0;
					@Override
					public Identifier determineImplicitName(LocalMetadataBuildingContext context) {
						return primaryTable.getPrimaryKey().getColumn( count++ )
								.getNameIdentifier( metadataBuildingContext );
					}
				}
		);
		keyBinding.sortProperties();
		setForeignKeyName( keyBinding,
				entitySource.getExplicitForeignKeyName() );
		// model.getKey().setType( new Type( model.getIdentifier() ) );
		entityDescriptor.createPrimaryKey();
		entityDescriptor.createForeignKey();

		// todo : tooling hints

		bindAllEntityAttributes( mappingDocument, entitySource, entityDescriptor );

		bindJoinedSubclassEntities( entitySource, entityDescriptor );
	}
	private void bindUnionSubclassEntities(
			EntitySource entitySource,
			PersistentClass superEntityDescriptor) {
		for ( var subType : entitySource.getSubTypes() ) {
			final var subEntityDescriptor =
					new UnionSubclass( superEntityDescriptor, metadataBuildingContext );
			subEntityDescriptor.setCached( superEntityDescriptor.isCached() );
			bindUnionSubclassEntity( (SubclassEntitySourceImpl) subType, subEntityDescriptor );
			superEntityDescriptor.addSubclass( subEntityDescriptor );
			entitySource.getLocalMetadataBuildingContext().getMetadataCollector()
					.addEntityBinding( subEntityDescriptor );
		}
	}

	private void bindUnionSubclassEntity(
			SubclassEntitySourceImpl entitySource,
			UnionSubclass entityDescriptor) {
		final var mappingDocument = entitySource.sourceMappingDocument();

		bindBasicEntityValues( mappingDocument, entitySource, entityDescriptor );

		final var primaryTable = bindEntityTableSpecification(
				mappingDocument,
				entitySource.getPrimaryTable(),
				entityDescriptor.getSuperclass().getTable(),
				entitySource,
				entityDescriptor
		);
		entityDescriptor.setTable( primaryTable );

		if ( BOOT_LOGGER.isTraceEnabled() ) {
			BOOT_LOGGER.mappingUnionSubclassToTable(
					entityDescriptor.getEntityName(),
					primaryTable.getName() );
		}

		// todo : tooling hints

		bindAllEntityAttributes(
				entitySource.sourceMappingDocument(),
				entitySource,
				entityDescriptor
		);

		bindUnionSubclassEntities( entitySource, entityDescriptor );
	}

	private void bindSimpleEntityIdentifier(
			MappingDocument sourceDocument,
			final EntityHierarchySource hierarchySource,
			RootClass rootEntityDescriptor) {
		final var idSource = (IdentifierSourceSimple) hierarchySource.getIdentifierSource();

		final var idValue = new BasicValue( sourceDocument, rootEntityDescriptor.getTable() );
		rootEntityDescriptor.setIdentifier( idValue );

		bindSimpleValueType(
				sourceDocument,
				idSource.getIdentifierAttributeSource().getTypeInformation(),
				idValue
		);

		final String propertyName = idSource.getIdentifierAttributeSource().getName();
		if ( propertyName == null || !rootEntityDescriptor.hasPojoRepresentation() ) {
			if ( !idValue.isTypeSpecified() ) {
				throw new MappingException(
						"must specify an identifier type: " + rootEntityDescriptor.getEntityName(),
						sourceDocument.getOrigin()
				);
			}
		}
		else {
			idValue.setTypeUsingReflection( rootEntityDescriptor.getClassName(), propertyName );
		}

		final var identifierAttributeSource =
				(RelationalValueSourceContainer)
						idSource.getIdentifierAttributeSource();
		relationalObjectBinder.bindColumnsAndFormulas(
				sourceDocument,
				identifierAttributeSource.getRelationalValueSources(),
				idValue,
				false,
				context -> {
					context.getBuildingOptions().getImplicitNamingStrategy().determineIdentifierColumnName(
							new ImplicitIdentifierColumnNameSource() {
								@Override
								public EntityNaming getEntityNaming() {
									return hierarchySource.getRoot().getEntityNamingSource();
								}

								@Override
								public AttributePath getIdentifierAttributePath() {
									return idSource.getIdentifierAttributeSource().getAttributePath();
								}

								@Override
								public MetadataBuildingContext getBuildingContext() {
									return context;
								}
							}
					);
					return database.toIdentifier( propertyName );
				}
		);

		if ( propertyName != null ) {
			final var property = new Property();
			property.setValue( idValue );
			bindProperty( sourceDocument, idSource.getIdentifierAttributeSource(), property );
			rootEntityDescriptor.setIdentifierProperty( property );
			rootEntityDescriptor.setDeclaredIdentifierProperty( property );
		}

		makeIdGenerator(
				sourceDocument,
				idSource.getIdentifierGeneratorDescriptor(),
				idValue,
				metadataBuildingContext
		);
		if ( isNotEmpty( idSource.getUnsavedValue() ) ) {
			idValue.setNullValue( idSource.getUnsavedValue() );
		}
	}

	private void bindAggregatedCompositeEntityIdentifier(
			MappingDocument mappingDocument,
			EntityHierarchySourceImpl hierarchySource,
			RootClass rootEntityDescriptor) {

		// an aggregated composite-id is a composite-id that defines a singular
		// (composite) attribute as part of the entity to represent the id.

		final var identifierSource =
				(IdentifierSourceAggregatedComposite)
						hierarchySource.getIdentifierSource();

		final var cid = new Component( mappingDocument, rootEntityDescriptor );
		cid.setKey( true );
		rootEntityDescriptor.setIdentifier( cid );

		final String idClassName = extractIdClassName( identifierSource );

		final String idPropertyName = identifierSource.getIdentifierAttributeSource().getName();
		final String pathPart = idPropertyName == null ? "<id>" : idPropertyName;

		bindComponent(
				mappingDocument,
				hierarchySource.getRoot().getAttributeRoleBase().append( pathPart ).getFullPath(),
				identifierSource.getEmbeddableSource(),
				cid,
				idClassName,
				rootEntityDescriptor.getClassName(),
				idPropertyName,
				idPropertyName == null,
				idClassName == null && idPropertyName == null,
				identifierSource.getEmbeddableSource().isDynamic()
		);

		finishBindingCompositeIdentifier(
				mappingDocument,
				rootEntityDescriptor,
				identifierSource,
				cid,
				idPropertyName
		);
	}

	private String extractIdClassName(IdentifierSourceAggregatedComposite identifierSource) {
		final var typeDescriptor = identifierSource.getEmbeddableSource().getTypeDescriptor();
		return typeDescriptor == null ? null : typeDescriptor.getName();
	}

	private static final String ID_MAPPER_PATH_PART = '<' + NavigablePath.IDENTIFIER_MAPPER_PROPERTY + '>';

	private void bindNonAggregatedCompositeEntityIdentifier(
			MappingDocument mappingDocument,
			EntityHierarchySourceImpl hierarchySource,
			RootClass rootEntityDescriptor) {
		final var identifierSource =
				(IdentifierSourceNonAggregatedComposite)
						hierarchySource.getIdentifierSource();

		final var cid = new Component( mappingDocument, rootEntityDescriptor );
		cid.setKey( true );
		rootEntityDescriptor.setIdentifier( cid );

		final String idClassName = extractIdClassName( identifierSource );

		final var attributeRoleBase = hierarchySource.getRoot().getAttributeRoleBase();

		final String className = rootEntityDescriptor.getClassName();
		bindComponent(
				mappingDocument,
				attributeRoleBase.append( "<id>" ).getFullPath(),
				identifierSource.getEmbeddableSource(),
				cid,
				idClassName,
				className,
				null,
				true,
				idClassName == null,
				false
		);

		if ( idClassName != null ) {
			// We also need to bind the "id mapper". Ugh, terrible name. Basically,
			// we need to create a virtual (embedded) composite for the non-aggregated
			// attributes on the entity itself.
			final var mapper = new Component( mappingDocument, rootEntityDescriptor );
			bindComponent(
					mappingDocument,
					attributeRoleBase.append( ID_MAPPER_PATH_PART ).getFullPath(),
					identifierSource.getEmbeddableSource(),
					mapper,
					className,
					null,
					null,
					true,
					true,
					false
			);

			rootEntityDescriptor.setIdentifierMapper( mapper );
			rootEntityDescriptor.setDeclaredIdentifierMapper( mapper );
			final var property = new Property();
			property.setName( NavigablePath.IDENTIFIER_MAPPER_PROPERTY );
			property.setUpdatable( false );
			property.setInsertable( false );
			property.setValue( mapper );
			property.setPropertyAccessorName( EMBEDDED.getExternalName() );
			rootEntityDescriptor.addProperty( property );
		}

		finishBindingCompositeIdentifier( mappingDocument, rootEntityDescriptor, identifierSource, cid, null );
	}

	private String extractIdClassName(IdentifierSourceNonAggregatedComposite identifierSource) {
		final var idClassSource = identifierSource.getIdClassSource();
		if ( idClassSource == null ) {
			return null;
		}
		else {
			final var typeDescriptor = idClassSource.getTypeDescriptor();
			return typeDescriptor == null ? null : typeDescriptor.getName();
		}
	}

	private void finishBindingCompositeIdentifier(
			MappingDocument sourceDocument,
			RootClass rootEntityDescriptor,
			CompositeIdentifierSource identifierSource,
			Component cid,
			String propertyName) {
		if ( propertyName == null ) {
			rootEntityDescriptor.setEmbeddedIdentifier( cid.isEmbedded() );
			if ( cid.isEmbedded() ) {
				//TODO: what is the implication of this?
				cid.setDynamic( !rootEntityDescriptor.hasPojoRepresentation() );
			}
		}
		else {
			final var property = new Property();
			property.setValue( cid );
			bindProperty(
					sourceDocument,
					( (IdentifierSourceAggregatedComposite) identifierSource )
							.getIdentifierAttributeSource(),
					property
			);
			rootEntityDescriptor.setIdentifierProperty( property );
			rootEntityDescriptor.setDeclaredIdentifierProperty( property );
		}

		makeIdGenerator(
				sourceDocument,
				identifierSource.getIdentifierGeneratorDescriptor(),
				cid,
				metadataBuildingContext
		);
	}

	private void bindEntityVersion(
			MappingDocument sourceDocument,
			EntityHierarchySourceImpl hierarchySource,
			RootClass rootEntityDescriptor) {
		final var versionAttributeSource = hierarchySource.getVersionAttributeSource();

		final var versionValue =
				new BasicValue( sourceDocument, rootEntityDescriptor.getTable() );

		versionValue.makeVersion();

		bindSimpleValueType( sourceDocument, versionAttributeSource.getTypeInformation(), versionValue );

		relationalObjectBinder.bindColumnsAndFormulas(
				sourceDocument,
				versionAttributeSource.getRelationalValueSources(),
				versionValue,
				false,
				context -> implicitNamingStrategy.determineBasicColumnName( versionAttributeSource )
		);

		final var property = new Property();
		property.setValue( versionValue );
		bindProperty( sourceDocument, versionAttributeSource, property );

		final String unsavedValue = versionAttributeSource.getUnsavedValue();
		if ( unsavedValue != null ) {
			versionValue.setNullValue( unsavedValue );
		}
		else {
			versionValue.setNullValueUndefined();
		}
		if ( versionAttributeSource.getSource().equals("db") ) {
			property.setValueGeneratorCreator(
					context -> new SourceGeneration( SourceType.DB, property.getType().getReturnedClass(), context ) );
		}

		rootEntityDescriptor.setVersion( property );
		rootEntityDescriptor.setDeclaredVersion( property );
		rootEntityDescriptor.addProperty( property );
	}

	private void bindEntityDiscriminator(
			MappingDocument sourceDocument,
			final EntityHierarchySourceImpl hierarchySource,
			RootClass rootEntityDescriptor) {
		final var discriminatorValue =
				new BasicValue( sourceDocument, rootEntityDescriptor.getTable() );
		rootEntityDescriptor.setDiscriminator( discriminatorValue );

		final var discriminatorSource = hierarchySource.getDiscriminatorSource();

		String typeName = discriminatorSource.getExplicitHibernateTypeName();
		if ( typeName == null ) {
			typeName = "string";
		}
		bindSimpleValueType( sourceDocument, new HibernateTypeSourceImpl( typeName ), discriminatorValue );

		relationalObjectBinder.bindColumnOrFormula(
				sourceDocument,
				discriminatorSource.getDiscriminatorRelationalValueSource(),
				discriminatorValue,
				false,
				context -> implicitNamingStrategy.determineDiscriminatorColumnName( discriminatorSource )
		);

		rootEntityDescriptor.setPolymorphic( true );
		rootEntityDescriptor.setDiscriminatorInsertable( discriminatorSource.isInserted() );

		// todo : currently isForced() is defined as boolean, not Boolean
		//		although it has always been that way (DTD too)
		final boolean force = discriminatorSource.isForced()
							|| sourceDocument.getBuildingOptions().shouldImplicitlyForceDiscriminatorInSelect();
		rootEntityDescriptor.setForceDiscriminator( force );
	}

	private void bindAllEntityAttributes(
			MappingDocument mappingDocument,
			EntitySource entitySource,
			PersistentClass entityDescriptor) {
		final var entityTableXref =
				mappingDocument.getMetadataCollector()
						.getEntityTableXref( entityDescriptor.getEntityName() );
		if ( entityTableXref == null ) {
			throw new AssertionFailure(
					String.format(
							Locale.ENGLISH,
							"Unable to locate EntityTableXref for entity [%s] : %s",
							entityDescriptor.getEntityName(),
							mappingDocument.getOrigin()
					)
			);
		}

		// make sure we bind secondary tables first!
		for ( var secondaryTableSource : entitySource.getSecondaryTableMap().values() ) {
			final var secondaryTableJoin = new Join();
			secondaryTableJoin.setPersistentClass( entityDescriptor );
			bindSecondaryTable( mappingDocument, secondaryTableSource, secondaryTableJoin, entityTableXref );
			entityDescriptor.addJoin( secondaryTableJoin );
		}

		for ( var attributeSource : entitySource.attributeSources() ) {
			if ( attributeSource instanceof PluralAttributeSource pluralAttributeSource) {
				// plural attribute
				final var attribute =
						createPluralAttribute( mappingDocument, pluralAttributeSource, entityDescriptor );
				attribute.setOptional( true );
				entityDescriptor.addProperty( attribute );
			}
			else {
				// singular attribute
				if ( attributeSource instanceof SingularAttributeSourceBasic basicAttributeSource ) {
					final Identifier tableName =
							determineTable( mappingDocument, basicAttributeSource.getName(), basicAttributeSource );
					final var secondaryTableJoin = entityTableXref.locateJoin( tableName );
					final AttributeContainer attributeContainer;
					final Table table;
					if ( secondaryTableJoin == null ) {
						table = entityDescriptor.getTable();
						attributeContainer = entityDescriptor;
					}
					else {
						table = secondaryTableJoin.getTable();
						attributeContainer = secondaryTableJoin;
					}

					final var attribute = createBasicAttribute(
							mappingDocument,
							basicAttributeSource,
							new BasicValue( mappingDocument, table ),
							entityDescriptor.getClassName()
					);

					handleAttribute( attribute, secondaryTableJoin, attributeContainer, mappingDocument, entityDescriptor,
							basicAttributeSource );
				}
				else if ( attributeSource instanceof SingularAttributeSourceEmbedded embeddedAttributeSource ) {
					final Identifier tableName = determineTable( mappingDocument, embeddedAttributeSource );
					final var secondaryTableJoin = entityTableXref.locateJoin( tableName );
					final AttributeContainer attributeContainer;
					final Table table;
					if ( secondaryTableJoin == null ) {
						table = entityDescriptor.getTable();
						attributeContainer = entityDescriptor;
					}
					else {
						table = secondaryTableJoin.getTable();
						attributeContainer = secondaryTableJoin;
					}

					final var attribute = createEmbeddedAttribute(
							mappingDocument,
							embeddedAttributeSource,
							new Component( mappingDocument, table, entityDescriptor ),
							entityDescriptor.getClassName()
					);

					handleAttribute(
							attribute,
							secondaryTableJoin,
							attributeContainer,
							mappingDocument,
							entityDescriptor,
							embeddedAttributeSource
					);
				}
				else if ( attributeSource instanceof SingularAttributeSourceManyToOne manyToOneAttributeSource ) {
					final Identifier tableName =
							determineTable( mappingDocument, manyToOneAttributeSource.getName(), manyToOneAttributeSource );
					final var secondaryTableJoin = entityTableXref.locateJoin( tableName );
					final AttributeContainer attributeContainer;
					final Table table;
					if ( secondaryTableJoin == null ) {
						table = entityDescriptor.getTable();
						attributeContainer = entityDescriptor;
					}
					else {
						table = secondaryTableJoin.getTable();
						attributeContainer = secondaryTableJoin;
					}

					final var attribute = createManyToOneAttribute(
							mappingDocument,
							manyToOneAttributeSource,
							new ManyToOne( mappingDocument, table ),
							entityDescriptor.getClassName()
					);

					handleAttribute(
							attribute,
							secondaryTableJoin,
							attributeContainer,
							mappingDocument,
							entityDescriptor,
							manyToOneAttributeSource
					);
				}
				else if ( attributeSource instanceof SingularAttributeSourceOneToOne oneToOneAttributeSource ) {
					final var table = entityDescriptor.getTable();
					final var attribute = createOneToOneAttribute(
							mappingDocument,
							oneToOneAttributeSource,
							new OneToOne( mappingDocument, table, entityDescriptor ),
							entityDescriptor.getClassName()
					);

					attribute.setOptional( attribute.getValue().isNullable() );

					entityDescriptor.addProperty( attribute );

					handleNaturalIdBinding( mappingDocument, entityDescriptor, attribute,
							oneToOneAttributeSource.getNaturalIdMutability() );
				}
				else if ( attributeSource instanceof SingularAttributeSourceAny anyAttributeSource ) {
					final Identifier tableName = determineTable(
							mappingDocument,
							anyAttributeSource.getName(),
							anyAttributeSource.getKeySource().getRelationalValueSources()
					);
					final var secondaryTableJoin = entityTableXref.locateJoin( tableName );
					final AttributeContainer attributeContainer;
					final Table table;
					if ( secondaryTableJoin == null ) {
						table = entityDescriptor.getTable();
						attributeContainer = entityDescriptor;
					}
					else {
						table = secondaryTableJoin.getTable();
						attributeContainer = secondaryTableJoin;
					}

					final var attribute = createAnyAssociationAttribute(
							mappingDocument,
							anyAttributeSource,
							new Any( mappingDocument, table ),
							entityDescriptor.getEntityName()
					);

					handleAttribute(
							attribute,
							secondaryTableJoin,
							attributeContainer,
							mappingDocument,
							entityDescriptor,
							anyAttributeSource
					);
				}
			}
		}
	}

	private void handleAttribute(
			Property attribute,
			Join secondaryTableJoin,
			AttributeContainer attributeContainer,
			MappingDocument mappingDocument,
			PersistentClass entityDescriptor,
			SingularAttributeSource attributeSource) {
		attribute.setOptional( isOptional( secondaryTableJoin, attribute ) );
		attributeContainer.addProperty( attribute );
		handleNaturalIdBinding( mappingDocument, entityDescriptor, attribute,
				attributeSource.getNaturalIdMutability() );
	}

	private static boolean isOptional(Join secondaryTableJoin, Property attribute) {
		return secondaryTableJoin != null && secondaryTableJoin.isOptional()
			|| attribute.getValue().isNullable();
	}

	private void handleNaturalIdBinding(
			MappingDocument mappingDocument,
			PersistentClass entityBinding,
			Property attributeBinding,
			NaturalIdMutability naturalIdMutability) {
		if ( naturalIdMutability != NaturalIdMutability.NOT_NATURAL_ID ) {
			attributeBinding.setNaturalIdentifier( true );
			if ( naturalIdMutability == NaturalIdMutability.IMMUTABLE ) {
				attributeBinding.setUpdatable( false );
			}
			final var metadataCollector = mappingDocument.getMetadataCollector();
			final String entityName = entityBinding.getEntityName();
			var ukBinder = metadataCollector.locateNaturalIdUniqueKeyBinder( entityName );
			if ( ukBinder == null ) {
				ukBinder = new NaturalIdUniqueKeyBinderImpl( mappingDocument, entityBinding );
				metadataCollector.registerNaturalIdUniqueKeyBinder( entityName, ukBinder );
			}
			ukBinder.addAttributeBinding( attributeBinding );
		}
	}

	private Property createPluralAttribute(
			MappingDocument sourceDocument,
			PluralAttributeSource attributeSource,
			PersistentClass entityDescriptor) {
		final Collection collectionBinding;
		if ( attributeSource instanceof PluralAttributeSourceListImpl pluralAttributeSourceList ) {
			final var list = new org.hibernate.mapping.List(sourceDocument, entityDescriptor);
			collectionBinding = list;
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );
			registerSecondPass(
					new PluralAttributeListSecondPass( sourceDocument, pluralAttributeSourceList, list ),
					sourceDocument
			);
		}
		else if ( attributeSource instanceof PluralAttributeSourceSetImpl ) {
			collectionBinding = new Set( sourceDocument, entityDescriptor );
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );
			registerSecondPass(
					new PluralAttributeSetSecondPass( sourceDocument, attributeSource, collectionBinding ),
					sourceDocument
			);
		}
		else if ( attributeSource instanceof PluralAttributeSourceMapImpl pluralAttributeSourceMap ) {
			final var map = new org.hibernate.mapping.Map( sourceDocument, entityDescriptor );
			collectionBinding = map;
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );
			registerSecondPass(
					new PluralAttributeMapSecondPass( sourceDocument, pluralAttributeSourceMap, map ),
					sourceDocument
			);
		}
		else if ( attributeSource instanceof PluralAttributeSourceBagImpl ) {
			collectionBinding = new Bag( sourceDocument, entityDescriptor );
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );
			registerSecondPass(
					new PluralAttributeBagSecondPass( sourceDocument, attributeSource, collectionBinding ),
					sourceDocument
			);
		}
		else if ( attributeSource instanceof PluralAttributeSourceIdBagImpl ) {
			collectionBinding = new IdentifierBag( sourceDocument, entityDescriptor );
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );
			registerSecondPass(
					new PluralAttributeIdBagSecondPass( sourceDocument, attributeSource, collectionBinding ),
					sourceDocument
			);
		}
		else if ( attributeSource instanceof PluralAttributeSourceArrayImpl arraySource ) {
			final var array = new Array(sourceDocument, entityDescriptor);
			collectionBinding = array;
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );
			array.setElementClassName( sourceDocument.qualifyClassName( arraySource.getElementClass() ) );
			registerSecondPass(
					new PluralAttributeArraySecondPass( sourceDocument, arraySource, array ),
					sourceDocument
			);
		}
		else if ( attributeSource instanceof PluralAttributeSourcePrimitiveArrayImpl pluralAttributeSourcePrimitiveArray ) {
			final var primitiveArray = new PrimitiveArray( sourceDocument, entityDescriptor );
			collectionBinding = primitiveArray;
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );
			registerSecondPass(
					new PluralAttributePrimitiveArraySecondPass(
							sourceDocument,
							pluralAttributeSourcePrimitiveArray,
							primitiveArray
					),
					sourceDocument
			);
		}
		else {
			throw new AssertionFailure(
					"Unexpected PluralAttributeSource type: " + attributeSource.getClass().getName()
			);
		}

		sourceDocument.getMetadataCollector()
				.addCollectionBinding( collectionBinding );

		final var attribute = new Property();
		attribute.setValue( collectionBinding );
		bindProperty( sourceDocument, attributeSource, attribute );
		return attribute;
	}

	private void bindCollectionMetadata(
			MappingDocument mappingDocument, PluralAttributeSource source, Collection binding) {
		binding.setRole( source.getAttributeRole().getFullPath() );
		binding.setInverse( source.isInverse() );
		binding.setMutable( source.isMutable() );
		binding.setOptimisticLocked( source.isIncludedInOptimisticLocking() );

		if ( source.getCustomPersisterClassName() != null ) {
			DEPRECATION_LOGGER.debugf(
					"Custom CollectionPersister impls are no longer supported - %s (%s)",
					binding.getRole(),
					mappingDocument.getOrigin().getName()
			);
		}

		applyCaching( mappingDocument, source.getCaching(), binding );

		bindCollectionType( mappingDocument, source, binding );

		final var fetchCharacteristics = source.getFetchCharacteristics();
		binding.setLazy( fetchCharacteristics.getFetchTiming() == FetchTiming.DELAYED );
		binding.setExtraLazy( fetchCharacteristics.isExtraLazy() );

		setupFetching( source, binding );

		for ( String name : source.getSynchronizedTableNames() ) {
			binding.getSynchronizedTables().add( name );
		}

		binding.setLoaderName( source.getCustomLoaderName() );
		bindCustomSql( source, binding );

		if ( source instanceof Sortable sortable && sortable.isSorted() ) {
			binding.setSorted( true );
			final String comparatorName = sortable.getComparatorName();
			if ( !comparatorName.equals( "natural" ) ) {
				binding.setComparatorClassName( comparatorName );
			}
		}

		if ( source instanceof Orderable orderable && orderable.isOrdered() ) {
			binding.setOrderBy( orderable.getOrder() );
		}

		final String cascadeStyle = source.getCascadeStyleName();
		if ( cascadeStyle != null && cascadeStyle.contains( "delete-orphan" ) ) {
			binding.setOrphanDelete( true );
		}

		for ( var filterSource : source.getFilterSources() ) {
			binding.addFilter(
					filterSource.getName(),
					filterCondition( filterSource, mappingDocument ),
					filterSource.shouldAutoInjectAliases(),
					filterSource.getAliasToTableMap(),
					filterSource.getAliasToEntityMap()
			);
		}
	}

	private static void bindCollectionType(
			MappingDocument mappingDocument, PluralAttributeSource source, Collection binding) {
		// bind the collection type info
		final String explicitTypeName = source.getTypeInformation().getName();
		final Map<String,String> typeParameters = new HashMap<>();
		final String typeName;
		if ( explicitTypeName != null ) {
			// see if there is a corresponding type-def
			final var typeDefinition =
					mappingDocument.getMetadataCollector()
							.getTypeDefinition( explicitTypeName );
			if ( typeDefinition != null ) {
				typeName = typeDefinition.getTypeImplementorClass().getName();
				final var parameters = typeDefinition.getParameters();
				if ( parameters != null ) {
					typeParameters.putAll( parameters );
				}
			}
			else {
				// it could be an unqualified class name, in which case qualify
				// it with the implicit package name for this context, if one.
				typeName = mappingDocument.qualifyClassName( explicitTypeName );
			}
		}
		else {
			typeName = null;
		}
		final var parameters = source.getTypeInformation().getParameters();
		if ( parameters != null ) {
			typeParameters.putAll( parameters );
		}
		binding.setTypeName( typeName );
		binding.setTypeParameters( typeParameters );
	}

	private static void bindCustomSql(PluralAttributeSource source, Collection binding) {
		if ( source.getCustomSqlInsert() != null ) {
			binding.setCustomSQLInsert(
					source.getCustomSqlInsert().sql(),
					source.getCustomSqlInsert().callable(),
					source.getCustomSqlInsert().checkStyle()
			);
		}
		if ( source.getCustomSqlUpdate() != null ) {
			binding.setCustomSQLUpdate(
					source.getCustomSqlUpdate().sql(),
					source.getCustomSqlUpdate().callable(),
					source.getCustomSqlUpdate().checkStyle()
			);
		}
		if ( source.getCustomSqlDelete() != null ) {
			binding.setCustomSQLDelete(
					source.getCustomSqlDelete().sql(),
					source.getCustomSqlDelete().callable(),
					source.getCustomSqlDelete().checkStyle()
			);
		}
		if ( source.getCustomSqlDeleteAll() != null ) {
			binding.setCustomSQLDeleteAll(
					source.getCustomSqlDeleteAll().sql(),
					source.getCustomSqlDeleteAll().callable(),
					source.getCustomSqlDeleteAll().checkStyle()
			);
		}
	}

	private static void setupFetching(PluralAttributeSource source, Collection binding) {
		final var fetchCharacteristics = source.getFetchCharacteristics();
		final var fetchStyle = fetchCharacteristics.getFetchStyle();
		binding.setFetchMode( switch ( fetchStyle ) {
			case SELECT, BATCH, SUBSELECT -> FetchMode.SELECT;
			case JOIN -> FetchMode.JOIN;
		} );
		switch ( fetchStyle ) {
			case BATCH:
				binding.setBatchSize( fetchCharacteristics.getBatchSize() );
				break;
			case SUBSELECT:
				binding.setSubselectLoadable( true );
				binding.getOwner().setSubselectLoadableCollections( true );
				break;
		}
	}

	private void applyCaching(MappingDocument mappingDocument, Caching caching, Collection collection) {
		if ( isCacheEnabled( mappingDocument, caching ) ) {
			collection.setCacheConcurrencyStrategy( getConcurrencyStrategy( mappingDocument, caching ).getExternalName() );
			collection.setCacheRegionName( caching == null ? null : caching.getRegion() );
		}
	}

	private Identifier determineTable(
			MappingDocument sourceDocument,
			String attributeName,
			RelationalValueSourceContainer relationalValueSourceContainer) {
		return determineTable( sourceDocument, attributeName, relationalValueSourceContainer.getRelationalValueSources() );
	}

	private Identifier determineTable(
			MappingDocument mappingDocument,
			SingularAttributeSourceEmbedded embeddedAttributeSource) {
		Identifier tableName = null;
		for ( var attributeSource : embeddedAttributeSource.getEmbeddableSource().attributeSources() ) {
			final Identifier determinedName;
			if ( attributeSource instanceof RelationalValueSourceContainer relationalValueSourceContainer ) {
				determinedName = determineTable(
						mappingDocument,
						embeddedAttributeSource.getAttributeRole().getFullPath(),
						relationalValueSourceContainer

				);
			}
			else if ( attributeSource instanceof SingularAttributeSourceEmbedded singularAttributeSourceEmbedded ) {
				determinedName = determineTable( mappingDocument, singularAttributeSourceEmbedded );
			}
			else if ( attributeSource instanceof SingularAttributeSourceAny singularAttributeSourceAny ) {
				determinedName = determineTable(
						mappingDocument,
						attributeSource.getAttributeRole().getFullPath(),
						singularAttributeSourceAny.getKeySource().getRelationalValueSources()
				);
			}
			else {
				continue;
			}

			if ( !Objects.equals( tableName, determinedName ) ) {
				if ( tableName != null ) {
					throw new MappingException(
							String.format(
									Locale.ENGLISH,
									"Attribute [%s] referenced columns from multiple tables: %s, %s",
									embeddedAttributeSource.getAttributeRole().getFullPath(),
									tableName,
									determinedName
							),
							mappingDocument.getOrigin()
					);
				}
				tableName = determinedName;
			}
		}
		return tableName;
	}

	private Identifier determineTable(
			MappingDocument mappingDocument,
			String attributeName,
			List<RelationalValueSource> relationalValueSources) {
		String tableName = null;
		for ( var relationalValueSource : relationalValueSources ) {
			// We need to get the containing table name for both columns and formulas,
			// particularly when a column/formula is for a property on a secondary table.
			final String containingTableName = relationalValueSource.getContainingTableName();
			if ( !Objects.equals( tableName, containingTableName ) ) {
				if ( tableName != null ) {
					throw new MappingException(
							String.format(
									Locale.ENGLISH,
									"Attribute [%s] referenced columns from multiple tables: %s, %s",
									attributeName,
									tableName,
									containingTableName
							),
							mappingDocument.getOrigin()
					);
				}
				tableName = containingTableName;
			}
		}
		return database.toIdentifier( tableName );
	}

	private void bindSecondaryTable(
			MappingDocument mappingDocument,
			SecondaryTableSource secondaryTableSource,
			Join secondaryTableJoin,
			final EntityTableXref entityTableXref) {
		final var persistentClass = secondaryTableJoin.getPersistentClass();

		final var tableSpecificationSource = secondaryTableSource.getTableSource();
		final var namespace = locateTableNamespace( tableSpecificationSource );

		Table secondaryTable;
		final Identifier logicalTableName;
		if ( tableSpecificationSource instanceof TableSource tableSource ) {
			logicalTableName = database.toIdentifier( tableSource.getExplicitTableName() );
			secondaryTable = namespace.locateTable( logicalTableName );
			if ( secondaryTable == null ) {
				secondaryTable =
						namespace.createTable( logicalTableName,
								identifier -> new Table( mappingDocument.getCurrentContributorName(), namespace, identifier, false ) );
			}
			else {
				secondaryTable.setAbstract( false );
			}
			secondaryTable.setComment( tableSource.getComment() );
		}
		else if ( tableSpecificationSource instanceof InLineViewSource inLineViewSource ) {
			logicalTableName = toIdentifier( inLineViewSource.getLogicalName() );
			secondaryTable = new Table(
					metadataBuildingContext.getCurrentContributorName(),
					namespace,
					inLineViewSource.getSelectStatement(),
					false
			);
		}
		else {
			throw new AssertionFailure( "Unexpected table specification soure type" );
		}

		secondaryTableJoin.setTable( secondaryTable );
		entityTableXref.addSecondaryTable( mappingDocument, logicalTableName, secondaryTableJoin );

		bindCustomSql( secondaryTableSource, secondaryTableJoin );

		secondaryTableJoin.setInverse( secondaryTableSource.isInverse() );
		secondaryTableJoin.setOptional( secondaryTableSource.isOptional() );

		if ( BOOT_LOGGER.isTraceEnabled() ) {
			BOOT_LOGGER.mappingEntitySecondaryTableToTable(
					persistentClass.getEntityName(),
					secondaryTable.getName()
			);
		}

		final var keyBinding =
				new DependantValue( mappingDocument, secondaryTable,
						persistentClass.getIdentifier() );
		if ( mappingDocument.getBuildingOptions().useNationalizedCharacterData() ) {
			keyBinding.makeNationalized();
		}
		secondaryTableJoin.setKey( keyBinding );

		keyBinding.setOnDeleteAction( getOnDeleteAction( secondaryTableSource ) );

		// NOTE: no Type info to bind...

		relationalObjectBinder.bindColumns(
				mappingDocument,
				secondaryTableSource.getPrimaryKeyColumnSources(),
				keyBinding,
				secondaryTableSource.isOptional(),
				new RelationalObjectBinder.ColumnNamingDelegate() {
					int count = 0;
					@Override
					public Identifier determineImplicitName(LocalMetadataBuildingContext context) {
						return entityTableXref.getPrimaryTable().getPrimaryKey().getColumn( count++ )
								.getNameIdentifier( metadataBuildingContext );
					}
				}
		);

		keyBinding.sortProperties();
		setForeignKeyName( keyBinding,
				secondaryTableSource.getExplicitForeignKeyName() );

		// skip creating primary and foreign keys for a subselect.
		if ( secondaryTable.getSubselect() == null ) {
			secondaryTableJoin.createPrimaryKey();
			secondaryTableJoin.createForeignKey();
		}
	}

	private Namespace locateTableNamespace(TableSpecificationSource tableSpecificationSource) {
		return database.locateNamespace(
				determineCatalogName( tableSpecificationSource ),
				determineSchemaName( tableSpecificationSource )
		);
	}

	private Property createEmbeddedAttribute(
			MappingDocument sourceDocument,
			SingularAttributeSourceEmbedded embeddedSource,
			Component componentBinding,
			String containingClassName) {
		final String attributeName = embeddedSource.getName();

		bindComponent(
				sourceDocument,
				embeddedSource.getEmbeddableSource(),
				componentBinding,
				containingClassName,
				attributeName,
				embeddedSource.isVirtualAttribute()
		);

		prepareValueTypeViaReflection(
				sourceDocument,
				componentBinding,
				componentBinding.getComponentClassName(),
				attributeName,
				embeddedSource.getAttributeRole()
		);

		componentBinding.createForeignKey();

		final var attribute = new Property();
		attribute.setValue( componentBinding );
		bindProperty( sourceDocument, embeddedSource, attribute );
		if ( embeddedSource.isVirtualAttribute() ) {
			attribute.setPropertyAccessorName( EMBEDDED.getExternalName() );
		}
		return attribute;
	}

	private Property createBasicAttribute(
			MappingDocument sourceDocument,
			final SingularAttributeSourceBasic attributeSource,
			SimpleValue value,
			String containingClassName) {

		bindSimpleValueType( sourceDocument, attributeSource.getTypeInformation(), value );

		relationalObjectBinder.bindColumnsAndFormulas(
				sourceDocument,
				attributeSource.getRelationalValueSources(),
				value,
				attributeSource.areValuesNullableByDefault(),
				context -> implicitNamingStrategy.determineBasicColumnName( attributeSource )
		);


		prepareValueTypeViaReflection(
				sourceDocument,
				value,
				containingClassName,
				attributeSource.getName(),
				attributeSource.getAttributeRole()
		);

		markAsLobIfNecessary( attributeSource, value );

		value.createForeignKey();

		final var property = new Property();
		property.setValue( value );
		property.setLob( value.isLob() );
		bindProperty( sourceDocument, attributeSource, property );
		return property;
	}

	private void markAsLobIfNecessary(SingularAttributeSourceBasic attributeSource, SimpleValue value) {
		if ( !value.isLob() ) {
			// Determine whether the property is a LOB based on the type attribute on the attribute
			// property source. Expects the type to map to a CLOB, NCLOB, or BLOB SQL type internally.
			final String typeName = value.getTypeName();
			if ( typeName != null ) {
				final var basicType = getBasicType( attributeSource, typeName );
				if ( basicType instanceof AbstractSingleColumnStandardBasicType
						&& isLob( basicType.getJdbcType().getDdlTypeCode(), null ) ) {
					value.makeLob();
					return; // EARLY EXIT!
				}
			}
			// If the prior check didn't set the LOB flag, inspect the column SQL type,
			// and if it is a CLOB, NCLOB, or BLOB, then mark the value as a LOB
			for ( var relationalValueSource : attributeSource.getRelationalValueSources() ) {
				if ( relationalValueSource instanceof ColumnSource columnSource ) {
					if ( isLob( null, columnSource.getSqlType() ) ) {
						value.makeLob();
					}
				}
			}
		}
	}

	private static BasicType<?> getBasicType(SingularAttributeSourceBasic attributeSource, String typeName) {
		final var context = attributeSource.getBuildingContext();
		return typeName.startsWith( BasicTypeImpl.EXTERNALIZED_PREFIX )
				? context.getBootstrapContext().resolveAdHocBasicType( typeName )
				: context.getMetadataCollector().getTypeConfiguration()
						.getBasicTypeRegistry().getRegisteredType( typeName );
	}

	private static boolean isLob(Integer sqlType, String sqlTypeName) {
		if ( sqlType != null ) {
			return switch (sqlType) {
				case Types.BLOB, Types.CLOB, Types.NCLOB -> true;
				default -> false;
			};
		}
		else if ( sqlTypeName != null ) {
			return switch ( sqlTypeName.toLowerCase(Locale.ROOT) ) {
				case "blob", "clob", "nclob" -> true;
				default -> false;
			};
		}
		return false;
	}

	private Property createOneToOneAttribute(
			MappingDocument sourceDocument,
			SingularAttributeSourceOneToOne oneToOneSource,
			OneToOne oneToOneBinding,
			String containingClassName) {
		bindOneToOne( sourceDocument, oneToOneSource, oneToOneBinding );

		prepareValueTypeViaReflection(
				sourceDocument,
				oneToOneBinding,
				containingClassName,
				oneToOneSource.getName(),
				oneToOneSource.getAttributeRole()
		);

		handlePropertyRef( sourceDocument, oneToOneBinding,
				"<one-to-one name=\"" + oneToOneSource.getName() + "\"/>" );

		// Defer the creation of the foreign key as we need the
		// associated entity persister to be initialized so that
		// we can observe the properties/columns of a possible
		// component in the correct order
		metadataBuildingContext.getMetadataCollector()
				.addSecondPass( new OneToOneFkSecondPass( oneToOneBinding ) );

		final var property = new Property();
		property.setValue( oneToOneBinding );
		bindProperty( sourceDocument, oneToOneSource, property );
		return property;
	}

	private void handlePropertyRef(
			MappingDocument sourceDocument,
			ToOne binding,
			String synopsis) {
		final String propertyRef = binding.getReferencedPropertyName();
		if ( propertyRef != null ) {
			handlePropertyReference(
					sourceDocument,
					binding.getReferencedEntityName(),
					propertyRef,
					synopsis
			);
		}
	}

	private void handlePropertyReference(
			MappingDocument mappingDocument,
			String referencedEntityName,
			String referencedPropertyName,
			String sourceElementSynopsis) {
		final var collector = mappingDocument.getMetadataCollector();
		final var entityBinding = collector.getEntityBinding( referencedEntityName );
		if ( entityBinding == null ) {
			// entity may just not have been processed yet - set up a delayed handler
			registerDelayedPropertyReferenceHandler(
					mappingDocument,
					referencedEntityName,
					referencedPropertyName,
					sourceElementSynopsis,
					collector
			);
		}
		else {
			final var propertyBinding =
					entityBinding.getReferencedProperty( referencedPropertyName );
			if ( propertyBinding == null ) {
				// attribute may just not have been processed yet - set up a delayed handler
				registerDelayedPropertyReferenceHandler(
						mappingDocument,
						referencedEntityName,
						referencedPropertyName,
						sourceElementSynopsis,
						collector
				);
			}
			else {
				BOOT_LOGGER.tracef(
						"Property [%s.%s] referenced by property-ref [%s] was available - no need for delayed handling",
						referencedEntityName,
						referencedPropertyName,
						sourceElementSynopsis
				);
				( (SimpleValue) propertyBinding.getValue() ).setAlternateUniqueKey( true );
			}
		}
	}

	private void registerDelayedPropertyReferenceHandler(
			MappingDocument mappingDocument,
			String referencedEntityName,
			String referencedPropertyName,
			String sourceElementSynopsis,
			InFlightMetadataCollector collector) {
		final var handler =
				new DelayedPropertyReferenceHandlerImpl(
						referencedEntityName,
						referencedPropertyName,
						true,
						sourceElementSynopsis,
						mappingDocument.getOrigin()
				);
		BOOT_LOGGER.tracef(
				"Property [%s.%s] referenced by property-ref [%s] was not yet available - creating delayed handler",
				handler.referencedEntityName,
				handler.referencedPropertyName,
				handler.sourceElementSynopsis
		);
		collector.addDelayedPropertyReferenceHandler( handler );
	}

	public void bindOneToOne(
			final MappingDocument sourceDocument,
			final SingularAttributeSourceOneToOne oneToOneSource,
			final OneToOne oneToOneBinding) {
		oneToOneBinding.setPropertyName( oneToOneSource.getName() );

		relationalObjectBinder.bindFormulas(
				sourceDocument,
				oneToOneSource.getFormulaSources(),
				oneToOneBinding
		);

		if ( oneToOneSource.isConstrained() ) {
			checkConstrainedOneToOneOrphanDelete( sourceDocument, oneToOneSource );
			oneToOneBinding.setConstrained( true );
			oneToOneBinding.setForeignKeyType( ForeignKeyDirection.FROM_PARENT );
		}
		else {
			oneToOneBinding.setForeignKeyType( ForeignKeyDirection.TO_PARENT );
		}

		handleFetchCharacteristics( oneToOneSource, oneToOneBinding );

		final String referencedEntityAttributeName = oneToOneSource.getReferencedEntityAttributeName();
		if ( isNotEmpty( referencedEntityAttributeName ) ) {
			oneToOneBinding.setReferencedPropertyName( referencedEntityAttributeName );
			oneToOneBinding.setReferenceToPrimaryKey( false );
		}
		else {
			oneToOneBinding.setReferenceToPrimaryKey( true );
		}

		// todo : probably need some reflection here if null
		oneToOneBinding.setReferencedEntityName( oneToOneSource.getReferencedEntityName() );

		if ( oneToOneSource.isEmbedXml() == Boolean.TRUE ) {
			DEPRECATION_LOGGER.logDeprecationOfEmbedXmlSupport();
		}

		setForeignKeyName( oneToOneBinding,
				oneToOneSource.getExplicitForeignKeyName() );

		oneToOneBinding.setOnDeleteAction( getOnDeleteAction( oneToOneSource ) );
	}

	private static void handleFetchCharacteristics(SingularAttributeSourceToOne toOneSource, ToOne toOneBinding) {
		final var fetchCharacteristics = toOneSource.getFetchCharacteristics();
		toOneBinding.setLazy( fetchCharacteristics.getFetchTiming() == FetchTiming.DELAYED );
		toOneBinding.setFetchMode(
				fetchCharacteristics.getFetchStyle() == FetchStyle.SELECT
						? FetchMode.SELECT
						: FetchMode.JOIN
		);
		toOneBinding.setUnwrapProxy( fetchCharacteristics.isUnwrapProxies() );
	}

	private Property createManyToOneAttribute(
			MappingDocument sourceDocument,
			SingularAttributeSourceManyToOne manyToOneSource,
			ManyToOne manyToOneBinding,
			String containingClassName) {
		final String referencedEntityName =
				handleReferencedEntity( sourceDocument, manyToOneSource, manyToOneBinding, containingClassName );

		if ( manyToOneSource.isUnique() ) {
			manyToOneBinding.markAsLogicalOneToOne();
		}

		bindManyToOneAttribute( sourceDocument, manyToOneSource, manyToOneBinding, referencedEntityName );

		handlePropertyRef( sourceDocument, manyToOneBinding,
				"<many-to-one name=\"" + manyToOneSource.getName() + "\"/>" );

		final var property = new Property();
		property.setValue( manyToOneBinding );
		bindProperty( sourceDocument, manyToOneSource, property );

		checkManyToOneOrphanDelete( sourceDocument, manyToOneSource, manyToOneBinding );

		return property;
	}

	private String handleReferencedEntity(
			MappingDocument sourceDocument,
			SingularAttributeSourceManyToOne manyToOneSource,
			ManyToOne manyToOneBinding,
			String containingClassName) {
		final String explicitReferencedEntityName = manyToOneSource.getReferencedEntityName();
		if ( explicitReferencedEntityName != null ) {
			return explicitReferencedEntityName;
		}
		else {
			final String attributeName = manyToOneSource.getName();
			final var reflectedPropertyClass =
					reflectedPropertyClass( sourceDocument, containingClassName, attributeName );
			if ( reflectedPropertyClass != null ) {
				return reflectedPropertyClass.getName();
			}
			else {
				prepareValueTypeViaReflection(
						sourceDocument,
						manyToOneBinding,
						containingClassName,
						attributeName,
						manyToOneSource.getAttributeRole()
				);
				return manyToOneBinding.getTypeName();
			}
		}
	}

	private static void checkManyToOneOrphanDelete(
			MappingDocument sourceDocument,
			SingularAttributeSourceManyToOne manyToOneSource,
			ManyToOne manyToOneBinding) {
		// TODO: would be better to delay this until the end of binding (second pass, etc)
		//       in order to properly allow for a singular unique column for a many-to-one to
		//       to also trigger a "logical one-to-one". As is, this can occasionally lead to
		//       false exceptions if the many-to-one column binding is delayed and the
		//       uniqueness is indicated on the <column/> rather than on the <many-to-one/>
		//
		//       Ideally, would love to see a SimpleValue#validate approach, rather than a
		//       SimpleValue#isValid that is then handled at a higher level (Property, etc).
		//       The reason being that the current approach misses the exact reason a
		//       "validation" fails since it loses "context"
		final String cascadeStyleName = manyToOneSource.getCascadeStyleName();
		if ( cascadeStyleName != null && cascadeStyleName.contains( "delete-orphan" )
			&& !manyToOneBinding.isLogicalOneToOne() ) {
			throw new MappingException(
					String.format(
							Locale.ENGLISH,
							"many-to-one attribute [%s] specified delete-orphan but is not specified as unique; " +
							"remove delete-orphan cascading or specify unique=\"true\"",
							manyToOneSource.getAttributeRole().getFullPath()
					),
					sourceDocument.getOrigin()
			);
		}
	}

	private static void checkConstrainedOneToOneOrphanDelete(
			MappingDocument sourceDocument,
			SingularAttributeSourceOneToOne oneToOneSource) {
		final String cascadeStyleName = oneToOneSource.getCascadeStyleName();
		if ( cascadeStyleName != null && cascadeStyleName.contains( "delete-orphan" ) ) {
			throw new MappingException(
					String.format(
							Locale.ENGLISH,
							"one-to-one attribute [%s] cannot specify orphan delete cascading as it is constrained",
							oneToOneSource.getAttributeRole().getFullPath()
					),
					sourceDocument.getOrigin()
			);
		}
	}

	private void bindManyToOneAttribute(
			final MappingDocument sourceDocument,
			final SingularAttributeSourceManyToOne manyToOneSource,
			ManyToOne manyToOneBinding,
			String referencedEntityName) {
		// NOTE: no type information to bind

		handleReferencedEntity( manyToOneBinding, referencedEntityName,
				manyToOneSource.getReferencedEntityAttributeName() );

		handleFetchCharacteristics( manyToOneSource, manyToOneBinding );

		if ( manyToOneSource.isEmbedXml() == Boolean.TRUE ) {
			DEPRECATION_LOGGER.logDeprecationOfEmbedXmlSupport();
		}

		manyToOneBinding.setIgnoreNotFound( manyToOneSource.isIgnoreNotFound() );

		setForeignKeyName( manyToOneBinding,
				manyToOneSource.getExplicitForeignKeyName() );

		final var columnBinder = new ManyToOneColumnBinder(
				sourceDocument,
				manyToOneSource,
				manyToOneBinding,
				referencedEntityName
		);
		final boolean canBindColumnsImmediately = columnBinder.canProcessImmediately();
		if ( canBindColumnsImmediately ) {
			columnBinder.doSecondPass( null );
		}
		else {
			sourceDocument.getMetadataCollector().addSecondPass( columnBinder );
		}

		if ( !manyToOneSource.isIgnoreNotFound() ) {
			// we skip creating the FK here since this setting tells us there
			// cannot be a suitable/proper FK
			final var fkSecondPass = new ManyToOneFkSecondPass(
					sourceDocument,
					manyToOneSource,
					manyToOneBinding,
					referencedEntityName
			);
			if ( canBindColumnsImmediately && fkSecondPass.canProcessImmediately() ) {
				fkSecondPass.doSecondPass( null );
			}
			else {
				sourceDocument.getMetadataCollector().addSecondPass( fkSecondPass );
			}
		}

		manyToOneBinding.setOnDeleteAction( getOnDeleteAction( manyToOneSource ) );
	}

	private static void handleReferencedEntity(
			ManyToOne manyToOneBinding, String referencedEntityName, String referencedEntityAttributeName) {
		manyToOneBinding.setReferencedEntityName( referencedEntityName );
		if ( isNotEmpty( referencedEntityAttributeName ) ) {
			manyToOneBinding.setReferencedPropertyName( referencedEntityAttributeName );
			manyToOneBinding.setReferenceToPrimaryKey( false );
		}
		else {
			manyToOneBinding.setReferenceToPrimaryKey( true );
		}
	}

	private static void setForeignKeyName(SimpleValue manyToOneBinding, String foreignKeyName) {
		if ( isNotEmpty( foreignKeyName ) ) {
			if ( "none".equals( foreignKeyName ) ) {
				manyToOneBinding.disableForeignKey();
			}
			else {
				manyToOneBinding.setForeignKeyName( foreignKeyName );
			}
		}
	}

	private Property createAnyAssociationAttribute(
			MappingDocument sourceDocument,
			SingularAttributeSourceAny anyMapping,
			Any anyBinding,
			String entityName) {
		final var attributeRole = anyMapping.getAttributeRole();
		bindAny( sourceDocument, anyMapping, anyBinding, attributeRole );
		prepareValueTypeViaReflection( sourceDocument, anyBinding, entityName, anyMapping.getName(), attributeRole );
		anyBinding.createForeignKey();

		final var property = new Property();
		property.setValue( anyBinding );
		bindProperty( sourceDocument, anyMapping, property );
		return property;
	}

	private void bindAny(
			MappingDocument sourceDocument,
			AnyMappingSource anyMapping,
			Any anyBinding,
			AttributeRole attributeRole) {

		anyBinding.setLazy( anyMapping.isLazy() );

		final var keyTypeResolution =
				resolveType( sourceDocument,
						anyMapping.getKeySource().getTypeSource() );
		if ( keyTypeResolution != null ) {
			anyBinding.setIdentifierType( keyTypeResolution.typeName );
		}

		final var discriminatorSource = anyMapping.getDiscriminatorSource();

		final var discriminatorTypeResolution =
				resolveType( sourceDocument, discriminatorSource.getTypeSource() );
		anyBinding.setMetaType( discriminatorTypeName( discriminatorTypeResolution ) );
		anyBinding.setMetaValues( discriminatorValueToEntityNameMap( sourceDocument, anyMapping, attributeRole,
				anyDiscriminatorType( anyBinding, discriminatorTypeResolution ) ) );

		relationalObjectBinder.bindColumnOrFormula(
				sourceDocument,
				discriminatorSource.getRelationalValueSource(),
				anyBinding.getMetaMapping(),
				true,
				context -> implicitNamingStrategy.determineAnyDiscriminatorColumnName(
						discriminatorSource
				)
		);

		relationalObjectBinder.bindColumnsAndFormulas(
				sourceDocument,
				anyMapping.getKeySource().getRelationalValueSources(),
				anyBinding.getKeyMapping(),
				true,
				context -> implicitNamingStrategy.determineAnyKeyColumnName( anyMapping.getKeySource() )
		);
	}

	private static String discriminatorTypeName(TypeResolution discriminatorTypeResolution) {
		return discriminatorTypeResolution != null
			&& discriminatorTypeResolution.typeName != null
				? discriminatorTypeResolution.typeName
				: StandardBasicTypes.STRING.getName();
	}

	private BasicType<?> anyDiscriminatorType(Any anyBinding, TypeResolution discriminatorTypeResolution) {
		if ( discriminatorTypeResolution != null
				&& discriminatorTypeResolution.typeName != null ) {
			return resolveExplicitlyNamedAnyDiscriminatorType(
					discriminatorTypeResolution.typeName,
					discriminatorTypeResolution.parameters,
					anyBinding.getMetaMapping()
			);
		}
		else {
			return
					metadataBuildingContext.getBootstrapContext().getTypeConfiguration().getBasicTypeRegistry()
							.resolve( StandardBasicTypes.STRING );
		}
	}

	private static HashMap<Object, String> discriminatorValueToEntityNameMap(
			MappingDocument sourceDocument,
			AnyMappingSource anyMapping,
			AttributeRole attributeRole,
			BasicType<?> discriminatorType) {
		final HashMap<Object,String> discriminatorValueToEntityNameMap = new HashMap<>();
		anyMapping.getDiscriminatorSource().getValueMappings().forEach(
				(discriminatorValueString, entityName) -> {
					try {
						final Object discriminatorValue =
								discriminatorType.getJavaTypeDescriptor()
										.fromString( discriminatorValueString );
						discriminatorValueToEntityNameMap.put( discriminatorValue, entityName );
					}
					catch (Exception exception) {
						throw new MappingException(
								String.format(
										Locale.ENGLISH,
										"Unable to interpret <meta-value value=\"%s\" class=\"%s\"/> defined as part of <any/> attribute [%s]",
										discriminatorValueString,
										entityName,
										attributeRole.getFullPath()
								),
								exception,
								sourceDocument.getOrigin()
						);
					}
				}
		);
		return discriminatorValueToEntityNameMap;
	}

	private BasicType<?> resolveExplicitlyNamedAnyDiscriminatorType(
			String typeName,
			Map<String,String> parameters,
			Any.MetaValue discriminatorMapping) {
		final var bootstrapContext = metadataBuildingContext.getBootstrapContext();
		final var typeConfiguration = bootstrapContext.getTypeConfiguration();

		if ( isEmpty( parameters ) ) {
			// can use a standard one
			final var basicTypeByName =
					typeConfiguration.getBasicTypeRegistry()
							.getRegisteredType( typeName );
			if ( basicTypeByName != null ) {
				return basicTypeByName;
			}
		}

		// see if it is a named TypeDefinition
		final var typeDefinition =
				metadataBuildingContext.getTypeDefinitionRegistry()
						.resolve( typeName );
		if ( typeDefinition != null ) {
			final var resolution =
					typeDefinition.resolve( parameters, metadataBuildingContext,
							typeConfiguration.getCurrentBaseSqlTypeIndicators() );
			if ( resolution.getCombinedTypeParameters() != null ) {
				discriminatorMapping.setTypeParameters( resolution.getCombinedTypeParameters() );
			}
			return resolution.getLegacyResolvedBasicType();
		}
		else {
			final var classLoaderService = bootstrapContext.getClassLoaderService();
			try {
				final Object typeInstance =
						typeInstance( typeName,
								classLoaderService.classForName( typeName ) );
				if ( typeInstance instanceof ParameterizedType parameterizedType
						&& parameters != null ) {
					final var properties = new Properties();
					properties.putAll( parameters );
					parameterizedType.setParameterValues( properties );
				}
				return typeInstance instanceof UserType<?> userType
						? new CustomType<>( userType, typeConfiguration )
						: (BasicType<?>) typeInstance;
			}
			catch (ClassLoadingException e) {
				BOOT_LOGGER.unableToLoadExplicitAnyDiscriminatorType( typeName );
			}

			throw new org.hibernate.MappingException(
					"Unable to resolve explicit any-discriminator type name:"
					+ typeName
			);
		}
	}

	private Object typeInstance(String typeName, Class<?> typeJavaType) {
		if ( !metadataBuildingContext.getBuildingOptions().isAllowExtensionsInCdi() ) {
			return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( typeJavaType );
		}
		else {
			final String beanName = typeName + ":" + TypeDefinition.NAME_COUNTER.getAndIncrement();
			return metadataBuildingContext.getBootstrapContext().getManagedBeanRegistry()
					.getBean( beanName, typeJavaType ).getBeanInstance();
		}
	}

	private void prepareValueTypeViaReflection(
			MappingDocument sourceDocument,
			Value value,
			String containingClassName,
			String propertyName,
			AttributeRole attributeRole) {
		if ( StringHelper.isEmpty( propertyName ) ) {
			throw new MappingException(
					String.format(
							Locale.ENGLISH,
							"Attribute mapping must define a name attribute:"
								+ " containingClassName=[%s], propertyName=[%s], role=[%s]",
							containingClassName,
							propertyName,
							attributeRole.getFullPath()
					),
					sourceDocument.getOrigin()
			);
		}

		try {
			value.setTypeUsingReflection( containingClassName, propertyName );
		}
		catch (org.hibernate.MappingException ome) {
			throw new MappingException(
					String.format(
							Locale.ENGLISH,
							"Error calling Value#setTypeUsingReflection:"
								+ " containingClassName=[%s], propertyName=[%s], role=[%s]",
							containingClassName,
							propertyName,
							attributeRole.getFullPath()
					),
					ome,
					sourceDocument.getOrigin()
			);
		}
	}

	private void bindProperty(
			MappingDocument mappingDocument,
			AttributeSource propertySource,
			Property property) {
		property.setName( propertySource.getName() );

		final String propertyAccessorName = propertySource.getPropertyAccessorName();
		property.setPropertyAccessorName( isNotEmpty( propertyAccessorName )
				? propertyAccessorName
				: mappingDocument.getEffectiveDefaults().getDefaultAccessStrategyName() );

		if ( propertySource instanceof CascadeStyleSource cascadeStyleSource ) {
			final String cascadeStyleName = cascadeStyleSource.getCascadeStyleName();
			property.setCascade( isNotEmpty( cascadeStyleName )
					? cascadeStyleName
					: toCascadeString( mappingDocument.getEffectiveDefaults().getDefaultCascadeTypes() ) );
		}

		property.setOptimisticLocked( propertySource.isIncludedInOptimisticLocking() );

		if ( propertySource.isSingular() ) {
			final var singularAttributeSource = (SingularAttributeSource) propertySource;
			property.setInsertable( singularAttributeSource.isInsertable() );
			property.setUpdatable( singularAttributeSource.isUpdatable() );
			// isBytecodeLazy() refers to whether a property is lazy via bytecode enhancement (not proxies)
			property.setLazy( singularAttributeSource.isBytecodeLazy() );
			handleGenerationTiming( mappingDocument, propertySource, property,
					singularAttributeSource.getGenerationTiming() );
		}

		property.setMetaAttributes( propertySource.getToolingHintContext().getMetaAttributeMap() );

		if ( BOOT_LOGGER.isTraceEnabled() ) {
			BOOT_LOGGER.trace( "Mapped property: " + propertySource.getName()
								+ " -> [" + columns( property.getValue() ) + "]" );
		}
	}

	private String toCascadeString(EnumSet<CascadeType> defaultCascadeTypes) {
		if ( isEmpty( defaultCascadeTypes ) ) {
			return "none";
		}
		else {
			boolean firstPass = true;
			final var buffer = new StringBuilder();
			for ( var cascadeType : defaultCascadeTypes ) {
				if ( firstPass ) {
					firstPass = false;
				}
				else {
					buffer.append( ", " );
				}
				buffer.append( cascadeType.name().toLowerCase( Locale.ROOT ) );
			}
			return buffer.toString();
		}
	}

	private static void handleGenerationTiming(
			MappingDocument mappingDocument,
			AttributeSource propertySource,
			Property property,
			GenerationTiming timing) {
		if ( timing != null ) {
			if ( (timing == GenerationTiming.INSERT || timing == GenerationTiming.UPDATE)
					&& property.getValue() instanceof SimpleValue simpleValue
					&& simpleValue.isVersion() ) {
				// this is enforced by DTD, but just make sure
				throw new MappingException(
						"'generated' attribute cannot be 'insert' or 'update' for version/timestamp property",
						mappingDocument.getOrigin()
				);
			}
			if ( timing != GenerationTiming.NEVER ) {
				property.setValueGeneratorCreator(context -> new GeneratedGeneration( timing.getEventTypes() ) );

				// generated properties can *never* be insertable...
				if ( property.isInsertable() && timing.includesInsert() ) {
					BOOT_LOGGER.tracef(
							"Property [%s] specified %s generation, setting insertable to false: %s",
							propertySource.getName(),
							timing.name(),
							mappingDocument.getOrigin()
					);
					property.setInsertable( false );
				}

				// properties generated on update can never be updatable...
				if ( property.isUpdatable() && timing.includesUpdate() ) {
					BOOT_LOGGER.tracef(
							"Property [%s] specified ALWAYS generation, setting updateable to false: %s",
							propertySource.getName(),
							mappingDocument.getOrigin()
					);
					property.setUpdatable( false );
				}
			}
		}
	}

	private void bindComponent(
			MappingDocument sourceDocument,
			EmbeddableSource embeddableSource,
			Component component,
			String containingClassName,
			String propertyName,
			boolean isVirtual) {
		bindComponent(
				sourceDocument,
				embeddableSource.getAttributeRoleBase().getFullPath(),
				embeddableSource,
				component,
				extractExplicitComponentClassName( embeddableSource ),
				containingClassName,
				propertyName,
				isVirtual,
				isVirtual,
				embeddableSource.isDynamic()
		);
	}

	private String extractExplicitComponentClassName(EmbeddableSource embeddableSource) {
		final var typeDescriptor = embeddableSource.getTypeDescriptor();
		return typeDescriptor == null ? null : typeDescriptor.getName();
	}

	private void bindComponent(
			MappingDocument sourceDocument,
			String role,
			EmbeddableSource embeddableSource,
			Component componentBinding,
			String explicitComponentClassName,
			String containingClassName,
			String propertyName,
			boolean isComponentEmbedded,
			boolean isVirtual,
			boolean isDynamic) {

		componentBinding.setMetaAttributes( embeddableSource.getToolingHintContext().getMetaAttributeMap() );
		componentBinding.setRoleName( role );
		componentBinding.setEmbedded( isComponentEmbedded );

		// todo : better define the conditions in this if/else
		if ( isDynamic ) {
			bindDynamic( role, componentBinding );
		}
		else if ( isVirtual ) {
			bindVirtual( role, componentBinding );
		}
		else {
			bindComponent(
					sourceDocument,
					role,
					componentBinding,
					explicitComponentClassName,
					containingClassName,
					propertyName
			);
		}

		// todo : anything else to pass along?
		bindAllCompositeAttributes( sourceDocument, embeddableSource, componentBinding );

		final String parentReferenceAttributeName =
				embeddableSource.getParentReferenceAttributeName();
		if ( parentReferenceAttributeName != null ) {
			componentBinding.setParentProperty( parentReferenceAttributeName );
		}

		if ( embeddableSource.isUnique() ) {
			createUniqueKeyForEmbeddable( componentBinding );
		}
	}

	private void createUniqueKeyForEmbeddable(Component componentBinding) {
		final ArrayList<Column> columns = new ArrayList<>();
		for ( var selectable: componentBinding.getSelectables() ) {
			if ( selectable instanceof Column column ) {
				columns.add( column );
			}
		}
		//TODO: we may need to delay this
		componentBinding.getOwner().getTable()
				.createUniqueKey( columns, metadataBuildingContext );
	}

	private static void bindDynamic(String role, Component componentBinding) {
		// dynamic is represented as a Map
		BOOT_LOGGER.bindingDynamicComponent( role );
		componentBinding.setDynamic( true );
	}

	private static void bindVirtual(String role, Component componentBinding) {
		// virtual (what used to be called embedded) is just a conceptual composition...
		// <properties/> for example
		if ( componentBinding.getOwner().hasPojoRepresentation() ) {
			final String ownerClassName = componentBinding.getOwner().getClassName();
			BOOT_LOGGER.bindingVirtualComponentToOwner( role, ownerClassName );
			componentBinding.setComponentClassName( ownerClassName );
		}
		else {
			BOOT_LOGGER.bindingVirtualComponentAsDynamic( role );
			componentBinding.setDynamic( true );
		}
	}

	private static void bindComponent(
			MappingDocument sourceDocument,
			String role,
			Component componentBinding,
			String explicitComponentClassName,
			String containingClassName,
			String propertyName) {
		BOOT_LOGGER.bindingComponent( role );
		if ( isNotEmpty( explicitComponentClassName ) ) {
			try {
				final Class<?> componentClass =
						sourceDocument.getBootstrapContext().getClassLoaderAccess()
							.classForName( explicitComponentClassName );
				if ( CompositeUserType.class.isAssignableFrom( componentClass ) ) {
					@SuppressWarnings("unchecked") // Safe, we just checked
					final var compositeTypeClass =
							(Class<? extends CompositeUserType<?>>)
									componentClass;
					componentBinding.setTypeName( explicitComponentClassName );
					explicitComponentClassName =
							compositeUserType( sourceDocument, compositeTypeClass )
									.embeddable().getName();
				}
			}
			catch (ClassLoadingException ex) {
				BOOT_LOGGER.couldLoadComponentClass( explicitComponentClassName, ex );
			}
			BOOT_LOGGER.bindingComponentToExplicitClass( role, explicitComponentClassName );
			componentBinding.setComponentClassName( explicitComponentClassName );
		}
		else if ( componentBinding.getOwner().hasPojoRepresentation() ) {
			BOOT_LOGGER.attemptingToDetermineComponentClassByReflection( role );
			final var reflectedComponentClass =
					isNotEmpty( containingClassName ) && isNotEmpty( propertyName )
							? reflectedPropertyClass( sourceDocument, containingClassName, propertyName )
							: null;
			if ( reflectedComponentClass == null ) {
				BOOT_LOGGER.unableToDetermineComponentClassByReflection( role );
			}
			else {
				componentBinding.setComponentClassName( reflectedComponentClass.getName() );
			}
		}
		else {
			componentBinding.setDynamic( true );
		}
	}

	private static CompositeUserType<?> compositeUserType(
			MappingDocument sourceDocument,
			Class<? extends CompositeUserType<?>> componentClass) {
		return sourceDocument.getBuildingOptions().isAllowExtensionsInCdi()
				? sourceDocument.getBootstrapContext().getManagedBeanRegistry()
						.getBean( componentClass ).getBeanInstance()
				: FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( componentClass );
	}

	private void bindAllCompositeAttributes(
			MappingDocument sourceDocument,
			EmbeddableSource embeddableSource,
			Component component) {

		for ( var attributeSource : embeddableSource.attributeSources() ) {
			Property attribute;
			if ( attributeSource instanceof SingularAttributeSourceBasic singularAttributeSourceBasic ) {
				attribute = createBasicAttribute(
						sourceDocument,
						singularAttributeSourceBasic,
						new BasicValue( sourceDocument, component.getTable() ),
						component.getComponentClassName()
				);
			}
			else if ( attributeSource instanceof SingularAttributeSourceEmbedded singularAttributeSourceEmbedded ) {
				attribute = createEmbeddedAttribute(
						sourceDocument,
						singularAttributeSourceEmbedded,
						new Component( sourceDocument, component ),
						component.getComponentClassName()
				);
			}
			else if ( attributeSource instanceof SingularAttributeSourceManyToOne singularAttributeSourceManyToOne ) {
				attribute = createManyToOneAttribute(
						sourceDocument,
						singularAttributeSourceManyToOne,
						new ManyToOne( sourceDocument, component.getTable() ),
						component.getComponentClassName()
				);
			}
			else if ( attributeSource instanceof SingularAttributeSourceOneToOne singularAttributeSourceOneToOne ) {
				attribute = createOneToOneAttribute(
						sourceDocument,
						singularAttributeSourceOneToOne,
						new OneToOne( sourceDocument, component.getTable(), component.getOwner() ),
						component.getComponentClassName()
				);
			}
			else if ( attributeSource instanceof SingularAttributeSourceAny singularAttributeSourceAny ) {
				attribute = createAnyAssociationAttribute(
						sourceDocument,
						singularAttributeSourceAny,
						new Any( sourceDocument, component.getTable() ),
						component.getComponentClassName()
				);
			}
			else if ( attributeSource instanceof PluralAttributeSource pluralAttributeSource ) {
				attribute = createPluralAttribute(
						sourceDocument,
						pluralAttributeSource,
						component.getOwner()
				);
			}
			else {
				throw new AssertionFailure(
						String.format(
								Locale.ENGLISH,
								"Unexpected AttributeSource subtype [%s] as part of composite [%s]",
								attributeSource.getClass().getName(),
								attributeSource.getAttributeRole().getFullPath()
						)

				);
			}

			attribute.setOptional( attribute.getValue().isNullable() );

			component.addProperty( attribute );
		}
	}

	private static void bindSimpleValueType(
			MappingDocument mappingDocument,
			HibernateTypeSource typeSource,
			SimpleValue simpleValue) {
		if ( mappingDocument.getBuildingOptions().useNationalizedCharacterData() ) {
			simpleValue.makeNationalized();
		}

		final var typeResolution = resolveType( mappingDocument, typeSource );
		if ( typeResolution != null ) {
			if ( isNotEmpty( typeResolution.parameters ) ) {
				simpleValue.setTypeParameters( typeResolution.parameters );
				if ( simpleValue instanceof BasicValue basicValue ) {
					basicValue.setExplicitTypeParams( typeResolution.parameters );
				}
			}

			if ( typeResolution.typeName != null ) {
				simpleValue.setTypeName( typeResolution.typeName );
			}
		}
		// otherwise no explicit type info was found
	}

	private static class TypeResolution {
		private final String typeName;
		private final Map<String,String> parameters;

		public TypeResolution(String typeName, Map<String,String> parameters) {
			this.typeName = typeName;
			this.parameters = parameters;
		}
	}

	private static TypeResolution resolveType(
			MappingDocument sourceDocument,
			HibernateTypeSource typeSource) {
		final String typeSourceName = typeSource.getName();
		if ( StringHelper.isEmpty( typeSourceName ) ) {
			return null;
		}

		final var typeDefinition =
				sourceDocument.getMetadataCollector()
						.getTypeDefinition( typeSourceName );
		final Map<String,String> typeParameters = new HashMap<>();
		final String typeName;
		if ( typeDefinition == null ) {
			typeName = typeSourceName;
		}
		else {
			// the explicit name referred to a type-def
			typeName = typeDefinition.getTypeImplementorClass().getName();
			final var parameters = typeDefinition.getParameters();
			if ( parameters != null ) {
				typeParameters.putAll( parameters );
			}
		}
		// parameters on the property mapping should override parameters in the type-def
		final var parameters = typeSource.getParameters();
		if ( parameters != null ) {
			typeParameters.putAll( parameters );
		}
		return new TypeResolution( typeName, typeParameters );
	}

	private Table bindEntityTableSpecification(
			final MappingDocument mappingDocument,
			TableSpecificationSource tableSpecSource,
			Table denormalizedSuperTable,
			final EntitySource entitySource,
			PersistentClass entityDescriptor) {
		final String contributorName = mappingDocument.getCurrentContributorName();
		final boolean isAbstract = entityDescriptor.isAbstract() != null && entityDescriptor.isAbstract();
		final Identifier logicalTableName;
		final Table table;
		if ( tableSpecSource instanceof TableSource tableSource ) {
			logicalTableName = logicalTableName( mappingDocument, entitySource, tableSource );
			table = tableForEntity(
					tableSource,
					denormalizedSuperTable,
					logicalTableName,
					contributorName,
					isAbstract
			);
		}
		else if ( tableSpecSource instanceof InLineViewSource inlineViewSource )  {
			logicalTableName = database.toIdentifier( inlineViewSource.getLogicalName() );
			table = tableForSubselect(
					inlineViewSource,
					denormalizedSuperTable,
					contributorName,
					isAbstract,
					logicalTableName
			);
		}
		else {
			throw new AssertionFailure( "Unexpected table specification source type" );
		}

		final var metadataCollector = mappingDocument.getMetadataCollector();

		metadataCollector.addEntityTableXref(
				entitySource.getEntityNamingSource().getEntityName(),
				logicalTableName,
				table,
				superEntityTableXref( mappingDocument, entitySource, entityDescriptor, metadataCollector )
		);

		if ( tableSpecSource instanceof TableSource tableSource ) {
			table.setRowId( tableSource.getRowId() );
			final String checkConstraint = tableSource.getCheckConstraint();
			if ( isNotEmpty( checkConstraint ) ) {
				table.addCheckConstraint( checkConstraint );
			}
		}

		table.setComment( tableSpecSource.getComment() );

		metadataCollector.addTableNameBinding( logicalTableName, table );

		return table;
	}

	private Identifier logicalTableName(
			MappingDocument mappingDocument, EntitySource entitySource, TableSource tableSource) {
		if ( isNotEmpty( tableSource.getExplicitTableName() ) ) {
			return database.toIdentifier( tableSource.getExplicitTableName() );
		}
		else {
			return mappingDocument.getBuildingOptions().getImplicitNamingStrategy()
					.determinePrimaryTableName( new ImplicitEntityNameSource() {
						@Override
						public EntityNaming getEntityNaming() {
							return entitySource.getEntityNamingSource();
						}
						@Override
						public MetadataBuildingContext getBuildingContext() {
							return mappingDocument;
						}
					} );
		}
	}

	private Table tableForEntity(
			TableSource table,
			Table denormalizedSuperTable,
			Identifier logicalTableName,
			String contributorName,
			boolean isAbstract) {
		final var namespace = locateTableNamespace( table );
		if ( denormalizedSuperTable == null ) {
			return namespace.createTable(
					logicalTableName,
					(identifier) -> new Table(
							contributorName,
							namespace,
							identifier,
							isAbstract
					)
			);
		}
		else {
			return namespace.createDenormalizedTable(
					logicalTableName,
					(physicalTableName) -> new DenormalizedTable(
							contributorName,
							namespace,
							physicalTableName,
							isAbstract,
							denormalizedSuperTable
					)
			);
		}
	}

	private Table tableForSubselect(
			InLineViewSource inlineView,
			Table denormalizedSuperTable,
			String contributorName,
			boolean isAbstract,
			Identifier logicalTableName) {
		final var namespace = locateTableNamespace( inlineView );
		final String subselect = inlineView.getSelectStatement();
		final Table table;
		if ( denormalizedSuperTable == null ) {
			table = new Table( contributorName, namespace, subselect, isAbstract );
		}
		else {
			table = new DenormalizedTable(
					contributorName,
					namespace,
					subselect,
					isAbstract,
					denormalizedSuperTable
			);
		}
		table.setName( logicalTableName.render() );
		return table;
	}

	private static EntityTableXref superEntityTableXref(
			MappingDocument mappingDocument,
			EntitySource entitySource,
			PersistentClass entityDescriptor,
			InFlightMetadataCollector metadataCollector) {
		final var superType = entitySource.getSuperType();
		if ( superType != null ) {
			final var supertype = (EntitySource) superType;
			final String superEntityName = supertype.getEntityNamingSource().getEntityName();
			final var superEntityTableXref = metadataCollector.getEntityTableXref( superEntityName );
			if ( superEntityTableXref == null ) {
				throw new MappingException(
						String.format(
								Locale.ENGLISH,
								"Unable to locate entity table xref for entity [%s] super-type [%s]",
								entityDescriptor.getEntityName(),
								superEntityName
						),
						mappingDocument.getOrigin()
				);
			}
			return superEntityTableXref;
		}
		else {
			return null;
		}
	}

	private Identifier determineCatalogName(TableSpecificationSource tableSpecSource) {
		return isNotEmpty( tableSpecSource.getExplicitCatalogName() )
				? database.toIdentifier( tableSpecSource.getExplicitCatalogName() )
				: null;
	}

	private Identifier determineSchemaName(TableSpecificationSource tableSpecSource) {
		return isNotEmpty( tableSpecSource.getExplicitSchemaName() )
				? database.toIdentifier( tableSpecSource.getExplicitSchemaName() )
				: null;
	}

	private static void bindCustomSql(
			EntitySource entitySource,
			PersistentClass entityDescriptor) {
		if ( entitySource.getCustomSqlInsert() != null ) {
			entityDescriptor.setCustomSQLInsert(
					entitySource.getCustomSqlInsert().sql(),
					entitySource.getCustomSqlInsert().callable(),
					entitySource.getCustomSqlInsert().checkStyle()
			);
		}

		if ( entitySource.getCustomSqlUpdate() != null ) {
			entityDescriptor.setCustomSQLUpdate(
					entitySource.getCustomSqlUpdate().sql(),
					entitySource.getCustomSqlUpdate().callable(),
					entitySource.getCustomSqlUpdate().checkStyle()
			);
		}

		if ( entitySource.getCustomSqlDelete() != null ) {
			entityDescriptor.setCustomSQLDelete(
					entitySource.getCustomSqlDelete().sql(),
					entitySource.getCustomSqlDelete().callable(),
					entitySource.getCustomSqlDelete().checkStyle()
			);
		}

		entityDescriptor.setLoaderName( entitySource.getCustomLoaderName() );
	}

	private static void bindCustomSql(
			SecondaryTableSource secondaryTableSource,
			Join secondaryTable) {
		if ( secondaryTableSource.getCustomSqlInsert() != null ) {
			secondaryTable.setCustomSQLInsert(
					secondaryTableSource.getCustomSqlInsert().sql(),
					secondaryTableSource.getCustomSqlInsert().callable(),
					secondaryTableSource.getCustomSqlInsert().checkStyle()
			);
		}

		if ( secondaryTableSource.getCustomSqlUpdate() != null ) {
			secondaryTable.setCustomSQLUpdate(
					secondaryTableSource.getCustomSqlUpdate().sql(),
					secondaryTableSource.getCustomSqlUpdate().callable(),
					secondaryTableSource.getCustomSqlUpdate().checkStyle()
			);
		}

		if ( secondaryTableSource.getCustomSqlDelete() != null ) {
			secondaryTable.setCustomSQLDelete(
					secondaryTableSource.getCustomSqlDelete().sql(),
					secondaryTableSource.getCustomSqlDelete().callable(),
					secondaryTableSource.getCustomSqlDelete().checkStyle()
			);
		}
	}

	private void registerSecondPass(SecondPass secondPass, MetadataBuildingContext context) {
		context.getMetadataCollector().addSecondPass( secondPass );
	}


	public static final class DelayedPropertyReferenceHandlerImpl
			implements InFlightMetadataCollector.DelayedPropertyReferenceHandler {
		public final String referencedEntityName;
		public final String referencedPropertyName;
		public final boolean isUnique;
		private final String sourceElementSynopsis;
		public final Origin propertyRefOrigin;

		public DelayedPropertyReferenceHandlerImpl(
				String referencedEntityName,
				String referencedPropertyName,
				boolean isUnique,
				String sourceElementSynopsis,
				Origin propertyRefOrigin) {
			this.referencedEntityName = referencedEntityName;
			this.referencedPropertyName = referencedPropertyName;
			this.isUnique = isUnique;
			this.sourceElementSynopsis = sourceElementSynopsis;
			this.propertyRefOrigin = propertyRefOrigin;
		}

		public void process(InFlightMetadataCollector metadataCollector) {
			BOOT_LOGGER.tracef(
					"Performing delayed property-ref handling [%s, %s, %s]",
					referencedEntityName,
					referencedPropertyName,
					sourceElementSynopsis
			);

			final var entityBinding = metadataCollector.getEntityBinding( referencedEntityName );
			if ( entityBinding == null ) {
				throw new MappingException(
						String.format(
								Locale.ENGLISH,
								"property-ref [%s] referenced an unmapped entity [%s]",
								sourceElementSynopsis,
								referencedEntityName
						),
						propertyRefOrigin
				);
			}

			final var propertyBinding = entityBinding.getReferencedProperty( referencedPropertyName );
			if ( propertyBinding == null ) {
				throw new MappingException(
						String.format(
								Locale.ENGLISH,
								"property-ref [%s] referenced an unknown entity property [%s#%s]",
								sourceElementSynopsis,
								referencedEntityName,
								referencedPropertyName
						),
						propertyRefOrigin
				);
			}

			if ( isUnique ) {
				( (SimpleValue) propertyBinding.getValue() ).setAlternateUniqueKey( true );
			}
		}
	}

	private abstract class AbstractPluralAttributeSecondPass implements SecondPass {
		private final MappingDocument mappingDocument;
		private final PluralAttributeSource pluralAttributeSource;
		private final Collection collectionBinding;

		protected AbstractPluralAttributeSecondPass(
				MappingDocument mappingDocument,
				PluralAttributeSource pluralAttributeSource,
				Collection collectionBinding) {
			this.mappingDocument = mappingDocument;
			this.pluralAttributeSource = pluralAttributeSource;
			this.collectionBinding = collectionBinding;
		}

		public MappingDocument getMappingDocument() {
			return mappingDocument;
		}

		public PluralAttributeSource getPluralAttributeSource() {
			return pluralAttributeSource;
		}

		public Collection getCollectionBinding() {
			return collectionBinding;
		}

		@Override
		public void doSecondPass(Map<String, PersistentClass> persistentClasses) {
			bindCollectionTable();

			bindCollectionKey();
			bindCollectionIdentifier();
			bindCollectionIndex();
			bindCollectionElement();

			createBackReferences();

			collectionBinding.createAllKeys();

			if ( BOOT_LOGGER.isTraceEnabled() ) {
				final var collection = getCollectionBinding();
				BOOT_LOGGER.tracef( "Mapped collection: %s",
						getPluralAttributeSource().getAttributeRole().getFullPath() );
				BOOT_LOGGER.tracef( "   + table -> %s", collection.getTable().getName() );
				BOOT_LOGGER.tracef( "   + key -> %s", columns( collection.getKey() ) );
				if ( collection.isIndexed() ) {
					BOOT_LOGGER.tracef( "   + index -> %s",
							columns( ( (IndexedCollection) collection).getIndex() ) );
				}
				if ( collection.isOneToMany() ) {
					BOOT_LOGGER.tracef( "   + one-to-many -> %s",
							( (OneToMany) collection.getElement() ).getReferencedEntityName() );
				}
				else {
					BOOT_LOGGER.tracef( "   + element -> %s",
							columns( collection.getElement() ) );
				}
			}
		}

		private void bindCollectionTable() {
			// 2 main branches here:
			//		1) one-to-many
			//		2) everything else

			if ( pluralAttributeSource.getElementSource()
					instanceof PluralAttributeElementSourceOneToMany elementSource ) {
				// For one-to-many mappings, the "collection table" is the same as the table
				// of the associated entity (the entity making up the collection elements).
				// So lookup the associated entity and use its table here
				final var persistentClass = getReferencedEntityBinding( elementSource.getReferencedEntityName() );
				// even though <key/> defines a property-ref I do not see where legacy
				// code ever attempts to use that to "adjust" the table in its use to
				// the actual table the referenced property belongs to.
				// todo : for correctness, though, we should look into that ^^
				collectionBinding.setCollectionTable( persistentClass.getTable() );
			}
			else {
				final var tableSpecSource = pluralAttributeSource.getCollectionTableSpecificationSource();
				final var namespace = locateTableNamespace( tableSpecSource );

				final Table collectionTable;
				if ( tableSpecSource instanceof TableSource tableSource ) {
					collectionTable = namespace.createTable(
							logicalName( tableSource ),
							identifier -> new Table(
									metadataBuildingContext.getCurrentContributorName(),
									namespace,
									identifier,
									false
							)
					);
				}
				else if ( tableSpecSource instanceof InLineViewSource inlineViewSource ) {
					collectionTable = new Table(
							metadataBuildingContext.getCurrentContributorName(),
							namespace,
							inlineViewSource.getSelectStatement(),
							false
					);
				}
				else {
					throw new AssertionFailure( "Unexpected table specification source type" );
				}

				collectionBinding.setCollectionTable( collectionTable );
			}

			final var collectionTable = collectionBinding.getCollectionTable();

			if ( BOOT_LOGGER.isTraceEnabled() ) {
				BOOT_LOGGER.mappingCollectionToTable(
						collectionBinding.getRole(),
						collectionTable.getName() );
			}

			final String tableComment = pluralAttributeSource.getCollectionTableComment();
			if ( tableComment != null ) {
				collectionTable.setComment( tableComment );
			}
			final String tableCheck = pluralAttributeSource.getCollectionTableCheck();
			if ( tableCheck != null ) {
				collectionTable.addCheckConstraint( tableCheck );
			}
		}

		private Identifier logicalName(TableSource tableSource) {
			final String explicitTableName = tableSource.getExplicitTableName();
			if ( isNotEmpty( explicitTableName ) ) {
				return toIdentifier( explicitTableName,
						mappingDocument.getEffectiveDefaults().isDefaultQuoteIdentifiers() );
			}
			else {
				final var implicitNamingSource =
						new ImplicitCollectionTableNameSource() {
							@Override
							public Identifier getOwningPhysicalTableName() {
								return collectionBinding.getOwner().getTable().getNameIdentifier();
							}

							@Override
							public EntityNaming getOwningEntityNaming() {
								return new EntityNamingSourceImpl( collectionBinding.getOwner() );
							}

							@Override
							public AttributePath getOwningAttributePath() {
								return pluralAttributeSource.getAttributePath();
							}

							@Override
							public MetadataBuildingContext getBuildingContext() {
								return mappingDocument;
							}
						};
				return mappingDocument.getBuildingOptions().getImplicitNamingStrategy()
						.determineCollectionTableName( implicitNamingSource );
			}
		}

		protected void createBackReferences() {
			if ( collectionBinding.isOneToMany()
					&& !collectionBinding.isInverse()
					&& !collectionBinding.getKey().isNullable() ) {
				// for non-inverse one-to-many, with a not-null fk, add a backref!
				final var oneToMany = (OneToMany) collectionBinding.getElement();
				final var referenced =
						getReferencedEntityBinding( oneToMany.getReferencedEntityName() );
				final var backref = new Backref();
				backref.setName( '_' + collectionBinding.getOwnerEntityName()
								+ "." + pluralAttributeSource.getName() + "Backref" );
				backref.setOptional( true );
				backref.setUpdatable( false );
				backref.setSelectable( false );
				backref.setCollectionRole( collectionBinding.getRole() );
				backref.setEntityName( collectionBinding.getOwner().getEntityName() );
				backref.setValue( collectionBinding.getKey() );
				referenced.addProperty( backref );

				if ( BOOT_LOGGER.isTraceEnabled() ) {
					BOOT_LOGGER.tracef(
							"Added virtual backref property [%s] : %s",
							backref.getName(),
							pluralAttributeSource.getAttributeRole().getFullPath()
					);
				}
			}
		}

		protected void bindCollectionKey() {
			final var pluralAttributeSource = getPluralAttributeSource();
			final var keySource = pluralAttributeSource.getKeySource();
			final String referencedPropertyName = keySource.getReferencedPropertyName();
			final var collectionBinding = getCollectionBinding();
			collectionBinding.setReferencedPropertyName( referencedPropertyName );

			final var owner = collectionBinding.getOwner();
			final var keyVal = referencedPropertyName == null
					? owner.getIdentifier()
					: (KeyValue) owner.getRecursiveProperty( referencedPropertyName ).getValue();
			final var key = new DependantValue(
					mappingDocument,
					collectionBinding.getCollectionTable(),
					keyVal
			);
			setForeignKeyName( key,
					keySource.getExplicitForeignKeyName() );
			key.setOnDeleteAction( getOnDeleteAction( keySource ) );

//			final ImplicitJoinColumnNameSource.Nature implicitNamingNature;
//			if ( getPluralAttributeSource().getElementSource() instanceof PluralAttributeElementSourceManyToMany
//					|| getPluralAttributeSource().getElementSource() instanceof PluralAttributeElementSourceOneToMany ) {
//				implicitNamingNature = ImplicitJoinColumnNameSource.Nature.ENTITY_COLLECTION;
//			}
//			else {
//				implicitNamingNature = ImplicitJoinColumnNameSource.Nature.ELEMENT_COLLECTION;
//			}

			relationalObjectBinder.bindColumnsAndFormulas(
					mappingDocument,
					keySource.getRelationalValueSources(),
					key,
					keySource.areValuesNullableByDefault(),
					context -> context.getMetadataCollector().getDatabase()
							.toIdentifier( Collection.DEFAULT_KEY_COLUMN_NAME )
			);

			key.sortProperties();
			key.createForeignKey();
			collectionBinding.setKey( key );

			key.setNullable( keySource.areValuesNullableByDefault() );
			key.setUpdateable( keySource.areValuesIncludedInUpdateByDefault() );
		}

		protected void bindCollectionIdentifier() {
			final var idSource = getPluralAttributeSource().getCollectionIdSource();
			if ( idSource != null ) {
				final var idBagBinding = (IdentifierCollection) getCollectionBinding();
				final var idBinding = new BasicValue(
						mappingDocument,
						idBagBinding.getCollectionTable()
				);

				bindSimpleValueType(
						mappingDocument,
						idSource.getTypeInformation(),
						idBinding
				);

				relationalObjectBinder.bindColumn(
						mappingDocument,
						idSource.getColumnSource(),
						idBinding,
						false,
						context -> database.toIdentifier( IdentifierCollection.DEFAULT_IDENTIFIER_COLUMN_NAME )
				);

				idBagBinding.setIdentifier( idBinding );

				makeIdGenerator(
						mappingDocument,
						new IdentifierGeneratorDefinition( idSource.getGeneratorName(), idSource.getParameters() ),
						idBinding,
						metadataBuildingContext
				);
			}
		}

		protected void bindCollectionIndex() {
		}

		protected void bindCollectionElement() {
			final var pluralElementSource = pluralAttributeSource.getElementSource();
			if ( BOOT_LOGGER.isTraceEnabled() ) {
				BOOT_LOGGER.tracef( "Binding [%s] element type for a [%s]",
						pluralElementSource.getNature(), pluralAttributeSource.getNature() );
			}
			if ( pluralElementSource instanceof PluralAttributeElementSourceBasic elementSource ) {
				bindBasicElement( elementSource );
			}
			else if ( pluralElementSource instanceof PluralAttributeElementSourceEmbedded elementSource ) {
				bindEmbeddedElement( elementSource );
			}
			else if ( pluralElementSource instanceof PluralAttributeElementSourceOneToMany elementSource ) {
				bindOneToManyElement( elementSource );
			}
			else if ( pluralElementSource instanceof PluralAttributeElementSourceManyToMany elementSource ) {
				bindManyToManyElement( elementSource );
			}
			else if ( pluralElementSource instanceof PluralAttributeElementSourceManyToAny elementSource ) {
				bindManyToAnyElement( elementSource );
			}
		}

		private void bindManyToAnyElement(PluralAttributeElementSourceManyToAny elementSource) {
			final var elementBinding =
					new Any( mappingDocument,
							collectionBinding.getCollectionTable() );
			bindAny( mappingDocument, elementSource, elementBinding,
					pluralAttributeSource.getAttributeRole().append( "element" ) );
			collectionBinding.setElement( elementBinding );
			// Collection#setWhere is used to set the "where" clause that applies to the collection table
			// (which is the join table for a many-to-any association).
			// This "where" clause comes from the collection mapping; e.g., <set name="..." ... where="..." .../>
			collectionBinding.setWhere( getPluralAttributeSource().getWhere() );
		}

		private void bindManyToManyElement(PluralAttributeElementSourceManyToMany elementSource) {
			final var elementBinding =
					new ManyToOne( mappingDocument,
							collectionBinding.getCollectionTable() );
			relationalObjectBinder.bindColumnsAndFormulas(
					mappingDocument,
					elementSource.getRelationalValueSources(),
					elementBinding,
					false,
					context -> context.getMetadataCollector().getDatabase()
							.toIdentifier( Collection.DEFAULT_ELEMENT_COLUMN_NAME )
			);
			handleFetchCharacteristics( elementSource, elementBinding );
			setForeignKeyName( elementBinding,
					elementSource.getExplicitForeignKeyName() );
			final String referencedEntityName = elementSource.getReferencedEntityName();
			handleReferencedEntity( elementBinding, referencedEntityName,
					elementSource.getReferencedEntityAttributeName() );
			collectionBinding.setElement( elementBinding );
			final var referencedEntityBinding = getReferencedEntityBinding( referencedEntityName );
			// Collection#setWhere is used to set the "where" clause that applies to the collection table
			// (which is the join table for a many-to-many association).
			// This "where" clause comes from the collection mapping; e.g., <set name="..." ... where="..." .../>
			collectionBinding.setWhere( pluralAttributeSource.getWhere() );
			collectionBinding.setManyToManyWhere(
					getNonEmptyOrConjunctionIfBothNonEmpty(
							referencedEntityBinding.getWhere(),
							elementSource.getWhere()
					)
			);
			collectionBinding.setManyToManyOrdering( elementSource.getOrder() );
			bindManyToManyFilters( elementSource, mappingDocument, collectionBinding, elementBinding );
		}

		private void bindManyToManyFilters(
				PluralAttributeElementSourceManyToMany elementSource,
				MappingDocument mappingDocument,
				Collection collectionBinding,
				ManyToOne elementBinding) {
			if ( !isEmpty( elementSource.getFilterSources() )
					|| elementSource.getWhere() != null ) {
				if ( collectionBinding.getFetchMode() == FetchMode.JOIN
						&& elementBinding.getFetchMode() != FetchMode.JOIN ) {
					throw new MappingException(
							String.format(
									Locale.ENGLISH,
									"many-to-many defining filter or where without join fetching is not " +
											"valid within collection [%s] using join fetching",
									getPluralAttributeSource().getAttributeRole().getFullPath()
							),
							mappingDocument.getOrigin()
					);
				}
			}

			for ( var filterSource : elementSource.getFilterSources() ) {
				if ( filterSource.getName() == null ) {
					if ( BOOT_LOGGER.isTraceEnabled() ) {
						BOOT_LOGGER.tracef(
								"Encountered filter with no name associated with many-to-many [%s]; skipping",
								getPluralAttributeSource().getAttributeRole().getFullPath()
						);
					}
				}
				else {
					if ( filterSource.getCondition() == null ) {
						throw new MappingException(
								String.format(
										Locale.ENGLISH,
										"No filter condition found for filter [%s] associated with many-to-many [%s]",
										filterSource.getName(),
										getPluralAttributeSource().getAttributeRole().getFullPath()
								),
								mappingDocument.getOrigin()
						);
					}
					if ( BOOT_LOGGER.isTraceEnabled() ) {
						BOOT_LOGGER.tracef(
								"Applying many-to-many filter [%s] as [%s] to collection [%s]",
								filterSource.getName(),
								filterSource.getCondition(),
								getPluralAttributeSource().getAttributeRole().getFullPath()
						);
					}
					collectionBinding.addManyToManyFilter(
							filterSource.getName(),
							filterSource.getCondition(),
							filterSource.shouldAutoInjectAliases(),
							filterSource.getAliasToTableMap(),
							filterSource.getAliasToEntityMap()
					);
				}
			}
		}

		private void bindOneToManyElement(PluralAttributeElementSourceOneToMany elementSource) {
			final var elementBinding =
					new OneToMany( mappingDocument,
							collectionBinding.getOwner() );
			collectionBinding.setElement( elementBinding );
			final var referencedEntityBinding =
					getReferencedEntityBinding( elementSource.getReferencedEntityName() );
			collectionBinding.setWhere(
					getNonEmptyOrConjunctionIfBothNonEmpty(
							referencedEntityBinding.getWhere(),
							pluralAttributeSource.getWhere()
					)
			);
			elementBinding.setReferencedEntityName( referencedEntityBinding.getEntityName() );
			elementBinding.setAssociatedClass( referencedEntityBinding );
			elementBinding.setIgnoreNotFound( elementSource.isIgnoreNotFound() );
		}

		private void bindEmbeddedElement(PluralAttributeElementSourceEmbedded elementSource) {
			final var elementBinding = new Component( mappingDocument, collectionBinding );
			final var embeddableSource = elementSource.getEmbeddableSource();
			bindComponent( mappingDocument, embeddableSource, elementBinding, null,
					embeddableSource.getAttributePathBase().getProperty(), false );
			collectionBinding.setElement( elementBinding );
			// Collection#setWhere is used to set the "where" clause that applies to the collection table
			// (the table containing the embeddable elements)
			// This "where" clause comes from the collection mapping; e.g., <set name="..." ... where="..." .../>
			collectionBinding.setWhere( pluralAttributeSource.getWhere() );
		}

		private void bindBasicElement(PluralAttributeElementSourceBasic elementSource) {
			final var elementBinding =
					new BasicValue( mappingDocument,
							collectionBinding.getCollectionTable() );
			bindSimpleValueType( mappingDocument, elementSource.getExplicitHibernateTypeSource(), elementBinding );
			relationalObjectBinder.bindColumnsAndFormulas(
					mappingDocument,
					elementSource.getRelationalValueSources(),
					elementBinding,
					elementSource.areValuesNullableByDefault(),
					context -> context.getMetadataCollector().getDatabase()
							.toIdentifier( Collection.DEFAULT_ELEMENT_COLUMN_NAME )
			);
			collectionBinding.setElement( elementBinding );
			// Collection#setWhere is used to set the "where" clause that applies to the collection table
			// (the table containing the basic elements)
			// This "where" clause comes from the collection mapping; e.g., <set name="..." ... where="..." .../>
			collectionBinding.setWhere( pluralAttributeSource.getWhere() );
		}

		private static void handleFetchCharacteristics(
				PluralAttributeElementSourceManyToMany elementSource, ManyToOne elementBinding) {
			final var characteristics = elementSource.getFetchCharacteristics();
			elementBinding.setLazy( characteristics.getFetchTiming() != FetchTiming.IMMEDIATE );
			elementBinding.setFetchMode(
					characteristics.getFetchStyle() == FetchStyle.SELECT
							? FetchMode.SELECT
							: FetchMode.JOIN
			);
		}

		private PersistentClass getReferencedEntityBinding(String referencedEntityName) {
			final var entityBinding =
					mappingDocument.getMetadataCollector()
							.getEntityBinding( referencedEntityName );
			if ( entityBinding == null ) {
				throw new MappingException(
						String.format(
								Locale.ENGLISH,
								"Collection [%s] references an unmapped entity [%s]",
								getPluralAttributeSource().getAttributeRole().getFullPath(),
								referencedEntityName
						),
						mappingDocument.getOrigin()
				);
			}
			return entityBinding;
		}
	}

	private class PluralAttributeListSecondPass extends AbstractPluralAttributeSecondPass {
		public PluralAttributeListSecondPass(
				MappingDocument sourceDocument,
				IndexedPluralAttributeSource attributeSource,
				org.hibernate.mapping.List collectionBinding) {
			super( sourceDocument, attributeSource, collectionBinding );
		}

		@Override
		public IndexedPluralAttributeSource getPluralAttributeSource() {
			return (IndexedPluralAttributeSource) super.getPluralAttributeSource();
		}

		@Override
		public org.hibernate.mapping.List getCollectionBinding() {
			return (org.hibernate.mapping.List) super.getCollectionBinding();
		}

		@Override
		protected void bindCollectionIndex() {
			bindListOrArrayIndex(
					getMappingDocument(),
					getPluralAttributeSource(),
					getCollectionBinding()
			);
		}

		@Override
		protected void createBackReferences() {
			super.createBackReferences();
			createIndexBackRef(
					getMappingDocument(),
					getPluralAttributeSource(),
					getCollectionBinding()
			);
		}
	}

	private class PluralAttributeSetSecondPass extends AbstractPluralAttributeSecondPass {
		public PluralAttributeSetSecondPass(
				MappingDocument sourceDocument,
				PluralAttributeSource attributeSource,
				Collection collectionBinding) {
			super( sourceDocument, attributeSource, collectionBinding );
		}
	}

	private class PluralAttributeMapSecondPass extends AbstractPluralAttributeSecondPass {
		public PluralAttributeMapSecondPass(
				MappingDocument sourceDocument,
				IndexedPluralAttributeSource attributeSource,
				org.hibernate.mapping.Map collectionBinding) {
			super( sourceDocument, attributeSource, collectionBinding );
		}

		@Override
		public IndexedPluralAttributeSource getPluralAttributeSource() {
			return (IndexedPluralAttributeSource) super.getPluralAttributeSource();
		}

		@Override
		public org.hibernate.mapping.Map getCollectionBinding() {
			return (org.hibernate.mapping.Map) super.getCollectionBinding();
		}

		@Override
		protected void bindCollectionIndex() {
			bindMapKey(
					getMappingDocument(),
					getPluralAttributeSource(),
					getCollectionBinding()
			);
		}

		@Override
		protected void createBackReferences() {
			super.createBackReferences();

			final var collectionBinding = getCollectionBinding();

			boolean indexIsFormula = false;
			for ( var selectable: collectionBinding.getIndex().getSelectables() ) {
				if ( selectable.isFormula() ) {
					indexIsFormula = true;
					break;
				}
			}

			if ( collectionBinding.isOneToMany()
					&& !collectionBinding.getKey().isNullable()
					&& !collectionBinding.isInverse()
					&& !indexIsFormula ) {
				final var oneToMany = (OneToMany) collectionBinding.getElement();
				final String entityName = oneToMany.getReferencedEntityName();
				final var referenced =
						getMappingDocument().getMetadataCollector()
								.getEntityBinding( entityName );
				final var backref = new IndexBackref();
				backref.setName( '_' + collectionBinding.getOwnerEntityName()
								+ "." + getPluralAttributeSource().getName() + "IndexBackref" );
				backref.setOptional( true );
				backref.setUpdatable( false );
				backref.setSelectable( false );
				backref.setCollectionRole( collectionBinding.getRole() );
				backref.setEntityName( collectionBinding.getOwner().getEntityName() );
				backref.setValue( collectionBinding.getIndex() );
				referenced.addProperty( backref );
			}
		}
	}

	private class PluralAttributeBagSecondPass extends AbstractPluralAttributeSecondPass {
		public PluralAttributeBagSecondPass(
				MappingDocument sourceDocument,
				PluralAttributeSource attributeSource,
				Collection collectionBinding) {
			super( sourceDocument, attributeSource, collectionBinding );
		}
	}

	private class PluralAttributeIdBagSecondPass extends AbstractPluralAttributeSecondPass {
		public PluralAttributeIdBagSecondPass(
				MappingDocument sourceDocument,
				PluralAttributeSource attributeSource,
				Collection collectionBinding) {
			super( sourceDocument, attributeSource, collectionBinding );
		}
	}

	private class PluralAttributeArraySecondPass extends AbstractPluralAttributeSecondPass {
		public PluralAttributeArraySecondPass(
				MappingDocument sourceDocument,
				IndexedPluralAttributeSource attributeSource,
				Array collectionBinding) {
			super( sourceDocument, attributeSource, collectionBinding );
		}

		@Override
		public IndexedPluralAttributeSource getPluralAttributeSource() {
			return (IndexedPluralAttributeSource) super.getPluralAttributeSource();
		}

		@Override
		public Array getCollectionBinding() {
			return (Array) super.getCollectionBinding();
		}

		@Override
		protected void bindCollectionIndex() {
			bindListOrArrayIndex(
					getMappingDocument(),
					getPluralAttributeSource(),
					getCollectionBinding()
			);
		}

		@Override
		protected void createBackReferences() {
			super.createBackReferences();
			createIndexBackRef(
					getMappingDocument(),
					getPluralAttributeSource(),
					getCollectionBinding()
			);
		}
	}

	private void createIndexBackRef(
			MappingDocument mappingDocument,
			IndexedPluralAttributeSource pluralAttributeSource,
			IndexedCollection collectionBinding) {
		if ( collectionBinding.isOneToMany()
				&& !collectionBinding.getKey().isNullable()
				&& !collectionBinding.isInverse() ) {
			final var oneToMany = (OneToMany) collectionBinding.getElement();
			final String entityName = oneToMany.getReferencedEntityName();
			final var referenced =
					mappingDocument.getMetadataCollector()
							.getEntityBinding( entityName );
			final var backref = new IndexBackref();
			backref.setName( '_' + collectionBinding.getOwnerEntityName()
							+ "." + pluralAttributeSource.getName() + "IndexBackref" );
			backref.setOptional( true );
			backref.setUpdatable( false );
			backref.setSelectable( false );
			backref.setCollectionRole( collectionBinding.getRole() );
			backref.setEntityName( collectionBinding.getOwner().getEntityName() );
			backref.setValue( collectionBinding.getIndex() );
			referenced.addProperty( backref );
		}
	}

	private class PluralAttributePrimitiveArraySecondPass extends AbstractPluralAttributeSecondPass {
		public PluralAttributePrimitiveArraySecondPass(
				MappingDocument sourceDocument,
				IndexedPluralAttributeSource attributeSource,
				PrimitiveArray collectionBinding) {
			super( sourceDocument, attributeSource, collectionBinding );
		}

		@Override
		public IndexedPluralAttributeSource getPluralAttributeSource() {
			return (IndexedPluralAttributeSource) super.getPluralAttributeSource();
		}

		@Override
		public PrimitiveArray getCollectionBinding() {
			return (PrimitiveArray) super.getCollectionBinding();
		}

		@Override
		protected void bindCollectionIndex() {
			bindListOrArrayIndex(
					getMappingDocument(),
					getPluralAttributeSource(),
					getCollectionBinding()
			);
		}

		@Override
		protected void createBackReferences() {
			super.createBackReferences();
			createIndexBackRef(
					getMappingDocument(),
					getPluralAttributeSource(),
					getCollectionBinding()
			);
		}
	}

	public void bindListOrArrayIndex(
			MappingDocument mappingDocument,
			final IndexedPluralAttributeSource attributeSource,
			org.hibernate.mapping.List collectionBinding) {
		final var indexSource =
				(PluralAttributeSequentialIndexSource)
						attributeSource.getIndexSource();
		final var indexBinding =
				new BasicValue( mappingDocument,
						collectionBinding.getCollectionTable() );
		bindSimpleValueType( mappingDocument, indexSource.getTypeInformation(), indexBinding );

		relationalObjectBinder.bindColumnsAndFormulas(
				mappingDocument,
				indexSource.getRelationalValueSources(),
				indexBinding,
				attributeSource.getElementSource() instanceof PluralAttributeElementSourceOneToMany,
				context -> context.getBuildingOptions().getImplicitNamingStrategy().determineListIndexColumnName(
						new ImplicitIndexColumnNameSource() {
							@Override
							public AttributePath getPluralAttributePath() {
								return attributeSource.getAttributePath();
							}

							@Override
							public MetadataBuildingContext getBuildingContext() {
								return context;
							}
						}
				)
		);

		collectionBinding.setIndex( indexBinding );
		collectionBinding.setBaseIndex( indexSource.getBase() );
	}

	private void bindMapKey(
			final MappingDocument mappingDocument,
			final IndexedPluralAttributeSource pluralAttributeSource,
			final org.hibernate.mapping.Map collectionBinding) {
		final var indexSource = pluralAttributeSource.getIndexSource();
		if ( indexSource instanceof PluralAttributeMapKeySourceBasic mapKeySource ) {
			bindBasicMapKey( mappingDocument, pluralAttributeSource, collectionBinding, mapKeySource );
		}
		else if ( indexSource instanceof PluralAttributeMapKeySourceEmbedded mapKeySource ) {
			bindEmbeddedMapKey( mappingDocument, pluralAttributeSource, collectionBinding, mapKeySource );
		}
		else if ( indexSource instanceof PluralAttributeMapKeyManyToManySource mapKeySource ) {
			bindManyToManyMapKey( mappingDocument, pluralAttributeSource, collectionBinding, mapKeySource );
		}
		else if ( indexSource instanceof PluralAttributeMapKeyManyToAnySource mapKeySource) {
			bindManyToAnyMapKey( mappingDocument, pluralAttributeSource, collectionBinding, mapKeySource );
		}
	}

	private void bindManyToAnyMapKey(
			MappingDocument mappingDocument,
			IndexedPluralAttributeSource pluralAttributeSource,
			org.hibernate.mapping.Map collectionBinding,
			PluralAttributeMapKeyManyToAnySource mapKeySource) {
		final var mapKeyBinding =
				new Any( mappingDocument,
						collectionBinding.getCollectionTable() );
		bindAny( mappingDocument, mapKeySource, mapKeyBinding,
				pluralAttributeSource.getAttributeRole().append( "key" ) );
		collectionBinding.setIndex( mapKeyBinding );
	}

	private void bindManyToManyMapKey(
			MappingDocument mappingDocument,
			IndexedPluralAttributeSource pluralAttributeSource,
			org.hibernate.mapping.Map collectionBinding,
			PluralAttributeMapKeyManyToManySource mapKeySource) {
		final var mapKeyBinding =
				new ManyToOne( mappingDocument,
						collectionBinding.getCollectionTable() );

		mapKeyBinding.setReferencedEntityName( mapKeySource.getReferencedEntityName() );

		relationalObjectBinder.bindColumnsAndFormulas(
				mappingDocument,
				mapKeySource.getRelationalValueSources(),
				mapKeyBinding,
				true,
				context -> implicitNamingStrategy.determineMapKeyColumnName(
						new ImplicitMapKeyColumnNameSource() {
							@Override
							public AttributePath getPluralAttributePath() {
								return pluralAttributeSource.getAttributePath();
							}

							@Override
							public MetadataBuildingContext getBuildingContext() {
								return context;
							}
						}
				)
		);
		collectionBinding.setIndex( mapKeyBinding );
	}

	private void bindEmbeddedMapKey(
			MappingDocument mappingDocument,
			IndexedPluralAttributeSource pluralAttributeSource,
			org.hibernate.mapping.Map collectionBinding,
			PluralAttributeMapKeySourceEmbedded mapKeySource) {
		final var componentBinding = new Component( mappingDocument, collectionBinding );
		bindComponent(
				mappingDocument,
				mapKeySource.getEmbeddableSource(),
				componentBinding,
				null,
				pluralAttributeSource.getName(),
				false
		);
		collectionBinding.setIndex( componentBinding );
	}

	private void bindBasicMapKey(
			MappingDocument mappingDocument,
			IndexedPluralAttributeSource pluralAttributeSource,
			org.hibernate.mapping.Map collectionBinding,
			PluralAttributeMapKeySourceBasic mapKeySource) {
		final var value =
				new BasicValue( mappingDocument,
						collectionBinding.getCollectionTable() );
		bindSimpleValueType( mappingDocument, mapKeySource.getTypeInformation(), value );
		if ( !value.isTypeSpecified() ) {
			throw new MappingException(
					"map index element must specify a type: "
					+ pluralAttributeSource.getAttributeRole().getFullPath(),
					mappingDocument.getOrigin()
			);
		}

		relationalObjectBinder.bindColumnsAndFormulas(
				mappingDocument,
				mapKeySource.getRelationalValueSources(),
				value,
				true,
				context -> database.toIdentifier( IndexedCollection.DEFAULT_INDEX_COLUMN_NAME )
		);

		collectionBinding.setIndex( value );
	}

	private class ManyToOneColumnBinder implements ImplicitColumnNamingSecondPass {
		private final MappingDocument mappingDocument;
		private final SingularAttributeSourceManyToOne manyToOneSource;
		private final ManyToOne manyToOneBinding;

		private final String referencedEntityName;

		private final boolean allColumnsNamed;

		public ManyToOneColumnBinder(
				MappingDocument mappingDocument,
				SingularAttributeSourceManyToOne manyToOneSource,
				ManyToOne manyToOneBinding,
				String referencedEntityName) {
			this.mappingDocument = mappingDocument;
			this.manyToOneSource = manyToOneSource;
			this.manyToOneBinding = manyToOneBinding;
			this.referencedEntityName = referencedEntityName;

			boolean allNamed = true;
			for ( var relationalValueSource : manyToOneSource.getRelationalValueSources() ) {
				if ( relationalValueSource instanceof ColumnSource columnSource ) {
					if ( columnSource.getName() == null ) {
						allNamed = false;
						break;
					}
				}
			}
			this.allColumnsNamed = allNamed;
		}

		public boolean canProcessImmediately() {
			if ( allColumnsNamed ) {
				return true;
			}

			final var referencedEntityBinding =
					mappingDocument.getMetadataCollector()
							.getEntityBinding( referencedEntityName );
			if ( referencedEntityBinding == null ) {
				return false;
			}

			// For implicit naming, we can do it immediately if the associated entity
			// is bound and the reference is to its PK. For property-refs, we'd have to
			// be *sure* that the column(s) for the referenced property is fully bound,
			// and we just cannot know that in today's model.

			return manyToOneSource.getReferencedEntityAttributeName() == null;
		}

		@Override
		public void doSecondPass(Map<String, PersistentClass> persistentClasses) {
			if ( allColumnsNamed ) {
				relationalObjectBinder.bindColumnsAndFormulas(
						mappingDocument,
						manyToOneSource.getRelationalValueSources(),
						manyToOneBinding,
						manyToOneSource.areValuesNullableByDefault(),
						context -> {
							throw new AssertionFailure( "Should not be called" );
						}
				);
			}
			else {
				// Otherwise we have some dependency resolution to do in order to perform
				// implicit naming. If we get here, we assume that there is only a single
				// column making up the FK.

				final var referencedEntityBinding =
						mappingDocument.getMetadataCollector()
								.getEntityBinding( referencedEntityName );

				if ( referencedEntityBinding == null ) {
					throw new AssertionFailure(
							"Unable to locate referenced entity mapping [" + referencedEntityName +
									"] in order to process many-to-one FK : " + manyToOneSource.getAttributeRole().getFullPath()
					);
				}

				relationalObjectBinder.bindColumnsAndFormulas(
						mappingDocument,
						manyToOneSource.getRelationalValueSources(),
						manyToOneBinding,
						manyToOneSource.areValuesNullableByDefault(),
						context -> implicitNamingStrategy.determineBasicColumnName(
								new ImplicitBasicColumnNameSource() {
									@Override
									public AttributePath getAttributePath() {
										return manyToOneSource.getAttributePath();
									}

									@Override
									public boolean isCollectionElement() {
										return false;
									}

									@Override
									public MetadataBuildingContext getBuildingContext() {
										return context;
									}
								}
						)
				);
			}
		}
	}

	private static class ManyToOneFkSecondPass implements FkSecondPass {
		private final MappingDocument mappingDocument;
		private final ManyToOne manyToOneBinding;

		private final String referencedEntityName;
		private final String referencedEntityAttributeName;

		private ManyToOneFkSecondPass(
				MappingDocument mappingDocument,
				SingularAttributeSourceManyToOne manyToOneSource,
				ManyToOne manyToOneBinding,
				String referencedEntityName) {
			if ( referencedEntityName == null ) {
				throw new MappingException(
						"entity name referenced by many-to-one required ["
							+ manyToOneSource.getAttributeRole().getFullPath() + "]",
						mappingDocument.getOrigin()
				);
			}
			this.mappingDocument = mappingDocument;
			this.manyToOneBinding = manyToOneBinding;
			this.referencedEntityName = referencedEntityName;
			this.referencedEntityAttributeName = manyToOneSource.getReferencedEntityAttributeName();
		}

		@Override
		public Value getValue() {
			return manyToOneBinding;
		}

		@Override
		public String getReferencedEntityName() {
			return referencedEntityName;
		}

		@Override
		public boolean isInPrimaryKey() {
			return false;
		}

		@Override
		public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws org.hibernate.MappingException {
			if ( referencedEntityAttributeName == null ) {
				manyToOneBinding.createForeignKey();
			}
			else {
				manyToOneBinding.createPropertyRefConstraints(
						mappingDocument.getMetadataCollector()
								.getEntityBindingMap() );
			}
		}

		public boolean canProcessImmediately() {
			// We can process the FK immediately if it is a reference to the associated
			// entity's PK.
			//
			// There is an assumption here that the columns making up the FK have been bound.
			// We assume the caller checks that
			final var referencedEntityBinding =
					mappingDocument.getMetadataCollector()
							.getEntityBinding( referencedEntityName );
			return referencedEntityBinding != null
				&& referencedEntityAttributeName != null;

		}
	}

	private static class OneToOneFkSecondPass implements FkSecondPass {
		private final OneToOne oneToOneBinding;

		private OneToOneFkSecondPass(OneToOne oneToOneBinding) {
			this.oneToOneBinding = oneToOneBinding;
		}

		@Override
		public Value getValue() {
			return oneToOneBinding;
		}

		@Override
		public String getReferencedEntityName() {
			return oneToOneBinding.getReferencedEntityName();
		}

		@Override
		public boolean isInPrimaryKey() {
			return false;
		}

		@Override
		public void doSecondPass(Map<String, PersistentClass> persistentClasses) {
			oneToOneBinding.createForeignKey();
		}
	}

	private static class NaturalIdUniqueKeyBinderImpl implements NaturalIdUniqueKeyBinder {
		private final MappingDocument mappingDocument;
		private final PersistentClass entityBinding;
		private final List<Property> attributeBindings = new ArrayList<>();

		public NaturalIdUniqueKeyBinderImpl(MappingDocument mappingDocument, PersistentClass entityBinding) {
			this.mappingDocument = mappingDocument;
			this.entityBinding = entityBinding;
		}

		@Override
		public void addAttributeBinding(Property attributeBinding) {
			attributeBindings.add( attributeBinding );
		}

		@Override
		public void process() {
			BOOT_LOGGER.bindingNaturalIdUniqueKey( entityBinding.getEntityName() );

			final List<Identifier> columnNames = new ArrayList<>();

			final var uniqueKey = new UniqueKey( entityBinding.getTable() );
			for ( var attributeBinding : attributeBindings ) {
				for ( var selectable : attributeBinding.getSelectables() ) {
					if ( selectable instanceof Column column ) {
						uniqueKey.addColumn( column );
						columnNames.add( column.getNameIdentifier( mappingDocument ) );
					}
				}
				uniqueKey.addColumns( attributeBinding.getValue() );
			}

			final var uniqueKeyName = mappingDocument.getBuildingOptions().getImplicitNamingStrategy()
					.determineUniqueKeyName(
							new ImplicitUniqueKeyNameSource() {
								@Override
								public Identifier getTableName() {
									return entityBinding.getTable().getNameIdentifier();
								}

								@Override
								public List<Identifier> getColumnNames() {
									return columnNames;
								}

								@Override
								public MetadataBuildingContext getBuildingContext() {
									return mappingDocument;
								}

								@Override
								public Identifier getUserProvidedIdentifier() {
									final String name = uniqueKey.getName();
									return name == null ? null : toIdentifier( name );
								}
							}
					);
			uniqueKey.setName( uniqueKeyName.render( getDialect() ) );

			entityBinding.getTable().addUniqueKey( uniqueKey );
		}

		private Dialect getDialect() {
			return mappingDocument.getMetadataCollector().getDatabase().getDialect();
		}
	}

	private String columns(Value value) {
		final var builder = new StringBuilder();
		for ( var selectable : value.getSelectables() ) {
			if ( !builder.isEmpty() ) {
				builder.append( ", " );
			}
			builder.append( selectable.getText() );
		}
		return builder.toString();
	}

	private static OnDeleteAction getOnDeleteAction(ForeignKeyContributingSource entitySource) {
		return entitySource.isCascadeDeleteEnabled() ? OnDeleteAction.CASCADE : OnDeleteAction.NO_ACTION;
	}
}
