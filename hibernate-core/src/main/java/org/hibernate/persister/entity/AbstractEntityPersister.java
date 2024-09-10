/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
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

import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.JDBCException;
import org.hibernate.LazyInitializationException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.PropertyValueException;
import org.hibernate.QueryException;
import org.hibernate.Remove;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.internal.SoftDeleteHelper;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementHelper;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeDescriptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributesMetadata;
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
import org.hibernate.classic.Lifecycle;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.internal.CacheHelper;
import org.hibernate.engine.internal.ImmutableEntityEntryFactory;
import org.hibernate.engine.internal.MutableEntityEntryFactory;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.profile.internal.FetchProfileAffectee;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.NaturalIdResolutions;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.internal.VersionGeneration;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.internal.GeneratedValuesHelper;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.Assigned;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.ForeignGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.FilterHelper;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.internal.util.LazyValue;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.LockModeEnumMap;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.TooManyRowsAffectedException;
import org.hibernate.loader.ast.internal.CacheEntityLoaderHelper;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.loader.ast.internal.LoaderSqlAstCreationState;
import org.hibernate.loader.ast.internal.MultiIdEntityLoaderArrayParam;
import org.hibernate.loader.ast.internal.MultiIdEntityLoaderStandard;
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
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.AttributeMappingsMap;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
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
import org.hibernate.metamodel.mapping.MappedDiscriminatorConverter;
import org.hibernate.metamodel.mapping.MappingModelHelper;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.mapping.internal.BasicEntityIdentifierMappingImpl;
import org.hibernate.metamodel.mapping.internal.CompoundNaturalIdMapping;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
import org.hibernate.metamodel.mapping.internal.DiscriminatorTypeImpl;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.loader.ast.internal.EntityConcreteTypeLoader;
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
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
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
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.query.PathException;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sql.internal.SQLQueryParser;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategyProvider;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.Alias;
import org.hibernate.sql.Delete;
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
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.entity.internal.EntityResultImpl;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.stat.spi.StatisticsImplementor;
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
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.processIfManagedEntity;
import static org.hibernate.engine.internal.ManagedTypeHelper.processIfPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.processIfSelfDirtinessTracker;
import static org.hibernate.engine.internal.Versioning.isVersionIncrementRequired;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;
import static org.hibernate.internal.util.ReflectHelper.isAbstractClass;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.qualifyConditionally;
import static org.hibernate.internal.util.collections.ArrayHelper.contains;
import static org.hibernate.internal.util.collections.ArrayHelper.to2DStringArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toBooleanArray;
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
import static org.hibernate.persister.entity.DiscriminatorHelper.NOT_NULL_DISCRIMINATOR;
import static org.hibernate.persister.entity.DiscriminatorHelper.NULL_DISCRIMINATOR;
import static org.hibernate.pretty.MessageHelper.infoString;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * Basic functionality for persisting an entity via JDBC, using either generated or custom SQL.
 *
 * @author Gavin King
 */
