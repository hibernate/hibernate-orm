/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.JDBCException;
import org.hibernate.LazyInitializationException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.MappingException;
import org.hibernate.PropertyValueException;
import org.hibernate.QueryException;
import org.hibernate.Timeouts;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementHelper;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeDescriptor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cache.spi.entry.ReferenceCacheEntryImpl;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.cache.spi.entry.StructuredCacheEntry;
import org.hibernate.cache.spi.entry.UnstructuredCacheEntry;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.internal.ImmutableEntityEntryFactory;
import org.hibernate.engine.internal.MutableEntityEntryFactory;
import org.hibernate.engine.profile.internal.FetchProfileAffectee;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.internal.VersionGeneration;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.internal.FilterHelper;
import org.hibernate.internal.util.ImmutableBitSet;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.LockModeEnumMap;
import org.hibernate.jdbc.Expectation;
import org.hibernate.loader.ast.internal.EntityConcreteTypeLoader;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.loader.ast.internal.LoaderSqlAstCreationState;
import org.hibernate.loader.ast.internal.MultiIdEntityLoaderArrayParam;
import org.hibernate.loader.ast.internal.MultiIdEntityLoaderInPredicate;
import org.hibernate.loader.ast.internal.SingleIdArrayLoadPlan;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderProvidedQueryImpl;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderStandardImpl;
import org.hibernate.loader.ast.internal.SingleUniqueKeyEntityLoaderStandard;
import org.hibernate.loader.ast.spi.BatchLoaderFactory;
import org.hibernate.loader.ast.spi.MultiIdEntityLoader;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.loader.ast.spi.SingleIdEntityLoader;
import org.hibernate.loader.ast.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.AttributeMappingsMap;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.mapping.internal.BasicEntityIdentifierMappingImpl;
import org.hibernate.metamodel.mapping.internal.CompoundNaturalIdMapping;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
import org.hibernate.metamodel.mapping.internal.DiscriminatorTypeImpl;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityRowIdMappingImpl;
import org.hibernate.metamodel.mapping.internal.EntityVersionMappingImpl;
import org.hibernate.metamodel.mapping.internal.ExplicitColumnDiscriminatorMappingImpl;
import org.hibernate.metamodel.mapping.internal.GeneratedValuesProcessor;
import org.hibernate.metamodel.mapping.internal.ImmutableAttributeMappingList;
import org.hibernate.metamodel.mapping.internal.InFlightEntityMappingType;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.SimpleAttributeMetadata;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.metamodel.mapping.internal.UnifiedAnyDiscriminatorConverter;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.models.internal.util.CollectionHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.DeleteCoordinatorSoft;
import org.hibernate.persister.entity.mutation.DeleteCoordinatorStandard;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.InsertCoordinatorStandard;
import org.hibernate.persister.entity.mutation.MergeCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinatorNoOp;
import org.hibernate.persister.entity.mutation.UpdateCoordinatorStandard;
import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.query.PathException;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sql.internal.SQLQueryParser;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategyProvider;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.Alias;
import org.hibernate.sql.InFragment;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.AliasedExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.entity.internal.EntityResultImpl;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.tuple.NonIdentifierAttribute;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.TypeConfiguration;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static org.hibernate.boot.model.internal.SoftDeleteHelper.resolveSoftDeleteMapping;
import static org.hibernate.engine.internal.CacheHelper.fromSharedCache;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.processIfPersistentAttributeInterceptable;
import static org.hibernate.generator.EventType.FORCE_INCREMENT;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getGeneratedValuesDelegate;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.ReflectHelper.isAbstractClass;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.StringHelper.qualifyConditionally;
import static org.hibernate.internal.util.StringHelper.root;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_INT_ARRAY;
import static org.hibernate.internal.util.collections.ArrayHelper.contains;
import static org.hibernate.internal.util.collections.ArrayHelper.indexOf;
import static org.hibernate.internal.util.collections.ArrayHelper.isAllTrue;
import static org.hibernate.internal.util.collections.ArrayHelper.to2DStringArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toIntArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toObjectArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toStringArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toTypeArray;
import static org.hibernate.internal.util.collections.CollectionHelper.combine;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.setOfSize;
import static org.hibernate.internal.util.collections.CollectionHelper.toSmallList;
import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.supportsSqlArrayType;
import static org.hibernate.metamodel.RepresentationMode.POJO;
import static org.hibernate.metamodel.mapping.EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME;
import static org.hibernate.metamodel.mapping.internal.GeneratedValuesProcessor.getGeneratedAttributes;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildBasicAttributeMapping;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildEncapsulatedCompositeIdentifierMapping;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildNonEncapsulatedCompositeIdentifierMapping;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.resolveAggregateColumnBasicType;
import static org.hibernate.metamodel.mapping.internal.MappingModelHelper.isCompatibleModelPart;
import static org.hibernate.persister.entity.DiscriminatorHelper.NOT_NULL_DISCRIMINATOR;
import static org.hibernate.persister.entity.DiscriminatorHelper.NULL_DISCRIMINATOR;
import static org.hibernate.pretty.MessageHelper.infoString;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * Basic functionality for persisting an entity via JDBC, using either generated or custom SQL.
 *
 * @author Gavin King
 */
