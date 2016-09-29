/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.model.Caching;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitCollectionTableNameSource;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitIdentifierColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitIndexColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitMapKeyColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.source.internal.ImplicitColumnNamingSecondPass;
import org.hibernate.boot.model.source.spi.AnyMappingSource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.CascadeStyleSource;
import org.hibernate.boot.model.source.spi.CollectionIdSource;
import org.hibernate.boot.model.source.spi.ColumnSource;
import org.hibernate.boot.model.source.spi.CompositeIdentifierSource;
import org.hibernate.boot.model.source.spi.EmbeddableSource;
import org.hibernate.boot.model.source.spi.EntitySource;
import org.hibernate.boot.model.source.spi.FilterSource;
import org.hibernate.boot.model.source.spi.HibernateTypeSource;
import org.hibernate.boot.model.source.spi.IdentifiableTypeSource;
import org.hibernate.boot.model.source.spi.IdentifierSourceAggregatedComposite;
import org.hibernate.boot.model.source.spi.IdentifierSourceNonAggregatedComposite;
import org.hibernate.boot.model.source.spi.IdentifierSourceSimple;
import org.hibernate.boot.model.source.spi.InLineViewSource;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.Orderable;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSourceBasic;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSourceEmbedded;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSourceManyToAny;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSourceManyToMany;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSourceOneToMany;
import org.hibernate.boot.model.source.spi.PluralAttributeKeySource;
import org.hibernate.boot.model.source.spi.PluralAttributeMapKeyManyToAnySource;
import org.hibernate.boot.model.source.spi.PluralAttributeMapKeyManyToManySource;
import org.hibernate.boot.model.source.spi.PluralAttributeMapKeySourceBasic;
import org.hibernate.boot.model.source.spi.PluralAttributeMapKeySourceEmbedded;
import org.hibernate.boot.model.source.spi.PluralAttributeSequentialIndexSource;
import org.hibernate.boot.model.source.spi.PluralAttributeSource;
import org.hibernate.boot.model.source.spi.PluralAttributeSourceArray;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.RelationalValueSourceContainer;
import org.hibernate.boot.model.source.spi.SecondaryTableSource;
import org.hibernate.boot.model.source.spi.SingularAttributeSource;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceAny;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceBasic;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceEmbedded;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceManyToOne;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceOneToOne;
import org.hibernate.boot.model.source.spi.Sortable;
import org.hibernate.boot.model.source.spi.TableSource;
import org.hibernate.boot.model.source.spi.TableSpecificationSource;
import org.hibernate.boot.model.source.spi.VersionAttributeSource;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.InFlightMetadataCollector.EntityTableXref;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.NaturalIdUniqueKeyBinder;
import org.hibernate.cfg.FkSecondPass;
import org.hibernate.cfg.SecondPass;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.loader.PropertyPath;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.AttributeContainer;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.Bag;
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
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.SyntheticProperty;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.mapping.Value;
import org.hibernate.tuple.GeneratedValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.ForeignKeyDirection;

/**
 * Responsible for coordinating the binding of all information inside entity tags ({@code <class/>}, etc).
 *
 * @author Steve Ebersole
 */
public class ModelBinder {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( ModelBinder.class );
	private static final boolean debugEnabled = log.isDebugEnabled();

	private final MetadataBuildingContext metadataBuildingContext;

	private final Database database;
	private final ObjectNameNormalizer objectNameNormalizer;
	private final ImplicitNamingStrategy implicitNamingStrategy;
	private final RelationalObjectBinder relationalObjectBinder;

	public static ModelBinder prepare(MetadataBuildingContext context) {
		return new ModelBinder( context );
	}

	public ModelBinder(final MetadataBuildingContext context) {
		this.metadataBuildingContext = context;

		this.database = context.getMetadataCollector().getDatabase();
		this.objectNameNormalizer = new ObjectNameNormalizer() {
			@Override
			protected MetadataBuildingContext getBuildingContext() {
				return context;
			}
		};
		this.implicitNamingStrategy = context.getBuildingOptions().getImplicitNamingStrategy();
		this.relationalObjectBinder = new RelationalObjectBinder( context );
	}

	public void finishUp(MetadataBuildingContext context) {
	}

	public void bindEntityHierarchy(EntityHierarchySourceImpl hierarchySource) {
		final RootClass rootEntityDescriptor = new RootClass( metadataBuildingContext );
		bindRootEntity( hierarchySource, rootEntityDescriptor );
		hierarchySource.getRoot()
				.getLocalMetadataBuildingContext()
				.getMetadataCollector()
				.addEntityBinding( rootEntityDescriptor );

		switch ( hierarchySource.getHierarchyInheritanceType() ) {
			case NO_INHERITANCE: {
				// nothing to do
				break;
			}
			case DISCRIMINATED: {
				bindDiscriminatorSubclassEntities( hierarchySource.getRoot(), rootEntityDescriptor );
				break;
			}
			case JOINED: {
				bindJoinedSubclassEntities( hierarchySource.getRoot(), rootEntityDescriptor );
				break;
			}
			case UNION: {
				bindUnionSubclassEntities( hierarchySource.getRoot(), rootEntityDescriptor );
				break;
			}
		}
	}

	private void bindRootEntity(EntityHierarchySourceImpl hierarchySource, RootClass rootEntityDescriptor) {
		final MappingDocument mappingDocument = hierarchySource.getRoot().sourceMappingDocument();

		bindBasicEntityValues(
				mappingDocument,
				hierarchySource.getRoot(),
				rootEntityDescriptor
		);

		final Table primaryTable = bindEntityTableSpecification(
				mappingDocument,
				hierarchySource.getRoot().getPrimaryTable(),
				null,
				hierarchySource.getRoot(),
				rootEntityDescriptor
		);

		rootEntityDescriptor.setTable( primaryTable );
		if ( log.isDebugEnabled() ) {
			log.debugf( "Mapping class: %s -> %s", rootEntityDescriptor.getEntityName(), primaryTable.getName() );
		}

		rootEntityDescriptor.setOptimisticLockStyle( hierarchySource.getOptimisticLockStyle() );
		rootEntityDescriptor.setMutable( hierarchySource.isMutable() );
		rootEntityDescriptor.setWhere( hierarchySource.getWhere() );
		rootEntityDescriptor.setExplicitPolymorphism( hierarchySource.isExplicitPolymorphism() );

		bindEntityIdentifier(
				mappingDocument,
				hierarchySource,
				rootEntityDescriptor
		);

		if ( hierarchySource.getVersionAttributeSource() != null ) {
			bindEntityVersion(
					mappingDocument,
					hierarchySource,
					rootEntityDescriptor
			);
		}

		if ( hierarchySource.getDiscriminatorSource() != null ) {
			bindEntityDiscriminator(
					mappingDocument,
					hierarchySource,
					rootEntityDescriptor
			);
		}

		applyCaching( mappingDocument, hierarchySource.getCaching(), rootEntityDescriptor );

		// Primary key constraint
		rootEntityDescriptor.createPrimaryKey();

		bindAllEntityAttributes(
				mappingDocument,
				hierarchySource.getRoot(),
				rootEntityDescriptor
		);

		if ( hierarchySource.getNaturalIdCaching() != null ) {
			if ( hierarchySource.getNaturalIdCaching().getRequested() == TruthValue.TRUE ) {
				rootEntityDescriptor.setNaturalIdCacheRegionName( hierarchySource.getNaturalIdCaching().getRegion() );
			}
		}
	}

	private void applyCaching(MappingDocument mappingDocument, Caching caching, RootClass rootEntityDescriptor) {
		if ( caching == null || caching.getRequested() == TruthValue.UNKNOWN ) {
			// see if JPA's SharedCacheMode indicates we should implicitly apply caching
			//
			// here we only really look for ALL.  Ideally we could look at NONE too as a means
			// to selectively disable all caching, but historically hbm.xml mappings were processed
			// outside this concept and whether to cache or not was defined wholly by what
			// is defined in the mapping document.  So for backwards compatibility we
			// do not consider ENABLE_SELECTIVE nor DISABLE_SELECTIVE here.
			//
			// Granted, ALL was not historically considered either, but I have a practical
			// reason for wanting to support this... our legacy tests built using
			// Configuration applied a similar logic but that capability is no longer
			// accessible from Configuration
			switch ( mappingDocument.getBuildingOptions().getSharedCacheMode() ) {
				case ALL: {
					caching = new Caching(
							null,
							mappingDocument.getBuildingOptions().getImplicitCacheAccessType(),
							false,
							TruthValue.UNKNOWN
					);
					break;
				}
				case NONE: {
					// Ideally we'd disable all caching...
					break;
				}
				case ENABLE_SELECTIVE: {
					// this is default behavior for hbm.xml
					break;
				}
				case DISABLE_SELECTIVE: {
					// really makes no sense for hbm.xml
					break;
				}
				default: {
					// null or UNSPECIFIED, nothing to do.  IMO for hbm.xml this is equivalent
					// to ENABLE_SELECTIVE
					break;
				}
			}
		}

		if ( caching == null || caching.getRequested() == TruthValue.FALSE ) {
			return;
		}

		if ( caching.getAccessType() != null ) {
			rootEntityDescriptor.setCacheConcurrencyStrategy( caching.getAccessType().getExternalName() );
		}
		else {
			rootEntityDescriptor.setCacheConcurrencyStrategy( mappingDocument.getBuildingOptions().getImplicitCacheAccessType().getExternalName() );
		}
		rootEntityDescriptor.setCacheRegionName( caching.getRegion() );
		rootEntityDescriptor.setLazyPropertiesCacheable( caching.isCacheLazyProperties() );
		rootEntityDescriptor.setCachingExplicitlyRequested( caching.getRequested() != TruthValue.UNKNOWN );
	}