@Internal
public abstract class AbstractEntityPersister
		implements InFlightEntityMappingType, EntityMutationTarget, LazyPropertyInitializer, PostInsertIdentityPersister, FetchProfileAffectee, DeprecatedEntityStuff {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractEntityPersister.class );

	public static final String ENTITY_CLASS = "class";
	public static final String VERSION_COLUMN_ALIAS = "version_";

	private final NavigableRole navigableRole;
	private final SessionFactoryImplementor factory;
	private final EntityEntryFactory entityEntryFactory;

	private final String sqlAliasStem;

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
	private final int[] propertyColumnSpans;
	private final String[][] propertyColumnAliases;
	private final String[][] propertyColumnNames;
	private final String[][] propertyColumnFormulaTemplates;
	private final String[][] propertyColumnWriters;
	private final boolean[][] propertyColumnUpdateable;
	private final boolean[][] propertyColumnInsertable;
	private final Set<String> sharedColumnNames;

	private final List<Integer> lobProperties;

	//information about lazy properties of this class
	private final String[] lazyPropertyNames;
	private final int[] lazyPropertyNumbers;
	private final Type[] lazyPropertyTypes;
	private final String[][] lazyPropertyColumnAliases;
	private final Set<String> nonLazyPropertyNames;

	//information about all properties in class hierarchy
	private final String[] subclassPropertyNameClosure;
	private final Type[] subclassPropertyTypeClosure;
	private final String[][] subclassPropertyFormulaTemplateClosure;
	private final String[][] subclassPropertyColumnNameClosure;
	private final String[][] subclassPropertyColumnReaderClosure;
	private final String[][] subclassPropertyColumnReaderTemplateClosure;
	private final FetchMode[] subclassPropertyFetchModeClosure;
	private final boolean[] subclassPropertyNullabilityClosure;
	private final boolean[] propertyDefinedOnSubclass;
	private final CascadeStyle[] subclassPropertyCascadeStyleClosure;

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

	private boolean[] tableHasColumns;

	private final Map<String,String[]> subclassPropertyColumnNames = new HashMap<>();

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
	protected final BasicEntityPropertyMapping propertyMapping;

	private final boolean implementsLifecycle;

	private List<UniqueKeyEntry> uniqueKeyEntries = null; //lazily initialized
	private ConcurrentHashMap<String,SingleIdArrayLoadPlan> nonLazyPropertyLoadPlansByName;

	@Deprecated(since = "6.0")
	public AbstractEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final PersisterCreationContext creationContext) throws HibernateException {
		this( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy,
				(RuntimeModelCreationContext) creationContext );
	}

	public AbstractEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final RuntimeModelCreationContext creationContext) throws HibernateException {

		//set it here, but don't call it, since it's still uninitialized!
		factory = creationContext.getSessionFactory();

		sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromEntityName( persistentClass.getEntityName() );

		navigableRole = new NavigableRole( persistentClass.getEntityName() );

		final SessionFactoryOptions sessionFactoryOptions = creationContext.getSessionFactoryOptions();

		if ( sessionFactoryOptions.isSecondLevelCacheEnabled() ) {
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

		entityMetamodel = new EntityMetamodel( persistentClass, this, creationContext );

		entityEntryFactory = entityMetamodel.isMutable()
				? MutableEntityEntryFactory.INSTANCE
				: ImmutableEntityEntryFactory.INSTANCE;

		// Handle any filters applied to the class level
		filterHelper = isNotEmpty( persistentClass.getFilters() ) ? new FilterHelper(
				persistentClass.getFilters(),
				getEntityNameByTableNameMap(
						persistentClass,
						factory.getSqlStringGenerationContext()
				),
				factory
		) : null;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		representationStrategy = creationContext.getBootstrapContext().getRepresentationStrategySelector()
				.resolveStrategy( persistentClass, this, creationContext );

		javaType = representationStrategy.getLoadJavaType();
		assert javaType != null;
		this.implementsLifecycle = Lifecycle.class.isAssignableFrom( javaType.getJavaTypeClass() );

		concreteProxy = isPolymorphic()
				&& ( getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() || hasProxy() )
				&& persistentClass.isConcreteProxy();

		final Dialect dialect = creationContext.getDialect();

		batchSize = persistentClass.getBatchSize() < 0
				? factory.getSessionFactoryOptions().getDefaultBatchFetchSize()
				: persistentClass.getBatchSize();
		hasSubselectLoadableCollections = persistentClass.hasSubselectLoadableCollections();
		hasPartitionedSelectionMapping = persistentClass.hasPartitionedSelectionMapping();
		hasCollectionNotReferencingPK = persistentClass.hasCollectionNotReferencingPK();

		propertyMapping = new BasicEntityPropertyMapping( this );

		// IDENTIFIER

		identifierColumnSpan = persistentClass.getIdentifier().getColumnSpan();
		rootTableKeyColumnNames = new String[identifierColumnSpan];
		rootTableKeyColumnReaders = new String[identifierColumnSpan];
		rootTableKeyColumnReaderTemplates = new String[identifierColumnSpan];
		identifierAliases = new String[identifierColumnSpan];

		final String rowId = persistentClass.getRootTable().getRowId();
		rowIdName = rowId == null ? null : dialect.rowId( rowId );

		queryLoaderName = persistentClass.getLoaderName();

		final TypeConfiguration typeConfiguration = creationContext.getTypeConfiguration();
		final SqmFunctionRegistry functionRegistry = creationContext.getFunctionRegistry();

		List<Column> columns = persistentClass.getIdentifier().getColumns();
		for (int i = 0; i < columns.size(); i++ ) {
			Column column = columns.get(i);
			rootTableKeyColumnNames[i] = column.getQuotedName( dialect );
			rootTableKeyColumnReaders[i] = column.getReadExpr( dialect );
			rootTableKeyColumnReaderTemplates[i] = column.getTemplate(
					dialect,
					typeConfiguration,
					functionRegistry
			);
			identifierAliases[i] = column.getAlias( dialect, persistentClass.getRootTable() );
		}

		// VERSION

		versionColumnName = persistentClass.isVersioned()
				? persistentClass.getVersion().getColumns().get(0).getQuotedName( dialect )
				: null;

		//WHERE STRING

		if ( isEmpty( persistentClass.getWhere() ) ) {
			sqlWhereStringTableExpression = null;
			sqlWhereStringTemplate = null;
		}
		else {
			PersistentClass containingClass = persistentClass;
			while ( containingClass.getSuperclass() != null ) {
				final PersistentClass superclass = containingClass.getSuperclass();
				if ( !Objects.equals( persistentClass.getWhere(), superclass.getWhere() ) ) {
					break;
				}
				containingClass = superclass;
			}
			sqlWhereStringTableExpression = determineTableName( containingClass.getTable() );
			sqlWhereStringTemplate = Template.renderWhereStringTemplate(
					"(" + persistentClass.getWhere() + ")",
					dialect,
					typeConfiguration,
					functionRegistry
			);
		}

		// PROPERTIES
		final int hydrateSpan = entityMetamodel.getPropertySpan();
		propertyColumnSpans = new int[hydrateSpan];
		propertyColumnAliases = new String[hydrateSpan][];
		propertyColumnNames = new String[hydrateSpan][];
		propertyColumnFormulaTemplates = new String[hydrateSpan][];
		propertyColumnWriters = new String[hydrateSpan][];
		propertyColumnUpdateable = new boolean[hydrateSpan][];
		propertyColumnInsertable = new boolean[hydrateSpan][];
		sharedColumnNames = new HashSet<>();
		nonLazyPropertyNames = new HashSet<>();

		final HashSet<Property> thisClassProperties = new HashSet<>();
		final ArrayList<String> lazyNames = new ArrayList<>();
		final ArrayList<Integer> lazyNumbers = new ArrayList<>();
		final ArrayList<Type> lazyTypes = new ArrayList<>();
		final ArrayList<String[]> lazyColAliases = new ArrayList<>();

		final ArrayList<Integer> lobPropertiesLocalCollector = new ArrayList<>();
		final List<Property> propertyClosure = persistentClass.getPropertyClosure();
		boolean foundFormula = false;
		for ( int i = 0; i < propertyClosure.size(); i++ ) {
			final Property prop = propertyClosure.get(i);
			thisClassProperties.add( prop );

			final int span = prop.getColumnSpan();
			propertyColumnSpans[i] = span;

			final String[] colNames = new String[span];
			final String[] colAliases = new String[span];
			final String[] colWriters = new String[span];
			final String[] formulaTemplates = new String[span];
			final List<Selectable> selectables = prop.getSelectables();
			for ( int k = 0; k < selectables.size(); k++ ) {
				final Selectable selectable = selectables.get(k);
				colAliases[k] = selectable.getAlias( dialect, prop.getValue().getTable() );
				if ( selectable.isFormula() ) {
					foundFormula = true;
					final Formula formula = (Formula) selectable;
					formula.setFormula( substituteBrackets( formula.getFormula() ) );
					formulaTemplates[k] = selectable.getTemplate(
							dialect,
							typeConfiguration,
							functionRegistry
					);
				}
				else {
					final Column column = (Column) selectable;
					colNames[k] = column.getQuotedName( dialect );
					colWriters[k] = column.getWriteExpr( prop.getValue().getSelectableType( creationContext.getMetadata(), k ), dialect );
				}
			}
			propertyColumnNames[i] = colNames;
			propertyColumnFormulaTemplates[i] = formulaTemplates;
			propertyColumnWriters[i] = colWriters;
			propertyColumnAliases[i] = colAliases;

			final boolean lazy = !EnhancementHelper.includeInBaseFetchGroup(
					prop,
					entityMetamodel.isInstrumented(),
					entityName -> {
						final PersistentClass entityBinding = creationContext
								.getMetadata()
								.getEntityBinding( entityName );
						assert entityBinding != null;
						return entityBinding.hasSubclasses();
					},
					sessionFactoryOptions.isCollectionsInDefaultFetchGroupEnabled()
			);

			if ( lazy ) {
				lazyNames.add( prop.getName() );
				lazyNumbers.add( i );
				lazyTypes.add( prop.getValue().getType() );
				lazyColAliases.add( colAliases );
			}
			else {
				nonLazyPropertyNames.add( prop.getName() );
			}

			propertyColumnUpdateable[i] = prop.getValue().getColumnUpdateability();
			propertyColumnInsertable[i] = prop.getValue().getColumnInsertability();

			if ( prop.isLob() && dialect.forceLobAsLastValue() ) {
				lobPropertiesLocalCollector.add( i );
			}
		}
		lobProperties = toSmallList( lobPropertiesLocalCollector );
		hasFormulaProperties = foundFormula;
		lazyPropertyColumnAliases = to2DStringArray( lazyColAliases );
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
		final ArrayList<CascadeStyle> cascades = new ArrayList<>();
		final ArrayList<Boolean> definedBySubclass = new ArrayList<>();
		final ArrayList<Boolean> propNullables = new ArrayList<>();

		if ( persistentClass.hasSubclasses() ) {
			for ( Selectable selectable : persistentClass.getIdentifier().getSelectables() ) {
				if ( !selectable.isFormula() ) {
					// Identifier columns are always shared between subclasses
					sharedColumnNames.add( ( (Column) selectable ).getQuotedName( dialect ) );
				}
			}
		}

		for ( Property prop : persistentClass.getSubclassPropertyClosure() ) {
			names.add( prop.getName() );
			types.add( prop.getType() );

			final boolean isDefinedBySubclass = !thisClassProperties.contains( prop );
			definedBySubclass.add( isDefinedBySubclass );
			propNullables.add( prop.isOptional() || isDefinedBySubclass ); //TODO: is this completely correct?

			final String[] cols = new String[ prop.getColumnSpan() ];
			final String[] readers = new String[ prop.getColumnSpan() ];
			final String[] readerTemplates = new String[ prop.getColumnSpan() ];
			final String[] forms = new String[ prop.getColumnSpan() ];

			final List<Selectable> selectables = prop.getSelectables();
			for ( int i = 0; i < selectables.size(); i++ ) {
				final Selectable selectable = selectables.get(i);
				if ( selectable.isFormula() ) {
					final String template = selectable.getTemplate(
							dialect,
							typeConfiguration,
							functionRegistry
					);
					forms[i] = template;
					final String formulaAlias = selectable.getAlias( dialect );
					if ( prop.isSelectable() && !formulaAliases.contains( formulaAlias ) ) {
						formulaAliases.add( formulaAlias );
					}
				}
				else {
					final Column column = (Column) selectable;
					final String colName = column.getQuotedName(dialect);
					cols[i] = colName;
					final String columnAlias = selectable.getAlias( dialect, prop.getValue().getTable() );
					if ( prop.isSelectable() && !aliases.contains( columnAlias ) ) {
						aliases.add( columnAlias );
					}

					readers[i] = column.getReadExpr( dialect );
					readerTemplates[i] = column.getTemplate(
							dialect,
							typeConfiguration,
							functionRegistry
					);
					if ( isDefinedBySubclass && persistentClass.isDefinedOnMultipleSubclasses( column )
							|| !isDefinedBySubclass && persistentClass.hasSubclasses() ) {
						sharedColumnNames.add( colName );
					}
				}
			}
			propColumns.add( cols );
			propColumnReaders.add( readers );
			propColumnReaderTemplates.add( readerTemplates );
			templates.add( forms );

			joinedFetchesList.add( prop.getValue().getFetchMode() );
			cascades.add( prop.getCascadeStyle() );
		}
		subclassColumnAliasClosure = toStringArray( aliases );
		subclassFormulaAliasClosure = toStringArray( formulaAliases );

		subclassPropertyNameClosure = toStringArray( names );
		subclassPropertyTypeClosure = toTypeArray( types );
		subclassPropertyNullabilityClosure = toBooleanArray( propNullables );
		subclassPropertyFormulaTemplateClosure = to2DStringArray( templates );
		subclassPropertyColumnNameClosure = to2DStringArray( propColumns );
		subclassPropertyColumnReaderClosure = to2DStringArray( propColumnReaders );
		subclassPropertyColumnReaderTemplateClosure = to2DStringArray( propColumnReaderTemplates );

		subclassPropertyCascadeStyleClosure = new CascadeStyle[cascades.size()];
		int j = 0;
		for (CascadeStyle cascade: cascades) {
			subclassPropertyCascadeStyleClosure[j++] = cascade;
		}
		subclassPropertyFetchModeClosure = new FetchMode[joinedFetchesList.size()];
		j = 0;
		for (FetchMode fetchMode : joinedFetchesList) {
			subclassPropertyFetchModeClosure[j++] = fetchMode;
		}

		propertyDefinedOnSubclass = toBooleanArray( definedBySubclass );

		useReferenceCacheEntries = shouldUseReferenceCacheEntries( creationContext.getSessionFactoryOptions() );
		useShallowQueryCacheLayout = shouldUseShallowCacheLayout(
				persistentClass.getQueryCacheLayout(),
				creationContext.getSessionFactoryOptions()
		);
		storeDiscriminatorInShallowQueryCacheLayout = shouldStoreDiscriminatorInShallowQueryCacheLayout(
				persistentClass.getQueryCacheLayout(),
				creationContext.getSessionFactoryOptions()
		);
		cacheEntryHelper = buildCacheEntryHelper( creationContext.getSessionFactoryOptions() );
		invalidateCache = sessionFactoryOptions.isSecondLevelCacheEnabled()
				&& canWriteToCache
				&& shouldInvalidateCache( persistentClass, creationContext );

		final List<Object> values = new ArrayList<>();
		final List<String> sqlValues = new ArrayList<>();

		if ( persistentClass.isPolymorphic() && persistentClass.getDiscriminator() != null ) {
			if ( !getEntityMetamodel().isAbstract() ) {
				values.add( DiscriminatorHelper.getDiscriminatorValue( persistentClass ) );
				sqlValues.add( DiscriminatorHelper.getDiscriminatorSQLValue( persistentClass, dialect ) );
			}

			final List<Subclass> subclasses = persistentClass.getSubclasses();
			for ( int k = 0; k < subclasses.size(); k++ ) {
				final Subclass subclass = subclasses.get( k );
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

	private NamedQueryMemento getNamedQueryMemento(MetadataImplementor bootModel) {
		final NamedQueryMemento memento =
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
			final NamedQueryMemento memento = getNamedQueryMemento( null );
			return new SingleIdEntityLoaderProvidedQueryImpl<>( this, memento );
		}
		return buildSingleIdEntityLoader( new LoadQueryInfluencers( factory ) );
	}

	private SingleIdEntityLoader<?> buildSingleIdEntityLoader(LoadQueryInfluencers loadQueryInfluencers) {
		if ( loadQueryInfluencers.effectivelyBatchLoadable( this ) ) {
			final int batchSize = loadQueryInfluencers.effectiveBatchSize( this );
			return factory.getServiceRegistry()
					.requireService( BatchLoaderFactory.class )
					.createEntityBatchLoader( batchSize, this, loadQueryInfluencers );
		}
		else {
			return new SingleIdEntityLoaderStandardImpl<>( this, loadQueryInfluencers );
		}
	}

	public static Map<String, String> getEntityNameByTableNameMap(
			PersistentClass persistentClass,
			SqlStringGenerationContext stringGenerationContext) {
		final Map<String, String> entityNameByTableNameMap = new HashMap<>();
		PersistentClass superType = persistentClass.getSuperPersistentClass();
		while ( superType != null ) {
			entityNameByTableNameMap.put( superType.getTable().getQualifiedName( stringGenerationContext ), superType.getEntityName() );
			for ( Join join : superType.getJoins() ) {
				entityNameByTableNameMap.put( join.getTable().getQualifiedName( stringGenerationContext ), superType.getEntityName() );
			}
			superType = superType.getSuperPersistentClass();
		}
		for ( PersistentClass subclass : persistentClass.getSubclassClosure() ) {
			entityNameByTableNameMap.put( subclass.getTable().getQualifiedName( stringGenerationContext ), subclass.getEntityName() );
			for ( Join join : subclass.getJoins() ) {
				entityNameByTableNameMap.put( join.getTable().getQualifiedName( stringGenerationContext ), subclass.getEntityName() );
			}
		}
		return entityNameByTableNameMap;
	}

	protected MultiIdEntityLoader<Object> buildMultiIdLoader() {
		if ( getIdentifierType() instanceof BasicType
				&& supportsSqlArrayType( factory.getJdbcServices().getDialect() ) ) {
			return new MultiIdEntityLoaderArrayParam<>( this, factory );
		}
		else {
			return new MultiIdEntityLoaderStandard<>( this, identifierColumnSpan, factory );
		}
	}

	private String getIdentitySelectString(Dialect dialect) {
		try {
			return dialect.getIdentityColumnSupport()
					.getIdentitySelectString(
							getTableName(0),
							getKeyColumns(0)[0],
							( (BasicType<?>) getIdentifierType() ).getJdbcType().getDdlTypeCode()
					);
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
		else if ( entityMetamodel.isMutable() ) {
			// 1) are immutable
			return false;
		}
		else {
			// 2) have no associations.
			// Eventually we want to be a little more lenient with associations.
			for ( Type type : getSubclassPropertyTypeClosure() ) {
				if ( type instanceof AnyType || type instanceof CollectionType || type instanceof EntityType ) {
					return false;
				}
			}
			return true;
		}
	}

	private boolean shouldUseShallowCacheLayout(CacheLayout entityQueryCacheLayout, SessionFactoryOptions options) {
		final CacheLayout queryCacheLayout;
		if ( entityQueryCacheLayout == null ) {
			queryCacheLayout = options.getQueryCacheLayout();
		}
		else {
			queryCacheLayout = entityQueryCacheLayout;
		}
		return queryCacheLayout == CacheLayout.SHALLOW || queryCacheLayout == CacheLayout.SHALLOW_WITH_DISCRIMINATOR
				|| queryCacheLayout == CacheLayout.AUTO && ( canUseReferenceCacheEntries() || canReadFromCache() );
	}

	private boolean shouldStoreDiscriminatorInShallowQueryCacheLayout(CacheLayout entityQueryCacheLayout, SessionFactoryOptions options) {
		final CacheLayout queryCacheLayout;
		if ( entityQueryCacheLayout == null ) {
			queryCacheLayout = options.getQueryCacheLayout();
		}
		else {
			queryCacheLayout = entityQueryCacheLayout;
		}
		return queryCacheLayout == CacheLayout.SHALLOW_WITH_DISCRIMINATOR;
	}

	@Override
	public abstract String getSubclassTableName(int j);

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

	public abstract int getSubclassTableSpan();

	public abstract int getTableSpan();

	public abstract boolean hasDuplicateTables();

	/**
	 * @deprecated Only ever used from places where we really want to use<ul>
	 *     <li>{@link SelectStatement} (select generator)</li>
	 *     <li>{@link InsertSelectStatement}</li>
	 *     <li>{@link org.hibernate.sql.ast.tree.update.UpdateStatement}</li>
	 *     <li>{@link org.hibernate.sql.ast.tree.delete.DeleteStatement}</li>
	 * </ul>
	 */
	@Deprecated( since = "6.2" )
	public abstract String getTableName(int j);

	public abstract String[] getKeyColumns(int j);

	public abstract boolean isPropertyOfTable(int property, int j);

	protected abstract int[] getPropertyTableNumbers();

	protected abstract int getSubclassPropertyTableNumber(int i);

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

	public boolean isInverseTable(int j) {
		return false;
	}

	public boolean isNullableTable(int j) {
		return false;
	}

	protected boolean isNullableSubclassTable(int j) {
		return false;
	}

	@Override
	public boolean isSubclassEntityName(String entityName) {
		return entityMetamodel.getSubclassEntityNames().contains( entityName );
	}

	public boolean isSharedColumn(String columnExpression) {
		return sharedColumnNames.contains( columnExpression );
	}

	protected boolean[] getTableHasColumns() {
		return tableHasColumns;
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

	@Internal
	public GeneratedValuesProcessor getInsertGeneratedValuesProcessor() {
		return insertGeneratedValuesProcessor;
	}

	@Internal
	public GeneratedValuesProcessor getUpdateGeneratedValuesProcessor() {
		return updateGeneratedValuesProcessor;
	}

	@Override
	public boolean hasRowId() {
		return rowIdName != null;
	}

	/**
	 * For Hibernate Reactive
	 */
	@SuppressWarnings("unused")
	public boolean[][] getPropertyColumnUpdateable() {
		return propertyColumnUpdateable;
	}

	/**
	 * For Hibernate Reactive
	 */
	@SuppressWarnings("unused")
	public boolean[][] getPropertyColumnInsertable() {
		return propertyColumnInsertable;
	}

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
		else if ( entityMetamodel.isDynamicUpdate() ) {
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
			for ( Subclass subclass : persistentClass.getSubclasses() ) {
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
			entityMetamodel.setLazy( false );
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
		if ( this.uniqueKeyEntries == null ) {
			this.uniqueKeyEntries = initUniqueKeyEntries( this );
		}
		return this.uniqueKeyEntries;
	}

	private static List<UniqueKeyEntry> initUniqueKeyEntries(final AbstractEntityPersister aep) {
		final ArrayList<UniqueKeyEntry> uniqueKeys = new ArrayList<>();
		for ( Type propertyType : aep.getPropertyTypes() ) {
			if ( propertyType instanceof AssociationType ) {
				final AssociationType associationType = (AssociationType) propertyType;
				final String ukName = associationType.getLHSPropertyName();
				if ( ukName != null ) {
					final AttributeMapping attributeMapping = aep.findAttributeMapping( ukName );
					if ( attributeMapping != null ) {
						final int index = attributeMapping.getStateArrayPosition();
						final Type type = aep.getPropertyTypes()[index];
						uniqueKeys.add( new UniqueKeyEntry( ukName, index, type ) );
					}
				}
				else if ( associationType instanceof ManyToOneType ) {
					final ManyToOneType manyToOneType = ( (ManyToOneType) associationType );
					if ( manyToOneType.isLogicalOneToOne() && manyToOneType.isReferenceToPrimaryKey() ) {
						final AttributeMapping attributeMapping = aep.findAttributeMapping( manyToOneType.getPropertyName() );
						if ( attributeMapping != null ) {
							final int index = attributeMapping.getStateArrayPosition();
							final Type type = aep.getPropertyTypes()[index];
							uniqueKeys.add( new UniqueKeyEntry( manyToOneType.getPropertyName(), index, type ) );
						}
					}
				}
			}
		}
		return CollectionHelper.toSmallList( uniqueKeys );
	}

	protected Map<String, SingleIdArrayLoadPlan> getLazyLoadPlanByFetchGroup() {
		final BytecodeEnhancementMetadata metadata = entityMetamodel.getBytecodeEnhancementMetadata();
		return metadata.isEnhancedForLazyLoading() && metadata.getLazyAttributesMetadata().hasLazyAttributes()
				? createLazyLoadPlanByFetchGroup( metadata )
				: emptyMap();
	}

	private Map<String, SingleIdArrayLoadPlan> createLazyLoadPlanByFetchGroup(BytecodeEnhancementMetadata metadata) {
		final Map<String, SingleIdArrayLoadPlan> result = new HashMap<>();
		final LazyAttributesMetadata attributesMetadata = metadata.getLazyAttributesMetadata();
		for ( String groupName : attributesMetadata.getFetchGroupNames() ) {
			final SingleIdArrayLoadPlan loadPlan =
					createLazyLoadPlan( attributesMetadata.getFetchGroupAttributeDescriptors( groupName ) );
			if ( loadPlan != null ) {
				result.put( groupName, loadPlan );
			}
		}
		return result;
	}

	private SingleIdArrayLoadPlan createLazyLoadPlan(List<LazyAttributeDescriptor> fetchGroupAttributeDescriptors) {
		final List<ModelPart> partsToSelect = new ArrayList<>( fetchGroupAttributeDescriptors.size() );
		for ( LazyAttributeDescriptor lazyAttributeDescriptor : fetchGroupAttributeDescriptors ) {
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
			JdbcParametersList.Builder jdbcParametersBuilder = JdbcParametersList.newBuilder();
			final SelectStatement select = LoaderSelectBuilder.createSelect(
					this,
					partsToSelect,
					getIdentifierMapping(),
					null,
					1,
					new LoadQueryInfluencers( factory ),
					LockOptions.NONE,
					jdbcParametersBuilder::add,
					factory
			);
			JdbcParametersList jdbcParameters = jdbcParametersBuilder.build();
			return new SingleIdArrayLoadPlan(
					this,
					getIdentifierMapping(),
					select,
					jdbcParameters,
					LockOptions.NONE,
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
		final EntityResultImpl entityResult = new EntityResultImpl(
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
			final String subclassTableName = getSubclassTableName( i );
			if ( subclassTableName.equals( joinTableExpression ) ) {
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
		final NamedTableReference joinedTableReference = new NamedTableReference(
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
						getKeyColumnNames(),
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
		final EntityIdentifierMapping identifierMapping = getIdentifierMapping();

		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );

		assert pkColumnNames.length == fkColumnNames.length;
		assert pkColumnNames.length == identifierMapping.getJdbcTypeCount();

		identifierMapping.forEachSelectable(
				(columnIndex, selection) -> {
					final String rootPkColumnName = pkColumnNames[ columnIndex ];
					final Expression pkColumnExpression = creationState.getSqlExpressionResolver().resolveSqlExpression(
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
					final Expression fkColumnExpression = creationState.getSqlExpressionResolver().resolveSqlExpression(
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
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final EntityEntry entry = persistenceContext.getEntry( entity );
		final PersistentAttributeInterceptor interceptor = asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
		assert interceptor != null : "Expecting bytecode interceptor to be non-null";

		if ( hasCollections() ) {
			final Type type = getPropertyType( fieldName );
			if ( type instanceof CollectionType ) {
				// we have a condition where a collection attribute is being access via enhancement:
				// 		we can circumvent all the rest and just return the PersistentCollection
				final CollectionType collectionType = (CollectionType) type;
				final CollectionPersister persister = factory.getRuntimeMetamodels()
						.getMappingMetamodel()
						.getCollectionDescriptor( collectionType.getRole() );

				// Get/create the collection, and make sure it is initialized!  This initialized part is
				// different from proxy-based scenarios where we have to create the PersistentCollection
				// reference "ahead of time" to add as a reference to the proxy.  For bytecode solutions
				// we are not creating the PersistentCollection ahead of time, but instead we are creating
				// it on first request through the enhanced entity.

				// see if there is already a collection instance associated with the session
				// 		NOTE : can this ever happen?
				final Object key = getCollectionKey( persister, entity, entry, session );
				assert key != null;
				PersistentCollection<?> collection = persistenceContext.getCollection( new CollectionKey( persister, key ) );
				if ( collection == null ) {
					collection = collectionType.instantiate( session, persister, key );
					collection.setOwner( entity );
					persistenceContext.addUninitializedCollection( persister, collection, key );
				}

//				// HHH-11161 Initialize, if the collection is not extra lazy
//				if ( !persister.isExtraLazy() ) {
//					session.initializeCollection( collection, false );
//				}
				interceptor.attributeInitialized( fieldName );

				if ( collectionType.isArrayType() ) {
					persistenceContext.addCollectionHolder( collection );
				}

				// update the "state" of the entity's EntityEntry to over-write UNFETCHED_PROPERTY reference
				// for the collection to the just loaded collection
				final EntityEntry ownerEntry = persistenceContext.getEntry( entity );
				if ( ownerEntry == null ) {
					// the entity is not in the session; it was probably deleted,
					// so we cannot load the collection anymore.
					throw new LazyInitializationException(
							"Could not locate EntityEntry for the collection owner in the PersistenceContext"
					);
				}
				ownerEntry.overwriteLoadedStateCollectionValue( fieldName, collection );

				// EARLY EXIT!!!
				return collection;
			}
		}

		final Object id = session.getContextEntityIdentifier( entity );
		if ( entry == null ) {
			throw new HibernateException( "entity is not associated with the session: " + id );
		}

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev(
					"Initializing lazy properties of: {0}, field access: {1}",
					infoString( this, id, getFactory() ),
					fieldName
			);
		}

		if ( session.getCacheMode().isGetEnabled() && canReadFromCache() && isLazyPropertiesCacheable() ) {
			final EntityDataAccess cacheAccess = getCacheAccessStrategy();
			final Object cacheKey = cacheAccess.generateCacheKey(id, this, session.getFactory(), session.getTenantIdentifier() );
			final Object ce = CacheHelper.fromSharedCache( session, cacheKey, this, cacheAccess );
			if ( ce != null ) {
				final CacheEntry cacheEntry = (CacheEntry) getCacheEntryStructure().destructure( ce, factory );
				final Object initializedValue = initializeLazyPropertiesFromCache( fieldName, entity, session, entry, cacheEntry );
				if (initializedValue != LazyPropertyInitializer.UNFETCHED_PROPERTY) {
					// The following should be redundant, since the setter should have set this already.
					// interceptor.attributeInitialized(fieldName);

					// NOTE EARLY EXIT!!!
					return initializedValue;
				}
			}
		}

		return initializeLazyPropertiesFromDatastore( entity, id, entry, fieldName, session );

	}

	public @Nullable Object getCollectionKey(
			CollectionPersister persister,
			Object owner,
			EntityEntry ownerEntry,
			SharedSessionContractImplementor session) {
		final CollectionType collectionType = persister.getCollectionType();

		if ( ownerEntry != null ) {
			// this call only works when the owner is associated with the Session, which is not always the case
			return collectionType.getKeyOfOwner( owner, session );
		}

		final EntityPersister ownerPersister = persister.getOwnerEntityPersister();
		if ( collectionType.getLHSPropertyName() == null ) {
			// collection key is defined by the owning entity identifier
			return ownerPersister.getIdentifier( owner, session );
		}
		else {
			return ownerPersister.getPropertyValue( owner, collectionType.getLHSPropertyName() );
		}
	}

	protected Object initializeLazyPropertiesFromDatastore(
			final Object entity,
			final Object id,
			final EntityEntry entry,
			final String fieldName,
			final SharedSessionContractImplementor session) {
		if ( nonLazyPropertyNames.contains( fieldName ) ) {
			// An eager property can be lazy because of an applied EntityGraph
			final List<ModelPart> partsToSelect = new ArrayList<>(1);
			int propertyIndex = getPropertyIndex( fieldName );
			partsToSelect.add( getAttributeMapping( propertyIndex ) );
			SingleIdArrayLoadPlan lazyLoanPlan;
			ConcurrentHashMap<String, SingleIdArrayLoadPlan> propertyLoadPlansByName = this.nonLazyPropertyLoadPlansByName;
			if ( propertyLoadPlansByName == null ) {
				propertyLoadPlansByName = new ConcurrentHashMap<>();
				lazyLoanPlan = createLazyLoanPlan( partsToSelect );
				propertyLoadPlansByName.put( fieldName, lazyLoanPlan );
				this.nonLazyPropertyLoadPlansByName = propertyLoadPlansByName;
			}
			else {
				lazyLoanPlan = nonLazyPropertyLoadPlansByName.get( fieldName );
				if ( lazyLoanPlan == null ) {
					lazyLoanPlan = createLazyLoanPlan( partsToSelect );
					nonLazyPropertyLoadPlansByName.put( fieldName, lazyLoanPlan );
				}
			}
			try {
				final Object[] values = lazyLoanPlan.load( id, session );
				final Object selectedValue = values[0];
				initializeLazyProperty(
						entity,
						entry,
						selectedValue,
						propertyIndex,
						getPropertyTypes()[propertyIndex]
				);
				return selectedValue;
			}
			catch (JDBCException ex) {
				throw session.getJdbcServices().getSqlExceptionHelper().convert(
						ex.getSQLException(),
						"could not initialize lazy properties: " + infoString( this, id, getFactory() ),
						lazyLoanPlan.getJdbcSelect().getSqlString()
				);
			}
		}
		else {
			if ( !hasLazyProperties() ) {
				throw new AssertionFailure( "no lazy properties" );
			}

			final PersistentAttributeInterceptor interceptor = asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
			assert interceptor != null : "Expecting bytecode interceptor to be non-null";

			LOG.tracef( "Initializing lazy properties from datastore (triggered for `%s`)", fieldName );

			final String fetchGroup = getEntityMetamodel().getBytecodeEnhancementMetadata()
					.getLazyAttributesMetadata()
					.getFetchGroupName( fieldName );
			final List<LazyAttributeDescriptor> fetchGroupAttributeDescriptors = getEntityMetamodel().getBytecodeEnhancementMetadata()
					.getLazyAttributesMetadata()
					.getFetchGroupAttributeDescriptors( fetchGroup );

			final Set<String> initializedLazyAttributeNames = interceptor.getInitializedLazyAttributeNames();

			final SingleIdArrayLoadPlan lazySelect = getSQLLazySelectLoadPlan( fetchGroup );

			try {
				Object result = null;
				final Object[] values = lazySelect.load( id, session );
				int i = 0;
				for ( LazyAttributeDescriptor fetchGroupAttributeDescriptor : fetchGroupAttributeDescriptors ) {
					final boolean previousInitialized = initializedLazyAttributeNames.contains(
							fetchGroupAttributeDescriptor.getName() );

					if ( previousInitialized ) {
						// todo : one thing we should consider here is potentially un-marking an attribute as dirty based on the selected value
						// 		we know the current value - getPropertyValue( entity, fetchGroupAttributeDescriptor.getAttributeIndex() );
						// 		we know the selected value (see selectedValue below)
						//		we can use the attribute Type to tell us if they are the same
						//
						//		assuming entity is a SelfDirtinessTracker we can also know if the attribute is
						//			currently considered dirty, and if really not dirty we would do the un-marking
						//
						//		of course that would mean a new method on SelfDirtinessTracker to allow un-marking

						// its already been initialized (e.g. by a write) so we don't want to overwrite
						i++;
						continue;
					}

					final Object selectedValue = values[i++];
					final boolean set = initializeLazyProperty(
							fieldName,
							entity,
							entry,
							fetchGroupAttributeDescriptor,
							selectedValue
					);
					if ( set ) {
						result = selectedValue;
						interceptor.attributeInitialized( fetchGroupAttributeDescriptor.getName() );
					}
				}

				LOG.trace( "Done initializing lazy properties" );

				return result;

			}
			catch (JDBCException ex) {
				throw session.getJdbcServices().getSqlExceptionHelper().convert(
						ex.getSQLException(),
						"could not initialize lazy properties: " + infoString( this, id, getFactory() ),
						lazySelect.getJdbcSelect().getSqlString()
				);
			}
		}
	}

	protected Object initializeLazyPropertiesFromCache(
			final String fieldName,
			final Object entity,
			final SharedSessionContractImplementor session,
			final EntityEntry entry,
			final CacheEntry cacheEntry) {

		LOG.trace( "Initializing lazy properties from second-level cache" );

		Object result = null;
		Serializable[] disassembledValues = cacheEntry.getDisassembledState();
		for ( int j = 0; j < lazyPropertyNames.length; j++ ) {
			final Serializable cachedValue = disassembledValues[lazyPropertyNumbers[j]];
			final Type lazyPropertyType = lazyPropertyTypes[j];
			final String propertyName = lazyPropertyNames[j];
			if ( cachedValue == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				if ( fieldName.equals(propertyName) ) {
					result = LazyPropertyInitializer.UNFETCHED_PROPERTY;
				}
				// don't try to initialize the unfetched property
			}
			else {
				final Object propValue = lazyPropertyType.assemble( cachedValue, session, entity );
				if ( initializeLazyProperty( fieldName, entity, entry, j, propValue ) ) {
					result = propValue;
				}
			}
		}

		LOG.trace( "Done initializing lazy properties" );

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
		setPropertyValue( entity, lazyPropertyNumbers[index], propValue );
		if ( entry.getLoadedState() != null ) {
			// object have been loaded with setReadOnly(true); HHH-2236
			entry.getLoadedState()[lazyPropertyNumbers[index]] = lazyPropertyTypes[index].deepCopy( propValue, factory );
		}
		// If the entity has deleted state, then update that as well
		if ( entry.getDeletedState() != null ) {
			entry.getDeletedState()[lazyPropertyNumbers[index]] = lazyPropertyTypes[index].deepCopy( propValue, factory );
		}
		return fieldName.equals( lazyPropertyNames[index] );
	}



	protected boolean initializeLazyProperty(
			final String fieldName,
			final Object entity,
			final EntityEntry entry,
			LazyAttributeDescriptor fetchGroupAttributeDescriptor,
			final Object propValue) {
		final String name = fetchGroupAttributeDescriptor.getName();
		initializeLazyProperty(
				entity,
				entry,
				propValue,
				getPropertyIndex( name ),
				fetchGroupAttributeDescriptor.getType()
		);
		return fieldName.equals( name );
	}

	private void initializeLazyProperty(Object entity, EntityEntry entry, Object propValue, int index, Type type) {
		setPropertyValue( entity, index, propValue );
		if ( entry.getLoadedState() != null ) {
			// object have been loaded with setReadOnly(true); HHH-2236
			entry.getLoadedState()[index] = type.deepCopy(
					propValue,
					factory
			);
		}
		// If the entity has deleted state, then update that as well
		if ( entry.getDeletedState() != null ) {
			entry.getDeletedState()[index] = type.deepCopy(
					propValue,
					factory
			);
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
	public boolean isBatchLoadable() {
		return batchSize > 1;
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
		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				LockOptions.NONE,
				this::fetchProcessor,
				true,
				new LoadQueryInfluencers( factory ),
				factory
		);

		final NavigablePath entityPath = new NavigablePath( getRootPathName() );
		final TableGroup rootTableGroup = createRootTableGroup(
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
		final SelectClause selectClause = rootQuerySpec.getSelectClause();
		final List<SqlSelection> sqlSelections = selectClause.getSqlSelections();
		int i = 0;
		int columnIndex = 0;
		final String[] columnAliases = getSubclassColumnAliasClosure();
		final int columnAliasesSize = columnAliases.length;
		for ( String identifierAlias : identifierAliases ) {
			sqlSelections.set(
					i,
					new SqlSelectionImpl(
							i,
							new AliasedExpression( sqlSelections.get( i ).getExpression(), identifierAlias + suffix )
					)
			);
			if ( i < columnAliasesSize && columnAliases[i].equals( identifierAlias ) ) {
				columnIndex++;
			}
			i++;
		}

		if ( entityMetamodel.hasSubclasses() ) {
			sqlSelections.set(
					i,
					new SqlSelectionImpl(
							i,
							new AliasedExpression( sqlSelections.get( i ).getExpression(), getDiscriminatorAlias() + suffix )
					)
			);
			i++;
		}

		if ( hasRowId() ) {
			sqlSelections.set(
					i,
					new SqlSelectionImpl(
							i,
							new AliasedExpression( sqlSelections.get( i ).getExpression(), ROWID_ALIAS + suffix )
					)
			);
			i++;
		}

		final String[] formulaAliases = getSubclassFormulaAliasClosure();
		int formulaIndex = 0;
		for ( ; i < sqlSelections.size(); i++ ) {
			final SqlSelection sqlSelection = sqlSelections.get( i );
			final ColumnReference columnReference = (ColumnReference) sqlSelection.getExpression();
			final String selectAlias = !columnReference.isColumnExpressionFormula()
					? columnAliases[columnIndex++] + suffix
					: formulaAliases[formulaIndex++] + suffix;
			sqlSelections.set(
					i,
					new SqlSelectionImpl(
							sqlSelection.getValuesArrayPosition(),
							new AliasedExpression( sqlSelection.getExpression(), selectAlias )
					)
			);
		}

		final String sql = getFactory().getJdbcServices()
				.getDialect()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( getFactory(), new SelectStatement( rootQuerySpec ) )
				.translate( null, QueryOptions.NONE )
				.getSqlString();
		final int fromIndex = sql.lastIndexOf( " from" );
		final String expression;
		if ( fromIndex != -1 ) {
			expression = sql.substring( "select ".length(), fromIndex );
		}
		else {
			expression = sql.substring( "select ".length() );
		}
		return expression;
	}

	private ImmutableFetchList fetchProcessor(FetchParent fetchParent, LoaderSqlAstCreationState creationState) {
		final FetchableContainer fetchableContainer = fetchParent.getReferencedMappingContainer();
		final int size = fetchableContainer.getNumberOfFetchables();
		final ImmutableFetchList.Builder fetches = new ImmutableFetchList.Builder( fetchableContainer );

		for ( int i = 0; i < size; i++ ) {
			final Fetchable fetchable = fetchableContainer.getFetchable( i );
			// Ignore plural attributes
			if ( !( fetchable instanceof PluralAttributeMapping ) ) {
				final FetchTiming fetchTiming = fetchable.getMappedFetchOptions().getTiming();
				if ( fetchable.asBasicValuedModelPart() != null ) {
					// Ignore lazy basic columns
					if ( fetchTiming == FetchTiming.DELAYED ) {
						continue;
					}
				}
				else if ( fetchable instanceof Association ) {
					final Association association = (Association) fetchable;
					// Ignore the fetchable if the FK is on the other side
					if ( association.getSideNature() == ForeignKeyDescriptor.Nature.TARGET ) {
						continue;
					}
					// Ensure the FK comes from the root table
					if ( !getRootTableName().equals( association.getForeignKeyDescriptor().getKeyTable() ) ) {
						continue;
					}
				}

				if ( fetchTiming == null ) {
					throw new AssertionFailure("fetchTiming was null");
				}

				if ( fetchable.isSelectable() ) {
					final Fetch fetch = fetchParent.generateFetchableFetch(
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

		return fetches.build();
	}

	/**
	 * @deprecated use {@link Fetchable#isSelectable()} instead.
	 */
	@Deprecated
	public boolean isSelectable(FetchParent fetchParent, Fetchable fetchable) {
		return fetchable.isSelectable();
	}

	@Override
	public String[] getIdentifierAliases(String suffix) {
		// NOTE: this assumes something about how propertySelectFragment is implemented by the subclass!
		// was toUnquotedAliasStrings( getIdentifierColumnNames() ) before - now tried
		// to remove that unquoting and missing aliases..
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
		// to remove that unquoting and missing aliases..
		return entityMetamodel.hasSubclasses()
				? new Alias( suffix ).toAliasString( getDiscriminatorAlias() )
				: null;
	}

	@Override
	public Object[] getDatabaseSnapshot(Object id, SharedSessionContractImplementor session) throws HibernateException {
		return singleIdLoader.loadDatabaseSnapshot( id, session );
	}

	@Override
	public Object getIdByUniqueKey(Object key, String uniquePropertyName, SharedSessionContractImplementor session) {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef(
					"resolving unique key [%s] to identifier for entity [%s]",
					key,
					getEntityName()
			);
		}

		return getUniqueKeyLoader( uniquePropertyName, session ).resolveId( key, session );
	}


	/**
	 * Generate the SQL that selects the version number by id
	 */
	public String generateSelectVersionString() {
		final SimpleSelect select = new SimpleSelect( getFactory() ).setTableName( getVersionedTableName() );
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
			SharedSessionContractImplementor session) throws HibernateException {
		assert getMappedTableDetails().getTableName().equals( getVersionedTableName() );
		final Object nextVersion = calculateNextVersion( id, currentVersion, session );
		updateCoordinator.forceVersionIncrement( id, currentVersion, nextVersion, batching, session );
		return nextVersion;
	}

	private Object calculateNextVersion(Object id, Object currentVersion, SharedSessionContractImplementor session) {
		if ( !isVersioned() ) {
			throw new AssertionFailure( "cannot force version increment on non-versioned entity" );
		}

		if ( isVersionGeneratedOnExecution() ) {
			// the difficulty here is exactly what we update in order to
			// force the version to be incremented in the db...
			throw new HibernateException( "LockMode.FORCE is currently not supported for generated version properties" );

		}

		final EntityVersionMapping versionMapping = getVersionMapping();
		final Object nextVersion = getVersionJavaType().next(
				currentVersion,
				versionMapping.getLength(),
				versionMapping.getTemporalPrecision() != null
						? versionMapping.getTemporalPrecision()
						: versionMapping.getPrecision(),
				versionMapping.getScale(),
				session
		);
		if ( LOG.isTraceEnabled() ) {
			LOG.trace(
					"Forcing version increment [" + infoString( this, id, getFactory() ) + "; "
							+ getVersionType().toLoggableString( currentVersion, getFactory() ) + " -> "
							+ getVersionType().toLoggableString( nextVersion, getFactory() ) + "]"
			);
		}
		return nextVersion;
	}

	/**
	 * Retrieve the version number
	 */
	@Override
	public Object getCurrentVersion(Object id, SharedSessionContractImplementor session) throws HibernateException {

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Getting version: {0}", infoString( this, id, getFactory() ) );
		}
		final String versionSelectString = getVersionSelectString();
		try {
			final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
			final PreparedStatement st = jdbcCoordinator.getStatementPreparer().prepareStatement( versionSelectString );
			try {
				getIdentifierType().nullSafeSet( st, id, 1, session );
				final ResultSet rs = jdbcCoordinator.getResultSetReturn().extract( st, versionSelectString );
				try {
					if ( !rs.next() ) {
						return null;
					}
					if ( !isVersioned() ) {
						return this;
					}
					return getVersionMapping().getJdbcMapping().getJdbcValueExtractor().extract( rs, 1, session );
				}
				finally {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( rs, st );
				}
			}
			finally {
				jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( st );
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

	protected LockingStrategy generateLocker(LockMode lockMode) {
		return factory.getJdbcServices().getDialect().getLockingStrategy( this, lockMode );
	}

	private LockingStrategy getLocker(LockMode lockMode) {
		return lockers.computeIfAbsent( lockMode, this::generateLocker );
	}

	@Override
	public void lock(
			Object id,
			Object version,
			Object object,
			LockMode lockMode,
			EventSource session) throws HibernateException {
		getLocker( lockMode ).lock( id, version, object, LockOptions.WAIT_FOREVER, session );
	}

	@Override
	public void lock(
			Object id,
			Object version,
			Object object,
			LockOptions lockOptions,
			EventSource session) throws HibernateException {
		getLocker( lockOptions.getLockMode() ).lock( id, version, object, lockOptions.getTimeOut(), session );
	}

	@Override
	public String getRootTableName() {
		return getSubclassTableName( 0 );
	}

	@Override
	public String getRootTableAlias(String drivingAlias) {
		return drivingAlias;
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
	public Type toType(String propertyName) throws QueryException {
		// todo (PropertyMapping) : simple delegation (aka, easy to remove)
		return propertyMapping.toType( propertyName );
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

	/**
	 * Warning:
	 * When there are duplicated property names in the subclasses
	 * of the class, this method may return the wrong table
	 * number for the duplicated subclass property (note that
	 * SingleTableEntityPersister defines an overloaded form
	 * which takes the entity name.
	 */
	@Override
	public int getSubclassPropertyTableNumber(String propertyPath) {
		throw new UnsupportedOperationException();
//		String rootPropertyName = StringHelper.root( propertyPath );
//		Type type = propertyMapping.toType( rootPropertyName );
//		if ( type.isAssociationType() ) {
//			AssociationType assocType = (AssociationType) type;
//			if ( assocType.useLHSPrimaryKey() ) {
//				// performance op to avoid the array search
//				return 0;
//			}
//			else if ( type instanceof CollectionType ) {
//				// properly handle property-ref-based associations
//				rootPropertyName = assocType.getLHSPropertyName();
//			}
//		}
//		//Enable for HHH-440, which we don't like:
//		/*if ( type.isComponentType() && !propertyName.equals(rootPropertyName) ) {
//			String unrooted = StringHelper.unroot(propertyName);
//			int idx = ArrayHelper.indexOf( getSubclassColumnClosure(), unrooted );
//			if ( idx != -1 ) {
//				return getSubclassColumnTableNumberClosure()[idx];
//			}
//		}*/
//		int index = ArrayHelper.indexOf( getSubclassPropertyNameClosure(), rootPropertyName ); //TODO: optimize this better!
//		return index == -1 ? 0 : getSubclassPropertyTableNumber( index );
	}

	@Override
	public Declarer getSubclassPropertyDeclarer(String propertyPath) {
		int tableIndex = getSubclassPropertyTableNumber( propertyPath );
		if ( tableIndex == 0 ) {
			return Declarer.CLASS;
		}
		else if ( isClassOrSuperclassTable( tableIndex ) ) {
			return Declarer.SUPERCLASS;
		}
		else {
			return Declarer.SUBCLASS;
		}
	}

	@Override
	public String getPropertyTableName(String propertyName) {
		final AttributeMapping attributeMapping = findAttributeMapping( propertyName );
		if ( attributeMapping instanceof SelectableMapping ) {
			return ( (SelectableMapping) attributeMapping ).getContainingTableExpression();
		}
		else if ( attributeMapping instanceof EmbeddableValuedModelPart ) {
			return attributeMapping.getContainingTableExpression();
		}
		else if ( attributeMapping instanceof DiscriminatedAssociationModelPart ) {
			return ( (DiscriminatedAssociationModelPart) attributeMapping ).getDiscriminatorPart()
					.getContainingTableExpression();
		}
		else if ( attributeMapping instanceof ToOneAttributeMapping ) {
			final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attributeMapping;
			if ( toOneAttributeMapping.getSideNature() == ForeignKeyDescriptor.Nature.KEY ) {
				return toOneAttributeMapping.getForeignKeyDescriptor().getKeyTable();
			}
			else {
				return toOneAttributeMapping.getForeignKeyDescriptor().getTargetTable();
			}
		}
		assert attributeMapping instanceof PluralAttributeMapping;
		return ( (PluralAttributeMapping) attributeMapping ).getKeyDescriptor().getKeyTable();
	}

	private DiscriminatorType discriminatorType;


	protected DiscriminatorType resolveDiscriminatorType() {
		if ( discriminatorType == null ) {
			discriminatorType = buildDiscriminatorType();
		}
		return discriminatorType;
	}

	private DiscriminatorType buildDiscriminatorType() {
		final BasicType<?> underlingJdbcMapping = getDiscriminatorType();
		if ( underlingJdbcMapping == null ) {
			return null;
		}

		final JavaTypeRegistry javaTypeRegistry = factory.getTypeConfiguration().getJavaTypeRegistry();

		final JavaType<Object> domainJavaType;
		if ( representationStrategy.getMode() == POJO
				&& getEntityName().equals( getJavaType().getJavaTypeClass().getName() ) ) {
			domainJavaType = javaTypeRegistry.resolveDescriptor( Class.class );
		}
		else {
			domainJavaType = javaTypeRegistry.resolveDescriptor( String.class );
		}

		//noinspection rawtypes
		final DiscriminatorConverter converter = MappedDiscriminatorConverter.fromValueMappings(
				getNavigableRole().append( EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME ),
				domainJavaType,
				underlingJdbcMapping,
				getSubclassByDiscriminatorValue(),
				factory.getMappingMetamodel()
		);

		//noinspection unchecked,rawtypes
		return new DiscriminatorTypeImpl( underlingJdbcMapping, converter );
	}

	@Override
	public DiscriminatorMetadata getTypeDiscriminatorMetadata() {
		return this::buildDiscriminatorType;
	}

	public static String generateTableAlias(String rootAlias, int tableNumber) {
		if ( tableNumber == 0 ) {
			return rootAlias;
		}
		final StringBuilder alias = new StringBuilder().append( rootAlias );
		if ( !rootAlias.endsWith( "_" ) ) {
			alias.append( '_' );
		}
		return alias.append( tableNumber ).append( '_' ).toString();
	}

	@Override
	public String[] toColumns(String name, final int i) {
		final String alias = generateTableAlias( name, getSubclassPropertyTableNumber( i ) );
		final String[] cols = getSubclassPropertyColumnNames( i );
		final String[] templates = getSubclassPropertyFormulaTemplateClosure()[i];
		final String[] result = new String[cols.length];
		for ( int j = 0; j < cols.length; j++ ) {
			result[j] = cols[j] == null
					? StringHelper.replace( templates[j], Template.TEMPLATE, alias )
					: StringHelper.qualify( alias, cols[j] );
		}
		return result;
	}

	private int getSubclassPropertyIndex(String propertyName) {
		return ArrayHelper.indexOf( subclassPropertyNameClosure, propertyName );
	}

	@Override
	public String[] getPropertyColumnNames(int i) {
		return propertyColumnNames[i];
	}

	public String[] getPropertyColumnWriters(int i) {
		return propertyColumnWriters[i];
	}

	public int getPropertyColumnSpan(int i) {
		return propertyColumnSpans[i];
	}

	public boolean hasFormulaProperties() {
		return hasFormulaProperties;
	}

	@Override
	public FetchMode getFetchMode(int i) {
		return subclassPropertyFetchModeClosure[i];
	}

	@Override
	public CascadeStyle getCascadeStyle(int i) {
		return subclassPropertyCascadeStyleClosure[i];
	}

	@Override
	public Type getSubclassPropertyType(int i) {
		return subclassPropertyTypeClosure[i];
	}

	@Override
	public String getSubclassPropertyName(int i) {
		return subclassPropertyNameClosure[i];
	}

	@Override
	public int countSubclassProperties() {
		return subclassPropertyTypeClosure.length;
	}

	@Override
	public String[] getSubclassPropertyColumnNames(int i) {
		return subclassPropertyColumnNameClosure[i];
	}

	@Override
	public boolean isDefinedOnSubclass(int i) {
		return propertyDefinedOnSubclass[i];
	}

	@Override
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
			return ArrayHelper.EMPTY_INT_ARRAY;
		}
		final List<Integer> fields = new ArrayList<>( attributeNames.length );

		// Sort attribute names so that we can traverse mappings efficiently
		Arrays.sort( attributeNames );

		int index = 0;
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping attributeMapping = attributeMappings.get( i );
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
		final BitSet mutablePropertiesIndexes = entityMetamodel.getMutablePropertiesIndexes();
		final int estimatedSize = attributeNames == null ? 0 : attributeNames.length + mutablePropertiesIndexes.cardinality();
		final List<Integer> fields = new ArrayList<>( estimatedSize );
		if ( estimatedSize == 0 ) {
			return ArrayHelper.EMPTY_INT_ARRAY;
		}
		if ( !mutablePropertiesIndexes.isEmpty() ) {
			// We have to check the state for "mutable" properties as dirty tracking isn't aware of mutable types
			final Type[] propertyTypes = entityMetamodel.getPropertyTypes();
			final boolean[] propertyCheckability = entityMetamodel.getPropertyCheckability();
			for ( int i = mutablePropertiesIndexes.nextSetBit(0); i >= 0;
					i = mutablePropertiesIndexes.nextSetBit(i + 1) ) {
				// This is kindly borrowed from org.hibernate.type.TypeHelper.findDirty
				if ( isDirty( currentState, previousState, propertyTypes, propertyCheckability, i, session ) ) {
					fields.add( i );
				}
			}
		}

		if ( attributeNames.length != 0 ) {
			final boolean[] propertyUpdateability = entityMetamodel.getPropertyUpdateability();
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
					final AttributeMapping attributeMapping = attributeMappings.get( i );
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
					final Integer index = entityMetamodel.getPropertyIndexOrNull( attributeName );
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
	public String[] getSubclassPropertyColumnNames(String propertyName) {
		//TODO: should we allow suffixes on these ?
		return subclassPropertyColumnNames.get( propertyName );
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
		return getUniqueKeyLoader( propertyName, session ).load( uniqueKey, LockOptions.NONE, readOnly, session );
	}

	private Map<SingularAttributeMapping, SingleUniqueKeyEntityLoader<?>> uniqueKeyLoadersNew;

	protected SingleUniqueKeyEntityLoader<?> getUniqueKeyLoader(String attributeName, SharedSessionContractImplementor session) {
		final SingularAttributeMapping attribute = (SingularAttributeMapping) findByPath( attributeName );
		final LoadQueryInfluencers influencers = session.getLoadQueryInfluencers();
		// no subselect fetching for entities for now
		if ( isAffectedByInfluencers( influencers, true ) ) {
			return new SingleUniqueKeyEntityLoaderStandard<>(
					this,
					attribute,
					influencers
			);
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
					new SingleUniqueKeyEntityLoaderStandard<>( this, attribute, new LoadQueryInfluencers( factory ) );
			uniqueKeyLoadersNew.put( attribute, loader );
			return loader;
		}
	}

	@Override
	public int getPropertyIndex(String propertyName) {
		return entityMetamodel.getPropertyIndex( propertyName );
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
		String idProp = getIdentifierPropertyName();
		if ( idProp != null ) {
			propertyMapping.initPropertyPaths(
					idProp, getIdentifierType(), getIdentifierColumnNames(),
					getIdentifierColumnReaders(), getIdentifierColumnReaderTemplates(), null, mapping
			);
		}
		if ( entityMetamodel.getIdentifierProperty().isEmbedded() ) {
			propertyMapping.initPropertyPaths(
					null, getIdentifierType(), getIdentifierColumnNames(),
					getIdentifierColumnReaders(), getIdentifierColumnReaderTemplates(), null, mapping
			);
		}
		if ( !entityMetamodel.hasNonIdentifierPropertyNamedId() ) {
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
		if ( entityMetamodel.isPolymorphic() ) {
			initDiscriminatorPropertyPath( mapping );
		}
	}

	protected boolean check(
			int rows,
			Object id,
			int tableNumber,
			Expectation expectation,
			PreparedStatement statement,
			String statementSQL) throws HibernateException {
		try {
			expectation.verifyOutcome( rows, statement, -1, statementSQL );
		}
		catch ( StaleStateException e ) {
			if ( !isNullableTable( tableNumber ) ) {
				final StatisticsImplementor statistics = getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.optimisticFailure( getEntityName() );
				}
				throw new StaleObjectStateException( getEntityName(), id );
			}
			return false;
		}
		catch ( TooManyRowsAffectedException e ) {
			throw new HibernateException(
					"Duplicate identifier in table for: " +
							infoString( this, id, getFactory() )
			);
		}
		catch ( Throwable t ) {
			return false;
		}
		return true;
	}

	public final boolean checkVersion(final boolean[] includeProperty) {
		return includeProperty[getVersionProperty()] || isVersionGeneratedOnExecution();
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
		final SimpleSelect select = new SimpleSelect( getFactory() )
				.setTableName( getTableName(0) )
				.addColumns( getKeyColumns(0) );
		for ( int i = 0; i < propertyNames.length; i++ ) {
			select.addRestriction( getPropertyColumnNames( propertyNames[i] ) );
		}
		return select.toStatementString();
	}

	@Override
	public String getSelectByUniqueKeyString(String[] propertyNames, String[] columnNames) {
		final SimpleSelect select = new SimpleSelect( getFactory() )
				.setTableName( getTableName( 0 ) )
				.addColumns( columnNames );
		for ( final String propertyName : propertyNames ) {
			select.addRestriction( getPropertyColumnNames( propertyName ) );
		}
		return select.toStatementString();
	}

	@Internal
	public boolean hasLazyDirtyFields(int[] dirtyFields) {
		final boolean[] propertyLaziness = getPropertyLaziness();
		for ( int i = 0; i < dirtyFields.length; i++ ) {
			if ( propertyLaziness[dirtyFields[i]] ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public GeneratedValuesMutationDelegate getInsertDelegate() {
		return insertDelegate;
	}

	@Override
	public GeneratedValuesMutationDelegate getUpdateDelegate() {
		return updateDelegate;
	}

	protected EntityTableMapping[] getTableMappings() {
		return tableMappings;
	}

	/**
	 * @deprecated this method is no longer used
	 */
	@Deprecated(since = "6", forRemoval = true)
	public int getTableMappingsCount() {
		return tableMappings.length;
	}

	public EntityTableMapping getTableMapping(int i) {
		return tableMappings[i];
	}

	/**
	 * Unfortunately we cannot directly use `SelectableMapping#getContainingTableExpression()`
	 * as that blows up for attributes declared on super-type for union-subclass mappings
	 */
	public String physicalTableNameForMutation(SelectableMapping selectableMapping) {
		assert !selectableMapping.isFormula();
		return selectableMapping.getContainingTableExpression();
	}

	public EntityTableMapping getPhysicalTableMappingForMutation(SelectableMapping selectableMapping) {
		final String tableNameForMutation = physicalTableNameForMutation( selectableMapping );
		for ( int i = 0; i < tableMappings.length; i++ ) {
			if ( tableNameForMutation.equals( tableMappings[i].getTableName() ) ) {
				return tableMappings[i];
			}
		}

		throw new IllegalArgumentException( "Unable to resolve TableMapping for selectable - " + selectableMapping );
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

	@Override
	public boolean hasSkippableTables() {
		return false;
	}

	protected boolean hasAnySkippableTables(boolean[] optionalTables, boolean[] inverseTables) {
		// todo (6.x) : cache this?
		for ( int i = 0; i < optionalTables.length; i++ ) {
			if ( optionalTables[i] ) {
				return true;
			}
		}

		for ( int i = 0; i < inverseTables.length; i++ ) {
			if ( inverseTables[i] ) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Delete an object
	 */
	@Override
	public void delete(Object id, Object version, Object object, SharedSessionContractImplementor session) {
		deleteCoordinator.delete( object, id, version, session );
	}

	/**
	 * @deprecated this method is no longer used
	 */
	@Deprecated(since = "6", forRemoval = true)
	protected boolean isAllOrDirtyOptLocking() {
		return entityMetamodel.getOptimisticLockStyle().isAllOrDirty();
	}

	protected void logStaticSQL() {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Static SQL for entity: %s", getEntityName() );
			for ( Map.Entry<String, SingleIdArrayLoadPlan> entry : lazyLoadPlanByFetchGroup.entrySet() ) {
				LOG.debugf( " Lazy select (%s) : %s", entry.getKey(), entry.getValue().getJdbcSelect().getSqlString() );
			}
			if ( sqlVersionSelectString != null ) {
				LOG.debugf( " Version select: %s", sqlVersionSelectString );
			}

			{
				final MutationOperationGroup staticInsertGroup = insertCoordinator.getStaticMutationOperationGroup();
				if ( staticInsertGroup != null ) {
					for ( int i = 0; i < staticInsertGroup.getNumberOfOperations(); i++ ) {
						final MutationOperation mutation = staticInsertGroup.getOperation( i );
						if ( mutation instanceof JdbcOperation ) {
							LOG.debugf( " Insert (%s): %s", i, ( (JdbcOperation) mutation ).getSqlString() );
						}
					}
				}
			}

			{
				final MutationOperationGroup staticUpdateGroup = updateCoordinator.getStaticMutationOperationGroup();
				if ( staticUpdateGroup != null ) {
					for ( int i = 0; i < staticUpdateGroup.getNumberOfOperations(); i++ ) {
						final MutationOperation mutation = staticUpdateGroup.getOperation( i );
						if ( mutation instanceof JdbcOperation ) {
							LOG.debugf( " Update (%s): %s", i, ( (JdbcOperation) mutation ).getSqlString() );
						}
					}
				}
			}

			{
				final MutationOperationGroup staticDeleteGroup = deleteCoordinator.getStaticMutationOperationGroup();
				if ( staticDeleteGroup != null ) {
					for ( int i = 0; i < staticDeleteGroup.getNumberOfOperations(); i++ ) {
						final MutationOperation mutation = staticDeleteGroup.getOperation( i );
						if ( mutation instanceof JdbcOperation ) {
							LOG.debugf( " Delete (%s): %s", i, ( (JdbcOperation) mutation ).getSqlString() );
						}
					}
				}
			}
		}
	}

	public abstract Map<Object, String> getSubclassByDiscriminatorValue();

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
															: getKeyColumnNames(),
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
				final TableReference tableReference = tableGroup.resolveTableReference( getSoftDeleteTableDetails().getTableName() );
				final Predicate softDeletePredicate = SoftDeleteHelper.createNonSoftDeletedRestriction(
						tableReference,
						softDeleteMapping,
						creationState.getSqlExpressionResolver()
				);
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
			final Map<String, EntityNameUse> entityNameUseMap;
			final Collection<EntityMappingType> subMappingTypes = getSubMappingTypes();
			entityNameUseMap = new HashMap<>( 1 + subMappingTypes.size() + ( isInherited() ? 1 : 0 ) );
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
				return new NegatedPredicate( new NullnessPredicate( sqlExpression ) );
			}
			else {
				final QueryLiteral<Object> literal = new QueryLiteral<>( value, discriminatorType );
				return new ComparisonPredicate( sqlExpression, ComparisonOperator.EQUAL, literal );
			}
		}
	}

	private Predicate createInListPredicate(BasicType<?> discriminatorType, Expression sqlExpression) {
		final List<Expression> values = new ArrayList<>( fullDiscriminatorValues.length );
		boolean hasNull = false, hasNonNull = false;
		for ( Object discriminatorValue : fullDiscriminatorValues ) {
			if ( discriminatorValue == NULL_DISCRIMINATOR ) {
				hasNull = true;
			}
			else if ( discriminatorValue == NOT_NULL_DISCRIMINATOR ) {
				hasNonNull = true;
			}
			else {
				values.add( new QueryLiteral<>( discriminatorValue, discriminatorType) );
			}
		}
		final Predicate predicate = new InListPredicate( sqlExpression, values );
		if ( hasNull || hasNonNull ) {
			final Junction junction = new Junction( Junction.Nature.DISJUNCTION );

			if ( hasNull && hasNonNull ) {
				// This means we need to select everything, we don't need a predicate at all
				// Return an empty Junction
				return junction;
			}

			if ( hasNonNull ) {
				return new NullnessPredicate( sqlExpression, true );
			}
			else if ( hasNull ) {
				junction.add( new NullnessPredicate( sqlExpression ) );
			}

			junction.add( predicate );
			return junction;
		}
		return predicate;
	}

	protected String getPrunedDiscriminatorPredicate(
			Map<String, EntityNameUse> entityNameUses,
			MappingMetamodelImplementor mappingMetamodel,
			String alias) {
		final InFragment frag = new InFragment();
		if ( isDiscriminatorFormula() ) {
			frag.setFormula( alias, getDiscriminatorFormulaTemplate() );
		}
		else {
			frag.setColumn( alias, getDiscriminatorColumnName() );
		}
		boolean containsNotNull = false;
		for ( Map.Entry<String, EntityNameUse> entry : entityNameUses.entrySet() ) {
			final EntityNameUse.UseKind useKind = entry.getValue().getKind();
			if ( useKind == EntityNameUse.UseKind.PROJECTION || useKind == EntityNameUse.UseKind.EXPRESSION ) {
				// We only care about treat and filter uses which allow to reduce the amount of rows to select
				continue;
			}
			final EntityPersister persister = mappingMetamodel.getEntityDescriptor( entry.getKey() );
			// Filtering for abstract entities makes no sense, so ignore that
			// Also, it makes no sense to filter for any of the super types,
			// as the query will contain a filter for that already anyway
			if ( !persister.isAbstract() && ( this == persister || !isTypeOrSuperType( persister ) ) ) {
				containsNotNull = containsNotNull || InFragment.NOT_NULL.equals( persister.getDiscriminatorSQLValue() );
				frag.addValue( persister.getDiscriminatorSQLValue() );
			}
		}
		final List<String> discriminatorSQLValues = Arrays.asList( ( (AbstractEntityPersister) getRootEntityDescriptor() ).fullDiscriminatorSQLValues );
		if ( frag.getValues().size() == discriminatorSQLValues.size() ) {
			// Nothing to prune if we filter for all subtypes
			return null;
		}

		if ( containsNotNull ) {
			final String lhs;
			if ( isDiscriminatorFormula() ) {
				lhs = StringHelper.replace( getDiscriminatorFormulaTemplate(), Template.TEMPLATE, alias );
			}
			else {
				lhs = qualifyConditionally( alias, getDiscriminatorColumnName() );
			}
			final List<String> actualDiscriminatorSQLValues = new ArrayList<>( discriminatorSQLValues.size() );
			for ( String value : discriminatorSQLValues ) {
				if ( !frag.getValues().contains( value ) && !InFragment.NULL.equals( value ) ) {
					actualDiscriminatorSQLValues.add( value );
				}
			}
			final StringBuilder sb = new StringBuilder( 70 + actualDiscriminatorSQLValues.size() * 10 ).append( " or " );
			if ( !actualDiscriminatorSQLValues.isEmpty() ) {
				sb.append( lhs ).append( " is not in (" );
				sb.append( String.join( ",", actualDiscriminatorSQLValues ) );
				sb.append( ") and " );
			}
			sb.append( lhs ).append( " is not null" );
			frag.getValues().remove( InFragment.NOT_NULL );
			return frag.toFragmentString() + sb;
		}
		else {
			return frag.toFragmentString();
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
			final FilterAliasGenerator filterAliasGenerator = useQualifier && tableGroup != null
					? getFilterAliasGenerator( tableGroup )
					: null;
			filterHelper.applyEnabledFilters(
					predicateConsumer,
					filterAliasGenerator,
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
			final TableReference tableReference = tableGroup.resolveTableReference( sqlWhereStringTableExpression );
			if ( tableReference == null ) {
				return null;
			}
			else {
				return useQualifier && tableReference.getIdentificationVariable() != null
						? tableReference.getIdentificationVariable()
						: tableReference.getTableId();
			}
		}
	}

	@Override
	public String generateFilterConditionAlias(String rootAlias) {
		return rootAlias;
	}

	protected boolean shouldInnerJoinSubclassTable(int subclassTableNumber, Set<String> treatAsDeclarations) {
		if ( isClassOrSuperclassJoin( subclassTableNumber ) ) {
			// the table is either this persister's driving table or (one of) its superclass persister's driving
			// tables which can be inner joined as long as the `shouldInnerJoin` condition resolves to true
			return !isInverseTable( subclassTableNumber )
				&& !isNullableTable( subclassTableNumber );
		}

		// otherwise we have a subclass table and need to look a little deeper...

		// IMPL NOTE : By default includeSubclasses indicates that all subclasses should be joined and that each
		// subclass ought to be joined by outer-join.  However, TREAT-AS always requires that an inner-join be used
		// so we give TREAT-AS higher precedence...

		return isSubclassTableIndicatedByTreatAsDeclarations( subclassTableNumber, treatAsDeclarations );
	}

	protected boolean isSubclassTableIndicatedByTreatAsDeclarations(int subclassTableNumber, Set<String> treatAsDeclarations) {
		return false;
	}

	/**
	 * Post-construct is a callback for AbstractEntityPersister subclasses to call after they are all done with their
	 * constructor processing.  It allows AbstractEntityPersister to extend its construction after all subclass-specific
	 * details have been handled.
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

		final List<AttributeMapping> insertGeneratedAttributes = hasInsertGeneratedProperties() ?
				GeneratedValuesProcessor.getGeneratedAttributes( this, INSERT )
				: Collections.emptyList();
		final List<AttributeMapping> updateGeneratedAttributes = hasUpdateGeneratedProperties() ?
				GeneratedValuesProcessor.getGeneratedAttributes( this, UPDATE )
				: Collections.emptyList();

		insertGeneratedProperties = initInsertGeneratedProperties( insertGeneratedAttributes );
		updateGeneratedProperties = initUpdateGeneratedProperties( updateGeneratedAttributes );

		insertDelegate = createInsertDelegate();
		updateDelegate = createUpdateDelegate();

		if ( isIdentifierAssignedByInsert() ) {
			identitySelectString = getIdentitySelectString( factory.getJdbcServices().getDialect() );
		}

		if ( hasInsertGeneratedProperties() ) {
			insertGeneratedValuesProcessor = createGeneratedValuesProcessor( INSERT, insertGeneratedAttributes );
		}
		if ( hasUpdateGeneratedProperties() ) {
			updateGeneratedValuesProcessor = createGeneratedValuesProcessor( UPDATE, updateGeneratedAttributes );
		}

		insertCoordinator = buildInsertCoordinator();
		updateCoordinator = buildUpdateCoordinator();
		deleteCoordinator = buildDeleteCoordinator();
		mergeCoordinator = buildMergeCoordinator();

		final int joinSpan = getTableSpan();

		tableHasColumns = new boolean[joinSpan];
		for ( int j = 0; j < joinSpan; j++ ) {
			final String tableName = getTableName( j );
			final EntityTableMapping tableMapping = findTableMapping( tableName );
			tableHasColumns[j] = tableMapping.hasColumns();
		}

		//select SQL
		sqlVersionSelectString = generateSelectVersionString();
	}

	protected GeneratedValuesMutationDelegate createInsertDelegate() {
		if ( isIdentifierAssignedByInsert() ) {
			final OnExecutionGenerator generator = (OnExecutionGenerator) getGenerator();
			return generator.getGeneratedIdentifierDelegate( this );
		}
		return GeneratedValuesHelper.getGeneratedValuesDelegate( this, INSERT );
	}

	protected GeneratedValuesMutationDelegate createUpdateDelegate() {
		return GeneratedValuesHelper.getGeneratedValuesDelegate( this, UPDATE );
	}

	private EntityTableMapping findTableMapping(String tableName) {
		for ( int i = 0; i < tableMappings.length; i++ ) {
			if ( tableMappings[i].getTableName().equals( tableName ) ) {
				return tableMappings[i];
			}
		}
		throw new IllegalArgumentException( "Unknown table : " + tableName );
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
				final Consumer<SelectableConsumer> selectableConsumerConsumer = tableKeyColumnVisitationSupplier.get();
				final List<EntityTableMapping.KeyColumn> keyColumns = new ArrayList<>();
				selectableConsumerConsumer.accept( (selectionIndex, selectableMapping) -> {
					keyColumns.add( new EntityTableMapping.KeyColumn(
							tableExpression,
							selectableMapping.getSelectionExpression(),
							selectableMapping.getWriteExpression(),
							selectableMapping.isFormula(),
							selectableMapping.getJdbcMapping()
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
						new EntityTableMapping.KeyMapping( keyColumns, identifierMapping ),
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
						entityMetamodel.isDynamicUpdate(),
						entityMetamodel.isDynamicInsert()
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

		final EntityTableMapping[] list = new EntityTableMapping[tableBuilderMap.size()];
		int i = 0;
		for ( Map.Entry<String, TableMappingBuilder> entry : tableBuilderMap.entrySet() ) {
			list[i++] = entry.getValue().build();
		}
		return list;
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
	interface MutabilityOrderedTableConsumer {
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
			final AttributeMapping attributeMapping = attributeMappings.get( i );
			if ( attributeMapping instanceof SingularAttributeMapping ) {
				return new UpdateCoordinatorStandard( this, factory );
			}
		}
		// otherwise, nothing to update
		return new UpdateCoordinatorNoOp( this );
	}

	protected UpdateCoordinator buildMergeCoordinator() {
		// we only have updates to issue for entities with one or more singular attributes
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping attributeMapping = attributeMappings.get( i );
			if ( attributeMapping instanceof SingularAttributeMapping ) {
				return new MergeCoordinator( this, factory );
			}
		}
		// otherwise, nothing to update
		return new UpdateCoordinatorNoOp( this );
	}

	protected DeleteCoordinator buildDeleteCoordinator() {
		if ( softDeleteMapping == null ) {
			return new DeleteCoordinatorStandard( this, factory );
		}
		else {
			return new DeleteCoordinatorSoft( this, factory );
		}
	}

	public void addDiscriminatorToInsertGroup(MutationGroupBuilder insertGroupBuilder) {
	}

	public void addSoftDeleteToInsertGroup(MutationGroupBuilder insertGroupBuilder) {
		if ( softDeleteMapping != null ) {
			final TableInsertBuilder insertBuilder = insertGroupBuilder.getTableDetailsBuilder( getIdentifierTableName() );
			insertBuilder.addValueColumn( softDeleteMapping );
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
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Fetching entity: {0}", infoString( this, id, getFactory() ) );
		}

		final SingleIdEntityLoader<?> loader = determineLoaderToUse( session );
		return optionalObject == null
				? loader.load( id, lockOptions, readOnly, session )
				: loader.load( id, optionalObject, lockOptions, readOnly, session );
	}

	protected SingleIdEntityLoader<?> determineLoaderToUse(SharedSessionContractImplementor session) {
		if ( hasNamedQueryLoader() ) {
			return getSingleIdLoader();
		}
		else {
			final LoadQueryInfluencers influencers = session.getLoadQueryInfluencers();
			// no subselect fetching for entities for now
			return isAffectedByInfluencers( influencers, true )
					? buildSingleIdEntityLoader( influencers )
					: getSingleIdLoader();
		}
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
		final BytecodeEnhancementMetadata enhancementMetadata = getEntityMetamodel().getBytecodeEnhancementMetadata();
		final BytecodeLazyAttributeInterceptor currentInterceptor = enhancementMetadata.extractLazyInterceptor( entity );
		if ( currentInterceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
			final EnhancementAsProxyLazinessInterceptor proxyInterceptor = (EnhancementAsProxyLazinessInterceptor) currentInterceptor;

			final EntityKey entityKey = proxyInterceptor.getEntityKey();
			final Object identifier = entityKey.getIdentifier();

			Object loaded = null;
			if ( canReadFromCache && session.isEventSource() ) {
				LoadEvent loadEvent = new LoadEvent( identifier, entity, session.asEventSource(), false );
				loaded = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache( loadEvent, this, entityKey );
			}
			if ( loaded == null ) {
				loaded = determineLoaderToUse( session ).load( identifier, entity, LockOptions.NONE, session );
			}

			if ( loaded == null ) {
				final PersistenceContext persistenceContext = session.getPersistenceContext();
				persistenceContext.removeEntry( entity );
				persistenceContext.removeEntity( entityKey );
				factory.getEntityNotFoundDelegate().handleEntityNotFound( entityKey.getEntityName(), identifier );
			}

			final LazyAttributeLoadingInterceptor interceptor =
					enhancementMetadata.injectInterceptor( entity, identifier, session );

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

		throw new IllegalStateException();
	}

	@Override
	public List<?> multiLoad(Object[] ids, EventSource session, MultiIdLoadOptions loadOptions) {
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
		final RootGraphImplementor<?> graph = loadQueryInfluencers.getEffectiveEntityGraph().getGraph();
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
			if ( filterHelper.isAffectedBy( loadQueryInfluencers.getEnabledFilters(), onlyApplyForLoadByKeyFilters ) ) {
				return true;
			}

			return isAffectedByEnabledFilters( new HashSet<>(), loadQueryInfluencers, onlyApplyForLoadByKeyFilters );
		}
		return false;
	}

	@Override
	public boolean isSubclassPropertyNullable(int i) {
		return subclassPropertyNullabilityClosure[i];
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
		int[] props = DirtyHelper.findDirty(
				entityMetamodel.getDirtyCheckablePropertyTypes(),
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
		int[] props = DirtyHelper.findModified(
				entityMetamodel.getProperties(),
				current,
				old,
				propertyColumnUpdateable,
				getPropertyUpdateability(),
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
	 * Which properties appear in the SQL update?
	 * (Initialized, updateable ones!)
	 */
	public boolean[] getPropertyUpdateability(Object entity) {
		return hasUninitializedLazyProperties( entity )
				? getNonLazyPropertyUpdateability()
				: getPropertyUpdateability();
	}

	private void logDirtyProperties(int[] props) {
		if ( LOG.isTraceEnabled() ) {
			for ( int prop : props ) {
				final String propertyName = getAttributeMapping( prop ).getAttributeName();
				LOG.trace( StringHelper.qualify( getEntityName(), propertyName ) + " is dirty" );
			}
		}
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public EntityMetamodel getEntityMetamodel() {
		return entityMetamodel;
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
		return entityMetamodel.getName();
	}

	@Override
	public EntityType getEntityType() {
		return entityMetamodel.getEntityType();
	}

	public boolean isPolymorphic() {
		return entityMetamodel.isPolymorphic();
	}

	@Override
	public boolean isInherited() {
		return entityMetamodel.isInherited();
	}

	@Override
	public boolean hasCascades() {
		return entityMetamodel.hasCascades();
	}

	@Override
	public boolean hasCascadeDelete() {
		return entityMetamodel.hasCascadeDelete();
	}

	@Override
	public boolean hasOwnedCollections() {
		return entityMetamodel.hasOwnedCollections();
	}

	@Override
	public boolean hasIdentifierProperty() {
		return !entityMetamodel.getIdentifierProperty().isVirtual();
	}

	@Override
	public BasicType<?> getVersionType() {
		return entityMetamodel.getVersionProperty() == null
				? null
				: (BasicType<?>) entityMetamodel.getVersionProperty().getType();
	}

	@Override
	public int getVersionProperty() {
		return entityMetamodel.getVersionPropertyIndex();
	}

	@Override
	public boolean isVersioned() {
		return entityMetamodel.isVersioned();
	}

	@Override
	public boolean isIdentifierAssignedByInsert() {
		return entityMetamodel.getIdentifierProperty().isIdentifierAssignedByInsert();
	}

	@Override
	public boolean hasLazyProperties() {
		return entityMetamodel.hasLazyProperties();
	}

//	public boolean hasUninitializedLazyProperties(Object entity) {
//		if ( hasLazyProperties() ) {
//			InterceptFieldCallback callback = ( ( InterceptFieldEnabled ) entity ).getInterceptFieldCallback();
//			return callback != null && !( ( FieldInterceptor ) callback ).isInitialized();
//		}
//		else {
//			return false;
//		}
//	}

	@Override
	public void afterReassociate(Object entity, SharedSessionContractImplementor session) {
		final BytecodeEnhancementMetadata metadata = getEntityMetamodel().getBytecodeEnhancementMetadata();
		if ( metadata.isEnhancedForLazyLoading() ) {
			final BytecodeLazyAttributeInterceptor interceptor = metadata.extractLazyInterceptor( entity );
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
		if ( naturalIdMapping == null ) {
			return;
		}

		if ( ! naturalIdMapping.isMutable() ) {
			// we assume there were no changes to natural id during detachment for now, that is validated later
			// during flush.
			return;
		}

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final NaturalIdResolutions naturalIdResolutions = persistenceContext.getNaturalIdResolutions();
		final Object id = getIdentifier( entity, session );

		// for reattachment of mutable natural-ids, we absolutely positively have to grab the snapshot from the
		// database, because we have no other way to know if the state changed while detached.
		final Object[] entitySnapshot = persistenceContext.getDatabaseSnapshot( id, this );
		final Object naturalIdSnapshot = entitySnapshot == StatefulPersistenceContext.NO_ROW
				? null
				: naturalIdMapping.extractNaturalIdFromEntityState( entitySnapshot );

		naturalIdResolutions.removeSharedResolution( id, naturalIdSnapshot, this, false );
		final Object naturalId = naturalIdMapping.extractNaturalIdFromEntity( entity );
		naturalIdResolutions.manageLocalResolution( id, naturalId, this, CachedNaturalIdValueSource.UPDATE );
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
					final PersistenceContext persistenceContext;
					if ( version == null
							&& ( persistenceContext = session.getPersistenceContext() ).hasLoadContext()
									&& !persistenceContext.getLoadContexts().isLoadingFinished() ) {
						// check if we're currently loading this entity instance, the version
						// will be null but the entity cannot be considered transient
						final EntityHolder holder =
								persistenceContext.getEntityHolder( new EntityKey( id, this ) );
						if ( holder != null && holder.isEventuallyInitialized() && holder.getEntity() == entity ) {
							return false;
						}
					}
					final Generator identifierGenerator = getGenerator();
					if ( identifierGenerator != null && !( identifierGenerator instanceof ForeignGenerator ) ) {
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
			final EntityDataAccess cache = getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey( id, this, session.getFactory(), session.getTenantIdentifier() );
			final Object ce = CacheHelper.fromSharedCache( session, ck, this, getCacheAccessStrategy() );
			if ( ce != null ) {
				return false;
			}
		}

		return null;
	}

	@Override
	public boolean hasCollections() {
		return entityMetamodel.hasCollections();
	}

	@Override
	public boolean hasMutableProperties() {
		return entityMetamodel.hasMutableProperties();
	}

	@Override
	public boolean isMutable() {
		return entityMetamodel.isMutable();
	}

	@Override
	public boolean isAbstract() {
		return entityMetamodel.isAbstract();
	}

	@Override
	public boolean hasSubclasses() {
		return entityMetamodel.hasSubclasses();
	}

	@Override
	public boolean hasProxy() {
		// skip proxy instantiation if entity is bytecode enhanced
		return entityMetamodel.isLazy() && !entityMetamodel.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
	}

	@Override @Deprecated
	public IdentifierGenerator getIdentifierGenerator() throws HibernateException {
		return entityMetamodel.getIdentifierProperty().getIdentifierGenerator();
	}

	@Override
	public Generator getGenerator() {
		return entityMetamodel.getIdentifierProperty().getGenerator();
	}

	@Override
	public BeforeExecutionGenerator getVersionGenerator() {
		return versionGenerator;
	}

	@Override
	public String getRootEntityName() {
		return entityMetamodel.getRootName();
	}

	@Override @Deprecated
	public ClassMetadata getClassMetadata() {
		return this;
	}

	@Override
	public String getMappedSuperclass() {
		return entityMetamodel.getSuperclass();
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return entityMetamodel.isExplicitPolymorphism();
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

		EntityConcreteTypeLoader concreteTypeLoader = this.concreteTypeLoader;
		if ( concreteTypeLoader == null ) {
			this.concreteTypeLoader = concreteTypeLoader = new EntityConcreteTypeLoader( this, session.getFactory() );
		}
		return concreteTypeLoader.getConcreteType( id, session );
	}

	@Override
	public String[] getKeyColumnNames() {
		return getIdentifierColumnNames();
	}

	@Override
	public String getName() {
		return getEntityName();
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public boolean consumesEntityAlias() {
		return true;
	}

	@Override
	public boolean consumesCollectionAlias() {
		return false;
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
	public Type getType() {
		return entityMetamodel.getEntityType();
	}

	@Override
	public boolean isSelectBeforeUpdateRequired() {
		return entityMetamodel.isSelectBeforeUpdate();
	}

	public final OptimisticLockStyle optimisticLockStyle() {
		return entityMetamodel.getOptimisticLockStyle();
	}

	@Override
	public Object createProxy(Object id, SharedSessionContractImplementor session) throws HibernateException {
		return representationStrategy.getProxyFactory().getProxy( id, session );
	}

	@Override
	public String toString() {
		return StringHelper.unqualify( getClass().getName() )
				+ '(' + entityMetamodel.getName() + ')';
	}

	@Override
	public boolean isInstrumented() {
		return entityMetamodel.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
	}

	@Override
	public boolean hasInsertGeneratedProperties() {
		return entityMetamodel.hasInsertGeneratedValues();
	}

	@Override
	public boolean hasUpdateGeneratedProperties() {
		return entityMetamodel.hasUpdateGeneratedValues();
	}

	@Override
	public boolean isVersionPropertyGenerated() {
		return isVersioned()
			&& ( isVersionGeneratedOnExecution() || isVersionGeneratedBeforeExecution() );
	}

	@Override
	public boolean isVersionPropertyInsertable() {
		return isVersioned() && getPropertyInsertability()[getVersionProperty()];
	}

	public boolean isVersionGeneratedOnExecution() {
		final Generator strategy = getEntityMetamodel().getGenerators()[ getVersionProperty() ];
		return strategy != null && strategy.generatesSometimes() && strategy.generatedOnExecution();
	}

	public boolean isVersionGeneratedBeforeExecution() {
		final Generator strategy = getEntityMetamodel().getGenerators()[ getVersionProperty() ];
		return strategy != null && strategy.generatesSometimes() && !strategy.generatedOnExecution();
	}

	@Override
	public void afterInitialize(Object entity, SharedSessionContractImplementor session) {
		if ( isPersistentAttributeInterceptable( entity ) && getRepresentationStrategy().getMode() == POJO ) {
			final BytecodeLazyAttributeInterceptor interceptor = getEntityMetamodel().getBytecodeEnhancementMetadata()
					.extractLazyInterceptor( entity );
			assert interceptor != null;
			if ( interceptor.getLinkedSession() == null ) {
				interceptor.setSession( session );
			}
		}

		// clear the fields that are marked as dirty in the dirtiness tracker
		processIfSelfDirtinessTracker( entity, AbstractEntityPersister::clearDirtyAttributes );
		processIfManagedEntity( entity, AbstractEntityPersister::useTracker );
	}

	private static void clearDirtyAttributes(final SelfDirtinessTracker entity) {
		entity.$$_hibernate_clearDirtyAttributes();
	}

	private static void useTracker(final ManagedEntity entity) {
		entity.$$_hibernate_setUseTracker( true );
	}

	@Override
	public String[] getPropertyNames() {
		return entityMetamodel.getPropertyNames();
	}

	@Override
	public Type[] getPropertyTypes() {
		return entityMetamodel.getPropertyTypes();
	}

	@Override
	public boolean[] getPropertyLaziness() {
		return entityMetamodel.getPropertyLaziness();
	}

	@Override
	public boolean[] getPropertyUpdateability() {
		return entityMetamodel.getPropertyUpdateability();
	}

	@Override
	public boolean[] getPropertyCheckability() {
		return entityMetamodel.getPropertyCheckability();
	}

	public boolean[] getNonLazyPropertyUpdateability() {
		return entityMetamodel.getNonlazyPropertyUpdateability();
	}

	@Override
	public boolean[] getPropertyInsertability() {
		return entityMetamodel.getPropertyInsertability();
	}

	@Override
	public boolean[] getPropertyNullability() {
		return entityMetamodel.getPropertyNullability();
	}

	@Override
	public boolean[] getPropertyVersionability() {
		return entityMetamodel.getPropertyVersionability();
	}

	@Override
	public CascadeStyle[] getPropertyCascadeStyles() {
		return entityMetamodel.getCascadeStyles();
	}

	@Override
	public boolean isPropertySelectable(int propertyNumber) {
		return getAttributeMapping( propertyNumber ).getAttributeMetadata().isSelectable();
	}

	@Override
	public final Class<?> getMappedClass() {
		return this.getMappedJavaType().getJavaTypeClass();
	}

	@Override
	public boolean implementsLifecycle() {
		return this.implementsLifecycle;
	}

	@Override
	public Class<?> getConcreteProxyClass() {
		final JavaType<?> proxyJavaType = getRepresentationStrategy().getProxyJavaType();
		return proxyJavaType != null ? proxyJavaType.getJavaTypeClass() : javaType.getJavaTypeClass();
	}

	@Override
	public void setPropertyValues(Object object, Object[] values) {
		if ( accessOptimizer != null ) {
			accessOptimizer.setPropertyValues( object, values );
		}
		else {
			final BytecodeEnhancementMetadata enhancementMetadata = entityMetamodel.getBytecodeEnhancementMetadata();
			final AttributeMappingsList attributeMappings = getAttributeMappings();
			if ( enhancementMetadata.isEnhancedForLazyLoading() ) {
				for ( int i = 0; i < attributeMappings.size(); i++ ) {
					final Object value = values[i];
					if ( value != UNFETCHED_PROPERTY ) {
						setterCache[i].set( object, value );
					}
				}
			}
			else {
				for ( int i = 0; i < attributeMappings.size(); i++ ) {
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
			final BytecodeEnhancementMetadata enhancementMetadata = entityMetamodel.getBytecodeEnhancementMetadata();
			final AttributeMappingsList attributeMappings = getAttributeMappings();
			final Object[] values = new Object[attributeMappings.size()];
			if ( enhancementMetadata.isEnhancedForLazyLoading() ) {
				final LazyAttributesMetadata lazyAttributesMetadata = enhancementMetadata.getLazyAttributesMetadata();
				for ( int i = 0; i < attributeMappings.size(); i++ ) {
					final AttributeMapping attributeMapping = attributeMappings.get( i );
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
	public Object getPropertyValue(Object object, String propertyName) {
		final int dotIndex = propertyName.indexOf( '.' );
		final String basePropertyName = dotIndex == -1
				? propertyName
				: propertyName.substring( 0, dotIndex );
		final AttributeMapping attributeMapping = findAttributeMapping( basePropertyName );
		ManagedMappingType baseValueType = null;
		Object baseValue = null;
		if ( attributeMapping != null ) {
			baseValue = getterCache[attributeMapping.getStateArrayPosition()].get( object );
			if ( dotIndex != -1 ) {
				baseValueType = (ManagedMappingType) attributeMapping.getMappedType();
			}
		}
		else if ( identifierMapping instanceof NonAggregatedIdentifierMapping ) {
			final AttributeMapping mapping = ( (NonAggregatedIdentifierMapping) identifierMapping ).findSubPart(
					propertyName,
					null
			).asAttributeMapping();
			if ( mapping != null ) {
				baseValue = mapping.getValue( object );
				if ( dotIndex != -1 ) {
					baseValueType = (ManagedMappingType) mapping.getMappedType();
				}
			}
		}
		return getPropertyValue( baseValue, baseValueType, propertyName, dotIndex );
	}

	private Object getPropertyValue(
			Object baseValue,
			ManagedMappingType baseValueType,
			String propertyName,
			int dotIndex) {
		if ( baseValueType == null ) {
			return baseValue;
		}
		else {
			final int nextDotIndex = propertyName.indexOf( '.', dotIndex + 1 );
			final int endIndex = nextDotIndex == -1 ? propertyName.length() : nextDotIndex;
			final AttributeMapping attributeMapping =
					baseValueType.findAttributeMapping( propertyName.substring( dotIndex + 1, endIndex ) );
			baseValue = attributeMapping.getValue( baseValue );
			baseValueType = nextDotIndex == -1 ? null : (ManagedMappingType) attributeMapping.getMappedType();
			return getPropertyValue( baseValue, baseValueType, propertyName, nextDotIndex );
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
		return getVersionMapping() == null ? null
				: getVersionMapping().getVersionAttribute().getPropertyAccess().getGetter().get( object );
	}

	@Override
	public Object instantiate(Object id, SharedSessionContractImplementor session) {
		final Object instance = getRepresentationStrategy().getInstantiator().instantiate( session.getFactory() );
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
		final BytecodeLazyAttributeInterceptor interceptor =
				getEntityMetamodel().getBytecodeEnhancementMetadata()
						.extractLazyInterceptor( entity );
		if ( interceptor != null ) {
			interceptor.setSession( session );
		}
	}

	@Override
	public boolean isInstance(Object object) {
		return getRepresentationStrategy().getInstantiator().isInstance( object, getFactory() );
	}

	@Override
	public boolean hasUninitializedLazyProperties(Object object) {
		return entityMetamodel.getBytecodeEnhancementMetadata().hasUnFetchedAttributes( object );
	}

	@Override
	public void resetIdentifier(
			Object entity,
			Object currentId,
			Object currentVersion,
			SharedSessionContractImplementor session) {
		if ( entityMetamodel.getIdentifierProperty().getGenerator() instanceof Assigned ) {
			return;
		}

		// reset the identifier
		final Object defaultIdentifier = identifierMapping.getUnsavedStrategy().getDefaultValue( currentId );
		setIdentifier( entity, defaultIdentifier, session );

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
				&& !getRepresentationStrategy().getInstantiator().isSameClass( instance, factory ) ) {
			// todo (6.0) : this previously used `org.hibernate.tuple.entity.EntityTuplizer#determineConcreteSubclassEntityName`
			//		- we may need something similar here...
			for ( EntityMappingType subclassMappingType : subclassMappingTypes.values() ) {
				if ( subclassMappingType.getEntityPersister().getRepresentationStrategy()
						.getInstantiator().isSameClass(instance, factory) ) {
					return subclassMappingType.getEntityPersister();
				}
			}
		}
		return this;
	}

	@Override @Deprecated(since = "6.0")
	public boolean isMultiTable() {
		return hasMultipleTables();
	}

	protected boolean hasMultipleTables() {
		return false;
	}

	public int getPropertySpan() {
		return entityMetamodel.getPropertySpan();
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

		final Object[] result = new Object[attributeMappings.size()];
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			result[i] = getterCache[i].getForInsert( entity, mergeMap, session );
		}
		return result;
	}

	protected boolean shouldGetAllProperties(Object entity) {
		final BytecodeEnhancementMetadata metadata = getEntityMetamodel().getBytecodeEnhancementMetadata();
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
			throw new UnsupportedOperationException( "Entity has no insert-generated properties - `" + getEntityName() + "`" );
		}
		insertGeneratedValuesProcessor.processGeneratedValues( entity, id, state, generatedValues, session );
	}

	protected List<? extends ModelPart> initInsertGeneratedProperties(List<AttributeMapping> generatedAttributes) {
		final int originalSize = generatedAttributes.size();
		final List<ModelPart> generatedBasicAttributes = new ArrayList<>( originalSize );
		for ( AttributeMapping generatedAttribute : generatedAttributes ) {
			// todo (7.0) : support non selectable mappings? Component, ToOneAttributeMapping, ...
			if ( generatedAttribute.asBasicValuedModelPart() != null
					&& generatedAttribute.getContainingTableExpression().equals( getRootTableName() ) ) {
				generatedBasicAttributes.add( generatedAttribute );
			}
		}

		final List<ModelPart> identifierList = isIdentifierAssignedByInsert() ?
				List.of( getIdentifierMapping() ) :
				Collections.emptyList();
		if ( originalSize > 0 && generatedBasicAttributes.size() == originalSize ) {
			return Collections.unmodifiableList( combine( identifierList, generatedBasicAttributes ) );
		}
		else  {
			return identifierList;
		}
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
			throw new AssertionFailure( "Entity has no update-generated properties - `" + getEntityName() + "`" );
		}
		updateGeneratedValuesProcessor.processGeneratedValues( entity, id, state, generatedValues, session );
	}

	protected List<? extends ModelPart> initUpdateGeneratedProperties(List<AttributeMapping> generatedAttributes) {
		final int originalSize = generatedAttributes.size();
		final List<ModelPart> generatedBasicAttributes = new ArrayList<>( originalSize );
		for ( AttributeMapping generatedAttribute : generatedAttributes ) {
			if ( generatedAttribute instanceof SelectableMapping
					&& ( (SelectableMapping) generatedAttribute ).getContainingTableExpression().equals( getSubclassTableName( 0 ) ) ) {
				generatedBasicAttributes.add( generatedAttribute );
			}
		}

		if ( generatedBasicAttributes.size() == originalSize ) {
			return Collections.unmodifiableList( generatedBasicAttributes );
		}
		else  {
			return Collections.emptyList();
		}
	}

	@Override
	public List<? extends ModelPart> getUpdateGeneratedProperties() {
		return updateGeneratedProperties;
	}

	@Override
	public String getIdentifierPropertyName() {
		return entityMetamodel.getIdentifierProperty().getName();
	}

	@Override
	public Type getIdentifierType() {
		return entityMetamodel.getIdentifierProperty().getType();
	}

	@Override
	public boolean hasSubselectLoadableCollections() {
		return hasSubselectLoadableCollections;
	}

	@Override
	public boolean hasCollectionNotReferencingPK() {
		return hasCollectionNotReferencingPK;
	}

	@Override
	public int[] getNaturalIdentifierProperties() {
		return entityMetamodel.getNaturalIdentifierProperties();
	}

	protected void verifyHasNaturalId() {
		if ( ! hasNaturalIdentifier() ) {
			throw new HibernateException( "Entity does not define a natural id : " + getEntityName() );
		}
	}

	@Override
	public Object getNaturalIdentifierSnapshot(Object id, SharedSessionContractImplementor session) {
		verifyHasNaturalId();
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef(
					"Getting current natural-id snapshot state for `%s#%s",
					getEntityName(),
					id
			);
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
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef(
					"Resolving natural-id [%s] to id : %s ",
					Arrays.asList( naturalIdValues ),
					infoString( this )
			);
		}
		return getNaturalIdLoader().resolveNaturalIdToId( naturalIdValues, session );
	}

	@Override
	public boolean hasNaturalIdentifier() {
		return entityMetamodel.hasNaturalIdentifier();
	}

	@Override
	public void setPropertyValue(Object object, String propertyName, Object value) {
		final AttributeMapping attributeMapping = findSubPart( propertyName, this ).asAttributeMapping();
		setterCache[attributeMapping.getStateArrayPosition()].set( object, value );
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

	@Override
	public BytecodeEnhancementMetadata getInstrumentationMetadata() {
		return getBytecodeEnhancementMetadata();
	}

	@Override
	public BytecodeEnhancementMetadata getBytecodeEnhancementMetadata() {
		return entityMetamodel.getBytecodeEnhancementMetadata();
	}

	@Override
	public String getTableAliasForColumn(String columnName, String rootAlias) {
		return generateTableAlias( rootAlias, determineTableNumberForColumn( columnName ) );
	}

	public int determineTableNumberForColumn(String columnName) {
		return 0;
	}

	protected String determineTableName(Table table) {
		return MappingModelCreationHelper.getTableIdentifierExpression( table, factory );
	}

	@Override
	public EntityEntryFactory getEntityEntryFactory() {
		return this.entityEntryFactory;
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
	public void forEachAttributeMapping(final IndexedConsumer<? super AttributeMapping> consumer) {
		attributeMappings.indexedForEach( consumer );
	}

	@Override
	public void prepareMappingModel(MappingModelCreationProcess creationProcess) {
		if ( identifierMapping != null ) {
			return;
		}

		final RuntimeModelCreationContext creationContext = creationProcess.getCreationContext();

		final PersistentClass bootEntityDescriptor = creationContext
				.getBootModel()
				.getEntityBinding( getEntityName() );

//		EntityMappingType rootEntityDescriptor;
		if ( superMappingType != null ) {
			( (InFlightEntityMappingType) superMappingType ).prepareMappingModel( creationProcess );
			if ( shouldProcessSuperMapping() ) {
				discriminatorMapping = superMappingType.getDiscriminatorMapping();
				identifierMapping = superMappingType.getIdentifierMapping();
				naturalIdMapping = superMappingType.getNaturalIdMapping();
				versionMapping = superMappingType.getVersionMapping();
				rowIdMapping = superMappingType.getRowIdMapping();
				softDeleteMapping = superMappingType.getSoftDeleteMapping();
			}
			else {
				prepareMappingModel( creationProcess, bootEntityDescriptor );
			}
//			rootEntityDescriptor = superMappingType.getRootEntityDescriptor();
		}
		else {
			prepareMappingModel( creationProcess, bootEntityDescriptor );
//			rootEntityDescriptor = this;
		}

		final EntityMetamodel currentEntityMetamodel = getEntityMetamodel();

		if ( currentEntityMetamodel.isVersioned() ) {
			final BeforeExecutionGenerator generator = currentEntityMetamodel.getVersionGenerator();
			// need to do this here because EntityMetamodel doesn't have the EntityVersionMapping :-(
			versionGenerator = generator == null ? new VersionGeneration( versionMapping ) : generator;
		}

		int stateArrayPosition = getStateArrayInitialPosition( creationProcess );

		final NonIdentifierAttribute[] properties = currentEntityMetamodel.getProperties();
		AttributeMappingsMap.Builder mappingsBuilder = AttributeMappingsMap.builder();
		int fetchableIndex = getFetchableIndexOffset();
		for ( int i = 0; i < currentEntityMetamodel.getPropertySpan(); i++ ) {
			final NonIdentifierAttribute runtimeAttrDefinition = properties[i];
			final Property bootProperty = bootEntityDescriptor.getProperty( runtimeAttrDefinition.getName() );

			if ( superMappingType == null
					|| superMappingType.findAttributeMapping( bootProperty.getName() ) == null ) {
				mappingsBuilder.put(
						runtimeAttrDefinition.getName(),
						generateNonIdAttributeMapping(
								runtimeAttrDefinition,
								bootProperty,
								stateArrayPosition++,
								fetchableIndex++,
								creationProcess
						)
				);
			}
			declaredAttributeMappings = mappingsBuilder.build();
//			else {
				// its defined on the supertype, skip it here
//			}
		}

		getAttributeMappings();

		postProcessAttributeMappings( creationProcess, bootEntityDescriptor );

		final ReflectionOptimizer reflectionOptimizer = representationStrategy.getReflectionOptimizer();
		accessOptimizer = reflectionOptimizer != null ? reflectionOptimizer.getAccessOptimizer() : null;

		// register a callback for after all `#prepareMappingModel` calls have finished.  here we want to delay the
		// generation of `staticFetchableList` because we need to wait until after all subclasses have had their
		// `#prepareMappingModel` called (and their declared attribute mappings resolved)
		creationProcess.registerInitializationCallback(
				"Entity(" + getEntityName() + ") `staticFetchableList` generator",
				() -> {
					final ImmutableAttributeMappingList.Builder builder =
							new ImmutableAttributeMappingList.Builder( attributeMappings.size() );
					visitSubTypeAttributeMappings( builder::add );
					assert superMappingType != null || builder.assertFetchableIndexes();
					staticFetchableList = builder.build();
					return true;
				}
		);

		boolean needsMultiTableInsert = hasMultipleTables();
		if ( needsMultiTableInsert ) {
			creationProcess.registerInitializationCallback(
					"Entity(" + getEntityName() + ") `sqmMultiTableMutationStrategy` interpretation",
					() -> {
						sqmMultiTableMutationStrategy =
								interpretSqmMultiTableStrategy( this, creationProcess );
						if ( sqmMultiTableMutationStrategy == null ) {
							return false;
						}
						else {
							sqmMultiTableMutationStrategy.prepare(
									creationProcess,
									creationContext.getJdbcServices().getBootstrapJdbcConnectionAccess()
							);
							return true;
						}
					}
			);

		}
		else {
			sqmMultiTableMutationStrategy = null;
		}

		if ( !needsMultiTableInsert && getGenerator() instanceof BulkInsertionCapableIdentifierGenerator ) {
			if ( getGenerator() instanceof OptimizableGenerator ) {
				final Optimizer optimizer = ( (OptimizableGenerator) getGenerator() ).getOptimizer();
				needsMultiTableInsert = optimizer != null && optimizer.getIncrementSize() > 1;
			}
		}

		if ( needsMultiTableInsert ) {
			creationProcess.registerInitializationCallback(
					"Entity(" + getEntityName() + ") `sqmMultiTableInsertStrategy` interpretation",
					() -> {
						sqmMultiTableInsertStrategy =
								interpretSqmMultiTableInsertStrategy( this, creationProcess );
						if ( sqmMultiTableInsertStrategy == null ) {
							return false;
						}
						else {
							sqmMultiTableInsertStrategy.prepare(
									creationProcess,
									creationContext.getJdbcServices().getBootstrapJdbcConnectionAccess()
							);
							return true;
						}
					}
			);

		}
		else {
			sqmMultiTableInsertStrategy = null;
		}
	}

	private int getFetchableIndexOffset() {
		if ( superMappingType != null ) {
			final EntityMappingType rootEntityDescriptor = getRootEntityDescriptor();
			int offset = rootEntityDescriptor.getNumberOfDeclaredAttributeMappings();
			for ( EntityMappingType subMappingType : rootEntityDescriptor.getSubMappingTypes() ) {
				if ( subMappingType == this ) {
					break;
				}
				// Determining the number of attribute mappings unfortunately has to be done this way,
				// because calling `subMappingType.getNumberOfDeclaredAttributeMappings()` at this point
				// may produce wrong results because subMappingType might not have completed prepareMappingModel yet
				final int propertySpan = subMappingType.getEntityPersister().getEntityMetamodel().getPropertySpan();
				final int superPropertySpan = subMappingType.getSuperMappingType()
						.getEntityPersister()
						.getEntityMetamodel()
						.getPropertySpan();
				final int numberOfDeclaredAttributeMappings = propertySpan - superPropertySpan;
				offset += numberOfDeclaredAttributeMappings;
			}
			return offset;
		}
		return 0;
	}

	private void prepareMappingModel(MappingModelCreationProcess creationProcess, PersistentClass bootEntityDescriptor) {
		final EntityInstantiator instantiator = getRepresentationStrategy().getInstantiator();
		final Supplier<?> templateInstanceCreator;
		if ( ! instantiator.canBeInstantiated() ) {
			templateInstanceCreator = null;
		}
		else {
			final SessionFactoryImplementor sessionFactory = creationProcess.getCreationContext().getSessionFactory();
			templateInstanceCreator = new LazyValue<>( () -> instantiator.instantiate( sessionFactory ) )::getValue;
		}

		identifierMapping = creationProcess.processSubPart(
				EntityIdentifierMapping.ID_ROLE_NAME,
				(role, process) -> generateIdentifierMapping( templateInstanceCreator, bootEntityDescriptor, process )
		);

		versionMapping = generateVersionMapping( templateInstanceCreator, bootEntityDescriptor, creationProcess );

		if ( rowIdName == null ) {
			rowIdMapping = null;
		}
		else {
			rowIdMapping = creationProcess.processSubPart(
					rowIdName,
					(role, process) -> new EntityRowIdMappingImpl( rowIdName, getTableName(), this )
			);
		}

		discriminatorMapping = generateDiscriminatorMapping( bootEntityDescriptor );
		softDeleteMapping = resolveSoftDeleteMapping( this, bootEntityDescriptor, getIdentifierTableName(), creationProcess );

		if ( softDeleteMapping != null ) {
			if ( bootEntityDescriptor.getRootClass().getCustomSQLDelete() != null ) {
				throw new UnsupportedMappingException( "Entity may not define both @SoftDelete and @SQLDelete" );
			}
		}
	}

	private static SoftDeleteMapping resolveSoftDeleteMapping(
			AbstractEntityPersister persister,
			PersistentClass bootEntityDescriptor,
			String identifierTableName,
			MappingModelCreationProcess creationProcess) {
		final RootClass rootClass = bootEntityDescriptor.getRootClass();
		return SoftDeleteHelper.resolveSoftDeleteMapping(
				persister,
				rootClass,
				identifierTableName,
				creationProcess.getCreationContext().getJdbcServices().getDialect()
		);
	}

	private void postProcessAttributeMappings(MappingModelCreationProcess creationProcess, PersistentClass bootEntityDescriptor) {
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

	protected NaturalIdMapping generateNaturalIdMapping(MappingModelCreationProcess creationProcess, PersistentClass bootEntityDescriptor) {
		//noinspection AssertWithSideEffects
		assert bootEntityDescriptor.hasNaturalId();

		final int[] naturalIdAttributeIndexes = entityMetamodel.getNaturalIdentifierProperties();
		assert naturalIdAttributeIndexes.length > 0;

		if ( naturalIdAttributeIndexes.length == 1 ) {
			final String propertyName = entityMetamodel.getPropertyNames()[ naturalIdAttributeIndexes[ 0 ] ];
			final AttributeMapping attributeMapping = findAttributeMapping( propertyName );
			final SingularAttributeMapping singularAttributeMapping = (SingularAttributeMapping) attributeMapping;
			return new SimpleNaturalIdMapping( singularAttributeMapping, this, creationProcess );
		}

		// collect the names of the attributes making up the natural-id.
		final Set<String> attributeNames = setOfSize( naturalIdAttributeIndexes.length );
		for ( int naturalIdAttributeIndex : naturalIdAttributeIndexes ) {
			attributeNames.add( this.getPropertyNames()[ naturalIdAttributeIndex ] );
		}

		// then iterate over the attribute mappings finding the ones having names
		// in the collected names.  iterate here because it is already alphabetical

		final List<SingularAttributeMapping> collectedAttrMappings = new ArrayList<>();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping attributeMapping = attributeMappings.get( i );
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

		final EntityMappingType superMappingType = entityMappingDescriptor.getSuperMappingType();
		if ( superMappingType != null ) {
			final SqmMultiTableMutationStrategy sqmMultiTableMutationStrategy =
					superMappingType.getSqmMultiTableMutationStrategy();
			if ( sqmMultiTableMutationStrategy != null ) {
				return sqmMultiTableMutationStrategy;
			}
		}

		final ServiceRegistry serviceRegistry = creationProcess.getCreationContext().getServiceRegistry();
		return serviceRegistry.requireService( SqmMultiTableMutationStrategyProvider.class )
				.createMutationStrategy( entityMappingDescriptor, creationProcess );
	}

	protected static SqmMultiTableInsertStrategy interpretSqmMultiTableInsertStrategy(
			AbstractEntityPersister entityMappingDescriptor,
			MappingModelCreationProcess creationProcess) {
		final ServiceRegistry serviceRegistry = creationProcess.getCreationContext().getServiceRegistry();
		return serviceRegistry.requireService( SqmMultiTableMutationStrategyProvider.class )
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
			final Integer precision;
			final Integer scale;
			if ( getDiscriminatorFormulaTemplate() == null ) {
				final Column column = bootEntityDescriptor.getDiscriminator() == null
						? null
						: bootEntityDescriptor.getDiscriminator().getColumns().get( 0 );
				discriminatorColumnExpression = getDiscriminatorColumnReaders();
				if ( column == null ) {
					columnDefinition = null;
					length = null;
					precision = null;
					scale = null;
				}
				else {
					columnDefinition = column.getSqlType();
					length = column.getLength();
					precision = column.getPrecision();
					scale = column.getScale();
				}
			}
			else {
				discriminatorColumnExpression = getDiscriminatorFormulaTemplate();
				columnDefinition = null;
				length = null;
				precision = null;
				scale = null;
			}
			return new ExplicitColumnDiscriminatorMappingImpl(
					this,
					discriminatorColumnExpression,
					getTableName(),
					discriminatorColumnExpression,
					getDiscriminatorFormulaTemplate() != null,
					isPhysicalDiscriminator(),
					false,
					columnDefinition,
					null,
					length,
					precision,
					scale,
					(DiscriminatorType<?>) getTypeDiscriminatorMetadata().getResolutionType()
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
					getPropertyNames()[getVersionProperty()],
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
			final InFlightEntityMappingType inFlightEntityMappingType = (InFlightEntityMappingType) superMappingType;
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

		if ( idType instanceof CompositeType ) {
			final CompositeType cidType = (CompositeType) idType;

			// NOTE: the term `isEmbedded` here uses Hibernate's older (pre-JPA) naming for its "non-aggregated"
			// composite-id support.  It unfortunately conflicts with the JPA usage of "embedded".  Here we normalize
			// the legacy naming to the more descriptive encapsulated versus non-encapsulated phrasing

			final boolean encapsulated = !cidType.isEmbedded();
			if ( encapsulated ) {
				// we have an `@EmbeddedId`
				return MappingModelCreationHelper.buildEncapsulatedCompositeIdentifierMapping(
						this,
						bootEntityDescriptor.getIdentifierProperty(),
						bootEntityDescriptor.getIdentifierProperty().getName(),
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
		final Integer precision;
		final Integer scale;
		if ( bootEntityDescriptor.getIdentifier() == null ) {
			columnDefinition = null;
			length = null;
			precision = null;
			scale = null;
		}
		else {
			Column column = bootEntityDescriptor.getIdentifier().getColumns().get( 0 );
			columnDefinition = column.getSqlType();
			length = column.getLength();
			precision = column.getPrecision();
			scale = column.getScale();
		}

		final Value value = bootEntityDescriptor.getIdentifierProperty().getValue();
		return new BasicEntityIdentifierMappingImpl(
				this,
				templateInstanceCreator,
				bootEntityDescriptor.getIdentifierProperty().getName(),
				getTableName(),
				rootTableKeyColumnNames[0],
				columnDefinition,
				length,
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
		return MappingModelCreationHelper.buildNonEncapsulatedCompositeIdentifierMapping(
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
		final Property versionProperty = bootModelRootEntityDescriptor.getVersion();
		final BasicValue bootModelVersionValue = (BasicValue) versionProperty.getValue();
		final BasicValue.Resolution<?> basicTypeResolution = bootModelVersionValue.resolve();

		final Column column = (Column) bootModelVersionValue.getColumn();
		final Dialect dialect = creationProcess.getCreationContext().getDialect();

		return new EntityVersionMappingImpl(
				bootModelRootEntityDescriptor.getRootClass(),
				templateInstanceCreator,
				bootModelRootEntityDescriptor.getVersion().getName(),
				entityPersister.getTableName(),
				column.getText( dialect ),
				column.getSqlType(),
				column.getLength(),
				column.getPrecision(),
				column.getScale(),
				column.getTemporalPrecision(),
				basicTypeResolution.getLegacyResolvedBasicType(),
				entityPersister,
				creationProcess
		);
	}

	protected AttributeMapping generateNonIdAttributeMapping(
			NonIdentifierAttribute tupleAttrDefinition,
			Property bootProperty,
			int stateArrayPosition,
			int fetchableIndex,
			MappingModelCreationProcess creationProcess) {
		final RuntimeModelCreationContext creationContext = creationProcess.getCreationContext();

		final String attrName = tupleAttrDefinition.getName();
		final Type attrType = tupleAttrDefinition.getType();

		final int propertyIndex = getPropertyIndex( bootProperty.getName() );

		final String tableExpression = getTableName( getPropertyTableNumbers()[propertyIndex] );
		final String[] attrColumnNames = getPropertyColumnNames( propertyIndex );

		final PropertyAccess propertyAccess = getRepresentationStrategy().resolvePropertyAccess( bootProperty );

		final Value value = bootProperty.getValue();
		if ( propertyIndex == getVersionProperty() ) {
			Column column = value.getColumns().get( 0 );
			return MappingModelCreationHelper.buildBasicAttributeMapping(
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
				precision = column.getPrecision();
				temporalPrecision = column.getTemporalPrecision();
				scale = column.getScale();
				isLob = column.isSqlTypeLob( creationProcess.getCreationContext().getMetadata() );
				nullable = column.isNullable();
			}
			else {
				final BasicValue basicBootValue = (BasicValue) value;

				if ( attrColumnNames[ 0 ] != null ) {
					attrColumnExpression = attrColumnNames[ 0 ];
					isAttrColumnExpressionFormula = false;

					final List<Selectable> selectables = basicBootValue.getSelectables();
					assert !selectables.isEmpty();
					final Selectable selectable = selectables.get(0);

					assert attrColumnExpression.equals( selectable.getText( creationContext.getDialect() ) );

					customReadExpr = selectable.getTemplate(
							creationContext.getDialect(),
							creationContext.getTypeConfiguration(),
							creationContext.getFunctionRegistry()
					);
					customWriteExpr = selectable.getWriteExpr( (JdbcMapping) attrType, creationContext.getDialect() );
					Column column = value.getColumns().get( 0 );
					columnDefinition = column.getSqlType();
					length = column.getLength();
					precision = column.getPrecision();
					temporalPrecision = column.getTemporalPrecision();
					scale = column.getScale();
					nullable = column.isNullable();
					isLob = column.isSqlTypeLob( creationContext.getMetadata() );
					MappingModelCreationHelper.resolveAggregateColumnBasicType( creationProcess, role, column );
				}
				else {
					final String[] attrColumnFormulaTemplate = propertyColumnFormulaTemplates[ propertyIndex ];
					attrColumnExpression = attrColumnFormulaTemplate[ 0 ];
					isAttrColumnExpressionFormula = true;
					customReadExpr = null;
					customWriteExpr = null;
					columnDefinition = null;
					length = null;
					precision = null;
					temporalPrecision = null;
					scale = null;
					nullable = true;
					isLob = false;
				}
			}

			return MappingModelCreationHelper.buildBasicAttributeMapping(
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
		else if ( attrType instanceof AnyType ) {
			final JavaType<Object> baseAssociationJtd =
					creationContext.getTypeConfiguration().getJavaTypeRegistry()
							.getDescriptor( Object.class );

			final AnyType anyType = (AnyType) attrType;

			final MutabilityPlan<?> mutabilityPlan = new DiscriminatedAssociationAttributeMapping.MutabilityPlanImpl( anyType );
			final SimpleAttributeMetadata attributeMetadataAccess = new SimpleAttributeMetadata(
					propertyAccess,
					mutabilityPlan,
					bootProperty.isOptional(),
					bootProperty.isInsertable(),
					bootProperty.isUpdateable(),
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
					(AnyType) attrType,
					(Any) value,
					creationProcess
			);
		}
		else if ( attrType instanceof CompositeType ) {
			DependantValue dependantValue = null;
			if ( bootProperty.getValue() instanceof DependantValue ) {
				dependantValue = ( (DependantValue) bootProperty.getValue() );
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
		else if ( attrType instanceof EntityType ) {
			return buildSingularAssociationAttributeMapping(
					attrName,
					getNavigableRole().append( attrName ),
					stateArrayPosition,
					fetchableIndex,
					bootProperty,
					this,
					this,
					(EntityType) attrType,
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
	public TableDetails getSoftDeleteTableDetails() {
		return getIdentifierTableDetails();
	}

	@Override
	public AttributeMappingsList getAttributeMappings() {
		if ( attributeMappings == null ) {
			int sizeHint = declaredAttributeMappings.size();
			sizeHint += (superMappingType == null ? 0 : superMappingType.getAttributeMappings().size() );
			ImmutableAttributeMappingList.Builder builder = new ImmutableAttributeMappingList.Builder( sizeHint );

			if ( superMappingType != null ) {
				superMappingType.forEachAttributeMapping( builder::add );
			}

			for ( AttributeMapping am : declaredAttributeMappings.valueIterator() ) {
				builder.add( am );
			}
			this.attributeMappings = builder.build();
			final Getter[] getters = new Getter[attributeMappings.size()];
			final Setter[] setters = new Setter[attributeMappings.size()];
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final PropertyAccess propertyAccess = attributeMappings.get( i ).getAttributeMetadata().getPropertyAccess();
				getters[i] = propertyAccess.getGetter();
				setters[i] = propertyAccess.getSetter();
			}
			this.getterCache = getters;
			this.setterCache = setters;
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
		final AttributeMapping declaredAttribute = declaredAttributeMappings.get( name );
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
		LOG.tracef( "#findSubPart(`%s`)", name );

		if ( EntityDiscriminatorMapping.matchesRoleName( name ) ) {
			return discriminatorMapping;
		}

		final AttributeMapping declaredAttribute = declaredAttributeMappings.get( name );
		if ( declaredAttribute != null ) {
			return declaredAttribute;
		}

		if ( superMappingType != null ) {
			final ModelPart superDefinedAttribute = superMappingType.findSubPart( name, superMappingType );
			if ( superDefinedAttribute != null ) {
				// Prefer the identifier mapping of the concrete class
				if ( superDefinedAttribute.isEntityIdentifierMapping() ) {
					final ModelPart identifierModelPart = getIdentifierModelPart( name, treatTargetType );
					if ( identifierModelPart != null ) {
						return identifierModelPart;
					}
				}
				return superDefinedAttribute;
			}
		}

		if ( treatTargetType != null ) {
			if ( ! treatTargetType.isTypeOrSuperType( this ) ) {
				return null;
			}

			if ( subclassMappingTypes != null && !subclassMappingTypes.isEmpty() ) {
				for ( EntityMappingType subMappingType : subclassMappingTypes.values() ) {
					if ( ! treatTargetType.isTypeOrSuperType( subMappingType ) ) {
						continue;
					}

					final ModelPart subDefinedAttribute = subMappingType.findSubTypesSubPart( name, treatTargetType );

					if ( subDefinedAttribute != null ) {
						return subDefinedAttribute;
					}
				}
			}
		}
		else {
			if ( subclassMappingTypes != null && !subclassMappingTypes.isEmpty() ) {
				ModelPart attribute = null;
				for ( EntityMappingType subMappingType : subclassMappingTypes.values() ) {
					final ModelPart subDefinedAttribute = subMappingType.findSubTypesSubPart( name, treatTargetType );
					if ( subDefinedAttribute != null ) {
						if ( attribute != null && !MappingModelHelper.isCompatibleModelPart( attribute, subDefinedAttribute ) ) {
							throw new PathException(
									String.format(
											Locale.ROOT,
											"Could not resolve attribute '%s' of '%s' due to the attribute being declared in multiple subtypes '%s' and '%s'",
											name,
											getJavaType().getTypeName(),
											attribute.asAttributeMapping().getDeclaringType()
													.getJavaType().getTypeName(),
											subDefinedAttribute.asAttributeMapping().getDeclaringType()
													.getJavaType().getTypeName()
									)
							);
						}
						attribute = subDefinedAttribute;
					}
				}
				if ( attribute != null ) {
					return attribute;
				}
			}
		}

		final ModelPart identifierModelPart = getIdentifierModelPart( name, treatTargetType );
		if ( identifierModelPart != null ) {
			return identifierModelPart;
		}
		else {
			for ( AttributeMapping attribute : declaredAttributeMappings.valueIterator() ) {
				if ( attribute instanceof EmbeddableValuedModelPart && attribute instanceof VirtualModelPart ) {
					EmbeddableValuedModelPart part = (EmbeddableValuedModelPart) attribute;
					final ModelPart subPart = part.findSubPart( name, null );
					if ( subPart != null ) {
						return subPart;
					}
				}
			}
			return null;
		}
	}

	@Override
	public ModelPart findSubTypesSubPart(String name, EntityMappingType treatTargetType) {
		final AttributeMapping declaredAttribute = declaredAttributeMappings.get( name );
		if ( declaredAttribute != null ) {
			return declaredAttribute;
		}
		else {
			if ( subclassMappingTypes != null && !subclassMappingTypes.isEmpty() ) {
				for ( EntityMappingType subMappingType : subclassMappingTypes.values() ) {
					final ModelPart subDefinedAttribute = subMappingType.findSubTypesSubPart( name, treatTargetType );
					if ( subDefinedAttribute != null ) {
						return subDefinedAttribute;
					}
				}
			}
			return null;
		}
	}

	private ModelPart getIdentifierModelPart(String name, EntityMappingType treatTargetType) {
		final EntityIdentifierMapping identifierMapping = getIdentifierMappingForJoin();
		if ( identifierMapping instanceof NonAggregatedIdentifierMapping ) {
			NonAggregatedIdentifierMapping mapping = (NonAggregatedIdentifierMapping) identifierMapping;
			final ModelPart subPart = mapping.findSubPart( name, treatTargetType );
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
			|| !entityMetamodel.hasNonIdentifierPropertyNamedId() && "id".equals( name );
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
	public int getNumberOfKeyFetchables() {
		return 0;
	}

	@Override
	public Fetchable getKeyFetchable(int position) {
		throw new IndexOutOfBoundsException( position );
	}

	@Override
	public Fetchable getFetchable(int position) {
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
					for ( EntityMappingType subtype : subclassMappingTypes.values() ) {
						final AttributeMappingsMap declaredAttributeMappings = subtype.getDeclaredAttributeMappings();
						for ( AttributeMapping declaredAttributeMapping : declaredAttributeMappings.valueIterator() ) {
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
			final AttributeMapping attributeMapping = attributeMappings.get( i );
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
		final EntityIdentifierMapping identifierMapping = getIdentifierMapping();
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
		final EntityIdentifierMapping identifierMapping = getIdentifierMapping();
		final Object identifier = value == null ? null
				: identifierMapping.disassemble( identifierMapping.getIdentifier( value ), session );
		return identifierMapping.forEachDisassembledJdbcValue( identifier, offset, x, y, consumer, session );
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return hasPartitionedSelectionMapping;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated With no replacement
	 *
	 * @see #getInsertCoordinator()
	 * @see #tableMappings
	 * @see EntityTableMapping#isInsertCallable()
	 */
	@Deprecated( since = "6", forRemoval = true )
	public boolean isInsertCallable(int j) {
		return tableMappings[j].isInsertCallable();
	}

	/**
	 * @deprecated With no replacement
	 *
	 * @see #getUpdateCoordinator()
	 * @see #tableMappings
	 * @see EntityTableMapping#isUpdateCallable()
	 */
	@Deprecated( since = "6", forRemoval = true )
	public boolean isUpdateCallable(int j) {
		return tableMappings[j].isUpdateCallable();
	}

	/**
	 * @deprecated With no replacement
	 *
	 * @see #getDeleteCoordinator()
	 * @see #tableMappings
	 * @see EntityTableMapping#isDeleteCallable()
	 */
	@Deprecated( since = "6", forRemoval = true )
	public boolean isDeleteCallable(int j) {
		return tableMappings[j].isDeleteCallable();
	}

	/**
	 * @deprecated With no replacement
	 */
	@Deprecated( since = "6", forRemoval = true )
	protected boolean isSubclassTableSequentialSelect(int j) {
		return false;
	}

	/**
	 * @deprecated With no replacement.
	 */
	@Deprecated( since = "6", forRemoval = true )
	public EntityMappingType getElementTypeDescriptor() {
		return this;
	}



	/**
	 * @deprecated No longer used
	 */
	@Deprecated( forRemoval = true )
	@Remove
	public abstract boolean isTableCascadeDeleteEnabled(int j);

	/**
	 * @deprecated No longer used
	 */
	@Deprecated(forRemoval = true)
	@Remove
	protected boolean isInverseSubclassTable(int j) {
		return false;
	}

	/**
	 * @deprecated No longer used.  See {@link #getDeleteCoordinator()}
	 */
	@Deprecated(forRemoval = true)
	@Remove
	public String[] getSQLDeleteStrings() {
		return extractSqlStrings( deleteCoordinator.getStaticMutationOperationGroup() );
	}

	private String[] extractSqlStrings(MutationOperationGroup operationGroup) {
		final int numberOfOperations = operationGroup.getNumberOfOperations();
		final String[] strings = new String[numberOfOperations];
		for ( int i = 0; i < numberOfOperations; i++ ) {
			final MutationOperation operation = operationGroup.getOperation( i );
			if ( operation instanceof JdbcOperation ) {
				strings[i] = ( (JdbcOperation) operation ).getSqlString();
			}
		}
		return strings;
	}


	/**
	 * @deprecated No longer used.  See {@link #getUpdateCoordinator()}
	 */
	@Deprecated(forRemoval = true)
	@Remove
	public String[] getSQLUpdateStrings() {
		return extractSqlStrings( updateCoordinator.getStaticMutationOperationGroup() );
	}

	/**
	 * Decide which tables need to be updated.
	 * <p>
	 * The return here is an array of boolean values with each index corresponding
	 * to a given table in the scope of this persister.
	 *
	 * @param dirtyProperties The indices of all the entity properties considered dirty.
	 * @param hasDirtyCollection Whether any collections owned by the entity which were considered dirty.
	 *
	 * @return Array of booleans indicating which table require updating.
	 *
	 * @deprecated No longer used.  See {@link UpdateCoordinator}
	 */
	@Deprecated(forRemoval = true)
	@Remove
	public boolean[] getTableUpdateNeeded(final int[] dirtyProperties, boolean hasDirtyCollection) {
		if ( dirtyProperties == null ) {
			return getTableHasColumns(); // for objects that came in via update()
		}
		else {
			boolean[] updateability = getPropertyUpdateability();
			int[] propertyTableNumbers = getPropertyTableNumbers();
			boolean[] tableUpdateNeeded = new boolean[getTableSpan()];
			for ( int property : dirtyProperties ) {
				int table = propertyTableNumbers[property];
				tableUpdateNeeded[table] = tableUpdateNeeded[table]
						|| getPropertyColumnSpan( property ) > 0 && updateability[property];
				if ( getPropertyColumnSpan( property ) > 0 && !updateability[property] ) {
					LOG.ignoreImmutablePropertyModification( getPropertyNames()[property], getEntityName() );
				}
			}
			if ( isVersioned() ) {
				tableUpdateNeeded[0] = tableUpdateNeeded[0]
						|| isVersionIncrementRequired( dirtyProperties, hasDirtyCollection, getPropertyVersionability() );
			}
			return tableUpdateNeeded;
		}
	}

	/**
	 * @deprecated No longer used.  See {@link MutationExecutorService}
	 */
	@Deprecated(forRemoval = true)
	@Remove
	public boolean isBatchable() {
		return optimisticLockStyle().isNone()
			|| !isVersioned() && optimisticLockStyle().isVersion()
			|| getFactory().getSessionFactoryOptions().isJdbcBatchVersionedData();
	}

	/**
	 * Generate the SQL that deletes a row by id (and version)
	 *
	 * @deprecated No longer used.  See {@link DeleteCoordinatorStandard}
	 */
	@Deprecated(forRemoval = true)
	@Remove
	public String generateDeleteString(int j) {
		final Delete delete = new Delete( getFactory() )
				.setTableName( getTableName( j ) )
				.addColumnRestriction( getKeyColumns( j ) );
		if ( j == 0 ) {
			delete.setVersionColumnName( getVersionColumnName() );
		}
		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			delete.setComment( "delete " + getEntityName() );
		}
		return delete.toStatementString();
	}

	/**
	 * Marshall the fields of a persistent instance to a prepared statement
	 *
	 * @deprecated No longer used.
	 */
	@Deprecated(forRemoval = true)
	@Remove
	public int dehydrate(
			final Object id,
			final Object[] fields,
			final Object rowId,
			final boolean[] includeProperty,
			final boolean[][] includeColumns,
			final int j,
			final PreparedStatement ps,
			final SharedSessionContractImplementor session,
			int index,
			boolean isUpdate) throws SQLException, HibernateException {

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Dehydrating entity: {0}", infoString( this, id, getFactory() ) );
		}

		for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
			if ( includeProperty[i] && isPropertyOfTable( i, j )
					&& !lobProperties.contains( i ) ) {
				getPropertyTypes()[i].nullSafeSet( ps, fields[i], index, includeColumns[i], session );
				index += ArrayHelper.countTrue( includeColumns[i] ); //TODO:  this is kinda slow...
			}
		}

		if ( !isUpdate ) {
			index += dehydrateId( id, rowId, ps, session, index );
		}

		// HHH-4635
		// Oracle expects all Lob properties to be last in inserts
		// and updates.  Insert them at the end.
		for ( int i : lobProperties ) {
			if ( includeProperty[i] && isPropertyOfTable( i, j ) ) {
				getPropertyTypes()[i].nullSafeSet( ps, fields[i], index, includeColumns[i], session );
				index += ArrayHelper.countTrue( includeColumns[i] ); //TODO:  this is kinda slow...
			}
		}

		if ( isUpdate ) {
			index += dehydrateId( id, rowId, ps, session, index );
		}

		return index;

	}

	private int dehydrateId(
			final Object id,
			final Object rowId,
			final PreparedStatement ps,
			final SharedSessionContractImplementor session,
			int index) throws SQLException {
		if ( rowId != null ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						String.format(
								"binding parameter [%s] as ROWID - [%s]",
								index,
								rowId
						)
				);
			}

			ps.setObject( index, rowId );
			return 1;
		}
		else if ( id != null ) {
			getIdentifierType().nullSafeSet( ps, id, index, session );
			return getIdentifierColumnSpan();
		}
		return 0;
	}

	@Deprecated( since = "6.2", forRemoval = true )
	@Remove
	protected String[] generateSQLDeleteStrings(Object[] loadedState) {
		int span = getTableSpan();
		String[] deleteStrings = new String[span];
		for ( int j = span - 1; j >= 0; j-- ) {
			final Delete delete = new Delete( getFactory() )
					.setTableName( getTableName( j ) )
					.addColumnRestriction( getKeyColumns( j ) );
			if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
				delete.setComment( "delete " + getEntityName() + " [" + j + "]" );
			}

			boolean[] versionability = getPropertyVersionability();
			Type[] types = getPropertyTypes();
			for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
				if ( isPropertyOfTable( i, j ) && versionability[i] ) {
					// this property belongs to the table and it is not specifically
					// excluded from optimistic locking by optimistic-lock="false"
					String[] propertyColumnNames = getPropertyColumnNames( i );
					boolean[] propertyNullness = types[i].toColumnNullness( loadedState[i], getFactory() );
					for ( int k = 0; k < propertyNullness.length; k++ ) {
						if ( propertyNullness[k] ) {
							delete.addColumnRestriction( propertyColumnNames[k] );
						}
						else {
							delete.addColumnIsNullRestriction( propertyColumnNames[k] );
						}
					}
				}
			}
			deleteStrings[j] = delete.toStatementString();
		}
		return deleteStrings;
	}

	@Deprecated( since = "6.2", forRemoval = true )
	@Remove
	public final boolean isAllNull(Object[] array, int tableNumber) {
		for ( int i = 0; i < array.length; i++ ) {
			if ( isPropertyOfTable( i, tableNumber ) && array[i] != null ) {
				return false;
			}
		}
		return true;
	}






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

	@Deprecated private final EntityMetamodel entityMetamodel;

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
		if ( !entityMetamodel.hasNonIdentifierPropertyNamedId() ) {
			subclassPropertyAliases.put( ENTITY_ID, getIdentifierAliases() );
			subclassPropertyColumnNames.put( ENTITY_ID, getIdentifierColumnNames() );
		}

		// aliases named identifier ( alias.idname )
		if ( hasIdentifierProperty() ) {
			subclassPropertyAliases.put( getIdentifierPropertyName(), getIdentifierAliases() );
			subclassPropertyColumnNames.put( getIdentifierPropertyName(), getIdentifierColumnNames() );
		}

		// aliases for composite-id's
		if ( getIdentifierType() instanceof ComponentType ) {
			// Fetch embedded identifiers property names from the "virtual" identifier component
			final ComponentType componentId = (ComponentType) getIdentifierType();
			final String[] idPropertyNames = componentId.getPropertyNames();
			final String[] idAliases = getIdentifierAliases();
			final String[] idColumnNames = getIdentifierColumnNames();

			for ( int i = 0; i < idPropertyNames.length; i++ ) {
				if ( entityMetamodel.hasNonIdentifierPropertyNamedId() ) {
					subclassPropertyAliases.put(
							ENTITY_ID + "." + idPropertyNames[i],
							new String[] {idAliases[i]}
					);
					subclassPropertyColumnNames.put(
							ENTITY_ID + "." + getIdentifierPropertyName() + "." + idPropertyNames[i],
							new String[] {idColumnNames[i]}
					);
				}
//				if (hasIdentifierProperty() && !ENTITY_ID.equals( getIdentifierPropertyNames() ) ) {
				if ( hasIdentifierProperty() ) {
					subclassPropertyAliases.put(
							getIdentifierPropertyName() + "." + idPropertyNames[i],
							new String[] {idAliases[i]}
					);
					subclassPropertyColumnNames.put(
							getIdentifierPropertyName() + "." + idPropertyNames[i],
							new String[] {idColumnNames[i]}
					);
				}
				else {
					// embedded composite ids ( alias.idName1, alias.idName2 )
					subclassPropertyAliases.put( idPropertyNames[i], new String[] {idAliases[i]} );
					subclassPropertyColumnNames.put( idPropertyNames[i], new String[] {idColumnNames[i]} );
				}
			}
		}

		if ( entityMetamodel.isPolymorphic() ) {
			subclassPropertyAliases.put( ENTITY_CLASS, new String[] {getDiscriminatorAlias()} );
			subclassPropertyColumnNames.put( ENTITY_CLASS, new String[] {getDiscriminatorColumnName()} );
		}

	}

	private void internalInitSubclassPropertyAliasesMap(String path, List<Property> properties) {
		for (Property property : properties) {
			final String name = path == null ? property.getName() : path + "." + property.getName();
			if ( property.isComposite() ) {
				Component component = (Component) property.getValue();
				internalInitSubclassPropertyAliasesMap( name, component.getProperties() );
			}

			String[] aliases = new String[property.getColumnSpan()];
			String[] cols = new String[property.getColumnSpan()];
			int l = 0;
			for ( Selectable selectable: property.getSelectables() ) {
				Dialect dialect = getFactory().getJdbcServices().getDialect();
				aliases[l] = selectable.getAlias( dialect, property.getValue().getTable() );
				cols[l] = selectable.getText(dialect); // TODO: skip formulas?
				l++;
			}

			subclassPropertyAliases.put( name, aliases );
			subclassPropertyColumnNames.put( name, cols );
		}

	}

	/**
	 * Called by Hibernate Reactive
	 *
	 * @deprecated Hibernate no longer uses aliases to read from result sets
	 */
	@Deprecated	@SuppressWarnings("unused")
	protected String[][] getLazyPropertyColumnAliases() {
		return lazyPropertyColumnAliases;
	}

	/**
	 * @deprecated Hibernate no longer uses aliases to read from result sets
	 */
	@Deprecated
	public String getDiscriminatorAlias() {
		return DISCRIMINATOR_ALIAS;
	}

	protected String getSqlWhereStringTableExpression(){
		return sqlWhereStringTableExpression;
	}
}