@Internal
@SuppressWarnings("deprecation")
public abstract class AbstractEntityPersister
		extends EntityMetamodel
		implements EntityPersister, InFlightEntityMappingType, EntityMutationTarget, LazyPropertyInitializer,
				FetchProfileAffectee, Joinable {

	/**
	 * The property name of the "special" identifier property in HQL
	 *
	 * @deprecated this feature of HQL is now deprecated
	 */
	@Deprecated(since = "6.2")
	public static final String ENTITY_ID = "id";
	public static final String ENTITY_CLASS = "class";

	public static final String VERSION_COLUMN_ALIAS = "version_";
	public static final String ROWID_ALIAS = "rowid_";

	private final NavigableRole navigableRole;
	private final SessionFactoryImplementor factory;
	private final EntityEntryFactory entityEntryFactory;

	private final String sqlAliasStem;
	private final String jpaEntityName;

	private SingleIdEntityLoader<?> singleIdLoader;
	private MultiIdEntityLoader<?> multiIdLoader;
	private NaturalIdLoader<?> naturalIdLoader;
	private MultiNaturalIdLoader<?> multiNaturalIdLoader;

	private final String[] rootTableKeyColumnNames;
	private final String[] rootTableKeyColumnReaders;
	private final String[] rootTableKeyColumnReaderTemplates;
	private final String[] identifierAliases;
	private final int identifierColumnSpan;
	private final String versionColumnName;
	private final boolean hasFormulaProperties;
	protected final int batchSize;
	private final boolean hasSubselectLoadableCollections;
	private final boolean hasPartitionedSelectionMapping;
	private final boolean hasCollectionNotReferencingPK;
	protected final String rowIdName;

	// The optional SQL string defined in the where attribute
	private final String sqlWhereStringTableExpression;
	private final String sqlWhereStringTemplate;

	//information about properties of this class,
	//including inherited properties
	//(only really needed for updatable/insertable properties)
	private final String[][] propertyColumnAliases;
	private final String[][] propertyColumnNames;
	private final String[][] propertyColumnFormulaTemplates;
	private final boolean[][] propertyColumnUpdateable;
	private final boolean[][] propertyColumnInsertable;
	private final Set<String> sharedColumnNames;

	//information about lazy properties of this class
	private final String[] lazyPropertyNames;
	private final int[] lazyPropertyNumbers;
	private final Type[] lazyPropertyTypes;
	private final Set<String> nonLazyPropertyNames;

	//information about all properties in class hierarchy
	private final String[] subclassPropertyNameClosure;
	private final Type[] subclassPropertyTypeClosure;
	private final String[][] subclassPropertyFormulaTemplateClosure;
	private final String[][] subclassPropertyColumnNameClosure;
	private final String[][] subclassPropertyColumnReaderClosure;
	private final String[][] subclassPropertyColumnReaderTemplateClosure;
	private final FetchMode[] subclassPropertyFetchModeClosure;

	private Map<String, SingleIdArrayLoadPlan> lazyLoadPlanByFetchGroup;
	private final LockModeEnumMap<LockingStrategy> lockers = new LockModeEnumMap<>();
	private String sqlVersionSelectString;

	private EntityTableMapping[] tableMappings;
	private InsertCoordinator insertCoordinator;
	private UpdateCoordinator updateCoordinator;
	private DeleteCoordinator deleteCoordinator;
	private UpdateCoordinator mergeCoordinator;

	private SqmMultiTableMutationStrategy sqmMultiTableMutationStrategy;
	private SqmMultiTableInsertStrategy sqmMultiTableInsertStrategy;

	private final EntityDataAccess cacheAccessStrategy;
	private final NaturalIdDataAccess naturalIdRegionAccessStrategy;
	private final CacheEntryHelper cacheEntryHelper;
	private final boolean canReadFromCache;
	private final boolean canWriteToCache;
	private final boolean invalidateCache;
	private final boolean isLazyPropertiesCacheable;
	private final boolean useReferenceCacheEntries;
	private final boolean useShallowQueryCacheLayout;
	private final boolean storeDiscriminatorInShallowQueryCacheLayout;

	// dynamic filters attached to the class-level
	private final FilterHelper filterHelper;
	private volatile Set<String> affectingFetchProfileNames;

	protected List<? extends ModelPart> insertGeneratedProperties;
	protected List<? extends ModelPart> updateGeneratedProperties;
	private GeneratedValuesProcessor insertGeneratedValuesProcessor;
	private GeneratedValuesProcessor updateGeneratedValuesProcessor;

	private GeneratedValuesMutationDelegate insertDelegate;
	private GeneratedValuesMutationDelegate updateDelegate;
	private String identitySelectString;

	private final JavaType<?> javaType;
	private final EntityRepresentationStrategy representationStrategy;

	private EntityMappingType superMappingType;
	private SortedMap<String, EntityMappingType> subclassMappingTypes;
	private final boolean concreteProxy;
	private EntityConcreteTypeLoader concreteTypeLoader;

	private EntityIdentifierMapping identifierMapping;
	private NaturalIdMapping naturalIdMapping;
	private EntityVersionMapping versionMapping;
	private EntityRowIdMapping rowIdMapping;
	private EntityDiscriminatorMapping discriminatorMapping;
	private SoftDeleteMapping softDeleteMapping;

	private AttributeMappingsList attributeMappings;
	protected AttributeMappingsMap declaredAttributeMappings = AttributeMappingsMap.builder().build();
	protected AttributeMappingsList staticFetchableList;
	// We build a cache for getters and setters to avoid megamorphic calls
	private Getter[] getterCache;
	private Setter[] setterCache;

	private final String queryLoaderName;

	private BeforeExecutionGenerator versionGenerator;

	protected ReflectionOptimizer.AccessOptimizer accessOptimizer;

	protected final String[] fullDiscriminatorSQLValues;
	private final Object[] fullDiscriminatorValues;

	/**
	 * Warning:
	 * When there are duplicated property names in the subclasses
	 * then propertyMapping will only contain one of those properties.
	 * To ensure correct results, propertyMapping should only be used
	 * for the concrete EntityPersister (since the concrete EntityPersister
	 * cannot have duplicated property names).
	 */
	private final EntityPropertyMapping propertyMapping;

	private List<UniqueKeyEntry> uniqueKeyEntries = null; //lazily initialized
	private ConcurrentHashMap<String,SingleIdArrayLoadPlan> nonLazyPropertyLoadPlansByName;

	public AbstractEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final RuntimeModelCreationContext creationContext)
				throws HibernateException {
		super( persistentClass, creationContext );
		jpaEntityName = persistentClass.getJpaEntityName();

		//set it here, but don't call it, since it's still uninitialized!
		factory = creationContext.getSessionFactory();

		sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromEntityName( persistentClass.getEntityName() );

		navigableRole = new NavigableRole( persistentClass.getEntityName() );

		final var factoryOptions = creationContext.getSessionFactoryOptions();

		if ( factoryOptions.isSecondLevelCacheEnabled() ) {
			this.cacheAccessStrategy = cacheAccessStrategy;
			this.naturalIdRegionAccessStrategy = naturalIdRegionAccessStrategy;
			canWriteToCache = determineCanWriteToCache( persistentClass, cacheAccessStrategy );
			canReadFromCache = determineCanReadFromCache( persistentClass, cacheAccessStrategy );
			isLazyPropertiesCacheable = persistentClass.getRootClass().isLazyPropertiesCacheable();
		}
		else {
			this.cacheAccessStrategy = null;
			this.naturalIdRegionAccessStrategy = null;
			canWriteToCache = false;
			canReadFromCache = false;
			isLazyPropertiesCacheable = true;
		}

		entityEntryFactory =
				isMutable()
						? MutableEntityEntryFactory.INSTANCE
						: ImmutableEntityEntryFactory.INSTANCE;

		// Handle any filters applied to the class level
		if ( isNotEmpty( persistentClass.getFilters() ) ) {
			filterHelper = new FilterHelper(
					persistentClass.getFilters(),
					getEntityNameByTableNameMap( persistentClass,
							factory.getSqlStringGenerationContext() ),
					factory
			);
		}
		else {
			filterHelper = null;
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		representationStrategy =
				creationContext.getBootstrapContext().getRepresentationStrategySelector()
						.resolveStrategy( persistentClass, this, creationContext );
		javaType = representationStrategy.getLoadJavaType();
		assert javaType != null;
		accessOptimizer = accessOptimizer( representationStrategy );

		concreteProxy =
				isPolymorphic()
					&& ( getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() || hasProxy() )
					&& persistentClass.isConcreteProxy();

		final var dialect = creationContext.getDialect();

		batchSize =
				persistentClass.getBatchSize() < 0
						? factoryOptions.getDefaultBatchFetchSize()
						: persistentClass.getBatchSize();
		hasSubselectLoadableCollections = persistentClass.hasSubselectLoadableCollections();
		hasPartitionedSelectionMapping = persistentClass.hasPartitionedSelectionMapping();
		hasCollectionNotReferencingPK = persistentClass.hasCollectionNotReferencingPK();

		propertyMapping = new EntityPropertyMapping( this );

		// IDENTIFIER

		identifierColumnSpan = persistentClass.getIdentifier().getColumnSpan();
		rootTableKeyColumnNames = new String[identifierColumnSpan];
		rootTableKeyColumnReaders = new String[identifierColumnSpan];
		rootTableKeyColumnReaderTemplates = new String[identifierColumnSpan];
		identifierAliases = new String[identifierColumnSpan];

		final var rootTable = persistentClass.getRootTable();
		final String rowId = rootTable.getRowId();
		rowIdName = rowId == null ? null : dialect.rowId( rowId );

		queryLoaderName = persistentClass.getLoaderName();

		final var typeConfiguration = creationContext.getTypeConfiguration();

		final var columns = persistentClass.getIdentifier().getColumns();
		for (int i = 0; i < columns.size(); i++ ) {
			final var column = columns.get(i);
			rootTableKeyColumnNames[i] = column.getQuotedName( dialect );
			rootTableKeyColumnReaders[i] = column.getReadExpr( dialect );
			rootTableKeyColumnReaderTemplates[i] = column.getTemplate( dialect, typeConfiguration );
			identifierAliases[i] = column.getAlias( dialect, rootTable );
		}

		// VERSION

		versionColumnName =
				persistentClass.isVersioned()
						? persistentClass.getVersion().getColumns().get(0).getQuotedName( dialect )
						: null;

		//WHERE STRING

		if ( isEmpty( persistentClass.getWhere() ) ) {
			sqlWhereStringTableExpression = null;
			sqlWhereStringTemplate = null;
		}
		else {
			sqlWhereStringTableExpression =
					determineTableName( getCountainingClass( persistentClass ).getTable() );
			sqlWhereStringTemplate =
					renderSqlWhereStringTemplate( persistentClass, dialect, typeConfiguration );
		}

		// PROPERTIES
		final int hydrateSpan = getPropertySpan();
		propertyColumnAliases = new String[hydrateSpan][];
		propertyColumnNames = new String[hydrateSpan][];
		propertyColumnFormulaTemplates = new String[hydrateSpan][];
		propertyColumnUpdateable = new boolean[hydrateSpan][];
		propertyColumnInsertable = new boolean[hydrateSpan][];
		sharedColumnNames = new HashSet<>();
		nonLazyPropertyNames = new HashSet<>();

		final HashSet<Property> thisClassProperties = new HashSet<>();
		final ArrayList<String> lazyNames = new ArrayList<>();
		final ArrayList<Integer> lazyNumbers = new ArrayList<>();
		final ArrayList<Type> lazyTypes = new ArrayList<>();

		final var propertyClosure = persistentClass.getPropertyClosure();
		boolean foundFormula = false;
		for ( int i = 0; i < propertyClosure.size(); i++ ) {
			final var property = propertyClosure.get(i);
			thisClassProperties.add( property );
			final var propertyValue = property.getValue();

			final int span = property.getColumnSpan();
			final String[] colNames = new String[span];
			final String[] colAliases = new String[span];
			final String[] formulaTemplates = new String[span];
			final var selectables = property.getSelectables();
			for ( int k = 0; k < selectables.size(); k++ ) {
				final var selectable = selectables.get(k);
				colAliases[k] = selectable.getAlias( dialect, propertyValue.getTable() );
				if ( selectable instanceof Formula formula ) {
					foundFormula = true;
					formula.setFormula( substituteBrackets( formula.getFormula() ) );
					formulaTemplates[k] = selectable.getTemplate( dialect, typeConfiguration );
				}
				else if ( selectable instanceof Column column ) {
					colNames[k] = column.getQuotedName( dialect );
				}
			}
			propertyColumnNames[i] = colNames;
			propertyColumnFormulaTemplates[i] = formulaTemplates;
			propertyColumnAliases[i] = colAliases;

			final boolean lazy = !EnhancementHelper.includeInBaseFetchGroup(
					property,
					isInstrumented(),
					entityName -> {
						final var entityBinding =
								creationContext.getMetadata()
										.getEntityBinding( entityName );
						assert entityBinding != null;
						return entityBinding.hasSubclasses();
					},
					factoryOptions.isCollectionsInDefaultFetchGroupEnabled()
			);

			if ( lazy ) {
				lazyNames.add( property.getName() );
				lazyNumbers.add( i );
				lazyTypes.add( propertyValue.getType() );
			}
			else {
				nonLazyPropertyNames.add( property.getName() );
			}

			propertyColumnUpdateable[i] = propertyValue.getColumnUpdateability();
			propertyColumnInsertable[i] = propertyValue.getColumnInsertability();
		}
		hasFormulaProperties = foundFormula;
		lazyPropertyNames = toStringArray( lazyNames );
		lazyPropertyNumbers = toIntArray( lazyNumbers );
		lazyPropertyTypes = toTypeArray( lazyTypes );

		// SUBCLASS PROPERTY CLOSURE
		final ArrayList<String> aliases = new ArrayList<>();
		final ArrayList<String> formulaAliases = new ArrayList<>();
		final ArrayList<Type> types = new ArrayList<>();
		final ArrayList<String> names = new ArrayList<>();
		final ArrayList<String[]> templates = new ArrayList<>();
		final ArrayList<String[]> propColumns = new ArrayList<>();
		final ArrayList<String[]> propColumnReaders = new ArrayList<>();
		final ArrayList<String[]> propColumnReaderTemplates = new ArrayList<>();
		final ArrayList<FetchMode> joinedFetchesList = new ArrayList<>();

		if ( persistentClass.hasSubclasses() ) {
			for ( var selectable : persistentClass.getIdentifier().getSelectables() ) {
				if ( selectable instanceof Column column ) {
					// Identifier columns are always shared between subclasses
					sharedColumnNames.add( column.getQuotedName( dialect ) );
				}
			}
		}

		for ( var prop : persistentClass.getSubclassPropertyClosure() ) {
			names.add( prop.getName() );
			types.add( prop.getType() );

			final int columnSpan = prop.getColumnSpan();
			final String[] columnNames = new String[columnSpan];
			final String[] readers = new String[columnSpan];
			final String[] readerTemplates = new String[columnSpan];
			final String[] formulaTemplates = new String[columnSpan];

			final var selectables = prop.getSelectables();
			for ( int i = 0; i < selectables.size(); i++ ) {
				final var selectable = selectables.get(i);
				if ( selectable instanceof Formula ) {
					formulaTemplates[i] = selectable.getTemplate( dialect, typeConfiguration );
					final String formulaAlias = selectable.getAlias( dialect );
					if ( prop.isSelectable() && !formulaAliases.contains( formulaAlias ) ) {
						formulaAliases.add( formulaAlias );
					}
				}
				else if ( selectable instanceof Column column ) {
					final String quotedColumnName = column.getQuotedName( dialect );
					columnNames[i] = quotedColumnName;
					final String columnAlias = selectable.getAlias( dialect, prop.getValue().getTable() );
					if ( prop.isSelectable() && !aliases.contains( columnAlias ) ) {
						aliases.add( columnAlias );
					}
					readers[i] = column.getReadExpr( dialect );
					readerTemplates[i] = column.getTemplate( dialect, typeConfiguration );
					if ( thisClassProperties.contains( prop )
							? persistentClass.hasSubclasses()
							: persistentClass.isDefinedOnMultipleSubclasses( column ) ) {
						sharedColumnNames.add( quotedColumnName );
					}
				}
			}
			propColumns.add( columnNames );
			propColumnReaders.add( readers );
			propColumnReaderTemplates.add( readerTemplates );
			templates.add( formulaTemplates );

			joinedFetchesList.add( prop.getValue().getFetchMode() );
		}
		subclassColumnAliasClosure = toStringArray( aliases );
		subclassFormulaAliasClosure = toStringArray( formulaAliases );

		subclassPropertyNameClosure = toStringArray( names );
		subclassPropertyTypeClosure = toTypeArray( types );
		subclassPropertyFormulaTemplateClosure = to2DStringArray( templates );
		subclassPropertyColumnNameClosure = to2DStringArray( propColumns );
		subclassPropertyColumnReaderClosure = to2DStringArray( propColumnReaders );
		subclassPropertyColumnReaderTemplateClosure = to2DStringArray( propColumnReaderTemplates );

		subclassPropertyFetchModeClosure = new FetchMode[joinedFetchesList.size()];
		int j = 0;
		for ( var fetchMode : joinedFetchesList) {
			subclassPropertyFetchModeClosure[j++] = fetchMode;
		}

		useReferenceCacheEntries = shouldUseReferenceCacheEntries( factoryOptions );
		final var queryCacheLayout = persistentClass.getQueryCacheLayout();
		useShallowQueryCacheLayout =
				shouldUseShallowCacheLayout( queryCacheLayout, factoryOptions );
		storeDiscriminatorInShallowQueryCacheLayout =
				shouldStoreDiscriminatorInShallowQueryCacheLayout( queryCacheLayout, factoryOptions );
		cacheEntryHelper = buildCacheEntryHelper( factoryOptions );
		invalidateCache =
				factoryOptions.isSecondLevelCacheEnabled()
						&& canWriteToCache
						&& shouldInvalidateCache( persistentClass, creationContext );

		final List<Object> values = new ArrayList<>();
		final List<String> sqlValues = new ArrayList<>();

		if ( persistentClass.isPolymorphic() && persistentClass.getDiscriminator() != null ) {
			if ( !isAbstract() ) {
				values.add( DiscriminatorHelper.getDiscriminatorValue( persistentClass ) );
				sqlValues.add( DiscriminatorHelper.getDiscriminatorSQLValue( persistentClass, dialect ) );
			}

			final var subclasses = persistentClass.getSubclasses();
			for ( int k = 0; k < subclasses.size(); k++ ) {
				final var subclass = subclasses.get( k );
				//copy/paste from EntityMetamodel:
				if ( !isAbstract( subclass ) ) {
					values.add( DiscriminatorHelper.getDiscriminatorValue( subclass ) );
					sqlValues.add( DiscriminatorHelper.getDiscriminatorSQLValue( subclass, dialect ) );
				}
			}
		}

		fullDiscriminatorSQLValues = toStringArray( sqlValues );
		fullDiscriminatorValues = toObjectArray( values );

		if ( hasNamedQueryLoader() ) {
			getNamedQueryMemento( creationContext.getBootModel() );
		}
	}

	private static String renderSqlWhereStringTemplate(
			PersistentClass persistentClass, Dialect dialect, TypeConfiguration typeConfiguration) {
		return Template.renderWhereStringTemplate(
				"(" + persistentClass.getWhere() + ")",
				dialect,
				typeConfiguration
		);
	}

	private static PersistentClass getCountainingClass(PersistentClass persistentClass) {
		var containingClass = persistentClass;
		while ( containingClass.getSuperclass() != null ) {
			final var superclass = containingClass.getSuperclass();
			if ( !Objects.equals( persistentClass.getWhere(), superclass.getWhere() ) ) {
				break;
			}
			containingClass = superclass;
		}
		return containingClass;
	}

	private NamedQueryMemento<?> getNamedQueryMemento(MetadataImplementor bootModel) {
		final var memento =
				factory.getQueryEngine().getNamedObjectRepository()
						.resolve( factory, bootModel, queryLoaderName );
		if ( memento == null ) {
			throw new IllegalArgumentException( "Could not resolve named query '" + queryLoaderName
					+ "' for loading entity '" + getEntityName() + "'" );
		}
		return memento;
	}

	/**
	 * For Hibernate Reactive
	 */
	protected SingleIdEntityLoader<?> buildSingleIdEntityLoader() {
		if ( hasNamedQueryLoader() ) {
			// We must resolve the named query on-demand through the boot model because it isn't initialized yet
			final var memento = getNamedQueryMemento( null );
			return new SingleIdEntityLoaderProvidedQueryImpl<>( this, memento );
		}
		else {
			return buildSingleIdEntityLoader( new LoadQueryInfluencers( factory ), null );
		}
	}

	private SingleIdEntityLoader<?> buildSingleIdEntityLoader(
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions) {
		// whether we need this depends on whether EntityBatchLoader can handle locking properly
		// todo (db-locking) : determine whether this ^^ is the case
		if ( lockOptions != null && needsOneOffLoader( lockOptions ) ) {
			return new SingleIdEntityLoaderStandardImpl<>( this, loadQueryInfluencers );
		}
		else if ( loadQueryInfluencers.effectivelyBatchLoadable( this ) ) {
			final int batchSize = loadQueryInfluencers.effectiveBatchSize( this );
			return factory.getServiceRegistry().requireService( BatchLoaderFactory.class )
					.createEntityBatchLoader( batchSize, this, loadQueryInfluencers );
		}
		else {
			return new SingleIdEntityLoaderStandardImpl<>( this, loadQueryInfluencers );
		}
	}

	private boolean needsOneOffLoader(LockOptions lockOptions) {
		return lockOptions.getLockMode().isPessimistic()
			&& lockOptions.hasNonDefaultOptions();
	}

	public static Map<String, String> getEntityNameByTableNameMap(
			PersistentClass persistentClass,
			SqlStringGenerationContext stringGenerationContext) {
		final Map<String, String> entityNameByTableNameMap = new HashMap<>();
		PersistentClass superType = persistentClass.getSuperPersistentClass();
		while ( superType != null ) {
			final String entityName = superType.getEntityName();
			entityNameByTableNameMap.put( superType.getTable().getQualifiedName( stringGenerationContext ), entityName );
			for ( var join : superType.getJoins() ) {
				entityNameByTableNameMap.put( join.getTable().getQualifiedName( stringGenerationContext ), entityName );
			}
			superType = superType.getSuperPersistentClass();
		}
		for ( var subclass : persistentClass.getSubclassClosure() ) {
			final String entityName = subclass.getEntityName();
			entityNameByTableNameMap.put( subclass.getTable().getQualifiedName( stringGenerationContext ), entityName );
			for ( var join : subclass.getJoins() ) {
				entityNameByTableNameMap.put( join.getTable().getQualifiedName( stringGenerationContext ), entityName );
			}
		}
		return entityNameByTableNameMap;
	}

	/**
	 * Used by Hibernate Reactive
	 */
	protected MultiIdEntityLoader<?> buildMultiIdLoader() {
		return getIdentifierType() instanceof BasicType
			&& supportsSqlArrayType( getDialect() )
				? new MultiIdEntityLoaderArrayParam<>( this, factory )
				: new MultiIdEntityLoaderInPredicate<>( this, identifierColumnSpan, factory );
	}

	private String getIdentitySelectString(Dialect dialect) {
		try {
			final var identifierType = (BasicType<?>) getIdentifierType();
			final int idTypeCode = identifierType.getJdbcType().getDdlTypeCode();
			return dialect.getIdentityColumnSupport()
					.getIdentitySelectString( getTableName(0), getKeyColumns(0)[0], idTypeCode );
		}
		catch (MappingException ex) {
			return null;
		}
	}

	static boolean isAbstract(PersistentClass subclass) {
		final Boolean knownAbstract = subclass.isAbstract();
		return knownAbstract == null
				? subclass.hasPojoRepresentation() && isAbstractClass( subclass.getMappedClass() )
				: knownAbstract;
	}

	private boolean shouldUseReferenceCacheEntries(SessionFactoryOptions options) {
		// Check if we can use Reference Cached entities in 2lc
		// todo : should really validate that the cache access type is read-only
		if ( !options.isDirectReferenceCacheEntriesEnabled() ) {
			return false;
		}
		// for now, limit this to just entities that:
		else if ( isMutable() ) {
			// 1) are immutable
			return false;
		}
		else {
			// 2) have no associations.
			// Eventually we want to be a little more lenient with associations.
			for ( var type : getSubclassPropertyTypeClosure() ) {
				if ( type.isAnyType() || type.isCollectionType() || type.isEntityType() ) {
					return false;
				}
			}
			return true;
		}
	}

	private static CacheLayout queryCacheLayout(CacheLayout entityQueryCacheLayout, SessionFactoryOptions options) {
		return entityQueryCacheLayout == null ? options.getQueryCacheLayout() : entityQueryCacheLayout;
	}

	private boolean shouldUseShallowCacheLayout(CacheLayout entityQueryCacheLayout, SessionFactoryOptions options) {
		return switch ( queryCacheLayout( entityQueryCacheLayout, options ) ) {
			case FULL -> false;
			case AUTO -> canUseReferenceCacheEntries() || canReadFromCache();
			default -> true;
		};
	}

	private static boolean shouldStoreDiscriminatorInShallowQueryCacheLayout(
			CacheLayout entityQueryCacheLayout, SessionFactoryOptions options) {
		return queryCacheLayout( entityQueryCacheLayout, options ) == CacheLayout.SHALLOW_WITH_DISCRIMINATOR;
	}

	protected abstract String[] getSubclassTableNames();

	protected abstract String[] getSubclassTableKeyColumns(int j);

	protected abstract boolean isClassOrSuperclassTable(int j);

	protected boolean isClassOrSuperclassJoin(int j) {
		// TODO:
		// SingleTableEntityPersister incorrectly used isClassOrSuperclassJoin == isClassOrSuperclassTable,
		// this caused HHH-12895, as this resulted in the subclass tables always being joined, even if no
		// property on these tables was accessed.
		//
		// JoinedTableEntityPersister does not use isClassOrSuperclassJoin at all, probably incorrectly so.
		// I however haven't been able to reproduce any quirks regarding <join>s, secondary tables or
		// @JoinTable's.
		//
		// Probably this method needs to be properly implemented for the various entity persisters,
		// but this at least fixes the SingleTableEntityPersister, while maintaining the previous
		// behaviour for other persisters.
		return isClassOrSuperclassTable( j );
	}

	public abstract boolean isPropertyOfTable(int property, int j);

	protected abstract int[] getPropertyTableNumbers();

	private static final String DISCRIMINATOR_ALIAS = "clazz_";

	@Override
	public String getDiscriminatorColumnName() {
		return DISCRIMINATOR_ALIAS;
	}

	public String getDiscriminatorColumnReaders() {
		return DISCRIMINATOR_ALIAS;
	}

	public String getDiscriminatorColumnReaderTemplate() {
		return getSubclassEntityNames().size() == 1
				? getDiscriminatorSQLValue()
				: Template.TEMPLATE + "." + DISCRIMINATOR_ALIAS;
	}

	public String getDiscriminatorFormulaTemplate() {
		return null;
	}

	@Override
	public boolean isInverseTable(int j) {
		return false;
	}

	@Override
	public boolean isNullableTable(int j) {
		return false;
	}

	protected boolean isNullableSubclassTable(int j) {
		return false;
	}

	@Override
	public boolean isSubclassEntityName(String entityName) {
		return getSubclassEntityNames().contains( entityName );
	}

	@Override
	public boolean isSharedColumn(String columnExpression) {
		return sharedColumnNames.contains( columnExpression );
	}

	@Override
	public String[] getRootTableKeyColumnNames() {
		return rootTableKeyColumnNames;
	}

	SingleIdArrayLoadPlan getSQLLazySelectLoadPlan(String fetchGroup) {
		return lazyLoadPlanByFetchGroup.get( fetchGroup );
	}

	@Override
	public InsertCoordinator getInsertCoordinator() {
		return insertCoordinator;
	}

	@Override
	public UpdateCoordinator getUpdateCoordinator() {
		return updateCoordinator;
	}

	@Override
	public DeleteCoordinator getDeleteCoordinator() {
		return deleteCoordinator;
	}

	@Override
	public UpdateCoordinator getMergeCoordinator() {
		return mergeCoordinator;
	}

	public String getVersionSelectString() {
		return sqlVersionSelectString;
	}

	@Internal // called by Hibernate Reactive
	@SuppressWarnings("unused")
	public GeneratedValuesProcessor getInsertGeneratedValuesProcessor() {
		return insertGeneratedValuesProcessor;
	}

	@Internal // called by Hibernate Reactive
	@SuppressWarnings("unused")
	public GeneratedValuesProcessor getUpdateGeneratedValuesProcessor() {
		return updateGeneratedValuesProcessor;
	}

	@Override
	public boolean hasRowId() {
		return rowIdName != null;
	}

	@Override
	public String[] getTableNames() {
		final String[] tableNames = new String[getTableSpan()];
		for ( int i = 0; i < tableNames.length; i++ ) {
			tableNames[i] = getTableName( i );
		}
		return tableNames;
	}

	/**
	 * We might need to use cache invalidation if we have formulas,
	 * dynamic update, or secondary tables.
	 *
	 * @see #isCacheInvalidationRequired()
	 */
	private boolean shouldInvalidateCache(
			PersistentClass persistentClass,
			RuntimeModelCreationContext creationContext) {
		if ( hasFormulaProperties() ) {
			// we need to evaluate formulas in the database
			return true;
		}
		else if ( isVersioned() ) {
			// we don't need to be "careful" in the case of
			// versioned entities
			return false;
		}
		else if ( isDynamicUpdate() ) {
			// if the unversioned entity has dynamic updates
			// there is a risk of concurrent updates
			return true;
		}
		else if ( isCacheComplianceEnabled( creationContext ) ) {
			// The JPA TCK (inadvertently, but still...)
			// requires that we cache entities with secondary
			// tables instead of being more careful and just
			// invalidating them
			return false;
		}
		else {
			// if the unversioned entity has second tables
			// there is a risk of concurrent updates
			// todo : this should really consider optionality of the secondary tables
			//        in count so non-optional tables do not cause this bypass
			return persistentClass.getJoinClosureSpan() >= 1;
		}
	}

	private boolean isCacheComplianceEnabled(RuntimeModelCreationContext creationContext) {
		return creationContext.getSessionFactoryOptions()
				.getJpaCompliance()
				.isJpaCacheComplianceEnabled();
	}

	private boolean determineCanWriteToCache(PersistentClass persistentClass, EntityDataAccess cacheAccessStrategy) {
		return cacheAccessStrategy != null && persistentClass.isCached();
	}

	private boolean determineCanReadFromCache(PersistentClass persistentClass, EntityDataAccess cacheAccessStrategy) {
		if ( cacheAccessStrategy == null ) {
			return false;
		}
		else if ( persistentClass.isCached() ) {
			return true;
		}
		else {
			for ( var subclass : persistentClass.getSubclasses() ) {
				if ( subclass.isCached() ) {
					return true;
				}
			}
			return false;
		}
	}

	protected CacheEntryHelper buildCacheEntryHelper(SessionFactoryOptions options) {
		if ( cacheAccessStrategy == null ) {
			// the entity defined no caching...
			return NoopCacheEntryHelper.INSTANCE;
		}
		else if ( canUseReferenceCacheEntries() ) {
			setLazy( false );
			// todo : do we also need to unset proxy factory?
			return new ReferenceCacheEntryHelper( this );
		}
		else {
			return options.isStructuredCacheEntriesEnabled()
					? new StructuredCacheEntryHelper( this )
					: new StandardCacheEntryHelper( this );
		}
	}

	@Override
	public boolean canUseReferenceCacheEntries() {
		return useReferenceCacheEntries;
	}

	@Override
	public boolean useShallowQueryCacheLayout() {
		return useShallowQueryCacheLayout;
	}

	@Override
	public boolean storeDiscriminatorInShallowQueryCacheLayout() {
		return storeDiscriminatorInShallowQueryCacheLayout;
	}

	@Override
	public boolean hasFilterForLoadByKey() {
		if ( filterHelper != null ) {
			for ( String filterName : filterHelper.getFilterNames() ) {
				if ( factory.getFilterDefinition( filterName ).isAppliedToLoadByKey() ) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Iterable<UniqueKeyEntry> uniqueKeyEntries() {
		if ( uniqueKeyEntries == null ) {
			uniqueKeyEntries = initUniqueKeyEntries( this );
		}
		return uniqueKeyEntries;
	}

	private static List<UniqueKeyEntry> initUniqueKeyEntries(final AbstractEntityPersister persister) {
		final ArrayList<UniqueKeyEntry> uniqueKeys = new ArrayList<>();
		for ( var propertyType : persister.getPropertyTypes() ) {
			if ( propertyType instanceof AssociationType associationType ) {
				final String ukName = associationType.getLHSPropertyName();
				if ( ukName != null ) {
					final var attributeMapping = persister.findAttributeMapping( ukName );
					if ( attributeMapping != null ) {
						final int index = attributeMapping.getStateArrayPosition();
						final Type type = persister.getPropertyTypes()[index];
						uniqueKeys.add( new UniqueKeyEntry( ukName, index, type ) );
					}
				}
				else if ( associationType instanceof ManyToOneType manyToOneType
							&& manyToOneType.isLogicalOneToOne()
							&& manyToOneType.isReferenceToPrimaryKey() ) {
					final var attributeMapping = persister.findAttributeMapping( manyToOneType.getPropertyName() );
					if ( attributeMapping != null ) {
						final int index = attributeMapping.getStateArrayPosition();
						final Type type = persister.getPropertyTypes()[index];
						uniqueKeys.add( new UniqueKeyEntry( manyToOneType.getPropertyName(), index, type ) );
					}
				}
			}
		}
		return toSmallList( uniqueKeys );
	}

	protected Map<String, SingleIdArrayLoadPlan> getLazyLoadPlanByFetchGroup() {
		final var metadata = getBytecodeEnhancementMetadata();
		return metadata.isEnhancedForLazyLoading() && metadata.getLazyAttributesMetadata().hasLazyAttributes()
				? createLazyLoadPlanByFetchGroup( metadata )
				: emptyMap();
	}

	private Map<String, SingleIdArrayLoadPlan> createLazyLoadPlanByFetchGroup(BytecodeEnhancementMetadata metadata) {
		final Map<String, SingleIdArrayLoadPlan> result = new HashMap<>();
		final var attributesMetadata = metadata.getLazyAttributesMetadata();
		for ( String groupName : attributesMetadata.getFetchGroupNames() ) {
			final var plan = createLazyLoadPlan( attributesMetadata.getFetchGroupAttributeDescriptors( groupName ) );
			if ( plan != null ) {
				result.put( groupName, plan );
			}
		}
		return result;
	}

	private SingleIdArrayLoadPlan createLazyLoadPlan(List<LazyAttributeDescriptor> fetchGroupAttributeDescriptors) {
		final List<ModelPart> partsToSelect = new ArrayList<>( fetchGroupAttributeDescriptors.size() );
		for ( var lazyAttributeDescriptor : fetchGroupAttributeDescriptors ) {
			// all this only really needs to consider properties
			// of this class, not its subclasses, but since we
			// are reusing code used for sequential selects, we
			// use the subclass closure
			partsToSelect.add( getAttributeMapping( getSubclassPropertyIndex( lazyAttributeDescriptor.getName() ) ) );
		}
		return createLazyLoanPlan( partsToSelect );
	}

	private SingleIdArrayLoadPlan createLazyLoanPlan(List<ModelPart> partsToSelect) {
		if ( partsToSelect.isEmpty() ) {
			// only one-to-one is lazily fetched
			return null;
		}
		else {
			final var lockOptions = new LockOptions();
			final var jdbcParametersBuilder = JdbcParametersList.newBuilder();
			final var select = LoaderSelectBuilder.createSelect(
					this,
					partsToSelect,
					getIdentifierMapping(),
					null,
					1,
					new LoadQueryInfluencers( factory ),
					lockOptions,
					jdbcParametersBuilder::add,
					factory
			);
			return new SingleIdArrayLoadPlan(
					this,
					getIdentifierMapping(),
					select,
					jdbcParametersBuilder.build(),
					lockOptions,
					factory
			);
		}
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public boolean containsTableReference(String tableExpression) {
		return contains( getSubclassTableNames(), tableExpression );
	}

	@Override
	public String getPartName() {
		return getEntityName();
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final var entityResult = new EntityResultImpl(
				navigablePath,
				this,
				tableGroup,
				resultVariable
		);
		entityResult.afterInitialize( entityResult, creationState );
		//noinspection unchecked
		return entityResult;
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		identifierMapping.applySqlSelections(
				navigablePath.append( identifierMapping.getPartName() ),
				tableGroup,
				creationState
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		identifierMapping.applySqlSelections(
				navigablePath.append( identifierMapping.getPartName() ),
				tableGroup,
				creationState,
				selectionConsumer
		);
	}

	@Override
	public NaturalIdMapping getNaturalIdMapping() {
		return naturalIdMapping;
	}

	@Override
	public TableReference createPrimaryTableReference(
			SqlAliasBase sqlAliasBase,
			SqlAstCreationState sqlAstCreationState) {
		return new NamedTableReference( getTableName(), sqlAliasBase.generateNewAlias() );
	}

	@Override
	public TableReferenceJoin createTableReferenceJoin(
			String joinTableExpression,
			SqlAliasBase sqlAliasBase,
			TableReference lhs,
			SqlAstCreationState creationState) {
		for ( int i = 1; i < getSubclassTableSpan(); i++ ) {
			if ( getSubclassTableName( i ).equals( joinTableExpression ) ) {
				return generateTableReferenceJoin(
						lhs,
						joinTableExpression,
						sqlAliasBase,
						shouldInnerJoinSubclassTable( i, emptySet() ),
						getSubclassTableKeyColumns( i ),
						creationState
				);
			}
		}

		return null;
	}

	protected TableReferenceJoin generateTableReferenceJoin(
			TableReference lhs,
			String joinTableExpression,
			SqlAliasBase sqlAliasBase,
			boolean innerJoin,
			String[] targetColumns,
			SqlAstCreationState creationState) {
		final var joinedTableReference = new NamedTableReference(
				joinTableExpression,
				sqlAliasBase.generateNewAlias(),
				!innerJoin
		);
		return new TableReferenceJoin(
				innerJoin,
				joinedTableReference,
				generateJoinPredicate(
						lhs,
						joinedTableReference,
						getIdentifierColumnNames(),
						targetColumns,
						creationState
				)
		);
	}

	protected Predicate generateJoinPredicate(
			TableReference rootTableReference,
			TableReference joinedTableReference,
			String[] pkColumnNames,
			String[] fkColumnNames,
			SqlAstCreationState creationState) {
		final var identifierMapping = getIdentifierMapping();

		final var conjunction = new Junction( Junction.Nature.CONJUNCTION );

		assert pkColumnNames.length == fkColumnNames.length;
		assert pkColumnNames.length == identifierMapping.getJdbcTypeCount();

		identifierMapping.forEachSelectable(
				(columnIndex, selection) -> {
					final var sqlExpressionResolver = creationState.getSqlExpressionResolver();

					final String rootPkColumnName = pkColumnNames[ columnIndex ];
					final Expression pkColumnExpression = sqlExpressionResolver.resolveSqlExpression(
							createColumnReferenceKey(
									rootTableReference,
									rootPkColumnName,
									selection.getJdbcMapping()
							),
							sqlAstProcessingState -> new ColumnReference(
									rootTableReference.getIdentificationVariable(),
									rootPkColumnName,
									false,
									null,
									selection.getJdbcMapping()
							)
					);

					final String fkColumnName = fkColumnNames[ columnIndex ];
					final Expression fkColumnExpression = sqlExpressionResolver.resolveSqlExpression(
							createColumnReferenceKey(
									joinedTableReference,
									fkColumnName,
									selection.getJdbcMapping()
							),
							sqlAstProcessingState -> new ColumnReference(
									joinedTableReference.getIdentificationVariable(),
									fkColumnName,
									false,
									null,
									selection.getJdbcMapping()
							)
					);

					conjunction.add( new ComparisonPredicate( pkColumnExpression, ComparisonOperator.EQUAL, fkColumnExpression ) );
				}
		);

		return conjunction;
	}

	@Override
	public Object initializeLazyProperty(String fieldName, Object entity, SharedSessionContractImplementor session) {
		return hasCollections() && getPropertyType( fieldName ) instanceof CollectionType collectionType
				? initializedLazyCollection( fieldName, entity, collectionType, session )
				: initializedLazyField( fieldName, entity, session );
	}

	private Object initializedLazyField(
			String fieldName,
			Object entity,
			SharedSessionContractImplementor session) {
		final Object id = session.getContextEntityIdentifier( entity );
		final var entry = session.getPersistenceContext().getEntry( entity );
		if ( entry == null ) {
			throw new HibernateException( "entity is not associated with the session: " + id );
		}

		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.initializingLazyPropertiesOf(
					infoString( this, id, getFactory() ),
					fieldName
			);
		}

		// attempt to read it from second-level cache
		if ( session.getCacheMode().isGetEnabled()
				&& canReadFromCache()
				&& isLazyPropertiesCacheable() ) {
			final var cacheAccess = getCacheAccessStrategy();
			final Object cacheKey =
					cacheAccess.generateCacheKey(
							id,
							this,
							session.getFactory(),
							session.getTenantIdentifier()
					);
			final Object structuredEntry = fromSharedCache( session, cacheKey, this, cacheAccess );
			if ( structuredEntry != null ) {
				final var cacheEntry = (CacheEntry) getCacheEntryStructure().destructure( structuredEntry, factory );
				final Object initializedValue = initializeLazyPropertiesFromCache( fieldName, entity, session, entry, cacheEntry );
				if ( initializedValue != LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
					// The following should be redundant, since the setter should have set this already.
					// interceptor.attributeInitialized(fieldName);

					// NOTE EARLY EXIT!!!
					return initializedValue;
				}
			}
		}

		return initializeLazyPropertiesFromDatastore( entity, id, entry, fieldName, session );
	}

	private PersistentCollection<?> initializedLazyCollection(
			String fieldName,
			Object entity,
			CollectionType collectionType,
			SharedSessionContractImplementor session) {
		// a collection attribute is being accessed via enhancement:
		// we can circumvent all the rest and just return the PersistentCollection
		final var persister =
				factory.getMappingMetamodel()
						.getCollectionDescriptor( collectionType.getRole() );

		// Get/create the collection, and make sure it is initialized! This initialized part is
		// different from proxy-based scenarios where we have to create the PersistentCollection
		// reference "ahead of time" to add as a reference to the proxy. For bytecode solutions
		// we are not creating the PersistentCollection ahead of time, but instead we are creating
		// it on first request through the enhanced entity.

		// see if there is already a collection instance associated with the session
		// NOTE: can this ever happen?
		var collection = getCollection( entity, collectionType, session, persister );

		final var interceptor = asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
		assert interceptor != null : "Expecting bytecode interceptor to be non-null";
		interceptor.attributeInitialized( fieldName );

		final var persistenceContext = session.getPersistenceContextInternal();
		if ( collectionType.isArrayType() ) {
			persistenceContext.addCollectionHolder( collection );
		}
		// update the "state" of the owning entity's EntityEntry to overwrite the
		// UNFETCHED_PROPERTY for the collection to the just-loaded collection
		final var ownerEntry = persistenceContext.getEntry( entity );
		if ( ownerEntry == null ) {
			// the entity is not in the session; it was probably deleted,
			// so we cannot load the collection anymore.
			throw new LazyInitializationException(
					"Could not locate EntityEntry for the collection owner in the PersistenceContext"
			);
		}
		ownerEntry.overwriteLoadedStateCollectionValue( fieldName, collection );

		return collection;
	}

	private static PersistentCollection<?> getCollection(
			Object entity,
			CollectionType collectionType,
			SharedSessionContractImplementor session,
			CollectionPersister persister) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final var entry = persistenceContext.getEntry( entity );
		final Object key = getCollectionKey( persister, entity, entry, session );
		assert key != null;
		final var collection = persistenceContext.getCollection( new CollectionKey( persister, key ) );
		if ( collection == null ) {
			final var newCollection = collectionType.instantiate( session, persister, key );
			newCollection.setOwner( entity );
			persistenceContext.addUninitializedCollection( persister, newCollection, key );
			return newCollection;
		}
		else {
			return collection;
		}
	}

	public @Nullable static Object getCollectionKey(
			CollectionPersister persister,
			Object owner,
			EntityEntry ownerEntry,
			SharedSessionContractImplementor session) {
		final var collectionType = persister.getCollectionType();
		if ( ownerEntry != null ) {
			// this call only works when the owner is associated with the Session, which is not always the case
			return collectionType.getKeyOfOwner( owner, session );
		}
		else {
			final var ownerPersister = persister.getOwnerEntityPersister();
			return collectionType.getLHSPropertyName() == null
					// collection key is defined by the owning entity identifier
					? ownerPersister.getIdentifier( owner, session )
					: ownerPersister.getPropertyValue( owner, collectionType.getLHSPropertyName() );
		}
	}

	protected Object initializeLazyPropertiesFromDatastore(
			final Object entity,
			final Object id,
			final EntityEntry entry,
			final String fieldName,
			final SharedSessionContractImplementor session) {
		return isNonLazyPropertyName( fieldName )
				? initLazyProperty( entity, id, entry, fieldName, session )
				: initLazyProperties( entity, id, entry, fieldName, session );
	}

	// Hibernate Reactive uses this
	protected boolean isNonLazyPropertyName(String fieldName) {
		return nonLazyPropertyNames.contains( fieldName );
	}

	private Object initLazyProperties(
			Object entity,
			Object id,
			EntityEntry entry,
			String fieldName,
			SharedSessionContractImplementor session) {

		assert hasLazyProperties();
		CORE_LOGGER.initializingLazyPropertiesFromDatastore( fieldName );

		final var interceptor = asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
		assert interceptor != null : "Expecting bytecode interceptor to be non-null";

		final var lazyAttributesMetadata = getBytecodeEnhancementMetadata().getLazyAttributesMetadata();
		final String fetchGroup = lazyAttributesMetadata.getFetchGroupName( fieldName );
		final var fetchGroupAttributeDescriptors =
				lazyAttributesMetadata.getFetchGroupAttributeDescriptors( fetchGroup );
		final var lazySelectLoadPlan = getSQLLazySelectLoadPlan( fetchGroup );
		try {
			Object finalResult = null;
			final Object[] results = lazySelectLoadPlan.load( id, session );
			final Set<String> initializedLazyAttributeNames = interceptor.getInitializedLazyAttributeNames();
			int i = 0;
			for ( var fetchGroupAttributeDescriptor : fetchGroupAttributeDescriptors ) {
				final String attributeName = fetchGroupAttributeDescriptor.getName();
				if ( fieldName.equals( attributeName ) ) {
					finalResult = results[i];
				}
				if ( !initializedLazyAttributeNames.contains( attributeName ) ) {
					initializeLazyProperty(
							entity,
							entry,
							results[i],
							getPropertyIndex( attributeName ),
							fetchGroupAttributeDescriptor.getType()
					);
				}
				// if the attribute has already been initialized (e.g. by a write) we don't want to overwrite
				i++;
				// TODO: we should consider un-marking an attribute as dirty based on the selected value
				// - we know the current value:
				//   getPropertyValue( entity, fetchGroupAttributeDescriptor.getAttributeIndex() );
				// - we know the selected value (see selectedValue below)
				// - we can use the attribute Type to tell us if they are the same
				// - assuming entity is a SelfDirtinessTracker we can also know if the attribute is currently
				//   considered dirty, and if really not dirty we would do the un-marking
				// - of course that would mean a new method on SelfDirtinessTracker to allow un-marking
			}
			CORE_LOGGER.doneInitializingLazyProperties();
			return finalResult;
		}
		catch (JDBCException ex) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					ex.getSQLException(),
					"could not initialize lazy properties: "
							+ infoString( this, id, getFactory() ),
					ex.getSQL()
			);
		}
	}

	private Object initLazyProperty(
			Object entity,
			Object id,
			EntityEntry entry,
			String fieldName,
			SharedSessionContractImplementor session) {
		// An eager property can be lazy because of an applied EntityGraph
		final int propertyIndex = getPropertyIndex( fieldName );
		final List<ModelPart> partsToSelect = List.of( getAttributeMapping( propertyIndex ) );
		final var lazyLoanPlan = getOrCreateLazyLoadPlan( fieldName, partsToSelect );
		try {
			final Object[] results = lazyLoanPlan.load( id, session );
			final Object result = results[0];
			initializeLazyProperty( entity, entry, result, propertyIndex, getPropertyTypes()[propertyIndex] );
			return result;
		}
		catch (JDBCException ex) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					ex.getSQLException(),
					"could not initialize lazy properties: "
							+ infoString( this, id, getFactory() ),
					ex.getSQL()
			);
		}
	}

	private SingleIdArrayLoadPlan getOrCreateLazyLoadPlan(String fieldName, List<ModelPart> partsToSelect) {
		final var plans = nonLazyPropertyLoadPlansByName;
		if ( plans == null ) {
			nonLazyPropertyLoadPlansByName = new ConcurrentHashMap<>();
		}
		else {
			final var lazyLoanPlan = plans.get( fieldName );
			if ( lazyLoanPlan != null ) {
				return lazyLoanPlan;
			}
		}
		final var newLazyLoanPlan = createLazyLoanPlan( partsToSelect );
		nonLazyPropertyLoadPlansByName.put( fieldName, newLazyLoanPlan );
		return newLazyLoanPlan;
	}

	protected Object initializeLazyPropertiesFromCache(
			final String fieldName,
			final Object entity,
			final SharedSessionContractImplementor session,
			final EntityEntry entry,
			final CacheEntry cacheEntry) {
		CORE_LOGGER.initializingLazyPropertiesFromSecondLevelCache();
		Object result = null;
		final var disassembledValues = cacheEntry.getDisassembledState();
		for ( int j = 0; j < lazyPropertyNames.length; j++ ) {
			final var cachedValue = disassembledValues[lazyPropertyNumbers[j]];
			if ( cachedValue == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				if ( fieldName.equals( lazyPropertyNames[j] ) ) {
					result = LazyPropertyInitializer.UNFETCHED_PROPERTY;
				}
				// don't try to initialize the unfetched property
			}
			else {
				final Object propValue = lazyPropertyTypes[j].assemble( cachedValue, session, entity );
				if ( initializeLazyProperty( fieldName, entity, entry, j, propValue ) ) {
					result = propValue;
				}
			}
		}
		CORE_LOGGER.doneInitializingLazyProperties();
		return result;
	}

	/**
	 * Called by Hibernate Reactive
	 */
	protected boolean initializeLazyProperty(
			final String fieldName,
			final Object entity,
			final EntityEntry entry,
			final int index,
			final Object propValue) {
		final int propertyNumber = lazyPropertyNumbers[index];
		setPropertyValue( entity, propertyNumber, propValue );
		if ( entry.getMaybeLazySet() != null ) {
			var bitSet = entry.getMaybeLazySet().toBitSet();
			bitSet.set( propertyNumber );
			entry.setMaybeLazySet( ImmutableBitSet.valueOf( bitSet ) );
		}
		final var loadedState = entry.getLoadedState();
		if ( loadedState != null ) {
			// object have been loaded with setReadOnly(true); HHH-2236
			loadedState[propertyNumber] = copiedLazyPropertyValue( index, propValue );
		}
		// If the entity has deleted state, then update that as well
		final var deletedState = entry.getDeletedState();
		if ( deletedState != null ) {
			deletedState[propertyNumber] = copiedLazyPropertyValue( index, propValue );
		}
		return fieldName.equals( lazyPropertyNames[index] );
	}

	private Object copiedLazyPropertyValue(int index, Object propValue) {
		return lazyPropertyTypes[index].deepCopy( propValue, factory );
	}

	/**
	 * Used by Hibernate Reactive
	 * @deprecated
	 */
	@Deprecated(since = "7.2", forRemoval = true)
	protected boolean initializeLazyProperty(
			final String fieldName,
			final Object entity,
			final EntityEntry entry,
			final LazyAttributeDescriptor fetchGroupAttributeDescriptor,
			final Object propValue) {
		final String name = fetchGroupAttributeDescriptor.getName();
		initializeLazyProperty( entity, entry, propValue,
				getPropertyIndex( name ),
				fetchGroupAttributeDescriptor.getType() );
		return fieldName.equals( name );
	}

	// Used by Hibernate Reactive
	protected void initializeLazyProperty(Object entity, EntityEntry entry, Object propValue, int index, Type type) {
		setPropertyValue( entity, index, propValue );
		if ( entry.getMaybeLazySet() != null ) {
			var bitSet = entry.getMaybeLazySet().toBitSet();
			bitSet.set( index );
			entry.setMaybeLazySet( ImmutableBitSet.valueOf( bitSet ) );
		}
		final var loadedState = entry.getLoadedState();
		if ( loadedState != null ) {
			// object has been loaded with setReadOnly(true); HHH-2236
			loadedState[index] = type.deepCopy( propValue, factory );
		}
		// If the entity has deleted state, then update that as well
		final var deletedState = entry.getDeletedState();
		if ( deletedState != null ) {
			deletedState[index] = type.deepCopy( propValue, factory );
		}
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public Serializable[] getQuerySpaces() {
		return getPropertySpaces();
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public String[] getIdentifierColumnNames() {
		return rootTableKeyColumnNames;
	}

	public String[] getIdentifierColumnReaders() {
		return rootTableKeyColumnReaders;
	}

	public String[] getIdentifierColumnReaderTemplates() {
		return rootTableKeyColumnReaderTemplates;
	}

	public int getIdentifierColumnSpan() {
		return identifierColumnSpan;
	}

	public String[] getIdentifierAliases() {
		return identifierAliases;
	}

	@Override
	public String getVersionColumnName() {
		return versionColumnName;
	}

	public String getVersionedTableName() {
		return getTableName( 0 );
	}

	/**
	 * We can't immediately add to the cache if we have formulas
	 * which must be evaluated, or if we have the possibility of
	 * two concurrent updates to the same item being merged on
	 * the database. This second case can happen if:
	 * <ol>
	 * <li> the item is not versioned, and either
	 * <li>we have dynamic update enabled, or
	 * <li>the state of the item spans multiple tables.
	 * </ol>
	 * Therefore, we're careful, and just invalidate the cache in
	 * these cases (the item will be readded when it's read again
	 * fresh from the database).
	 */
	@Override
	public boolean isCacheInvalidationRequired() {
		return invalidateCache;
	}

	@Override
	public boolean isLazyPropertiesCacheable() {
		return isLazyPropertiesCacheable;
	}

	@Override
	public String selectFragment(String alias, String suffix) {
		final var rootQuerySpec = new QuerySpec( true );
		final var sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				new LockOptions(),
				this::fetchProcessor,
				true,
				new LoadQueryInfluencers( factory ),
				factory.getSqlTranslationEngine()
		);

		final var entityPath = new NavigablePath( getRootPathName() );
		final var rootTableGroup = createRootTableGroup(
				true,
				entityPath,
				null,
				new SqlAliasBaseConstant( alias ),
				() -> p -> {},
				sqlAstCreationState
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( entityPath, rootTableGroup );

		createDomainResult( entityPath, rootTableGroup, null, sqlAstCreationState );

		// Wrap expressions with aliases
		final var sqlSelections = rootQuerySpec.getSelectClause().getSqlSelections();
		final Set<String> processedExpressions = new HashSet<>( sqlSelections.size() );
		int i = 0;
		final int identifierSelectionSize = identifierMapping.getJdbcTypeCount();
		for ( int j = 0; j < identifierSelectionSize; j++ ) {
			final var selectableMapping = identifierMapping.getSelectable( j );
			if ( processedExpressions.add( selectableMapping.getSelectionExpression() ) ) {
				aliasSelection( sqlSelections, i, identifierAliases[j] + suffix );
				i++;
			}
		}

		if ( hasSubclasses() ) {
			assert discriminatorMapping.getJdbcTypeCount() == 1;
			final var selectableMapping = discriminatorMapping.getSelectable( 0 );
			if ( processedExpressions.add( selectableMapping.getSelectionExpression() ) ) {
				aliasSelection( sqlSelections, i, getDiscriminatorAlias() + suffix );
				i++;
			}
		}

		if ( hasRowId() ) {
			if ( processedExpressions.add( rowIdMapping.getSelectionExpression() ) ) {
				aliasSelection( sqlSelections, i, ROWID_ALIAS + suffix );
				i++;
			}
		}

		final String[] columnAliases = getSubclassColumnAliasClosure();
		final String[] formulaAliases = getSubclassFormulaAliasClosure();
		int columnIndex = 0;
		int formulaIndex = 0;
		final int size = getNumberOfFetchables();
		// getSubclassColumnAliasClosure contains the _identifierMapper columns when it has an id class,
		// which need to be skipped
		if ( identifierMapping instanceof NonAggregatedIdentifierMapping nonAggregatedIdentifierMapping
				&& nonAggregatedIdentifierMapping.getIdClassEmbeddable() != null ) {
			columnIndex = identifierSelectionSize;
		}
		for ( int j = 0; j < size; j++ ) {
			final var fetchable = getFetchable( j );
			if ( !(fetchable instanceof PluralAttributeMapping)
					&& !skipFetchable( fetchable, fetchable.getMappedFetchOptions().getTiming() )
					&& fetchable.isSelectable() ) {
				final int jdbcTypeCount = fetchable.getJdbcTypeCount();
				for ( int k = 0; k < jdbcTypeCount; k++ ) {
					final var selectableMapping = fetchable.getSelectable( k );
					if ( processedExpressions.add( selectableMapping.getSelectionExpression() ) ) {
						final String baseAlias = selectableMapping.isFormula()
								? formulaAliases[formulaIndex++]
								: columnAliases[columnIndex++];
						aliasSelection( sqlSelections, i, baseAlias + suffix );
						i++;
					}
				}
			}
		}

		final String sql =
				getDialect().getSqlAstTranslatorFactory()
						.buildSelectTranslator( getFactory(), new SelectStatement( rootQuerySpec ) )
						.translate( null, QueryOptions.NONE )
						.getSqlString();
		final int fromIndex = sql.lastIndexOf( " from" );
		return fromIndex != -1
				? sql.substring( "select ".length(), fromIndex )
				: sql.substring( "select ".length() );
	}

	private static void aliasSelection(
			List<SqlSelection> sqlSelections,
			int selectionIndex,
			String alias) {
		final var expression = sqlSelections.get( selectionIndex ).getExpression();
		sqlSelections.set( selectionIndex,
				new SqlSelectionImpl( selectionIndex, new AliasedExpression( expression, alias ) ) );
	}

	private ImmutableFetchList fetchProcessor(FetchParent fetchParent, LoaderSqlAstCreationState creationState) {
		final var fetchableContainer = fetchParent.getReferencedMappingContainer();
		final int size = fetchableContainer.getNumberOfFetchables();
		final var fetches = new ImmutableFetchList.Builder( fetchableContainer );
		for ( int i = 0; i < size; i++ ) {
			final var fetchable = fetchableContainer.getFetchable( i );
			// Ignore plural attributes
			if ( !( fetchable instanceof PluralAttributeMapping ) ) {
				final var fetchTiming = fetchable.getMappedFetchOptions().getTiming();
				if ( !skipFetchable( fetchable, fetchTiming ) ) {
					if ( fetchTiming == null ) {
						throw new AssertionFailure( "fetchTiming was null" );
					}
					if ( fetchable.isSelectable() ) {
						final var fetch = fetchParent.generateFetchableFetch(
								fetchable,
								fetchParent.resolveNavigablePath( fetchable ),
								fetchTiming,
								false,
								null,
								creationState
						);
						fetches.add( fetch );
					}
				}
			}
		}
		return fetches.build();
	}

	private boolean skipFetchable(Fetchable fetchable, FetchTiming fetchTiming) {
		if ( fetchable.asBasicValuedModelPart() != null ) {
			// Ignore lazy basic columns
			return fetchTiming == FetchTiming.DELAYED;
		}
		else if ( fetchable instanceof Association association ) {
			// Ignore the fetchable if the FK is on the other side
			return association.getSideNature() == ForeignKeyDescriptor.Nature.TARGET
				// Ensure the FK comes from the root table
				|| !getRootTableName().equals( association.getForeignKeyDescriptor().getKeyTable() );
		}
		else {
			return false;
		}
	}

	@Override
	public String[] getIdentifierAliases(String suffix) {
		// NOTE: this assumes something about how propertySelectFragment is implemented by the subclass!
		// was toUnquotedAliasStrings( getIdentifierColumnNames() ) before - now tried
		// to remove that unquoting and missing aliases
		return new Alias( suffix ).toAliasStrings( getIdentifierAliases() );
	}

	@Override
	public String[] getPropertyAliases(String suffix, int i) {
		// NOTE: this assumes something about how propertySelectFragment is implemented by the subclass!
		return new Alias( suffix ).toUnquotedAliasStrings( propertyColumnAliases[i] );
	}

	@Override
	public String getDiscriminatorAlias(String suffix) {
		// NOTE: this assumes something about how propertySelectFragment is implemented by the subclass!
		// toUnquotedAliasStrings( getDiscriminatorColumnName() ) before - now tried
		// to remove that unquoting and missing aliases
		return hasSubclasses()
				? new Alias( suffix ).toAliasString( getDiscriminatorAlias() )
				: null;
	}

	@Override
	public Object[] getDatabaseSnapshot(Object id, SharedSessionContractImplementor session) throws HibernateException {
		return singleIdLoader.loadDatabaseSnapshot( id, session );
	}

	@Override
	public Object getIdByUniqueKey(Object key, String uniquePropertyName, SharedSessionContractImplementor session) {
		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.resolvingUniqueKeyToIdentifier( key, getEntityName() );
		}

		return getUniqueKeyLoader( uniquePropertyName, session ).resolveId( key, session );
	}


	/**
	 * Generate the SQL that selects the version number by id
	 */
	public String generateSelectVersionString() {
		final var select = new SimpleSelect( getFactory() ).setTableName( getVersionedTableName() );
		if ( isVersioned() ) {
			select.addColumn( getVersionColumnName(), VERSION_COLUMN_ALIAS );
		}
		else {
			select.addColumns( rootTableKeyColumnNames );
		}
		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			select.setComment( "get version " + getEntityName() );
		}
		return select.addRestriction( rootTableKeyColumnNames ).toStatementString();
	}

	protected GeneratedValuesProcessor createGeneratedValuesProcessor(
			EventType timing,
			List<AttributeMapping> generatedAttributes) {
		return new GeneratedValuesProcessor( this, generatedAttributes, timing, getFactory() );
	}

	@Override
	public Object forceVersionIncrement(Object id, Object currentVersion, SharedSessionContractImplementor session) {
		assert getMappedTableDetails().getTableName().equals( getVersionedTableName() );
		final Object nextVersion = calculateNextVersion( id, currentVersion, session );
		updateCoordinator.forceVersionIncrement( id, currentVersion, nextVersion, session );
		return nextVersion;
	}

	@Override
	public Object forceVersionIncrement(
			Object id,
			Object currentVersion,
			boolean batching,
			SharedSessionContractImplementor session)
					throws HibernateException {
		assert getMappedTableDetails().getTableName().equals( getVersionedTableName() );
		final Object nextVersion = calculateNextVersion( id, currentVersion, session );
		updateCoordinator.forceVersionIncrement( id, currentVersion, nextVersion, batching, session );
		return nextVersion;
	}

	private Object calculateNextVersion(Object id, Object currentVersion, SharedSessionContractImplementor session) {
		assert isVersioned();
		final Object nextVersion =
				generatorForForceIncrement()
						// TODO: pass in owner entity
						.generate( session, null, currentVersion, FORCE_INCREMENT );
		if ( CORE_LOGGER.isTraceEnabled() ) {
			final var versionType = getVersionType();
			CORE_LOGGER.forcingVersionIncrement(
					"[" + infoString( this, id, factory ) + "; "
					+ versionType.toLoggableString( currentVersion, factory ) + " -> "
					+ versionType.toLoggableString( nextVersion, factory ) + "]"
			);
		}
		return nextVersion;
	}

	private BeforeExecutionGenerator generatorForForceIncrement() {
		if ( versionPropertyGenerator() instanceof BeforeExecutionGenerator generator
				&& generator.generatesOnForceIncrement() ) {
			// Special case to accommodate the fact that we don't yet
			// allow OnExecutionGenerators with force-increment locking.
			// When possible, falls back to treating the generator as a
			// BeforeExecutionGenerator. In particular, this works for
			// CurrentTimestampGeneration. But it requires an additional
			// request to the database to generate the timestamp. This
			// solution is neither particularly elegant nor efficient.
			return generator;
		}
		else if ( isVersionGeneratedOnExecution() ) {
			// TODO: Ideally, we would allow this case, producing an
			//       UPDATE statement which sets the version column.
			//       Then we could remove the previous special case.
			throw new HibernateException( "Force-increment lock not supported for '@Version' property with OnExecutionGenerator" );
		}
		else {
			final var generator = getVersionGenerator();
			if ( !generator.generatesOnForceIncrement() ) {
				throw new HibernateException( "Force-increment lock not supported for '@Version' generator" );
			}
			return generator;
		}
	}

	/**
	 * Retrieve the version number
	 */
	@Override
	public Object getCurrentVersion(Object id, SharedSessionContractImplementor session) throws HibernateException {

		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.readingEntityVersion( infoString( this, id, getFactory() ) );
		}
		final String versionSelectString = getVersionSelectString();
		try {
			final var jdbcCoordinator = session.getJdbcCoordinator();
			final var statement = jdbcCoordinator.getStatementPreparer().prepareStatement( versionSelectString );
			final var resourceRegistry = jdbcCoordinator.getLogicalConnection().getResourceRegistry();
			try {
				getIdentifierType().nullSafeSet( statement, id, 1, session );
				final var resultSet = jdbcCoordinator.getResultSetReturn().extract( statement, versionSelectString );
				try {
					if ( !resultSet.next() ) {
						return null;
					}
					else if ( !isVersioned() ) {
						return this;
					}
					else {
						return getVersionMapping().getJdbcMapping().getJdbcValueExtractor()
								.extract( resultSet, 1, session );
					}
				}
				finally {
					resourceRegistry.release( resultSet, statement );
				}
			}
			finally {
				resourceRegistry.release( statement );
				jdbcCoordinator.afterStatementExecution();
			}
		}
		catch ( SQLException e ) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"could not retrieve version: " + infoString( this, id, getFactory() ),
					versionSelectString
			);
		}
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		return getIdentifierMapping().getJdbcMapping( index );
	}

	protected LockingStrategy generateLocker(LockMode lockMode, Locking.Scope lockScope) {
		return getDialect().getLockingStrategy( this, lockMode, lockScope );
	}

	// Used by Hibernate Reactive
	protected LockingStrategy getLocker(LockMode lockMode, Locking.Scope lockScope) {
		return lockScope != Locking.Scope.ROOT_ONLY
				// be sure to not use the cached form if any form of extended locking is requested
				? generateLocker( lockMode, lockScope )
				: lockers.computeIfAbsent( lockMode, (l) -> generateLocker( lockMode, lockScope ) );
	}

	@Override
	public void lock(
			Object id,
			Object version,
			Object object,
			LockMode lockMode,
			SharedSessionContractImplementor session)
					throws HibernateException {
		getLocker( lockMode, Locking.Scope.ROOT_ONLY )
				.lock( id, version, object, Timeouts.WAIT_FOREVER, session );
	}

	@Override
	public void lock(Object id, Object version, Object object, LockMode lockMode, EventSource session) {
		lock( id, version, object, lockMode, (SharedSessionContractImplementor) session );
	}

	@Override
	public void lock(
			Object id,
			Object version,
			Object object,
			LockOptions lockOptions,
			SharedSessionContractImplementor session)
					throws HibernateException {
		getLocker( lockOptions.getLockMode(), lockOptions.getScope() )
				.lock( id, version, object, lockOptions.getTimeout(), session );
	}

	@Override
	public void lock(Object id, Object version, Object object, LockOptions lockOptions, EventSource session) {
		lock( id, version, object, lockOptions, (SharedSessionContractImplementor) session );
	}

	@Override
	public String getRootTableName() {
		return getSubclassTableName( 0 );
	}

	@Override
	public String[] getRootTableIdentifierColumnNames() {
		return getRootTableKeyColumnNames();
	}

	/**
	 * {@inheritDoc}
	 *
	 * Warning:
	 * When there are duplicated property names in the subclasses
	 * then this method may return the wrong results.
	 * To ensure correct results, this method should only be used when
	 * {@literal this} is the concrete EntityPersister (since the
	 * concrete EntityPersister cannot have duplicated property names).
	 */
	@Override
	public String[] toColumns(String propertyName) throws QueryException {
		return propertyMapping.getColumnNames( propertyName );
	}

	/**
	 * {@inheritDoc}
	 *
	 * Warning:
	 * When there are duplicated property names in the subclasses
	 * then this method may return the wrong results.
	 * To ensure correct results, this method should only be used when
	 * {@literal this} is the concrete EntityPersister (since the
	 * concrete EntityPersister cannot have duplicated property names).
	 */
	@Override
	public String[] getPropertyColumnNames(String propertyName) {
		return propertyMapping.getColumnNames( propertyName );
	}

	private DiscriminatorType<?> discriminatorDomainType;

	@Override
	public DiscriminatorType<?> getDiscriminatorDomainType() {
		if ( discriminatorDomainType == null ) {
			discriminatorDomainType = buildDiscriminatorType();
		}
		return discriminatorDomainType;
	}

	private DiscriminatorType<?> buildDiscriminatorType() {
		final var discriminatorBasicType = getDiscriminatorType();
		return discriminatorBasicType == null
				? null
				: new DiscriminatorTypeImpl<>(
						discriminatorBasicType,
						new UnifiedAnyDiscriminatorConverter<>(
								getNavigableRole()
										.append( DISCRIMINATOR_ROLE_NAME ),
								factory.getTypeConfiguration().getJavaTypeRegistry()
										.resolveDescriptor( discriminatedType() ),
								discriminatorBasicType.getRelationalJavaType(),
								getSubclassByDiscriminatorValue(),
								null,
								factory.getMappingMetamodel()
						)
				);
	}

	private Class<?> discriminatedType() {
		return representationStrategy.getMode() == POJO
			&& getEntityName().equals( getJavaType().getJavaTypeClass().getName() )
				? Class.class
				: String.class;
	}

	public static String generateTableAlias(String rootAlias, int tableNumber) {
		if ( tableNumber == 0 ) {
			return rootAlias;
		}
		else {
			final var alias = new StringBuilder().append( rootAlias );
			if ( !rootAlias.endsWith( "_" ) ) {
				alias.append( '_' );
			}
			return alias.append( tableNumber ).append( '_' ).toString();
		}
	}

	private int getSubclassPropertyIndex(String propertyName) {
		return indexOf( subclassPropertyNameClosure, propertyName );
	}

	public String[] getPropertyColumnNames(int i) {
		return propertyColumnNames[i];
	}

	public boolean hasFormulaProperties() {
		return hasFormulaProperties;
	}

	public FetchMode getFetchMode(int i) {
		return subclassPropertyFetchModeClosure[i];
	}

	public Type getSubclassPropertyType(int i) {
		return subclassPropertyTypeClosure[i];
	}

	@Override
	public int countSubclassProperties() {
		return subclassPropertyTypeClosure.length;
	}

	@Override
	public String[] getSubclassPropertyColumnNames(int i) {
		return subclassPropertyColumnNameClosure[i];
	}

	public String[][] getSubclassPropertyFormulaTemplateClosure() {
		return subclassPropertyFormulaTemplateClosure;
	}

	protected Type[] getSubclassPropertyTypeClosure() {
		return subclassPropertyTypeClosure;
	}

	protected String[][] getSubclassPropertyColumnNameClosure() {
		return subclassPropertyColumnNameClosure;
	}

	public String[][] getSubclassPropertyColumnReaderClosure() {
		return subclassPropertyColumnReaderClosure;
	}

	public String[][] getSubclassPropertyColumnReaderTemplateClosure() {
		return subclassPropertyColumnReaderTemplateClosure;
	}

	protected String[] getSubclassPropertyNameClosure() {
		return subclassPropertyNameClosure;
	}

	private static boolean isPrefix(final AttributeMapping attributeMapping, final String currentAttributeName) {
		final String attributeName = attributeMapping.getAttributeName();
		final int nameLength = attributeName.length();
		return currentAttributeName.startsWith( attributeName )
			&& ( currentAttributeName.length() == nameLength || currentAttributeName.charAt(nameLength) == '.' );
	}

	@Override
	public int[] resolveAttributeIndexes(String[] attributeNames) {
		if ( attributeNames == null || attributeNames.length == 0 ) {
			return EMPTY_INT_ARRAY;
		}
		final List<Integer> fields = new ArrayList<>( attributeNames.length );

		// Sort attribute names so that we can traverse mappings efficiently
		Arrays.sort( attributeNames );

		int index = 0;
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final var attributeMapping = attributeMappings.get( i );
			if ( isPrefix( attributeMapping, attributeNames[index] ) ) {
				fields.add( attributeMapping.getStateArrayPosition() );
				index++;
				if ( index < attributeNames.length ) {
					// Skip duplicates
					do {
						if ( attributeNames[index].equals( attributeMapping.getAttributeName() ) ) {
							index++;
						}
						else {
							break;
						}
					} while ( index < attributeNames.length );
				}
				else {
					break;
				}
			}
		}

		return toIntArray( fields );
	}

	@Override
	public int[] resolveDirtyAttributeIndexes(
			final Object[] currentState,
			final Object[] previousState,
			final String[] attributeNames,
			final SessionImplementor session) {
		final var mutablePropertiesIndexes = getMutablePropertiesIndexes();
		final int estimatedSize =
				attributeNames == null
						? 0
						: attributeNames.length + mutablePropertiesIndexes.cardinality();
		final List<Integer> fields = new ArrayList<>( estimatedSize );
		if ( estimatedSize == 0 ) {
			return EMPTY_INT_ARRAY;
		}
		if ( !mutablePropertiesIndexes.isEmpty() ) {
			// We have to check the state for "mutable" properties as dirty tracking isn't aware of mutable types
			final Type[] propertyTypes = getPropertyTypes();
			final boolean[] propertyCheckability = getPropertyCheckability();
			for ( int i = mutablePropertiesIndexes.nextSetBit(0); i >= 0;
					i = mutablePropertiesIndexes.nextSetBit(i + 1) ) {
				// This is kindly borrowed from org.hibernate.type.TypeHelper.findDirty
				if ( isDirty( currentState, previousState, propertyTypes, propertyCheckability, i, session ) ) {
					fields.add( i );
				}
			}
		}

		if ( attributeNames.length != 0 ) {
			final boolean[] propertyUpdateability = getPropertyUpdateability();
			if ( superMappingType == null ) {
				/*
						Sort attribute names so that we can traverse mappings efficiently
						we cannot do this when there is a supertype because given:

						class SuperEntity {
							private String bSuper;
							private String aSuper;
						}

						class ChildEntity extends SuperEntity {
							private String aChild;
							private String bChild;
						}

						`attributeMappings` contains { aSuper, bSuper, aChild, bChild	}
						while the sorted `attributeNames` { aChild, aSuper, bChild, bSuper }
				 */

				Arrays.sort( attributeNames );
				int index = 0;
				for ( int i = 0; i < attributeMappings.size(); i++ ) {
					final var attributeMapping = attributeMappings.get( i );
					final String attributeName = attributeMapping.getAttributeName();
					if ( isPrefix( attributeMapping, attributeNames[index] ) ) {
						final int position = attributeMapping.getStateArrayPosition();
						if ( propertyUpdateability[position] && !fields.contains( position ) ) {
							fields.add( position );
						}
						index++;
						if ( index < attributeNames.length ) {
							// Skip duplicates
							do {
								if ( attributeNames[index].equals( attributeName ) ) {
									index++;
								}
								else {
									break;
								}
							} while ( index < attributeNames.length );
						}
						else {
							break;
						}
					}
				}
			}
			else {
				for ( String attributeName : attributeNames ) {
					final Integer index = getPropertyIndexOrNull( attributeName );
					if ( index != null && propertyUpdateability[index] && !fields.contains( index ) ) {
						fields.add( index );
					}
				}
			}
		}

		return toIntArray( fields );
	}

	private boolean isDirty(
			Object[] currentState,
			Object[] previousState,
			Type[] propertyTypes,
			boolean[] propertyCheckability,
			int i,
			SessionImplementor session) {
		return currentState[i] != LazyPropertyInitializer.UNFETCHED_PROPERTY
				// Consider mutable properties as dirty if we don't have a previous state
				&& ( previousState == null
						|| previousState[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
						|| propertyCheckability[i]
								&& propertyTypes[i].isDirty(
										previousState[i],
										currentState[i],
										propertyColumnUpdateable[i],
										session
								)
				);
	}

	@Override
	public Object loadByUniqueKey(
			String propertyName,
			Object uniqueKey,
			SharedSessionContractImplementor session) throws HibernateException {
		return loadByUniqueKey( propertyName, uniqueKey, null, session );
	}

	public Object loadByUniqueKey(
			String propertyName,
			Object uniqueKey,
			Boolean readOnly,
			SharedSessionContractImplementor session) throws HibernateException {
		return getUniqueKeyLoader( propertyName, session ).load( uniqueKey, new LockOptions(), readOnly, session );
	}

	private Map<SingularAttributeMapping, SingleUniqueKeyEntityLoader<?>> uniqueKeyLoadersNew;

	protected SingleUniqueKeyEntityLoader<?> getUniqueKeyLoader(String attributeName, SharedSessionContractImplementor session) {
		final var attribute = (SingularAttributeMapping) findByPath( attributeName );
		final var influencers = session.getLoadQueryInfluencers();
		// no subselect fetching for entities for now
		if ( isAffectedByInfluencers( influencers, true ) ) {
			return new SingleUniqueKeyEntityLoaderStandard<>( this, attribute, influencers );
		}
		final SingleUniqueKeyEntityLoader<?> existing;
		if ( uniqueKeyLoadersNew == null ) {
			uniqueKeyLoadersNew = new ConcurrentHashMap<>();
			existing = null;
		}
		else {
			existing = uniqueKeyLoadersNew.get( attribute );
		}

		if ( existing != null ) {
			return existing;
		}
		else {
			final SingleUniqueKeyEntityLoader<?> loader =
					new SingleUniqueKeyEntityLoaderStandard<>( this, attribute,
							new LoadQueryInfluencers( factory ) );
			uniqueKeyLoadersNew.put( attribute, loader );
			return loader;
		}
	}

	private void initOrdinaryPropertyPaths(Metadata mapping) throws MappingException {
		for ( int i = 0; i < getSubclassPropertyNameClosure().length; i++ ) {
			propertyMapping.initPropertyPaths(
					getSubclassPropertyNameClosure()[i],
					getSubclassPropertyTypeClosure()[i],
					getSubclassPropertyColumnNameClosure()[i],
					getSubclassPropertyColumnReaderClosure()[i],
					getSubclassPropertyColumnReaderTemplateClosure()[i],
					getSubclassPropertyFormulaTemplateClosure()[i],
					mapping
			);
		}
	}

	private void initIdentifierPropertyPaths(Metadata mapping) throws MappingException {
		final String idProp = getIdentifierPropertyName();
		if ( idProp != null ) {
			propertyMapping.initPropertyPaths(
					idProp, getIdentifierType(), getIdentifierColumnNames(),
					getIdentifierColumnReaders(), getIdentifierColumnReaderTemplates(), null, mapping
			);
		}
		if ( getIdentifierProperty().isEmbedded() ) {
			propertyMapping.initPropertyPaths(
					null, getIdentifierType(), getIdentifierColumnNames(),
					getIdentifierColumnReaders(), getIdentifierColumnReaderTemplates(), null, mapping
			);
		}
		if ( !hasNonIdentifierPropertyNamedId() ) {
			propertyMapping.initPropertyPaths(
					ENTITY_ID, getIdentifierType(), getIdentifierColumnNames(),
					getIdentifierColumnReaders(), getIdentifierColumnReaderTemplates(), null, mapping
			);
		}
	}

	private void initDiscriminatorPropertyPath(Metadata mapping) {
		propertyMapping.initPropertyPaths(
				ENTITY_CLASS,
				getDiscriminatorType(),
				new String[] {getDiscriminatorColumnName()},
				new String[] {getDiscriminatorColumnReaders()},
				new String[] {getDiscriminatorColumnReaderTemplate()},
				new String[] {getDiscriminatorFormulaTemplate()},
				mapping
		);
	}

	protected void initPropertyPaths(Metadata mapping) throws MappingException {
		initOrdinaryPropertyPaths( mapping );
		initOrdinaryPropertyPaths( mapping ); //do two passes, for collection property-ref!
		initIdentifierPropertyPaths( mapping );
		if ( isPolymorphic() ) {
			initDiscriminatorPropertyPath( mapping );
		}
	}

	@Override
	public String getIdentitySelectString() {
		return identitySelectString;
	}

	@Override
	public String getSelectByUniqueKeyString(String propertyName) {
		return getSelectByUniqueKeyString( new String[] { propertyName } );
	}

	@Override
	public String getSelectByUniqueKeyString(String[] propertyNames) {
		final var select =
				new SimpleSelect( getFactory() )
						.setTableName( getTableName(0) )
						.addColumns( getKeyColumns(0) );
		for ( String propertyName : propertyNames ) {
			select.addRestriction( getPropertyColumnNames( propertyName ) );
		}
		return select.toStatementString();
	}

	@Override
	public String getSelectByUniqueKeyString(String[] propertyNames, String[] columnNames) {
		final var select =
				new SimpleSelect( getFactory() )
						.setTableName( getTableName( 0 ) )
						.addColumns( columnNames );
		for ( final String propertyName : propertyNames ) {
			select.addRestriction( getPropertyColumnNames( propertyName ) );
		}
		return select.toStatementString();
	}

	@Override
	public GeneratedValuesMutationDelegate getInsertDelegate() {
		return insertDelegate;
	}

	@Override
	public GeneratedValuesMutationDelegate getUpdateDelegate() {
		return updateDelegate;
	}

	@Override
	public EntityTableMapping[] getTableMappings() {
		return tableMappings;
	}

	protected EntityTableMapping getTableMapping(int i) {
		return tableMappings[i];
	}

	@Override
	public void forEachTableDetails(Consumer<TableDetails> consumer) {
		CollectionHelper.forEach( getTableMappings(), consumer );
	}

	/**
	 * Unfortunately we cannot directly use `SelectableMapping#getContainingTableExpression()`
	 * as that blows up for attributes declared on super-type for union-subclass mappings
	 */
	@Override
	public String physicalTableNameForMutation(SelectableMapping selectableMapping) {
		assert !selectableMapping.isFormula();
		return selectableMapping.getContainingTableExpression();
	}

	@Override
	public EntityMappingType getTargetPart() {
		return this;
	}

	@Override
	public void forEachMutableTable(Consumer<EntityTableMapping> consumer) {
		for ( int i = 0; i < tableMappings.length; i++ ) {
			// inverse tables are not mutable from this mapping
			if ( !tableMappings[i].isInverse() ) {
				consumer.accept( tableMappings[i] );
			}
		}
	}

	@Override
	public void forEachMutableTableReverse(Consumer<EntityTableMapping> consumer) {
		for ( int i = tableMappings.length - 1; i >= 0; i-- ) {
			// inverse tables are not mutable from this mapping
			if ( !tableMappings[i].isInverse() ) {
				consumer.accept( tableMappings[i] );
			}
		}
	}

	@Override
	public String getIdentifierTableName() {
		return getTableName( 0 );
	}

	@Override
	public EntityTableMapping getIdentifierTableMapping() {
		return tableMappings[0];
	}

	@Override
	public ModelPart getIdentifierDescriptor() {
		return identifierMapping;
	}

	protected void logStaticSQL() {
		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.staticSqlForEntity( getEntityName() );
			for ( var entry : lazyLoadPlanByFetchGroup.entrySet() ) {
				MODEL_MUTATION_LOGGER.lazySelect( String.valueOf(entry.getKey()), entry.getValue().getJdbcSelect().getSqlString() );
			}
			if ( sqlVersionSelectString != null ) {
				MODEL_MUTATION_LOGGER.versionSelect( sqlVersionSelectString );
			}

			{
				final var staticInsertGroup = insertCoordinator.getStaticMutationOperationGroup();
				if ( staticInsertGroup != null ) {
					for ( int i = 0; i < staticInsertGroup.getNumberOfOperations(); i++ ) {
						if ( staticInsertGroup.getOperation( i ) instanceof JdbcOperation jdbcOperation ) {
							MODEL_MUTATION_LOGGER.insertOperationSql( i, jdbcOperation.getSqlString() );
						}
					}
				}
			}

			{
				final var staticUpdateGroup = updateCoordinator.getStaticMutationOperationGroup();
				if ( staticUpdateGroup != null ) {
					for ( int i = 0; i < staticUpdateGroup.getNumberOfOperations(); i++ ) {
						if ( staticUpdateGroup.getOperation( i ) instanceof JdbcOperation jdbcOperation ) {
							MODEL_MUTATION_LOGGER.updateOperationSql( i, jdbcOperation.getSqlString() );
						}
					}
				}
			}

			{
				final var staticDeleteGroup = deleteCoordinator.getStaticMutationOperationGroup();
				if ( staticDeleteGroup != null ) {
					for ( int i = 0; i < staticDeleteGroup.getNumberOfOperations(); i++ ) {
						if ( staticDeleteGroup.getOperation( i ) instanceof JdbcOperation jdbcOperation ) {
							MODEL_MUTATION_LOGGER.deleteOperationSql( i, jdbcOperation.getSqlString() );
						}
					}
				}
			}
		}
	}

	public abstract Map<Object, String> getSubclassByDiscriminatorValue();

	public abstract String[] getConstraintOrderedTableNameClosure();

	public abstract boolean needsDiscriminator();

	protected boolean isDiscriminatorFormula() {
		return false;
	}

	@Override
	public TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAstCreationState creationState) {
		final SqlAliasBase sqlAliasBase = SqlAliasBase.from(
				explicitSqlAliasBase,
				explicitSourceAlias,
				this,
				creationState.getSqlAliasBaseGenerator()
		);
		final TableReference rootTableReference = new NamedTableReference(
				needsDiscriminator() ? getRootTableName() : getTableName(),
				sqlAliasBase.generateNewAlias()
		);

		final TableGroup tableGroup = new StandardTableGroup(
				canUseInnerJoins,
				navigablePath,
				this,
				explicitSourceAlias,
				rootTableReference,
				true,
				sqlAliasBase,
				getRootEntityDescriptor()::containsTableReference,
				(tableExpression, tg) -> {
					final String[] subclassTableNames = getSubclassTableNames();
					for ( int i = 0; i < subclassTableNames.length; i++ ) {
						if ( tableExpression.equals( subclassTableNames[ i ] ) ) {
							final NamedTableReference joinedTableReference = new NamedTableReference(
									tableExpression,
									sqlAliasBase.generateNewAlias(),
									isNullableSubclassTable( i )
							);
							return new TableReferenceJoin(
									shouldInnerJoinSubclassTable( i, emptySet() ),
									joinedTableReference,
									additionalPredicateCollectorAccess == null
											? null
											: generateJoinPredicate(
													rootTableReference,
													joinedTableReference,
													needsDiscriminator()
															? getRootTableKeyColumnNames()
															: getIdentifierColumnNames(),
													getSubclassTableKeyColumns( i ),
													creationState
											)
							);
						}
					}
					return null;
				},
				getFactory()
		);

		if ( additionalPredicateCollectorAccess != null ) {
			if ( needsDiscriminator() ) {
				final String alias = tableGroup.getPrimaryTableReference().getIdentificationVariable();
				final Predicate discriminatorPredicate = createDiscriminatorPredicate( alias, tableGroup, creationState );
				additionalPredicateCollectorAccess.get().accept( discriminatorPredicate );
			}

			if ( softDeleteMapping != null ) {
				final TableReference tableReference =
						tableGroup.resolveTableReference( getSoftDeleteTableDetails().getTableName() );
				final Predicate softDeletePredicate =
						softDeleteMapping.createNonDeletedRestriction( tableReference,
								creationState.getSqlExpressionResolver() );
				additionalPredicateCollectorAccess.get().accept( softDeletePredicate );
				if ( tableReference != rootTableReference && creationState.supportsEntityNameUsage() ) {
					// Register entity name usage for the hierarchy root table to avoid pruning
					creationState.registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION, getRootEntityName() );
				}
			}
		}

		return tableGroup;
	}

	@Override
	public void applyDiscriminator(
			Consumer<Predicate> predicateConsumer,
			String alias,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		if ( needsDiscriminator() ) {
			assert !creationState.supportsEntityNameUsage() : "Entity name usage should have been used instead";
			final var subMappingTypes = getSubMappingTypes();
			final Map<String, EntityNameUse> entityNameUseMap =
					new HashMap<>( 1 + subMappingTypes.size() + ( isInherited() ? 1 : 0 ) );
			if ( subMappingTypes.isEmpty() ) {
				entityNameUseMap.put( getEntityName(), EntityNameUse.TREAT );
			}
			else {
				entityNameUseMap.put( getEntityName(), EntityNameUse.TREAT );
				// We need to register TREAT uses for all subtypes when pruning
				for ( EntityMappingType subMappingType : subMappingTypes ) {
					entityNameUseMap.put( subMappingType.getEntityName(), EntityNameUse.TREAT );
				}
			}
			if ( isInherited() ) {
				// Make sure the table group includes the root table when needed for TREAT
				tableGroup.resolveTableReference( getRootTableName() );
				entityNameUseMap.put( getRootEntityName(), EntityNameUse.EXPRESSION );
			}
			pruneForSubclasses( tableGroup, entityNameUseMap );
		}
	}

	private Predicate createDiscriminatorPredicate(
			String alias,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		final SqlExpressionResolver.ColumnReferenceKey columnReferenceKey;
		final String discriminatorExpression;
		if ( isDiscriminatorFormula() ) {
			discriminatorExpression = getDiscriminatorFormulaTemplate();
			columnReferenceKey = createColumnReferenceKey(
					tableGroup.getPrimaryTableReference(),
					getDiscriminatorFormulaTemplate(),
					getDiscriminatorType()
			);
		}
		else {
			discriminatorExpression = getDiscriminatorColumnName();
			columnReferenceKey = createColumnReferenceKey(
					tableGroup.getPrimaryTableReference(),
					getDiscriminatorColumnName(),
					getDiscriminatorType()
			);
		}

		final BasicType<?> discriminatorType = (BasicType<?>) getDiscriminatorMapping().getJdbcMapping();
		final Expression sqlExpression = creationState.getSqlExpressionResolver().resolveSqlExpression(
				columnReferenceKey,
				sqlAstProcessingState -> new ColumnReference(
						alias,
						discriminatorExpression,
						isDiscriminatorFormula(),
						null,
						discriminatorType.getJdbcMapping()
				)
		);

		return createDisciminatorPredicate( discriminatorType, sqlExpression );
	}

	private Predicate createDisciminatorPredicate(BasicType<?> discriminatorType, Expression sqlExpression) {
		if ( hasSubclasses() ) {
			return createInListPredicate( discriminatorType, sqlExpression );
		}
		else {
			final Object value = getDiscriminatorValue();
			if ( value == NULL_DISCRIMINATOR ) {
				return new NullnessPredicate( sqlExpression );
			}
			else if ( value == NOT_NULL_DISCRIMINATOR ) {
				return new NullnessPredicate( sqlExpression, true );
			}
			else {
				return new ComparisonPredicate( sqlExpression, ComparisonOperator.EQUAL,
						new QueryLiteral<>( value, discriminatorType ) );
			}
		}
	}

	private Predicate createInListPredicate(BasicType<?> discriminatorType, Expression sqlExpression) {
		boolean hasNull = false, hasNonNull = false;
		for ( Object discriminatorValue : fullDiscriminatorValues ) {
			if ( discriminatorValue == NULL_DISCRIMINATOR ) {
				hasNull = true;
			}
			else if ( discriminatorValue == NOT_NULL_DISCRIMINATOR ) {
				hasNonNull = true;
			}
		}
		if ( hasNull && hasNonNull ) {
			// This means we need to select all rows,
			// and so we don't need a predicate at all
			// Just return an empty Junction
			return new Junction( Junction.Nature.DISJUNCTION );
		}
		else if ( hasNonNull ) {
			// we need every row with a non-null discriminator
			return new NullnessPredicate( sqlExpression, true );
		}
		else if ( hasNull ) {
			final var junction = new Junction( Junction.Nature.DISJUNCTION );
			junction.add( new NullnessPredicate( sqlExpression ) );
			junction.add( discriminatorValuesPredicate( discriminatorType, sqlExpression ) );
			return junction;
		}
		else {
			return discriminatorValuesPredicate( discriminatorType, sqlExpression );
		}
	}

	private InListPredicate discriminatorValuesPredicate(BasicType<?> discriminatorType, Expression sqlExpression) {
		final List<Expression> values = new ArrayList<>( fullDiscriminatorValues.length );
		for ( Object discriminatorValue : fullDiscriminatorValues ) {
			if ( !(discriminatorValue instanceof MarkerObject) ) {
				values.add( new QueryLiteral<>( discriminatorValue, discriminatorType) );
			}
		}
		return new InListPredicate( sqlExpression, values);
	}

	protected String getPrunedDiscriminatorPredicate(
			Map<String, EntityNameUse> entityNameUses,
			MappingMetamodelImplementor mappingMetamodel,
			String alias) {
		final var fragment = new InFragment();
		if ( isDiscriminatorFormula() ) {
			fragment.setFormula( alias, getDiscriminatorFormulaTemplate() );
		}
		else {
			fragment.setColumn( alias, getDiscriminatorColumnName() );
		}
		boolean containsNotNull = false;
		for ( var entry : entityNameUses.entrySet() ) {
			final var useKind = entry.getValue().getKind();
			if ( useKind == EntityNameUse.UseKind.PROJECTION || useKind == EntityNameUse.UseKind.EXPRESSION ) {
				// We only care about treat and filter uses which allow to reduce the amount of rows to select
				continue;
			}
			final var persister = mappingMetamodel.getEntityDescriptor( entry.getKey() );
			// Filtering for abstract entities makes no sense, so ignore that
			// Also, it makes no sense to filter for any of the super types,
			// as the query will contain a filter for that already anyway
			if ( !persister.isAbstract() && ( this == persister || !isTypeOrSuperType( persister ) ) ) {
				containsNotNull = containsNotNull || InFragment.NOT_NULL.equals( persister.getDiscriminatorSQLValue() );
				fragment.addValue( persister.getDiscriminatorSQLValue() );
			}
		}
		final var rootEntityDescriptor = (AbstractEntityPersister) getRootEntityDescriptor();
		final List<String> discriminatorSQLValues = Arrays.asList( rootEntityDescriptor.fullDiscriminatorSQLValues );
		if ( fragment.getValues().size() == discriminatorSQLValues.size() ) {
			// Nothing to prune if we filter for all subtypes
			return null;
		}

		if ( containsNotNull ) {
			final String lhs = isDiscriminatorFormula()
					? StringHelper.replace( getDiscriminatorFormulaTemplate(), Template.TEMPLATE, alias )
					: qualifyConditionally( alias, getDiscriminatorColumnName() );
			final List<String> actualDiscriminatorSQLValues = new ArrayList<>( discriminatorSQLValues.size() );
			for ( String value : discriminatorSQLValues ) {
				if ( !fragment.getValues().contains( value ) && !InFragment.NULL.equals( value ) ) {
					actualDiscriminatorSQLValues.add( value );
				}
			}
			final var sql =
					new StringBuilder( 70 + actualDiscriminatorSQLValues.size() * 10 )
							.append( " or " );
			if ( !actualDiscriminatorSQLValues.isEmpty() ) {
				sql.append( lhs ).append( " is not in (" );
				sql.append( String.join( ",", actualDiscriminatorSQLValues ) );
				sql.append( ") and " );
			}
			sql.append( lhs ).append( " is not null" );
			fragment.getValues().remove( InFragment.NOT_NULL );
			return fragment.toFragmentString() + sql;
		}
		else {
			return fragment.toFragmentString();
		}
	}

	@Override
	public void applyFilterRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			SqlAstCreationState creationState) {
		if ( filterHelper != null ) {
			filterHelper.applyEnabledFilters(
					predicateConsumer,
					useQualifier && tableGroup != null
							? getFilterAliasGenerator( tableGroup )
							: null,
					enabledFilters,
					onlyApplyLoadByKeyFilters,
					tableGroup,
					creationState
			);
		}
	}

	@Override
	public void applyBaseRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup, boolean useQualifier,
			Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			Set<String> treatAsDeclarations,
			SqlAstCreationState creationState) {
		applyFilterRestrictions(
				predicateConsumer,
				tableGroup,
				useQualifier,
				enabledFilters,
				onlyApplyLoadByKeyFilters,
				creationState
		);
		applyWhereRestrictions( predicateConsumer, tableGroup, useQualifier, creationState );
	}

	@Override
	public boolean hasWhereRestrictions() {
		return sqlWhereStringTemplate != null;
	}

	@Override
	public void applyWhereRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			SqlAstCreationState creationState) {
		if ( sqlWhereStringTemplate != null ) {
			final String alias = getAliasInWhere( tableGroup, useQualifier );
			final String fragment = StringHelper.replace( sqlWhereStringTemplate, Template.TEMPLATE, alias );
			predicateConsumer.accept( new SqlFragmentPredicate( fragment ) );
		}
	}

	private String getAliasInWhere(TableGroup tableGroup, boolean useQualifier) {
		if ( tableGroup == null ) {
			return null;
		}
		else {
			final var tableReference = tableGroup.resolveTableReference( sqlWhereStringTableExpression );
			return tableReference == null ? null :
					useQualifier && tableReference.getIdentificationVariable() != null
							? tableReference.getIdentificationVariable()
							: tableReference.getTableId();
		}
	}

	protected boolean shouldInnerJoinSubclassTable(int subclassTableNumber, Set<String> treatAsDeclarations) {
		if ( isClassOrSuperclassJoin( subclassTableNumber ) ) {
			// the table is either this persister's driving table or (one of) its superclass persister's driving
			// tables which can be inner-joined as long as the 'shouldInnerJoin' condition resolves to true
			return !isInverseTable( subclassTableNumber )
				&& !isNullableTable( subclassTableNumber );
		}
		else {
			// otherwise we have a subclass table and need to look a little deeper...
			// IMPL NOTE: By default, 'includeSubclasses' indicates that all subclasses should be joined and that
			// each subclass ought to be joined by outer join. However, 'TREAT AS' always requires that an inner
			// join be used, so we give 'TREAT AS' higher precedence.
			return isSubclassTableIndicatedByTreatAsDeclarations( subclassTableNumber, treatAsDeclarations );
		}
	}

	protected boolean isSubclassTableIndicatedByTreatAsDeclarations(int subclassTableNumber, Set<String> treatAsDeclarations) {
		return false;
	}

	/**
	 * Post-construct is a callback for {@code AbstractEntityPersister}
	 * subclasses to call after they are all done with their constructor
	 * processing. It allows {@code AbstractEntityPersister} to extend
	 * its construction after subclass-specific details have all been
	 * taken care of.
	 *
	 * @param mapping The mapping
	 *
	 * @throws MappingException Indicates a problem accessing the Mapping
	 */
	protected void postConstruct(Metadata mapping) throws MappingException {
		initPropertyPaths( mapping );
	}

	@Override
	public void prepareLoaders() {
		// Hibernate Reactive needs to override the loaders
		singleIdLoader = buildSingleIdEntityLoader();
		multiIdLoader = buildMultiIdLoader();

		lazyLoadPlanByFetchGroup = getLazyLoadPlanByFetchGroup();

		logStaticSQL();
	}

	private void doLateInit() {
		tableMappings = buildTableMappings();

		final List<AttributeMapping> insertGeneratedAttributes =
				hasInsertGeneratedProperties()
						? getGeneratedAttributes( this, INSERT )
						: emptyList();
		final List<AttributeMapping> updateGeneratedAttributes =
				hasUpdateGeneratedProperties()
						? getGeneratedAttributes( this, UPDATE )
						: emptyList();

		insertGeneratedProperties = initInsertGeneratedProperties( insertGeneratedAttributes );
		updateGeneratedProperties = initUpdateGeneratedProperties( updateGeneratedAttributes );

		insertDelegate = createInsertDelegate();
		updateDelegate = createUpdateDelegate();

		if ( isIdentifierAssignedByInsert() ) {
			identitySelectString = getIdentitySelectString( getDialect() );
		}

		if ( hasInsertGeneratedProperties() ) {
			insertGeneratedValuesProcessor =
					createGeneratedValuesProcessor( INSERT, insertGeneratedAttributes );
		}
		if ( hasUpdateGeneratedProperties() ) {
			updateGeneratedValuesProcessor =
					createGeneratedValuesProcessor( UPDATE, updateGeneratedAttributes );
		}

		insertCoordinator = buildInsertCoordinator();
		updateCoordinator = buildUpdateCoordinator();
		deleteCoordinator = buildDeleteCoordinator();
		mergeCoordinator = buildMergeCoordinator();

		//select SQL
		sqlVersionSelectString = generateSelectVersionString();
	}

	protected GeneratedValuesMutationDelegate createInsertDelegate() {
		if ( isIdentifierAssignedByInsert() ) {
			final var generator = (OnExecutionGenerator) getGenerator();
			return generator.getGeneratedIdentifierDelegate( this );
		}
		return getGeneratedValuesDelegate( this, INSERT );
	}

	protected GeneratedValuesMutationDelegate createUpdateDelegate() {
		return getGeneratedValuesDelegate( this, UPDATE );
	}

	private static class TableMappingBuilder {
		private final String tableName;
		private final int relativePosition;
		private final EntityTableMapping.KeyMapping keyMapping;
		private final boolean isOptional;
		private final boolean isInverse;
		private final boolean isIdentifierTable;

		private final Expectation insertExpectation;
		private final String customInsertSql;
		private final boolean insertCallable;

		private final Expectation updateExpectation;
		private final String customUpdateSql;
		private final boolean updateCallable;

		private final boolean cascadeDeleteEnabled;
		private final Expectation deleteExpectation;
		private final String customDeleteSql;
		private final boolean deleteCallable;
		private final boolean dynamicUpdate;
		private final boolean dynamicInsert;

		private final List<Integer> attributeIndexes = new ArrayList<>();

		public TableMappingBuilder(
				String tableName,
				int relativePosition,
				EntityTableMapping.KeyMapping keyMapping,
				boolean isOptional,
				boolean isInverse,
				boolean isIdentifierTable,
				Expectation insertExpectation,
				String customInsertSql,
				boolean insertCallable,
				Expectation updateExpectation,
				String customUpdateSql,
				boolean updateCallable,
				boolean cascadeDeleteEnabled,
				Expectation deleteExpectation,
				String customDeleteSql,
				boolean deleteCallable,
				boolean dynamicUpdate,
				boolean dynamicInsert) {
			this.tableName = tableName;
			this.relativePosition = relativePosition;
			this.keyMapping = keyMapping;
			this.isOptional = isOptional;
			this.isInverse = isInverse;
			this.isIdentifierTable = isIdentifierTable;
			this.insertExpectation = insertExpectation;
			this.customInsertSql = customInsertSql;
			this.insertCallable = insertCallable;
			this.updateExpectation = updateExpectation;
			this.customUpdateSql = customUpdateSql;
			this.updateCallable = updateCallable;
			this.cascadeDeleteEnabled = cascadeDeleteEnabled;
			this.deleteExpectation = deleteExpectation;
			this.customDeleteSql = customDeleteSql;
			this.deleteCallable = deleteCallable;
			this.dynamicUpdate = dynamicUpdate;
			this.dynamicInsert = dynamicInsert;
		}

		private EntityTableMapping build() {
			return new EntityTableMapping(
					tableName,
					relativePosition,
					keyMapping,
					isOptional,
					isInverse,
					isIdentifierTable,
					toIntArray( attributeIndexes ),
					insertExpectation,
					customInsertSql,
					insertCallable,
					updateExpectation,
					customUpdateSql,
					updateCallable,
					cascadeDeleteEnabled,
					deleteExpectation,
					customDeleteSql,
					deleteCallable,
					dynamicUpdate,
					dynamicInsert
			);
		}
	}

	/**
	 * Builds the EntityTableMapping descriptors for the tables mapped by this entity.
	 *
	 * @see #visitMutabilityOrderedTables
	 */
	protected EntityTableMapping[] buildTableMappings() {
		final LinkedHashMap<String, TableMappingBuilder> tableBuilderMap = new LinkedHashMap<>();
		visitMutabilityOrderedTables( (tableExpression, relativePosition, tableKeyColumnVisitationSupplier) -> {
			final TableMappingBuilder tableMappingBuilder;

			final TableMappingBuilder existing = tableBuilderMap.get( tableExpression );

			final boolean inverseTable = isInverseTable( relativePosition );
			if ( existing == null ) {
				final List<EntityTableMapping.KeyColumn> keyColumns = new ArrayList<>();
				tableKeyColumnVisitationSupplier.get()
						.accept( (selectionIndex, selectableMapping) -> {
							keyColumns.add( new EntityTableMapping.KeyColumn(
									tableExpression,
									selectableMapping
							) );
						} );

				final boolean isIdentifierTable = isIdentifierTable( tableExpression );

				final String customInsertSql = customSQLInsert[ relativePosition ] == null
						? null
						: substituteBrackets( customSQLInsert[ relativePosition ] );
				final String customUpdateSql = customSQLUpdate[ relativePosition ] == null
						? null
						: substituteBrackets( customSQLUpdate[ relativePosition ] );
				final String customDeleteSql = customSQLDelete[ relativePosition ] == null
						? null
						: substituteBrackets( customSQLDelete[ relativePosition ] );

				tableMappingBuilder = new TableMappingBuilder(
						tableExpression,
						relativePosition,
						EntityTableMapping.createKeyMapping( keyColumns, identifierMapping ),
						!isIdentifierTable && isNullableTable( relativePosition ),
						inverseTable,
						isIdentifierTable,
						insertExpectations[ relativePosition ],
						customInsertSql,
						insertCallable[ relativePosition ],
						updateExpectations[ relativePosition ],
						customUpdateSql,
						updateCallable[ relativePosition ],
						isTableCascadeDeleteEnabled( relativePosition ),
						deleteExpectations[ relativePosition ],
						customDeleteSql,
						deleteCallable[ relativePosition ],
						isDynamicUpdate(),
						isDynamicInsert()
				);

				tableBuilderMap.put( tableExpression, tableMappingBuilder );
			}
			else {
				tableMappingBuilder = existing;
			}

			if ( !inverseTable ) {
				collectAttributesIndexesForTable( relativePosition, tableMappingBuilder.attributeIndexes::add );
			}
		} );

		final var entityTableMappings = new EntityTableMapping[tableBuilderMap.size()];
		int i = 0;
		for ( var entry : tableBuilderMap.entrySet() ) {
			entityTableMappings[i++] = entry.getValue().build();
		}
		return entityTableMappings;
	}

	/**
	 * Visit details about each table for this entity, using "mutability ordering".
	 * When inserting rows, the order we go through the tables to avoid foreign key
	 * problems among the entity's group of tables.
	 * <p>
	 * Used while {@linkplain #buildTableMappings building} the
	 * {@linkplain EntityTableMapping table mapping} descriptors for each table.
	 *
	 * @see #forEachMutableTable
	 * @see #forEachMutableTableReverse
	 */
	protected abstract void visitMutabilityOrderedTables(MutabilityOrderedTableConsumer consumer);

	/**
	 * Consumer for processing table details.  Used while {@linkplain #buildTableMappings() building}
	 * the {@link EntityTableMapping} descriptors.
	 */
	protected interface MutabilityOrderedTableConsumer {
		void consume(
				String tableExpression,
				int relativePosition,
				Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier);
	}

	private void collectAttributesIndexesForTable(int naturalTableIndex, Consumer<Integer> indexConsumer) {
		forEachAttributeMapping( (attributeIndex, attributeMapping) -> {
			if ( isPropertyOfTable( attributeIndex, naturalTableIndex ) ) {
				indexConsumer.accept( attributeIndex );
			}
		} );
	}

	protected abstract boolean isIdentifierTable(String tableExpression);

	protected InsertCoordinator buildInsertCoordinator() {
		return new InsertCoordinatorStandard( this, factory );
	}

	protected UpdateCoordinator buildUpdateCoordinator() {
		// we only have updates to issue for entities with one or more singular attributes
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			if ( attributeMappings.get( i ) instanceof SingularAttributeMapping ) {
				return new UpdateCoordinatorStandard( this, factory );
			}
		}
		// otherwise, nothing to update
		return new UpdateCoordinatorNoOp( this );
	}

	protected UpdateCoordinator buildMergeCoordinator() {
		// we only have updates to issue for entities with one or more singular attributes
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			if ( attributeMappings.get( i ) instanceof SingularAttributeMapping ) {
				return new MergeCoordinator( this, factory );
			}
		}
		// otherwise, nothing to update
		return new UpdateCoordinatorNoOp( this );
	}

	protected DeleteCoordinator buildDeleteCoordinator() {
		return softDeleteMapping == null
				? new DeleteCoordinatorStandard( this, factory )
				: new DeleteCoordinatorSoft( this, factory );
	}

	@Override
	public void addDiscriminatorToInsertGroup(MutationGroupBuilder insertGroupBuilder) {
	}

	@Override
	public void addSoftDeleteToInsertGroup(MutationGroupBuilder insertGroupBuilder) {
		if ( softDeleteMapping != null ) {
			final TableInsertBuilder insertBuilder = insertGroupBuilder.getTableDetailsBuilder( getIdentifierTableName() );
			final var mutatingTable = insertBuilder.getMutatingTable();
			final var columnReference = new ColumnReference( mutatingTable, softDeleteMapping );
			final var nonDeletedValueBinding = softDeleteMapping.createNonDeletedValueBinding( columnReference );
			insertBuilder.addValueColumn( nonDeletedValueBinding );
		}
	}

	protected String substituteBrackets(String sql) {
		return new SQLQueryParser( sql, null, getFactory() ).process();
	}

	@Override
	public final void postInstantiate() throws MappingException {
		doLateInit();
	}

	/**
	 * Load an instance using either the {@code forUpdateLoader} or the outer joining {@code loader},
	 * depending upon the value of the {@code lock} parameter
	 */
	@Override
	public Object load(Object id, Object optionalObject, LockMode lockMode, SharedSessionContractImplementor session) {
		return load( id, optionalObject, lockMode.toLockOptions(), session );
	}

	/**
	 * Load an instance using either the {@code forUpdateLoader} or the outer joining {@code loader},
	 * depending upon the value of the {@code lock} parameter
	 */
	@Override
	public Object load(Object id, Object optionalObject, LockOptions lockOptions, SharedSessionContractImplementor session)
			throws HibernateException {
		return doLoad( id, optionalObject, lockOptions, null, session );
	}

	@Override
	public Object load(Object id, Object optionalObject, LockOptions lockOptions, SharedSessionContractImplementor session, Boolean readOnly)
			throws HibernateException {
		return doLoad( id, optionalObject, lockOptions, readOnly, session );
	}

	private Object doLoad(Object id, Object optionalObject, LockOptions lockOptions, Boolean readOnly, SharedSessionContractImplementor session)
			throws HibernateException {
		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.fetchingEntity( infoString( this, id, getFactory() ) );
		}

		final var loader = determineLoaderToUse( session, lockOptions );
		return optionalObject == null
				? loader.load( id, lockOptions, readOnly, session )
				: loader.load( id, optionalObject, lockOptions, readOnly, session );
	}

	protected SingleIdEntityLoader<?> determineLoaderToUse(SharedSessionContractImplementor session, LockOptions lockOptions) {
		if ( hasNamedQueryLoader() ) {
			return getSingleIdLoader();
		}

		final var influencers = session.getLoadQueryInfluencers();
		if ( isAffectedByInfluencers( influencers, true ) ) {
			return buildSingleIdEntityLoader( influencers, lockOptions );
		}
		return getSingleIdLoader();
//		if ( hasNamedQueryLoader() ) {
//			return getSingleIdLoader();
//		}
//		else {
//			final boolean hasNonDefaultLockOptions = lockOptions != null
//					&& lockOptions.getLockMode().isPessimistic()
//					&& lockOptions.hasNonDefaultOptions();
//			final LoadQueryInfluencers influencers = session.getLoadQueryInfluencers();
//
//			final boolean needsUniqueLoader = hasNonDefaultLockOptions
//					|| isAffectedByInfluencers( influencers, true );
//			return needsUniqueLoader
//					? buildSingleIdEntityLoader( influencers, lockOptions )
//					: getSingleIdLoader();
//		}
	}

	private boolean hasNamedQueryLoader() {
		return queryLoaderName != null;
	}

	public SingleIdEntityLoader<?> getSingleIdLoader() {
		return singleIdLoader;
	}

	@Override
	public Object initializeEnhancedEntityUsedAsProxy(
			Object entity,
			String nameOfAttributeBeingAccessed,
			SharedSessionContractImplementor session) {
		if ( getBytecodeEnhancementMetadata().extractLazyInterceptor( entity )
				instanceof EnhancementAsProxyLazinessInterceptor proxyInterceptor ) {
			final var entityKey = proxyInterceptor.getEntityKey();
			final Object id = entityKey.getIdentifier();
			final Object loaded = loadEnhancedEntityUsedAsProxy( entity, session, entityKey );
			if ( loaded == null ) {
				final var persistenceContext = session.getPersistenceContext();
				persistenceContext.removeEntry( entity );
				persistenceContext.removeEntity( entityKey );
				factory.getEntityNotFoundDelegate().handleEntityNotFound( entityKey.getEntityName(), id );
			}
			return readEnhancedEntityAttribute( entity, id, nameOfAttributeBeingAccessed, session );
		}
		else {
			throw new AssertionFailure( "The BytecodeLazyAttributeInterceptor was not an instance of EnhancementAsProxyLazinessInterceptor" );
		}
	}

	private Object loadEnhancedEntityUsedAsProxy(
			Object entity,
			SharedSessionContractImplementor session,
			EntityKey entityKey) {
		if ( canReadFromCache && session.isEventSource() ) {
			final Object cachedEntity =
					session.loadFromSecondLevelCache( this, entityKey, entity, LockMode.NONE );
			if ( cachedEntity != null ) {
				return cachedEntity;
			}
		}
		final var lockOptions = new LockOptions();
		return determineLoaderToUse( session, lockOptions )
				.load( entityKey.getIdentifier(), entity, lockOptions, session );
	}

	private Object readEnhancedEntityAttribute(
			Object entity, Object id, String nameOfAttributeBeingAccessed,
			SharedSessionContractImplementor session) {
		final var interceptor =
				getBytecodeEnhancementMetadata()
						.injectInterceptor( entity, id, session );
		final Object value;
		if ( nameOfAttributeBeingAccessed == null ) {
			return null;
		}
		else if ( interceptor.isAttributeLoaded( nameOfAttributeBeingAccessed ) ) {
			value = getPropertyValue( entity, nameOfAttributeBeingAccessed );
		}
		else {
			value = initializeLazyProperty( nameOfAttributeBeingAccessed, entity, session );
		}
		return interceptor.readObject( entity, nameOfAttributeBeingAccessed, value );
	}

	@Override
	public List<?> multiLoad(Object[] ids, EventSource session, MultiIdLoadOptions loadOptions) {
		return multiLoad( ids, (SharedSessionContractImplementor) session, loadOptions );
	}

	@Override
	public List<?> multiLoad(Object[] ids, SharedSessionContractImplementor session, MultiIdLoadOptions loadOptions) {
		return multiIdLoader.load( ids, loadOptions, session );
	}

	@Override
	public void registerAffectingFetchProfile(String fetchProfileName) {
		if ( affectingFetchProfileNames == null ) {
			affectingFetchProfileNames = new HashSet<>();
		}
		affectingFetchProfileNames.add( fetchProfileName );
	}

	@Override
	public boolean isAffectedByEntityGraph(LoadQueryInfluencers loadQueryInfluencers) {
		final var graph = loadQueryInfluencers.getEffectiveEntityGraph().getGraph();
		return graph != null
			&& graph.appliesTo( getFactory().getJpaMetamodel().entity( getEntityName() ) );
	}

	@Override
	public boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers loadQueryInfluencers) {
		if ( affectingFetchProfileNames != null && loadQueryInfluencers.hasEnabledFetchProfiles() ) {
			for ( String profileName : loadQueryInfluencers.getEnabledFetchProfileNames() ) {
				if ( affectingFetchProfileNames.contains( profileName ) ) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isAffectedByEnabledFilters(
			LoadQueryInfluencers loadQueryInfluencers,
			boolean onlyApplyForLoadByKeyFilters) {
		if ( filterHelper != null && loadQueryInfluencers.hasEnabledFilters() ) {
			return filterHelper.isAffectedBy( loadQueryInfluencers.getEnabledFilters(), onlyApplyForLoadByKeyFilters )
				|| isAffectedByEnabledFilters( new HashSet<>(), loadQueryInfluencers, onlyApplyForLoadByKeyFilters );

		}
		else {
			return false;
		}
	}

	/**
	 * Locate the property-indices of all properties considered to be dirty.
	 *
	 * @param currentState The current state of the entity (the state to be checked).
	 * @param previousState The previous state of the entity (the state to be checked against).
	 * @param entity The entity for which we are checking state dirtiness.
	 * @param session The session in which the check is occurring.
	 *
	 * @return {@code null} or the indices of the dirty properties
	 *
	 */
	@Override
	public int[] findDirty(Object[] currentState, Object[] previousState, Object entity, SharedSessionContractImplementor session)
			throws HibernateException {
		final int[] props = DirtyHelper.findDirty(
				getDirtyCheckablePropertyTypes(),
				currentState,
				previousState,
				propertyColumnUpdateable,
				session
		);
		if ( props == null ) {
			return null;
		}
		else {
			logDirtyProperties( props );
			return props;
		}
	}

	/**
	 * Locate the property-indices of all properties considered to be dirty.
	 *
	 * @param old The old state of the entity.
	 * @param current The current state of the entity.
	 * @param entity The entity for which we are checking state modification.
	 * @param session The session in which the check is occurring.
	 *
	 * @return {@code null} or the indices of the modified properties
	 *
	 */
	@Override
	public int[] findModified(Object[] old, Object[] current, Object entity, SharedSessionContractImplementor session)
			throws HibernateException {
		final int[] modified = DirtyHelper.findModified(
				getProperties(),
				current,
				old,
				propertyColumnUpdateable,
				getPropertyUpdateability(),
				session
		);
		if ( modified == null ) {
			return null;
		}
		else {
			logDirtyProperties( modified );
			return modified;
		}
	}

	/**
	 * Which properties appear in the SQL update?
	 * (Initialized, updateable ones!)
	 */
	public boolean[] getPropertyUpdateability(Object entity) {
		return hasUninitializedLazyProperties( entity )
				? getNonLazyPropertyUpdateability()
				: getPropertyUpdateability();
	}

	private void logDirtyProperties(int[] props) {
		if ( CORE_LOGGER.isTraceEnabled() ) {
			for ( int prop : props ) {
				final String propertyName = getAttributeMapping( prop ).getAttributeName();
				CORE_LOGGER.propertyIsDirty( qualify( getEntityName(), propertyName ) );
			}
		}
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	private Dialect getDialect() {
		return factory.getJdbcServices().getDialect();
	}

	@Override
	public EntityMetamodel getEntityMetamodel() {
		return this;
	}

	@Override
	public boolean canReadFromCache() {
		return canReadFromCache;
	}

	@Override
	public boolean canWriteToCache() {
		return canWriteToCache;
	}

	@Override
	public boolean hasCache() {
		return canWriteToCache;
	}

	@Override
	public EntityDataAccess getCacheAccessStrategy() {
		return cacheAccessStrategy;
	}

	@Override
	public CacheEntryStructure getCacheEntryStructure() {
		return cacheEntryHelper.getCacheEntryStructure();
	}

	@Override
	public CacheEntry buildCacheEntry(Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
		return cacheEntryHelper.buildCacheEntry( entity, state, version, session );
	}

	@Override
	public boolean hasNaturalIdCache() {
		return naturalIdRegionAccessStrategy != null;
	}

	@Override
	public NaturalIdDataAccess getNaturalIdCacheAccessStrategy() {
		return naturalIdRegionAccessStrategy;
	}


	// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@Override
	public final String getEntityName() {
		return getName();
	}

	@Override
	public @Nullable String getJpaEntityName() {
		return jpaEntityName;
	}

	@Override
	public boolean hasIdentifierProperty() {
		return !getIdentifierProperty().isVirtual();
	}

	@Override
	public BasicType<?> getVersionType() {
		final var versionProperty = getVersionProperty();
		return versionProperty == null ? null : (BasicType<?>) versionProperty.getType();
	}

	@Override
	public boolean isIdentifierAssignedByInsert() {
		return getIdentifierProperty().isIdentifierAssignedByInsert();
	}

	@Override
	public void afterReassociate(Object entity, SharedSessionContractImplementor session) {
		final var metadata = getBytecodeEnhancementMetadata();
		if ( metadata.isEnhancedForLazyLoading() ) {
			final var interceptor = metadata.extractLazyInterceptor( entity );
			if ( interceptor == null ) {
				metadata.injectInterceptor( entity, getIdentifier( entity, session ), session );
			}
			else {
				interceptor.setSession( session );
			}
		}
		handleNaturalIdReattachment( entity, session );
	}

	private void handleNaturalIdReattachment(Object entity, SharedSessionContractImplementor session) {
		if ( naturalIdMapping != null ) {
			if ( naturalIdMapping.isMutable() ) {
				final var persistenceContext = session.getPersistenceContextInternal();
				final var naturalIdResolutions = persistenceContext.getNaturalIdResolutions();
				final Object id = getIdentifier( entity, session );

				// for reattachment of mutable natural-ids, we absolutely positively have to grab the snapshot from the
				// database, because we have no other way to know if the state changed while detached.
				final Object[] entitySnapshot = persistenceContext.getDatabaseSnapshot( id, this );
				final Object naturalIdSnapshot = naturalIdFromSnapshot( entitySnapshot );

				naturalIdResolutions.removeSharedResolution( id, naturalIdSnapshot, this, false );
				final Object naturalId = naturalIdMapping.extractNaturalIdFromEntity( entity );
				naturalIdResolutions.manageLocalResolution( id, naturalId, this, CachedNaturalIdValueSource.UPDATE );
			}
			// otherwise we assume there were no changes to natural id during detachment for now,
			// that is validated later during flush.
		}
	}

	private Object naturalIdFromSnapshot(Object[] entitySnapshot) {
		return entitySnapshot == PersistenceContext.NO_ROW ? null
				: naturalIdMapping.extractNaturalIdFromEntityState( entitySnapshot );
	}

	@Override
	public Boolean isTransient(Object entity, SharedSessionContractImplementor session) throws HibernateException {
		final Object id = getIdentifier( entity, session );
		// we *always* assume an instance with a null
		// identifier or no identifier property is unsaved!
		if ( id == null ) {
			return true;
		}

		// check the version unsaved-value, if appropriate
		if ( isVersioned() ) {
			// let this take precedence if defined, since it works for
			// assigned identifiers
			final Object version = getVersion( entity );
			final Boolean isUnsaved = versionMapping.getUnsavedStrategy().isUnsaved( version );
			if ( isUnsaved != null ) {
				if ( isUnsaved ) {
					if ( version == null ) {
						final var persistenceContext = session.getPersistenceContext();
						if ( persistenceContext.hasLoadContext()
								&& !persistenceContext.getLoadContexts().isLoadingFinished() ) {
							// check if we're currently loading this entity instance, the version
							// will be null, but the entity cannot be considered transient
							final var holder = persistenceContext.getEntityHolder( new EntityKey( id, this ) );
							if ( holder != null && holder.isEventuallyInitialized() && holder.getEntity() == entity ) {
								return false;
							}
						}
					}
					if ( getGenerator() != null ) {
						final Boolean unsaved = identifierMapping.getUnsavedStrategy().isUnsaved( id );
						if ( unsaved != null && !unsaved ) {
							throw new PropertyValueException(
									"Detached entity with generated id '" + id
											+ "' has an uninitialized version value '" + version + "'",
									getEntityName(),
									getVersionColumnName()
							);
						}
					}
				}
				return isUnsaved;
			}
		}

		// check the id unsaved-value
		final Boolean result = identifierMapping.getUnsavedStrategy().isUnsaved( id );
		if ( result != null ) {
			return result;
		}

		// check to see if it is in the second-level cache
		if ( session.getCacheMode().isGetEnabled() && canReadFromCache() ) {
			final Object cacheKey =
					getCacheAccessStrategy()
							.generateCacheKey( id, this, session.getFactory(), session.getTenantIdentifier() );
			final Object cacheEntry = fromSharedCache( session, cacheKey, this, getCacheAccessStrategy() );
			if ( cacheEntry != null ) {
				return false;
			}
		}

		return null;
	}

	@Override
	public boolean hasProxy() {
		// skip proxy instantiation if entity is bytecode enhanced
		return isLazy()
			&& !getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
	}

	@Override @Deprecated
	public IdentifierGenerator getIdentifierGenerator() throws HibernateException {
		return getIdentifierProperty().getIdentifierGenerator();
	}

	@Override
	public Generator getGenerator() {
		return getIdentifierProperty().getGenerator();
	}

	@Override
	public BeforeExecutionGenerator getVersionGenerator() {
		return versionGenerator;
	}

	@Override
	public String getRootEntityName() {
		return getRootName();
	}

	@Override
	public String getMappedSuperclass() {
		return getSuperclass();
	}

	@Override
	public boolean isConcreteProxy() {
		return concreteProxy;
	}

	@Override
	public EntityMappingType resolveConcreteProxyTypeForId(Object id, SharedSessionContractImplementor session) {
		if ( !concreteProxy ) {
			return this;
		}
		else {
			var concreteTypeLoader = this.concreteTypeLoader;
			if ( concreteTypeLoader == null ) {
				this.concreteTypeLoader = concreteTypeLoader =
						new EntityConcreteTypeLoader( this, session.getFactory() );
			}
			return concreteTypeLoader.getConcreteType( id, session );
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * Warning:
	 * When there are duplicated property names in the subclasses
	 * then this method may return the wrong results.
	 * To ensure correct results, this method should only be used when
	 * {@literal this} is the concrete EntityPersister (since the
	 * concrete EntityPersister cannot have duplicated property names).
	 *
	 * @deprecated by the supertypes
	 */
	@Override @Deprecated
	public Type getPropertyType(String propertyName) throws MappingException {
		// todo (PropertyMapping) : caller also deprecated (aka, easy to remove)
		return propertyMapping.toType( propertyName );
	}

	@Override
	public boolean isSelectBeforeUpdateRequired() {
		return isSelectBeforeUpdate();
	}

	public final OptimisticLockStyle optimisticLockStyle() {
		return getOptimisticLockStyle();
	}

	@Override
	public Object createProxy(Object id, SharedSessionContractImplementor session) throws HibernateException {
		return representationStrategy.getProxyFactory().getProxy( id, session );
	}

	@Override
	public String toString() {
		return unqualify( getClass().getName() )
				+ '(' + getName() + ')';
	}

	@Override
	public boolean isInstrumented() {
		return getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
	}

	@Override
	public boolean hasInsertGeneratedProperties() {
		return hasInsertGeneratedValues();
	}

	@Override
	public boolean hasUpdateGeneratedProperties() {
		return hasUpdateGeneratedValues();
	}

	@Override
	public boolean hasPreInsertGeneratedProperties() {
		return hasPreInsertGeneratedValues();
	}

	@Override
	public boolean hasPreUpdateGeneratedProperties() {
		return hasPreUpdateGeneratedValues();
	}

	@Override
	public boolean isVersionPropertyGenerated() {
		return isVersioned()
			&& ( isVersionGeneratedOnExecution() || isVersionGeneratedBeforeExecution() );
	}

	private Generator versionPropertyGenerator() {
		return getGenerators()[ this.getVersionPropertyIndex() ];
	}

	public boolean isVersionGeneratedOnExecution() {
		final var strategy = versionPropertyGenerator();
		return strategy != null
			&& strategy.generatesSometimes()
			&& strategy.generatedOnExecution();
	}

	public boolean isVersionGeneratedBeforeExecution() {
		final var strategy = versionPropertyGenerator();
		return strategy != null
			&& strategy.generatesSometimes()
			&& !strategy.generatedOnExecution();
	}

	@Override
	public void afterInitialize(Object entity, SharedSessionContractImplementor session) {
		if ( isPersistentAttributeInterceptable( entity )
				&& getRepresentationStrategy().getMode() == POJO ) {
			final var interceptor =
					getBytecodeEnhancementMetadata()
							.extractLazyInterceptor( entity );
			assert interceptor != null;
			if ( interceptor.getLinkedSession() == null ) {
				interceptor.setSession( session );
			}
		}
	}

	@Override
	public boolean[] getNonLazyPropertyUpdateability() {
		return getNonlazyPropertyUpdateability();
	}

	@Override
	public CascadeStyle[] getPropertyCascadeStyles() {
		return getCascadeStyles();
	}

	@Override
	public final Class<?> getMappedClass() {
		return this.getMappedJavaType().getJavaTypeClass();
	}

	@Override
	public Class<?> getConcreteProxyClass() {
		final var proxyJavaType = getRepresentationStrategy().getProxyJavaType();
		return proxyJavaType != null ? proxyJavaType.getJavaTypeClass() : javaType.getJavaTypeClass();
	}

	@Override
	public void setPropertyValues(Object object, Object[] values) {
		if ( accessOptimizer != null ) {
			accessOptimizer.setPropertyValues( object, values );
		}
		else {
			final int size = getAttributeMappings().size();
			if ( getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() ) {
				for ( int i = 0; i < size; i++ ) {
					final Object value = values[i];
					if ( value != UNFETCHED_PROPERTY ) {
						setterCache[i].set( object, value );
					}
				}
			}
			else {
				for ( int i = 0; i < size; i++ ) {
					setterCache[i].set( object, values[i] );
				}
			}
		}
	}

	@Override
	public void setPropertyValue(Object object, int i, Object value) {
		setterCache[i].set( object, value );
	}

	@Override
	public Object[] getPropertyValues(Object object) {
		if ( accessOptimizer != null ) {
			return accessOptimizer.getPropertyValues( object );
		}
		else {
			final var enhancementMetadata = getBytecodeEnhancementMetadata();
			final var attributeMappings = getAttributeMappings();
			final Object[] values = new Object[attributeMappings.size()];
			if ( enhancementMetadata.isEnhancedForLazyLoading() ) {
				final var lazyAttributesMetadata = enhancementMetadata.getLazyAttributesMetadata();
				for ( int i = 0; i < attributeMappings.size(); i++ ) {
					final var attributeMapping = attributeMappings.get( i );
					if ( !lazyAttributesMetadata.isLazyAttribute( attributeMapping.getAttributeName() )
							|| enhancementMetadata.isAttributeLoaded( object, attributeMapping.getAttributeName() ) ) {
						values[i] = getterCache[i].get( object );
					}
					else {
						values[i] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
					}
				}
			}
			else {
				for ( int i = 0; i < attributeMappings.size(); i++ ) {
					values[i] = getterCache[i].get( object );
				}
			}

			return values;
		}
	}

	@Override
	public Object getPropertyValue(Object object, int i) {
		return getterCache[i].get( object );
	}

	@Override
	public Object getPropertyValue(Object object, String path) {
		final String basePropertyName = root( path );
		final boolean isBasePath = basePropertyName.length() == path.length();
		final var attributeMapping = findAttributeMapping( basePropertyName );
		final Object baseValue;
		final MappingType baseValueType;
		if ( attributeMapping != null ) {
			baseValue = getterCache[ attributeMapping.getStateArrayPosition() ].get( object );
			baseValueType = attributeMapping.getMappedType();
		}
		else if ( identifierMapping instanceof NonAggregatedIdentifierMapping nonAggregatedIdentifierMapping ) {
			final var mapping =
					nonAggregatedIdentifierMapping.findSubPart( path, null )
							.asAttributeMapping();
			baseValue = mapping == null ? null : mapping.getValue( object );
			baseValueType = mapping == null ? null : mapping.getMappedType();
		}
		else {
			baseValue = null;
			baseValueType = null;
		}
		return isBasePath
				? baseValue
				: getPropertyValue( baseValue, (ManagedMappingType) baseValueType, path, basePropertyName );
	}

	private Object getPropertyValue(
			Object baseValue,
			ManagedMappingType baseValueType,
			String path,
			String prefix) {
		if ( baseValueType == null ) {
			// TODO: is this necessary? Should it be an exception instead?
			return baseValue;
		}
		else {
			final int afterDot = prefix.length() + 1;
			final int nextDotIndex = path.indexOf( '.', afterDot );
			final String pathSoFar = nextDotIndex < 0 ? path : path.substring( 0, nextDotIndex );
			final var attributeMapping = baseValueType.findAttributeMapping( pathSoFar.substring( afterDot ) );
			final var value = attributeMapping.getValue( baseValue );
			final var type = nextDotIndex < 0 ? null : (ManagedMappingType) attributeMapping.getMappedType();
			return getPropertyValue( value, type, path, pathSoFar );
		}
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return identifierMapping.getIdentifier( entity );
	}

	@Override
	public Object getIdentifier(Object entity, MergeContext mergeContext) {
		return identifierMapping.getIdentifier( entity, mergeContext );
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		identifierMapping.setIdentifier( entity, id, session );
	}

	@Override
	public Object getVersion(Object object) {
		final var versionMapping = getVersionMapping();
		return versionMapping == null ? null
				: versionMapping.getVersionAttribute().getPropertyAccess().getGetter().get( object );
	}

	@Override
	public Object instantiate(Object id, SharedSessionContractImplementor session) {
		final Object instance = getRepresentationStrategy().getInstantiator().instantiate();
		linkToSession( instance, session );
		if ( id != null ) {
			setIdentifier( instance, id, session );
		}
		return instance;
	}

	protected void linkToSession(Object entity, SharedSessionContractImplementor session) {
		if ( session != null ) {
			processIfPersistentAttributeInterceptable( entity, this::setSession, session );
		}
	}

	private void setSession(PersistentAttributeInterceptable entity, SharedSessionContractImplementor session) {
		final var interceptor =
				getBytecodeEnhancementMetadata()
						.extractLazyInterceptor( entity );
		if ( interceptor != null ) {
			interceptor.setSession( session );
		}
	}

	@Override
	public boolean isInstance(Object object) {
		return getRepresentationStrategy().getInstantiator().isInstance( object );
	}

	@Override
	public boolean hasUninitializedLazyProperties(Object object) {
		return getBytecodeEnhancementMetadata().hasUnFetchedAttributes( object );
	}

	@Override
	public void resetIdentifier(
			Object entity,
			Object currentId,
			Object currentVersion,
			SharedSessionContractImplementor session) {
		if ( !getGenerator().allowAssignedIdentifiers() ) {
			// reset the identifier
			final Object defaultIdentifier = identifierMapping.getUnsavedStrategy().getDefaultValue( currentId );
			setIdentifier( entity, defaultIdentifier, session );
		}
		// reset the version
		if ( versionMapping != null ) {
			final Object defaultVersion = versionMapping.getUnsavedStrategy().getDefaultValue( currentVersion );
			versionMapping.getVersionAttribute().getPropertyAccess().getSetter().set( entity, defaultVersion );
		}
	}

	@Override
	public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory) {
		if ( instance != null
				&& hasSubclasses()
				&& !getRepresentationStrategy().getInstantiator().isSameClass( instance ) ) {
			// todo (6.0) : this previously used `org.hibernate.tuple.entity.EntityTuplizer#determineConcreteSubclassEntityName`
			//		- we may need something similar here...
			for ( var subclassMappingType : subclassMappingTypes.values() ) {
				final var persister = subclassMappingType.getEntityPersister();
				if ( persister.getRepresentationStrategy().getInstantiator().isSameClass( instance ) ) {
					return persister;
				}
			}
		}
		return this;
	}

	@Override
	public boolean hasMultipleTables() {
		return false;
	}

	@Override
	public Object[] getPropertyValuesToInsert(
			Object entity,
			Map<Object,Object> mergeMap,
			SharedSessionContractImplementor session)
				throws HibernateException {
		if ( shouldGetAllProperties( entity ) && accessOptimizer != null ) {
			return accessOptimizer.getPropertyValues( entity );
		}
		else {
			final Object[] result = new Object[attributeMappings.size()];
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				result[i] = getterCache[i].getForInsert( entity, mergeMap, session );
			}
			return result;
		}
	}

	protected boolean shouldGetAllProperties(Object entity) {
		final var metadata = getBytecodeEnhancementMetadata();
		return !metadata.isEnhancedForLazyLoading()
			|| !metadata.hasUnFetchedAttributes( entity );
	}

	@Override
	public void processInsertGeneratedProperties(
			Object id,
			Object entity,
			Object[] state,
			GeneratedValues generatedValues,
			SharedSessionContractImplementor session) {
		if ( insertGeneratedValuesProcessor == null ) {
			throw new UnsupportedOperationException( "Entity has no insert-generated properties - '" + getEntityName() + "'" );
		}
		insertGeneratedValuesProcessor.processGeneratedValues( entity, id, state, generatedValues, session );
	}

	protected List<? extends ModelPart> initInsertGeneratedProperties(List<AttributeMapping> generatedAttributes) {
		final int originalSize = generatedAttributes.size();
		final List<ModelPart> generatedBasicAttributes = new ArrayList<>( originalSize );
		for ( var generatedAttribute : generatedAttributes ) {
			// todo (7.0) : support non selectable mappings? Component, ToOneAttributeMapping, ...
			if ( generatedAttribute.asBasicValuedModelPart() != null
					&& generatedAttribute.getContainingTableExpression().equals( getRootTableName() ) ) {
				generatedBasicAttributes.add( generatedAttribute );
			}
		}

		final List<ModelPart> identifierList =
				isIdentifierAssignedByInsert()
						? List.of( getIdentifierMapping() )
						: emptyList();
		return originalSize > 0 && generatedBasicAttributes.size() == originalSize
				? unmodifiableList( combine( identifierList, generatedBasicAttributes ) )
				: identifierList;
	}

	@Override
	public List<? extends ModelPart> getInsertGeneratedProperties() {
		return insertGeneratedProperties;
	}

	@Override
	public void processUpdateGeneratedProperties(
			Object id,
			Object entity,
			Object[] state,
			GeneratedValues generatedValues,
			SharedSessionContractImplementor session) {
		if ( updateGeneratedValuesProcessor == null ) {
			throw new AssertionFailure( "Entity has no update-generated properties - '" + getEntityName() + "'" );
		}
		updateGeneratedValuesProcessor.processGeneratedValues( entity, id, state, generatedValues, session );
	}

	protected List<? extends ModelPart> initUpdateGeneratedProperties(List<AttributeMapping> generatedAttributes) {
		final int originalSize = generatedAttributes.size();
		final List<ModelPart> generatedBasicAttributes = new ArrayList<>( originalSize );
		for ( var generatedAttribute : generatedAttributes ) {
			if ( generatedAttribute instanceof SelectableMapping selectableMapping
					&& selectableMapping.getContainingTableExpression().equals( getSubclassTableName( 0 ) ) ) {
				generatedBasicAttributes.add( generatedAttribute );
			}
		}
		return generatedBasicAttributes.size() == originalSize
				? unmodifiableList( generatedBasicAttributes )
				: emptyList();
	}

	@Override
	public List<? extends ModelPart> getUpdateGeneratedProperties() {
		return updateGeneratedProperties;
	}

	@Override
	public String getIdentifierPropertyName() {
		return getIdentifierProperty().getName();
	}

	@Override
	public Type getIdentifierType() {
		return getIdentifierProperty().getType();
	}

	@Override
	public boolean hasSubselectLoadableCollections() {
		return hasSubselectLoadableCollections;
	}

	@Override
	public boolean hasCollectionNotReferencingPK() {
		return hasCollectionNotReferencingPK;
	}

	protected void verifyHasNaturalId() {
		if ( ! hasNaturalIdentifier() ) {
			throw new HibernateException( "Entity does not define a natural id : " + getEntityName() );
		}
	}

	@Override
	public Object getNaturalIdentifierSnapshot(Object id, SharedSessionContractImplementor session) {
		verifyHasNaturalId();
		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.gettingCurrentNaturalIdSnapshot( getEntityName(), id );
		}
		return getNaturalIdLoader().resolveIdToNaturalId( id, session );
	}


	@Override
	public NaturalIdLoader<?> getNaturalIdLoader() {
		verifyHasNaturalId();
		if ( naturalIdLoader == null ) {
			naturalIdLoader = naturalIdMapping.makeLoader( this );
		}
		return naturalIdLoader;
	}

	@Override
	public MultiNaturalIdLoader<?> getMultiNaturalIdLoader() {
		verifyHasNaturalId();
		if ( multiNaturalIdLoader == null ) {
			multiNaturalIdLoader = naturalIdMapping.makeMultiLoader( this );
		}
		return multiNaturalIdLoader;
	}

	@Override
	public Object loadEntityIdByNaturalId(
			Object[] naturalIdValues,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		verifyHasNaturalId();
		return getNaturalIdLoader().resolveNaturalIdToId( naturalIdValues, session );
	}

	public static int getTableId(String tableName, String[] tables) {
		for ( int j = 0; j < tables.length; j++ ) {
			if ( tableName.equalsIgnoreCase( tables[j] ) ) {
				return j;
			}
		}
		throw new AssertionFailure( "Table " + tableName + " not found" );
	}

	@Override
	public EntityRepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Override @Deprecated(forRemoval = true)
	public BytecodeEnhancementMetadata getInstrumentationMetadata() {
		return getBytecodeEnhancementMetadata();
	}

	@Override
	public String getTableNameForColumn(String columnName) {
		return getTableName( determineTableNumberForColumn( columnName ) );
	}

	protected int determineTableNumberForColumn(String columnName) {
		return 0;
	}

	protected String determineTableName(Table table) {
		return table.getSubselect() != null
				? "( " + createSqlQueryParser( table ).process() + " )"
				: factory.getSqlStringGenerationContext().format( table.getQualifiedTableName() );
	}

	private SQLQueryParser createSqlQueryParser(Table table) {
		return new SQLQueryParser(
				table.getSubselect(),
				null,
				// NOTE: this allows finer control over catalog and schema used for
				// placeholder handling (`{h-catalog}`, `{h-schema}`, `{h-domain}`)
				new ExplicitSqlStringGenerationContext( table.getCatalog(), table.getSchema(), factory )
		);
	}

	@Override
	public EntityEntryFactory getEntityEntryFactory() {
		return entityEntryFactory;
	}

	/**
	 * Consolidated these onto a single helper because the 2 pieces work in tandem.
	 */
	public interface CacheEntryHelper {
		CacheEntryStructure getCacheEntryStructure();

		CacheEntry buildCacheEntry(Object entity, Object[] state, Object version, SharedSessionContractImplementor session);
	}

	private static class StandardCacheEntryHelper implements CacheEntryHelper {
		private final EntityPersister persister;

		private StandardCacheEntryHelper(EntityPersister persister) {
			this.persister = persister;
		}

		@Override
		public CacheEntryStructure getCacheEntryStructure() {
			return UnstructuredCacheEntry.INSTANCE;
		}

		@Override
		public CacheEntry buildCacheEntry(Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
			return new StandardCacheEntryImpl( state, persister, version, session, entity );
		}
	}

	private static class ReferenceCacheEntryHelper implements CacheEntryHelper {
		private final EntityPersister persister;

		private ReferenceCacheEntryHelper(EntityPersister persister) {
			this.persister = persister;
		}

		@Override
		public CacheEntryStructure getCacheEntryStructure() {
			return UnstructuredCacheEntry.INSTANCE;
		}

		@Override
		public CacheEntry buildCacheEntry(Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
			return new ReferenceCacheEntryImpl( entity, persister );
		}
	}

	private static class StructuredCacheEntryHelper implements CacheEntryHelper {
		private final EntityPersister persister;
		private final StructuredCacheEntry structure;

		private StructuredCacheEntryHelper(EntityPersister persister) {
			this.persister = persister;
			this.structure = new StructuredCacheEntry( persister );
		}

		@Override
		public CacheEntryStructure getCacheEntryStructure() {
			return structure;
		}

		@Override
		public CacheEntry buildCacheEntry(Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
			return new StandardCacheEntryImpl( state, persister, version, session, entity );
		}
	}

	private static class NoopCacheEntryHelper implements CacheEntryHelper {
		public static final NoopCacheEntryHelper INSTANCE = new NoopCacheEntryHelper();

		@Override
		public CacheEntryStructure getCacheEntryStructure() {
			return UnstructuredCacheEntry.INSTANCE;
		}

		@Override
		public CacheEntry buildCacheEntry(Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
			throw new HibernateException( "Illegal attempt to build cache entry for non-cached entity" );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// org.hibernate.metamodel.mapping.EntityMappingType

	@Override
	public void forEachAttributeMapping(Consumer<? super AttributeMapping> action) {
		this.attributeMappings.forEach( action );
	}

	@Override
	public void forEachAttributeMapping(IndexedConsumer<? super AttributeMapping> consumer) {
		attributeMappings.indexedForEach( consumer );
	}

	@Override
	public void prepareMappingModel(MappingModelCreationProcess creationProcess) {
		if ( identifierMapping == null ) {
			prepareMappings( creationProcess );
			handleSubtypeMappings( creationProcess );
			prepareMultiTableMutationStrategy( creationProcess );
			prepareMultiTableInsertStrategy( creationProcess );
		}
	}

	private void handleSubtypeMappings(MappingModelCreationProcess creationProcess) {
		// Register a callback for after all `#prepareMappingModel` calls have finished. Here we want to delay the
		// generation of `staticFetchableList` because we need to wait until after all subclasses have had their
		// `#prepareMappingModel` called (and their declared attribute mappings resolved)
		creationProcess.registerInitializationCallback(
				"Entity(" + getEntityName() + ") `staticFetchableList` generator",
				() -> {
					final var builder = new ImmutableAttributeMappingList.Builder( attributeMappings.size() );
					visitSubTypeAttributeMappings( builder::add );
					assert superMappingType != null || builder.assertFetchableIndexes();
					staticFetchableList = builder.build();
					return true;
				}
		);
	}

	private static ReflectionOptimizer.AccessOptimizer accessOptimizer(EntityRepresentationStrategy strategy) {
		final var reflectionOptimizer = strategy.getReflectionOptimizer();
		return reflectionOptimizer == null ? null : reflectionOptimizer.getAccessOptimizer();
	}

	private void prepareMappings(MappingModelCreationProcess creationProcess) {
		final var persistentClass =
				creationProcess.getCreationContext().getBootModel()
						.getEntityBinding( getEntityName() );
		initializeSpecialAttributeMappings( creationProcess, persistentClass );
		versionGenerator = createVersionGenerator( super.getVersionGenerator(), versionMapping );
		buildDeclaredAttributeMappings( creationProcess, persistentClass );
		getAttributeMappings();
		initializeNaturalIdMapping( creationProcess, persistentClass );
	}

	private void initializeSpecialAttributeMappings
			(MappingModelCreationProcess creationProcess, PersistentClass bootEntityDescriptor) {
		if ( superMappingType != null ) {
			( (InFlightEntityMappingType) superMappingType ).prepareMappingModel( creationProcess );
			if ( shouldProcessSuperMapping() ) {
				inheritSupertypeSpecialAttributeMappings();
			}
			else {
				prepareMappingModel( creationProcess, bootEntityDescriptor );
			}
		}
		else {
			prepareMappingModel( creationProcess, bootEntityDescriptor );
		}
	}

	private void inheritSupertypeSpecialAttributeMappings() {
		discriminatorMapping = superMappingType.getDiscriminatorMapping();
		identifierMapping = superMappingType.getIdentifierMapping();
		naturalIdMapping = superMappingType.getNaturalIdMapping();
		versionMapping = superMappingType.getVersionMapping();
		rowIdMapping = superMappingType.getRowIdMapping();
		softDeleteMapping = superMappingType.getSoftDeleteMapping();
	}

	private void buildDeclaredAttributeMappings
			(MappingModelCreationProcess creationProcess, PersistentClass bootEntityDescriptor) {
		final var properties = getProperties();
		final var mappingsBuilder = AttributeMappingsMap.builder();
		int stateArrayPosition = getStateArrayInitialPosition( creationProcess );
		int fetchableIndex = getFetchableIndexOffset();
		for ( int i = 0; i < getPropertySpan(); i++ ) {
			final var runtimeAttributeDefinition = properties[i];
			final String attributeName = runtimeAttributeDefinition.getName();
			final var bootProperty = bootEntityDescriptor.getProperty( attributeName );
			if ( superMappingType == null
					|| superMappingType.findAttributeMapping( bootProperty.getName() ) == null ) {
				mappingsBuilder.put(
						attributeName,
						generateNonIdAttributeMapping(
								runtimeAttributeDefinition,
								bootProperty,
								stateArrayPosition++,
								fetchableIndex++,
								creationProcess
						)
				);
			}
			declaredAttributeMappings = mappingsBuilder.build();
			// otherwise, it's defined on the supertype, skip it here
		}
	}

	private static @Nullable BeforeExecutionGenerator createVersionGenerator(
			@Nullable BeforeExecutionGenerator configuredGenerator,
			@Nullable EntityVersionMapping versionMapping) {
		if ( versionMapping != null ) {
			// need to do this here because EntityMetamodel doesn't have the EntityVersionMapping :-(
			return configuredGenerator == null ? new VersionGeneration( versionMapping ) : configuredGenerator;
		}
		else {
			return configuredGenerator;
		}
	}

	private void prepareMultiTableMutationStrategy(MappingModelCreationProcess creationProcess) {
		// No need for multi-table mutation strategy for subselect entity since update/delete don't make sense
		if ( !isSubselect() && hasMultipleTables() ) {
			creationProcess.registerInitializationCallback(
					"Entity(" + getEntityName() + ") `sqmMultiTableMutationStrategy` interpretation",
					() -> {
						sqmMultiTableMutationStrategy =
								interpretSqmMultiTableStrategy( this, creationProcess );
						if ( sqmMultiTableMutationStrategy == null ) {
							return false;
						}
						else {
							sqmMultiTableMutationStrategy.prepare( creationProcess );
							return true;
						}
					}
			);
		}
	}

	private void prepareMultiTableInsertStrategy(MappingModelCreationProcess creationProcess) {
		// No need for multi-table insert strategy for subselect entity since insert doesn't make sense
		if ( !isSubselect() && ( hasMultipleTables() || generatorNeedsMultiTableInsert() ) ) {
			creationProcess.registerInitializationCallback(
					"Entity(" + getEntityName() + ") `sqmMultiTableInsertStrategy` interpretation",
					() -> {
						sqmMultiTableInsertStrategy =
								interpretSqmMultiTableInsertStrategy( this, creationProcess );
						if ( sqmMultiTableInsertStrategy == null ) {
							return false;
						}
						else {
							sqmMultiTableInsertStrategy.prepare( creationProcess );
							return true;
						}
					}
			);
		}
	}

	private boolean isSubselect() {
		// For the lack of a
		return getRootTableName().charAt( 0 ) == '(';
	}

	private boolean generatorNeedsMultiTableInsert() {
		final var generator = getGenerator();
		if ( generator instanceof BulkInsertionCapableIdentifierGenerator
				&& generator instanceof OptimizableGenerator optimizableGenerator ) {
			final var optimizer = optimizableGenerator.getOptimizer();
			return optimizer != null && optimizer.getIncrementSize() > 1;
		}
		else {
			return false;
		}
	}

	private int getFetchableIndexOffset() {
		if ( superMappingType != null ) {
			final var rootEntityDescriptor = getRootEntityDescriptor();
			int offset = rootEntityDescriptor.getNumberOfDeclaredAttributeMappings();
			for ( var subMappingType : rootEntityDescriptor.getSubMappingTypes() ) {
				if ( subMappingType == this ) {
					break;
				}
				// Determining the number of attribute mappings unfortunately has to be done this way,
				// because calling `subMappingType.getNumberOfDeclaredAttributeMappings()` at this point
				// may produce wrong results because subMappingType might not have completed prepareMappingModel yet
				final int propertySpan =
						subMappingType.getEntityPersister().getPropertySpan();
				final int superPropertySpan =
						subMappingType.getSuperMappingType().getEntityPersister().getPropertySpan();
				final int numberOfDeclaredAttributeMappings = propertySpan - superPropertySpan;
				offset += numberOfDeclaredAttributeMappings;
			}
			return offset;
		}
		return 0;
	}

	private void prepareMappingModel(MappingModelCreationProcess creationProcess, PersistentClass bootEntityDescriptor) {
		final var instantiator = getRepresentationStrategy().getInstantiator();
		final Supplier<?> instantiate = instantiator.canBeInstantiated() ? instantiator::instantiate : null;
		identifierMapping =
				creationProcess.processSubPart( EntityIdentifierMapping.ID_ROLE_NAME,
						(role, process) -> generateIdentifierMapping( instantiate, bootEntityDescriptor, process ) );
		versionMapping = generateVersionMapping( instantiate, bootEntityDescriptor, creationProcess );
		rowIdMapping = rowIdName == null ? null
				: creationProcess.processSubPart( rowIdName,
						(role, process) -> new EntityRowIdMappingImpl( rowIdName, getTableName(), this ) );
		discriminatorMapping = generateDiscriminatorMapping( bootEntityDescriptor );
		final var rootClass = bootEntityDescriptor.getRootClass();
		softDeleteMapping =
				resolveSoftDeleteMapping( this, rootClass, getIdentifierTableName(), creationProcess );
		if ( softDeleteMapping != null && rootClass.getCustomSQLDelete() != null ) {
			throw new UnsupportedMappingException( "Entity may not define both @SoftDelete and @SQLDelete" );
		}
	}

	private void initializeNaturalIdMapping
			(MappingModelCreationProcess creationProcess, PersistentClass bootEntityDescriptor) {
		if ( superMappingType != null ) {
			naturalIdMapping = superMappingType.getNaturalIdMapping();
		}
		else if ( bootEntityDescriptor.hasNaturalId() ) {
			naturalIdMapping = generateNaturalIdMapping( creationProcess, bootEntityDescriptor );
		}
		else {
			naturalIdMapping = null;
		}
	}

	protected NaturalIdMapping generateNaturalIdMapping
			(MappingModelCreationProcess creationProcess, PersistentClass bootEntityDescriptor) {
		//noinspection AssertWithSideEffects
		assert bootEntityDescriptor.hasNaturalId();

		final int[] naturalIdAttributeIndexes = getNaturalIdentifierProperties();
		assert naturalIdAttributeIndexes.length > 0;

		if ( naturalIdAttributeIndexes.length == 1 ) {
			final String propertyName = getPropertyNames()[ naturalIdAttributeIndexes[ 0 ] ];
			final var attributeMapping = (SingularAttributeMapping) findAttributeMapping( propertyName );
			return new SimpleNaturalIdMapping( attributeMapping, this, creationProcess );
		}

		// collect the names of the attributes making up the natural-id.
		final Set<String> attributeNames = setOfSize( naturalIdAttributeIndexes.length );
		for ( int naturalIdAttributeIndex : naturalIdAttributeIndexes ) {
			attributeNames.add( getPropertyNames()[ naturalIdAttributeIndex ] );
		}

		// then iterate over the attribute mappings finding the ones having names
		// in the collected names.  iterate here because it is already alphabetical

		final List<SingularAttributeMapping> collectedAttrMappings = new ArrayList<>();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final var attributeMapping = attributeMappings.get( i );
			if ( attributeNames.contains( attributeMapping.getAttributeName() ) ) {
				collectedAttrMappings.add( (SingularAttributeMapping) attributeMapping );
			}
		}

		if ( collectedAttrMappings.size() <= 1 ) {
			throw new MappingException( "Expected multiple natural-id attributes, but found only one: " + getEntityName() );
		}

		return new CompoundNaturalIdMapping(this, collectedAttrMappings, creationProcess );
	}

	protected static SqmMultiTableMutationStrategy interpretSqmMultiTableStrategy(
			AbstractEntityPersister entityMappingDescriptor,
			MappingModelCreationProcess creationProcess) {
		assert entityMappingDescriptor.hasMultipleTables();
		final var superMappingType = entityMappingDescriptor.getSuperMappingType();
		if ( superMappingType != null ) {
			final var sqmMultiTableMutationStrategy =
					superMappingType.getSqmMultiTableMutationStrategy();
			if ( sqmMultiTableMutationStrategy != null ) {
				return sqmMultiTableMutationStrategy;
			}
		}
		return creationProcess.getCreationContext().getServiceRegistry()
				.requireService( SqmMultiTableMutationStrategyProvider.class )
				.createMutationStrategy( entityMappingDescriptor, creationProcess );
	}

	protected static SqmMultiTableInsertStrategy interpretSqmMultiTableInsertStrategy(
			AbstractEntityPersister entityMappingDescriptor,
			MappingModelCreationProcess creationProcess) {
		return creationProcess.getCreationContext().getServiceRegistry()
				.requireService( SqmMultiTableMutationStrategyProvider.class )
				.createInsertStrategy( entityMappingDescriptor, creationProcess );
	}

	@Override
	public SqmMultiTableMutationStrategy getSqmMultiTableMutationStrategy() {
		return sqmMultiTableMutationStrategy;
	}

	@Override
	public SqmMultiTableInsertStrategy getSqmMultiTableInsertStrategy() {
		return sqmMultiTableInsertStrategy;
	}

	protected int getStateArrayInitialPosition(MappingModelCreationProcess creationProcess) {
		// todo (6.0) not sure this is correct in case of SingleTable Inheritance
		//            and for Table per class when the selection is the root
		if ( superMappingType == null ) {
			return 0;
		}
		else {
			( (InFlightEntityMappingType) superMappingType ).prepareMappingModel( creationProcess );
			return superMappingType.getNumberOfAttributeMappings();
		}
	}

	protected boolean isPhysicalDiscriminator() {
		return getDiscriminatorFormulaTemplate() == null;
	}

	protected EntityDiscriminatorMapping generateDiscriminatorMapping(PersistentClass bootEntityDescriptor) {
		if ( getDiscriminatorType() == null ) {
			return null;
		}
		else {
			final String discriminatorColumnExpression;
			final String columnDefinition;
			final Long length;
			final Integer arrayLength;
			final Integer precision;
			final Integer scale;
			final String discriminatorFormulaTemplate = getDiscriminatorFormulaTemplate();
			if ( discriminatorFormulaTemplate == null ) {
				final var discriminator = bootEntityDescriptor.getDiscriminator();
				final Column column =
						discriminator == null
								? null
								: discriminator.getColumns().get( 0 );
				discriminatorColumnExpression = getDiscriminatorColumnReaders();
				if ( column == null ) {
					columnDefinition = null;
					length = null;
					arrayLength = null;
					precision = null;
					scale = null;
				}
				else {
					columnDefinition = column.getSqlType();
					length = column.getLength();
					arrayLength = column.getArrayLength();
					precision = column.getPrecision();
					scale = column.getScale();
				}
			}
			else {
				discriminatorColumnExpression = discriminatorFormulaTemplate;
				columnDefinition = null;
				length = null;
				arrayLength = null;
				precision = null;
				scale = null;
			}
			return new ExplicitColumnDiscriminatorMappingImpl(
					this,
					discriminatorColumnExpression,
					getTableName(),
					discriminatorColumnExpression,
					discriminatorFormulaTemplate != null,
					isPhysicalDiscriminator(),
					false,
					columnDefinition,
					null,
					length,
					arrayLength,
					precision,
					scale,
					getDiscriminatorDomainType()
			);
		}
	}

	@Override
	public abstract BasicType<?> getDiscriminatorType();

	protected EntityVersionMapping generateVersionMapping(
			Supplier<?> templateInstanceCreator,
			PersistentClass bootEntityDescriptor,
			MappingModelCreationProcess creationProcess) {
		if ( getVersionType() == null ) {
			return null;
		}
		else {
			return creationProcess.processSubPart(
					getPropertyNames()[this.getVersionPropertyIndex()],
					(role, process) -> generateVersionMapping(
							this,
							templateInstanceCreator,
							bootEntityDescriptor,
							creationProcess
					)
			);
		}
	}

	protected boolean shouldProcessSuperMapping(){
		return true;
	}

	@Override
	public void linkWithSuperType(MappingModelCreationProcess creationProcess) {
		if ( getMappedSuperclass() != null ) {
			superMappingType = creationProcess.getEntityPersister( getMappedSuperclass() );
			final var inFlightEntityMappingType = (InFlightEntityMappingType) superMappingType;
			inFlightEntityMappingType.linkWithSubType(this, creationProcess);
			if ( subclassMappingTypes != null ) {
				subclassMappingTypes.values()
						.forEach( sub -> inFlightEntityMappingType.linkWithSubType(sub, creationProcess) );
			}
		}
	}

	@Override
	public void linkWithSubType(EntityMappingType sub, MappingModelCreationProcess creationProcess) {
		if ( subclassMappingTypes == null ) {
			subclassMappingTypes = new TreeMap<>();
		}
		subclassMappingTypes.put( sub.getEntityName(), sub );
		if ( superMappingType != null ) {
			( (InFlightEntityMappingType) superMappingType ).linkWithSubType( sub, creationProcess );
		}
	}

	@Override
	public int getNumberOfAttributeMappings() {
		if ( attributeMappings == null ) {
			// force calculation of `attributeMappings`
			getAttributeMappings();
		}
		return attributeMappings.size();
	}

	@Override
	public AttributeMapping getAttributeMapping(int position) {
		return attributeMappings.get( position );
	}

	@Override
	public int getNumberOfDeclaredAttributeMappings() {
		return declaredAttributeMappings.size();
	}

	@Override
	public AttributeMappingsMap getDeclaredAttributeMappings() {
		return declaredAttributeMappings;
	}

	@Override
	public void visitDeclaredAttributeMappings(Consumer<? super AttributeMapping> action) {
		declaredAttributeMappings.forEachValue( action );
	}

	@Override
	public EntityMappingType getSuperMappingType() {
		return superMappingType;
	}

	@Override
	public Collection<EntityMappingType> getSubMappingTypes() {
		return subclassMappingTypes == null ? emptyList() : subclassMappingTypes.values();
	}

	@Override
	public boolean isTypeOrSuperType(EntityMappingType targetType) {
		if ( targetType == null ) {
			// todo (6.0) : need to think through what this ought to indicate (if we allow it at all)
			//		- see `org.hibernate.metamodel.mapping.internal.AbstractManagedMappingType#isTypeOrSuperType`
			return true;
		}
		else if ( targetType == this ) {
			return true;
		}
		else if ( superMappingType != null ) {
			return superMappingType.isTypeOrSuperType( targetType );
		}
		else {
			return false;
		}
	}


	protected EntityIdentifierMapping generateIdentifierMapping(
			Supplier<?> templateInstanceCreator,
			PersistentClass bootEntityDescriptor,
			MappingModelCreationProcess creationProcess) {
		final Type idType = getIdentifierType();

		if ( idType instanceof CompositeType cidType ) {

			// NOTE: the term `isEmbedded` here uses Hibernate's older (pre-JPA) naming for its "non-aggregated"
			// composite-id support.  It unfortunately conflicts with the JPA usage of "embedded".  Here we normalize
			// the legacy naming to the more descriptive encapsulated versus non-encapsulated phrasing

			final boolean encapsulated = !cidType.isEmbedded();
			if ( encapsulated ) {
				// we have an `@EmbeddedId`
				final var identifierProperty = bootEntityDescriptor.getIdentifierProperty();
				return buildEncapsulatedCompositeIdentifierMapping(
						this,
						identifierProperty,
						identifierProperty.getName(),
						getTableName(),
						rootTableKeyColumnNames,
						cidType,
						creationProcess
				);
			}

			// otherwise we have a non-encapsulated composite-identifier
			return generateNonEncapsulatedCompositeIdentifierMapping( creationProcess, bootEntityDescriptor );
		}
		final String columnDefinition;
		final Long length;
		final Integer arrayLength;
		final Integer precision;
		final Integer scale;
		final var identifier = bootEntityDescriptor.getIdentifier();
		if ( identifier == null ) {
			columnDefinition = null;
			length = null;
			arrayLength = null;
			precision = null;
			scale = null;
		}
		else {
			final Column column = identifier.getColumns().get( 0 );
			columnDefinition = column.getSqlType();
			length = column.getLength();
			arrayLength = column.getArrayLength();
			precision = column.getPrecision();
			scale = column.getScale();
		}

		final var identifierProperty = bootEntityDescriptor.getIdentifierProperty();
		final var value = identifierProperty.getValue();
		return new BasicEntityIdentifierMappingImpl(
				this,
				templateInstanceCreator,
				identifierProperty.getName(),
				getTableName(),
				rootTableKeyColumnNames[0],
				columnDefinition,
				length,
				arrayLength,
				precision,
				scale,
				value.isColumnInsertable( 0 ),
				value.isColumnUpdateable( 0 ),
				(BasicType<?>) idType,
				creationProcess
		);
	}

	protected EntityIdentifierMapping generateNonEncapsulatedCompositeIdentifierMapping(
			MappingModelCreationProcess creationProcess,
			PersistentClass bootEntityDescriptor) {
		return buildNonEncapsulatedCompositeIdentifierMapping(
				this,
				getTableName(),
				getRootTableKeyColumnNames(),
				bootEntityDescriptor,
				creationProcess
		);
	}

	/**
	 * @param entityPersister The AbstractEntityPersister being constructed - still initializing
	 * @param bootModelRootEntityDescriptor The boot-time entity descriptor for the "root entity" in the hierarchy
	 * @param creationProcess The SF creation process - access to useful things
	 */
	protected static EntityVersionMapping generateVersionMapping(
			AbstractEntityPersister entityPersister,
			Supplier<?> templateInstanceCreator,
			PersistentClass bootModelRootEntityDescriptor,
			MappingModelCreationProcess creationProcess) {
		final var versionProperty = bootModelRootEntityDescriptor.getVersion();
		final var bootModelVersionValue = (BasicValue) versionProperty.getValue();
		final var basicTypeResolution = bootModelVersionValue.resolve();

		final var column = (Column) bootModelVersionValue.getColumn();
		final var dialect = creationProcess.getCreationContext().getDialect();

		return new EntityVersionMappingImpl(
				bootModelRootEntityDescriptor.getRootClass(),
				templateInstanceCreator,
				bootModelRootEntityDescriptor.getVersion().getName(),
				entityPersister.getTableName(),
				column.getText( dialect ),
				column.getSqlType(),
				column.getLength(),
				column.getArrayLength(),
				column.getPrecision(),
				column.getScale(),
				column.getTemporalPrecision(),
				basicTypeResolution.getLegacyResolvedBasicType(),
				entityPersister
		);
	}

	protected AttributeMapping generateNonIdAttributeMapping(
			NonIdentifierAttribute tupleAttrDefinition,
			Property bootProperty,
			int stateArrayPosition,
			int fetchableIndex,
			MappingModelCreationProcess creationProcess) {
		final var creationContext = creationProcess.getCreationContext();

		final String attrName = tupleAttrDefinition.getName();
		final Type attrType = tupleAttrDefinition.getType();

		final int propertyIndex = getPropertyIndex( bootProperty.getName() );

		final String tableExpression = getTableName( getPropertyTableNumbers()[propertyIndex] );
		final String[] attrColumnNames = getPropertyColumnNames( propertyIndex );

		final var propertyAccess = getRepresentationStrategy().resolvePropertyAccess( bootProperty );

		final var value = bootProperty.getValue();
		if ( propertyIndex == this.getVersionPropertyIndex() ) {
			final Column column = value.getColumns().get( 0 );
			return buildBasicAttributeMapping(
					attrName,
					getNavigableRole().append( bootProperty.getName() ),
					stateArrayPosition,
					fetchableIndex,
					bootProperty,
					this,
					(BasicType<?>) attrType,
					tableExpression,
					attrColumnNames[0],
					null,
					false,
					null,
					"?",
					column.getSqlType(),
					column.getLength(),
					column.getArrayLength(),
					column.getPrecision(),
					column.getScale(),
					column.getTemporalPrecision(),
					column.isSqlTypeLob( creationProcess.getCreationContext().getMetadata() ),
					column.isNullable(),
					value.isColumnInsertable( 0 ),
					value.isColumnUpdateable( 0 ),
					propertyAccess,
					tupleAttrDefinition.getCascadeStyle(),
					creationProcess
			);
		}

		if ( attrType instanceof BasicType ) {
			final NavigableRole role = getNavigableRole().append( bootProperty.getName() );
			final String attrColumnExpression;
			final boolean isAttrColumnExpressionFormula;
			final String customReadExpr;
			final String customWriteExpr;
			final String columnDefinition;
			final Long length;
			final Integer arrayLength;
			final Integer precision;
			final Integer scale;
			final Integer temporalPrecision;
			final boolean isLob;
			final boolean nullable;

			if ( value instanceof DependantValue ) {
				attrColumnExpression = attrColumnNames[0];
				isAttrColumnExpressionFormula = false;
				customReadExpr = null;
				customWriteExpr = "?";
				Column column = value.getColumns().get( 0 );
				columnDefinition = column.getSqlType();
				length = column.getLength();
				arrayLength = column.getArrayLength();
				precision = column.getPrecision();
				temporalPrecision = column.getTemporalPrecision();
				scale = column.getScale();
				isLob = column.isSqlTypeLob( creationProcess.getCreationContext().getMetadata() );
				nullable = column.isNullable();
			}
			else {
				final var basicBootValue = (BasicValue) value;

				if ( attrColumnNames[ 0 ] != null ) {
					attrColumnExpression = attrColumnNames[ 0 ];
					isAttrColumnExpressionFormula = false;

					final var selectables = basicBootValue.getSelectables();
					assert !selectables.isEmpty();
					final var selectable = selectables.get(0);

					final var dialect = creationContext.getDialect();

					assert attrColumnExpression.equals( selectable.getText( dialect ) );

					customReadExpr = selectable.getTemplate(
							dialect,
							creationContext.getTypeConfiguration()
					);
					customWriteExpr = selectable.getWriteExpr(
							(JdbcMapping) attrType,
							dialect,
							creationContext.getBootModel()
					);
					final var column = value.getColumns().get( 0 );
					columnDefinition = column.getSqlType();
					length = column.getLength();
					arrayLength = column.getArrayLength();
					precision = column.getPrecision();
					temporalPrecision = column.getTemporalPrecision();
					scale = column.getScale();
					nullable = column.isNullable();
					isLob = column.isSqlTypeLob( creationContext.getMetadata() );
					resolveAggregateColumnBasicType( creationProcess, role, column );
				}
				else {
					final String[] attrColumnFormulaTemplate = propertyColumnFormulaTemplates[ propertyIndex ];
					attrColumnExpression = attrColumnFormulaTemplate[ 0 ];
					isAttrColumnExpressionFormula = true;
					customReadExpr = null;
					customWriteExpr = null;
					columnDefinition = null;
					length = null;
					arrayLength = null;
					precision = null;
					temporalPrecision = null;
					scale = null;
					nullable = true;
					isLob = false;
				}
			}

			return buildBasicAttributeMapping(
					attrName,
					role,
					stateArrayPosition,
					fetchableIndex,
					bootProperty,
					this,
					(BasicType<?>) value.getType(),
					tableExpression,
					attrColumnExpression,
					null,
					isAttrColumnExpressionFormula,
					customReadExpr,
					customWriteExpr,
					columnDefinition,
					length,
					arrayLength,
					precision,
					scale,
					temporalPrecision,
					isLob,
					nullable,
					value.isColumnInsertable( 0 ),
					value.isColumnUpdateable( 0 ),
					propertyAccess,
					tupleAttrDefinition.getCascadeStyle(),
					creationProcess
			);
		}
		else if ( attrType instanceof AnyType anyType ) {
			final var baseAssociationJtd =
					creationContext.getTypeConfiguration().getJavaTypeRegistry()
							.resolveDescriptor( Object.class );

			final MutabilityPlan<?> mutabilityPlan =
					new DiscriminatedAssociationAttributeMapping.MutabilityPlanImpl( anyType );
			final var attributeMetadataAccess = new SimpleAttributeMetadata(
					propertyAccess,
					mutabilityPlan,
					bootProperty.isOptional(),
					bootProperty.isInsertable(),
					bootProperty.isUpdatable(),
					bootProperty.isOptimisticLocked(),
					bootProperty.isSelectable()
			);

			return new DiscriminatedAssociationAttributeMapping(
					navigableRole.append( bootProperty.getName() ),
					baseAssociationJtd,
					this,
					stateArrayPosition,
					fetchableIndex,
					attributeMetadataAccess,
					bootProperty.isLazy() ? FetchTiming.DELAYED : FetchTiming.IMMEDIATE,
					propertyAccess,
					bootProperty,
					anyType,
					(Any) value,
					creationProcess
			);
		}
		else if ( attrType instanceof CompositeType ) {
			final DependantValue dependantValue;
			if ( bootProperty.getValue() instanceof DependantValue depValue ) {
				dependantValue = depValue;
			}
			else {
				dependantValue = null;
			}
			return buildEmbeddedAttributeMapping(
					attrName,
					stateArrayPosition,
					fetchableIndex,
					bootProperty,
					dependantValue,
					0,
					this,
					(CompositeType) attrType,
					tableExpression,
					null,
					propertyAccess,
					tupleAttrDefinition.getCascadeStyle(),
					creationProcess
			);
		}
		else if ( attrType instanceof CollectionType ) {
			return buildPluralAttributeMapping(
					attrName,
					stateArrayPosition,
					fetchableIndex,
					bootProperty,
					this,
					propertyAccess,
					tupleAttrDefinition.getCascadeStyle(),
					getFetchMode( stateArrayPosition ),
					creationProcess
			);
		}
		else if ( attrType instanceof EntityType entityType ) {
			return buildSingularAssociationAttributeMapping(
					attrName,
					getNavigableRole().append( attrName ),
					stateArrayPosition,
					fetchableIndex,
					bootProperty,
					this,
					this,
					entityType,
					propertyAccess,
					tupleAttrDefinition.getCascadeStyle(),
					creationProcess
			);
		}

		// todo (6.0) : for now ignore any non basic-typed attributes

		return null;
	}

	/**
	 * For Hibernate Reactive
	 */
	protected EmbeddedAttributeMapping buildEmbeddedAttributeMapping(
			String attrName,
			int stateArrayPosition,
			int fetchableIndex,
			Property bootProperty,
			DependantValue dependantValue,
			int dependantColumnIndex,
			ManagedMappingType declaringType,
			CompositeType attrType,
			String tableExpression,
			String[] rootTableKeyColumnNames,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		return MappingModelCreationHelper.buildEmbeddedAttributeMapping(
				attrName,
				stateArrayPosition,
				fetchableIndex,
				bootProperty,
				dependantValue,
				dependantColumnIndex,
				declaringType,
				attrType,
				tableExpression,
				rootTableKeyColumnNames,
				propertyAccess,
				cascadeStyle,
				creationProcess
		);
	}

	/**
	 * For Hibernate Reactive
	 */
	protected AttributeMapping buildSingularAssociationAttributeMapping(
			String attrName,
			NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			Property bootProperty,
			ManagedMappingType declaringType,
			EntityPersister declaringEntityPersister,
			EntityType attrType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			MappingModelCreationProcess creationProcess) {
		return MappingModelCreationHelper.buildSingularAssociationAttributeMapping(
				attrName,
				navigableRole,
				stateArrayPosition,
				fetchableIndex,
				bootProperty,
				declaringType,
				declaringEntityPersister,
				attrType,
				propertyAccess,
				cascadeStyle,
				creationProcess
		);
	}

	/**
	 * For Hibernate Reactive
	 */
	protected AttributeMapping buildPluralAttributeMapping(
			String attrName,
			int stateArrayPosition,
			int fetchableIndex,
			Property bootProperty,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess,
			CascadeStyle cascadeStyle,
			FetchMode fetchMode,
			MappingModelCreationProcess creationProcess) {
		return MappingModelCreationHelper.buildPluralAttributeMapping(
				attrName,
				stateArrayPosition,
				fetchableIndex,
				bootProperty,
				declaringType,
				propertyAccess,
				cascadeStyle,
				fetchMode,
				creationProcess
		);
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return javaType;
	}

	@Override
	public EntityPersister getEntityPersister() {
		return this;
	}

	@Override
	public EntityIdentifierMapping getIdentifierMapping() {
		return identifierMapping;
	}

	@Override
	public EntityVersionMapping getVersionMapping() {
		return versionMapping;
	}

	@Override
	public EntityRowIdMapping getRowIdMapping() {
		return rowIdMapping;
	}

	@Override
	public EntityDiscriminatorMapping getDiscriminatorMapping() {
		return discriminatorMapping;
	}

	@Override
	public SoftDeleteMapping getSoftDeleteMapping() {
		return softDeleteMapping;
	}

	@Override
	public AttributeMappingsList getAttributeMappings() {
		if ( attributeMappings == null ) {
			int sizeHint = declaredAttributeMappings.size();
			sizeHint += (superMappingType == null ? 0 : superMappingType.getAttributeMappings().size() );
			final var builder = new ImmutableAttributeMappingList.Builder( sizeHint );

			if ( superMappingType != null ) {
				superMappingType.forEachAttributeMapping( builder::add );
			}

			for ( var am : declaredAttributeMappings.valueIterator() ) {
				builder.add( am );
			}
			attributeMappings = builder.build();
			final Getter[] getters = new Getter[attributeMappings.size()];
			final Setter[] setters = new Setter[attributeMappings.size()];
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final var propertyAccess = attributeMappings.get( i ).getAttributeMetadata().getPropertyAccess();
				getters[i] = propertyAccess.getGetter();
				setters[i] = propertyAccess.getSetter();
			}
			getterCache = getters;
			setterCache = setters;
			// subclasses?  it depends on the usage
		}

		return attributeMappings;
	}

	@Override
	public AttributeMapping findDeclaredAttributeMapping(String name) {
		return declaredAttributeMappings.get( name );
	}

	@Override
	public AttributeMapping findAttributeMapping(String name) {
		final var declaredAttribute = declaredAttributeMappings.get( name );
		if ( declaredAttribute != null ) {
			return declaredAttribute;
		}

		if ( superMappingType != null ) {
			return superMappingType.findAttributeMapping( name );
		}

		return null;
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {

		if ( EntityDiscriminatorMapping.matchesRoleName( name ) ) {
			return discriminatorMapping;
		}

		final var declaredAttribute = declaredAttributeMappings.get( name );
		if ( declaredAttribute != null ) {
			return declaredAttribute;
		}

		if ( superMappingType != null ) {
			final var superDefinedAttribute =
					superMappingType.findSubPart( name, superMappingType );
			if ( superDefinedAttribute != null ) {
				// Prefer the identifier mapping of the concrete class
				if ( superDefinedAttribute.isEntityIdentifierMapping() ) {
					final var identifierModelPart =
							getIdentifierModelPart( name, treatTargetType );
					if ( identifierModelPart != null ) {
						return identifierModelPart;
					}
				}
				return superDefinedAttribute;
			}
		}

		if ( treatTargetType == null ) {
			final var subDefinedAttribute = findSubPartInSubclassMappings( name );
			if ( subDefinedAttribute != null ) {
				return subDefinedAttribute;
			}
		}
		else if ( treatTargetType != this ) {
			if ( !treatTargetType.isTypeOrSuperType( this ) ) {
				return null;
			}
			// Prefer attributes defined in the treat target type or its subtypes
			final var treatTypeSubPart = treatTargetType.findSubTypesSubPart( name, null );
			if ( treatTypeSubPart != null ) {
				return treatTypeSubPart;
			}
			else {
				// If not found, look in the treat target type's supertypes
				EntityMappingType superType = treatTargetType.getSuperMappingType();
				while ( superType != this ) {
					final var superTypeSubPart = superType.findDeclaredAttributeMapping( name );
					if ( superTypeSubPart != null ) {
						return superTypeSubPart;
					}
					superType = superType.getSuperMappingType();
				}
			}
		}

		final var identifierModelPart = getIdentifierModelPart( name, treatTargetType );
		if ( identifierModelPart != null ) {
			return identifierModelPart;
		}
		else {
			for ( var attribute : declaredAttributeMappings.valueIterator() ) {
				if ( attribute instanceof EmbeddableValuedModelPart part
						&& attribute instanceof VirtualModelPart ) {
					final var subPart = part.findSubPart( name, null );
					if ( subPart != null ) {
						return subPart;
					}
				}
			}
			return null;
		}
	}

	private ModelPart findSubPartInSubclassMappings(String name) {
		ModelPart attribute = null;
		if ( isNotEmpty( subclassMappingTypes ) ) {
			for ( var subMappingType : subclassMappingTypes.values() ) {
				final var subDefinedAttribute = subMappingType.findSubTypesSubPart( name, null );
				if ( subDefinedAttribute != null ) {
					if ( attribute != null && !isCompatibleModelPart( attribute, subDefinedAttribute ) ) {
						throw new PathException( String.format(
								Locale.ROOT,
								"Could not resolve attribute '%s' of '%s' due to the attribute being declared in multiple subtypes '%s' and '%s'",
								name,
								getJavaType().getTypeName(),
								attribute.asAttributeMapping().getDeclaringType().getJavaType().getTypeName(),
								subDefinedAttribute.asAttributeMapping().getDeclaringType().getJavaType().getTypeName()
						) );
					}
					attribute = subDefinedAttribute;
				}
			}
		}
		return attribute;
	}

	@Override
	public ModelPart findSubTypesSubPart(String name, EntityMappingType treatTargetType) {
		final var declaredAttribute = declaredAttributeMappings.get( name );
		if ( declaredAttribute != null ) {
			return declaredAttribute;
		}
		else {
			return findSubPartInSubclassMappings( name );
		}
	}

	private ModelPart getIdentifierModelPart(String name, EntityMappingType treatTargetType) {
		final var identifierMapping = getIdentifierMappingForJoin();
		if ( identifierMapping instanceof final NonAggregatedIdentifierMapping mapping ) {
			final var subPart = mapping.findSubPart( name, treatTargetType );
			if ( subPart != null ) {
				return subPart;
			}
		}

		if ( isIdentifierReference( name ) ) {
			return identifierMapping;
		}

		return null;
	}

	private boolean isIdentifierReference(String name) {
		return EntityIdentifierMapping.ID_ROLE_NAME.equals( name )
			|| hasIdentifierProperty() && getIdentifierPropertyName().equals( name )
			|| !hasNonIdentifierPropertyNamedId() && "id".equals( name );
	}

	@Override
	public void visitSubParts(
			Consumer<ModelPart> consumer,
			EntityMappingType treatTargetType) {
		consumer.accept( identifierMapping );
		declaredAttributeMappings.forEachValue( consumer );
	}

	@Override
	public void visitKeyFetchables(Consumer<? super Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		// No-op
	}

	@Override
	public void visitKeyFetchables(IndexedConsumer<? super Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		// No-op
	}

	@Override
	public int getNumberOfFetchables() {
		return getStaticFetchableList().size();
	}

	@Override
	public int getNumberOfFetchableKeys() {
		return superMappingType == null ? getNumberOfFetchables() : getRootEntityDescriptor().getNumberOfFetchables();
	}

	@Override
	public Fetchable getKeyFetchable(int position) {
		throw new IndexOutOfBoundsException( position );
	}

	@Override
	public AttributeMapping getFetchable(int position) {
		return getStaticFetchableList().get( position );
	}

	@Override
	public void visitFetchables(Consumer<? super Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		if ( treatTargetType == null ) {
			getStaticFetchableList().forEach( fetchableConsumer );
//			staticFetchableList.forEach( fetchableConsumer );
		}
		else {
			if ( treatTargetType.isTypeOrSuperType( this ) ) {
				visitSubTypeAttributeMappings( fetchableConsumer );
			}
			else {
				attributeMappings.forEach( fetchableConsumer );
			}
		}
	}

	@Override
	public void visitFetchables(IndexedConsumer<? super Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		if ( treatTargetType == null ) {
			getStaticFetchableList().indexedForEach( fetchableConsumer );
		}
		else {
			attributeMappings.indexedForEach( fetchableConsumer );
			if ( treatTargetType.isTypeOrSuperType( this ) ) {
				if ( subclassMappingTypes != null ) {
					int offset = attributeMappings.size();
					for ( var subtype : subclassMappingTypes.values() ) {
						final var declaredAttributeMappings = subtype.getDeclaredAttributeMappings();
						for ( var declaredAttributeMapping : declaredAttributeMappings.valueIterator() ) {
							fetchableConsumer.accept( offset++, declaredAttributeMapping );
						}
					}
				}
			}
		}
	}

	protected AttributeMappingsList getStaticFetchableList() {
		return staticFetchableList;
	}

	@Override
	public void visitAttributeMappings(Consumer<? super AttributeMapping> action) {
		attributeMappings.forEach( action );
	}

	@Override
	public void visitSuperTypeAttributeMappings(Consumer<? super AttributeMapping> action) {
		if ( superMappingType != null ) {
			superMappingType.visitSuperTypeAttributeMappings( action );
		}
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer selectableConsumer) {
		int span = 0;
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final var attributeMapping = attributeMappings.get( i );
			span += attributeMapping.forEachSelectable( span + offset, selectableConsumer );
		}
		return span;
	}

	@Override
	public void visitSubTypeAttributeMappings(Consumer<? super AttributeMapping> action) {
		forEachAttributeMapping( action );
		if ( subclassMappingTypes != null ) {
			for ( EntityMappingType subType : subclassMappingTypes.values() ) {
				subType.visitDeclaredAttributeMappings( action );
			}
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityDefinition impl (walking model - deprecated)

	@Override
	public int getJdbcTypeCount() {
		return getIdentifierMapping().getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return getIdentifierMapping().forEachJdbcType( offset, action );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		final var identifierMapping = getIdentifierMapping();
		final Object identifier = identifierMapping.getIdentifier( value );
		return identifierMapping.disassemble( identifier, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return getIdentifierMapping()
				.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> consumer,
			SharedSessionContractImplementor session) {
		final var identifierMapping = getIdentifierMapping();
		final Object identifier = value == null ? null
				: identifierMapping.disassemble( identifierMapping.getIdentifier( value ), session );
		return identifierMapping.forEachDisassembledJdbcValue( identifier, offset, x, y, consumer, session );
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return hasPartitionedSelectionMapping;
	}

	public abstract boolean isTableCascadeDeleteEnabled(int j);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// State built and stored here during instantiation, and only used in other
	// phases of initialization
	//		- postConstruct
	//		- postInstantiate
	//		- prepareMappingModel
	//		- ...
	//
	// This is effectively bootstrap state that is kept around during runtime.
	//
	// Would be better to encapsulate and store this state relative to the
	// `PersisterCreationContext` so it can get released after bootstrap
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Deprecated protected Expectation[] insertExpectations;
	@Deprecated protected Expectation[] updateExpectations;
	@Deprecated protected Expectation[] deleteExpectations;

	@Deprecated protected boolean[] insertCallable;
	@Deprecated protected boolean[] updateCallable;
	@Deprecated protected boolean[] deleteCallable;

	@Deprecated protected String[] customSQLInsert;
	@Deprecated protected String[] customSQLUpdate;
	@Deprecated protected String[] customSQLDelete;



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// State related to this we handle differently in 6+.  In other words, state
	// that is no longer needed
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Deprecated private final String[] subclassColumnAliasClosure;
	@Deprecated private final String[] subclassFormulaAliasClosure;
	@Deprecated private final Map<String,String[]> subclassPropertyAliases = new HashMap<>();

	/**
	 * @deprecated Hibernate no longer uses aliases to read from result sets
	 */
	@Deprecated	protected String[] getSubclassColumnAliasClosure() {
		return subclassColumnAliasClosure;
	}

	/**
	 * @deprecated Hibernate no longer uses aliases to read from result sets
	 */
	@Deprecated	protected String[] getSubclassFormulaAliasClosure() {
		return subclassFormulaAliasClosure;
	}

	/**
	 * @deprecated Hibernate no longer uses aliases to read from result sets
	 */
	@Deprecated	@Override
	public String[] getSubclassPropertyColumnAliases(String propertyName, String suffix) {
		final String[] rawAliases = subclassPropertyAliases.get( propertyName );
		if ( rawAliases == null ) {
			return null;
		}
		else {
			final String[] result = new String[rawAliases.length];
			for ( int i = 0; i < rawAliases.length; i++ ) {
				result[i] = new Alias( suffix ).toUnquotedAliasString( rawAliases[i] );
			}
			return result;
		}
	}

	/**
	 * Must be called by subclasses, at the end of their constructors
	 *
	 * @deprecated Hibernate no longer uses aliases to read from result sets
	 */
	@Deprecated	protected void initSubclassPropertyAliasesMap(PersistentClass model) throws MappingException {

		// ALIASES
		internalInitSubclassPropertyAliasesMap( null, model.getSubclassPropertyClosure() );

		// aliases for identifier ( alias.id ); skip if the entity defines a non-id property named 'id'
		if ( !hasNonIdentifierPropertyNamedId() ) {
			subclassPropertyAliases.put( ENTITY_ID, getIdentifierAliases() );
		}

		// aliases named identifier ( alias.idname )
		if ( hasIdentifierProperty() ) {
			subclassPropertyAliases.put( getIdentifierPropertyName(), getIdentifierAliases() );
		}

		// aliases for composite-id's
		if ( getIdentifierType() instanceof ComponentType componentId ) {
			// Fetch embedded identifiers property names from the "virtual" identifier component
			final String[] idPropertyNames = componentId.getPropertyNames();
			final String[] idAliases = getIdentifierAliases();

			for ( int i = 0; i < idPropertyNames.length; i++ ) {
				if ( hasNonIdentifierPropertyNamedId() ) {
					subclassPropertyAliases.put(
							ENTITY_ID + "." + idPropertyNames[i],
							new String[] {idAliases[i]}
					);
				}
//				if (hasIdentifierProperty() && !ENTITY_ID.equals( getIdentifierPropertyNames() ) ) {
				if ( hasIdentifierProperty() ) {
					subclassPropertyAliases.put(
							getIdentifierPropertyName() + "." + idPropertyNames[i],
							new String[] {idAliases[i]}
					);
				}
				else {
					// embedded composite ids ( alias.idName1, alias.idName2 )
					subclassPropertyAliases.put( idPropertyNames[i], new String[] {idAliases[i]} );
				}
			}
		}

		if ( isPolymorphic() ) {
			subclassPropertyAliases.put( ENTITY_CLASS, new String[] {getDiscriminatorAlias()} );
		}

	}

	private void internalInitSubclassPropertyAliasesMap(String path, List<Property> properties) {
		for ( var property : properties ) {
			final String name = path == null ? property.getName() : path + "." + property.getName();
			if ( property.isComposite() ) {
				final var component = (Component) property.getValue();
				internalInitSubclassPropertyAliasesMap( name, component.getProperties() );
			}

			final String[] aliases = new String[property.getColumnSpan()];
			int l = 0;
			final var dialect = getDialect();
			for ( var selectable: property.getSelectables() ) {
				aliases[l] = selectable.getAlias( dialect, property.getValue().getTable() );
				l++;
			}

			subclassPropertyAliases.put( name, aliases );
		}

	}

	public String getDiscriminatorAlias() {
		return DISCRIMINATOR_ALIAS;
	}

	protected String getSqlWhereStringTableExpression(){
		return sqlWhereStringTableExpression;
	}

	@Override
	public boolean managesColumns(String[] columnNames) {
		for ( String columnName : columnNames ) {
			if ( !writesToColumn( columnName ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean writesToColumn(String columnName) {
		if ( contains( rootTableKeyColumnNames, columnName ) ) {
			return true;
		}
		for ( int i = 0; i < propertyColumnNames.length; i++ ) {
			if ( contains( propertyColumnNames[i], columnName )
					&& isAllTrue( propertyColumnInsertable[i] )
					&& isAllTrue( propertyColumnUpdateable[i] ) ) {
				return true;
			}
		}
		return false;
	}
}