	private void bindEntityIdentifier(
			MappingDocument mappingDocument,
			EntityHierarchySourceImpl hierarchySource,
			RootClass rootEntityDescriptor) {
		switch ( hierarchySource.getIdentifierSource().getNature() ) {
			case SIMPLE: {
				bindSimpleEntityIdentifier(
						mappingDocument,
						hierarchySource,
						rootEntityDescriptor
				);
				break;
			}
			case AGGREGATED_COMPOSITE: {
				bindAggregatedCompositeEntityIdentifier(
						mappingDocument,
						hierarchySource,
						rootEntityDescriptor
				);
				break;
			}
			case NON_AGGREGATED_COMPOSITE: {
				bindNonAggregatedCompositeEntityIdentifier(
						mappingDocument,
						hierarchySource,
						rootEntityDescriptor
				);
				break;
			}
			default: {
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
	}

	private void bindBasicEntityValues(
			MappingDocument sourceDocument,
			AbstractEntitySourceImpl entitySource,
			PersistentClass entityDescriptor) {
		entityDescriptor.setEntityName( entitySource.getEntityNamingSource().getEntityName() );
		entityDescriptor.setJpaEntityName( entitySource.getEntityNamingSource().getJpaEntityName() );
		entityDescriptor.setClassName( entitySource.getEntityNamingSource().getClassName() );

		entityDescriptor.setDiscriminatorValue(
				entitySource.getDiscriminatorMatchValue() != null
						? entitySource.getDiscriminatorMatchValue()
						: entityDescriptor.getEntityName()
		);

		// NOTE : entitySource#isLazy already accounts for MappingDefaults#areEntitiesImplicitlyLazy
		if ( StringHelper.isNotEmpty( entitySource.getProxy() ) ) {
			final String qualifiedProxyName = sourceDocument.qualifyClassName( entitySource.getProxy() );
			entityDescriptor.setProxyInterfaceName( qualifiedProxyName );
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

		entityDescriptor.setAbstract( entitySource.isAbstract() );

		sourceDocument.getMetadataCollector().addImport(
				entitySource.getEntityNamingSource().getEntityName(),
				entitySource.getEntityNamingSource().getEntityName()
		);

		if ( sourceDocument.getMappingDefaults().isAutoImportEnabled() && entitySource.getEntityNamingSource().getEntityName().indexOf( '.' ) > 0 ) {
			sourceDocument.getMetadataCollector().addImport(
					StringHelper.unqualify( entitySource.getEntityNamingSource().getEntityName() ),
					entitySource.getEntityNamingSource().getEntityName()
			);
		}

		if ( entitySource.getTuplizerClassMap() != null ) {
			if ( entitySource.getTuplizerClassMap().size() > 1 ) {
				DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfMultipleEntityModeSupport();
			}
			for ( Map.Entry<EntityMode,String> tuplizerEntry : entitySource.getTuplizerClassMap().entrySet() ) {
				entityDescriptor.addTuplizer(
						tuplizerEntry.getKey(),
						tuplizerEntry.getValue()
				);
			}
		}

		if ( StringHelper.isNotEmpty( entitySource.getXmlNodeName() ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfDomEntityModeSupport();
		}

		entityDescriptor.setDynamicInsert( entitySource.isDynamicInsert() );
		entityDescriptor.setDynamicUpdate( entitySource.isDynamicUpdate() );
		entityDescriptor.setBatchSize( entitySource.getBatchSize() );
		entityDescriptor.setSelectBeforeUpdate( entitySource.isSelectBeforeUpdate() );

		if ( StringHelper.isNotEmpty( entitySource.getCustomPersisterClassName() ) ) {
			try {
				entityDescriptor.setEntityPersisterClass(
						sourceDocument.getClassLoaderAccess().classForName( entitySource.getCustomPersisterClassName() )
				);
			}
			catch (ClassLoadingException e) {
				throw new MappingException(
						String.format(
								Locale.ENGLISH,
								"Unable to load specified persister class : %s",
								entitySource.getCustomPersisterClassName()
						),
						e,
						sourceDocument.getOrigin()
				);
			}
		}

		bindCustomSql( sourceDocument, entitySource, entityDescriptor );

		final JdbcEnvironment jdbcEnvironment = sourceDocument.getMetadataCollector().getDatabase().getJdbcEnvironment();

		for ( String tableName : entitySource.getSynchronizedTableNames() ) {
			final Identifier physicalTableName = sourceDocument.getBuildingOptions().getPhysicalNamingStrategy().toPhysicalTableName(
					jdbcEnvironment.getIdentifierHelper().toIdentifier( tableName ),
					jdbcEnvironment
			);
			entityDescriptor.addSynchronizedTable( physicalTableName.render( jdbcEnvironment.getDialect() ) );
		}

		for ( FilterSource filterSource : entitySource.getFilterSources() ) {
			String condition = filterSource.getCondition();
			if ( condition == null ) {
				final FilterDefinition filterDefinition = sourceDocument.getMetadataCollector().getFilterDefinition( filterSource.getName() );
				if ( filterDefinition != null ) {
					condition = filterDefinition.getDefaultFilterCondition();
				}
			}

			entityDescriptor.addFilter(
					filterSource.getName(),
					condition,
					filterSource.shouldAutoInjectAliases(),
					filterSource.getAliasToTableMap(),
					filterSource.getAliasToEntityMap()
			);
		}

		for ( JaxbHbmNamedQueryType namedQuery : entitySource.getNamedQueries() ) {
			NamedQueryBinder.processNamedQuery(
					sourceDocument,
					namedQuery,
					entitySource.getEntityNamingSource().getEntityName() + "."
			);
		}
		for ( JaxbHbmNamedNativeQueryType namedQuery : entitySource.getNamedNativeQueries() ) {
			NamedQueryBinder.processNamedNativeQuery(
					sourceDocument,
					namedQuery,
					entitySource.getEntityNamingSource().getEntityName() + "."
			);
		}

		entityDescriptor.setMetaAttributes( entitySource.getToolingHintContext().getMetaAttributeMap() );
	}

	private void bindDiscriminatorSubclassEntities(
			AbstractEntitySourceImpl entitySource,
			PersistentClass superEntityDescriptor) {
		for ( IdentifiableTypeSource subType : entitySource.getSubTypes() ) {
			final SingleTableSubclass subEntityDescriptor = new SingleTableSubclass( superEntityDescriptor, metadataBuildingContext );
			bindDiscriminatorSubclassEntity( (SubclassEntitySourceImpl) subType, subEntityDescriptor );
			superEntityDescriptor.addSubclass( subEntityDescriptor );
			entitySource.getLocalMetadataBuildingContext().getMetadataCollector().addEntityBinding( subEntityDescriptor );
		}
	}

	private void bindDiscriminatorSubclassEntity(
			SubclassEntitySourceImpl entitySource,
			SingleTableSubclass entityDescriptor) {

		bindBasicEntityValues(
				entitySource.sourceMappingDocument(),
				entitySource,
				entityDescriptor
		);

		final String superEntityName = ( (EntitySource) entitySource.getSuperType() ).getEntityNamingSource()
				.getEntityName();
		final EntityTableXref superEntityTableXref = entitySource.getLocalMetadataBuildingContext()
				.getMetadataCollector()
				.getEntityTableXref( superEntityName );
		if ( superEntityTableXref == null ) {
			throw new MappingException(
					String.format(
							Locale.ENGLISH,
							"Unable to locate entity table xref for entity [%s] super-type [%s]",
							entityDescriptor.getEntityName(),
							superEntityName
					),
					entitySource.origin()
			);
		}

		entitySource.getLocalMetadataBuildingContext().getMetadataCollector().addEntityTableXref(
				entitySource.getEntityNamingSource().getEntityName(),
				database.toIdentifier(
						entitySource.getLocalMetadataBuildingContext().getMetadataCollector().getLogicalTableName(
								entityDescriptor.getTable()
						)
				),
				entityDescriptor.getTable(),
				superEntityTableXref
		);

		bindAllEntityAttributes(
				entitySource.sourceMappingDocument(),
				entitySource,
				entityDescriptor
		);

		bindDiscriminatorSubclassEntities( entitySource, entityDescriptor );
	}

	private void bindJoinedSubclassEntities(
			AbstractEntitySourceImpl entitySource,
			PersistentClass superEntityDescriptor) {
		for ( IdentifiableTypeSource subType : entitySource.getSubTypes() ) {
			final JoinedSubclass subEntityDescriptor = new JoinedSubclass( superEntityDescriptor, metadataBuildingContext );
			bindJoinedSubclassEntity( (JoinedSubclassEntitySourceImpl) subType, subEntityDescriptor );
			superEntityDescriptor.addSubclass( subEntityDescriptor );
			entitySource.getLocalMetadataBuildingContext().getMetadataCollector().addEntityBinding( subEntityDescriptor );
		}
	}

	private void bindJoinedSubclassEntity(
			JoinedSubclassEntitySourceImpl entitySource,
			JoinedSubclass entityDescriptor) {
		MappingDocument mappingDocument = entitySource.sourceMappingDocument();

		bindBasicEntityValues(
				mappingDocument,
				entitySource,
				entityDescriptor
		);

		final Table primaryTable = bindEntityTableSpecification(
				mappingDocument,
				entitySource.getPrimaryTable(),
				null,
				entitySource,
				entityDescriptor
		);

		entityDescriptor.setTable( primaryTable );
		if ( log.isDebugEnabled() ) {
			log.debugf( "Mapping joined-subclass: %s -> %s", entityDescriptor.getEntityName(), primaryTable.getName() );
		}

		// KEY
		final SimpleValue keyBinding = new DependantValue(
				mappingDocument.getMetadataCollector(),
				primaryTable,
				entityDescriptor.getIdentifier()
		);
		if ( mappingDocument.getBuildingOptions().useNationalizedCharacterData() ) {
			keyBinding.makeNationalized();
		}
		entityDescriptor.setKey( keyBinding );
		keyBinding.setCascadeDeleteEnabled( entitySource.isCascadeDeleteEnabled() );
		relationalObjectBinder.bindColumns(
				mappingDocument,
				entitySource.getPrimaryKeyColumnSources(),
				keyBinding,
				false,
				new RelationalObjectBinder.ColumnNamingDelegate() {
					int count = 0;
					@Override
					public Identifier determineImplicitName(LocalMetadataBuildingContext context) {
						final Column column = primaryTable.getPrimaryKey().getColumn( count++ );
						return database.toIdentifier( column.getQuotedName() );
					}
				}
		);
		keyBinding.setForeignKeyName( entitySource.getExplicitForeignKeyName() );
		// model.getKey().setType( new Type( model.getIdentifier() ) );
		entityDescriptor.createPrimaryKey();
		entityDescriptor.createForeignKey();

		// todo : tooling hints

		bindAllEntityAttributes(
				entitySource.sourceMappingDocument(),
				entitySource,
				entityDescriptor
		);

		bindJoinedSubclassEntities( entitySource, entityDescriptor );
	}

	private void bindUnionSubclassEntities(
			EntitySource entitySource,
			PersistentClass superEntityDescriptor) {
		for ( IdentifiableTypeSource subType : entitySource.getSubTypes() ) {
			final UnionSubclass subEntityDescriptor = new UnionSubclass( superEntityDescriptor, metadataBuildingContext );
			bindUnionSubclassEntity( (SubclassEntitySourceImpl) subType, subEntityDescriptor );
			superEntityDescriptor.addSubclass( subEntityDescriptor );
			entitySource.getLocalMetadataBuildingContext().getMetadataCollector().addEntityBinding( subEntityDescriptor );
		}
	}

	private void bindUnionSubclassEntity(
			SubclassEntitySourceImpl entitySource,
			UnionSubclass entityDescriptor) {
		MappingDocument mappingDocument = entitySource.sourceMappingDocument();

		bindBasicEntityValues(
				mappingDocument,
				entitySource,
				entityDescriptor
		);

		final Table primaryTable = bindEntityTableSpecification(
				mappingDocument,
				entitySource.getPrimaryTable(),
				entityDescriptor.getSuperclass().getTable(),
				entitySource,
				entityDescriptor
		);
		entityDescriptor.setTable( primaryTable );

		if ( log.isDebugEnabled() ) {
			log.debugf( "Mapping union-subclass: %s -> %s", entityDescriptor.getEntityName(), primaryTable.getName() );
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
			final EntityHierarchySourceImpl hierarchySource,
			RootClass rootEntityDescriptor) {
		final IdentifierSourceSimple idSource = (IdentifierSourceSimple) hierarchySource.getIdentifierSource();

		final SimpleValue idValue = new SimpleValue(
				sourceDocument.getMetadataCollector(),
				rootEntityDescriptor.getTable()
		);
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

		relationalObjectBinder.bindColumnsAndFormulas(
				sourceDocument,
				( (RelationalValueSourceContainer) idSource.getIdentifierAttributeSource() ).getRelationalValueSources(),
				idValue,
				false,
				new RelationalObjectBinder.ColumnNamingDelegate() {
					@Override
					public Identifier determineImplicitName(final LocalMetadataBuildingContext context) {
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
				}
		);

		if ( propertyName != null ) {
			Property prop = new Property();
			prop.setValue( idValue );
			bindProperty(
					sourceDocument,
					idSource.getIdentifierAttributeSource(),
					prop
			);
			rootEntityDescriptor.setIdentifierProperty( prop );
			rootEntityDescriptor.setDeclaredIdentifierProperty( prop );
		}

		makeIdentifier(
				sourceDocument,
				idSource.getIdentifierGeneratorDescriptor(),
				idSource.getUnsavedValue(),
				idValue
		);
	}

	private void makeIdentifier(
			final MappingDocument sourceDocument,
			IdentifierGeneratorDefinition generator,
			String unsavedValue,
			SimpleValue identifierValue) {
		if ( generator != null ) {
			String generatorName = generator.getStrategy();
			Properties params = new Properties();

			// see if the specified generator name matches a registered <identifier-generator/>
			IdentifierGeneratorDefinition generatorDef = sourceDocument.getMetadataCollector().getIdentifierGenerator( generatorName );
			if ( generatorDef != null ) {
				generatorName = generatorDef.getStrategy();
				params.putAll( generatorDef.getParameters() );
			}

			identifierValue.setIdentifierGeneratorStrategy( generatorName );

			// YUCK!  but cannot think of a clean way to do this given the string-config based scheme
			params.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER, objectNameNormalizer);

			if ( database.getDefaultNamespace().getPhysicalName().getSchema() != null ) {
				params.setProperty(
						PersistentIdentifierGenerator.SCHEMA,
						database.getDefaultNamespace().getPhysicalName().getSchema().render( database.getDialect() )
				);
			}
			if ( database.getDefaultNamespace().getPhysicalName().getCatalog() != null ) {
				params.setProperty(
						PersistentIdentifierGenerator.CATALOG,
						database.getDefaultNamespace().getPhysicalName().getCatalog().render( database.getDialect() )
				);
			}

			params.putAll( generator.getParameters() );

			identifierValue.setIdentifierGeneratorProperties( params );
		}

		identifierValue.getTable().setIdentifierValue( identifierValue );

		if ( StringHelper.isNotEmpty( unsavedValue ) ) {
			identifierValue.setNullValue( unsavedValue );
		}
		else {
			if ( "assigned".equals( identifierValue.getIdentifierGeneratorStrategy() ) ) {
				identifierValue.setNullValue( "undefined" );
			}
			else {
				identifierValue.setNullValue( null );
			}
		}
	}

	private void bindAggregatedCompositeEntityIdentifier(
			MappingDocument mappingDocument,
			EntityHierarchySourceImpl hierarchySource,
			RootClass rootEntityDescriptor) {

		// an aggregated composite-id is a composite-id that defines a singular
		// (composite) attribute as part of the entity to represent the id.

		final IdentifierSourceAggregatedComposite identifierSource
				= (IdentifierSourceAggregatedComposite) hierarchySource.getIdentifierSource();

		final Component cid = new Component( mappingDocument.getMetadataCollector(), rootEntityDescriptor );
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
				idClassName == null && idPropertyName == null,
				identifierSource.getEmbeddableSource().isDynamic(),
				identifierSource.getIdentifierAttributeSource().getXmlNodeName()
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
		if ( identifierSource.getEmbeddableSource().getTypeDescriptor() == null ) {
			return null;
		}

		return identifierSource.getEmbeddableSource().getTypeDescriptor().getName();
	}

	private static final String ID_MAPPER_PATH_PART = '<' + PropertyPath.IDENTIFIER_MAPPER_PROPERTY + '>';

	private void bindNonAggregatedCompositeEntityIdentifier(
			MappingDocument mappingDocument,
			EntityHierarchySourceImpl hierarchySource,
			RootClass rootEntityDescriptor) {
		final IdentifierSourceNonAggregatedComposite identifierSource
				= (IdentifierSourceNonAggregatedComposite) hierarchySource.getIdentifierSource();

		final Component cid = new Component( mappingDocument.getMetadataCollector(), rootEntityDescriptor );
		cid.setKey( true );
		rootEntityDescriptor.setIdentifier( cid );

		final String idClassName = extractIdClassName( identifierSource );

		bindComponent(
				mappingDocument,
				hierarchySource.getRoot().getAttributeRoleBase().append( "<id>" ).getFullPath(),
				identifierSource.getEmbeddableSource(),
				cid,
				idClassName,
				rootEntityDescriptor.getClassName(),
				null,
				idClassName == null,
				false,
				null
		);

		if ( idClassName != null ) {
			// we also need to bind the "id mapper".  ugh, terrible name.  Basically we need to
			// create a virtual (embedded) composite for the non-aggregated attributes on the entity
			// itself.
			final Component mapper = new Component( mappingDocument.getMetadataCollector(), rootEntityDescriptor );
			bindComponent(
					mappingDocument,
					hierarchySource.getRoot().getAttributeRoleBase().append( ID_MAPPER_PATH_PART ).getFullPath(),
					identifierSource.getEmbeddableSource(),
					mapper,
					rootEntityDescriptor.getClassName(),
					null,
					null,
					true,
					false,
					null
			);

			rootEntityDescriptor.setIdentifierMapper(mapper);
			Property property = new Property();
			property.setName( PropertyPath.IDENTIFIER_MAPPER_PROPERTY );
			property.setUpdateable( false );
			property.setInsertable( false );
			property.setValue( mapper );
			property.setPropertyAccessorName( "embedded" );
			rootEntityDescriptor.addProperty( property );
		}

		finishBindingCompositeIdentifier( mappingDocument, rootEntityDescriptor, identifierSource, cid, null );
	}

	private String extractIdClassName(IdentifierSourceNonAggregatedComposite identifierSource) {
		if ( identifierSource.getIdClassSource() == null ) {
			return null;
		}

		if ( identifierSource.getIdClassSource().getTypeDescriptor() == null ) {
			return null;
		}

		return identifierSource.getIdClassSource().getTypeDescriptor().getName();
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
				// todo : what is the implication of this?
				cid.setDynamic( !rootEntityDescriptor.hasPojoRepresentation() );
				/*
				 * Property prop = new Property(); prop.setName("id");
				 * prop.setPropertyAccessorName("embedded"); prop.setValue(id);
				 * entity.setIdentifierProperty(prop);
				 */
			}
		}
		else {
			Property prop = new Property();
			prop.setValue( cid );
			bindProperty(
					sourceDocument,
					( (IdentifierSourceAggregatedComposite) identifierSource ).getIdentifierAttributeSource(),
					prop
			);
			rootEntityDescriptor.setIdentifierProperty( prop );
			rootEntityDescriptor.setDeclaredIdentifierProperty( prop );
		}

		makeIdentifier(
				sourceDocument,
				identifierSource.getIdentifierGeneratorDescriptor(),
				null,
				cid
		);
	}

	private void bindEntityVersion(
			MappingDocument sourceDocument,
			EntityHierarchySourceImpl hierarchySource,
			RootClass rootEntityDescriptor) {
		final VersionAttributeSource versionAttributeSource = hierarchySource.getVersionAttributeSource();

		final SimpleValue versionValue = new SimpleValue(
				sourceDocument.getMetadataCollector(),
				rootEntityDescriptor.getTable()
		);

		versionValue.makeVersion();

		bindSimpleValueType(
				sourceDocument,
				versionAttributeSource.getTypeInformation(),
				versionValue
		);

		relationalObjectBinder.bindColumnsAndFormulas(
				sourceDocument,
				versionAttributeSource.getRelationalValueSources(),
				versionValue,
				false,
				new RelationalObjectBinder.ColumnNamingDelegate() {
					@Override
					public Identifier determineImplicitName(LocalMetadataBuildingContext context) {
						return implicitNamingStrategy.determineBasicColumnName( versionAttributeSource );
					}
				}
		);

		Property prop = new Property();
		prop.setValue( versionValue );
		bindProperty(
				sourceDocument,
				versionAttributeSource,
				prop
		);

		// for version properties marked as being generated, make sure they are "always"
		// generated; aka, "insert" is invalid; this is dis-allowed by the DTD,
		// but just to make sure...
		if ( prop.getValueGenerationStrategy() != null ) {
			if ( prop.getValueGenerationStrategy().getGenerationTiming() == GenerationTiming.INSERT ) {
				throw new MappingException(
						"'generated' attribute cannot be 'insert' for version/timestamp property",
						sourceDocument.getOrigin()
				);
			}
		}

		if ( versionAttributeSource.getUnsavedValue() != null ) {
			versionValue.setNullValue( versionAttributeSource.getUnsavedValue() );
		}
		else {
			versionValue.setNullValue( "undefined" );
		}

		rootEntityDescriptor.setVersion( prop );
		rootEntityDescriptor.addProperty( prop );
	}

	private void bindEntityDiscriminator(
			MappingDocument sourceDocument,
			final EntityHierarchySourceImpl hierarchySource,
			RootClass rootEntityDescriptor) {
		final SimpleValue discriminatorValue = new SimpleValue(
				sourceDocument.getMetadataCollector(),
				rootEntityDescriptor.getTable()
		);
		rootEntityDescriptor.setDiscriminator( discriminatorValue );

		String typeName = hierarchySource.getDiscriminatorSource().getExplicitHibernateTypeName();
		if ( typeName == null ) {
			typeName = "string";
		}
		bindSimpleValueType(
				sourceDocument,
				new HibernateTypeSourceImpl( typeName ),
				discriminatorValue
		);

		relationalObjectBinder.bindColumnOrFormula(
				sourceDocument,
				hierarchySource.getDiscriminatorSource().getDiscriminatorRelationalValueSource(),
				discriminatorValue,
				false,
				new RelationalObjectBinder.ColumnNamingDelegate() {
					@Override
					public Identifier determineImplicitName(final LocalMetadataBuildingContext context) {
						return implicitNamingStrategy.determineDiscriminatorColumnName(
								hierarchySource.getDiscriminatorSource()
						);
					}
				}
		);

		rootEntityDescriptor.setPolymorphic( true );
		rootEntityDescriptor.setDiscriminatorInsertable( hierarchySource.getDiscriminatorSource().isInserted() );

		// todo : currently isForced() is defined as boolean, not Boolean
		//		although it has always been that way (DTD too)
		final boolean force = hierarchySource.getDiscriminatorSource().isForced()
				|| sourceDocument.getBuildingOptions().shouldImplicitlyForceDiscriminatorInSelect();
		rootEntityDescriptor.setForceDiscriminator( force );
	}

	private void bindAllEntityAttributes(
			MappingDocument mappingDocument,
			EntitySource entitySource,
			PersistentClass entityDescriptor) {
		final EntityTableXref entityTableXref = mappingDocument.getMetadataCollector().getEntityTableXref(
				entityDescriptor.getEntityName()
		);
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
		for ( SecondaryTableSource secondaryTableSource : entitySource.getSecondaryTableMap().values() ) {
			final Join secondaryTableJoin = new Join();
			secondaryTableJoin.setPersistentClass( entityDescriptor );
			bindSecondaryTable(
					mappingDocument,
					secondaryTableSource,
					secondaryTableJoin,
					entityTableXref
			);
			entityDescriptor.addJoin( secondaryTableJoin );
		}

		for ( AttributeSource attributeSource : entitySource.attributeSources() ) {
			if ( PluralAttributeSource.class.isInstance( attributeSource ) ) {
				// plural attribute
				final Property attribute = createPluralAttribute(
						mappingDocument,
						(PluralAttributeSource) attributeSource,
						entityDescriptor
				);
				entityDescriptor.addProperty( attribute );
			}
			else {
				// singular attribute
				if ( SingularAttributeSourceBasic.class.isInstance( attributeSource ) ) {
					final SingularAttributeSourceBasic basicAttributeSource = (SingularAttributeSourceBasic) attributeSource;
					final Identifier tableName = determineTable( mappingDocument, basicAttributeSource.getName(), basicAttributeSource );
					final AttributeContainer attributeContainer;
					final Table table;
					final Join secondaryTableJoin = entityTableXref.locateJoin( tableName );
					if ( secondaryTableJoin == null ) {
						table = entityDescriptor.getTable();
						attributeContainer = entityDescriptor;
					}
					else {
						table = secondaryTableJoin.getTable();
						attributeContainer = secondaryTableJoin;
					}

					final Property attribute = createBasicAttribute(
							mappingDocument,
							basicAttributeSource,
							new SimpleValue( mappingDocument.getMetadataCollector(), table ),
							entityDescriptor.getClassName()
					);

					if ( secondaryTableJoin != null ) {
						attribute.setOptional( secondaryTableJoin.isOptional() );
					}

					attributeContainer.addProperty( attribute );

					handleNaturalIdBinding(
							mappingDocument,
							entityDescriptor,
							attribute,
							basicAttributeSource.getNaturalIdMutability()
					);
				}
				else if ( SingularAttributeSourceEmbedded.class.isInstance( attributeSource ) ) {
					final SingularAttributeSourceEmbedded embeddedAttributeSource = (SingularAttributeSourceEmbedded) attributeSource;
					final Identifier tableName = determineTable( mappingDocument, embeddedAttributeSource );
					final AttributeContainer attributeContainer;
					final Table table;
					final Join secondaryTableJoin = entityTableXref.locateJoin( tableName );
					if ( secondaryTableJoin == null ) {
						table = entityDescriptor.getTable();
						attributeContainer = entityDescriptor;
					}
					else {
						table = secondaryTableJoin.getTable();
						attributeContainer = secondaryTableJoin;
					}

					final Property attribute = createEmbeddedAttribute(
							mappingDocument,
							(SingularAttributeSourceEmbedded) attributeSource,
							new Component( mappingDocument.getMetadataCollector(), table, entityDescriptor ),
							entityDescriptor.getClassName()
					);

					if ( secondaryTableJoin != null ) {
						attribute.setOptional( secondaryTableJoin.isOptional() );
					}

					attributeContainer.addProperty( attribute );

					handleNaturalIdBinding(
							mappingDocument,
							entityDescriptor,
							attribute,
							embeddedAttributeSource.getNaturalIdMutability()
					);
				}
				else if ( SingularAttributeSourceManyToOne.class.isInstance( attributeSource ) ) {
					final SingularAttributeSourceManyToOne manyToOneAttributeSource = (SingularAttributeSourceManyToOne) attributeSource;
					final Identifier tableName = determineTable( mappingDocument, manyToOneAttributeSource.getName(), manyToOneAttributeSource );
					final AttributeContainer attributeContainer;
					final Table table;
					final Join secondaryTableJoin = entityTableXref.locateJoin( tableName );
					if ( secondaryTableJoin == null ) {
						table = entityDescriptor.getTable();
						attributeContainer = entityDescriptor;
					}
					else {
						table = secondaryTableJoin.getTable();
						attributeContainer = secondaryTableJoin;
					}

					final Property attribute = createManyToOneAttribute(
							mappingDocument,
							manyToOneAttributeSource,
							new ManyToOne( mappingDocument.getMetadataCollector(), table ),
							entityDescriptor.getClassName()
					);

					if ( secondaryTableJoin != null ) {
						attribute.setOptional( secondaryTableJoin.isOptional() );
					}

					attributeContainer.addProperty( attribute );

					handleNaturalIdBinding(
							mappingDocument,
							entityDescriptor,
							attribute,
							manyToOneAttributeSource.getNaturalIdMutability()
					);
				}
				else if ( SingularAttributeSourceOneToOne.class.isInstance( attributeSource ) ) {
					final SingularAttributeSourceOneToOne oneToOneAttributeSource = (SingularAttributeSourceOneToOne) attributeSource;
					final Table table = entityDescriptor.getTable();
					final Property attribute = createOneToOneAttribute(
							mappingDocument,
							oneToOneAttributeSource,
							new OneToOne( mappingDocument.getMetadataCollector(), table, entityDescriptor ),
							entityDescriptor.getClassName()
					);
					entityDescriptor.addProperty( attribute );

					handleNaturalIdBinding(
							mappingDocument,
							entityDescriptor,
							attribute,
							oneToOneAttributeSource.getNaturalIdMutability()
					);
				}
				else if ( SingularAttributeSourceAny.class.isInstance( attributeSource ) ) {
					final SingularAttributeSourceAny anyAttributeSource = (SingularAttributeSourceAny) attributeSource;
					final Identifier tableName = determineTable(
							mappingDocument,
							anyAttributeSource.getName(),
							anyAttributeSource.getKeySource().getRelationalValueSources()
					);
					final AttributeContainer attributeContainer;
					final Table table;
					final Join secondaryTableJoin = entityTableXref.locateJoin( tableName );
					if ( secondaryTableJoin == null ) {
						table = entityDescriptor.getTable();
						attributeContainer = entityDescriptor;
					}
					else {
						table = secondaryTableJoin.getTable();
						attributeContainer = secondaryTableJoin;
					}

					final Property attribute = createAnyAssociationAttribute(
							mappingDocument,
							anyAttributeSource,
							new Any( mappingDocument.getMetadataCollector(), table ),
							entityDescriptor.getEntityName()
					);

					if ( secondaryTableJoin != null ) {
						attribute.setOptional( secondaryTableJoin.isOptional() );
					}

					attributeContainer.addProperty( attribute );

					handleNaturalIdBinding(
							mappingDocument,
							entityDescriptor,
							attribute,
							anyAttributeSource.getNaturalIdMutability()
					);
				}
			}
		}
	}

	private void handleNaturalIdBinding(
			MappingDocument mappingDocument,
			PersistentClass entityBinding,
			Property attributeBinding,
			NaturalIdMutability naturalIdMutability) {
		if ( naturalIdMutability == NaturalIdMutability.NOT_NATURAL_ID ) {
			return;
		}

		attributeBinding.setNaturalIdentifier( true );

		if ( naturalIdMutability == NaturalIdMutability.IMMUTABLE ) {
			attributeBinding.setUpdateable( false );
		}

		NaturalIdUniqueKeyBinder ukBinder = mappingDocument.getMetadataCollector().locateNaturalIdUniqueKeyBinder(
				entityBinding.getEntityName()
		);

		if ( ukBinder == null ) {
			ukBinder = new NaturalIdUniqueKeyBinderImpl( mappingDocument, entityBinding );
			mappingDocument.getMetadataCollector().registerNaturalIdUniqueKeyBinder(
					entityBinding.getEntityName(),
					ukBinder
			);
		}

		ukBinder.addAttributeBinding( attributeBinding );
	}

	private Property createPluralAttribute(
			MappingDocument sourceDocument,
			PluralAttributeSource attributeSource,
			PersistentClass entityDescriptor) {
		final Collection collectionBinding;

		if ( attributeSource instanceof PluralAttributeSourceListImpl ) {
			collectionBinding = new org.hibernate.mapping.List( sourceDocument.getMetadataCollector(), entityDescriptor );
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );

			registerSecondPass(
					new PluralAttributeListSecondPass(
							sourceDocument,
							(IndexedPluralAttributeSource) attributeSource,
							(org.hibernate.mapping.List) collectionBinding
					),
					sourceDocument
			);
		}
		else if ( attributeSource instanceof PluralAttributeSourceSetImpl ) {
			collectionBinding = new Set( sourceDocument.getMetadataCollector(), entityDescriptor );
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );

			registerSecondPass(
					new PluralAttributeSetSecondPass( sourceDocument, attributeSource, collectionBinding ),
					sourceDocument
			);
		}
		else if ( attributeSource instanceof PluralAttributeSourceMapImpl ) {
			collectionBinding = new org.hibernate.mapping.Map( sourceDocument.getMetadataCollector(), entityDescriptor );
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );

			registerSecondPass(
					new PluralAttributeMapSecondPass(
							sourceDocument,
							(IndexedPluralAttributeSource) attributeSource,
							(org.hibernate.mapping.Map) collectionBinding
					),
					sourceDocument
			);
		}
		else if ( attributeSource instanceof PluralAttributeSourceBagImpl ) {
			collectionBinding = new Bag( sourceDocument.getMetadataCollector(), entityDescriptor );
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );

			registerSecondPass(
					new PluralAttributeBagSecondPass( sourceDocument, attributeSource, collectionBinding ),
					sourceDocument
			);
		}
		else if ( attributeSource instanceof PluralAttributeSourceIdBagImpl ) {
			collectionBinding = new IdentifierBag( sourceDocument.getMetadataCollector(), entityDescriptor );
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );

