/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import org.hibernate.ObjectNotFoundException;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import jakarta.persistence.PessimisticLockScope;
import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.AssertionFailure;
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
import org.hibernate.Timeouts;
import org.hibernate.action.queue.internal.decompose.entity.DeleteDecomposerStandard;
import org.hibernate.action.queue.spi.meta.ColumnDescriptor;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableKeyDescriptor;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeDescriptor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.accessor.MultiValueReader;
import org.hibernate.accessor.MultiValueWriter;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cache.spi.entry.ReferenceCacheEntryImpl;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.cache.spi.entry.StructuredCacheEntry;
import org.hibernate.cache.spi.entry.UnstructuredCacheEntry;
import org.hibernate.cascade.spi.CascadePropertySelection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.internal.EntityLockingStrategyRequestImpl;
import org.hibernate.dialect.lock.spi.LockingStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.profile.internal.FetchProfileAffectee;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.cascade.spi.CascadeStyle;
import org.hibernate.cascade.spi.CascadingAction;
import org.hibernate.engine.internal.EntityCacheRestrictions;
import org.hibernate.engine.internal.FilteredAssociationMapping;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.jpa.spi.EntityCallbacks;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.internal.VersionGeneration;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.insert.InsertReturningDelegate;
import org.hibernate.id.insert.UpdateVersionSelectDelegate;
import org.hibernate.internal.util.ImmutableBitSet;
import org.hibernate.spi.IndexedConsumer;
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
import org.hibernate.mapping.AttributeContainer;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.AttributeMappingsMap;
import org.hibernate.metamodel.mapping.AuditMapping;
import org.hibernate.metamodel.mapping.AuxiliaryMapping;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.mapping.DiscriminatorValue;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.TenantIdMapping;
import org.hibernate.loader.ast.internal.TenantIdLoader;
import org.hibernate.metamodel.mapping.internal.TenantIdMappingImpl;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.LegacyAuxiliaryMutationSupport;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.TemporalMapping;
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
import org.hibernate.action.queue.internal.decompose.entity.DeleteDecomposer;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.persister.entity.mutation.EntityTableMappingImpl;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.action.queue.internal.decompose.entity.InsertDecomposer;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.action.queue.internal.decompose.entity.UpdateDecomposer;
import org.hibernate.persister.filter.internal.FilterHelper;
import org.hibernate.sql.ast.spi.query.predicate.SqlFragmentPredicate;
import org.hibernate.persister.state.spi.StateManagement;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyValueAccessor;
import org.hibernate.query.PathException;
import org.hibernate.query.named.spi.NamedQueryMemento;
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
import org.hibernate.sql.ast.spi.creation.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.creation.SqlAliasBase;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.creation.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.expression.AliasedExpression;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.from.AuxiliaryTableReference;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.StandardTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.from.TableReferenceJoin;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InListPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Junction;
import org.hibernate.sql.ast.spi.query.predicate.NullnessPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.model.builder.MutationGroupBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilder;
import org.hibernate.sql.spi.mutation.jdbc.JdbcMutationOperation;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.entity.internal.EntityResultImpl;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.MappingContext;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;
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
import java.util.function.Function;
import java.util.function.Supplier;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.function.Function.identity;
import static org.hibernate.boot.model.internal.AuditHelper.getOverridesMap;
import static org.hibernate.engine.internal.CacheHelper.fromSharedCache;
import static org.hibernate.engine.internal.CacheHelper.readingFromCache;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.processIfPersistentAttributeInterceptable;
import static org.hibernate.event.jpa.internal.EntityCallbacksFactory.buildCallbacks;
import static org.hibernate.generator.EventType.FORCE_INCREMENT;
import static org.hibernate.dialect.generated.spi.GeneratedValuesSupport.Capability.UPDATE_RETURNING;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getGeneratedValuesDelegate;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.ReflectHelper.isAbstractClass;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.StringHelper.qualifyConditionally;
import static org.hibernate.internal.util.StringHelper.replace;
import static org.hibernate.internal.util.StringHelper.root;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_INT_ARRAY;
import static org.hibernate.internal.util.collections.ArrayHelper.contains;
import static org.hibernate.internal.util.collections.ArrayHelper.indexOf;
import static org.hibernate.internal.util.collections.ArrayHelper.isAllTrue;
import static org.hibernate.internal.util.collections.ArrayHelper.slice;
import static org.hibernate.internal.util.collections.ArrayHelper.to2DStringArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toIntArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toStringArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toTypeArray;
import static org.hibernate.internal.util.collections.CollectionHelper.combine;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.setOfSize;
import static org.hibernate.internal.util.collections.CollectionHelper.toSmallList;
import static org.hibernate.jdbc.Expectations.createExpectation;
import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.supportsSqlArrayType;
import static org.hibernate.metamodel.RepresentationMode.POJO;
import static org.hibernate.metamodel.mapping.EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME;
import static org.hibernate.metamodel.mapping.internal.GeneratedValuesProcessor.getGeneratedAttributes;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildBasicAttributeMapping;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildEncapsulatedCompositeIdentifierMapping;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildNonEncapsulatedCompositeIdentifierMapping;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.resolveAggregateColumnBasicType;
import static org.hibernate.metamodel.mapping.internal.MappingModelHelper.isCompatibleModelPart;
import static org.hibernate.pretty.MessageHelper.infoString;
import static org.hibernate.spi.NavigablePath.IDENTIFIER_MAPPER_PROPERTY;
import static org.hibernate.sql.ast.spi.creation.SqlExpressionResolver.createColumnReferenceKey;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * Basic functionality for persisting an entity via JDBC, using either generated or custom SQL.
 *
 * @author Gavin King
 */