			registerSecondPass(
					new PluralAttributeIdBagSecondPass( sourceDocument, attributeSource, collectionBinding ),
					sourceDocument
			);
		}
		else if ( attributeSource instanceof PluralAttributeSourceArrayImpl ) {
			final PluralAttributeSourceArray arraySource = (PluralAttributeSourceArray) attributeSource;
			collectionBinding = new Array( sourceDocument.getMetadataCollector(), entityDescriptor );
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );

			( (Array) collectionBinding ).setElementClassName(
					sourceDocument.qualifyClassName( arraySource.getElementClass() )
			);

			registerSecondPass(
					new PluralAttributeArraySecondPass(
							sourceDocument,
							arraySource,
							(Array) collectionBinding
					),
					sourceDocument
			);
		}
		else if ( attributeSource instanceof PluralAttributeSourcePrimitiveArrayImpl ) {
			collectionBinding = new PrimitiveArray( sourceDocument.getMetadataCollector(), entityDescriptor );
			bindCollectionMetadata( sourceDocument, attributeSource, collectionBinding );

			registerSecondPass(
					new PluralAttributePrimitiveArraySecondPass(
							sourceDocument,
							(IndexedPluralAttributeSource) attributeSource,
							(PrimitiveArray) collectionBinding
					),
					sourceDocument
			);
		}
		else {
			throw new AssertionFailure(
					"Unexpected PluralAttributeSource type : " + attributeSource.getClass().getName()
			);
		}

		sourceDocument.getMetadataCollector().addCollectionBinding( collectionBinding );

		final Property attribute = new Property();
		attribute.setValue( collectionBinding );
		bindProperty(
				sourceDocument,
				attributeSource,
				attribute
		);

		return attribute;
	}

	private void bindCollectionMetadata(MappingDocument mappingDocument, PluralAttributeSource source, Collection binding) {
		binding.setRole( source.getAttributeRole().getFullPath() );
		binding.setInverse( source.isInverse() );
		binding.setMutable( source.isMutable() );
		binding.setOptimisticLocked( source.isIncludedInOptimisticLocking() );

		if ( source.getCustomPersisterClassName() != null ) {
			binding.setCollectionPersisterClass(
					mappingDocument.getClassLoaderAccess().classForName(
							mappingDocument.qualifyClassName( source.getCustomPersisterClassName() )
					)
			);
		}

		applyCaching( mappingDocument, source.getCaching(), binding );

		// bind the collection type info
		String typeName = source.getTypeInformation().getName();
		Map typeParameters = new HashMap();
		if ( typeName != null ) {
			// see if there is a corresponding type-def
			final TypeDefinition typeDef = mappingDocument.getMetadataCollector().getTypeDefinition( typeName );
			if ( typeDef != null ) {
				typeName = typeDef.getTypeImplementorClass().getName();
				if ( typeDef.getParameters() != null ) {
					typeParameters.putAll( typeDef.getParameters() );
				}
			}
			else {
				// it could be a unqualified class name, in which case we should qualify
				// it with the implicit package name for this context, if one.
				typeName = mappingDocument.qualifyClassName( typeName );
			}
		}
		if ( source.getTypeInformation().getParameters() != null ) {
			typeParameters.putAll( source.getTypeInformation().getParameters() );
		}

		binding.setTypeName( typeName );
		binding.setTypeParameters( typeParameters );

		if ( source.getFetchCharacteristics().getFetchTiming() == FetchTiming.DELAYED ) {
			binding.setLazy( true );
			binding.setExtraLazy( source.getFetchCharacteristics().isExtraLazy() );
		}
		else {
			binding.setLazy( false );
		}

		switch ( source.getFetchCharacteristics().getFetchStyle() ) {
			case SELECT: {
				binding.setFetchMode( FetchMode.SELECT );
				break;
			}
			case JOIN: {
				binding.setFetchMode( FetchMode.JOIN );
				break;
			}
			case BATCH: {
				binding.setFetchMode( FetchMode.SELECT );
				binding.setBatchSize( source.getFetchCharacteristics().getBatchSize() );
				break;
			}
			case SUBSELECT: {
				binding.setFetchMode( FetchMode.SELECT );
				binding.setSubselectLoadable( true );
				// todo : this could totally be done using a "symbol map" approach
				binding.getOwner().setSubselectLoadableCollections( true );
				break;
			}
			default: {
				throw new AssertionFailure( "Unexpected FetchStyle : " + source.getFetchCharacteristics().getFetchStyle().name() );
			}
		}

		for ( String name : source.getSynchronizedTableNames() ) {
			binding.getSynchronizedTables().add( name );
		}

		binding.setWhere( source.getWhere() );
		binding.setLoaderName( source.getCustomLoaderName() );
		if ( source.getCustomSqlInsert() != null ) {
			binding.setCustomSQLInsert(
					source.getCustomSqlInsert().getSql(),
					source.getCustomSqlInsert().isCallable(),
					source.getCustomSqlInsert().getCheckStyle()
			);
		}
		if ( source.getCustomSqlUpdate() != null ) {
			binding.setCustomSQLUpdate(
					source.getCustomSqlUpdate().getSql(),
					source.getCustomSqlUpdate().isCallable(),
					source.getCustomSqlUpdate().getCheckStyle()
			);
		}
		if ( source.getCustomSqlDelete() != null ) {
			binding.setCustomSQLDelete(
					source.getCustomSqlDelete().getSql(),
					source.getCustomSqlDelete().isCallable(),
					source.getCustomSqlDelete().getCheckStyle()
			);
		}
		if ( source.getCustomSqlDeleteAll() != null ) {
			binding.setCustomSQLDeleteAll(
					source.getCustomSqlDeleteAll().getSql(),
					source.getCustomSqlDeleteAll().isCallable(),
					source.getCustomSqlDeleteAll().getCheckStyle()
			);
		}

		if ( source instanceof Sortable ) {
			final Sortable sortable = (Sortable) source;
			if ( sortable.isSorted() ) {
				binding.setSorted( true );
				if ( ! sortable.getComparatorName().equals( "natural" ) ) {
					binding.setComparatorClassName( sortable.getComparatorName() );
				}
			}
			else {
				binding.setSorted( false );
			}
		}

		if ( source instanceof Orderable ) {
			if ( ( (Orderable) source ).isOrdered() ) {
				binding.setOrderBy( ( (Orderable) source ).getOrder() );
			}
		}

		final String cascadeStyle = source.getCascadeStyleName();
		if ( cascadeStyle != null && cascadeStyle.contains( "delete-orphan" ) ) {
			binding.setOrphanDelete( true );
		}

		for ( FilterSource filterSource : source.getFilterSources() ) {
			String condition = filterSource.getCondition();
			if ( condition == null ) {
				final FilterDefinition filterDefinition = mappingDocument.getMetadataCollector().getFilterDefinition( filterSource.getName() );
				if ( filterDefinition != null ) {
					condition = filterDefinition.getDefaultFilterCondition();
				}
			}

			binding.addFilter(
					filterSource.getName(),
					condition,
					filterSource.shouldAutoInjectAliases(),
					filterSource.getAliasToTableMap(),
					filterSource.getAliasToEntityMap()
			);
		}
	}

	private void applyCaching(MappingDocument mappingDocument, Caching caching, Collection collection) {
		if ( caching == null || caching.getRequested() == TruthValue.UNKNOWN ) {
			// see if JPA's SharedCacheMode indicates we should implicitly apply caching
			switch ( mappingDocument.getBuildingOptions().getSharedCacheMode() ) {
				case ALL: {
					caching = new Caching(
							null,
							mappingDocument.getBuildingOptions().getImplicitCacheAccessType(),
							false,
							TruthValue.UNKNOWN
					);
					break;
				}
				case NONE: {
					// Ideally we'd disable all caching...
					break;
				}
				case ENABLE_SELECTIVE: {
					// this is default behavior for hbm.xml
					break;
				}
				case DISABLE_SELECTIVE: {
					// really makes no sense for hbm.xml
					break;
				}
				default: {
					// null or UNSPECIFIED, nothing to do.  IMO for hbm.xml this is equivalent
					// to ENABLE_SELECTIVE
					break;
				}
			}
		}

		if ( caching == null || caching.getRequested() == TruthValue.FALSE ) {
			return;
		}

		if ( caching.getAccessType() != null ) {
			collection.setCacheConcurrencyStrategy( caching.getAccessType().getExternalName() );
		}
		else {
			collection.setCacheConcurrencyStrategy( mappingDocument.getBuildingOptions().getImplicitCacheAccessType().getExternalName() );
		}
		collection.setCacheRegionName( caching.getRegion() );
//		collection.setCachingExplicitlyRequested( caching.getRequested() != TruthValue.UNKNOWN );
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
		for ( AttributeSource attributeSource : embeddedAttributeSource.getEmbeddableSource().attributeSources() ) {
			final Identifier determinedName;
			if ( RelationalValueSourceContainer.class.isInstance( attributeSource ) ) {
				determinedName = determineTable(
						mappingDocument,
						embeddedAttributeSource.getAttributeRole().getFullPath(),
						(RelationalValueSourceContainer) attributeSource

				);
			}
			else if ( SingularAttributeSourceEmbedded.class.isInstance( attributeSource ) ) {
				determinedName = determineTable( mappingDocument, (SingularAttributeSourceEmbedded) attributeSource );
			}
			else if ( SingularAttributeSourceAny.class.isInstance( attributeSource ) ) {
				determinedName = determineTable(
						mappingDocument,
						attributeSource.getAttributeRole().getFullPath(),
						( (SingularAttributeSourceAny) attributeSource ).getKeySource().getRelationalValueSources()
				);
			}
			else {
				continue;
			}

			if (  EqualsHelper.equals( tableName, determinedName ) ) {
				continue;
			}

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

		return tableName;
	}

	private Identifier determineTable(
			MappingDocument mappingDocument,
			String attributeName,
			List<RelationalValueSource> relationalValueSources) {
		String tableName = null;
		for ( RelationalValueSource relationalValueSource : relationalValueSources ) {
			// We need to get the containing table name for both columns and formulas,
			// particularly when a column/formula is for a property on a secondary table.
			if ( EqualsHelper.equals( tableName, relationalValueSource.getContainingTableName() ) ) {
				continue;
			}

			if ( tableName != null ) {
				throw new MappingException(
						String.format(
								Locale.ENGLISH,
								"Attribute [%s] referenced columns from multiple tables: %s, %s",
								attributeName,
								tableName,
								relationalValueSource.getContainingTableName()
						),
						mappingDocument.getOrigin()
				);
			}

			tableName = relationalValueSource.getContainingTableName();
		}

		return database.toIdentifier( tableName );
	}

	private void bindSecondaryTable(
			MappingDocument mappingDocument,
			SecondaryTableSource secondaryTableSource,
			Join secondaryTableJoin,
			final EntityTableXref entityTableXref) {
		final PersistentClass persistentClass = secondaryTableJoin.getPersistentClass();

		final Identifier catalogName = determineCatalogName( secondaryTableSource.getTableSource() );
		final Identifier schemaName = determineSchemaName( secondaryTableSource.getTableSource() );
		final Namespace namespace = database.locateNamespace( catalogName, schemaName );

		Table secondaryTable;
		final Identifier logicalTableName;

		if ( TableSource.class.isInstance( secondaryTableSource.getTableSource() ) ) {
			final TableSource tableSource = (TableSource) secondaryTableSource.getTableSource();
			logicalTableName = database.toIdentifier( tableSource.getExplicitTableName() );
			secondaryTable = namespace.locateTable( logicalTableName );
			if ( secondaryTable == null ) {
				secondaryTable = namespace.createTable( logicalTableName, false );
			}
			else {
				secondaryTable.setAbstract( false );
			}

			secondaryTable.setComment( tableSource.getComment() );
		}
		else {
			final InLineViewSource inLineViewSource = (InLineViewSource) secondaryTableSource.getTableSource();
			secondaryTable = new Table(
					namespace,
					inLineViewSource.getSelectStatement(),
					false
			);
			logicalTableName = Identifier.toIdentifier( inLineViewSource.getLogicalName() );
		}

		secondaryTableJoin.setTable( secondaryTable );
		entityTableXref.addSecondaryTable( mappingDocument, logicalTableName, secondaryTableJoin );

		bindCustomSql(
				mappingDocument,
				secondaryTableSource,
				secondaryTableJoin
		);

		secondaryTableJoin.setSequentialSelect( secondaryTableSource.getFetchStyle() == FetchStyle.SELECT );
		secondaryTableJoin.setInverse( secondaryTableSource.isInverse() );
		secondaryTableJoin.setOptional( secondaryTableSource.isOptional() );

		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Mapping entity secondary-table: %s -> %s",
					persistentClass.getEntityName(),
					secondaryTable.getName()
			);
		}

		final SimpleValue keyBinding = new DependantValue(
				mappingDocument.getMetadataCollector(),
				secondaryTable,
				persistentClass.getIdentifier()
		);
		if ( mappingDocument.getBuildingOptions().useNationalizedCharacterData() ) {
			keyBinding.makeNationalized();
		}
		secondaryTableJoin.setKey( keyBinding );

		keyBinding.setCascadeDeleteEnabled( secondaryTableSource.isCascadeDeleteEnabled() );

		// NOTE : no Type info to bind...

		relationalObjectBinder.bindColumns(
				mappingDocument,
				secondaryTableSource.getPrimaryKeyColumnSources(),
				keyBinding,
				secondaryTableSource.isOptional(),
				new RelationalObjectBinder.ColumnNamingDelegate() {
					int count = 0;
					@Override
					public Identifier determineImplicitName(LocalMetadataBuildingContext context) {
						final Column correspondingColumn = entityTableXref.getPrimaryTable().getPrimaryKey().getColumn( count++ );
						return database.toIdentifier( correspondingColumn.getQuotedName() );
					}
				}
		);

		keyBinding.setForeignKeyName( secondaryTableSource.getExplicitForeignKeyName() );

		// skip creating primary and foreign keys for a subselect.
		if ( secondaryTable.getSubselect() == null ) {
			secondaryTableJoin.createPrimaryKey();
			secondaryTableJoin.createForeignKey();
		}
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
				embeddedSource.getXmlNodeName(),
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

		final Property attribute;
		if ( embeddedSource.isVirtualAttribute() ) {
			attribute = new SyntheticProperty() {
				@Override
				public String getPropertyAccessorName() {
					return "embedded";
				}
			};
		}
		else {
			attribute = new Property();
		}
		attribute.setValue( componentBinding );
		bindProperty(
				sourceDocument,
				embeddedSource,
				attribute
		);

		if ( StringHelper.isNotEmpty( embeddedSource.getXmlNodeName() ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfDomEntityModeSupport();
		}

		return attribute;
	}

	private Property createBasicAttribute(
			MappingDocument sourceDocument,
			final SingularAttributeSourceBasic attributeSource,
			SimpleValue value,
			String containingClassName) {
		final String attributeName = attributeSource.getName();

		bindSimpleValueType(
				sourceDocument,
				attributeSource.getTypeInformation(),
				value
		);

		relationalObjectBinder.bindColumnsAndFormulas(
				sourceDocument,
				attributeSource.getRelationalValueSources(),
				value,
				attributeSource.areValuesNullableByDefault(),
				new RelationalObjectBinder.ColumnNamingDelegate() {
					@Override
					public Identifier determineImplicitName(LocalMetadataBuildingContext context) {
						return implicitNamingStrategy.determineBasicColumnName( attributeSource );
					}
				}
		);


		prepareValueTypeViaReflection(
				sourceDocument,
				value,
				containingClassName,
				attributeName,
				attributeSource.getAttributeRole()
		);

//		// this is done here 'cos we might only know the type here (ugly!)
//		// TODO: improve this a lot:
//		if ( value instanceof ToOne ) {
//			ToOne toOne = (ToOne) value;
//			String propertyRef = toOne.getReferencedEntityAttributeName();
//			if ( propertyRef != null ) {
//				mappings.addUniquePropertyReference( toOne.getReferencedEntityName(), propertyRef );
//			}
//			toOne.setCascadeDeleteEnabled( "cascade".equals( subnode.attributeValue( "on-delete" ) ) );
//		}
//		else if ( value instanceof Collection ) {
//			Collection coll = (Collection) value;
//			String propertyRef = coll.getReferencedEntityAttributeName();
//			// not necessarily a *unique* property reference
//			if ( propertyRef != null ) {
//				mappings.addPropertyReference( coll.getOwnerEntityName(), propertyRef );
//			}
//		}

		value.createForeignKey();

		Property property = new Property();
		property.setValue( value );
		bindProperty(
				sourceDocument,
				attributeSource,
				property
		);

		return property;
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

		final String propertyRef = oneToOneBinding.getReferencedPropertyName();
		if ( propertyRef != null ) {
			handlePropertyReference(
					sourceDocument,
					oneToOneBinding.getReferencedEntityName(),
					propertyRef,
					true,
					"<one-to-one name=\"" + oneToOneSource.getName() + "\"/>"
			);
		}

		oneToOneBinding.createForeignKey();

		Property prop = new Property();
		prop.setValue( oneToOneBinding );
		bindProperty(
				sourceDocument,
				oneToOneSource,
				prop
		);

		return prop;
	}

	private void handlePropertyReference(
			MappingDocument mappingDocument,
			String referencedEntityName,
			String referencedPropertyName,
			boolean isUnique,
			String sourceElementSynopsis) {
		PersistentClass entityBinding = mappingDocument.getMetadataCollector().getEntityBinding( referencedEntityName );
		if ( entityBinding == null ) {
			// entity may just not have been processed yet - set up a delayed handler
			registerDelayedPropertyReferenceHandler(
					new DelayedPropertyReferenceHandlerImpl(
							referencedEntityName,
							referencedPropertyName,
							isUnique,
							sourceElementSynopsis,
							mappingDocument.getOrigin()
					),
					mappingDocument
			);
		}
		else {
			Property propertyBinding = entityBinding.getReferencedProperty( referencedPropertyName );
			if ( propertyBinding == null ) {
				// attribute may just not have been processed yet - set up a delayed handler
				registerDelayedPropertyReferenceHandler(
						new DelayedPropertyReferenceHandlerImpl(
								referencedEntityName,
								referencedPropertyName,
								isUnique,
								sourceElementSynopsis,
								mappingDocument.getOrigin()
						),
						mappingDocument
				);
			}
			else {
				log.tracef(
						"Property [%s.%s] referenced by property-ref [%s] was available - no need for delayed handling",
						referencedEntityName,
						referencedPropertyName,
						sourceElementSynopsis
				);
				if ( isUnique ) {
					( (SimpleValue) propertyBinding.getValue() ).setAlternateUniqueKey( true );
				}
			}
		}
	}

	private void registerDelayedPropertyReferenceHandler(
			DelayedPropertyReferenceHandlerImpl handler,
			MetadataBuildingContext buildingContext) {
		log.tracef(
				"Property [%s.%s] referenced by property-ref [%s] was not yet available - creating delayed handler",
				handler.referencedEntityName,
				handler.referencedPropertyName,
				handler.sourceElementSynopsis
		);
		buildingContext.getMetadataCollector().addDelayedPropertyReferenceHandler( handler );
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
			if ( oneToOneSource.getCascadeStyleName() != null
					&& oneToOneSource.getCascadeStyleName().contains( "delete-orphan" ) ) {
				throw new MappingException(
						String.format(
								Locale.ENGLISH,
								"one-to-one attribute [%s] cannot specify orphan delete cascading as it is constrained",
								oneToOneSource.getAttributeRole().getFullPath()
						),
						sourceDocument.getOrigin()
				);
			}
			oneToOneBinding.setConstrained( true );
			oneToOneBinding.setForeignKeyType( ForeignKeyDirection.FROM_PARENT );
		}
		else {
			oneToOneBinding.setForeignKeyType( ForeignKeyDirection.TO_PARENT );
		}

		oneToOneBinding.setLazy( oneToOneSource.getFetchCharacteristics().getFetchTiming() == FetchTiming.DELAYED );
		oneToOneBinding.setFetchMode(
				oneToOneSource.getFetchCharacteristics().getFetchStyle() == FetchStyle.SELECT
						? FetchMode.SELECT
						: FetchMode.JOIN
		);
		oneToOneBinding.setUnwrapProxy( oneToOneSource.getFetchCharacteristics().isUnwrapProxies() );


		if ( StringHelper.isNotEmpty( oneToOneSource.getReferencedEntityAttributeName() ) ) {
			oneToOneBinding.setReferencedPropertyName( oneToOneSource.getReferencedEntityAttributeName() );
			oneToOneBinding.setReferenceToPrimaryKey( false );
		}
		else {
			oneToOneBinding.setReferenceToPrimaryKey( true );
		}

		// todo : probably need some reflection here if null
		oneToOneBinding.setReferencedEntityName( oneToOneSource.getReferencedEntityName() );

		if ( oneToOneSource.isEmbedXml() == Boolean.TRUE ) {
			DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfEmbedXmlSupport();
		}

		if ( StringHelper.isNotEmpty( oneToOneSource.getExplicitForeignKeyName() ) ) {
			oneToOneBinding.setForeignKeyName( oneToOneSource.getExplicitForeignKeyName() );
		}

		oneToOneBinding.setCascadeDeleteEnabled( oneToOneSource.isCascadeDeleteEnabled() );
	}

	private Property createManyToOneAttribute(
			MappingDocument sourceDocument,
			SingularAttributeSourceManyToOne manyToOneSource,
			ManyToOne manyToOneBinding,
			String containingClassName) {
		final String attributeName = manyToOneSource.getName();

		final String referencedEntityName;
		if ( manyToOneSource.getReferencedEntityName() != null ) {
			referencedEntityName = manyToOneSource.getReferencedEntityName();
		}
		else {
			Class reflectedPropertyClass = Helper.reflectedPropertyClass( sourceDocument, containingClassName, attributeName );
			if ( reflectedPropertyClass != null ) {
				referencedEntityName = reflectedPropertyClass.getName();
			}
			else {
				prepareValueTypeViaReflection(
						sourceDocument,
						manyToOneBinding,
						containingClassName,
						attributeName,
						manyToOneSource.getAttributeRole()
				);
				referencedEntityName = manyToOneBinding.getTypeName();
			}
		}

		if ( manyToOneSource.isUnique() ) {
			manyToOneBinding.markAsLogicalOneToOne();
		}

		bindManyToOneAttribute( sourceDocument, manyToOneSource, manyToOneBinding, referencedEntityName );

		final String propertyRef = manyToOneBinding.getReferencedPropertyName();

		if ( propertyRef != null ) {
			handlePropertyReference(
					sourceDocument,
					manyToOneBinding.getReferencedEntityName(),
					propertyRef,
					true,
					"<many-to-one name=\"" + manyToOneSource.getName() + "\"/>"
			);
		}

		Property prop = new Property();
		prop.setValue( manyToOneBinding );
		bindProperty(
				sourceDocument,
				manyToOneSource,
				prop
		);

		if ( StringHelper.isNotEmpty( manyToOneSource.getCascadeStyleName() ) ) {
			// todo : would be better to delay this the end of binding (second pass, etc)
			// in order to properly allow for a singular unique column for a many-to-one to
			// also trigger a "logical one-to-one".  As-is, this can occasionally lead to
			// false exceptions if the many-to-one column binding is delayed and the
			// uniqueness is indicated on the <column/> rather than on the <many-to-one/>
			//
			// Ideally, would love to see a SimpleValue#validate approach, rather than a
			// SimpleValue#isValid that is then handled at a higher level (Property, etc).
			// The reason being that the current approach misses the exact reason
			// a "validation" fails since it loses "context"
			if ( manyToOneSource.getCascadeStyleName().contains( "delete-orphan" ) ) {
				if ( !manyToOneBinding.isLogicalOneToOne() ) {
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
		}

		return prop;
	}

	private void bindManyToOneAttribute(
			final MappingDocument sourceDocument,
			final SingularAttributeSourceManyToOne manyToOneSource,
			ManyToOne manyToOneBinding,
			String referencedEntityName) {
		// NOTE : no type information to bind

		manyToOneBinding.setReferencedEntityName( referencedEntityName );
		if ( StringHelper.isNotEmpty( manyToOneSource.getReferencedEntityAttributeName() ) ) {
			manyToOneBinding.setReferencedPropertyName( manyToOneSource.getReferencedEntityAttributeName() );
			manyToOneBinding.setReferenceToPrimaryKey( false );
		}
		else {
			manyToOneBinding.setReferenceToPrimaryKey( true );
		}

		manyToOneBinding.setLazy( manyToOneSource.getFetchCharacteristics().getFetchTiming() == FetchTiming.DELAYED );
		manyToOneBinding.setUnwrapProxy( manyToOneSource.getFetchCharacteristics().isUnwrapProxies() );
		manyToOneBinding.setFetchMode(
				manyToOneSource.getFetchCharacteristics().getFetchStyle() == FetchStyle.SELECT
						? FetchMode.SELECT
						: FetchMode.JOIN
		);

		if ( manyToOneSource.isEmbedXml() == Boolean.TRUE ) {
			DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfEmbedXmlSupport();
		}

		manyToOneBinding.setIgnoreNotFound( manyToOneSource.isIgnoreNotFound() );

		if ( StringHelper.isNotEmpty( manyToOneSource.getExplicitForeignKeyName() ) ) {
			manyToOneBinding.setForeignKeyName( manyToOneSource.getExplicitForeignKeyName() );
		}

		final ManyToOneColumnBinder columnBinder = new ManyToOneColumnBinder(
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
			final ManyToOneFkSecondPass fkSecondPass = new ManyToOneFkSecondPass(
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

		manyToOneBinding.setCascadeDeleteEnabled( manyToOneSource.isCascadeDeleteEnabled() );
	}

	private Property createAnyAssociationAttribute(
			MappingDocument sourceDocument,
			SingularAttributeSourceAny anyMapping,
			Any anyBinding,
			String entityName) {
		final String attributeName = anyMapping.getName();

		bindAny( sourceDocument, anyMapping, anyBinding, anyMapping.getAttributeRole(), anyMapping.getAttributePath() );

		prepareValueTypeViaReflection( sourceDocument, anyBinding, entityName, attributeName, anyMapping.getAttributeRole() );

		anyBinding.createForeignKey();

		Property prop = new Property();
		prop.setValue( anyBinding );
		bindProperty(
				sourceDocument,
				anyMapping,
				prop
		);

		return prop;
	}

	private void bindAny(
			MappingDocument sourceDocument,
			final AnyMappingSource anyMapping,
			Any anyBinding,
			final AttributeRole attributeRole,
			AttributePath attributePath) {
		final TypeResolution keyTypeResolution = resolveType(
				sourceDocument,
				anyMapping.getKeySource().getTypeSource()
		);
		if ( keyTypeResolution != null ) {
			anyBinding.setIdentifierType( keyTypeResolution.typeName );
		}

		final TypeResolution discriminatorTypeResolution = resolveType(
				sourceDocument,
				anyMapping.getDiscriminatorSource().getTypeSource()
		);

		if ( discriminatorTypeResolution != null ) {
			anyBinding.setMetaType( discriminatorTypeResolution.typeName );
			try {
				final DiscriminatorType metaType = (DiscriminatorType) sourceDocument.getMetadataCollector()
						.getTypeResolver()
						.heuristicType( discriminatorTypeResolution.typeName );

				final HashMap anyValueBindingMap = new HashMap();
				for ( Map.Entry<String,String> discriminatorValueMappings : anyMapping.getDiscriminatorSource().getValueMappings().entrySet() ) {
					try {
						final Object discriminatorValue = metaType.stringToObject( discriminatorValueMappings.getKey() );
						final String mappedEntityName = sourceDocument.qualifyClassName( discriminatorValueMappings.getValue() );

						//noinspection unchecked
						anyValueBindingMap.put( discriminatorValue, mappedEntityName );
					}
					catch (Exception e) {
						throw new MappingException(
								String.format(
										Locale.ENGLISH,
										"Unable to interpret <meta-value value=\"%s\" class=\"%s\"/> defined as part of <any/> attribute [%s]",
										discriminatorValueMappings.getKey(),
										discriminatorValueMappings.getValue(),
										attributeRole.getFullPath()
								),
								e,
								sourceDocument.getOrigin()
						);
					}

				}
				anyBinding.setMetaValues( anyValueBindingMap );
			}
			catch (ClassCastException e) {
				throw new MappingException(
						String.format(
								Locale.ENGLISH,
								"Specified meta-type [%s] for <any/> attribute [%s] did not implement DiscriminatorType",
								discriminatorTypeResolution.typeName,
								attributeRole.getFullPath()
						),
						e,
						sourceDocument.getOrigin()
				);
			}
		}

		relationalObjectBinder.bindColumnOrFormula(
				sourceDocument,
				anyMapping.getDiscriminatorSource().getRelationalValueSource(),
				anyBinding,
				true,
				new RelationalObjectBinder.ColumnNamingDelegate() {
					@Override
					public Identifier determineImplicitName(LocalMetadataBuildingContext context) {
						return implicitNamingStrategy.determineAnyDiscriminatorColumnName(
								anyMapping.getDiscriminatorSource()
						);
					}
				}
		);

		relationalObjectBinder.bindColumnsAndFormulas(
				sourceDocument,
				anyMapping.getKeySource().getRelationalValueSources(),
				anyBinding,
				true,
				new RelationalObjectBinder.ColumnNamingDelegate() {
					@Override
					public Identifier determineImplicitName(LocalMetadataBuildingContext context) {
						return implicitNamingStrategy.determineAnyKeyColumnName(
								anyMapping.getKeySource()
						);
					}
				}
		);
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
							"Attribute mapping must define a name attribute: containingClassName=[%s], propertyName=[%s], role=[%s]",
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
							"Error calling Value#setTypeUsingReflection: containingClassName=[%s], propertyName=[%s], role=[%s]",
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

		if ( StringHelper.isNotEmpty( propertySource.getXmlNodeName() ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfDomEntityModeSupport();
		}

		property.setPropertyAccessorName(
				StringHelper.isNotEmpty( propertySource.getPropertyAccessorName() )
						? propertySource.getPropertyAccessorName()
						: mappingDocument.getMappingDefaults().getImplicitPropertyAccessorName()
		);

		if ( propertySource instanceof CascadeStyleSource ) {
			final CascadeStyleSource cascadeStyleSource = (CascadeStyleSource) propertySource;

			property.setCascade(
					StringHelper.isNotEmpty( cascadeStyleSource.getCascadeStyleName() )
							? cascadeStyleSource.getCascadeStyleName()
							: mappingDocument.getMappingDefaults().getImplicitCascadeStyleName()
			);
		}

		property.setOptimisticLocked( propertySource.isIncludedInOptimisticLocking() );

		if ( propertySource.isSingular() ) {
			final SingularAttributeSource singularAttributeSource = (SingularAttributeSource) propertySource;

			property.setInsertable( singularAttributeSource.isInsertable() );
			property.setUpdateable( singularAttributeSource.isUpdatable() );

			// NOTE : Property#is refers to whether a property is lazy via bytecode enhancement (not proxies)
			property.setLazy( singularAttributeSource.isBytecodeLazy() );

			final GenerationTiming generationTiming = singularAttributeSource.getGenerationTiming();
			if ( generationTiming == GenerationTiming.ALWAYS || generationTiming == GenerationTiming.INSERT ) {
				// we had generation specified...
				//   	HBM only supports "database generated values"
				property.setValueGenerationStrategy( new GeneratedValueGeneration( generationTiming ) );

				// generated properties can *never* be insertable...
				if ( property.isInsertable() ) {
					log.debugf(
							"Property [%s] specified %s generation, setting insertable to false : %s",
							propertySource.getName(),
							generationTiming.name(),
							mappingDocument.getOrigin()
					);
					property.setInsertable( false );
				}

				// properties generated on update can never be updatable...
				if ( property.isUpdateable() && generationTiming == GenerationTiming.ALWAYS ) {
					log.debugf(
							"Property [%s] specified ALWAYS generation, setting updateable to false : %s",
							propertySource.getName(),
							mappingDocument.getOrigin()
					);
					property.setUpdateable( false );
				}
			}
		}

		property.setMetaAttributes( propertySource.getToolingHintContext().getMetaAttributeMap() );

		if ( log.isDebugEnabled() ) {
			final StringBuilder message = new StringBuilder()
					.append( "Mapped property: " )
					.append( propertySource.getName() )
					.append( " -> [" );
			final Iterator itr = property.getValue().getColumnIterator();
			while ( itr.hasNext() ) {
				message.append( ( (Selectable) itr.next() ).getText() );
				if ( itr.hasNext() ) {
					message.append( ", " );
				}
			}
			message.append( "]" );
			log.debug( message.toString() );
		}
	}

	private void bindComponent(
			MappingDocument sourceDocument,
			EmbeddableSource embeddableSource,
			Component component,
			String containingClassName,
			String propertyName,
			String xmlNodeName,
			boolean isVirtual) {
		final String fullRole = embeddableSource.getAttributeRoleBase().getFullPath();
		final String explicitComponentClassName = extractExplicitComponentClassName( embeddableSource );

		bindComponent(
				sourceDocument,
				fullRole,
				embeddableSource,
				component,
				explicitComponentClassName,
				containingClassName,
				propertyName,
				isVirtual,
				embeddableSource.isDynamic(),
				xmlNodeName
		);
	}

	private String extractExplicitComponentClassName(EmbeddableSource embeddableSource) {
		if ( embeddableSource.getTypeDescriptor() == null ) {
			return null;
		}

		return embeddableSource.getTypeDescriptor().getName();
	}

	private void bindComponent(
			MappingDocument sourceDocument,
			String role,
			EmbeddableSource embeddableSource,
			Component componentBinding,
			String explicitComponentClassName,
			String containingClassName,
			String propertyName,
			boolean isVirtual,
			boolean isDynamic,
			String xmlNodeName) {

		componentBinding.setMetaAttributes( embeddableSource.getToolingHintContext().getMetaAttributeMap() );

		componentBinding.setRoleName( role );

		componentBinding.setEmbedded( isVirtual );

		// todo : better define the conditions in this if/else
		if ( isDynamic ) {
			// dynamic is represented as a Map
			log.debugf( "Binding dynamic-component [%s]", role );
			componentBinding.setDynamic( true );
		}
		else if ( isVirtual ) {
			// virtual (what used to be called embedded) is just a conceptual composition...
			// <properties/> for example
			if ( componentBinding.getOwner().hasPojoRepresentation() ) {
				log.debugf( "Binding virtual component [%s] to owner class [%s]", role, componentBinding.getOwner().getClassName() );
				componentBinding.setComponentClassName( componentBinding.getOwner().getClassName() );
			}
			else {
				log.debugf( "Binding virtual component [%s] as dynamic", role );
				componentBinding.setDynamic( true );
			}
		}
		else {
			log.debugf( "Binding component [%s]", role );
			if ( StringHelper.isNotEmpty( explicitComponentClassName ) ) {
				log.debugf( "Binding component [%s] to explicitly specified class", role, explicitComponentClassName );
				componentBinding.setComponentClassName( explicitComponentClassName );
			}
			else if ( componentBinding.getOwner().hasPojoRepresentation() ) {
				log.tracef( "Attempting to determine component class by reflection %s", role );
				final Class reflectedComponentClass;
				if ( StringHelper.isNotEmpty( containingClassName ) && StringHelper.isNotEmpty( propertyName ) ) {
					reflectedComponentClass = Helper.reflectedPropertyClass(
							sourceDocument,
							containingClassName,
							propertyName
					);
				}
				else {
					reflectedComponentClass = null;
				}

				if ( reflectedComponentClass == null ) {
					log.debugf(
							"Unable to determine component class name via reflection, and explicit " +
									"class name not given; role=[%s]",
							role
					);
				}
				else {
					componentBinding.setComponentClassName( reflectedComponentClass.getName() );
				}
			}
			else {
				componentBinding.setDynamic( true );
			}
		}

		String nodeName = xmlNodeName;
		if ( StringHelper.isNotEmpty( nodeName ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfDomEntityModeSupport();
		}

		// todo : anything else to pass along?
		bindAllCompositeAttributes(
				sourceDocument,
				embeddableSource,
				componentBinding
		);

		if ( embeddableSource.getParentReferenceAttributeName() != null ) {
			componentBinding.setParentProperty( embeddableSource.getParentReferenceAttributeName() );
		}

		if ( embeddableSource.isUnique() ) {
			final ArrayList<Column> cols = new ArrayList<Column>();
			final Iterator itr = componentBinding.getColumnIterator();
			while ( itr.hasNext() ) {
				final Object selectable = itr.next();
				// skip formulas.  ugh, yes terrible naming of these methods :(
				if ( !Column.class.isInstance( selectable ) ) {
					continue;
				}
				cols.add( (Column) selectable );
			}
			// todo : we may need to delay this
			componentBinding.getOwner().getTable().createUniqueKey( cols );
		}

		if ( embeddableSource.getTuplizerClassMap() != null ) {
			if ( embeddableSource.getTuplizerClassMap().size() > 1 ) {
				DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfMultipleEntityModeSupport();
			}
			for ( Map.Entry<EntityMode,String> tuplizerEntry : embeddableSource.getTuplizerClassMap().entrySet() ) {
				componentBinding.addTuplizer(
						tuplizerEntry.getKey(),
						tuplizerEntry.getValue()
				);
			}
		}
	}

	private void prepareComponentType(
			MappingDocument sourceDocument,
			String fullRole,
			Component componentBinding,
			String explicitComponentClassName,
			String containingClassName,
			String propertyName,
			boolean isVirtual,
			boolean isDynamic) {
	}

	private void bindAllCompositeAttributes(
			MappingDocument sourceDocument,
			EmbeddableSource embeddableSource,
			Component component) {

		for ( AttributeSource attributeSource : embeddableSource.attributeSources() ) {
			Property attribute = null;

			if ( SingularAttributeSourceBasic.class.isInstance( attributeSource ) ) {
				attribute = createBasicAttribute(
						sourceDocument,
						(SingularAttributeSourceBasic) attributeSource,
						new SimpleValue( sourceDocument.getMetadataCollector(), component.getTable() ),
						component.getComponentClassName()
				);
			}
			else if ( SingularAttributeSourceEmbedded.class.isInstance( attributeSource ) ) {
				attribute = createEmbeddedAttribute(
						sourceDocument,
						(SingularAttributeSourceEmbedded) attributeSource,
						new Component( sourceDocument.getMetadataCollector(), component ),
						component.getComponentClassName()
				);
			}
			else if ( SingularAttributeSourceManyToOne.class.isInstance( attributeSource ) ) {
				attribute = createManyToOneAttribute(
						sourceDocument,
						(SingularAttributeSourceManyToOne) attributeSource,
						new ManyToOne( sourceDocument.getMetadataCollector(), component.getTable() ),
						component.getComponentClassName()
				);
			}
			else if ( SingularAttributeSourceOneToOne.class.isInstance( attributeSource ) ) {
				attribute = createOneToOneAttribute(
						sourceDocument,
						(SingularAttributeSourceOneToOne) attributeSource,
						new OneToOne( sourceDocument.getMetadataCollector(), component.getTable(), component.getOwner() ),
						component.getComponentClassName()
				);
			}
			else if ( SingularAttributeSourceAny.class.isInstance( attributeSource ) ) {
				attribute = createAnyAssociationAttribute(
						sourceDocument,
						(SingularAttributeSourceAny) attributeSource,
						new Any( sourceDocument.getMetadataCollector(), component.getTable() ),
						component.getComponentClassName()
				);
			}
			else if ( PluralAttributeSource.class.isInstance( attributeSource ) ) {
				attribute = createPluralAttribute(
						sourceDocument,
						(PluralAttributeSource) attributeSource,
						component.getOwner()
				);
			}
			else {
				throw new AssertionFailure(
						String.format(
								Locale.ENGLISH,
								"Unexpected AttributeSource sub-type [%s] as part of composite [%s]",
								attributeSource.getClass().getName(),
								attributeSource.getAttributeRole().getFullPath()
						)

				);
			}

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

		final TypeResolution typeResolution = resolveType( mappingDocument, typeSource );
		if ( typeResolution == null ) {
			// no explicit type info was found
			return;
		}

		if ( CollectionHelper.isNotEmpty( typeResolution.parameters ) ) {
			simpleValue.setTypeParameters( typeResolution.parameters );
		}

		if ( typeResolution.typeName != null ) {
			simpleValue.setTypeName( typeResolution.typeName );
		}
	}

	private static class TypeResolution {
		private final String typeName;
		private final Properties parameters;

		public TypeResolution(String typeName, Properties parameters) {
			this.typeName = typeName;
			this.parameters = parameters;
		}
	}

	private static TypeResolution resolveType(
			MappingDocument sourceDocument,
			HibernateTypeSource typeSource) {
		if ( StringHelper.isEmpty( typeSource.getName() ) ) {
			return null;
		}

		String typeName = typeSource.getName();
		Properties typeParameters = new Properties();;

		final TypeDefinition typeDefinition = sourceDocument.getMetadataCollector().getTypeDefinition( typeName );
		if ( typeDefinition != null ) {
			// the explicit name referred to a type-def
			typeName = typeDefinition.getTypeImplementorClass().getName();
			if ( typeDefinition.getParameters() != null ) {
				typeParameters.putAll( typeDefinition.getParameters() );
			}
		}
//		else {
//			final BasicType basicType = sourceDocument.getMetadataCollector().getTypeResolver().basic( typeName );
//			if ( basicType == null ) {
//				throw new MappingException(
//						String.format(
//								Locale.ENGLISH,
//								"Mapping named an explicit type [%s] which could not be resolved",
//								typeName
//						),
//						sourceDocument.getOrigin()
//				);
//			}
//		}

		// parameters on the property mapping should override parameters in the type-def
		if ( typeSource.getParameters() != null ) {
			typeParameters.putAll( typeSource.getParameters() );
		}

		return new TypeResolution( typeName, typeParameters );
	}

	private Table bindEntityTableSpecification(
			final MappingDocument mappingDocument,
			TableSpecificationSource tableSpecSource,
			Table denormalizedSuperTable,
			final EntitySource entitySource,
			PersistentClass entityDescriptor) {
		final Namespace namespace = database.locateNamespace(
				determineCatalogName( tableSpecSource ),
				determineSchemaName( tableSpecSource )
		);

		final boolean isTable = TableSource.class.isInstance( tableSpecSource );
		final boolean isAbstract = entityDescriptor.isAbstract() == null ? false : entityDescriptor.isAbstract();
		final String subselect;
		final Identifier logicalTableName;
		final Table table;
		if ( isTable ) {
			final TableSource tableSource = (TableSource) tableSpecSource;

			if ( StringHelper.isNotEmpty( tableSource.getExplicitTableName() ) ) {
				logicalTableName = database.toIdentifier( tableSource.getExplicitTableName() );
			}
			else {
				final ImplicitEntityNameSource implicitNamingSource = new ImplicitEntityNameSource() {
					@Override
					public EntityNaming getEntityNaming() {
						return entitySource.getEntityNamingSource();
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return mappingDocument;
					}
				};
				logicalTableName = mappingDocument.getBuildingOptions()
						.getImplicitNamingStrategy()
						.determinePrimaryTableName( implicitNamingSource );
			}

			if ( denormalizedSuperTable == null ) {
				table = namespace.createTable( logicalTableName, isAbstract );
			}
			else {
				table = namespace.createDenormalizedTable(
						logicalTableName,
						isAbstract,
						denormalizedSuperTable
				);
			}
		}
		else {
			final InLineViewSource inLineViewSource = (InLineViewSource) tableSpecSource;
			subselect = inLineViewSource.getSelectStatement();
			logicalTableName = database.toIdentifier( inLineViewSource.getLogicalName() );
			if ( denormalizedSuperTable == null ) {
				table = new Table( namespace, subselect, isAbstract );
			}
			else {
				table = new DenormalizedTable( namespace, subselect, isAbstract, denormalizedSuperTable );
			}
			table.setName( logicalTableName.render() );
		}

		EntityTableXref superEntityTableXref = null;

		if ( entitySource.getSuperType() != null ) {
			//noinspection SuspiciousMethodCalls
			final String superEntityName = ( (EntitySource) entitySource.getSuperType() ).getEntityNamingSource()
					.getEntityName();
			superEntityTableXref = mappingDocument.getMetadataCollector().getEntityTableXref( superEntityName );
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
		}

		mappingDocument.getMetadataCollector().addEntityTableXref(
				entitySource.getEntityNamingSource().getEntityName(),
				logicalTableName,
				table,
				superEntityTableXref
		);

		if ( isTable ) {
			final TableSource tableSource = (TableSource) tableSpecSource;
			table.setRowId( tableSource.getRowId() );
			if ( StringHelper.isNotEmpty( tableSource.getCheckConstraint() ) ) {
				table.addCheckConstraint( tableSource.getCheckConstraint() );
			}
		} 

		table.setComment(tableSpecSource.getComment());

		mappingDocument.getMetadataCollector().addTableNameBinding( logicalTableName, table );

		return table;
	}

	private Identifier determineCatalogName(TableSpecificationSource tableSpecSource) {
		if ( StringHelper.isNotEmpty( tableSpecSource.getExplicitCatalogName() ) ) {
			return database.toIdentifier( tableSpecSource.getExplicitCatalogName() );
		}
		else {
			return database.getDefaultNamespace().getName().getCatalog();
		}
	}

	private Identifier determineSchemaName(TableSpecificationSource tableSpecSource) {
		if ( StringHelper.isNotEmpty( tableSpecSource.getExplicitSchemaName() ) ) {
			return database.toIdentifier( tableSpecSource.getExplicitSchemaName() );
		}
		else {
			return database.getDefaultNamespace().getName().getSchema();
		}
	}

	private static void bindCustomSql(
			MappingDocument sourceDocument,
			EntitySource entitySource,
			PersistentClass entityDescriptor) {
		if ( entitySource.getCustomSqlInsert() != null ) {
			entityDescriptor.setCustomSQLInsert(
					entitySource.getCustomSqlInsert().getSql(),
					entitySource.getCustomSqlInsert().isCallable(),
					entitySource.getCustomSqlInsert().getCheckStyle()
			);
		}

		if ( entitySource.getCustomSqlUpdate() != null ) {
			entityDescriptor.setCustomSQLUpdate(
					entitySource.getCustomSqlUpdate().getSql(),
					entitySource.getCustomSqlUpdate().isCallable(),
					entitySource.getCustomSqlUpdate().getCheckStyle()
			);
		}

		if ( entitySource.getCustomSqlDelete() != null ) {
			entityDescriptor.setCustomSQLDelete(
					entitySource.getCustomSqlDelete().getSql(),
					entitySource.getCustomSqlDelete().isCallable(),
					entitySource.getCustomSqlDelete().getCheckStyle()
			);
		}

		entityDescriptor.setLoaderName( entitySource.getCustomLoaderName() );
	}

	private static void bindCustomSql(
			MappingDocument sourceDocument,
			SecondaryTableSource secondaryTableSource,
			Join secondaryTable) {
		if ( secondaryTableSource.getCustomSqlInsert() != null ) {
			secondaryTable.setCustomSQLInsert(
					secondaryTableSource.getCustomSqlInsert().getSql(),
					secondaryTableSource.getCustomSqlInsert().isCallable(),
					secondaryTableSource.getCustomSqlInsert().getCheckStyle()
			);
		}

		if ( secondaryTableSource.getCustomSqlUpdate() != null ) {
			secondaryTable.setCustomSQLUpdate(
					secondaryTableSource.getCustomSqlUpdate().getSql(),
					secondaryTableSource.getCustomSqlUpdate().isCallable(),
					secondaryTableSource.getCustomSqlUpdate().getCheckStyle()
			);
		}

		if ( secondaryTableSource.getCustomSqlDelete() != null ) {
			secondaryTable.setCustomSQLDelete(
					secondaryTableSource.getCustomSqlDelete().getSql(),
					secondaryTableSource.getCustomSqlDelete().isCallable(),
					secondaryTableSource.getCustomSqlDelete().getCheckStyle()
			);
		}
	}

	private void registerSecondPass(SecondPass secondPass, MetadataBuildingContext context) {
		context.getMetadataCollector().addSecondPass( secondPass );
	}



	public static final class DelayedPropertyReferenceHandlerImpl implements InFlightMetadataCollector.DelayedPropertyReferenceHandler {
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
			log.tracef(
					"Performing delayed property-ref handling [%s, %s, %s]",
					referencedEntityName,
					referencedPropertyName,
					sourceElementSynopsis
			);

			PersistentClass entityBinding = metadataCollector.getEntityBinding( referencedEntityName );
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

			Property propertyBinding = entityBinding.getReferencedProperty( referencedPropertyName );
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
		public void doSecondPass(Map persistentClasses) throws org.hibernate.MappingException {
			bindCollectionTable();

			bindCollectionKey();
			bindCollectionIdentifier();
			bindCollectionIndex();
			bindCollectionElement();

			createBackReferences();

			collectionBinding.createAllKeys();

			if ( debugEnabled ) {
				log.debugf( "Mapped collection : " + getPluralAttributeSource().getAttributeRole().getFullPath() );
				log.debugf( "   + table -> " + getCollectionBinding().getTable().getName() );
				log.debugf( "   + key -> " + columns( getCollectionBinding().getKey() ) );
				if ( getCollectionBinding().isIndexed() ) {
					log.debugf( "   + index -> " + columns( ( (IndexedCollection) getCollectionBinding() ).getIndex() ) );
				}
				if ( getCollectionBinding().isOneToMany() ) {
					log.debugf( "   + one-to-many -> " + ( (OneToMany) getCollectionBinding().getElement() ).getReferencedEntityName() );
				}
				else {
					log.debugf( "   + element -> " + columns( getCollectionBinding().getElement() ) );
				}
			}
		}

		private String columns(Value value) {
			final StringBuilder builder = new StringBuilder();
			final Iterator<Selectable> selectableItr = value.getColumnIterator();
			while ( selectableItr.hasNext() ) {
				builder.append( selectableItr.next().getText() );
				if ( selectableItr.hasNext() ) {
					builder.append( ", " );
				}
			}
			return builder.toString();
		}

		private void bindCollectionTable() {
			// 2 main branches here:
			//		1) one-to-many
			//		2) everything else

			if ( pluralAttributeSource.getElementSource() instanceof PluralAttributeElementSourceOneToMany ) {
				// For one-to-many mappings, the "collection table" is the same as the table
				// of the associated entity (the entity making up the collection elements).
				// So lookup the associated entity and use its table here

				final PluralAttributeElementSourceOneToMany elementSource =
						(PluralAttributeElementSourceOneToMany) pluralAttributeSource.getElementSource();

				final PersistentClass persistentClass = mappingDocument.getMetadataCollector()
						.getEntityBinding( elementSource.getReferencedEntityName() );
				if ( persistentClass == null ) {
					throw new MappingException(
							String.format(
									Locale.ENGLISH,
									"Association [%s] references an unmapped entity [%s]",
									pluralAttributeSource.getAttributeRole().getFullPath(),
									pluralAttributeSource.getAttributeRole().getFullPath()
							),
							mappingDocument.getOrigin()
					);
				}

				// even though <key/> defines a property-ref I do not see where legacy
				// code ever attempts to use that to "adjust" the table in its use to
				// the actual table the referenced property belongs to.
				// todo : for correctness, though, we should look into that ^^
				collectionBinding.setCollectionTable( persistentClass.getTable() );
			}
			else {
				final TableSpecificationSource tableSpecSource = pluralAttributeSource.getCollectionTableSpecificationSource();
				final Identifier logicalCatalogName = determineCatalogName( tableSpecSource );
				final Identifier logicalSchemaName = determineSchemaName( tableSpecSource );
				final Namespace namespace = database.locateNamespace( logicalCatalogName, logicalSchemaName );

				final Table collectionTable;

				if ( tableSpecSource instanceof TableSource ) {
					final TableSource tableSource = (TableSource) tableSpecSource;
					Identifier logicalName;

					if ( StringHelper.isNotEmpty( tableSource.getExplicitTableName() ) ) {
						logicalName = Identifier.toIdentifier(
								tableSource.getExplicitTableName(),
								mappingDocument.getMappingDefaults().shouldImplicitlyQuoteIdentifiers()
						);
					}
					else {
						final EntityNaming ownerEntityNaming = new EntityNamingSourceImpl(
								collectionBinding.getOwner().getEntityName(),
								collectionBinding.getOwner().getClassName(),
								collectionBinding.getOwner().getJpaEntityName()
						);
						final ImplicitCollectionTableNameSource implicitNamingSource = new ImplicitCollectionTableNameSource() {
							@Override
							public Identifier getOwningPhysicalTableName() {
								return collectionBinding.getOwner().getTable().getNameIdentifier();
							}

							@Override
							public EntityNaming getOwningEntityNaming() {
								return ownerEntityNaming;
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
						logicalName = mappingDocument.getBuildingOptions()
								.getImplicitNamingStrategy()
								.determineCollectionTableName( implicitNamingSource );
					}

					collectionTable = namespace.createTable( logicalName, false );
				}
				else {
					collectionTable = new Table(
							namespace,
							( (InLineViewSource) tableSpecSource ).getSelectStatement(),
							false
					);
				}

				collectionBinding.setCollectionTable( collectionTable );
			}


			if ( debugEnabled ) {
				log.debugf( "Mapping collection: %s -> %s", collectionBinding.getRole(), collectionBinding.getCollectionTable().getName() );
			}

			if ( pluralAttributeSource.getCollectionTableComment() != null ) {
				collectionBinding.getCollectionTable().setComment( pluralAttributeSource.getCollectionTableComment() );
			}
			if ( pluralAttributeSource.getCollectionTableCheck() != null ) {
				collectionBinding.getCollectionTable().addCheckConstraint( pluralAttributeSource.getCollectionTableCheck() );
			}
		}

		protected void createBackReferences() {
			if ( collectionBinding.isOneToMany()
					&& !collectionBinding.isInverse()
					&& !collectionBinding.getKey().isNullable() ) {
				// for non-inverse one-to-many, with a not-null fk, add a backref!
				String entityName = ( (OneToMany) collectionBinding.getElement() ).getReferencedEntityName();
				PersistentClass referenced = mappingDocument.getMetadataCollector().getEntityBinding( entityName );
				Backref prop = new Backref();
				prop.setName( '_' + collectionBinding.getOwnerEntityName() + "." + pluralAttributeSource.getName() + "Backref" );
				prop.setUpdateable( false );
				prop.setSelectable( false );
				prop.setCollectionRole( collectionBinding.getRole() );
				prop.setEntityName( collectionBinding.getOwner().getEntityName() );
				prop.setValue( collectionBinding.getKey() );
				referenced.addProperty( prop );

				log.debugf(
						"Added virtual backref property [%s] : %s",
						prop.getName(),
						pluralAttributeSource.getAttributeRole().getFullPath()
				);
			}
		}

		protected void bindCollectionKey() {
			final PluralAttributeKeySource keySource = getPluralAttributeSource().getKeySource();
			final String propRef = keySource.getReferencedPropertyName();
			getCollectionBinding().setReferencedPropertyName( propRef );

			final KeyValue keyVal;
			if ( propRef == null ) {
				keyVal = getCollectionBinding().getOwner().getIdentifier();
			}
			else {
				keyVal = (KeyValue) getCollectionBinding().getOwner().getRecursiveProperty( propRef ).getValue();
			}
			final DependantValue key = new DependantValue(
					mappingDocument.getMetadataCollector(),
					getCollectionBinding().getCollectionTable(),
					keyVal
			);
			key.setForeignKeyName( keySource.getExplicitForeignKeyName() );
			key.setCascadeDeleteEnabled( getPluralAttributeSource().getKeySource().isCascadeDeleteEnabled() );

			final ImplicitJoinColumnNameSource.Nature implicitNamingNature;
			if ( getPluralAttributeSource().getElementSource() instanceof PluralAttributeElementSourceManyToMany
					|| getPluralAttributeSource().getElementSource() instanceof PluralAttributeElementSourceOneToMany ) {
				implicitNamingNature = ImplicitJoinColumnNameSource.Nature.ENTITY_COLLECTION;
			}
			else {
				implicitNamingNature = ImplicitJoinColumnNameSource.Nature.ELEMENT_COLLECTION;
			}

			relationalObjectBinder.bindColumnsAndFormulas(
					mappingDocument,
					getPluralAttributeSource().getKeySource().getRelationalValueSources(),
					key,
					getPluralAttributeSource().getKeySource().areValuesNullableByDefault(),
					new RelationalObjectBinder.ColumnNamingDelegate() {
						@Override
						public Identifier determineImplicitName(final LocalMetadataBuildingContext context) {
							// another case where HbmBinder was not adjusted to make use of NamingStrategy#foreignKeyColumnName
							// when that was added in developing annotation binding :(
//							return implicitNamingStrategy.determineJoinColumnName(
//									new ImplicitJoinColumnNameSource() {
//										private EntityNamingSourceImpl entityNamingSource;
//										private Identifier referencedColumnName;
//
//										@Override
//										public Nature getNature() {
//											return implicitNamingNature;
//										}
//
//										@Override
//										public EntityNaming getEntityNaming() {
//											if ( entityNamingSource == null ) {
//												entityNamingSource = new EntityNamingSourceImpl(
//														getCollectionBinding().getOwner().getEntityName(),
//														getCollectionBinding().getOwner().getClassName(),
//														getCollectionBinding().getOwner().getJpaEntityName()
//												);
//											}
//											return entityNamingSource;
//										}
//
//										@Override
//										public AttributePath getAttributePath() {
//											return getPluralAttributeSource().getAttributePath();
//										}
//
//										@Override
//										public Identifier getReferencedTableName() {
//											return getCollectionBinding().getCollectionTable().getNameIdentifier();
//										}
//
//										@Override
//										public Identifier getReferencedColumnName() {
//											if ( referencedColumnName == null ) {
//												final Iterator<Selectable> selectableItr = keyVal.getColumnIterator();
//												// assume there is just one, and that its a column...
//												final Column column = (Column) selectableItr.next();
//												referencedColumnName = getMappingDocument().getMetadataCollector()
//														.getDatabase()
//														.toIdentifier( column.getQuotedName() );
//											}
//											return referencedColumnName;
//										}
//
//										@Override
//										public MetadataBuildingContext getBuildingContext() {
//											return context;
//										}
//									}
//							);
							return context.getMetadataCollector().getDatabase().toIdentifier( Collection.DEFAULT_KEY_COLUMN_NAME );
						}
					}
			);

			key.createForeignKey();
			getCollectionBinding().setKey( key );

			key.setNullable( getPluralAttributeSource().getKeySource().areValuesNullableByDefault() );
			key.setUpdateable( getPluralAttributeSource().getKeySource().areValuesIncludedInUpdateByDefault() );
		}

		protected void bindCollectionIdentifier() {
			final CollectionIdSource idSource = getPluralAttributeSource().getCollectionIdSource();
			if ( idSource != null ) {
				final IdentifierCollection idBagBinding = (IdentifierCollection) getCollectionBinding();
				final SimpleValue idBinding = new SimpleValue(
						mappingDocument.getMetadataCollector(),
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
						new RelationalObjectBinder.ColumnNamingDelegate() {
							@Override
							public Identifier determineImplicitName(LocalMetadataBuildingContext context) {
								return database.toIdentifier( IdentifierCollection.DEFAULT_IDENTIFIER_COLUMN_NAME );
							}
						}
				);

				idBagBinding.setIdentifier( idBinding );

				makeIdentifier(
						mappingDocument,
						new IdentifierGeneratorDefinition( idSource.getGeneratorName(), idSource.getParameters() ),
						null,
						idBinding
				);
			}
		}

		protected void bindCollectionIndex() {
		}

		protected void bindCollectionElement() {
			if ( getPluralAttributeSource().getElementSource() instanceof PluralAttributeElementSourceBasic ) {
				final PluralAttributeElementSourceBasic elementSource =
						(PluralAttributeElementSourceBasic) getPluralAttributeSource().getElementSource();
				final SimpleValue elementBinding = new SimpleValue(
						getMappingDocument().getMetadataCollector(),
						getCollectionBinding().getCollectionTable()
				);

				bindSimpleValueType(
						getMappingDocument(),
						elementSource.getExplicitHibernateTypeSource(),
						elementBinding
				);

				relationalObjectBinder.bindColumnsAndFormulas(
						mappingDocument,
						elementSource.getRelationalValueSources(),
						elementBinding,
						elementSource.areValuesNullableByDefault(),
						new RelationalObjectBinder.ColumnNamingDelegate() {
							@Override
							public Identifier determineImplicitName(LocalMetadataBuildingContext context) {
//								return implicitNamingStrategy.determineBasicColumnName(
//										elementSource
//								);
								return context.getMetadataCollector().getDatabase().toIdentifier( Collection.DEFAULT_ELEMENT_COLUMN_NAME );
							}
						}
				);

				getCollectionBinding().setElement( elementBinding );
			}
			else if ( getPluralAttributeSource().getElementSource() instanceof PluralAttributeElementSourceEmbedded ) {
				final PluralAttributeElementSourceEmbedded elementSource =
						(PluralAttributeElementSourceEmbedded) getPluralAttributeSource().getElementSource();
				final Component elementBinding = new Component(
						getMappingDocument().getMetadataCollector(),
						getCollectionBinding()
				);

				final EmbeddableSource embeddableSource = elementSource.getEmbeddableSource();
				bindComponent(
						mappingDocument,
						embeddableSource,
						elementBinding,
						null,
						embeddableSource.getAttributePathBase().getProperty(),
						getPluralAttributeSource().getXmlNodeName(),
						false
				);

				getCollectionBinding().setElement( elementBinding );
			}
			else if ( getPluralAttributeSource().getElementSource() instanceof PluralAttributeElementSourceOneToMany ) {
				final PluralAttributeElementSourceOneToMany elementSource =
						(PluralAttributeElementSourceOneToMany) getPluralAttributeSource().getElementSource();
				final OneToMany elementBinding = new OneToMany(
						getMappingDocument().getMetadataCollector(),
						getCollectionBinding().getOwner()
				);
				collectionBinding.setElement( elementBinding );

				final PersistentClass referencedEntityBinding = mappingDocument.getMetadataCollector()
						.getEntityBinding( elementSource.getReferencedEntityName() );
				elementBinding.setReferencedEntityName( referencedEntityBinding.getEntityName() );
				elementBinding.setAssociatedClass( referencedEntityBinding );

				elementBinding.setIgnoreNotFound( elementSource.isIgnoreNotFound() );
			}
			else if ( getPluralAttributeSource().getElementSource() instanceof PluralAttributeElementSourceManyToMany ) {
				final PluralAttributeElementSourceManyToMany elementSource =
						(PluralAttributeElementSourceManyToMany) getPluralAttributeSource().getElementSource();
				final ManyToOne elementBinding = new ManyToOne(
						getMappingDocument().getMetadataCollector(),
						getCollectionBinding().getCollectionTable()
				);

				relationalObjectBinder.bindColumnsAndFormulas(
						getMappingDocument(),
						elementSource.getRelationalValueSources(),
						elementBinding,
						false,
						new RelationalObjectBinder.ColumnNamingDelegate() {
							@Override
							public Identifier determineImplicitName(final LocalMetadataBuildingContext context) {
//								return implicitNamingStrategy.determineJoinColumnName(
//										new ImplicitJoinColumnNameSource() {
//											private final PersistentClass pc = mappingDocument.getMetadataCollector()
//													.getEntityBinding( elementSource.getReferencedEntityName() );
//											private final EntityNaming referencedEntityNaming = new EntityNamingSourceImpl(
//													pc
//											);
//											private Identifier referencedTableName;
//											private Identifier referencedColumnName;
//
//											@Override
//											public Nature getNature() {
//												return Nature.ENTITY_COLLECTION;
//											}
//
//											@Override
//											public EntityNaming getEntityNaming() {
//												return referencedEntityNaming;
//											}
//
//											@Override
//											public AttributePath getAttributePath() {
//												// this is the mapped-by attribute, which we do not
//												// know here
//												return null;
//											}
//
//											@Override
//											public Identifier getReferencedTableName() {
//												if ( referencedTableName == null ) {
//													resolveTableAndColumn();
//												}
//												return referencedTableName;
//											}
//
//											private void resolveTableAndColumn() {
//												final Iterator itr;
//
//												if ( elementSource.getReferencedEntityAttributeName() == null ) {
//													// refers to PK
//													referencedTableName = pc.getIdentifier()
//															.getTable()
//															.getNameIdentifier();
//													itr = pc.getIdentifier().getColumnIterator();
//												}
//												else {
//													// refers to an attribute's column(s)
//													final Property referencedAttribute = pc.getProperty( elementSource.getReferencedEntityAttributeName() );
//													referencedTableName = referencedAttribute.getValue()
//															.getTable()
//															.getNameIdentifier();
//													itr = referencedAttribute.getValue().getColumnIterator();
//												}
//
//												// assume one and only one...
//												referencedColumnName = context.getMetadataCollector()
//														.getDatabase()
//														.getJdbcEnvironment()
//														.getIdentifierHelper()
//														.toIdentifier( ( (Column) itr.next() ).getQuotedName() );
//											}
//
//											@Override
//											public Identifier getReferencedColumnName() {
//												if ( referencedColumnName == null ) {
//													resolveTableAndColumn();
//												}
//												return referencedColumnName;
//											}
//
//											@Override
//											public MetadataBuildingContext getBuildingContext() {
//												return context;
//											}
//										}
//								);
								return context.getMetadataCollector().getDatabase().toIdentifier( Collection.DEFAULT_ELEMENT_COLUMN_NAME );
							}
						}
				);

				elementBinding.setLazy( elementSource.getFetchCharacteristics().getFetchTiming() != FetchTiming.IMMEDIATE );
				elementBinding.setFetchMode(
						elementSource.getFetchCharacteristics().getFetchStyle() == FetchStyle.SELECT
								? FetchMode.SELECT
								: FetchMode.JOIN
				);

				elementBinding.setForeignKeyName( elementSource.getExplicitForeignKeyName() );

				elementBinding.setReferencedEntityName( elementSource.getReferencedEntityName() );
				if ( StringHelper.isNotEmpty( elementSource.getReferencedEntityAttributeName() ) ) {
					elementBinding.setReferencedPropertyName( elementSource.getReferencedEntityAttributeName() );
					elementBinding.setReferenceToPrimaryKey( false );
				}
				else {
					elementBinding.setReferenceToPrimaryKey( true );
				}

				getCollectionBinding().setElement( elementBinding );

				getCollectionBinding().setManyToManyWhere( elementSource.getWhere() );
				getCollectionBinding().setManyToManyOrdering( elementSource.getOrder() );

				if ( !CollectionHelper.isEmpty( elementSource.getFilterSources() )
						|| elementSource.getWhere() != null ) {
					if ( getCollectionBinding().getFetchMode() == FetchMode.JOIN
							&& elementBinding.getFetchMode() != FetchMode.JOIN ) {
						throw new MappingException(
								String.format(
										Locale.ENGLISH,
										"many-to-many defining filter or where without join fetching is not " +
												"valid within collection [%s] using join fetching",
										getPluralAttributeSource().getAttributeRole().getFullPath()
								),
								getMappingDocument().getOrigin()
						);
					}
				}

				for ( FilterSource filterSource : elementSource.getFilterSources() ) {
					if ( filterSource.getName() == null ) {
						log.debugf(
								"Encountered filter with no name associated with many-to-many [%s]; skipping",
								getPluralAttributeSource().getAttributeRole().getFullPath()
						);
						continue;
					}

					if ( filterSource.getCondition() == null ) {
						throw new MappingException(
								String.format(
										Locale.ENGLISH,
										"No filter condition found for filter [%s] associated with many-to-many [%s]",
										filterSource.getName(),
										getPluralAttributeSource().getAttributeRole().getFullPath()
								),
								getMappingDocument().getOrigin()
						);
					}

					if ( debugEnabled ) {
						log.debugf(
								"Applying many-to-many filter [%s] as [%s] to collection [%s]",
								filterSource.getName(),
								filterSource.getCondition(),
								getPluralAttributeSource().getAttributeRole().getFullPath()
						);
					}

					getCollectionBinding().addManyToManyFilter(
							filterSource.getName(),
							filterSource.getCondition(),
							filterSource.shouldAutoInjectAliases(),
							filterSource.getAliasToTableMap(),
							filterSource.getAliasToEntityMap()
					);
				}
			}
			else if ( getPluralAttributeSource().getElementSource() instanceof PluralAttributeElementSourceManyToAny ) {
				final PluralAttributeElementSourceManyToAny elementSource =
						(PluralAttributeElementSourceManyToAny) getPluralAttributeSource().getElementSource();
				final Any elementBinding = new Any(
						getMappingDocument().getMetadataCollector(),
						getCollectionBinding().getCollectionTable()
				);
				bindAny(
						mappingDocument,
						elementSource,
						elementBinding,
						getPluralAttributeSource().getAttributeRole().append( "element" ),
						getPluralAttributeSource().getAttributePath().append( "element" )

				);
				getCollectionBinding().setElement( elementBinding );
			}
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

			boolean indexIsFormula = false;
			Iterator itr = getCollectionBinding().getIndex().getColumnIterator();
			while ( itr.hasNext() ) {
				if ( ( (Selectable) itr.next() ).isFormula() ) {
					indexIsFormula = true;
				}
			}

			if ( getCollectionBinding().isOneToMany()
					&& !getCollectionBinding().getKey().isNullable()
					&& !getCollectionBinding().isInverse()
					&& !indexIsFormula ) {
				final String entityName = ( (OneToMany) getCollectionBinding().getElement() ).getReferencedEntityName();
				final PersistentClass referenced = getMappingDocument().getMetadataCollector().getEntityBinding( entityName );
				final IndexBackref ib = new IndexBackref();
				ib.setName( '_' + getCollectionBinding().getOwnerEntityName() + "." + getPluralAttributeSource().getName() + "IndexBackref" );
				ib.setUpdateable( false );
				ib.setSelectable( false );
				ib.setCollectionRole( getCollectionBinding().getRole() );
				ib.setEntityName( getCollectionBinding().getOwner().getEntityName() );
				ib.setValue( getCollectionBinding().getIndex() );
				referenced.addProperty( ib );
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
			final String entityName = ( (OneToMany) collectionBinding.getElement() ).getReferencedEntityName();
			final PersistentClass referenced = mappingDocument.getMetadataCollector().getEntityBinding( entityName );
			final IndexBackref ib = new IndexBackref();
			ib.setName( '_' + collectionBinding.getOwnerEntityName() + "." + pluralAttributeSource.getName() + "IndexBackref" );
			ib.setUpdateable( false );
			ib.setSelectable( false );
			ib.setCollectionRole( collectionBinding.getRole() );
			ib.setEntityName( collectionBinding.getOwner().getEntityName() );
			ib.setValue( collectionBinding.getIndex() );
			referenced.addProperty( ib );
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
		final PluralAttributeSequentialIndexSource indexSource =
				(PluralAttributeSequentialIndexSource) attributeSource.getIndexSource();

		final SimpleValue indexBinding = new SimpleValue(
				mappingDocument.getMetadataCollector(),
				collectionBinding.getCollectionTable()
		);

		bindSimpleValueType(
				mappingDocument,
				indexSource.getTypeInformation(),
				indexBinding
		);

		relationalObjectBinder.bindColumnsAndFormulas(
				mappingDocument,
				indexSource.getRelationalValueSources(),
				indexBinding,
				attributeSource.getElementSource() instanceof PluralAttributeElementSourceOneToMany,
				new RelationalObjectBinder.ColumnNamingDelegate() {
					@Override
					public Identifier determineImplicitName(final LocalMetadataBuildingContext context) {
						return context.getBuildingOptions().getImplicitNamingStrategy().determineListIndexColumnName(
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
						);
					}
				}
		);

		collectionBinding.setIndex( indexBinding );
		collectionBinding.setBaseIndex( indexSource.getBase() );
	}

	private void bindMapKey(
			final MappingDocument mappingDocument,
			final IndexedPluralAttributeSource pluralAttributeSource,
			final org.hibernate.mapping.Map collectionBinding) {
		if ( pluralAttributeSource.getIndexSource() instanceof PluralAttributeMapKeySourceBasic ) {
			final PluralAttributeMapKeySourceBasic mapKeySource =
					(PluralAttributeMapKeySourceBasic) pluralAttributeSource.getIndexSource();
			final SimpleValue value = new SimpleValue(
					mappingDocument.getMetadataCollector(),
					collectionBinding.getCollectionTable()
			);
			bindSimpleValueType(
					mappingDocument,
					mapKeySource.getTypeInformation(),
					value
			);
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
					new RelationalObjectBinder.ColumnNamingDelegate() {
						@Override
						public Identifier determineImplicitName(LocalMetadataBuildingContext context) {
							return database.toIdentifier( IndexedCollection.DEFAULT_INDEX_COLUMN_NAME );
						}
					}
			);

			collectionBinding.setIndex( value );
		}
		else if ( pluralAttributeSource.getIndexSource() instanceof PluralAttributeMapKeySourceEmbedded ) {
			final PluralAttributeMapKeySourceEmbedded mapKeySource =
					(PluralAttributeMapKeySourceEmbedded) pluralAttributeSource.getIndexSource();
			final Component componentBinding = new Component(
					mappingDocument.getMetadataCollector(),
					collectionBinding
			);
			bindComponent(
					mappingDocument,
					mapKeySource.getEmbeddableSource(),
					componentBinding,
					null,
					pluralAttributeSource.getName(),
					mapKeySource.getXmlNodeName(),
					false
			);
			collectionBinding.setIndex( componentBinding );
		}
		else if ( pluralAttributeSource.getIndexSource() instanceof PluralAttributeMapKeyManyToManySource ) {
			final PluralAttributeMapKeyManyToManySource mapKeySource =
					(PluralAttributeMapKeyManyToManySource) pluralAttributeSource.getIndexSource();
			final ManyToOne mapKeyBinding = new ManyToOne(
					mappingDocument.getMetadataCollector(),
					collectionBinding.getCollectionTable()
			);

			mapKeyBinding.setReferencedEntityName( mapKeySource.getReferencedEntityName() );

			relationalObjectBinder.bindColumnsAndFormulas(
					mappingDocument,
					mapKeySource.getRelationalValueSources(),
					mapKeyBinding,
					true,
					new RelationalObjectBinder.ColumnNamingDelegate() {
						@Override
						public Identifier determineImplicitName(final LocalMetadataBuildingContext context) {
							return implicitNamingStrategy.determineMapKeyColumnName(
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
							);
						}
					}
			);
			collectionBinding.setIndex( mapKeyBinding );
		}
		else if ( pluralAttributeSource.getIndexSource() instanceof PluralAttributeMapKeyManyToAnySource ) {
			final PluralAttributeMapKeyManyToAnySource mapKeySource =
					(PluralAttributeMapKeyManyToAnySource) pluralAttributeSource.getIndexSource();
			final Any mapKeyBinding = new Any(
					mappingDocument.getMetadataCollector(),
					collectionBinding.getCollectionTable()
			);
			bindAny(
					mappingDocument,
					mapKeySource,
					mapKeyBinding,
					pluralAttributeSource.getAttributeRole().append( "key" ),
					pluralAttributeSource.getAttributePath().append( "key" )
			);
			collectionBinding.setIndex( mapKeyBinding );
		}
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
			for ( RelationalValueSource relationalValueSource : manyToOneSource.getRelationalValueSources() ) {
				if ( relationalValueSource instanceof ColumnSource ) {
					if ( ( (ColumnSource) relationalValueSource ).getName() == null ) {
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

			final PersistentClass referencedEntityBinding = mappingDocument.getMetadataCollector()
					.getEntityBinding( referencedEntityName );
			if ( referencedEntityBinding == null ) {
				return false;
			}

			// for implicit naming, we can do it immediately if the associated entity
			// is bound and the reference is to its PK.  For property-refs, we'd have to
			// be *sure* that the column(s) for the referenced property is fully bound
			// and we just cannot know that in today's model

			return manyToOneSource.getReferencedEntityAttributeName() == null;
		}

		@Override
		public void doSecondPass(Map persistentClasses) throws org.hibernate.MappingException {
			if ( allColumnsNamed ) {
				relationalObjectBinder.bindColumnsAndFormulas(
						mappingDocument,
						manyToOneSource.getRelationalValueSources(),
						manyToOneBinding,
						manyToOneSource.areValuesNullableByDefault(),
						new RelationalObjectBinder.ColumnNamingDelegate() {
							@Override
							public Identifier determineImplicitName(LocalMetadataBuildingContext context) {
								throw new AssertionFailure( "Argh!!!" );
							}
						}
				);
			}
			else {
				// Otherwise we have some dependency resolution to do in order to perform
				// implicit naming.  If we get here, we assume that there is only a single
				// column making up the FK

				final String referencedEntityAttributeName = manyToOneSource.getReferencedEntityAttributeName();

				final PersistentClass referencedEntityBinding = mappingDocument.getMetadataCollector()
						.getEntityBinding( referencedEntityName );

				if ( referencedEntityBinding == null ) {
					throw new AssertionFailure(
							"Unable to locate referenced entity mapping [" + referencedEntityName +
									"] in order to process many-to-one FK : " + manyToOneSource.getAttributeRole().getFullPath()
					);
				}

				final EntityNaming entityNaming = new EntityNamingSourceImpl( referencedEntityBinding );

				final Identifier referencedTableName;
				final Identifier referencedColumnName;

				if ( referencedEntityAttributeName == null ) {
					referencedTableName = referencedEntityBinding.getTable().getNameIdentifier();
					final Column referencedColumn = referencedEntityBinding.getTable()
							.getPrimaryKey()
							.getColumn( 0 );
					referencedColumnName = mappingDocument.getMetadataCollector()
							.getDatabase()
							.getJdbcEnvironment()
							.getIdentifierHelper()
							.toIdentifier( referencedColumn.getQuotedName() );
				}
				else {
					final Property referencedProperty = referencedEntityBinding.getReferencedProperty(
							referencedEntityAttributeName
					);
					final SimpleValue value = (SimpleValue) referencedProperty.getValue();
					referencedTableName = value.getTable().getNameIdentifier();
					final Column referencedColumn = (Column) value.getColumnIterator().next();
					referencedColumnName = mappingDocument.getMetadataCollector()
							.getDatabase()
							.getJdbcEnvironment()
							.getIdentifierHelper()
							.toIdentifier( referencedColumn.getQuotedName() );
				}

				relationalObjectBinder.bindColumnsAndFormulas(
						mappingDocument,
						manyToOneSource.getRelationalValueSources(),
						manyToOneBinding,
						manyToOneSource.areValuesNullableByDefault(),
						new RelationalObjectBinder.ColumnNamingDelegate() {
							@Override
							public Identifier determineImplicitName(final LocalMetadataBuildingContext context) {
								// NOTE : This sucks!!!  The problem is that the legacy HBMBinder routed this
								// through the legacy NamingStrategy#propertyToColumName.
								//
								// Basically, when developing the AnnotationBinder and
								// NamingStrategy#foreignKeyColumnName HbmBinder was never updated to
								// utilize that new method.
//								return implicitNamingStrategy.determineJoinColumnName(
//										new ImplicitJoinColumnNameSource() {
//											@Override
//											public Nature getNature() {
//												return Nature.ENTITY;
//											}
//
//											@Override
//											public EntityNaming getEntityNaming() {
//												return entityNaming;
//											}
//
//											@Override
//											public AttributePath getAttributePath() {
//												return manyToOneSource.getAttributePath();
//											}
//
//											@Override
//											public Identifier getReferencedTableName() {
//												return referencedTableName;
//											}
//
//											@Override
//											public Identifier getReferencedColumnName() {
//												return referencedColumnName;
//											}
//
//											@Override
//											public MetadataBuildingContext getBuildingContext() {
//												return context;
//											}
//										}
//								);
								return implicitNamingStrategy.determineBasicColumnName(
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
								);
							}
						}
				);
			}
		}
	}

	private class ManyToOneFkSecondPass extends FkSecondPass {
		private final MappingDocument mappingDocument;
		private final ManyToOne manyToOneBinding;

		private final String referencedEntityName;
		private final String referencedEntityAttributeName;


		public ManyToOneFkSecondPass(
				MappingDocument mappingDocument,
				SingularAttributeSourceManyToOne manyToOneSource,
				ManyToOne manyToOneBinding,
				String referencedEntityName) {
			super( manyToOneBinding, null );

			if ( referencedEntityName == null ) {
				throw new MappingException(
						"entity name referenced by many-to-one required [" + manyToOneSource.getAttributeRole().getFullPath() + "]",
						mappingDocument.getOrigin()
				);
			}
			this.mappingDocument = mappingDocument;
			this.manyToOneBinding = manyToOneBinding;
			this.referencedEntityName = referencedEntityName;

			this.referencedEntityAttributeName = manyToOneSource.getReferencedEntityAttributeName();
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
		public void doSecondPass(Map persistentClasses) throws org.hibernate.MappingException {
			if ( referencedEntityAttributeName == null ) {
				manyToOneBinding.createForeignKey();
			}
			else {
				manyToOneBinding.createPropertyRefConstraints( mappingDocument.getMetadataCollector().getEntityBindingMap() );
			}
		}

		public boolean canProcessImmediately() {
			// We can process the FK immediately if it is a reference to the associated
			// entity's PK.
			//
			// There is an assumption here that the columns making up the FK have been bound.
			// We assume the caller checks that
			final PersistentClass referencedEntityBinding = mappingDocument.getMetadataCollector()
					.getEntityBinding( referencedEntityName );
			return referencedEntityBinding != null && referencedEntityAttributeName != null;

		}
	}

	private class NaturalIdUniqueKeyBinderImpl implements NaturalIdUniqueKeyBinder {
		private final MappingDocument mappingDocument;
		private final PersistentClass entityBinding;
		private final List<Property> attributeBindings = new ArrayList<Property>();

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
			log.debugf( "Binding natural-id UniqueKey for entity : " + entityBinding.getEntityName() );

			final List<Identifier> columnNames = new ArrayList<Identifier>();

			final UniqueKey uk = new UniqueKey();
			uk.setTable( entityBinding.getTable() );
			for ( Property attributeBinding : attributeBindings ) {
				final Iterator itr = attributeBinding.getColumnIterator();
				while ( itr.hasNext() ) {
					final Object selectable = itr.next();
					if ( Column.class.isInstance( selectable ) ) {
						final Column column = (Column) selectable;
						uk.addColumn( column );
						columnNames.add(
								mappingDocument.getMetadataCollector().getDatabase().toIdentifier( column.getQuotedName() )
						);
					}
				}
				uk.addColumns( attributeBinding.getColumnIterator() );
			}

			final Identifier ukName = mappingDocument.getBuildingOptions().getImplicitNamingStrategy().determineUniqueKeyName(
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
					}
			);
			uk.setName( ukName.render( mappingDocument.getMetadataCollector().getDatabase().getDialect() ) );

			entityBinding.getTable().addUniqueKey( uk );
		}

	}
}