@Internal
@SuppressWarnings("deprecation")
public abstract class AbstractEntityPersister
		extends BaseEntityPersister
		implements EntityPersister, InFlightEntityMappingType, LazyPropertyInitializer, FetchProfileAffectee, Joinable {

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

	private final String sqlAliasStem;
	@Nullable
	private final String jpaEntityName;

	private final EntityCallbacks<Object> jpaCallbacks;

	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private SingleIdEntityLoader<?> singleIdLoader;
	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private MultiIdEntityLoader<?> multiIdLoader;
	@Nullable
	private NaturalIdLoader<?> naturalIdLoader;
	@Nullable
	private MultiNaturalIdLoader<?> multiNaturalIdLoader;

	private final String[] rootTableKeyColumnNames;
//	private final String[] rootTableKeyColumnReaders;
//	private final String[] rootTableKeyColumnReaderTemplates;
	private final String[] identifierAliases;
	private final int identifierColumnSpan;
	@Nullable
	private final String versionColumnName;
	private final boolean hasFormulaProperties;
	protected final int batchSize;
	private final boolean hasSubselectLoadableCollections;
	private final boolean hasSubselectLoadableAttributes;
	private final boolean hasPartitionedSelectionMapping;
	private final boolean hasCollectionNotReferencingPK;
	@Nullable
	protected final String rowIdName;

	// The optional SQL string defined in the where attribute
	@Nullable
	private final String sqlWhereStringTableExpression;
	@Nullable
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
	private final boolean[] propertyTemporalExcluded;
	private final boolean[] propertyAuditedExcluded;
	private final boolean hasTemporalExcludedProperties;
	private final int[] immutablePropertyIndexes;

	//information about lazy properties of this class
	private final String[] lazyPropertyNames;
	private final int[] lazyPropertyNumbers;
	private final Type[] lazyPropertyTypes;
	private final Set<String> nonLazyPropertyNames;

	//information about all properties in class hierarchy
	private final String[] subclassPropertyNameClosure;
	private final Type[] subclassPropertyTypeClosure;
//	private final String[][] subclassPropertyFormulaTemplateClosure;
	private final String[][] subclassPropertyColumnNameClosure;
//	private final String[][] subclassPropertyColumnReaderClosure;
//	private final String[][] subclassPropertyColumnReaderTemplateClosure;
	private final FetchStyle[] subclassPropertyFetchStyleClosure;

	private final StateManagement stateManagement;

	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private Map<String, SingleIdArrayLoadPlan> lazyLoadPlanByFetchGroup;
	private final LockModeEnumMap<LockingStrategy> lockers = new LockModeEnumMap<>();
	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private String sqlVersionSelectString;

	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private EntityTableDescriptor[] tableDescriptors;
	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private InsertDecomposer insertDecomposer;
	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private UpdateDecomposer updateDecomposer;
	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private DeleteDecomposer deleteDecomposer;

	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private EntityTableMapping[] tableMappings;
	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private InsertCoordinator insertCoordinator;
	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private UpdateCoordinator updateCoordinator;
	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private DeleteCoordinator deleteCoordinator;
	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private UpdateCoordinator mergeCoordinator;

	@Nullable
	private SqmMultiTableMutationStrategy sqmMultiTableMutationStrategy;
	@Nullable
	private SqmMultiTableInsertStrategy sqmMultiTableInsertStrategy;

	private final @Nullable EntityDataAccess cacheAccessStrategy;
	@Nullable
	private final NaturalIdDataAccess naturalIdRegionAccessStrategy;
	private final @Nonnull CacheEntryHelper cacheEntryHelper;
	private final boolean canReadFromCache;
	private final boolean canWriteToCache;
	private final boolean invalidateCache;
	private FilteredAssociationMapping filteredAssociationMapping = FilteredAssociationMapping.NONE;
	private EntityCacheRestrictions cacheRestrictions = EntityCacheRestrictions.NONE;
	private final boolean isLazyPropertiesCacheable;
	private final boolean useReferenceCacheEntries;
	private final boolean useShallowQueryCacheLayout;
	private final boolean storeDiscriminatorInShallowQueryCacheLayout;

	// dynamic filters attached to the class-level
	@Nullable
	private final FilterHelper filterHelper;
	@Nullable
	private volatile Set<String> affectingFetchProfileNames;

	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	protected List<? extends ModelPart> insertGeneratedProperties;
	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	protected List<? extends ModelPart> updateGeneratedProperties;
	@Nullable
	private GeneratedValuesProcessor insertGeneratedValuesProcessor;
	@Nullable
	private GeneratedValuesProcessor updateGeneratedValuesProcessor;

	@Nullable
	private GeneratedValuesMutationDelegate insertDelegate;
	@Nullable
	private GeneratedValuesMutationDelegate updateDelegate;
	@Nullable
	private String identitySelectString;

	private final JavaType<?> javaType;
	private final EntityRepresentationStrategy representationStrategy;

	@Nullable
	private EntityPersister superMappingType;
	@Nullable
	private SortedMap<String, EntityMappingType> subclassMappingTypes;
	private final boolean concreteProxy;
	@Nullable
	private EntityConcreteTypeLoader concreteTypeLoader;

	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private EntityIdentifierMapping identifierMapping;
	@Nullable
	private NaturalIdMapping naturalIdMapping;
	@Nullable
	private EntityVersionMapping versionMapping;
	@Nullable
	private TenantIdMapping tenantIdMapping;
	@Nullable
	private TenantIdLoader tenantIdLoader;
	@Nullable
	private EntityRowIdMapping rowIdMapping;
	@Nullable
	private EntityDiscriminatorMapping discriminatorMapping;
	@Nullable
	private AuxiliaryMapping auxiliaryMapping;

	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private AttributeMappingsList attributeMappings;
	protected AttributeMappingsMap declaredAttributeMappings = AttributeMappingsMap.builder().build();
	protected AttributeMappingsMap declaredGenericAttributeMappings = AttributeMappingsMap.builder().build();
	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	protected AttributeMappingsList staticFetchableList;
	// We build a cache for getters and setters to avoid megamorphic calls
	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private PropertyValueAccessor[] accessorCache;

	@Nullable
	private final String queryLoaderName;

	@Nullable
	protected MultiValueReader multiValueReader;
	@Nullable
	protected MultiValueWriter multiValueWriter;

	protected final String[] fullDiscriminatorSQLValues;
	private final DiscriminatorValue[] fullDiscriminatorValues;

	@Nullable
	private List<UniqueKeyEntry> uniqueKeyEntries = null; //lazily initialized
	@Nullable
	private ConcurrentHashMap<String,SingleIdArrayLoadPlan> nonLazyPropertyLoadPlansByName;

	public AbstractEntityPersister(
			@Nonnull final PersistentClass persistentClass,
			@Nullable final EntityDataAccess cacheAccessStrategy,
			@Nullable final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			@Nonnull final RuntimeModelCreationContext creationContext) throws HibernateException {
		this(
				persistentClass,
				cacheAccessStrategy,
				naturalIdRegionAccessStrategy,
				creationContext,
				identity()
		);
	}

	protected AbstractEntityPersister(
			@Nonnull final PersistentClass persistentClass,
			@Nullable final EntityDataAccess cacheAccessStrategy,
			@Nullable final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			@Nonnull final RuntimeModelCreationContext creationContext,
			@Nonnull final Function<StateManagement, StateManagement> statementManagerConverter)
				throws HibernateException {
		super( persistentClass, creationContext );

		final var factoryOptions = creationContext.getSessionFactoryOptions();

		this.jpaEntityName = persistentClass.getJpaEntityName();
		this.jpaCallbacks =
				buildCallbacks( persistentClass, factoryOptions,
						creationContext.getServiceRegistry() );

		//set it here, but don't call it, since it's still uninitialized!
		factory = creationContext.getSessionFactory();

		sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromEntityName( persistentClass.getEntityName() );

		navigableRole = new NavigableRole( persistentClass.getEntityName() );

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
		multiValueReader = representationStrategy.getMultiValueReader();
		multiValueWriter = representationStrategy.getMultiValueWriter();

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
		hasSubselectLoadableAttributes = persistentClass.hasSubselectLoadableAttributes();
		hasPartitionedSelectionMapping = persistentClass.hasPartitionedSelectionMapping();
		hasCollectionNotReferencingPK = persistentClass.hasCollectionNotReferencingPK();

		// IDENTIFIER

		identifierColumnSpan = persistentClass.getIdentifier().getColumnSpan();
		rootTableKeyColumnNames = new String[identifierColumnSpan];
//		rootTableKeyColumnReaders = new String[identifierColumnSpan];
//		rootTableKeyColumnReaderTemplates = new String[identifierColumnSpan];
		identifierAliases = new String[identifierColumnSpan];

		final var rootTable = persistentClass.getRootTable();
		final String rowId = rootTable.getRowId();
		rowIdName = rowId == null ? null : dialect.getRowIdSupport().resolveExpression( rowId );

		queryLoaderName = persistentClass.getLoaderName();

		final var typeConfiguration = creationContext.getTypeConfiguration();

		final var columns = persistentClass.getIdentifier().getColumns();
		for (int i = 0; i < columns.size(); i++ ) {
			final var column = columns.get(i);
			rootTableKeyColumnNames[i] = column.getQuotedName( dialect );
//			rootTableKeyColumnReaders[i] = column.getReadExpr( dialect );
//			rootTableKeyColumnReaderTemplates[i] = column.getTemplate( dialect, typeConfiguration );
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
					determineTableName( getContainingClass( persistentClass ).getTable() );
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
		propertyTemporalExcluded = new boolean[hydrateSpan];
		propertyAuditedExcluded = new boolean[hydrateSpan];
		sharedColumnNames = new HashSet<>();
		nonLazyPropertyNames = new HashSet<>();
		final List<Integer> immutableProperties = new ArrayList<>();

		final HashSet<Property> thisClassProperties = new HashSet<>();
		boolean foundTemporalExcluded = false;
		boolean foundNonExcludedCollection = false;

		final var propertyClosure = persistentClass.getPropertyClosure();
		boolean foundFormula = false;
		var auditOverrides = getOverridesMap( persistentClass,
				creationContext.getBootstrapContext().getModelsContext() );
		for ( int i = 0; i < propertyClosure.size(); i++ ) {
			final var property = propertyClosure.get(i);
			thisClassProperties.add( property );
			final var propertyValue = property.getValue();

			final boolean temporalExcluded = property.isTemporalExcluded();
			propertyTemporalExcluded[i] = temporalExcluded;
			foundTemporalExcluded = foundTemporalExcluded || temporalExcluded;

			var overrideForProperty = auditOverrides.get( property.getName() );
			if ( overrideForProperty != null ) {
				propertyAuditedExcluded[i] = !overrideForProperty.isAudited();
			}
			else {
				propertyAuditedExcluded[i] = property.isAuditedExcluded();
			}

			foundNonExcludedCollection = foundNonExcludedCollection
					|| propertyValue instanceof org.hibernate.mapping.Collection
							&& !temporalExcluded;

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

			propertyColumnUpdateable[i] = propertyValue.getColumnUpdateability();
			propertyColumnInsertable[i] = propertyValue.getColumnInsertability();

			if ( !property.isMutable() ) {
				immutableProperties.add( i );
			}
		}
		hasTemporalExcludedProperties = foundTemporalExcluded;
		hasFormulaProperties = foundFormula;

		final ArrayList<String> lazyNames = new ArrayList<>();
		final ArrayList<Integer> lazyNumbers = new ArrayList<>();
		final ArrayList<Type> lazyTypes = new ArrayList<>();
		final boolean[] propertyLaziness = getPropertyLaziness();
		final String[] propertyNames = getPropertyNames();
		final Type[] propertyTypes = getPropertyTypes();
		for ( int i = 0; i < propertyLaziness.length; i++ ) {
			if ( propertyLaziness[i] ) {
				lazyNames.add( propertyNames[i] );
				lazyNumbers.add( i );
				lazyTypes.add( propertyTypes[i] );
			}
			else {
				nonLazyPropertyNames.add( propertyNames[i] );
			}
		}
		lazyPropertyNames = toStringArray( lazyNames );
		lazyPropertyNumbers = toIntArray( lazyNumbers );
		lazyPropertyTypes = toTypeArray( lazyTypes );
		immutablePropertyIndexes = toIntArray( immutableProperties );

		// SUBCLASS PROPERTY CLOSURE
		final ArrayList<String> aliases = new ArrayList<>();
		final ArrayList<String> formulaAliases = new ArrayList<>();
		final ArrayList<Type> types = new ArrayList<>();
		final ArrayList<String> names = new ArrayList<>();
//		final ArrayList<String[]> templates = new ArrayList<>();
		final ArrayList<String[]> propColumns = new ArrayList<>();
		final ArrayList<String[]> propColumnAliases = new ArrayList<>();
//		final ArrayList<String[]> propColumnReaders = new ArrayList<>();
//		final ArrayList<String[]> propColumnReaderTemplates = new ArrayList<>();
		final ArrayList<FetchStyle> joinedFetchesList = new ArrayList<>();

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
			final String[] columnAliases = new String[columnSpan];
//			final String[] readers = new String[columnSpan];
//			final String[] readerTemplates = new String[columnSpan];
//			final String[] formulaTemplates = new String[columnSpan];

			final var selectables = prop.getSelectables();
			for ( int i = 0; i < selectables.size(); i++ ) {
				final var selectable = selectables.get(i);
				if ( selectable instanceof Formula ) {
					columnAliases[i] = selectable.getAlias( dialect, prop.getValue().getTable() );
//					formulaTemplates[i] = selectable.getTemplate( dialect, typeConfiguration );
					final String formulaAlias = selectable.getAlias( dialect );
					if ( prop.isSelectable() && !formulaAliases.contains( formulaAlias ) ) {
						formulaAliases.add( formulaAlias );
					}
				}
				else if ( selectable instanceof Column column ) {
					final String quotedColumnName = column.getQuotedName( dialect );
					columnNames[i] = quotedColumnName;
					final String columnAlias = selectable.getAlias( dialect, prop.getValue().getTable() );
					columnAliases[i] = columnAlias;
					if ( prop.isSelectable() && !aliases.contains( columnAlias ) ) {
						aliases.add( columnAlias );
					}
//					readers[i] = column.getReadExpr( dialect );
//					readerTemplates[i] = column.getTemplate( dialect, typeConfiguration );
					if ( thisClassProperties.contains( prop )
							? persistentClass.hasSubclasses()
							: persistentClass.isDefinedOnMultipleSubclasses( column ) ) {
						sharedColumnNames.add( quotedColumnName );
					}
				}
			}
			propColumns.add( columnNames );
			propColumnAliases.add( columnAliases );
//			propColumnReaders.add( readers );
//			propColumnReaderTemplates.add( readerTemplates );
//			templates.add( formulaTemplates );

			joinedFetchesList.add( prop.getValue().getFetchStyle() );
		}
		subclassColumnAliasClosure = toStringArray( aliases );
		subclassFormulaAliasClosure = toStringArray( formulaAliases );

		subclassPropertyNameClosure = toStringArray( names );
		subclassPropertyTypeClosure = toTypeArray( types );
//		subclassPropertyFormulaTemplateClosure = to2DStringArray( templates );
		subclassPropertyColumnNameClosure = to2DStringArray( propColumns );
		subclassPropertyColumnAliasClosure = to2DStringArray( propColumnAliases );
//		subclassPropertyColumnReaderClosure = to2DStringArray( propColumnReaders );
//		subclassPropertyColumnReaderTemplateClosure = to2DStringArray( propColumnReaderTemplates );

		subclassPropertyFetchStyleClosure = new FetchStyle[joinedFetchesList.size()];
		int j = 0;
		for ( var fetchStyle : joinedFetchesList) {
			subclassPropertyFetchStyleClosure[j++] = fetchStyle;
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

		final List<DiscriminatorValue> values = new ArrayList<>();
		final List<String> sqlValues = new ArrayList<>();

		if ( persistentClass.isPolymorphic() && persistentClass.getDiscriminator() != null ) {
			if ( !isAbstract() ) {
				values.add( DiscriminatorHelper.getDiscriminatorValue( persistentClass ) );
				sqlValues.add( DiscriminatorHelper.getDiscriminatorSQLValue( persistentClass, dialect ) );
			}

			final var subclasses = persistentClass.getSubclasses();
			for ( int k = 0; k < subclasses.size(); k++ ) {
				final var subclass = subclasses.get( k );
				if ( !isAbstract( subclass ) ) {
					values.add( DiscriminatorHelper.getDiscriminatorValue( subclass ) );
					sqlValues.add( DiscriminatorHelper.getDiscriminatorSQLValue( subclass, dialect ) );
				}
			}
		}

		fullDiscriminatorSQLValues = toStringArray( sqlValues );
		fullDiscriminatorValues = values.toArray( DiscriminatorValue[]::new );

		if ( hasNamedQueryLoader() ) {
			getNamedQueryMemento( creationContext.getBootModel() );
		}

		// Hibernate Reactive needs to convert the stateManagement so that it can create reactive coordinators
		stateManagement = statementManagerConverter.apply( persistentClass.getRootClass().getStateManagement() );
	}

	@Nonnull
	@Override
	public EntityCallbacks<Object> getEntityCallbacks() {
		return jpaCallbacks;
	}

	@Nonnull
	private static String renderSqlWhereStringTemplate(
			@Nonnull PersistentClass persistentClass, @Nonnull Dialect dialect, @Nonnull TypeConfiguration typeConfiguration) {
		return Template.renderWhereStringTemplate(
				"(" + persistentClass.getWhere() + ")",
				dialect,
				typeConfiguration
		);
	}

	@Nonnull
	private static PersistentClass getContainingClass(@Nonnull PersistentClass persistentClass) {
		var containingClass = persistentClass;
		while ( containingClass.getSuperclass() != null ) {
			final var superclass = containingClass.getSuperclass();
			if ( Objects.equals( persistentClass.getWhere(), superclass.getWhere() ) ) {
				containingClass = superclass;
			}
			else {
				break;
			}
		}
		return containingClass;
	}

	@Nonnull
	private NamedQueryMemento<?> getNamedQueryMemento(@Nullable MetadataImplementor bootModel) {
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
	@Nonnull
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

	@Nonnull
	private SingleIdEntityLoader<?> buildSingleIdEntityLoader(
			@Nonnull LoadQueryInfluencers loadQueryInfluencers,
			@Nullable LockOptions lockOptions) {
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

	private boolean needsOneOffLoader(@Nonnull LockOptions lockOptions) {
		return lockOptions.getLockMode().isPessimistic()
			&& lockOptions.hasNonDefaultOptions();
	}

	@Nonnull
	public static Map<String, String> getEntityNameByTableNameMap(
			@Nonnull PersistentClass persistentClass,
			@Nonnull SqlStringGenerationContext context) {
		final Map<String, String> entityNameByTableNameMap = new HashMap<>();
		PersistentClass superType = persistentClass.getSuperPersistentClass();
		while ( superType != null ) {
			final String entityName = superType.getEntityName();
			entityNameByTableNameMap.put( qualifiedTableName( context, superType ), entityName );
			for ( var join : superType.getJoins() ) {
				entityNameByTableNameMap.put( qualifiedTableName( context, join ), entityName );
			}
			superType = superType.getSuperPersistentClass();
		}
		for ( var subclass : persistentClass.getSubclassClosure() ) {
			final String entityName = subclass.getEntityName();
			entityNameByTableNameMap.put( qualifiedTableName( context, subclass ), entityName );
			for ( var join : subclass.getJoins() ) {
				entityNameByTableNameMap.put( qualifiedTableName( context, join ), entityName );
			}
		}
		return entityNameByTableNameMap;
	}

	@Nonnull
	private static String qualifiedTableName(@Nonnull SqlStringGenerationContext context, @Nonnull AttributeContainer container) {
		return container.getTable().getQualifiedName( context );
	}

	/**
	 * Used by Hibernate Reactive
	 */
	@Nonnull
	protected MultiIdEntityLoader<?> buildMultiIdLoader() {
		return getIdentifierType() instanceof BasicType
			&& supportsSqlArrayType( getDialect() )
				? new MultiIdEntityLoaderArrayParam<>( this, factory )
				: new MultiIdEntityLoaderInPredicate<>( this, identifierColumnSpan, factory );
	}

	@Nullable
	private String getIdentitySelectString(@Nonnull Dialect dialect) {
		final var identifierType = identifierType();
		if ( identifierType != null ) {
			try {
				return dialect.getIdentityColumnSupport()
						.getIdentitySelectString( getTableName( 0 ), getKeyColumns( 0 )[0],
								identifierType.getJdbcType().getDdlTypeCode() );
			}
			catch (MappingException ex) {
				// no proper IdentityColumnSupport in the dialect
				return null;
			}
		}
		else {
			return null;
		}
	}

	private @Nullable BasicType<?> identifierType() {
		if ( getIdentifierType() instanceof BasicType<?> type ) {
			return type;
		}
		else {
			final var componentType = (ComponentType) getIdentifierType();
			final var compositeGenerator = (CompositeNestedGeneratedValueGenerator) getGenerator();
			int position = 0;
			for ( boolean generatedOnExecution : compositeGenerator.getGeneratedOnExecutionColumnInclusions() ) {
				if ( generatedOnExecution ) {
					break;
				}
				position++;
			}
			return getUnderlyingType( factory.getRuntimeMetamodels(), componentType, position );
		}
	}

	@Nonnull
	private static BasicType<?> getUnderlyingType(@Nonnull MappingContext mappingContext, @Nonnull Type type, int typeIndex) {
		if ( type instanceof ComponentType componentType ) {
			int cols = 0;
			for ( var subtype : componentType.getSubtypes() ) {
				final int columnSpan = subtype.getColumnSpan( mappingContext );
				if ( cols+columnSpan > typeIndex ) {
					return getUnderlyingType( mappingContext, subtype, typeIndex-cols );
				}
				cols += columnSpan;
			}
			throw new IndexOutOfBoundsException();
		}
		else if ( type instanceof EntityType entityType ) {
			final var idType = entityType.getIdentifierOrUniqueKeyType( mappingContext );
			return getUnderlyingType( mappingContext, idType, typeIndex );
		}
		else {
			return (BasicType<?>) type;
		}
	}

	static boolean isAbstract(@Nonnull PersistentClass subclass) {
		final Boolean knownAbstract = subclass.isAbstract();
		return knownAbstract == null
				? subclass.hasPojoRepresentation() && isAbstractClass( subclass.getMappedClass() )
				: knownAbstract;
	}

	private boolean shouldUseReferenceCacheEntries(@Nonnull SessionFactoryOptions options) {
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

	@Nonnull
	private static CacheLayout queryCacheLayout(@Nullable CacheLayout entityQueryCacheLayout, @Nonnull SessionFactoryOptions options) {
		return entityQueryCacheLayout == null ? options.getQueryCacheLayout() : entityQueryCacheLayout;
	}

	private boolean shouldUseShallowCacheLayout(@Nullable CacheLayout entityQueryCacheLayout, @Nonnull SessionFactoryOptions options) {
		return switch ( queryCacheLayout( entityQueryCacheLayout, options ) ) {
			case FULL -> false;
			case AUTO -> canUseReferenceCacheEntries() || canReadFromCache();
			default -> true;
		};
	}

	private static boolean shouldStoreDiscriminatorInShallowQueryCacheLayout(
			@Nullable CacheLayout entityQueryCacheLayout, @Nonnull SessionFactoryOptions options) {
		return queryCacheLayout( entityQueryCacheLayout, options ) == CacheLayout.SHALLOW_WITH_DISCRIMINATOR;
	}

	@Nonnull
	protected abstract String[] getSubclassTableNames();

	@Nonnull
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

	@Nonnull
	protected abstract int[] getPropertyTableNumbers();

	private static final String DISCRIMINATOR_ALIAS = "clazz_";

	@Nullable
	@Override
	public String getDiscriminatorColumnName() {
		return DISCRIMINATOR_ALIAS;
	}

	@Nullable
	public String getDiscriminatorColumnReaders() {
		return DISCRIMINATOR_ALIAS;
	}

	@Nullable
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
	public boolean isSubclassEntityName(@Nonnull String entityName) {
		return getSubclassEntityNames().contains( entityName );
	}

	@Override
	public boolean isSharedColumn(@Nonnull String columnExpression) {
		return sharedColumnNames.contains( columnExpression );
	}

	@Nonnull
	@Override
	public String[] getRootTableKeyColumnNames() {
		return rootTableKeyColumnNames;
	}

	@Nullable
	SingleIdArrayLoadPlan getSQLLazySelectLoadPlan(@Nonnull String fetchGroup) {
		return lazyLoadPlanByFetchGroup.get( fetchGroup );
	}

	@Nonnull
	@Override
	public InsertDecomposer getInsertDecomposer() {
		return insertDecomposer;
	}

	@Nonnull
	@Override
	public UpdateDecomposer getUpdateDecomposer() {
		return updateDecomposer;
	}

	@Nonnull
	@Override
	public DeleteDecomposer getDeleteDecomposer() {
		return deleteDecomposer;
	}

	@Nonnull
	@Override
	public InsertCoordinator getInsertCoordinator() {
		return insertCoordinator;
	}

	@Nonnull
	@Override
	public UpdateCoordinator getUpdateCoordinator() {
		return updateCoordinator;
	}

	@Nonnull
	@Override
	public DeleteCoordinator getDeleteCoordinator() {
		return deleteCoordinator;
	}

	@Nonnull
	@Override
	public UpdateCoordinator getMergeCoordinator() {
		return mergeCoordinator;
	}

	@Nonnull
	public String getVersionSelectString() {
		return sqlVersionSelectString;
	}

	@Nullable
	@Internal // called by Hibernate Reactive
	@SuppressWarnings("unused")
	public GeneratedValuesProcessor getInsertGeneratedValuesProcessor() {
		return insertGeneratedValuesProcessor;
	}

	@Nullable
	@Internal // called by Hibernate Reactive
	@SuppressWarnings("unused")
	public GeneratedValuesProcessor getUpdateGeneratedValuesProcessor() {
		return updateGeneratedValuesProcessor;
	}

	@Override
	public boolean hasRowId() {
		return rowIdName != null;
	}

	@Nonnull
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
			@Nonnull PersistentClass persistentClass,
			@Nonnull RuntimeModelCreationContext creationContext) {
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

	private boolean isCacheComplianceEnabled(@Nonnull RuntimeModelCreationContext creationContext) {
		return creationContext.getSessionFactoryOptions()
				.getJpaCompliance()
				.isJpaCacheComplianceEnabled();
	}

	private boolean determineCanWriteToCache(@Nonnull PersistentClass persistentClass, @Nullable EntityDataAccess cacheAccessStrategy) {
		return cacheAccessStrategy != null && persistentClass.isCached();
	}

	private boolean determineCanReadFromCache(@Nonnull PersistentClass persistentClass, @Nullable EntityDataAccess cacheAccessStrategy) {
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

	@Nonnull
	protected CacheEntryHelper buildCacheEntryHelper(@Nonnull SessionFactoryOptions options) {
		if ( cacheAccessStrategy == null ) {
			// the entity defined no caching...
			return NoopCacheEntryHelper.INSTANCE;
		}
		else if ( canUseReferenceCacheEntries() ) {
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

	@Nonnull
	@Override
	public Iterable<UniqueKeyEntry> uniqueKeyEntries() {
		if ( uniqueKeyEntries == null ) {
			uniqueKeyEntries = initUniqueKeyEntries( this );
		}
		return uniqueKeyEntries;
	}

	@Nonnull
	private static List<UniqueKeyEntry> initUniqueKeyEntries(@Nonnull final AbstractEntityPersister persister) {
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

	@Nonnull
	protected Map<String, SingleIdArrayLoadPlan> getLazyLoadPlanByFetchGroup() {
		final var metadata = getBytecodeEnhancementMetadata();
		return metadata.isEnhancedForLazyLoading() && metadata.getLazyAttributesMetadata().hasLazyAttributes()
				? createLazyLoadPlanByFetchGroup( metadata )
				: emptyMap();
	}

	@Nonnull
	private Map<String, SingleIdArrayLoadPlan> createLazyLoadPlanByFetchGroup(@Nonnull BytecodeEnhancementMetadata metadata) {
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

	@Nullable
	private SingleIdArrayLoadPlan createLazyLoadPlan(@Nonnull List<LazyAttributeDescriptor> fetchGroupAttributeDescriptors) {
		final List<ModelPart> partsToSelect = new ArrayList<>( fetchGroupAttributeDescriptors.size() );
		for ( var lazyAttributeDescriptor : fetchGroupAttributeDescriptors ) {
			// all this only really needs to consider properties
			// of this class, not its subclasses, but since we
			// are reusing code used for sequential selects, we
			// use the subclass closure
			partsToSelect.add( getAttributeMapping( getSubclassPropertyIndex( lazyAttributeDescriptor.getName() ) ) );
		}
		return partsToSelect.isEmpty() ? null : createLazyLoanPlan( partsToSelect );
	}

	@Nonnull
	private SingleIdArrayLoadPlan createLazyLoanPlan(@Nonnull List<ModelPart> partsToSelect) {
		assert !partsToSelect.isEmpty();
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
				new SqlAliasBaseManager(),
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

	@Nonnull
	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public boolean containsTableReference(@Nonnull String tableExpression) {
		return contains( getSubclassTableNames(), tableExpression );
	}

	@Nonnull
	@Override
	public String getPartName() {
		return getEntityName();
	}

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		final var entityResult = new EntityResultImpl<T>(
				navigablePath,
				this,
				tableGroup,
				resultVariable
		);
		entityResult.afterInitialize( entityResult, creationState );
		return entityResult;
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState) {
		identifierMapping.applySqlSelections(
				navigablePath.append( identifierMapping.getPartName() ),
				tableGroup,
				creationState
		);
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		identifierMapping.applySqlSelections(
				navigablePath.append( identifierMapping.getPartName() ),
				tableGroup,
				creationState,
				selectionConsumer
		);
	}

	@Nullable
	@Override
	public NaturalIdMapping getNaturalIdMapping() {
		return naturalIdMapping;
	}

	@Nonnull
	@Override
	public TableReference createPrimaryTableReference(
			@Nonnull SqlAliasBase sqlAliasBase,
			@Nonnull SqlAstCreationState creationState) {
		final var loadQueryInfluencers = creationState.getLoadQueryInfluencers();
		final boolean useAuxiliaryTable =
				auxiliaryMapping != null
						&& auxiliaryMapping.useAuxiliaryTable( loadQueryInfluencers );
		final String primaryTableName =
				useAuxiliaryTable
						? castNonNull( auxiliaryMapping ).resolveTableName( getTableName() )
						: getTableName();
		final String primaryAlias = sqlAliasBase.generateNewAlias();
		final var tableReference =
				useAuxiliaryTable
						? new AuxiliaryTableReference( primaryTableName, getTableName(), primaryAlias )
						: new NamedTableReference( primaryTableName, primaryAlias );
		tableReference.applyAuxiliaryTable( auxiliaryMapping, loadQueryInfluencers );
		return tableReference;
	}

	@Nullable
	@Override
	public TableReferenceJoin createTableReferenceJoin(
			@Nonnull String joinTableExpression,
			@Nonnull SqlAliasBase sqlAliasBase,
			@Nonnull TableReference lhs,
			@Nonnull SqlAstCreationState creationState) {
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

	@Nonnull
	protected TableReferenceJoin generateTableReferenceJoin(
			@Nonnull TableReference lhs,
			@Nonnull String joinTableExpression,
			@Nonnull SqlAliasBase sqlAliasBase,
			boolean innerJoin,
			@Nonnull String[] targetColumns,
			@Nonnull SqlAstCreationState creationState) {
		final var joinedTableReference = new NamedTableReference(
				joinTableExpression,
				sqlAliasBase.generateNewAlias(),
				!innerJoin
		);
		joinedTableReference.applyAuxiliaryTable( auxiliaryMapping,
				creationState.getLoadQueryInfluencers() );
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

	@Nonnull
	protected Predicate generateJoinPredicate(
			@Nonnull TableReference rootTableReference,
			@Nonnull TableReference joinedTableReference,
			@Nonnull String[] pkColumnNames,
			@Nonnull String[] fkColumnNames,
			@Nonnull SqlAstCreationState creationState) {
		final var identifierMapping = getIdentifierMapping();

		final var conjunction = new Junction( Junction.Nature.CONJUNCTION );

		assert pkColumnNames.length == fkColumnNames.length;
		assert pkColumnNames.length == identifierMapping.getJdbcTypeCount();

		identifierMapping.forEachSelectable(
				(columnIndex, selection) -> {
					final var sqlExpressionResolver = creationState.getSqlExpressionResolver();

					final String rootPkColumnName = pkColumnNames[ columnIndex ];
					final var pkColumnExpression = sqlExpressionResolver.resolveSqlExpression(
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
					final var fkColumnExpression = sqlExpressionResolver.resolveSqlExpression(
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

	@Nullable
	@Override
	public Object initializeLazyProperty(@Nonnull String fieldName, @Nonnull Object entity, @Nonnull SharedSessionContractImplementor session) {
		return hasCollections() && getPropertyTypes()[getPropertyIndex( fieldName )] instanceof CollectionType collectionType
				? initializedLazyCollection( fieldName, entity, collectionType, session )
				: initializedLazyField( fieldName, entity, session );
	}

	@Nullable
	private Object initializedLazyField(
			@Nonnull String fieldName,
			@Nonnull Object entity,
			@Nonnull SharedSessionContractImplementor session) {
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
				&& isLazyPropertiesCacheable() ) {
			final Object cachedValue = initializeLazyFieldFromCache( fieldName, entity, session, id, entry );
			if ( cachedValue != UNFETCHED_PROPERTY ) {
				// The following should be redundant, since the setter should have set this already.
				// interceptor.attributeInitialized(fieldName);

				// NOTE EARLY EXIT!!!
				return cachedValue;
			}
		}

		return initializeLazyPropertiesFromDatastore( entity, id, entry, fieldName, session );
	}

	@Nonnull
	private Object initializeLazyFieldFromCache(
			@Nonnull String fieldName,
			@Nonnull Object entity,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object id,
			@Nonnull EntityEntry entry) {
		return readingFromCache( this, cache -> {
			final Object cacheKey =
					cache.generateCacheKey(
							id,
							this,
							session.getFactory(),
							session.getTenantIdentifier()
					);
			final Object structuredEntry = fromSharedCache( session, cacheKey, this, cache );
			return structuredEntry == null
					? UNFETCHED_PROPERTY
					: initializeLazyPropertiesFromCache( fieldName, entity, session, entry,
							(CacheEntry) getCacheEntryStructure().destructure( structuredEntry, factory ) );
		}, UNFETCHED_PROPERTY );
	}

	@Nonnull
	private PersistentCollection<?> initializedLazyCollection(
			@Nonnull String fieldName,
			@Nonnull Object entity,
			@Nonnull CollectionType collectionType,
			@Nonnull SharedSessionContractImplementor session) {
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
		final var collection = getCollection( entity, collectionType, session, persister );

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

	@Nonnull
	private static PersistentCollection<?> getCollection(
			@Nonnull Object entity,
			@Nonnull CollectionType collectionType,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CollectionPersister persister) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final var entry = persistenceContext.getEntry( entity );
		final Object key = getCollectionKey( persister, entity, entry, session );
		assert key != null;
		final var collection = persistenceContext.getCollection( session.generateCollectionKey( persister, key ) );
		if ( collection == null ) {
			final var newCollection = collectionType.instantiate( session, persister, key );
			newCollection.setOwner( entity );
			persistenceContext.addUninitializedCollection( persister, newCollection, key,
					entry != null && entry.isReadOnly() );
			return newCollection;
		}
		else {
			return collection;
		}
	}

	public @Nullable static Object getCollectionKey(
			@Nonnull CollectionPersister persister,
			@Nonnull Object owner,
			@Nullable EntityEntry ownerEntry,
			@Nonnull SharedSessionContractImplementor session) {
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

	@Nullable
	protected Object initializeLazyPropertiesFromDatastore(
			@Nonnull final Object entity,
			@Nonnull final Object id,
			@Nonnull final EntityEntry entry,
			@Nonnull final String fieldName,
			@Nonnull final SharedSessionContractImplementor session) {
		return isNonLazyPropertyName( fieldName )
				? initLazyProperty( entity, id, entry, fieldName, session )
				: initLazyProperties( entity, id, entry, fieldName, session );
	}

	// Hibernate Reactive uses this
	protected boolean isNonLazyPropertyName(@Nonnull String fieldName) {
		return nonLazyPropertyNames.contains( fieldName );
	}

	@Nullable
	private Object initLazyProperties(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull EntityEntry entry,
			@Nonnull String fieldName,
			@Nonnull SharedSessionContractImplementor session) {

		assert hasLazyProperties();
		CORE_LOGGER.initializingLazyPropertiesFromDatastore( fieldName );

		final var interceptor = asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
		assert interceptor != null : "Expecting bytecode interceptor to be non-null";

		final var lazyAttributesMetadata = getBytecodeEnhancementMetadata().getLazyAttributesMetadata();
		final String fetchGroup = lazyAttributesMetadata.getFetchGroupName( fieldName );
		final var fetchGroupAttributeDescriptors =
				lazyAttributesMetadata.getFetchGroupAttributeDescriptors( fetchGroup );
		final var lazySelectLoadPlan = castNonNull( getSQLLazySelectLoadPlan( fetchGroup ) );
		try {
			Object finalResult = null;
			final var results = lazySelectLoadPlan.load( id, session );
			if ( results == null ) {
				throw new ObjectNotFoundException( id, getEntityName() );
			}
			final var initializedLazyAttributeNames = interceptor.getInitializedLazyAttributeNames();
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
					"Could not initialize lazy properties: "
							+ infoString( this, id, getFactory() ),
					ex.getSQL()
			);
		}
	}

	@Nullable
	private Object initLazyProperty(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull EntityEntry entry,
			@Nonnull String fieldName,
			@Nonnull SharedSessionContractImplementor session) {
		// An eager property can be lazy because of an applied EntityGraph
		final int propertyIndex = getPropertyIndex( fieldName );
		final var lazyLoanPlan =
				getOrCreateLazyLoadPlan( fieldName,
						List.of( getAttributeMapping( propertyIndex ) ) );
		try {
			final var results = lazyLoanPlan.load( id, session );
			if ( results == null ) {
				throw new ObjectNotFoundException( id, getEntityName() );
			}
			assert results.length > 0;
			final Object result = results[0];
			initializeLazyProperty( entity, entry, result, propertyIndex, getPropertyTypes()[propertyIndex] );
			return result;
		}
		catch (JDBCException ex) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					ex.getSQLException(),
					"Could not initialize lazy properties: "
							+ infoString( this, id, getFactory() ),
					ex.getSQL()
			);
		}
	}

	@Nonnull
	private SingleIdArrayLoadPlan getOrCreateLazyLoadPlan(@Nonnull String fieldName, @Nonnull List<ModelPart> partsToSelect) {
		var plans = nonLazyPropertyLoadPlansByName;
		if ( plans == null ) {
			nonLazyPropertyLoadPlansByName = plans = new ConcurrentHashMap<>();
		}
		else {
			final var lazyLoanPlan = plans.get( fieldName );
			if ( lazyLoanPlan != null ) {
				return lazyLoanPlan;
			}
		}
		final var newLazyLoanPlan = createLazyLoanPlan( partsToSelect );
		plans.put( fieldName, newLazyLoanPlan );
		return newLazyLoanPlan;
	}

	@Nullable
	protected Object initializeLazyPropertiesFromCache(
			@Nonnull final String fieldName,
			@Nonnull final Object entity,
			@Nonnull final SharedSessionContractImplementor session,
			@Nonnull final EntityEntry entry,
			@Nonnull final CacheEntry cacheEntry) {
		CORE_LOGGER.initializingLazyPropertiesFromSecondLevelCache();
		Object result = null;
		final var disassembledValues = cacheEntry.getDisassembledState();
		for ( int j = 0; j < lazyPropertyNames.length; j++ ) {
			final var cachedValue = disassembledValues[lazyPropertyNumbers[j]];
			if ( cachedValue == UNFETCHED_PROPERTY ) {
				if ( fieldName.equals( lazyPropertyNames[j] ) ) {
					result = UNFETCHED_PROPERTY;
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
			@Nonnull final String fieldName,
			@Nonnull final Object entity,
			@Nonnull final EntityEntry entry,
			final int index,
			@Nullable final Object propValue) {
		final int propertyNumber = lazyPropertyNumbers[index];
		setPropertyValue( entity, propertyNumber, propValue );
		final var maybeLazySet = entry.getMaybeLazySet();
		if ( maybeLazySet != null ) {
			final var bitSet = maybeLazySet.toBitSet();
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

	@Nullable
	private Object copiedLazyPropertyValue(int index, @Nullable Object propValue) {
		return lazyPropertyTypes[index].deepCopy( propValue, factory );
	}

	/**
	 * Used by Hibernate Reactive
	 * @deprecated
	 */
	@Deprecated(since = "7.2", forRemoval = true)
	protected boolean initializeLazyProperty(
			@Nonnull final String fieldName,
			@Nonnull final Object entity,
			@Nonnull final EntityEntry entry,
			@Nonnull final LazyAttributeDescriptor fetchGroupAttributeDescriptor,
			@Nullable final Object propValue) {
		final String name = fetchGroupAttributeDescriptor.getName();
		initializeLazyProperty( entity, entry, propValue,
				getPropertyIndex( name ),
				fetchGroupAttributeDescriptor.getType() );
		return fieldName.equals( name );
	}

	// Used by Hibernate Reactive
	protected void initializeLazyProperty(@Nonnull Object entity, @Nonnull EntityEntry entry, @Nullable Object propValue, int index, @Nonnull Type type) {
		setPropertyValue( entity, index, propValue );
		final var maybeLazySet = entry.getMaybeLazySet();
		if ( maybeLazySet != null ) {
			final var bitSet = maybeLazySet.toBitSet();
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

	@Nonnull
	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Nonnull
	@Override
	public Serializable[] getQuerySpaces() {
		return getPropertySpaces();
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Nonnull
	@Override
	public String[] getIdentifierColumnNames() {
		return rootTableKeyColumnNames;
	}

	public int getIdentifierColumnSpan() {
		return identifierColumnSpan;
	}

	@Nonnull
	public String[] getIdentifierAliases() {
		return identifierAliases;
	}

	@Nullable
	@Override
	public String getVersionColumnName() {
		return versionColumnName;
	}

	@Nonnull
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
		return invalidateCache || cacheRestrictions.hasSqlRestrictions();
	}

	@Override
	public boolean isLazyPropertiesCacheable() {
		return isLazyPropertiesCacheable;
	}

	@Nonnull
	@Override
	public String selectFragment(@Nonnull String alias, @Nonnull String suffix) {
		final var rootQuerySpec = new QuerySpec( true );
		final var sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				LockOptions.NONE,
				this::fetchProcessor,
				true,
				new LoadQueryInfluencers( factory ),
				factory.getSqlTranslationEngine()
		) {
			@Override
			public boolean isProcedureOrNativeQuery() {
				return true;
			}
		};

		final var entityPath = new NavigablePath( getRootPathName() );
		final var rootTableGroup = createSelectFragmentRootTableGroup(
				alias,
				entityPath,
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
			final var selectableMapping = castNonNull( discriminatorMapping ).getSelectable( 0 );
			if ( processedExpressions.add( selectableMapping.getSelectionExpression() ) ) {
				aliasSelection( sqlSelections, i, castNonNull( getDiscriminatorAlias( suffix ) ) );
				i++;
			}
		}

		if ( rowIdMapping != null && processedExpressions.add( rowIdMapping.getSelectionExpression() ) ) {
			aliasSelection( sqlSelections, i, ROWID_ALIAS + suffix );
			i++;
		}

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
								? subclassFormulaAliasClosure[formulaIndex++]
								: subclassColumnAliasClosure[columnIndex++];
						aliasSelection( sqlSelections, i, baseAlias + suffix );
						i++;
					}
				}
			}
		}

		final String sql =
				getDialect().getSqlAstTranslatorFactory()
						.buildTranslator( new SqlAstTranslationRequest.Select( getFactory(), new SelectStatement( rootQuerySpec ) ) )
						.translate( null, QueryOptions.NONE )
						.getSqlString();
		final int fromIndex = sql.lastIndexOf( " from" );
		return fromIndex != -1
				? sql.substring( "select ".length(), fromIndex )
				: sql.substring( "select ".length() );
	}

	private static void aliasSelection(
			@Nonnull List<SqlSelection> sqlSelections,
			int selectionIndex,
			@Nonnull String alias) {
		final var expression = sqlSelections.get( selectionIndex ).getExpression();
		sqlSelections.set( selectionIndex,
				new SqlSelectionImpl( selectionIndex, new AliasedExpression( expression, alias ) ) );
	}

	@Nonnull
	private TableGroup createSelectFragmentRootTableGroup(
			@Nonnull String alias,
			@Nonnull NavigablePath entityPath,
			@Nonnull SqlAstCreationState sqlAstCreationState) {

		final TableReference rootTableReference = new NamedTableReference(
				getTableName(),
				alias
		);
		return new StandardTableGroup(
				true,
				entityPath,
				this,
				null,
				rootTableReference,
				true,
				new SqlAliasBaseConstant( alias ),
				getRootEntityDescriptor()::containsTableReference,
				(tableExpression, tg) -> {
					final String[] subclassTableNames = getSubclassTableNames();
					for ( int i = 0; i < subclassTableNames.length; i++ ) {
						if ( tableExpression.equals( subclassTableNames[i] ) ) {
							final NamedTableReference joinedTableReference = new NamedTableReference(
									tableExpression,
									generateTableAlias( alias, i ),
									isNullableSubclassTable( i )
							);
							return new TableReferenceJoin(
									shouldInnerJoinSubclassTable( i, emptySet() ),
									joinedTableReference,
									generateJoinPredicate(
											rootTableReference,
											joinedTableReference,
											needsDiscriminator()
													? getRootTableKeyColumnNames()
													: getIdentifierColumnNames(),
											getSubclassTableKeyColumns( i ),
											sqlAstCreationState
									)
							);
						}
					}
					return null;
				},
				getFactory()
		);
	}

	@Nonnull
	private ImmutableFetchList fetchProcessor(@Nonnull FetchParent fetchParent, @Nonnull LoaderSqlAstCreationState creationState) {
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

	private boolean skipFetchable(@Nonnull Fetchable fetchable, @Nonnull FetchTiming fetchTiming) {
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

	@Nonnull
	@Override
	public String[] getIdentifierAliases(@Nonnull String suffix) {
		// NOTE: this assumes something about how propertySelectFragment is implemented by the subclass!
		// was toUnquotedAliasStrings( getIdentifierColumnNames() ) before - now tried
		// to remove that unquoting and missing aliases
		return new Alias( suffix ).toAliasStrings( getIdentifierAliases() );
	}

	@Nonnull
	@Override
	public String[] getPropertyAliases(@Nonnull String suffix, int i) {
		// NOTE: this assumes something about how propertySelectFragment is implemented by the subclass!
		return new Alias( suffix ).toUnquotedAliasStrings( propertyColumnAliases[i] );
	}

	@Nullable
	@Override
	public String getDiscriminatorAlias(@Nonnull String suffix) {
		// NOTE: this assumes something about how propertySelectFragment is implemented by the subclass!
		// toUnquotedAliasStrings( getDiscriminatorColumnName() ) before - now tried
		// to remove that unquoting and missing aliases
		return hasSubclasses()
				? new Alias( suffix ).toAliasString( getDiscriminatorAlias() )
				: null;
	}

	@Nullable
	@Override
	public Object[] getDatabaseSnapshot(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) throws HibernateException {
		return singleIdLoader.loadDatabaseSnapshot( id, session );
	}

	@Nullable
	@Override
	public Object getIdByUniqueKey(@Nonnull Object key, @Nonnull String uniquePropertyName, @Nonnull SharedSessionContractImplementor session) {
		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.resolvingUniqueKeyToIdentifier( key, getEntityName() );
		}
		return getUniqueKeyLoader( uniquePropertyName, session ).resolveId( key, session );
	}


	/**
	 * Generate the SQL that selects the version number by id.
	 * <p>
	 * The select is used to verify, just before the transaction commits, that
	 * the version of an entity locked in {@link LockMode#OPTIMISTIC} mode is
	 * still current, and so it is rendered as a "current read": on a database
	 * where a plain read does not wait for the outcome of a concurrent
	 * uncommitted write to the row, and might return a stale snapshot, the
	 * dialect renders whatever makes the read wait for any concurrent writer
	 * and see the current version.
	 *
	 * @see SimpleSelect#setCurrentRead(boolean)
	 * @see org.hibernate.dialect.lock.spi.ReadGuarantees#isCurrentRead()
	 */
	@Nonnull
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
		return select.setCurrentRead( true ).addRestriction( rootTableKeyColumnNames ).toStatementString();
	}

	@Nonnull
	protected GeneratedValuesProcessor createGeneratedValuesProcessor(
			@Nonnull EventType timing,
			@Nonnull List<AttributeMapping> generatedAttributes) {
		return new GeneratedValuesProcessor( this, generatedAttributes, timing, getFactory() );
	}

	@Nonnull
	@Override
	public Object forceVersionIncrement(@Nonnull Object id, @Nullable Object currentVersion, @Nonnull SharedSessionContractImplementor session) {
		assert getMappedTableDetails().getTableName().equals( getVersionedTableName() );
		final Object nextVersion = calculateNextVersion( id, currentVersion, session );
		updateCoordinator.forceVersionIncrement( id, currentVersion, nextVersion, session );
		return nextVersion;
	}

	@Nonnull
	@Override
	public Object forceVersionIncrement(
			@Nonnull Object id,
			@Nullable Object currentVersion,
			boolean batching,
			@Nonnull SharedSessionContractImplementor session)
					throws HibernateException {
		assert getMappedTableDetails().getTableName().equals( getVersionedTableName() );
		final Object nextVersion = calculateNextVersion( id, currentVersion, session );
		updateCoordinator.forceVersionIncrement( id, currentVersion, nextVersion, batching, session );
		return nextVersion;
	}

	@Nonnull
	private Object calculateNextVersion(@Nonnull Object id, @Nullable Object currentVersion, @Nonnull SharedSessionContractImplementor session) {
		assert isVersioned();
		final Object nextVersion =
				generatorForForceIncrement()
						// TODO: pass in owner entity
						.generate( session, null, currentVersion, FORCE_INCREMENT );
		if ( CORE_LOGGER.isTraceEnabled() ) {
			final var versionType = castNonNull( getVersionType() );
			CORE_LOGGER.forcingVersionIncrement(
					"[" + infoString( this, id, factory ) + "; "
					+ versionType.toLoggableString( currentVersion, factory ) + " -> "
					+ versionType.toLoggableString( nextVersion, factory ) + "]"
			);
		}
		return nextVersion;
	}

	@Nonnull
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
			final var generator = castNonNull( getVersionGenerator() );
			if ( !generator.generatesOnForceIncrement() ) {
				throw new HibernateException( "Force-increment lock not supported for '@Version' generator" );
			}
			return generator;
		}
	}

	/**
	 * Retrieve the version number
	 */
	@Nullable
	@Override
	public Object getCurrentVersion(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) throws HibernateException {

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
					"Could not retrieve version: " + infoString( this, id, getFactory() ),
					versionSelectString
			);
		}
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping(int index) {
		return getIdentifierMapping().getJdbcMapping( index );
	}

	@Nonnull
	protected LockingStrategy generateLocker(@Nonnull LockMode lockMode, @Nonnull PessimisticLockScope lockScope) {
		final var factory = Objects.requireNonNull(
				getDialect().getEntityLockingStrategyFactory(),
				"Dialect returned a null entity locking strategy factory"
		);
		return Objects.requireNonNull(
				factory.createStrategy(
						new EntityLockingStrategyRequestImpl( this, lockMode, lockScope )
				),
				"Entity locking strategy factory returned null"
		);
	}

	// Used by Hibernate Reactive
	@Nonnull
	protected LockingStrategy getLocker(@Nonnull LockMode lockMode, @Nonnull PessimisticLockScope lockScope) {
		return lockScope != PessimisticLockScope.NORMAL
				// be sure to not use the cached form if any form of extended locking is requested
				? generateLocker( lockMode, lockScope )
				: lockers.computeIfAbsent( lockMode, (l) -> generateLocker( lockMode, lockScope ) );
	}

	@Override
	public void lock(
			@Nonnull Object id,
			@Nullable Object version,
			@Nonnull Object object,
			@Nonnull LockMode lockMode,
			@Nonnull SharedSessionContractImplementor session)
					throws HibernateException {
		getLocker( lockMode, PessimisticLockScope.NORMAL )
				.lock( id, version, object, Timeouts.WAIT_FOREVER, session );
	}

	@Override
	public void lock(
			@Nonnull Object id,
			@Nullable Object version,
			@Nonnull Object object,
			@Nonnull LockOptions lockOptions,
			@Nonnull SharedSessionContractImplementor session)
					throws HibernateException {
		getLocker( lockOptions.getLockMode(), lockOptions.getScope() )
				.lock( id, version, object, lockOptions.getTimeout(), session );
	}

	@Nonnull
	@Override
	public String getRootTableName() {
		return getSubclassTableName( 0 );
	}

	@Nonnull
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
	@Nonnull
	@Override @Deprecated(forRemoval = true)
	public String[] toColumns(@Nonnull String propertyName) throws QueryException {
		return getPropertyColumnNames( propertyName );
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
	@Nonnull
	@Override
	public String[] getPropertyColumnNames(@Nonnull String propertyName) {
		final var propertyPath = resolvePropertyPath( propertyName );
		if ( propertyPath == null ) {
			throw new MappingException( "Unknown property: " + propertyName );
		}
		return propertyPath.columnNames();
	}

	@Nullable
	private DiscriminatorType<?> discriminatorDomainType;

	@Nullable
	@Override
	public DiscriminatorType<?> getDiscriminatorDomainType() {
		if ( discriminatorDomainType == null ) {
			discriminatorDomainType = buildDiscriminatorType();
		}
		return discriminatorDomainType;
	}

	@Nullable
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

	@Nonnull
	private Class<?> discriminatedType() {
		return representationStrategy.getMode() == POJO
			&& getEntityName().equals( getJavaType().getJavaTypeClass().getName() )
				? Class.class
				: String.class;
	}

	@Nonnull
	public static String generateTableAlias(@Nonnull String rootAlias, int tableNumber) {
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

	private int getSubclassPropertyIndex(@Nonnull String propertyName) {
		return indexOf( subclassPropertyNameClosure, propertyName );
	}

	@Nonnull
	public String[] getPropertyColumnNames(int i) {
		return propertyColumnNames[i];
	}

	public boolean hasFormulaProperties() {
		return hasFormulaProperties;
	}

	@Nonnull
	public FetchStyle getFetchStyle(int i) {
		return subclassPropertyFetchStyleClosure[i];
	}

	@Nonnull
	public Type getSubclassPropertyType(int i) {
		return subclassPropertyTypeClosure[i];
	}

	@Override
	public int countSubclassProperties() {
		return subclassPropertyTypeClosure.length;
	}

	@Nonnull
	@Override
	public String[] getSubclassPropertyColumnNames(int i) {
		return subclassPropertyColumnNameClosure[i];
	}

	@Nonnull
	protected Type[] getSubclassPropertyTypeClosure() {
		return subclassPropertyTypeClosure;
	}

	private static boolean isPrefix(@Nonnull final AttributeMapping attributeMapping, @Nonnull final String currentAttributeName) {
		final String attributeName = attributeMapping.getAttributeName();
		final int nameLength = attributeName.length();
		return currentAttributeName.startsWith( attributeName )
			&& ( currentAttributeName.length() == nameLength || currentAttributeName.charAt(nameLength) == '.' );
	}

	private static int skipDuplicateAndNestedAttributeNames(
			@Nonnull final AttributeMapping attributeMapping,
			@Nonnull final String[] attributeNames,
			int index) {
		// The attributeNames array can contain the same attribute name multiple times,
		// which we want to skip. Similarly, it can contain nested paths, which we also want to skip,
		// because we work with top-level attribute indexes and have no way to flush nested attributes,
		// so we seek to the attributeNames element that isn't a prefix of the current attributeMapping
		while ( index < attributeNames.length && isPrefix( attributeMapping, attributeNames[index] ) ) {
			index++;
		}
		return index;
	}

	@Nonnull
	@Override
	public int[] resolveAttributeIndexes(@Nullable String[] attributeNames) {
		if ( attributeNames == null || attributeNames.length == 0 ) {
			return EMPTY_INT_ARRAY;
		}

		// Sort attribute names so that we can traverse mappings efficiently
		Arrays.sort( attributeNames );

		final List<Integer> fields = new ArrayList<>( attributeNames.length );
		int index = 0;
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final var attributeMapping = attributeMappings.get( i );
			if ( isPrefix( attributeMapping, attributeNames[index] ) ) {
				fields.add( attributeMapping.getStateArrayPosition() );
				index = skipDuplicateAndNestedAttributeNames( attributeMapping, attributeNames, index + 1 );
				if ( index >= attributeNames.length ) {
					break;
				}
			}
		}

		return toIntArray( fields );
	}

	@Nonnull
	@Override
	public int[] resolveDirtyAttributeIndexes(
			@Nonnull final Object[] currentState,
			@Nonnull final Object[] previousState,
			@Nullable final String[] attributeNames,
			@Nonnull final SessionImplementor session) {
		final var mutablePropertiesIndexes = getMutablePropertiesIndexes();
		final int estimatedSize =
				attributeNames == null
						? 0
						: attributeNames.length + mutablePropertiesIndexes.cardinality();
		if ( estimatedSize == 0 ) {
			return EMPTY_INT_ARRAY;
		}

		final List<Integer> fields = new ArrayList<>( estimatedSize );

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

		if ( attributeNames != null && attributeNames.length != 0 ) {
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
					if ( isPrefix( attributeMapping, attributeNames[index] ) ) {
						final int position = attributeMapping.getStateArrayPosition();
						if ( propertyUpdateability[position] && !fields.contains( position ) ) {
							fields.add( position );
						}
						index = skipDuplicateAndNestedAttributeNames( attributeMapping, attributeNames, index + 1 );
						if ( index >= attributeNames.length ) {
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
			@Nonnull Object[] currentState,
			@Nullable Object[] previousState,
			@Nonnull Type[] propertyTypes,
			@Nonnull boolean[] propertyCheckability,
			int i,
			@Nonnull SessionImplementor session) {
		return currentState[i] != UNFETCHED_PROPERTY
				// Consider mutable properties as dirty if we don't have a previous state
				&& ( previousState == null
						|| previousState[i] == UNFETCHED_PROPERTY
						|| propertyCheckability[i]
								&& propertyTypes[i].isDirty(
										previousState[i],
										currentState[i],
										propertyColumnUpdateable[i],
										session
								)
				);
	}

	@Nullable
	@Override
	public Object loadByUniqueKey(
			@Nonnull String propertyName,
			@Nonnull Object uniqueKey,
			@Nonnull SharedSessionContractImplementor session) throws HibernateException {
		return loadByUniqueKey( propertyName, uniqueKey, null, session );
	}

	@Nullable
	public Object loadByUniqueKey(
			@Nonnull String propertyName,
			@Nonnull Object uniqueKey,
			@Nullable Boolean readOnly,
			@Nonnull SharedSessionContractImplementor session) throws HibernateException {
		return getUniqueKeyLoader( propertyName, session ).load( uniqueKey, new LockOptions(), readOnly, session );
	}

	@Nullable
	private Map<SingularAttributeMapping, SingleUniqueKeyEntityLoader<?>> uniqueKeyLoadersNew;

	@Nonnull
	protected SingleUniqueKeyEntityLoader<?> getUniqueKeyLoader(@Nonnull String attributeName, @Nonnull SharedSessionContractImplementor session) {
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

	@Nullable
	private PropertyPath resolvePropertyPath(@Nonnull String propertyName) {
		if ( isEmpty( propertyName ) ) {
			return null;
		}
		else if ( ENTITY_CLASS.equals( propertyName ) && isPolymorphic() ) {
			return new PropertyPath( castNonNull( getDiscriminatorType() ),
					new String[] { getDiscriminatorColumnName() } );
		}

		final var identifierPropertyPath =
				resolveNonAggregatedIdentifierPropertyPath( propertyName );
		if ( identifierPropertyPath != null ) {
			return identifierPropertyPath;
		}
		else if ( hasIdentifierProperty() && propertyName.equals( getIdentifierPropertyName() ) ) {
			return new PropertyPath( getIdentifierType(), getIdentifierColumnNames() );
		}

		final int propertyIndex = getSubclassPropertyIndex( propertyName );
		if ( propertyIndex >= 0 ) {
			final var type = getSubclassPropertyType( propertyIndex );
			final var columnNames =
					resolveAssociationColumnNames( propertyName, type,
							getSubclassPropertyColumnNames( propertyIndex ) );
			return new PropertyPath( type, columnNames );
		}
		else if ( isIdentifierReference( propertyName ) ) {
			return new PropertyPath( getIdentifierType(), getIdentifierColumnNames() );
		}

		final int dotIndex = propertyName.indexOf( '.' );
		if ( dotIndex > 0 ) {
			final var basePath = resolvePropertyPath( propertyName.substring( 0, dotIndex ) );
			if ( basePath != null ) {
				return resolveSubPropertyPath( basePath.type(), basePath.columnNames(),
						propertyName.substring( dotIndex + 1 ) );
			}
		}

		return resolveEmbeddedPropertyPath( propertyName );
	}

	@Nullable
	private PropertyPath resolveNonAggregatedIdentifierPropertyPath(@Nonnull String propertyName) {
		return getIdentifierMapping() instanceof NonAggregatedIdentifierMapping
				? resolveSubPropertyPath( getIdentifierType(), getIdentifierColumnNames(), propertyName )
				: null;
	}

	@Nullable
	private PropertyPath resolveEmbeddedPropertyPath(@Nonnull String propertyName) {
		if ( isIdentifierEmbedded() ) {
			final var identifierPath =
					resolveSubPropertyPath( getIdentifierType(), getIdentifierColumnNames(), propertyName );
			if ( identifierPath != null ) {
				return identifierPath;
			}
		}

		for ( int i = 0; i < subclassPropertyTypeClosure.length; i++ ) {
			final var propertyType = subclassPropertyTypeClosure[i];
			if ( propertyType instanceof ComponentType componentType && componentType.isEmbedded() ) {
				final var embeddedPath =
						resolveSubPropertyPath( componentType, subclassPropertyColumnNameClosure[i], propertyName );
				if ( embeddedPath != null ) {
					return embeddedPath;
				}
			}
		}
		return null;
	}

	@Nullable
	private PropertyPath resolveSubPropertyPath(@Nonnull Type type, @Nonnull String[] columnNames, @Nonnull String propertyName) {
		if ( type instanceof CompositeType compositeType ) {
			return resolveCompositePropertyPath( compositeType, columnNames, propertyName );
		}
		else if ( type instanceof EntityType entityType ) {
			return resolveEntityIdentifierPropertyPath( entityType, columnNames, propertyName );
		}
		else {
			return null;
		}
	}

	@Nullable
	private PropertyPath resolveCompositePropertyPath(@Nonnull CompositeType compositeType, @Nonnull String[] columnNames, @Nonnull String propertyName) {
		final int dotIndex = propertyName.indexOf( '.' );
		final String componentPropertyName =
				dotIndex < 0
						? propertyName
						: propertyName.substring( 0, dotIndex );
		final int componentPropertyIndex =
				getCompositePropertyIndex( compositeType, componentPropertyName );
		if ( componentPropertyIndex < 0 ) {
			return null;
		}

		final var propertyType = compositeType.getSubtypes()[componentPropertyIndex];
		final var propertyColumnNames =
				getCompositePropertySelectableValues( compositeType, columnNames, componentPropertyIndex );
		return dotIndex < 0
				? new PropertyPath( propertyType, propertyColumnNames )
				: resolveSubPropertyPath( propertyType, propertyColumnNames,
						propertyName.substring( dotIndex + 1 ) );
	}

	private static int getCompositePropertyIndex(@Nonnull CompositeType compositeType, @Nonnull String propertyName) {
		final String[] propertyNames = compositeType.getPropertyNames();
		for ( int i = 0; i < propertyNames.length; i++ ) {
			if ( propertyNames[i].equals( propertyName ) ) {
				return i;
			}
		}
		return -1;
	}

	@Nonnull
	private String[] getCompositePropertySelectableValues(
			@Nonnull CompositeType compositeType,
			@Nonnull String[] selectableValues,
			int propertyIndex) {
		final var mappingContext = factory.getRuntimeMetamodels();
		final var subtypes = compositeType.getSubtypes();
		int begin = 0;
		for ( int i = 0; i < propertyIndex; i++ ) {
			begin += subtypes[i].getColumnSpan( mappingContext );
		}
		return slice( selectableValues, begin, subtypes[propertyIndex].getColumnSpan( mappingContext ) );
	}

	@Nullable
	private PropertyPath resolveEntityIdentifierPropertyPath(
			@Nonnull EntityType entityType,
			@Nonnull String[] columnNames,
			@Nonnull String propertyName) {
		final int dotIndex = propertyName.indexOf( '.' );
		final String identifierPropertyName =
				dotIndex < 0
						? propertyName
						: propertyName.substring( 0, dotIndex );
		final var runtimeMetamodels = factory.getRuntimeMetamodels();
		final var identifierType = entityType.getIdentifierOrUniqueKeyType( runtimeMetamodels );
		final String identifierOrUniqueKeyPropertyName =
				entityType.getIdentifierOrUniqueKeyPropertyName( runtimeMetamodels );
		final boolean matchesIdentifier =
				entityType.isReferenceToPrimaryKey()
						&& ENTITY_ID.equals( identifierPropertyName )
						&& !hasNonIdentifierPropertyNamedId( entityType );
		final boolean matchesIdentifierOrUniqueKey =
				!entityType.isNullable()
						&& identifierOrUniqueKeyPropertyName != null
						&& identifierOrUniqueKeyPropertyName.equals( identifierPropertyName );

		if ( matchesIdentifier || matchesIdentifierOrUniqueKey ) {
			return dotIndex < 0
					? new PropertyPath( identifierType, columnNames )
					: resolveSubPropertyPath( identifierType, columnNames,
							propertyName.substring( dotIndex + 1 ) );
		}
		return null;
	}

	private boolean hasNonIdentifierPropertyNamedId(@Nonnull EntityType entityType) {
		return entityType.getAssociatedEntityPersister( factory )
					instanceof BaseEntityPersister baseEntityPersister
			&& baseEntityPersister.hasNonIdentifierPropertyNamedId();
	}

	@Nonnull
	private String[] resolveAssociationColumnNames(@Nonnull String path, @Nonnull Type type, @Nonnull String[] columnNames) {
		if ( type instanceof AssociationType associationType ) {
			if ( associationType.useLHSPrimaryKey() ) {
				return getIdentifierColumnNames();
			}
			else {
				final String foreignKeyProperty = associationType.getLHSPropertyName();
				if ( foreignKeyProperty != null && !path.equals( foreignKeyProperty ) ) {
					final var foreignKeyPath = resolvePropertyPath( foreignKeyProperty );
					if ( foreignKeyPath != null ) {
						return foreignKeyPath.columnNames();
					}
				}
			}
		}
		return columnNames;
	}

	private record PropertyPath(Type type, String[] columnNames) {}

	@Nullable
	@Override
	public String getIdentitySelectString() {
		return identitySelectString;
	}

	@Nonnull
	@Override
	public String getSelectByUniqueKeyString(@Nonnull String propertyName) {
		return getSelectByUniqueKeyString( new String[] { propertyName } );
	}

	@Nonnull
	@Override
	public String getSelectByUniqueKeyString(@Nonnull String[] propertyNames) {
		final var select =
				new SimpleSelect( getFactory() )
						.setTableName( getTableName(0) )
						.addColumns( getKeyColumns(0) );
		for ( String propertyName : propertyNames ) {
			select.addRestriction( getPropertyColumnNames( propertyName ) );
		}
		return select.toStatementString();
	}

	@Nonnull
	@Override
	public String getSelectByUniqueKeyString(@Nonnull String[] propertyNames, @Nonnull String[] columnNames) {
		final var select =
				new SimpleSelect( getFactory() )
						.setTableName( getTableName( 0 ) )
						.addColumns( columnNames );
		for ( final String propertyName : propertyNames ) {
			select.addRestriction( getPropertyColumnNames( propertyName ) );
		}
		return select.toStatementString();
	}

	@Nullable
	@Override
	public GeneratedValuesMutationDelegate getInsertDelegate() {
		return insertDelegate;
	}

	@Nullable
	@Override
	public GeneratedValuesMutationDelegate getUpdateDelegate() {
		return updateDelegate;
	}

	@Nonnull
	@Override
	public EntityTableMapping[] getTableMappings() {
		return tableMappings;
	}

	@Nonnull
	protected EntityTableMapping getTableMapping(int i) {
		return tableMappings[i];
	}

	@Nonnull
	@Override
	public EntityTableDescriptor[] getTableDescriptors() {
		return tableDescriptors;
	}

	@Nonnull
	@Override
	public EntityTableDescriptor getIdentifierTableDescriptor() {
		return getTableDescriptors()[0];
	}

	@Override
	public void forEachMutableTableDescriptor(@Nonnull Consumer<EntityTableDescriptor> consumer) {
		for ( var tableMapping : tableDescriptors ) {
			// inverse tables are not mutable from this mapping
			if ( !tableMapping.isInverse() ) {
				consumer.accept( tableMapping );
			}
		}
	}

	@Override
	public void forEachMutableTableDescriptorReverse(@Nonnull Consumer<EntityTableDescriptor> consumer) {
		for ( int i = tableDescriptors.length - 1; i >= 0; i-- ) {
			final var tableMapping = tableDescriptors[i];
			// inverse tables are not mutable from this mapping
			if ( !tableMapping.isInverse() ) {
				consumer.accept( tableMapping );
			}
		}
	}

	@Override
	public void forEachTableDetails(@Nonnull Consumer<TableDetails> consumer) {
		CollectionHelper.forEach( getTableMappings(), consumer );
	}

	/**
	 * Unfortunately we cannot directly use `SelectableMapping#getContainingTableExpression()`
	 * as that blows up for attributes declared on super-type for union-subclass mappings
	 */
	@Nonnull
	@Override
	public String physicalTableNameForMutation(@Nonnull SelectableMapping selectableMapping) {
		assert !selectableMapping.isFormula();
		return selectableMapping.getContainingTableExpression();
	}

	@Nonnull
	@Override
	public EntityPersister getTargetPart() {
		return this;
	}

	@Override
	public void forEachMutableTable(@Nonnull Consumer<EntityTableMapping> consumer) {
		for ( var tableMapping : tableMappings ) {
			// inverse tables are not mutable from this mapping
			if ( !tableMapping.isInverse() ) {
				consumer.accept( tableMapping );
			}
		}
	}

	@Override
	public void forEachMutableTableReverse(@Nonnull Consumer<EntityTableMapping> consumer) {
		for ( int i = tableMappings.length - 1; i >= 0; i-- ) {
			final var tableMapping = tableMappings[i];
			// inverse tables are not mutable from this mapping
			if ( !tableMapping.isInverse() ) {
				consumer.accept( tableMapping );
			}
		}
	}

	@Nonnull
	@Override
	public String getIdentifierTableName() {
		return getTableName( 0 );
	}

	@Nonnull
	@Override
	public EntityTableMapping getIdentifierTableMapping() {
		return tableMappings[0];
	}

	@Nonnull
	@Override
	public ModelPart getIdentifierDescriptor() {
		return identifierMapping;
	}

	protected void logStaticSQL() {
		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.staticSqlForEntity( getEntityName() );
			for ( var entry : lazyLoadPlanByFetchGroup.entrySet() ) {
				MODEL_MUTATION_LOGGER.lazySelect( String.valueOf( entry.getKey() ),
						entry.getValue().getJdbcSelect().getSqlString() );
			}
			if ( sqlVersionSelectString != null ) {
				MODEL_MUTATION_LOGGER.versionSelect( sqlVersionSelectString );
			}

			{
				final var staticInsertOperations = insertDecomposer.getStaticInsertOperations();
				int i = 0;
				for ( var insertEntry : staticInsertOperations.entrySet() ) {
					if ( insertEntry.getValue() instanceof JdbcMutationOperation jdbcOperation ) {
						MODEL_MUTATION_LOGGER.insertOperationSql( i++, jdbcOperation.getSqlString() );
					}
				}
			}

			{
				final var staticUpdateOperations = updateDecomposer.getStaticUpdateOperations();
				int i = 0;
				for ( var updateEntry : staticUpdateOperations.entrySet() ) {
					if ( updateEntry.getValue() instanceof JdbcMutationOperation jdbcOperation ) {
						MODEL_MUTATION_LOGGER.updateOperationSql( i++, jdbcOperation.getSqlString() );
					}
				}
			}

			{
				final var staticDeleteOperations = deleteDecomposer.getStaticDeleteOperations();
				int i = 0;
				for ( var updateEntry : staticDeleteOperations.entrySet() ) {
					if ( updateEntry.getValue() instanceof JdbcMutationOperation jdbcOperation ) {
						MODEL_MUTATION_LOGGER.updateOperationSql( i++, jdbcOperation.getSqlString() );
					}
				}
			}
		}
	}

	@Nonnull
	public abstract Map<DiscriminatorValue, String> getSubclassByDiscriminatorValue();

	@Nonnull
	public abstract String[] getConstraintOrderedTableNameClosure();

	public abstract boolean needsDiscriminator();

	protected boolean isDiscriminatorFormula() {
		return false;
	}

	@Nonnull
	@Override
	public TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			@Nonnull NavigablePath navigablePath,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable Supplier<Consumer<Predicate>> additionalPredicateCollector,
			@Nonnull SqlAstCreationState creationState) {
		final var loadQueryInfluencers = creationState.getLoadQueryInfluencers();

		final var sqlAliasBase = SqlAliasBase.from(
				explicitSqlAliasBase,
				explicitSourceAlias,
				this,
				creationState.getSqlAliasBaseGenerator()
		);
		final boolean useAuxiliaryTable =
				auxiliaryMapping != null
						&& auxiliaryMapping.useAuxiliaryTable( loadQueryInfluencers );
		final String originalTableName =
				needsDiscriminator()
						? getRootTableName()
						: getTableName();
		final String rootTableName =
				useAuxiliaryTable
						? castNonNull( auxiliaryMapping ).resolveTableName( originalTableName )
						: originalTableName;
		final String rootAlias = sqlAliasBase.generateNewAlias();
		final var rootTableReference =
				useAuxiliaryTable
						? new AuxiliaryTableReference( rootTableName, originalTableName, rootAlias )
						: new NamedTableReference( rootTableName, rootAlias );
		rootTableReference.applyAuxiliaryTable( auxiliaryMapping, loadQueryInfluencers );

		final var tableGroup = new StandardTableGroup(
				canUseInnerJoins,
				navigablePath,
				this,
				explicitSourceAlias,
				rootTableReference,
				true,
				sqlAliasBase,
				getRootEntityDescriptor()::containsTableReference,
				(tableExpression, group) -> {
					final var subclassTableNames = getSubclassTableNames();
					for ( int i = 0; i < subclassTableNames.length; i++ ) {
						if ( tableExpression.equals( subclassTableNames[i] ) ) {
							final String auxiliaryTableName = useAuxiliaryTable
									? castNonNull( auxiliaryMapping ).resolveTableName( tableExpression )
									: null;
							final var joinedTableReference = auxiliaryTableName != null
									? new AuxiliaryTableReference(
											auxiliaryTableName,
											tableExpression,
											sqlAliasBase.generateNewAlias(),
											isNullableSubclassTable( i )
									)
									: new NamedTableReference(
											tableExpression,
											sqlAliasBase.generateNewAlias(),
											isNullableSubclassTable( i )
									);
							joinedTableReference.applyAuxiliaryTable( auxiliaryMapping, loadQueryInfluencers );
							final var tableReferenceJoin = new TableReferenceJoin(
									shouldInnerJoinSubclassTable( i, emptySet() ),
									joinedTableReference,
									additionalPredicateCollector == null
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
							if ( auxiliaryTableName != null ) {
								castNonNull( auxiliaryMapping ).applyPredicate(
										tableReferenceJoin,
										rootTableReference,
										tableExpression,
										AbstractEntityPersister.this,
										creationState.getSqlAliasBaseGenerator(),
										loadQueryInfluencers
								);
							}
							return tableReferenceJoin;
						}
					}
					return null;
				},
				getFactory()
		);

		if ( additionalPredicateCollector != null ) {
			if ( needsDiscriminator() ) {
				final String alias = tableGroup.getPrimaryTableReference().getIdentificationVariable();
				additionalPredicateCollector.get()
						.accept( createDiscriminatorPredicate( alias, tableGroup, creationState ) );
			}

			if ( auxiliaryMapping != null ) {
				castNonNull( auxiliaryMapping ).applyPredicate(
						additionalPredicateCollector,
						creationState,
						tableGroup,
						rootTableReference,
						this
				);
			}
		}

		return tableGroup;
	}

	@Override
	public void applyDiscriminator(
			@Nullable Consumer<Predicate> predicateConsumer,
			@Nullable String alias,
			@Nonnull TableGroup tableGroup,
			@Nonnull SqlAstCreationState creationState) {
		if ( needsDiscriminator() ) {
			assert !creationState.supportsEntityNameUsage() : "Entity name usage should have been used instead";
			final var subMappingTypes = getSubMappingTypes();
			final Map<String, EntityNameUse> entityNameUseMap =
					new HashMap<>( 1 + subMappingTypes.size() + ( isInherited() ? 1 : 0 ) );
			entityNameUseMap.put( getEntityName(), EntityNameUse.TREAT );
			if ( !subMappingTypes.isEmpty() ) {
				// We need to register TREAT uses for all subtypes when pruning
				for ( var subMappingType : subMappingTypes ) {
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

	@Nonnull
	private Predicate createDiscriminatorPredicate(
			@Nonnull String alias,
			@Nonnull TableGroup tableGroup,
			@Nonnull SqlAstCreationState creationState) {
		final String discriminatorExpression =
				isDiscriminatorFormula()
						? getDiscriminatorFormulaTemplate()
						: getDiscriminatorColumnName();

		final var discriminatorJdbcMapping =
				(BasicType<?>)
						getDiscriminatorMapping().getJdbcMapping();
		final var sqlExpression =
				creationState.getSqlExpressionResolver()
						.resolveSqlExpression(
								createColumnReferenceKey(
										tableGroup.getPrimaryTableReference(),
										discriminatorExpression,
										getDiscriminatorType()
								),
								sqlAstProcessingState -> new ColumnReference(
										alias,
										discriminatorExpression,
										isDiscriminatorFormula(),
										null,
										discriminatorJdbcMapping
								)
						);

		return createDisciminatorPredicate( discriminatorJdbcMapping, sqlExpression );
	}

	@Nonnull
	private Predicate createDisciminatorPredicate(@Nonnull BasicType<?> discriminatorType, @Nonnull Expression sqlExpression) {
		if ( hasSubclasses() ) {
			return createInListPredicate( discriminatorType, sqlExpression );
		}
		else {
			final DiscriminatorValue value = getDiscriminatorValue();
			if ( value == DiscriminatorValue.Special.NULL ) {
				return new NullnessPredicate( sqlExpression );
			}
			else if ( value == DiscriminatorValue.Special.NOT_NULL ) {
				return new NullnessPredicate( sqlExpression, true );
			}
			else if ( value instanceof DiscriminatorValue.Literal literal ) {
				return new ComparisonPredicate(
						sqlExpression,
						ComparisonOperator.EQUAL,
						new QueryLiteral<>( literal.value(), discriminatorType )
				);
			}
			else {
				throw new IllegalStateException( "Unexpected discriminator value: " + value );
			}
		}
	}

	@Nonnull
	private Predicate createInListPredicate(@Nonnull BasicType<?> discriminatorType, @Nonnull Expression sqlExpression) {
		boolean hasNull = false, hasNonnull = false;
		for ( DiscriminatorValue discriminatorValue : fullDiscriminatorValues ) {
			if ( discriminatorValue == DiscriminatorValue.Special.NULL ) {
				hasNull = true;
			}
			else if ( discriminatorValue == DiscriminatorValue.Special.NOT_NULL ) {
				hasNonnull = true;
			}
		}
		if ( hasNull && hasNonnull ) {
			// This means we need to select all rows,
			// and so we don't need a predicate at all
			// Just return an empty Junction
			return new Junction( Junction.Nature.DISJUNCTION );
		}
		else if ( hasNonnull ) {
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

	@Nonnull
	private InListPredicate discriminatorValuesPredicate(@Nonnull BasicType<?> discriminatorType, @Nonnull Expression sqlExpression) {
		final List<Expression> values = new ArrayList<>( fullDiscriminatorValues.length );
		for ( DiscriminatorValue discriminatorValue : fullDiscriminatorValues ) {
			if ( discriminatorValue instanceof DiscriminatorValue.Literal literal ) {
				values.add( new QueryLiteral<>( literal.value(), discriminatorType ) );
			}
		}
		return new InListPredicate( sqlExpression, values );
	}

	@Nullable
	protected String getPrunedDiscriminatorPredicate(
			@Nonnull Map<String, EntityNameUse> entityNameUses,
			@Nonnull MappingMetamodelImplementor mappingMetamodel,
			@Nonnull String alias) {
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
		final var discriminatorSQLValues = rootEntityDescriptor.fullDiscriminatorSQLValues;
		if ( fragment.getValues().size() == discriminatorSQLValues.length ) {
			// Nothing to prune if we filter for all subtypes
			return null;
		}
		else if ( containsNotNull ) {
			final String lhs = isDiscriminatorFormula()
					? replace( getDiscriminatorFormulaTemplate(), Template.TEMPLATE, alias )
					: qualifyConditionally( alias, getDiscriminatorColumnName() );
			final List<String> actualDiscriminatorSQLValues = new ArrayList<>( discriminatorSQLValues.length );
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
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nullable TableGroup tableGroup,
			boolean useQualifier,
			@Nonnull Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			@Nullable SqlAstCreationState creationState) {
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
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup, boolean useQualifier,
			@Nonnull Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			@Nullable Set<String> treatAsDeclarations,
			@Nullable SqlAstCreationState creationState) {
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
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nullable SqlAstCreationState creationState) {
		if ( sqlWhereStringTemplate != null ) {
			final String alias = getAliasInWhere( tableGroup, useQualifier );
			final String fragment = replace( sqlWhereStringTemplate, Template.TEMPLATE, alias );
			predicateConsumer.accept( new SqlFragmentPredicate( fragment ) );
		}
	}

	@Nullable
	private String getAliasInWhere(@Nullable TableGroup tableGroup, boolean useQualifier) {
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

	protected boolean shouldInnerJoinSubclassTable(int subclassTableNumber, @Nullable Set<String> treatAsDeclarations) {
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

	protected boolean isSubclassTableIndicatedByTreatAsDeclarations(int subclassTableNumber, @Nullable Set<String> treatAsDeclarations) {
		return false;
	}

	@Override
	public void buildTableDescriptorsEarly() {
		// Build tableDescriptors early so they're available to all persisters
		// before prepareLoaders() is called. This is necessary because subclass
		// persisters may reference their root persister's tableDescriptors.
		tableDescriptors = buildTableDescriptors();
	}

	@Override
	public void prepareLoaders() {
		tenantIdLoader = tenantIdMapping == null || tenantIdMapping.getAttributeMapping() == null
				? null : new TenantIdLoader( this );
		// Hibernate Reactive needs to override the loaders
		singleIdLoader = buildSingleIdEntityLoader();
		multiIdLoader = buildMultiIdLoader();

		lazyLoadPlanByFetchGroup = getLazyLoadPlanByFetchGroup();

		final var auditMapping = getAuditMapping();
		if ( auditMapping != null ) {
			auditMapping.getEntityLoader();
		}

		// tableDescriptors were built in buildTableDescriptorsEarly()
		// Now we can safely create decomposers which may access tableDescriptors
		insertDecomposer = buildInsertDecomposer( factory );
		updateDecomposer = buildUpdateDecomposer( factory );
		deleteDecomposer = buildDeleteDecomposer( factory );

		logStaticSQL();
	}

	@Nonnull
	protected InsertDecomposer buildInsertDecomposer(@Nonnull SessionFactoryImplementor factory) {
		return new InsertDecomposer(
				this,
				factory,
				stateManagement.getGraphIntegration().createEntityMutationPlanContributor( this )
		);
	}

	@Nonnull
	protected UpdateDecomposer buildUpdateDecomposer(@Nonnull SessionFactoryImplementor factory) {
		return new UpdateDecomposer(
				this,
				factory,
				stateManagement.getGraphIntegration().createEntityMutationPlanContributor( this )
		);
	}

	@Nonnull
	protected DeleteDecomposer buildDeleteDecomposer(@Nonnull SessionFactoryImplementor factory) {
		return new DeleteDecomposerStandard(
				this,
				factory,
				stateManagement.getGraphIntegration().createEntityMutationPlanContributor( this )
		);
	}

	@Override
	public final void postInstantiate(@Nonnull PersistentClass bootEntityDescriptor) throws MappingException {

		tableMappings = buildTableMappings( bootEntityDescriptor );
		filteredAssociationMapping = FilteredAssociationMapping.create( this );
		tenantIdMapping = TenantIdMappingImpl.create( this );

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

		final var legacyIntegration = stateManagement.getLegacyIntegration();
		insertCoordinator = legacyIntegration.createInsertCoordinator( this );
		updateCoordinator = legacyIntegration.createUpdateCoordinator( this );
		deleteCoordinator = legacyIntegration.createDeleteCoordinator( this );
		mergeCoordinator = legacyIntegration.createMergeCoordinator( this );

		//select SQL
		sqlVersionSelectString = generateSelectVersionString();
	}

	@Nonnull
	protected GeneratedValuesMutationDelegate createInsertDelegate() {
		if ( isIdentifierAssignedByInsert() ) {
			final var generator = (OnExecutionGenerator) getGenerator();
			return generator.getGeneratedIdentifierDelegate( this );
		}
		else {
			return getGeneratedValuesDelegate( this, INSERT );
		}
	}

	@Nonnull
	protected GeneratedValuesMutationDelegate createUpdateDelegate() {
		if ( supportsDatabaseDirtinessCheck() ) {
			final List<ModelPart> generatedProperties = new ArrayList<>( getUpdateGeneratedProperties() );
			generatedProperties.add( getVersionMapping() );
			return getDialect().getGeneratedValuesSupport().supports( UPDATE_RETURNING )
					? new InsertReturningDelegate( this, UPDATE, generatedProperties )
					: new UpdateVersionSelectDelegate( this, generatedProperties );
		}
		else {
			return getGeneratedValuesDelegate( this, UPDATE );
		}
	}

	private boolean supportsDatabaseDirtinessCheck() {
		if ( !isVersioned()
				|| isVersionPropertyGenerated()
				|| hasPartitionedSelectionMapping() && !getDialect().getGeneratedValuesSupport().supports( UPDATE_RETURNING )
				|| getIdentifierTableMapping().getUpdateDetails().getCustomSql() != null ) {
			return false;
		}

		boolean hasExcludedProperty = false;
		final boolean[] propertyUpdateability = getPropertyUpdateability();
		final boolean[] propertyVersionability = getPropertyVersionability();
		final var generators = getGenerators();
		for ( int i = 0; i < propertyUpdateability.length; i++ ) {
			final var attribute = getAttributeMapping( i );
			final var generator = generators[i];
			if ( generator != null && generator.generatedOnExecution() && generator.generatesOnUpdate() ) {
				// A value generated by the update cannot be compared with its previous value.
				if ( propertyVersionability[i] ) {
					return false;
				}
				hasExcludedProperty = true;
			}
			else if ( propertyVersionability[i] && attribute.isPluralAttributeMapping() ) {
				return false;
			}
			else if ( propertyUpdateability[i] && isVersion( attribute ) ) {
				if ( !propertyVersionability[i] ) {
					hasExcludedProperty = true;
				}
				else if ( supportsDatabaseDirtinessCheck( attribute ) ) {
					return false;
				}
			}
		}
		return hasExcludedProperty;
	}

	private boolean isVersion(@Nonnull AttributeMapping attribute) {
		return attribute != getVersionMapping().getVersionAttribute();
	}

	private boolean supportsDatabaseDirtinessCheck(@Nonnull AttributeMapping attribute) {
		if ( attribute instanceof SingularAttributeMapping singularAttribute ) {
			for ( int j = 0; j < singularAttribute.getJdbcTypeCount(); j++ ) {
				final var selectable = singularAttribute.getSelectable( j );
				if ( !selectable.isFormula() && selectable.isUpdateable() ) {
					if ( !isIdentifierTableSelectable( selectable )
						|| !selectable.getJdbcMapping().getJdbcType().isComparable()
						|| !"?".equals( selectable.getWriteExpression() ) ) {
						return true;
					}
				}
			}
			return false;
		}
		else {
			return true;
		}
	}

	private boolean isIdentifierTableSelectable(@Nonnull SelectableMapping selectable) {
		final String tableName = physicalTableNameForMutation( selectable );
		for ( var tableMapping : getTableMappings() ) {
			if ( tableName.equals( tableMapping.getTableName() ) ) {
				return tableMapping.isIdentifierTable();
			}
		}
		return false;
	}

	@Nonnull
	protected EntityTableDescriptor[] buildTableDescriptors() {
		final var tableBuilderMap = new LinkedHashMap<String, TableDescriptorBuilder>();
		visitMutabilityOrderedTables( (name, relativePosition, tableKeyColumnVisitationSupplier) -> {
			final var tableMappingBuilder = getTableDescriptorBuilder(
					name,
					relativePosition,
					tableKeyColumnVisitationSupplier,
					tableBuilderMap
			);
			tableBuilderMap.put( name, tableMappingBuilder );
		} );

		applyAttributes( tableBuilderMap );

		// Check if ANY table in this entity is self-referential
		// If so, ALL tables should be grouped by ordinalBase to keep operations
		// for the same entity instance together (e.g., primary + secondary tables)
		final boolean entityHasSelfReferentialTable = tableBuilderMap.values().stream()
				.anyMatch( builder -> builder.isSelfReferential );

		final var tableDescriptors = new EntityTableDescriptor[tableBuilderMap.size()];
		int i = 0;
		for ( var entry : tableBuilderMap.entrySet() ) {
			tableDescriptors[i++] = entry.getValue().build( entityHasSelfReferentialTable );
		}
		return tableDescriptors;
	}

	private void applyAttributes(@Nonnull LinkedHashMap<String, TableDescriptorBuilder> tableBuilderMap) {
		forEachAttributeMapping( (attributeIndex, attribute)
				-> applyAttribute( tableBuilderMap, attribute ) );
	}

	protected void applyAttribute(@Nonnull LinkedHashMap<String, TableDescriptorBuilder> tableBuilderMap, @Nonnull AttributeMapping attribute) {
		if ( applyAttribute( attribute ) ) {
			final var tableName = attribute.getContainingTableExpression();
			final var builder = tableBuilderMap.get( tableName );
			if ( builder != null && !builder.isInverse ) {
				builder.addAttribute( attribute );
				attribute.forEachSelectable( (selectableIndex, selectable)
						-> builder.addColumn( attribute, ColumnDescriptor.from( selectable ) ) );
			}
		}
	}

	private boolean applyAttribute(@Nonnull AttributeMapping attribute) {
		return !attribute.isPluralAttributeMapping()
			// - Skip identifier attributes - they're already represented in keyDescriptor
			//	For composite identifiers (@IdClass), the individual attributes (e.g., productId, operator)
			//	should not be duplicated in the attributes list
			&& !isIdentifierAttribute( attribute )
			// - Skip read-only attributes (insertable=false, updatable=false)
			//	TableDescriptor targets mutation operations, so read-only attributes are not applicable
			&& !isReadOnlyAttribute( attribute );
	}

	protected boolean isIdentifierAttribute(@Nonnull AttributeMapping attribute) {
		return attribute.isEntityIdentifierMapping() // @Id or @EmbeddedId
			|| IDENTIFIER_MAPPER_PROPERTY.equals( attribute.getAttributeName() ); // @IdClass
	}

	protected boolean isReadOnlyAttribute(@Nonnull AttributeMapping attribute) {
		for ( int i = 0; i < attribute.getJdbcTypeCount(); i++ ) {
			final var selectable = attribute.getSelectable( i );
			if ( selectable.isInsertable() || selectable.isUpdateable() ) {
				return false;
			}
		}

		return true;
	}

	@Nonnull
	private TableDescriptorBuilder getTableDescriptorBuilder(
			@Nonnull String tableName,
			int relativePosition,
			@Nonnull Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			@Nonnull LinkedHashMap<String, TableDescriptorBuilder> tableBuilderMap) {
		final var existing = tableBuilderMap.get( tableName );
		if ( existing != null ) {
			return existing;
		}
		else {
			final var tableMappingBuilder = createTableDescriptorBuilder(
					tableName,
					relativePosition,
					tableKeyColumnVisitationSupplier
			);
			tableBuilderMap.put( tableName, tableMappingBuilder );
			return tableMappingBuilder;
		}
	}

	@Nonnull
	protected static TableMutationDetails createTableMutationDetails(@Nonnull PersistentClass persistentClass) {
		return new TableMutationDetails(
				createCustomSqlMutationDetails(
						persistentClass.getCustomSQLInsert(),
						persistentClass.isCustomInsertCallable(),
						persistentClass.getInsertExpectation()
				),
				createCustomSqlMutationDetails(
						persistentClass.getCustomSQLUpdate(),
						persistentClass.isCustomUpdateCallable(),
						persistentClass.getUpdateExpectation()
				),
				createCustomSqlMutationDetails(
						persistentClass.getCustomSQLDelete(),
						persistentClass.isCustomDeleteCallable(),
						persistentClass.getDeleteExpectation()
				)
		);
	}

	@Nonnull
	protected static TableMutationDetails createTableMutationDetails(@Nonnull Join join) {
		return new TableMutationDetails(
				createCustomSqlMutationDetails(
						join.getCustomSQLInsert(),
						join.isCustomInsertCallable(),
						join.getInsertExpectation()
				),
				createCustomSqlMutationDetails(
						join.getCustomSQLUpdate(),
						join.isCustomUpdateCallable(),
						join.getUpdateExpectation()
				),
				createCustomSqlMutationDetails(
						join.getCustomSQLDelete(),
						join.isCustomDeleteCallable(),
						join.getDeleteExpectation()
				)
		);
	}

	@Nonnull
	private static CustomSqlMutationDetails createCustomSqlMutationDetails(
			@Nonnull String customSql,
			boolean callable,
			@Nonnull Supplier<? extends Expectation> expectation) {
		return new CustomSqlMutationDetails( createExpectation( expectation, callable ), customSql, callable );
	}

	@Nonnull
	protected abstract TableMutationDetails createTableMutationDetails(
			@Nonnull PersistentClass bootEntityDescriptor,
			int relativePosition);

	@Nonnull
	private TableMutationDetails resolveTableMutationDetails(
			@Nonnull PersistentClass bootEntityDescriptor,
			int relativePosition) {
		final var tableMutationDetails = createTableMutationDetails( bootEntityDescriptor, relativePosition );
		if ( tableMutationDetails == null ) {
			throw new AssertionFailure(
					"Table mutation details were not initialized for table position " + relativePosition
			);
		}
		return tableMutationDetails.resolveCustomSql( this::substituteBrackets );
	}

	@Nonnull
	private TableMutationDetails resolveTableMutationDetails(@Nonnull String tableName, int relativePosition) {
		final var tableMapping = findTableMapping( tableName, relativePosition );
		return new TableMutationDetails(
				CustomSqlMutationDetails.from( tableMapping.getInsertDetails() ),
				CustomSqlMutationDetails.from( tableMapping.getUpdateDetails() ),
				CustomSqlMutationDetails.from( tableMapping.getDeleteDetails() )
		);
	}

	@Nonnull
	private EntityTableMapping findTableMapping(@Nonnull String tableName, int relativePosition) {
		for ( var tableMapping : tableMappings ) {
			if ( tableMapping.relativePosition() == relativePosition && tableMapping.containsTableName( tableName ) ) {
				return tableMapping;
			}
		}
		for ( var tableMapping : tableMappings ) {
			if ( tableMapping.containsTableName( tableName ) ) {
				return tableMapping;
			}
		}
		throw new AssertionFailure( "Could not resolve table mapping for table " + tableName );
	}

	@Nonnull
	protected TableDescriptorBuilder createTableDescriptorBuilder(
			@Nonnull String tableName,
			int relativePosition,
			@Nonnull Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier) {
		var constraintModel = factory.getMappingMetamodel().getConstraintModel();
		// NOTE: if ActionQueue is not the graph-based one, isSelfReferential will have no impact
		final boolean isSelfReferential = constraintModel.hasSelfReferentialTable( tableName );
		final boolean hasUniqueKeys = isNotEmpty( constraintModel.getUniqueConstraintsForTable( tableName ) );
		var keyColumns = new ArrayList<ColumnDescriptor>();
		tableKeyColumnVisitationSupplier.get().accept( (index, selectableMapping)
				-> keyColumns.add( ColumnDescriptor.from( selectableMapping ) ) );

		final boolean isIdentifierTable = isIdentifierTable( tableName );
		final var mutationDetails = resolveTableMutationDetails( tableName, relativePosition );

		return new TableDescriptorBuilder(
				tableName,
				relativePosition,
				isIdentifierTable,
				!isIdentifierTable && isNullableTable( relativePosition ),
				isInverseTable( relativePosition ),
				mutationDetails,
				isTableCascadeDeleteEnabled( relativePosition ),
				isDynamicUpdate(),
				isDynamicInsert(),
				isSelfReferential,
				hasUniqueKeys,
				new TableKeyDescriptor( keyColumns )
			);
	}

	protected record TableMutationDetails(
			CustomSqlMutationDetails insertDetails,
			CustomSqlMutationDetails updateDetails,
			CustomSqlMutationDetails deleteDetails) {
		@Nonnull
		TableMutationDetails resolveCustomSql(@Nonnull Function<String, String> sqlResolver) {
			return new TableMutationDetails(
					insertDetails.resolveCustomSql( sqlResolver ),
					updateDetails.resolveCustomSql( sqlResolver ),
					deleteDetails.resolveCustomSql( sqlResolver )
			);
		}
	}

	protected record CustomSqlMutationDetails(
			Expectation expectation,
			String customSql,
			boolean callable) {
		@Nonnull
		static CustomSqlMutationDetails from(@Nonnull TableMapping.MutationDetails mutationDetails) {
			return new CustomSqlMutationDetails(
					mutationDetails.getExpectation(),
					mutationDetails.getCustomSql(),
					mutationDetails.isCallable()
			);
		}

		@Nonnull
		CustomSqlMutationDetails resolveCustomSql(@Nonnull Function<String, String> sqlResolver) {
			return new CustomSqlMutationDetails( expectation, sqlResolver.apply( customSql ), callable );
		}

		@Nonnull
		TableMapping.MutationDetails toMutationDetails(@Nonnull MutationType mutationType) {
			return new TableMapping.MutationDetails( mutationType, expectation, customSql, callable );
		}

		@Nonnull
		TableMapping.MutationDetails toMutationDetails(@Nonnull MutationType mutationType, boolean dynamicMutation) {
			return new TableMapping.MutationDetails( mutationType, expectation, customSql, callable, dynamicMutation );
		}
	}


	protected static class TableDescriptorBuilder {
		private final String tableName;
		private final int relativePosition;
		private final boolean isIdentifierTable;
		private final boolean isOptional;
		private final boolean isInverse;

		private final TableMutationDetails mutationDetails;
		private final boolean cascadeDeleteEnabled;

		private final boolean dynamicInsert;
		private final boolean dynamicUpdate;
		final boolean isSelfReferential;  // package-private for entity-wide self-referential check
		private final boolean hasUniqueKeys;

		private final TableKeyDescriptor keyDescriptor;
		private final List<ColumnDescriptor> columnDescriptors = new ArrayList<>();
		private final List<AttributeMapping> attributes = new ArrayList<>();
		private final Map<AttributeMapping,List<Integer>> attributeColumnIndexes = new HashMap<>();

		public TableDescriptorBuilder(
				@Nonnull String tableName,
				int relativePosition,
				boolean isIdentifierTable,
				boolean isOptional,
				boolean isInverse,
				@Nonnull TableMutationDetails mutationDetails,
				boolean cascadeDeleteEnabled,
				boolean dynamicInsert,
				boolean dynamicUpdate,
				boolean isSelfReferential,
				boolean hasUniqueKeys,
				@Nonnull TableKeyDescriptor keyDescriptor) {
			this.tableName = tableName;
			this.relativePosition = relativePosition;
			this.isIdentifierTable = isIdentifierTable;
			this.isOptional = isOptional;
			this.isInverse = isInverse;
			this.mutationDetails = mutationDetails;
			this.cascadeDeleteEnabled = cascadeDeleteEnabled;
			this.dynamicInsert = dynamicInsert;
			this.dynamicUpdate = dynamicUpdate;
			this.isSelfReferential = isSelfReferential;
			this.hasUniqueKeys = hasUniqueKeys;
			this.keyDescriptor = keyDescriptor;
		}

		@Nonnull
		protected EntityTableDescriptor build(boolean entityHasSelfReferentialTable) {
			return new EntityTableDescriptor(
					tableName,
					relativePosition,
					isIdentifierTable,
					isOptional,
					isInverse,
					entityHasSelfReferentialTable,
					hasUniqueKeys,
					cascadeDeleteEnabled,
					mutationDetails.insertDetails().toMutationDetails( MutationType.INSERT, dynamicInsert ),
					mutationDetails.updateDetails().toMutationDetails( MutationType.UPDATE, dynamicUpdate ),
					mutationDetails.deleteDetails().toMutationDetails( MutationType.DELETE ),
					columnDescriptors,
					attributes,
					attributeColumnIndexes,
					keyDescriptor
			);
		}

		public int addAttribute(@Nonnull AttributeMapping attribute) {
			attributes.add(attribute);
			return attributes.size() - 1;
		}

		public void addColumn(@Nonnull AttributeMapping attribute, @Nonnull ColumnDescriptor from) {
			// Check if this column already exists (e.g., multiple attributes mapping to same column)
			int existingIndex = findColumnIndex( from.name() );
			if ( existingIndex >= 0 ) {
				// Column already exists, just map this attribute to the existing column
				attributeColumnIndexes.computeIfAbsent( attribute, (a) -> new ArrayList<>() )
						.add( existingIndex );
			}
			else {
				// New column, add it
				columnDescriptors.add( from );
				attributeColumnIndexes.computeIfAbsent( attribute, (a) -> new ArrayList<>() )
						.add( columnDescriptors.size() - 1 );
			}
		}

		private int findColumnIndex(@Nonnull String columnName) {
			for ( int i = 0; i < columnDescriptors.size(); i++ ) {
				if ( columnDescriptors.get( i ).name().equals( columnName ) ) {
					return i;
				}
			}
			return -1;
		}
	}

	private static class TableMappingBuilder {
		private final String tableName;
		private final int relativePosition;
		private final EntityTableMappingImpl.KeyMapping keyMapping;
		private final boolean isOptional;
		private final boolean isInverse;
		private final boolean isIdentifierTable;
		private final boolean isSecondaryTable;

		private final TableMutationDetails mutationDetails;
		private final boolean cascadeDeleteEnabled;
		private final boolean dynamicUpdate;
		private final boolean dynamicInsert;

		private final List<Integer> attributeIndexes = new ArrayList<>();

		public TableMappingBuilder(
				@Nonnull String tableName,
				int relativePosition,
				@Nonnull EntityTableMappingImpl.KeyMapping keyMapping,
				boolean isOptional,
				boolean isInverse,
				boolean isIdentifierTable,
				boolean isSecondaryTable,
				@Nonnull TableMutationDetails mutationDetails,
				boolean cascadeDeleteEnabled,
				boolean dynamicUpdate,
				boolean dynamicInsert) {
			this.tableName = tableName;
			this.relativePosition = relativePosition;
			this.keyMapping = keyMapping;
			this.isOptional = isOptional;
			this.isInverse = isInverse;
			this.isIdentifierTable = isIdentifierTable;
			this.isSecondaryTable = isSecondaryTable;
			this.mutationDetails = mutationDetails;
			this.cascadeDeleteEnabled = cascadeDeleteEnabled;
			this.dynamicUpdate = dynamicUpdate;
			this.dynamicInsert = dynamicInsert;
		}

		@Nonnull
		private EntityTableMapping build() {
			final var insertDetails = mutationDetails.insertDetails();
			final var updateDetails = mutationDetails.updateDetails();
			final var deleteDetails = mutationDetails.deleteDetails();
			return new EntityTableMappingImpl(
					tableName,
					relativePosition,
					keyMapping,
					isOptional,
					isInverse,
					isIdentifierTable,
					isSecondaryTable,
					toIntArray( attributeIndexes ),
					insertDetails.expectation(),
					insertDetails.customSql(),
					insertDetails.callable(),
					updateDetails.expectation(),
					updateDetails.customSql(),
					updateDetails.callable(),
					cascadeDeleteEnabled,
					deleteDetails.expectation(),
					deleteDetails.customSql(),
					deleteDetails.callable(),
					dynamicUpdate,
					dynamicInsert
			);
		}
	}

	/**
	 * Builds the {@link EntityTableMappingImpl} descriptors for the tables mapped by this entity.
	 *
	 * @see #visitMutabilityOrderedTables
	 */
	@Nonnull
	protected EntityTableMapping[] buildTableMappings(@Nonnull PersistentClass bootEntityDescriptor) {
		final LinkedHashMap<String, TableMappingBuilder> tableBuilderMap = new LinkedHashMap<>();
		visitMutabilityOrderedTables( (tableExpression, relativePosition, tableKeyColumnSupplier) -> {
			final var tableMappingBuilder =
					getTableMappingBuilder(
							bootEntityDescriptor,
							tableExpression,
							relativePosition,
							tableKeyColumnSupplier,
							tableBuilderMap
					);
			if ( !isInverseTable( relativePosition ) ) {
				forEachAttributeMapping( (attributeIndex, attributeMapping) -> {
					if ( isPropertyOfTable( attributeIndex, relativePosition ) ) {
						tableMappingBuilder.attributeIndexes.add( attributeIndex );
					}
				} );
			}
		} );

		final var entityTableMappings = new EntityTableMapping[tableBuilderMap.size()];
		int i = 0;
		for ( var entry : tableBuilderMap.entrySet() ) {
			entityTableMappings[i++] = entry.getValue().build();
		}
		return entityTableMappings;
	}

	@Nonnull
	private TableMappingBuilder getTableMappingBuilder(
			@Nonnull PersistentClass bootEntityDescriptor,
			@Nonnull String tableExpression,
			int relativePosition,
			@Nonnull Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			@Nonnull Map<String, TableMappingBuilder> tableBuilderMap) {
		final var existing = tableBuilderMap.get( tableExpression );
		if ( existing != null ) {
			return existing;
		}
		else {
			final var tableMappingBuilder =
					createTableMappingBuilder(
							bootEntityDescriptor,
							tableExpression,
							relativePosition,
							tableKeyColumnVisitationSupplier
					);
			tableBuilderMap.put( tableExpression, tableMappingBuilder );
			return tableMappingBuilder;
		}
	}

	@Nonnull
	private TableMappingBuilder createTableMappingBuilder(
			@Nonnull PersistentClass bootEntityDescriptor,
			@Nonnull String tableExpression,
			int relativePosition,
			@Nonnull Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier) {
		final List<EntityTableMappingImpl.KeyColumn> keyColumns = new ArrayList<>();
		tableKeyColumnVisitationSupplier.get()
				.accept( (selectionIndex, selectableMapping) -> {
					keyColumns.add( new EntityTableMappingImpl.KeyColumn(
							tableExpression,
							selectableMapping
					) );
				} );

		final boolean isIdentifierTable = isIdentifierTable( tableExpression );
		final boolean isSecondaryTable = isSecondaryTable( tableExpression, relativePosition );
		final var mutationDetails = resolveTableMutationDetails( bootEntityDescriptor, relativePosition );

		return new TableMappingBuilder(
				tableExpression,
				relativePosition,
				EntityTableMapping.createKeyMapping( keyColumns, identifierMapping ),
				!isIdentifierTable && isNullableTable( relativePosition ),
				isInverseTable( relativePosition ),
				isIdentifierTable,
				isSecondaryTable,
				mutationDetails,
				isTableCascadeDeleteEnabled( relativePosition ),
				isDynamicUpdate(),
				isDynamicInsert()
		);
	}

	/**
	 * Determine if the specified table is a secondary table (@SecondaryTable).
	 * By default, returns false. Subclasses must override to provide accurate
	 * information.
	 */
	protected boolean isSecondaryTable(@Nonnull String tableExpression, int relativePosition) {
		return false;
	}

	/**
	 * Visit details about each table for this entity, using "mutability ordering".
	 * When inserting rows, the order we go through the tables to avoid foreign key
	 * problems among the entity's group of tables.
	 * <p>
	 * Used while {@linkplain #buildTableMappings building} the
	 * {@linkplain EntityTableMappingImpl table mapping} descriptors for each table.
	 *
	 * @see #forEachMutableTable
	 * @see #forEachMutableTableReverse
	 */
	protected abstract void visitMutabilityOrderedTables(@Nonnull MutabilityOrderedTableConsumer consumer);

	/**
	 * Consumer for processing table details.  Used while {@linkplain #buildTableMappings(PersistentClass) building}
	 * the {@link EntityTableMappingImpl} descriptors.
	 */
	protected interface MutabilityOrderedTableConsumer {
		void consume(
				@Nonnull String tableExpression,
				int relativePosition,
				@Nonnull Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier);
	}

	protected abstract boolean isIdentifierTable(@Nonnull String tableExpression);

	@Override
	public void addDiscriminatorToInsertGroup(@Nonnull MutationGroupBuilder insertGroupBuilder) {
	}

	@Override
	public void addAuxiliaryToInsertGroup(@Nonnull MutationGroupBuilder insertGroupBuilder) {
		if ( auxiliaryMapping instanceof LegacyAuxiliaryMutationSupport legacyMutationSupport ) {
			legacyMutationSupport.addToInsertGroup( insertGroupBuilder, this );
		}
	}

	@Override
	public void addSoftDeleteToInsertGroup(@Nonnull Function<String, TableInsertBuilder> insertGroupBuilder) {
		if ( getSoftDeleteMapping() != null ) {
			final TableInsertBuilder insertBuilder = insertGroupBuilder.apply( getIdentifierTableName() );
			final var mutatingTable = insertBuilder.getMutatingTable();
			final var columnReference = new ColumnReference( mutatingTable, getSoftDeleteMapping() );
			final var nonDeletedValueBinding = getSoftDeleteMapping().createNonDeletedValueBinding( columnReference );
			insertBuilder.addValueColumn( nonDeletedValueBinding );
		}
	}

	@Nullable
	protected String substituteBrackets(@Nullable String sql) {
		return sql == null ? null : new SQLQueryParser( sql, null, getFactory() ).process();
	}

	/**
	 * Load an instance using either the {@code forUpdateLoader} or the outer joining {@code loader},
	 * depending upon the value of the {@code lock} parameter
	 */
	@Nullable
	@Override
	public Object load(@Nonnull Object id, @Nullable Object optionalObject, @Nonnull LockMode lockMode, @Nonnull SharedSessionContractImplementor session) {
		return load( id, optionalObject, lockMode.toLockOptions(), session );
	}

	/**
	 * Load an instance using either the {@code forUpdateLoader} or the outer joining {@code loader},
	 * depending upon the value of the {@code lock} parameter
	 */
	@Nullable
	@Override
	public Object load(@Nonnull Object id, @Nullable Object optionalObject, @Nonnull LockOptions lockOptions, @Nonnull SharedSessionContractImplementor session)
			throws HibernateException {
		return doLoad( id, optionalObject, lockOptions, null, session );
	}

	@Nullable
	@Override
	public Object load(@Nonnull Object id, @Nullable Object optionalObject, @Nonnull LockOptions lockOptions, @Nonnull SharedSessionContractImplementor session, @Nullable Boolean readOnly)
			throws HibernateException {
		return doLoad( id, optionalObject, lockOptions, readOnly, session );
	}

	@Nullable
	private Object doLoad(@Nonnull Object id, @Nullable Object optionalObject, @Nonnull LockOptions lockOptions, @Nullable Boolean readOnly, @Nonnull SharedSessionContractImplementor session)
			throws HibernateException {
		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.fetchingEntity( infoString( this, id, getFactory() ) );
		}

		final var loader = determineLoaderToUse( session, lockOptions );
		return optionalObject == null
				? loader.load( id, lockOptions, readOnly, session )
				: loader.load( id, optionalObject, lockOptions, readOnly, session );
	}

	@Nonnull
	protected SingleIdEntityLoader<?> determineLoaderToUse(@Nonnull SharedSessionContractImplementor session, @Nonnull LockOptions lockOptions) {
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

	@Nonnull
	public SingleIdEntityLoader<?> getSingleIdLoader() {
		return singleIdLoader;
	}

	@Nullable
	@Override
	public Object initializeEnhancedEntityUsedAsProxy(
			@Nonnull Object entity,
			@Nullable String nameOfAttributeBeingAccessed,
			@Nonnull SharedSessionContractImplementor session) {
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

	@Nullable
	private Object loadEnhancedEntityUsedAsProxy(
			@Nonnull Object entity,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull EntityKey entityKey) {
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

	@Nullable
	private Object readEnhancedEntityAttribute(
			@Nonnull Object entity, @Nonnull Object id, @Nullable String nameOfAttributeBeingAccessed,
			@Nonnull SharedSessionContractImplementor session) {
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

	@Nonnull
	@Override
	public List<?> multiLoad(@Nonnull Object[] ids, @Nonnull SharedSessionContractImplementor session, @Nonnull MultiIdLoadOptions loadOptions) {
		return multiIdLoader.load( ids, loadOptions, session );
	}

	@Override
	public void registerAffectingFetchProfile(@Nonnull String fetchProfileName) {
		if ( affectingFetchProfileNames == null ) {
			affectingFetchProfileNames = new HashSet<>();
		}
		affectingFetchProfileNames.add( fetchProfileName );
	}

	@Override
	public boolean isAffectedByEntityGraph(@Nonnull LoadQueryInfluencers loadQueryInfluencers) {
		final var graph = loadQueryInfluencers.getEffectiveEntityGraph().getGraph();
		return graph != null
			&& graph.appliesTo( getFactory().getJpaMetamodel().entity( getEntityName() ) );
	}

	@Override
	public boolean isAffectedByEnabledFetchProfiles(@Nonnull LoadQueryInfluencers loadQueryInfluencers) {
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
			@Nonnull LoadQueryInfluencers loadQueryInfluencers,
			boolean onlyApplyForLoadByKeyFilters) {
		return loadQueryInfluencers.hasEnabledFilters()
			&& isAffectedByEnabledFilters( new HashSet<>(), loadQueryInfluencers, onlyApplyForLoadByKeyFilters );
	}

	@Override
	public boolean isAffectedByEnabledFilters(
			@Nonnull Set<ManagedMappingType> visitedTypes,
			@Nonnull LoadQueryInfluencers influencers,
			boolean onlyApplyForLoadByKey) {
		assert influencers.hasEnabledFilters();
		if ( !visitedTypes.add( this ) ) {
			return false;
		}
		return filterHelper != null
			&& filterHelper.isAffectedBy( influencers.getEnabledFilters(), onlyApplyForLoadByKey )
			|| areAttributesAffectedByEnabledFilters( visitedTypes, influencers, onlyApplyForLoadByKey );
	}

	@Override
	public boolean isAffectedByInfluencers(
			@Nonnull LoadQueryInfluencers influencers,
			boolean onlyApplyForLoadByKeyFilters) {
		return EntityPersister.super.isAffectedByInfluencers( influencers, onlyApplyForLoadByKeyFilters )
			|| auxiliaryMapping != null && auxiliaryMapping.isAffectedByInfluencers( influencers );
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
	@Nullable
	@Override
	public int[] findDirty(@Nonnull Object[] currentState, @Nonnull Object[] previousState, @Nonnull Object entity, @Nonnull SharedSessionContractImplementor session)
			throws HibernateException {
		final int[] dirty = DirtyHelper.findDirty(
				getDirtyCheckablePropertyTypes(),
				currentState,
				previousState,
				propertyColumnUpdateable,
				session
		);
		if ( dirty == null ) {
			return null;
		}
		else {
			logDirtyProperties( dirty );
			return dirty;
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
	@Nullable
	@Override
	public int[] findModified(@Nonnull Object[] old, @Nonnull Object[] current, @Nonnull Object entity, @Nonnull SharedSessionContractImplementor session)
			throws HibernateException {
		final int[] modified = DirtyHelper.findModified(
				getPropertyTypes(),
				getPropertyDirtyCheckability(),
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
	@Nonnull
	public boolean[] getPropertyUpdateability(@Nonnull Object entity) {
		return hasUninitializedLazyProperties( entity )
				? getNonLazyPropertyUpdateability()
				: getPropertyUpdateability();
	}

	@Override
	public boolean isPropertyTemporalExcluded(int attributeIndex) {
		return propertyTemporalExcluded[attributeIndex];
	}

	@Override
	public boolean isPropertyAuditedExcluded(int attributeIndex) {
		return propertyAuditedExcluded[attributeIndex];
	}

	@Override
	public boolean excludedFromTemporalVersioning(
			@Nullable int[] dirtyAttributeIndexes,
			boolean hasDirtyCollection) {
		if ( !hasTemporalExcludedProperties
				|| hasDirtyCollection
				|| dirtyAttributeIndexes == null ) {
			return false;
		}
		else {
			for ( final int index : dirtyAttributeIndexes ) {
				if ( !propertyTemporalExcluded[index] ) {
					return false;
				}
			}
			return true;
		}
	}

	private void logDirtyProperties(@Nonnull int[] properties) {
		if ( CORE_LOGGER.isTraceEnabled() ) {
			for ( int property : properties ) {
				CORE_LOGGER.propertyIsDirty( qualify( getEntityName(),
						getAttributeMapping( property ).getAttributeName() ) );
			}
		}
	}

	@Nonnull
	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Nonnull
	private Dialect getDialect() {
		return factory.getJdbcServices().getDialect();
	}

	@Override
	public FilteredAssociationMapping getFilteredAssociationMapping() {
		return filteredAssociationMapping;
	}

	@Override
	public void initializeCacheRestrictions(MetadataImplementor bootModel) {
		if ( canReadFromCache || canWriteToCache ) {
			cacheRestrictions = EntityCacheRestrictions.create( this, bootModel );
		}
	}

	@Override
	public boolean hasSqlRestrictedAssociations() {
		return cacheRestrictions.hasSqlRestrictions();
	}

	@Override
	public boolean isAffectedByEnabledFiltersForCache(LoadQueryInfluencers influencers) {
		return cacheRestrictions.isAffectedByFilters( influencers );
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
	@Nullable
	public EntityDataAccess getCacheAccessStrategy() {
		return cacheAccessStrategy;
	}

	@Override
	@Nonnull
	public CacheEntryStructure getCacheEntryStructure() {
		return cacheEntryHelper.getCacheEntryStructure();
	}

	@Override
	@Nonnull
	public CacheEntry buildCacheEntry(
			@Nonnull Object entity,
			@Nonnull Object[] state,
			@Nullable Object version,
			@Nonnull SharedSessionContractImplementor session) {
		assert entity != null;
		assert state != null;
		return cacheEntryHelper.buildCacheEntry( entity, state, version, session );
	}

	@Override
	public boolean hasNaturalIdCache() {
		return naturalIdRegionAccessStrategy != null;
	}

	@Nullable
	@Override
	public NaturalIdDataAccess getNaturalIdCacheAccessStrategy() {
		return naturalIdRegionAccessStrategy;
	}


	// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@Nonnull
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
		return !isIdentifierVirtual();
	}

	@Nullable
	@Override
	public BasicType<?> getVersionType() {
		final int versionPropertyIndex = getVersionPropertyIndex();
		return versionPropertyIndex == NO_VERSION_INDX
				? null
				: (BasicType<?>) getPropertyTypes()[versionPropertyIndex];
	}

	@Override
	public boolean isIdentifierAssignedByInsert() {
		return isIdentifierAssignedByInsertInternal();
	}

	@Override
	public void afterReassociate(@Nonnull Object entity, @Nonnull SharedSessionContractImplementor session) {
		final var metadata = getBytecodeEnhancementMetadata();
		if ( metadata.isEnhancedForLazyLoading() ) {
			final var interceptor = metadata.extractLazyInterceptor( entity );
			if ( interceptor == null ) {
				metadata.injectInterceptor( entity, castNonNull( getIdentifier( entity, session ) ), session );
			}
			else {
				interceptor.setSession( session );
			}
		}
		handleNaturalIdReattachment( entity, session );
	}

	private void handleNaturalIdReattachment(@Nonnull Object entity, @Nonnull SharedSessionContractImplementor session) {
		if ( naturalIdMapping != null ) {
			if ( naturalIdMapping.isMutable() ) {
				final var persistenceContext = session.getPersistenceContextInternal();
				final var naturalIdResolutions = persistenceContext.getNaturalIdResolutions();
				final Object id = castNonNull( getIdentifier( entity, session ) );

				// for reattachment of mutable natural-ids, we absolutely positively have to grab the snapshot from the
				// database, because we have no other way to know if the state changed while detached.
				final Object[] entitySnapshot = persistenceContext.getDatabaseSnapshot( id, this );
				final Object naturalIdSnapshot = naturalIdMapping.extractNaturalIdFromEntityState( entitySnapshot );

				naturalIdResolutions.removeSharedResolution( id, naturalIdSnapshot, this, false );
				final Object naturalId = naturalIdMapping.extractNaturalIdFromEntity( entity );
				naturalIdResolutions.manageLocalResolution( id, naturalId, this, CachedNaturalIdValueSource.UPDATE );
			}
			// otherwise we assume there were no changes to natural id during detachment for now,
			// that is validated later during flush.
		}
	}

	@Nullable
	@Override
	public Boolean isTransient(@Nonnull Object entity, @Nonnull SharedSessionContractImplementor session) throws HibernateException {
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
			final Boolean isUnsaved = castNonNull( versionMapping ).getUnsavedStrategy().isUnsaved( version );
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
		if ( session.getCacheMode().isGetEnabled() ) {
			final var cacheEntry = readingFromCache( this, cache -> {
				final Object cacheKey =
						cache.generateCacheKey( id, this,
								session.getFactory(), session.getTenantIdentifier() );
				return fromSharedCache( session, cacheKey, this, cache );
			}, null );
			if ( cacheEntry != null ) {
				return false;
			}
		}

		return null;
	}

	@Override
	public boolean isLazy() {
		return isLazyByMetadata()
			&& !canUseReferenceCacheEntries()
			&& canDelayLoad();
	}

	private boolean canDelayLoad() {
		// isLazy() is called while the representation strategy is being built
		// to decide whether a ProxyFactory should be created.
		return getBytecodeEnhancementMetadata().isEnhancedForLazyLoading()
			|| representationStrategy == null
			|| representationStrategy.getProxyFactory() != null;
	}

	@Override
	public boolean hasProxy() {
		// skip proxy instantiation if entity is bytecode enhanced
		return isLazy()
			&& !getBytecodeEnhancementMetadata().isEnhancedForLazyLoading()
			&& representationStrategy.getProxyFactory() != null;
	}

	@Nonnull
	@Override
	public Generator getGenerator() {
		return getIdentifierGenerator();
	}

	@Nonnull
	@Override
	public String getRootEntityName() {
		return getRootName();
	}

	@Nullable
	@Override
	public String getMappedSuperclass() {
		return getSuperclass();
	}

	@Override
	public boolean isConcreteProxy() {
		return concreteProxy;
	}

	@Nonnull
	@Override
	public EntityPersister resolveConcreteProxyTypeForId(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
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
	@Nonnull
	@Override @Deprecated
	public Type getPropertyType(@Nonnull String propertyName) throws MappingException {
		final var propertyPath = resolvePropertyPath( propertyName );
		if ( propertyPath == null ) {
			throw new QueryException( "Could not resolve property: " + propertyName + " of: " + getEntityName() );
		}
		return propertyPath.type();
	}

	@Override
	public boolean isSelectBeforeUpdateRequired() {
		return isSelectBeforeUpdate();
	}

	@Nonnull
	public final OptimisticLockStyle optimisticLockStyle() {
		return getOptimisticLockStyle();
	}

	@Nonnull
	@Override
	public Object createProxy(@Nonnull Object id, @Nullable SharedSessionContractImplementor session) throws HibernateException {
		return representationStrategy.getProxyFactory().getProxy( id, session );
	}

	@Nonnull
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

	@Nonnull
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
	public void afterInitialize(@Nonnull Object entity, @Nonnull SharedSessionContractImplementor session) {
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

	@Nonnull
	@Override
	public boolean[] getNonLazyPropertyUpdateability() {
		return getNonlazyPropertyUpdateability();
	}

	@Nonnull
	@Override
	public CascadeStyle[] getPropertyCascadeStyles() {
		return getCascadeStyles();
	}

	@Nonnull
	@Internal
	public final CascadePropertySelection getCascadePropertySelection(@Nonnull CascadingAction<?> action) {
		return super.getCascadePropertySelection( action );
	}

	@Nullable
	@Override
	public final Class<?> getMappedClass() {
		return this.getMappedJavaType().getJavaTypeClass();
	}

	@Nullable
	@Override
	public Class<?> getConcreteProxyClass() {
		final var proxyJavaType = getRepresentationStrategy().getProxyJavaType();
		return proxyJavaType != null ? proxyJavaType.getJavaTypeClass() : javaType.getJavaTypeClass();
	}

	@Override
	public void setPropertyValues(@Nonnull Object object, @Nonnull Object[] values) {
		if ( multiValueWriter != null ) {
			multiValueWriter.set( object, values );
		}
		else {
			final int size = getAttributeMappings().size();
			if ( getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() ) {
				for ( int i = 0; i < size; i++ ) {
					final Object value = values[i];
					if ( value != UNFETCHED_PROPERTY ) {
						accessorCache[i].set( object, value );
					}
				}
			}
			else {
				for ( int i = 0; i < size; i++ ) {
					accessorCache[i].set( object, values[i] );
				}
			}
		}
	}

	@Override
	public void setPropertyValue(@Nonnull Object object, int i, @Nullable Object value) {
		accessorCache[i].set( object, value );
	}

	@Nonnull
	@Override
	public Object[] getPropertyValues(@Nonnull Object object) {
		if ( multiValueReader != null ) {
			return multiValueReader.get( object );
		}
		else {
			final var enhancementMetadata = getBytecodeEnhancementMetadata();
			final var attributeMappings = getAttributeMappings();
			final var values = new Object[attributeMappings.size()];
			if ( enhancementMetadata.isEnhancedForLazyLoading() ) {
				final var lazyAttributesMetadata = enhancementMetadata.getLazyAttributesMetadata();
				for ( int i = 0; i < attributeMappings.size(); i++ ) {
					final var attributeMapping = attributeMappings.get( i );
					if ( !lazyAttributesMetadata.isLazyAttribute( attributeMapping.getAttributeName() )
							|| enhancementMetadata.isAttributeLoaded( object, attributeMapping.getAttributeName() ) ) {
						values[i] = accessorCache[i].get( object );
					}
					else {
						values[i] = UNFETCHED_PROPERTY;
					}
				}
			}
			else {
				for ( int i = 0; i < attributeMappings.size(); i++ ) {
					values[i] = accessorCache[i].get( object );
				}
			}

			return values;
		}
	}

	@Nullable
	@Override
	public Object getPropertyValue(@Nonnull Object object, int i) {
		return accessorCache[i].get( object );
	}

	@Nullable
	@Override
	public Object getPropertyValue(@Nonnull Object object, @Nonnull String path) {
		final String basePropertyName = root( path );
		final boolean isBasePath = basePropertyName.length() == path.length();
		final var attributeMapping = findAttributeMapping( basePropertyName );
		final Object baseValue;
		final MappingType baseValueType;
		if ( attributeMapping != null ) {
			baseValue = accessorCache[ attributeMapping.getStateArrayPosition() ].get( object );
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

	@Nullable
	private Object getPropertyValue(
			@Nullable Object baseValue,
			@Nullable ManagedMappingType baseValueType,
			@Nonnull String path,
			@Nonnull String prefix) {
		if ( baseValue == null || baseValueType == null ) {
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

	@Nullable
	@Override
	public Object getIdentifier(@Nonnull Object entity, @Nullable SharedSessionContractImplementor session) {
		return identifierMapping.getIdentifier( entity );
	}

	@Nullable
	@Override
	public Object getIdentifier(@Nonnull Object entity, @Nullable MergeContext mergeContext) {
		return identifierMapping.getIdentifier( entity, mergeContext );
	}

	@Override
	public void setIdentifier(@Nonnull Object entity, @Nullable Object id, @Nonnull SharedSessionContractImplementor session) {
		identifierMapping.setIdentifier( entity, id, session );
	}

	@Nullable
	@Override
	public Object getVersion(@Nonnull Object object) {
		final var versionMapping = getVersionMapping();
		return versionMapping == null ? null
				: versionMapping.getVersionAttribute().getPropertyAccess().getPropertyValueAccessor().get( object );
	}

	@Override
	@Nonnull
	public Object instantiate(@Nullable Object id, @Nonnull SharedSessionContractImplementor session) {
		final Object instance = getRepresentationStrategy().getInstantiator().instantiate();
		linkToSession( instance, session );
		if ( id != null ) {
			setIdentifier( instance, id, session );
		}
		return instance;
	}

	protected void linkToSession(@Nonnull Object entity, @Nullable SharedSessionContractImplementor session) {
		if ( session != null ) {
			processIfPersistentAttributeInterceptable( entity, this::setSession, session );
		}
	}

	private void setSession(@Nonnull PersistentAttributeInterceptable entity, @Nonnull SharedSessionContractImplementor session) {
		final var interceptor =
				getBytecodeEnhancementMetadata()
						.extractLazyInterceptor( entity );
		if ( interceptor != null ) {
			interceptor.setSession( session );
		}
	}

	@Override
	public boolean isInstance(@Nonnull Object object) {
		return getRepresentationStrategy().getInstantiator().isInstance( object );
	}

	@Override
	public boolean hasUninitializedLazyProperties(@Nonnull Object object) {
		return getBytecodeEnhancementMetadata().hasUnFetchedAttributes( object );
	}

	@Override
	public void resetIdentifier(
			@Nonnull Object entity,
			@Nonnull Object currentId,
			@Nullable Object currentVersion,
			@Nonnull SharedSessionContractImplementor session) {
		if ( !getGenerator().allowAssignedIdentifiers() ) {
			// reset the identifier
			final Object defaultIdentifier =
					identifierMapping.getUnsavedStrategy()
							.getDefaultValue( currentId );
			setIdentifier( entity, defaultIdentifier, session );
		}
		// reset the version
		if ( versionMapping != null ) {
			final Object defaultVersion =
					castNonNull( versionMapping ).getUnsavedStrategy()
							.getDefaultValue( currentVersion );
			versionMapping.getVersionAttribute().getPropertyAccess()
					.getPropertyValueAccessor().set( entity, defaultVersion );
		}
	}

	@Nonnull
	@Override
	public EntityPersister getSubclassEntityPersister(@Nullable Object instance, @Nonnull SessionFactoryImplementor factory) {
		if ( instance != null
				&& hasSubclasses()
				&& !getRepresentationStrategy().getInstantiator().isSameClass( instance ) ) {
			// todo (6.0) : this previously used the old tuple tuplizer infrastructure
			//		- we may need something similar here...
			for ( var subclassMappingType : castNonNull( subclassMappingTypes ).values() ) {
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

	@Nonnull
	@Override
	public Object[] getPropertyValuesToInsert(
			@Nonnull Object entity,
			@Nullable Map<Object,Object> mergeMap,
			@Nonnull SharedSessionContractImplementor session)
				throws HibernateException {
		if ( shouldGetAllProperties( entity ) && multiValueReader != null ) {
			return multiValueReader.get( entity );
		}
		else {
			final var result = new Object[attributeMappings.size()];
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				result[i] = accessorCache[i].getForInsert( entity, mergeMap, session );
			}
			return result;
		}
	}

	protected boolean shouldGetAllProperties(@Nonnull Object entity) {
		final var metadata = getBytecodeEnhancementMetadata();
		return !metadata.isEnhancedForLazyLoading()
			|| !metadata.hasUnFetchedAttributes( entity );
	}

	@Override
	public void processInsertGeneratedProperties(
			@Nonnull Object id,
			@Nonnull Object entity,
			@Nonnull Object[] state,
			@Nullable GeneratedValues generatedValues,
			@Nonnull SharedSessionContractImplementor session) {
		if ( insertGeneratedValuesProcessor == null ) {
			throw new UnsupportedOperationException( "Entity has no insert-generated properties - '" + getEntityName() + "'" );
		}
		insertGeneratedValuesProcessor.processGeneratedValues( entity, id, state, generatedValues, session );
	}

	@Nonnull
	protected List<? extends ModelPart> initInsertGeneratedProperties(@Nonnull List<AttributeMapping> generatedAttributes) {
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

	@Nonnull
	@Override
	public List<? extends ModelPart> getInsertGeneratedProperties() {
		return insertGeneratedProperties;
	}

	@Override
	public void processUpdateGeneratedProperties(
			@Nonnull Object id,
			@Nonnull Object entity,
			@Nonnull Object[] state,
			@Nullable GeneratedValues generatedValues,
			@Nonnull SharedSessionContractImplementor session) {
		if ( updateGeneratedValuesProcessor == null ) {
			throw new AssertionFailure( "Entity has no update-generated properties - '" + getEntityName() + "'" );
		}
		updateGeneratedValuesProcessor.processGeneratedValues( entity, id, state, generatedValues, session );
	}

	@Nonnull
	protected List<? extends ModelPart> initUpdateGeneratedProperties(@Nonnull List<AttributeMapping> generatedAttributes) {
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

	@Nonnull
	@Override
	public List<? extends ModelPart> getUpdateGeneratedProperties() {
		return updateGeneratedProperties;
	}

	@Nullable
	@Override
	public String getIdentifierPropertyName() {
		return getIdentifierAttributeName();
	}

	@Nonnull
	@Override
	public Type getIdentifierType() {
		return getIdentifierAttributeType();
	}

	@Override
	public boolean hasSubselectLoadableCollections() {
		return hasSubselectLoadableCollections;
	}

	@Override
	public boolean hasSubselectLoadableAttributes() {
		return hasSubselectLoadableAttributes;
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

	@Nullable
	@Override
	public Object getNaturalIdentifierSnapshot(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		verifyHasNaturalId();
		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.gettingCurrentNaturalIdSnapshot( getEntityName(), id );
		}
		return getNaturalIdLoader().resolveIdToNaturalId( id, session );
	}


	@Nonnull
	@Override
	public NaturalIdLoader<?> getNaturalIdLoader() {
		verifyHasNaturalId();
		if ( naturalIdLoader == null ) {
			naturalIdLoader = castNonNull( naturalIdMapping ).makeLoader( this );
		}
		return naturalIdLoader;
	}

	@Nonnull
	@Override
	public MultiNaturalIdLoader<?> getMultiNaturalIdLoader() {
		verifyHasNaturalId();
		if ( multiNaturalIdLoader == null ) {
			multiNaturalIdLoader = castNonNull( naturalIdMapping ).makeMultiLoader( this );
		}
		return multiNaturalIdLoader;
	}

	public static int getTableId(@Nonnull String tableName, @Nonnull String[] tables) {
		for ( int j = 0; j < tables.length; j++ ) {
			if ( tableName.equalsIgnoreCase( tables[j] ) ) {
				return j;
			}
		}
		throw new AssertionFailure( "Table " + tableName + " not found" );
	}

	@Nonnull
	@Override
	public EntityRepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Nonnull
	@Override
	public String getTableNameForColumn(@Nonnull String columnName) {
		return getTableName( determineTableNumberForColumn( columnName ) );
	}

	protected int determineTableNumberForColumn(@Nonnull String columnName) {
		return 0;
	}

	@Nonnull
	public String determineTableName(@Nonnull Table table) {
		return table.getSubselect() != null
				? "( " + createSqlQueryParser( table ).process() + " )"
				: factory.getSqlStringGenerationContext().format( table.getQualifiedTableName() );
	}

	@Nonnull
	private SQLQueryParser createSqlQueryParser(@Nonnull Table table) {
		return new SQLQueryParser(
				table.getSubselect(),
				null,
				// NOTE: this allows finer control over catalog and schema used for
				// placeholder handling (`{h-catalog}`, `{h-schema}`, `{h-domain}`)
				new ExplicitSqlStringGenerationContext( table.getCatalog(), table.getSchema(), factory )
		);
	}

	/**
	 * Consolidated these onto a single helper because the 2 pieces work in tandem.
	 */
	public interface CacheEntryHelper {
		@Nonnull
		CacheEntryStructure getCacheEntryStructure();

		@Nonnull
		CacheEntry buildCacheEntry(
				@Nonnull Object entity,
				@Nonnull Object[] state,
				@Nullable Object version,
				@Nonnull SharedSessionContractImplementor session);
	}

	private record StandardCacheEntryHelper(EntityPersister persister)
			implements CacheEntryHelper {

		@Override
		@Nonnull
		public CacheEntryStructure getCacheEntryStructure() {
			return UnstructuredCacheEntry.INSTANCE;
		}

		@Override
		@Nonnull
		public CacheEntry buildCacheEntry(
				@Nonnull Object entity,
				@Nonnull Object[] state,
				@Nullable Object version,
				@Nonnull SharedSessionContractImplementor session) {
			return new StandardCacheEntryImpl( state, persister, version, session, entity );
		}
	}

	private record ReferenceCacheEntryHelper(EntityPersister persister)
			implements CacheEntryHelper {

		@Override
		@Nonnull
		public CacheEntryStructure getCacheEntryStructure() {
			return UnstructuredCacheEntry.INSTANCE;
		}

		@Override
		@Nonnull
		public CacheEntry buildCacheEntry(
				@Nonnull Object entity,
				@Nonnull Object[] state,
				@Nullable Object version,
				@Nonnull SharedSessionContractImplementor session) {
			return new ReferenceCacheEntryImpl( entity, persister );
		}
	}

	private record StructuredCacheEntryHelper(EntityPersister persister, StructuredCacheEntry structure)
			implements CacheEntryHelper {

		private StructuredCacheEntryHelper(@Nonnull EntityPersister persister) {
			this( persister, new StructuredCacheEntry( persister ) );
		}

		@Override
		@Nonnull
		public CacheEntryStructure getCacheEntryStructure() {
			return structure;
		}

		@Override
		@Nonnull
		public CacheEntry buildCacheEntry(
				@Nonnull Object entity,
				@Nonnull Object[] state,
				@Nullable Object version,
				@Nonnull SharedSessionContractImplementor session) {
			return new StandardCacheEntryImpl( state, persister, version, session, entity );
		}
	}

	private static class NoopCacheEntryHelper implements CacheEntryHelper {
		public static final NoopCacheEntryHelper INSTANCE = new NoopCacheEntryHelper();

		@Override
		@Nonnull
		public CacheEntryStructure getCacheEntryStructure() {
			return UnstructuredCacheEntry.INSTANCE;
		}

		@Override
		@Nonnull
		public CacheEntry buildCacheEntry(
				@Nonnull Object entity,
				@Nonnull Object[] state,
				@Nullable Object version,
				@Nonnull SharedSessionContractImplementor session) {
			throw new HibernateException( "Illegal attempt to build cache entry for non-cached entity" );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// org.hibernate.metamodel.mapping.EntityMappingType

	@Override
	public void forEachAttributeMapping(@Nonnull Consumer<? super AttributeMapping> action) {
		this.attributeMappings.forEach( action );
	}

	@Override
	public void forEachAttributeMapping(@Nonnull IndexedConsumer<? super AttributeMapping> consumer) {
		attributeMappings.indexedForEach( consumer );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution flow for "mapping model initialization -
	//		1. prepareMappingModel
	//		2. postInstantiate
	//			2.a. doLateInit
	//		3. prepareLoaders
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void prepareMappingModel(@Nonnull MappingModelCreationProcess creationProcess) {
		if ( identifierMapping == null ) {
			prepareMappings( creationProcess );
			handleSubtypeMappings( creationProcess );
			prepareMultiTableMutationStrategy( creationProcess );
			prepareMultiTableInsertStrategy( creationProcess );
		}
	}

	private void handleSubtypeMappings(@Nonnull MappingModelCreationProcess creationProcess) {
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


	private void prepareMappings(@Nonnull MappingModelCreationProcess creationProcess) {
		final var persistentClass =
				creationProcess.getCreationContext().getBootModel()
						.getEntityBinding( getEntityName() );
		initializeSpecialAttributeMappings( creationProcess, persistentClass );
		setVersionGenerator( createVersionGenerator( getVersionGenerator(), versionMapping ) );
		buildDeclaredAttributeMappings( creationProcess, persistentClass );
		getAttributeMappings();
		initializeNaturalIdMapping( creationProcess, persistentClass );
	}

	private void initializeSpecialAttributeMappings
			(@Nonnull MappingModelCreationProcess creationProcess, @Nonnull PersistentClass bootEntityDescriptor) {
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
		final var superMappingType = castNonNull( this.superMappingType );
		discriminatorMapping = superMappingType.getDiscriminatorMapping();
		identifierMapping = superMappingType.getIdentifierMapping();
		naturalIdMapping = superMappingType.getNaturalIdMapping();
		versionMapping = superMappingType.getVersionMapping();
		rowIdMapping = superMappingType.getRowIdMapping();
		auxiliaryMapping = superMappingType.getAuxiliaryMapping();
	}

	private void buildDeclaredAttributeMappings
			(@Nonnull MappingModelCreationProcess creationProcess, @Nonnull PersistentClass bootEntityDescriptor) {
		final var allPropertyClosure = bootEntityDescriptor.getAllPropertyClosure();
		final var mappingsBuilder = AttributeMappingsMap.builder();
		final var genericMappingsBuilder = AttributeMappingsMap.builder();
		int stateArrayPosition = getStateArrayInitialPosition( creationProcess );
		int fetchableIndex = getFetchableIndexOffset();
		for ( var property : allPropertyClosure ) {
			if ( !property.isGeneric() ) {
				final String attributeName = property.getName();
				final var bootProperty = bootEntityDescriptor.getProperty( attributeName );
				if ( superMappingType == null
					|| superMappingType.findAttributeMapping( bootProperty.getName() ) == null ) {
					mappingsBuilder.put(
							attributeName,
							generateNonIdAttributeMapping(
									bootProperty,
									stateArrayPosition++,
									fetchableIndex++,
									creationProcess
							)
					);
				}
				declaredAttributeMappings = mappingsBuilder.build();
			}
			else {
				final int span = property.getColumnSpan();
				final String[] colNames = new String[span];
				final var selectables = property.getSelectables();
				final Dialect dialect = getDialect();
				final TypeConfiguration typeConfiguration = creationProcess.getCreationContext().getTypeConfiguration();
				for ( int k = 0; k < selectables.size(); k++ ) {
					final var selectable = selectables.get(k);
					if ( selectable instanceof Formula formula ) {
						formula.setFormula( substituteBrackets( formula.getFormula() ) );
						colNames[k] = selectable.getTemplate( dialect, typeConfiguration );
					}
					else if ( selectable instanceof Column column ) {
						colNames[k] = column.getQuotedName( dialect );
					}
				}
				final String tableName = determineTableName( property.getValue().getTable() );
				genericMappingsBuilder.put(
						property.getName(),
						generateNonIdAttributeMapping(
								property.getName(),
								property.getType(),
								property.getCascadeStyle(),
								-1,
								tableName,
								colNames,
								property,
								-1,
								-1,
								creationProcess
						)
				);
				declaredGenericAttributeMappings = genericMappingsBuilder.build();
			}
			// otherwise, it's defined on the supertype, skip it here
		}
	}

	private static @Nullable BeforeExecutionGenerator createVersionGenerator(
			@Nullable BeforeExecutionGenerator configuredGenerator,
			@Nullable EntityVersionMapping versionMapping) {
		if ( versionMapping != null ) {
			return configuredGenerator == null ? new VersionGeneration( versionMapping ) : configuredGenerator;
		}
		else {
			return configuredGenerator;
		}
	}

	private void prepareMultiTableMutationStrategy(@Nonnull MappingModelCreationProcess creationProcess) {
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

	private void prepareMultiTableInsertStrategy(@Nonnull MappingModelCreationProcess creationProcess) {
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
		if ( generator instanceof BulkInsertionCapableIdentifierGenerator bulkInsertionCapableGenerator
				&& generator instanceof OptimizableGenerator optimizableGenerator ) {
			final var optimizer = optimizableGenerator.getOptimizer();
			return optimizer != null && optimizer.getIncrementSize() > 1
				|| !bulkInsertionCapableGenerator.supportsBulkInsertionIdentifierGeneration();
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

	private void prepareMappingModel(@Nonnull MappingModelCreationProcess creationProcess, @Nonnull PersistentClass bootEntityDescriptor) {
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
		auxiliaryMapping = rootClass == bootEntityDescriptor ?
				stateManagement.createAuxiliaryMapping( this, rootClass, creationProcess ) :
				castNonNull( superMappingType ).getAuxiliaryMapping();
		if ( auxiliaryMapping instanceof SoftDeleteMapping && rootClass.getCustomSQLDelete() != null ) {
			throw new UnsupportedMappingException( "Entity may not define both @SoftDelete and @SQLDelete" );
		}
	}

	private void initializeNaturalIdMapping
			(@Nonnull MappingModelCreationProcess creationProcess, @Nonnull PersistentClass bootEntityDescriptor) {
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

	@Nonnull
	protected NaturalIdMapping generateNaturalIdMapping(
			@Nonnull MappingModelCreationProcess creationProcess,
			@Nonnull PersistentClass bootEntityDescriptor) {
		//noinspection AssertWithSideEffects
		assert bootEntityDescriptor.hasNaturalId();

		final int[] naturalIdAttributeIndexes = castNonNull( getNaturalIdentifierProperties() );
		assert naturalIdAttributeIndexes.length > 0;

		if ( naturalIdAttributeIndexes.length == 1 ) {
			if ( bootEntityDescriptor.getRootClass().getNaturalIdClass() != null ) {
				throw new UnsupportedMappingException( "NaturalIdClass not supported for simple naturaal-id mappings" );
			}
			final String propertyName = getPropertyNames()[ naturalIdAttributeIndexes[ 0 ] ];
			final var attributeMapping = (SingularAttributeMapping) findAttributeMapping( propertyName );
			return new SimpleNaturalIdMapping(
					attributeMapping,
					this,
					creationProcess
			);
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

		return new CompoundNaturalIdMapping(
				this,
				bootEntityDescriptor.getRootClass().getNaturalIdClass(),
				collectedAttrMappings,
				creationProcess
		);
	}

	@Nonnull
	protected static SqmMultiTableMutationStrategy interpretSqmMultiTableStrategy(
			@Nonnull AbstractEntityPersister entityMappingDescriptor,
			@Nonnull MappingModelCreationProcess creationProcess) {
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
				.createMutationStrategy( entityMappingDescriptor, creationProcess.getCreationContext() );
	}

	@Nonnull
	protected static SqmMultiTableInsertStrategy interpretSqmMultiTableInsertStrategy(
			@Nonnull AbstractEntityPersister entityMappingDescriptor,
			@Nonnull MappingModelCreationProcess creationProcess) {
		return creationProcess.getCreationContext().getServiceRegistry()
				.requireService( SqmMultiTableMutationStrategyProvider.class )
				.createInsertStrategy( entityMappingDescriptor, creationProcess.getCreationContext() );
	}

	@Nullable
	@Override
	public SqmMultiTableMutationStrategy getSqmMultiTableMutationStrategy() {
		return sqmMultiTableMutationStrategy;
	}

	@Nullable
	@Override
	public SqmMultiTableInsertStrategy getSqmMultiTableInsertStrategy() {
		return sqmMultiTableInsertStrategy;
	}

	protected int getStateArrayInitialPosition(@Nonnull MappingModelCreationProcess creationProcess) {
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

	@Nullable
	protected EntityDiscriminatorMapping generateDiscriminatorMapping(@Nonnull PersistentClass bootEntityDescriptor) {
		if ( getDiscriminatorType() == null ) {
			return null;
		}
		else {
			final String discriminatorColumnExpression;
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
					length = null;
					arrayLength = null;
					precision = null;
					scale = null;
				}
				else {
					length = column.getLength();
					arrayLength = column.getArrayLength();
					precision = column.getPrecision();
					scale = column.getScale();
				}
			}
			else {
				discriminatorColumnExpression = discriminatorFormulaTemplate;
				length = null;
				arrayLength = null;
				precision = null;
				scale = null;
			}
			return new ExplicitColumnDiscriminatorMappingImpl(
					this,
					castNonNull( discriminatorColumnExpression ),
					getTableName(),
					castNonNull( discriminatorColumnExpression ),
					discriminatorFormulaTemplate != null,
					isPhysicalDiscriminator(),
					false,
					null,
					length,
					arrayLength,
					precision,
					scale,
					castNonNull( getDiscriminatorDomainType() )
			);
		}
	}

	@Nullable
	@Override
	public abstract BasicType<?> getDiscriminatorType();

	@Nullable
	protected EntityVersionMapping generateVersionMapping(
			@Nullable Supplier<?> templateInstanceCreator,
			@Nonnull PersistentClass bootEntityDescriptor,
			@Nonnull MappingModelCreationProcess creationProcess) {
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
	public void linkWithSuperType(@Nonnull MappingModelCreationProcess creationProcess) {
		if ( getMappedSuperclass() != null ) {
			superMappingType = creationProcess.getEntityPersister( getMappedSuperclass() );
			final var inFlightEntityMappingType = (InFlightEntityMappingType) superMappingType;
			inFlightEntityMappingType.linkWithSubType(this, creationProcess);
			if ( subclassMappingTypes != null ) {
				castNonNull( subclassMappingTypes ).values()
						.forEach( sub -> inFlightEntityMappingType.linkWithSubType(sub, creationProcess) );
			}
		}
	}

	@Override
	public void linkWithSubType(@Nonnull EntityMappingType sub, @Nonnull MappingModelCreationProcess creationProcess) {
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

	@Nonnull
	@Override
	public AttributeMapping getAttributeMapping(int position) {
		return attributeMappings.get( position );
	}

	@Nonnull
	@Override
	public int[] getImmutablePropertyIndexes() {
		return immutablePropertyIndexes;
	}

	@Override
	public int getNumberOfDeclaredAttributeMappings() {
		return declaredAttributeMappings.size();
	}

	@Nonnull
	@Override
	public AttributeMappingsMap getDeclaredAttributeMappings() {
		return declaredAttributeMappings;
	}

	@Override
	public void visitDeclaredAttributeMappings(@Nonnull Consumer<? super AttributeMapping> action) {
		declaredAttributeMappings.forEachValue( action );
	}

	@Nullable
	@Override
	public EntityPersister getSuperMappingType() {
		return superMappingType;
	}

	@Nonnull
	@Override
	public Collection<EntityMappingType> getSubMappingTypes() {
		return subclassMappingTypes == null ? emptyList() : subclassMappingTypes.values();
	}

	@Override
	public boolean isTypeOrSuperType(@Nullable EntityMappingType targetType) {
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


	@Nonnull
	protected EntityIdentifierMapping generateIdentifierMapping(
			@Nullable Supplier<?> templateInstanceCreator,
			@Nonnull PersistentClass bootEntityDescriptor,
			@Nonnull MappingModelCreationProcess creationProcess) {
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
		final Long length;
		final Integer arrayLength;
		final Integer precision;
		final Integer scale;
		final var identifier = bootEntityDescriptor.getIdentifier();
		if ( identifier == null ) {
			length = null;
			arrayLength = null;
			precision = null;
			scale = null;
		}
		else {
			final var column = identifier.getColumns().get( 0 );
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

	@Nonnull
	protected EntityIdentifierMapping generateNonEncapsulatedCompositeIdentifierMapping(
			@Nonnull MappingModelCreationProcess creationProcess,
			@Nonnull PersistentClass bootEntityDescriptor) {
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
	@Nonnull
	protected static EntityVersionMapping generateVersionMapping(
			@Nonnull AbstractEntityPersister entityPersister,
			@Nullable Supplier<?> templateInstanceCreator,
			@Nonnull PersistentClass bootModelRootEntityDescriptor,
			@Nonnull MappingModelCreationProcess creationProcess) {
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
				column.getLength(),
				column.getArrayLength(),
				column.getPrecision(),
				column.getScale(),
				column.getTemporalPrecision(),
				basicTypeResolution.getLegacyResolvedBasicType(),
				entityPersister
		);
	}

	@Nonnull
	protected AttributeMapping generateNonIdAttributeMapping(
			@Nonnull Property bootProperty,
			int stateArrayPosition,
			int fetchableIndex,
			@Nonnull MappingModelCreationProcess creationProcess) {
		final var type = bootProperty.getType();
		final int propertyIndex = getPropertyIndex( bootProperty.getName() );
		return generateNonIdAttributeMapping(
				bootProperty.getName(),
				type,
				bootProperty.getCascadeStyle(),
				propertyIndex,
				getTableName( getPropertyTableNumbers()[propertyIndex] ),
				type instanceof BasicType<?> && bootProperty.getSelectables().get( 0 ).isFormula()
						? propertyColumnFormulaTemplates[ propertyIndex ]
						: getPropertyColumnNames( propertyIndex ),
				bootProperty,
				stateArrayPosition,
				fetchableIndex,
				creationProcess
		);
	}

	@Nonnull
	protected AttributeMapping generateNonIdAttributeMapping(
			@Nonnull String attrName,
			@Nonnull Type attrType,
			@Nonnull CascadeStyle cascadeStyle,
			int propertyIndex,
			@Nonnull String tableExpression,
			@Nonnull String[] attrColumnNames,
			@Nonnull Property bootProperty,
			int stateArrayPosition,
			int fetchableIndex,
			@Nonnull MappingModelCreationProcess creationProcess) {
		final var creationContext = creationProcess.getCreationContext();

		final var propertyAccess = getRepresentationStrategy().resolvePropertyAccess( bootProperty );

		final var value = bootProperty.getValue();
		if ( propertyIndex == getVersionPropertyIndex() ) {
			final var column = value.getColumns().get( 0 );
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
					column.getLength(),
					column.getArrayLength(),
					column.getPrecision(),
					column.getScale(),
					column.getTemporalPrecision(),
					column.isSqlTypeLob( creationProcess.getCreationContext().getMetadata() ),
					column.isNullable(),
					value.isColumnInsertable( 0 ),
					value.isColumnUpdateable( 0 ),
					propertyAccess
			);
		}

		if ( attrType instanceof BasicType ) {
			final var role = getNavigableRole().append( bootProperty.getName() );
			final String attrColumnExpression;
			final boolean isAttrColumnExpressionFormula;
			final String customReadExpr;
			final String customWriteExpr;
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

				if ( !value.getSelectables().get( 0 ).isFormula() ) {
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
					attrColumnExpression = attrColumnNames[ 0 ];
					isAttrColumnExpressionFormula = true;
					customReadExpr = null;
					customWriteExpr = null;
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
					length,
					arrayLength,
					precision,
					scale,
					temporalPrecision,
					isLob,
					nullable,
					value.isColumnInsertable( 0 ),
					value.isColumnUpdateable( 0 ),
					propertyAccess
			);
		}
		else if ( attrType instanceof AnyType anyType ) {

			final var attributeMetadataAccess = new SimpleAttributeMetadata(
					propertyAccess,
					new DiscriminatedAssociationAttributeMapping.MutabilityPlanImpl( anyType ),
					bootProperty.isOptional(),
					bootProperty.isInsertable(),
					bootProperty.isUpdatable(),
					bootProperty.isOptimisticLocked(),
					bootProperty.isSelectable()
			);

			return new DiscriminatedAssociationAttributeMapping(
					navigableRole.append( bootProperty.getName() ),
					creationContext.getTypeConfiguration().getJavaTypeRegistry()
							.resolveDescriptor( Object.class ),
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
		else if ( attrType instanceof CompositeType compositeType ) {
			return buildEmbeddedAttributeMapping(
					attrName,
					stateArrayPosition,
					fetchableIndex,
					bootProperty,
					bootProperty.getValue() instanceof DependantValue depValue ? depValue : null,
					0,
					this,
					compositeType,
					tableExpression,
					null,
					propertyAccess,
					cascadeStyle,
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
					cascadeStyle,
					getFetchStyle( stateArrayPosition ),
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
					cascadeStyle,
					creationProcess
			);
		}

		throw new UnsupportedMappingException( "Unsupported type for attribute '"
				+ getEntityName() + "." + attrName + "': " + attrType );
	}

	/**
	 * For Hibernate Reactive
	 */
	@Nonnull
	protected EmbeddedAttributeMapping buildEmbeddedAttributeMapping(
			@Nonnull String attrName,
			int stateArrayPosition,
			int fetchableIndex,
			@Nonnull Property bootProperty,
			@Nonnull DependantValue dependantValue,
			int dependantColumnIndex,
			@Nonnull ManagedMappingType declaringType,
			@Nonnull CompositeType attrType,
			@Nonnull String tableExpression,
			@Nullable String[] rootTableKeyColumnNames,
			@Nonnull PropertyAccess propertyAccess,
			@Nonnull CascadeStyle cascadeStyle,
			@Nonnull MappingModelCreationProcess creationProcess) {
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
	@Nonnull
	protected AttributeMapping buildSingularAssociationAttributeMapping(
			@Nonnull String attrName,
			@Nonnull NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			@Nonnull Property bootProperty,
			@Nonnull ManagedMappingType declaringType,
			@Nonnull EntityPersister declaringEntityPersister,
			@Nonnull EntityType attrType,
			@Nonnull PropertyAccess propertyAccess,
			@Nonnull CascadeStyle cascadeStyle,
			@Nonnull MappingModelCreationProcess creationProcess) {
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
	@Nonnull
	protected AttributeMapping buildPluralAttributeMapping(
			@Nonnull String attrName,
			int stateArrayPosition,
			int fetchableIndex,
			@Nonnull Property bootProperty,
			@Nonnull ManagedMappingType declaringType,
			@Nonnull PropertyAccess propertyAccess,
			@Nonnull CascadeStyle cascadeStyle,
			@Nonnull FetchStyle fetchStyle,
			@Nonnull MappingModelCreationProcess creationProcess) {
		return MappingModelCreationHelper.buildPluralAttributeMapping(
				attrName,
				stateArrayPosition,
				fetchableIndex,
				bootProperty,
				declaringType,
				propertyAccess,
				cascadeStyle,
				fetchStyle,
				creationProcess
		);
	}

	@Nonnull
	@Override
	public JavaType<?> getMappedJavaType() {
		return javaType;
	}

	@Nonnull
	@Override
	public EntityPersister getEntityPersister() {
		return this;
	}

	@Nonnull
	@Override
	public EntityIdentifierMapping getIdentifierMapping() {
		return identifierMapping;
	}

	@Nullable
	@Override
	public EntityVersionMapping getVersionMapping() {
		return versionMapping;
	}

	@Nullable
	@Override
	public TenantIdMapping getTenantIdMapping() {
		return tenantIdMapping;
	}

	@Nullable
	@Override
	public TenantIdLoader getTenantIdLoader() {
		return tenantIdLoader;
	}

	@Nullable
	@Override
	public EntityRowIdMapping getRowIdMapping() {
		return rowIdMapping;
	}

	@Nullable
	@Override
	public EntityDiscriminatorMapping getDiscriminatorMapping() {
		return discriminatorMapping;
	}

	@Nullable
	@Override
	public SoftDeleteMapping getSoftDeleteMapping() {
		return auxiliaryMapping instanceof SoftDeleteMapping softDeleteMapping
				? softDeleteMapping : null;
	}

	@Nullable
	@Override
	public TemporalMapping getTemporalMapping() {
		return auxiliaryMapping instanceof TemporalMapping temporalMapping
				? temporalMapping : null;
	}

	@Nullable
	@Override
	public AuditMapping getAuditMapping() {
		return auxiliaryMapping instanceof AuditMapping auditMapping
				? auditMapping : null;
	}

	@Nullable
	@Override
	public AuxiliaryMapping getAuxiliaryMapping() {
		return auxiliaryMapping;
	}

	@Nonnull
	@Override
	public AttributeMappingsList getAttributeMappings() {
		if ( attributeMappings == null ) {
			initAttributeMappings();
		}
		return attributeMappings;
	}

	private void initAttributeMappings() {
		final int sizeHint =
				declaredAttributeMappings.size()
				+ (superMappingType == null ? 0 : superMappingType.getAttributeMappings().size() );
		final var builder = new ImmutableAttributeMappingList.Builder( sizeHint );
		if ( superMappingType != null ) {
			superMappingType.forEachAttributeMapping( builder::add );
		}
		for ( var attributeMapping : declaredAttributeMappings.valueIterator() ) {
			builder.add( attributeMapping );
		}
		attributeMappings = builder.build();

		final int size = attributeMappings.size();
		accessorCache = new PropertyValueAccessor[size];
		for ( int i = 0; i < size; i++ ) {
			final var propertyAccess = attributeMappings.get( i ).getAttributeMetadata().getPropertyAccess();
			accessorCache[i] = propertyAccess.getPropertyValueAccessor();
		}
		// subclasses?  it depends on the usage
	}

	@Nullable
	@Override
	public AttributeMapping findDeclaredAttributeMapping(@Nonnull String name) {
		return declaredAttributeMappings.get( name );
	}

	@Nullable
	@Override
	public AttributeMapping findAttributeMapping(@Nonnull String name) {
		final var declaredAttribute = declaredAttributeMappings.get( name );
		if ( declaredAttribute != null ) {
			return declaredAttribute;
		}
		else if ( superMappingType != null ) {
			return superMappingType.findAttributeMapping( name );
		}
		else {
			return null;
		}
	}

	@Nullable
	@Override
	public ModelPart findSubPart(@Nonnull String name, @Nullable EntityMappingType treatTargetType) {

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
				var superType = treatTargetType.getSuperMappingType();
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

	@Nullable
	private ModelPart findSubPartInSubclassMappings(@Nonnull String name) {
		final var declaredGenericAttribute = declaredGenericAttributeMappings.get( name );
		if ( declaredGenericAttribute != null ) {
			return declaredGenericAttribute;
		}

		ModelPart attribute = null;
		if ( isNotEmpty( subclassMappingTypes ) ) {
			for ( var subMappingType : castNonNull( subclassMappingTypes ).values() ) {
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

	@Nullable
	@Override
	public ModelPart findSubTypesSubPart(@Nonnull String name, @Nullable EntityMappingType treatTargetType) {
		final var declaredAttribute = declaredAttributeMappings.get( name );
		return declaredAttribute != null
				? declaredAttribute
				: findSubPartInSubclassMappings( name );
	}

	@Nullable
	private ModelPart getIdentifierModelPart(@Nonnull String name, @Nullable EntityMappingType treatTargetType) {
		final var identifierMapping = getIdentifierMappingForJoin();
		if ( identifierMapping instanceof final NonAggregatedIdentifierMapping mapping ) {
			final var subPart = mapping.findSubPart( name, treatTargetType );
			if ( subPart != null ) {
				return subPart;
			}
		}
		return isIdentifierReference( name ) ? identifierMapping : null;
	}

	private boolean isIdentifierReference(@Nonnull String name) {
		return EntityIdentifierMapping.ID_ROLE_NAME.equals( name )
			|| hasIdentifierProperty() && name.equals( getIdentifierPropertyName() )
			|| !hasNonIdentifierPropertyNamedId() && "id".equals( name );
	}

	@Override
	public void visitSubParts(
			@Nonnull Consumer<ModelPart> consumer,
			@Nullable EntityMappingType treatTargetType) {
		consumer.accept( identifierMapping );
		declaredAttributeMappings.forEachValue( consumer );
	}

	@Override
	public void visitKeyFetchables(@Nonnull Consumer<? super Fetchable> fetchableConsumer, @Nullable EntityMappingType treatTargetType) {
		// No-op
	}

	@Override
	public void visitKeyFetchables(@Nonnull IndexedConsumer<? super Fetchable> fetchableConsumer, @Nullable EntityMappingType treatTargetType) {
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

	@Nonnull
	@Override
	public Fetchable getKeyFetchable(int position) {
		throw new IndexOutOfBoundsException( position );
	}

	@Nonnull
	@Override
	public AttributeMapping getFetchable(int position) {
		return getStaticFetchableList().get( position );
	}

	@Override
	public void visitFetchables(@Nonnull Consumer<? super Fetchable> fetchableConsumer, @Nullable EntityMappingType treatTargetType) {
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
	public void visitFetchables(@Nonnull IndexedConsumer<? super Fetchable> fetchableConsumer, @Nullable EntityMappingType treatTargetType) {
		if ( treatTargetType == null ) {
			getStaticFetchableList().indexedForEach( fetchableConsumer );
		}
		else {
			attributeMappings.indexedForEach( fetchableConsumer );
			if ( treatTargetType.isTypeOrSuperType( this ) ) {
				if ( subclassMappingTypes != null ) {
					int offset = attributeMappings.size();
					for ( var subtype : subclassMappingTypes.values() ) {
						for ( var declaredAttributeMapping :
								subtype.getDeclaredAttributeMappings().valueIterator() ) {
							fetchableConsumer.accept( offset++, declaredAttributeMapping );
						}
					}
				}
			}
		}
	}

	@Nonnull
	protected AttributeMappingsList getStaticFetchableList() {
		return staticFetchableList;
	}

	@Override
	public void visitAttributeMappings(@Nonnull Consumer<? super AttributeMapping> action) {
		attributeMappings.forEach( action );
	}

	@Override
	public void visitSuperTypeAttributeMappings(@Nonnull Consumer<? super AttributeMapping> action) {
		if ( superMappingType != null ) {
			superMappingType.visitSuperTypeAttributeMappings( action );
		}
	}

	@Override
	public int forEachSelectable(int offset, @Nonnull SelectableConsumer selectableConsumer) {
		int span = 0;
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			span += attributeMappings.get( i )
					.forEachSelectable( span + offset, selectableConsumer );
		}
		return span;
	}

	@Override
	public void visitSubTypeAttributeMappings(@Nonnull Consumer<? super AttributeMapping> action) {
		forEachAttributeMapping( action );
		if ( subclassMappingTypes != null ) {
			for ( var subType : subclassMappingTypes.values() ) {
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
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		return getIdentifierMapping().forEachJdbcType( offset, action );
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		else {
			final var identifierMapping = getIdentifierMapping();
			final Object identifier = identifierMapping.getIdentifier( value );
			return identifierMapping.disassemble( identifier, session );
		}
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return getIdentifierMapping()
				.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> consumer,
			@Nullable SharedSessionContractImplementor session) {
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
	// State related to this we handle differently in 6+.  In other words, state
	// that is no longer needed
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Deprecated private final String[] subclassColumnAliasClosure;
	@Deprecated private final String[] subclassFormulaAliasClosure;
	@Deprecated private final String[][] subclassPropertyColumnAliasClosure;

	/**
	 * @deprecated Hibernate no longer uses aliases to read from result sets
	 */
	@Nonnull
	@Deprecated
	@Override
	public String[] getSubclassPropertyColumnAliases(int i, @Nonnull String suffix) {
		return new Alias( suffix ).toUnquotedAliasStrings( subclassPropertyColumnAliasClosure[i] );
	}

	/**
	 * @deprecated Hibernate no longer uses aliases to read from result sets
	 */
	@Nullable
	@Deprecated	@Override
	public String[] getSubclassPropertyColumnAliases(@Nonnull String propertyName, @Nonnull String suffix) {
		final var rawAliases = resolveSubclassPropertyColumnAliases( propertyName );
		return rawAliases == null
				? null
				: new Alias( suffix ).toUnquotedAliasStrings( rawAliases );
	}

	@Nullable
	private String[] resolveSubclassPropertyColumnAliases(@Nonnull String propertyName) {
		if ( ENTITY_CLASS.equals( propertyName ) && isPolymorphic() ) {
			return new String[] { getDiscriminatorAlias() };
		}

		final var identifierAliases = resolveIdentifierPropertyColumnAliases( propertyName );
		if ( identifierAliases != null ) {
			return identifierAliases;
		}

		final int propertyIndex = getSubclassPropertyIndex( propertyName );
		if ( propertyIndex >= 0 ) {
			return subclassPropertyColumnAliasClosure[propertyIndex];
		}

		final int dotIndex = propertyName.indexOf( '.' );
		if ( dotIndex > 0 ) {
			final int basePropertyIndex = getSubclassPropertyIndex( propertyName.substring( 0, dotIndex ) );
			if ( basePropertyIndex >= 0 ) {
				return resolveSubPropertyColumnAliases(
						subclassPropertyTypeClosure[basePropertyIndex],
						subclassPropertyColumnAliasClosure[basePropertyIndex],
						propertyName.substring( dotIndex + 1 )
				);
			}
		}
		return null;
	}

	@Nullable
	private String[] resolveIdentifierPropertyColumnAliases(@Nonnull String propertyName) {
		final var identifierAliases = getIdentifierAliases();
		if ( !hasNonIdentifierPropertyNamedId() ) {
			if ( ENTITY_ID.equals( propertyName ) ) {
				return identifierAliases;
			}
			final String entityIdPathPrefix = ENTITY_ID + ".";
			if ( propertyName.startsWith( entityIdPathPrefix ) ) {
				return resolveSubPropertyColumnAliases(
						getIdentifierType(),
						identifierAliases,
						propertyName.substring( entityIdPathPrefix.length() )
				);
			}
		}

		if ( hasIdentifierProperty() ) {
			final String identifierPropertyName = getIdentifierPropertyName();
			if ( propertyName.equals( identifierPropertyName ) ) {
				return identifierAliases;
			}
			final String identifierPropertyPathPrefix = identifierPropertyName + ".";
			if ( propertyName.startsWith( identifierPropertyPathPrefix ) ) {
				return resolveSubPropertyColumnAliases(
						getIdentifierType(),
						identifierAliases,
						propertyName.substring( identifierPropertyPathPrefix.length() )
				);
			}
			else {
				return null;
			}
		}
		else if ( getIdentifierType() instanceof ComponentType ) {
			return resolveSubPropertyColumnAliases( getIdentifierType(), identifierAliases, propertyName );
		}
		else {
			return null;
		}
	}

	@Nullable
	private String[] resolveSubPropertyColumnAliases(@Nonnull Type type, @Nonnull String[] columnAliases, @Nonnull String propertyName) {
		return type instanceof CompositeType compositeType
				? resolveCompositePropertyColumnAliases( compositeType, columnAliases, propertyName )
				: null;
	}

	@Nullable
	private String[] resolveCompositePropertyColumnAliases(
			@Nonnull CompositeType compositeType,
			@Nonnull String[] columnAliases,
			@Nonnull String propertyName) {
		final int dotIndex = propertyName.indexOf( '.' );
		final String componentPropertyName =
				dotIndex < 0
						? propertyName
						: propertyName.substring( 0, dotIndex );
		final int componentPropertyIndex =
				getCompositePropertyIndex( compositeType, componentPropertyName );
		if ( componentPropertyIndex < 0 ) {
			return null;
		}

		final var propertyColumnAliases =
				getCompositePropertySelectableValues( compositeType, columnAliases, componentPropertyIndex );
		return dotIndex < 0
				? propertyColumnAliases
				: resolveSubPropertyColumnAliases( compositeType.getSubtypes()[componentPropertyIndex],
						propertyColumnAliases,
						propertyName.substring( dotIndex + 1 ) );
	}

	@Nullable
	public String getDiscriminatorAlias() {
		return DISCRIMINATOR_ALIAS;
	}

	@Nullable
	protected String getSqlWhereStringTableExpression(){
		return sqlWhereStringTableExpression;
	}

	@Override
	public boolean managesColumns(@Nonnull String[] columnNames) {
		for ( String columnName : columnNames ) {
			if ( !writesToColumn( columnName ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean writesToColumn(@Nonnull String columnName) {
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
