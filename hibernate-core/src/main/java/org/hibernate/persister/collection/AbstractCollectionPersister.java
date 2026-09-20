package org.hibernate.persister.collection;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.TransientObjectException;
import org.hibernate.action.queue.spi.meta.CollectionTableDescriptor;
import org.hibernate.action.queue.spi.meta.ColumnDescriptor;
import org.hibernate.action.queue.spi.meta.TableKeyDescriptor;
import org.hibernate.action.queue.internal.support.GraphBasedActionQueueFactory;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cache.spi.entry.StructuredCollectionCacheEntry;
import org.hibernate.cache.spi.entry.StructuredMapCacheEntry;
import org.hibernate.cache.spi.entry.UnstructuredCacheEntry;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.profile.internal.FetchProfileAffectee;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.jdbc.Expectation;
import org.hibernate.loader.ast.internal.CollectionElementLoaderByIndex;
import org.hibernate.loader.ast.internal.CollectionLoaderNamedQuery;
import org.hibernate.loader.ast.internal.CollectionLoaderSingleKey;
import org.hibernate.loader.ast.internal.CollectionLoaderSubSelectFetch;
import org.hibernate.loader.ast.internal.LoaderSqlAstCreationState;
import org.hibernate.loader.ast.spi.BatchLoaderFactory;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedCollectionPart;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.InFlightCollectionMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.PluralAttributeMappingImpl;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.persister.collection.mutation.CollectionTableMapping;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.RowMutationOperations;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.persister.filter.internal.FilterHelper;
import org.hibernate.sql.RestrictionRendering;
import org.hibernate.sql.ast.spi.query.predicate.SqlFragmentPredicate;
import org.hibernate.query.named.spi.NamedQueryMemento;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.Alias;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.Template;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.creation.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.expression.AliasedExpression;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping.MutationDetails;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameterList;
import org.hibernate.sql.ast.spi.model.ColumnWriteFragment;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.RestrictedTableMutation;
import org.hibernate.sql.ast.spi.model.TableDeleteStandard;
import org.hibernate.sql.spi.mutation.jdbc.JdbcDeleteMutation;
import org.hibernate.sql.spi.mutation.jdbc.JdbcMutationOperation;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.StringHelper.getNonEmptyOrConjunctionIfBothNonEmpty;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.replace;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.jdbc.Expectations.createExpectation;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.getTableIdentifierExpression;
import static org.hibernate.pretty.MessageHelper.collectionInfoString;
import static org.hibernate.sql.Template.renderWhereStringTemplate;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.temporal.TemporalTableStrategy.HISTORY_TABLE;

/**
 * Base implementation of the {@code QueryableCollection} interface.
 *
 * @author Gavin King
 *
 * @see BasicCollectionPersister
 * @see OneToManyPersister
 */
@Internal
public abstract class AbstractCollectionPersister
		implements CollectionPersister, InFlightCollectionMapping, CollectionMutationTarget,
		PluralAttributeMappingImpl.Aware, FetchProfileAffectee, Joinable {

	private final NavigableRole navigableRole;
	private final CollectionSemantics<?,?> collectionSemantics;
	private final EntityPersister ownerPersister;
	private final SessionFactoryImplementor factory;

	protected final String qualifiedTableName;
	private final CollectionTableMapping tableMapping;

	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private String sqlSelectSizeString;
	@Nullable
	private String sqlDetectRowByIndexString;
	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private String sqlDetectRowByElementString;

	protected boolean hasWhere;
	@Nullable
	protected String sqlWhereString;
	@Nullable
	private String sqlWhereStringTemplate;
	private volatile RestrictionRendering sqlWhereRendering;
	private volatile RestrictionRendering manyToManyWhereRendering;

	private final boolean hasOrder;
	private final boolean hasManyToManyOrder;

	@Nullable
	private final String mappedByProperty;

	protected final boolean indexContainsFormula;
	protected final boolean elementIsPureFormula;

	// columns
	protected final String[] keyColumnNames;
	protected final String[] keyFormulaTemplates;
	protected final String[] keyFormulas;
	@Nullable
	protected final String[] indexColumnNames;
	@Nullable
	protected final String[] indexFormulaTemplates;
	@Nullable
	protected final String[] indexFormulas;
	@Nullable
	protected final boolean[] indexColumnIsGettable;
	@Nullable
	protected final boolean[] indexColumnIsSettable;
	protected final String[] elementColumnNames;
	protected final String[] elementColumnWriters;
	protected final String[] elementColumnReaders;
	protected final String[] elementColumnReaderTemplates;
	protected final String[] elementFormulaTemplates;
	protected final String[] elementFormulas;
	protected final boolean[] elementColumnIsGettable;
	protected final boolean[] elementColumnIsSettable;

	@Nullable
	protected final String identifierColumnName;

	@Nullable
	private final String queryLoaderName;

	private final boolean isPrimitiveArray;
	private final boolean isLazy;
	private final boolean isExtraLazy;
	protected final boolean isInverse;
	private final boolean keyIsUpdateable;
	private final boolean isMutable;
	private final boolean isVersioned;
	protected final int batchSize;
	private final boolean hasOrphanDelete;
	private final boolean subselectLoadable;

	private final boolean cascadeDeleteEnabled;

	// extra information about the element type
	@Nullable
	private final Class<?> elementClass;

	private final Dialect dialect;
	protected final SqlExceptionHelper sqlExceptionHelper;
	@Nullable
	private final BeforeExecutionGenerator identifierGenerator;
	@Nullable
	private final EntityPersister elementPersister;
	private final @Nullable CollectionDataAccess cacheAccessStrategy;
	private final @Nonnull CacheEntryStructure cacheEntryStructure;
	private final boolean useShallowQueryCacheLayout;

	// dynamic filters for the collection
	@Nullable
	private final FilterHelper filterHelper;

	// dynamic filters specifically for many-to-many inside the collection
	@Nullable
	private final FilterHelper manyToManyFilterHelper;

	@Nullable
	private final String manyToManyWhereString;
	@Nullable
	private final String manyToManyWhereTemplate;

	private final String[] spaces;

	@Nullable
	private final Comparator<?> comparator;

	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private CollectionLoader collectionLoader;
	@Nullable
	private CollectionElementLoaderByIndex collectionElementLoaderByIndex;

	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private PluralAttributeMapping attributeMapping;
	@Nullable
	private volatile Set<String> affectingFetchProfiles;

	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private CollectionTableDescriptor collectionTableDescriptor;

	public AbstractCollectionPersister(
			@Nonnull Collection collectionBootDescriptor,
			@Nullable CollectionDataAccess cacheAccessStrategy,
			@Nonnull RuntimeModelCreationContext creationContext)
					throws MappingException, CacheException {
		factory = creationContext.getSessionFactory();
		final var factoryOptions = creationContext.getSessionFactoryOptions();

		collectionSemantics =
				creationContext.getBootstrapContext().getMetadataBuildingOptions()
						.getPersistentCollectionRepresentationResolver()
						.resolveRepresentation( collectionBootDescriptor );

		this.cacheAccessStrategy = cacheAccessStrategy;
		cacheEntryStructure =
				cacheEntryStructure( collectionBootDescriptor, factoryOptions );
		useShallowQueryCacheLayout =
				shouldUseShallowCacheLayout( collectionBootDescriptor.getQueryCacheLayout(), factoryOptions );

		dialect = creationContext.getDialect();
		sqlExceptionHelper = creationContext.getJdbcServices().getSqlExceptionHelper();
		collectionType = collectionBootDescriptor.getCollectionType();
		navigableRole = new NavigableRole( collectionBootDescriptor.getRole() );
		ownerPersister =
				creationContext.getDomainModel()
						.getEntityDescriptor( collectionBootDescriptor.getOwnerEntityName() );
		queryLoaderName = collectionBootDescriptor.getLoaderName();
		isMutable = collectionBootDescriptor.isMutable();
		mappedByProperty = collectionBootDescriptor.getMappedByProperty();

		final var elementBootDescriptor = collectionBootDescriptor.getElement();
		final var table = collectionBootDescriptor.getCollectionTable();

		elementType = elementBootDescriptor.getType();
		// isSet = collectionBinding.isSet();
		// isSorted = collectionBinding.isSorted();
		isPrimitiveArray = collectionBootDescriptor.isPrimitiveArray();
		subselectLoadable = collectionBootDescriptor.isSubselectLoadable();

		qualifiedTableName = determineTableName( table );

		final var synchronizedTables = collectionBootDescriptor.getSynchronizedTables();
		final int spacesSize = 1 + synchronizedTables.size();
		spaces = new String[spacesSize];
		final var tables = synchronizedTables.iterator();
		for ( int i = 1; i < spacesSize; i++ ) {
			spaces[i] = tables.next();
		}

		hasOrphanDelete = collectionBootDescriptor.hasOrphanDelete();

		batchSize = batchSize( collectionBootDescriptor, factoryOptions );

		isVersioned = collectionBootDescriptor.isOptimisticLocked();

		final var typeConfiguration = creationContext.getTypeConfiguration();

		// KEY

		final var key = collectionBootDescriptor.getKey();

		keyType = key.getType();
		final int keySpan = key.getColumnSpan();
		keyColumnNames = new String[keySpan];
		keyFormulaTemplates = new String[keySpan];
		keyFormulas = new String[keySpan];
		keyColumnAliases = new String[keySpan];
		int k = 0;
		for ( var selectable: key.getSelectables() ) {
			// NativeSQL: collect key column and auto-aliases
			keyColumnAliases[k] = selectable.getAlias( dialect, table );
			if ( selectable instanceof Formula formula ) {
				keyFormulaTemplates[k] = formula.getTemplate( dialect, typeConfiguration );
				keyFormulas[k] = formula.getFormula();
			}
			else if ( selectable instanceof Column column ) {
				keyColumnNames[k] = column.getQuotedName( dialect );
			}
			k++;
		}

		// unquotedKeyColumnNames = StringHelper.unQuote(keyColumnAliases);

		// ELEMENT

		elementPersister =
				elementType instanceof EntityType entityType
						// NativeSQL: collect element column and auto-aliases
						? creationContext.getDomainModel()
								.getEntityDescriptor( entityType.getAssociatedEntityName() )
						: null;
		// Defer this after the element persister was determined,
		// because it's needed in OneToManyPersister.getTableName()
		spaces[0] = getTableName();

		final int elementSpan = elementBootDescriptor.getColumnSpan();
		elementColumnAliases = new String[elementSpan];
		elementColumnNames = new String[elementSpan];
		elementColumnWriters = new String[elementSpan];
		elementColumnReaders = new String[elementSpan];
		elementColumnReaderTemplates = new String[elementSpan];
		elementFormulaTemplates = new String[elementSpan];
		elementFormulas = new String[elementSpan];
		elementColumnIsSettable = new boolean[elementSpan];
		elementColumnIsGettable = new boolean[elementSpan];
		boolean isPureFormula = true;
		final boolean oneToMany = collectionBootDescriptor.isOneToMany();
		final boolean[] columnInsertability = oneToMany ? null : elementBootDescriptor.getColumnInsertability();
		int j = 0;
		for ( var selectable: elementBootDescriptor.getSelectables() ) {
			elementColumnAliases[j] = selectable.getAlias( dialect, table );
			if ( selectable instanceof Formula formula ) {
				elementFormulaTemplates[j] = formula.getTemplate( dialect, typeConfiguration );
				elementFormulas[j] = formula.getFormula();
			}
			else if ( selectable instanceof Column column ) {
				elementColumnNames[j] = column.getQuotedName( dialect );
				elementColumnWriters[j] = column.getWriteExpr(
						elementBootDescriptor.getSelectableType( factory.getRuntimeMetamodels(), j ),
						dialect,
						creationContext.getBootModel()
				);
				elementColumnReaders[j] = column.getReadExpr( dialect );
				elementColumnReaderTemplates[j] = column.getTemplate( dialect, typeConfiguration );
				elementColumnIsGettable[j] = true;
				if ( elementType instanceof ComponentType || elementType instanceof AnyType ) {
					// Implements desired behavior specifically for @ElementCollection mappings.
					elementColumnIsSettable[j] = columnInsertability[j];
				}
				else {
					// Preserves legacy non-@ElementCollection behavior
					elementColumnIsSettable[j] = true;
				}
				isPureFormula = false;
			}
			j++;
		}
		elementIsPureFormula = isPureFormula;

		// INDEX AND ROW SELECT

		if ( collectionBootDescriptor instanceof IndexedCollection indexedCollection ) {
			assert collectionBootDescriptor.isIndexed();
			// NativeSQL: collect index column and auto-aliases
			final var index = indexedCollection.getIndex();
			indexType = index.getType();
			final int indexSpan = index.getColumnSpan();
			final boolean[] indexColumnInsertability = index.getColumnInsertability();
			final boolean[] indexColumnUpdatability = index.getColumnUpdateability();
			indexColumnNames = new String[indexSpan];
			indexFormulaTemplates = new String[indexSpan];
			indexFormulas = new String[indexSpan];
			indexColumnIsGettable = new boolean[indexSpan];
			indexColumnIsSettable = new boolean[indexSpan];
			indexColumnAliases = new String[indexSpan];
			int i = 0;
			boolean hasFormula = false;
			for ( var selectable: index.getSelectables() ) {
				indexColumnAliases[i] = selectable.getAlias( dialect );
				if ( selectable instanceof Formula indexFormula ) {
					indexFormulaTemplates[i] = indexFormula.getTemplate( dialect, typeConfiguration );
					indexFormulas[i] = indexFormula.getFormula();
					hasFormula = true;
				}
				else if ( selectable instanceof Column indexColumn ) {
					if ( indexedCollection.hasMapKeyProperty() ) {
						// If the Map key is set via @MapKey, it should not be written
						// since it is a reference to a field of the associated entity.
						// (Note that the analogous situation for Lists never arises
						// because @OrderBy is treated as defining a sorted bag.)
						indexColumnInsertability[i] = false;
						indexColumnUpdatability[i] = false;
						hasFormula = true; // this is incorrect, but needed for some reason
					}
					indexColumnNames[i] = indexColumn.getQuotedName( dialect );
					indexColumnIsGettable[i] = true;
					indexColumnIsSettable[i] = indexColumnInsertability[i] || indexColumnUpdatability[i];
				}
				i++;
			}
			indexContainsFormula = hasFormula;
		}
		else {
			indexContainsFormula = false;
			indexColumnIsGettable = null;
			indexColumnIsSettable = null;
			indexFormulaTemplates = null;
			indexFormulas = null;
			indexType = null;
			indexColumnNames = null;
			indexColumnAliases = null;
		}

		final boolean hasIdentifier = collectionBootDescriptor.isIdentified();
		if ( hasIdentifier ) {
			if ( collectionBootDescriptor.isOneToMany() ) {
				throw new MappingException( "one-to-many collections with identifiers are not supported" );
			}
			//noinspection ConstantConditions
			final var idCollection = (IdentifierCollection) collectionBootDescriptor;
			identifierType = idCollection.getIdentifier().getType();
			final var idColumn = idCollection.getIdentifier().getColumns().get(0);
			identifierColumnName = idColumn.getQuotedName( dialect );
			identifierColumnAlias = idColumn.getAlias( dialect );
			identifierGenerator = createGenerator( creationContext, idCollection );
		}
		else {
			identifierType = null;
			identifierColumnName = null;
			identifierColumnAlias = null;
			identifierGenerator = null;
		}

		isLazy = collectionBootDescriptor.isLazy();
		isExtraLazy = collectionBootDescriptor.isExtraLazy();

		isInverse = collectionBootDescriptor.isInverse();

		keyIsUpdateable = key.isUpdateable();

		elementClass =
				collectionBootDescriptor instanceof Array arrayDescriptor
						? arrayDescriptor.getElementClass()
						// for non-arrays, we don't need to know the element class
						: null; // elementType.returnedClass();

		hasOrder = collectionBootDescriptor.getOrderBy() != null;
		hasManyToManyOrder = collectionBootDescriptor.getManyToManyOrdering() != null;

		// Handle any filters applied to this collectionBinding
		filterHelper = filterHelper( collectionBootDescriptor, elementPersister, creationContext );
		// Handle any filters applied to this collectionBinding for many-to-many
		manyToManyFilterHelper = manyToManyFilterHelper( collectionBootDescriptor, creationContext );

		final String manyToManyWhere = collectionBootDescriptor.getManyToManyWhere();
		if ( isEmpty( manyToManyWhere ) ) {
			manyToManyWhereString = null;
			manyToManyWhereTemplate = null;
		}
		else {
			manyToManyWhereString = "( " + manyToManyWhere + ")";
			manyToManyWhereTemplate =
					renderWhereStringTemplate( manyToManyWhereString, creationContext.getDialect(), typeConfiguration );
		}

		comparator = collectionBootDescriptor.getComparator();

		initCollectionPropertyMap();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// "mapping model"

		if ( hasNamedQueryLoader() ) {
			getNamedQueryMemento( collectionBootDescriptor.getMetadata() );
		}

		tableMapping = buildCollectionTableMapping( collectionBootDescriptor, getTableName(), getCollectionSpaces() );

		cascadeDeleteEnabled =
				key.isCascadeDeleteEnabled()
				&& creationContext.getDialect().getForeignKeySupport().supportsOnDeleteAction( org.hibernate.annotations.OnDeleteAction.CASCADE );
	}

	@Nullable
	private FilterHelper manyToManyFilterHelper(@Nonnull Collection collection, @Nonnull RuntimeModelCreationContext context) {
		return collection.getManyToManyFilters().isEmpty()
				? null
				: new FilterHelper( collection.getManyToManyFilters(), context.getSessionFactory() );
	}

	@Nullable
	private FilterHelper filterHelper(
			@Nonnull Collection collection, @Nullable EntityPersister elementPersister, @Nonnull RuntimeModelCreationContext context) {
		final var filters = collection.getFilters();
		return filters.isEmpty()
				? null
				: new FilterHelper( filters, entityNameByTableNameMap( elementPersister, context ), factory );
	}

	@Nullable
	private static Map<String, String> entityNameByTableNameMap(
			@Nullable EntityPersister elementPersister, @Nonnull RuntimeModelCreationContext context) {
		return elementPersister == null
				? null
				: AbstractEntityPersister.getEntityNameByTableNameMap(
						context.getBootModel().getEntityBinding( elementPersister.getEntityName() ),
						context.getSessionFactory().getSqlStringGenerationContext()
				);
	}

	@Nullable
	public String getSqlWhereString() {
		return sqlWhereString;
	}

	private static int batchSize(@Nonnull Collection collection, @Nonnull SessionFactoryOptions options) {
		final int batchSize = collection.getBatchSize();
		return batchSize >= 0
				? batchSize
				: options.getDefaultBatchFetchSize();
	}

	@Nonnull
	private static CacheEntryStructure cacheEntryStructure(@Nonnull Collection collection, @Nonnull SessionFactoryOptions options) {
		if ( options.isStructuredCacheEntriesEnabled() ) {
			return collection.isMap()
					? StructuredMapCacheEntry.INSTANCE
					: StructuredCollectionCacheEntry.INSTANCE;
		}
		else {
			return UnstructuredCacheEntry.INSTANCE;
		}
	}

	@Nonnull
	SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return getFactory().getJdbcServices().getDialect().getSqlAstTranslatorFactory();
	}

	@Override
	public void prepareMappingModel(@Nonnull MappingModelCreationProcess creationProcess, @Nonnull Collection bootCollectionDescriptor) {
		final var creationContext = creationProcess.getCreationContext();
		if ( mappedByProperty != null && elementType instanceof EntityType entityType ) {
			final String entityName = entityType.getAssociatedEntityName();
			final var persistentClass =
					creationContext.getBootModel()
							.getEntityBinding( entityName );
			if ( persistentClass.getRecursiveProperty( mappedByProperty ).getValue() instanceof Any ) {
				// we want to delay processing of where-fragment, and therefore affected SQL, until
				// all model parts are ready so that we can access details about the ANY discriminator
				creationProcess.registerInitializationCallback(
						"Where-fragment handling for ANY inverse mapping : " + navigableRole,
						() -> {
							delayedWhereFragmentProcessing(
									creationProcess.getEntityPersister( entityName ),
									mappedByProperty,
									bootCollectionDescriptor,
									creationContext
							);
							buildStaticWhereFragmentSensitiveSql();
							return true;
						}
				);
				return;
			}
		}

		if ( isNotEmpty( bootCollectionDescriptor.getWhere() ) ) {
			hasWhere = true;
			sqlWhereString = "(" + bootCollectionDescriptor.getWhere() + ")";
			sqlWhereStringTemplate =
					renderWhereStringTemplate( sqlWhereString, dialect,
							creationContext.getTypeConfiguration() );
		}
		buildStaticWhereFragmentSensitiveSql();
	}

	private void delayedWhereFragmentProcessing(
			@Nonnull EntityPersister entityPersister,
			@Nonnull String mappedByProperty,
			@Nonnull Collection bootDescriptor,
			@Nonnull RuntimeModelCreationContext creationContext) {
		final String where = getWhere( entityPersister, mappedByProperty, bootDescriptor, creationContext );
		if ( isNotEmpty( where ) ) {
			hasWhere = true;
			sqlWhereString = "(" + where + ")";
			sqlWhereStringTemplate =
					renderWhereStringTemplate( sqlWhereString, dialect,
							creationContext.getTypeConfiguration() );
		}
	}

	@Nullable
	private String getWhere(
			@Nonnull EntityPersister entityPersister,
			@Nonnull String mappedByProperty,
			@Nonnull Collection collectionBootDescriptor,
			@Nonnull RuntimeModelCreationContext creationContext) {
		if ( resolveMappedBy( entityPersister, mappedByProperty )
					instanceof DiscriminatedAssociationAttributeMapping anyMapping ) {
			final var discriminatorMapping = anyMapping.getDiscriminatorMapping();
			final var discriminatorValueDetails =
					discriminatorMapping.getValueConverter()
							.getDetailsForEntityName( ownerPersister.getEntityName() );
			//noinspection unchecked
			final String discriminatorLiteral =
					discriminatorMapping.getUnderlyingJdbcMapping()
							.getJdbcLiteralFormatter().toJdbcLiteral(
									discriminatorValueDetails.getValue(),
									creationContext.getDialect(),
									creationContext.getSessionFactory().getWrapperOptions()
							);
			return getNonEmptyOrConjunctionIfBothNonEmpty( collectionBootDescriptor.getWhere(),
					discriminatorMapping.getSelectableName() + "=" + discriminatorLiteral );
		}
		else {
			return collectionBootDescriptor.getWhere();
		}
	}

	private void buildStaticWhereFragmentSensitiveSql() {
		sqlSelectSizeString = generateSelectSizeString( hasIndex() && !isMap() );
		sqlDetectRowByIndexString = generateDetectRowByIndexString();
		sqlDetectRowByElementString = generateDetectRowByElementString();
	}

	private boolean isMap() {
		return collectionSemantics.getCollectionClassification().toJpaClassification() == PluralAttribute.CollectionType.MAP;
	}

	@Nullable
	private static AttributeMapping resolveMappedBy(@Nonnull EntityPersister entityPersister, @Nonnull String mappedByProperty) {
		final var propertyPathParts = new StringTokenizer( mappedByProperty, ".", false );
		final int tokenCount = propertyPathParts.countTokens();
		assert tokenCount > 0;
		if ( tokenCount == 1 ) {
			return entityPersister.findAttributeMapping( propertyPathParts.nextToken() );
		}
		else {
			ManagedMappingType source = entityPersister;
			while ( propertyPathParts.hasMoreTokens() ) {
				final String partName = propertyPathParts.nextToken();
				final var namedPart = source.findAttributeMapping( partName );
				if ( !propertyPathParts.hasMoreTokens() ) {
					return namedPart;
				}
				source = (ManagedMappingType) namedPart.getPartMappingType();
			}
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"Unable to resolve mapped-by path : (%s) %s",
							entityPersister.getEntityName(),
							mappedByProperty
					)
			);
		}
	}

	@Nonnull
	private BeforeExecutionGenerator createGenerator(@Nonnull RuntimeModelCreationContext context, @Nonnull IdentifierCollection collection) {
		final Generator generator =
				collection.getIdentifier()
						.createGenerator( context.getDialect(), null, null, context.getGeneratorSettings() );
		if ( generator.generatedOnExecution() ) {
			throw new MappingException("must be an BeforeExecutionGenerator"); //TODO fix message
		}
		return (BeforeExecutionGenerator) generator;
	}

	private boolean shouldUseShallowCacheLayout(@Nullable CacheLayout collectionQueryCacheLayout, @Nonnull SessionFactoryOptions options) {
		final var queryCacheLayout =
				collectionQueryCacheLayout == null
						? options.getQueryCacheLayout()
						: collectionQueryCacheLayout;
		return queryCacheLayout == CacheLayout.SHALLOW
			|| queryCacheLayout == CacheLayout.AUTO && cacheAccessStrategy != null;
	}

	@Nonnull
	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Nullable
	@Override
	public Comparator<?> getSortingComparator() {
		return comparator;
	}

	@Nonnull
	protected String determineTableName(@Nonnull Table table) {
		return getTableIdentifierExpression( table, factory );
	}


	@Override
	public void postInstantiate() throws MappingException {
		collectionLoader =
				hasNamedQueryLoader()
						// We pass null as metamodel because we did the initialization during construction already
						? createNamedQueryCollectionLoader( this, getNamedQueryMemento( null ) )
						: createCollectionLoader( new LoadQueryInfluencers( factory ) );

		if ( attributeMapping.getIndexDescriptor() != null ) {
			collectionElementLoaderByIndex =
					new CollectionElementLoaderByIndex( attributeMapping, new LoadQueryInfluencers( factory ), factory );
		}

		// Build collection table descriptor
		// For one-to-many collections, this represents the element entity's table
		// For other collection types, this represents the collection table
		collectionTableDescriptor = buildCollectionTableDescriptor(
				tableMapping,
				attributeMapping,
				factory
		);

		logStaticSQL();
	}

	@Nonnull
	private NamedQueryMemento<?> getNamedQueryMemento(@Nullable MetadataImplementor bootModel) {
		final var memento =
				factory.getQueryEngine().getNamedObjectRepository()
						.resolve( factory, bootModel, queryLoaderName );
		if ( memento == null ) {
			throw new IllegalArgumentException( "Could not resolve named query '" + queryLoaderName
					+ "' for loading collection '" + getRole() + "'" );
		}
		return memento;
	}

	protected void logStaticSQL() {
		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.staticSqlForCollection( getRole() );

			final var rowMutationOperations = getRowMutationOperations();

			final var insertRowOperation = rowMutationOperations.getInsertRowOperation();
			final String insertRowSql = insertRowOperation != null ? insertRowOperation.getSqlString() : null;
			if ( insertRowSql != null ) {
				MODEL_MUTATION_LOGGER.collectionRowInsert( insertRowSql );
			}

			final var updateRowOperation = rowMutationOperations.getUpdateRowOperation();
			final String updateRowSql = updateRowOperation != null ? updateRowOperation.getSqlString() : null;
			if ( updateRowSql != null ) {
				MODEL_MUTATION_LOGGER.collectionRowUpdate( updateRowSql );
			}

			final var deleteRowOperation = rowMutationOperations.getDeleteRowOperation();
			final String deleteRowSql = deleteRowOperation != null ? deleteRowOperation.getSqlString() : null;
			if ( deleteRowSql != null ) {
				MODEL_MUTATION_LOGGER.collectionRowDelete( deleteRowSql );
			}

			final String deleteAllSql = getRemoveCoordinator().getSqlString();
			if ( deleteAllSql != null ) {
				MODEL_MUTATION_LOGGER.collectionOneShotDelete( deleteAllSql );
			}
		}
	}

	@Override
	public void initialize(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) throws HibernateException {
		determineLoaderToUse( key, session ).load( key, session );
	}

	// lazily initialize instance field via 'double-checked locking'
	// see https://en.wikipedia.org/wiki/Double-checked_locking on why 'volatile' and local copy is used
//	protected CollectionLoader getStandardCollectionLoader() {
//		CollectionLoader localCopy = standardCollectionLoader;
//		if ( localCopy == null ) {
//			synchronized (this) {
//				localCopy = standardCollectionLoader;
//				if ( localCopy == null ) {
//					localCopy = createCollectionLoader( new LoadQueryInfluencers( factory ) );
//					standardCollectionLoader  = localCopy;
//				}
//			}
//		}
//		return localCopy;
//	}

	private boolean hasNamedQueryLoader() {
		return queryLoaderName != null;
	}

	@Nonnull
	public CollectionLoader getCollectionLoader() {
		return collectionLoader;
	}

	@Nonnull
	protected CollectionLoader determineLoaderToUse(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		if ( hasNamedQueryLoader() ) {
			// if there is a user-specified loader, return that
			return getCollectionLoader();
		}
		else {
			final var influencers = session.getLoadQueryInfluencers();
			if ( influencers.effectiveSubselectFetchEnabled( this ) ) {
				final var subSelectLoader = resolveSubSelectLoader( key, session );
				if ( subSelectLoader != null ) {
					return subSelectLoader;
				}
			}
			return attributeMapping.isAffectedByInfluencers( influencers, true )
					? createCollectionLoader( influencers )
					: getCollectionLoader();
		}
	}

	@Nullable
	private CollectionLoader resolveSubSelectLoader(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final var subselect =
				persistenceContext.getBatchFetchQueue()
						.getSubselect( session.generateEntityKey( key, getOwnerEntityPersister() ) );
		if ( subselect == null ) {
			return null;
		}
		else {
			// Remove keys of any entities that have been evicted
			subselect.getResultingEntityKeys()
					.removeIf( entityKey -> !persistenceContext.containsEntity( entityKey ) );
			// Run a subquery loader
			return createSubSelectLoader( subselect, session );
		}
	}

	@Nonnull
	protected CollectionLoader createSubSelectLoader(@Nonnull SubselectFetch subselect, @Nonnull SharedSessionContractImplementor session) {
		return new CollectionLoaderSubSelectFetch( attributeMapping, null, subselect, session );
	}
//
//	private CollectionLoader reusableCollectionLoader;
//
//	protected CollectionLoader createCollectionLoader(LoadQueryInfluencers loadQueryInfluencers) {
//		if ( canUseReusableCollectionLoader( loadQueryInfluencers ) ) {
//			if ( reusableCollectionLoader == null ) {
//				reusableCollectionLoader = generateCollectionLoader( new LoadQueryInfluencers( factory ) );
//			}
//			return reusableCollectionLoader;
//		}
//		else {
//			// create a one-off
//			return generateCollectionLoader( loadQueryInfluencers );
//		}
//	}
//
//	private boolean canUseReusableCollectionLoader(LoadQueryInfluencers loadQueryInfluencers) {
//		// we can reuse it so long as none of the enabled influencers affect it
//		return attributeMapping.isNotAffectedByInfluencers( loadQueryInfluencers );
//	}

	@Nonnull
	private CollectionLoader createCollectionLoader(@Nonnull LoadQueryInfluencers loadQueryInfluencers) {
		if ( loadQueryInfluencers.effectivelyBatchLoadable( this ) ) {
			final int batchSize = loadQueryInfluencers.effectiveBatchSize( this );
			return factory.getServiceRegistry()
					.requireService( BatchLoaderFactory.class )
					.createCollectionBatchLoader( batchSize, loadQueryInfluencers, attributeMapping, factory );
		}
		else {
			return createSingleKeyCollectionLoader( loadQueryInfluencers );
		}
	}

	/**
	 * For Hibernate Reactive
	 */
	@Nonnull
	protected CollectionLoader createNamedQueryCollectionLoader(
			@Nonnull CollectionPersister persister, @Nonnull NamedQueryMemento<?> namedQueryMemento) {
		return new CollectionLoaderNamedQuery(persister, namedQueryMemento);
	}

	/**
	 * For Hibernate Reactive
	 */
	@Nonnull
	protected CollectionLoader createSingleKeyCollectionLoader(@Nonnull LoadQueryInfluencers loadQueryInfluencers) {
		return new CollectionLoaderSingleKey( attributeMapping, loadQueryInfluencers, factory );
	}

	@Override
	@Nullable
	public CollectionDataAccess getCacheAccessStrategy() {
		return cacheAccessStrategy;
	}

	@Override
	public boolean hasCache() {
		return cacheAccessStrategy != null;
	}

	@Override
	public boolean useShallowQueryCacheLayout() {
		return useShallowQueryCacheLayout;
	}

	@Nonnull
	public abstract RowMutationOperations getRowMutationOperations();
	@Nonnull
	public abstract RemoveCoordinator getRemoveCoordinator();

	@Override
	public boolean hasOrdering() {
		return hasOrder;
	}

	@Override
	public boolean hasManyToManyOrdering() {
		return isManyToMany() && hasManyToManyOrder;
	}

	/**
	 * Return the element class of an array, or null otherwise.  needed by arrays
	 */
	@Nullable
	@Override
	public Class<?> getElementClass() {
		return elementClass;
	}

	@Nonnull
	public Object incrementIndexByBase(@Nonnull Object index) {
		final int baseIndex = attributeMapping.getIndexMetadata().getListIndexBase();
		return baseIndex > 0 ? baseIndex + (Integer) index : index;
	}

	@Override
	public boolean isPrimitiveArray() {
		return isPrimitiveArray;
	}

	@Override
	public boolean isArray() {
		return collectionSemantics.getCollectionClassification() == CollectionClassification.ARRAY;
	}

	@Nullable
	@Override
	public String getIdentifierColumnName() {
		return hasId() ? identifierColumnName : null;
	}

	/**
	 * Generate a list of collection index, key and element columns
	 */
	@Nonnull
	@Override
	public String selectFragment(@Nonnull String alias, @Nonnull String columnSuffix) {
		final var attributeMapping = getAttributeMapping();
		final var rootQuerySpec = new QuerySpec( true );
		final var sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				LockOptions.NONE,
				(fetchParent, creationState) -> ImmutableFetchList.EMPTY,
				true,
				new LoadQueryInfluencers( factory ),
				factory.getSqlTranslationEngine()
		);

		final var entityPath = new NavigablePath( attributeMapping.getRootPathName() );
		final var rootTableGroup = attributeMapping.createRootTableGroup(
				true,
				entityPath,
				null,
				new SqlAliasBaseConstant( alias ),
				() -> p -> {},
				sqlAstCreationState
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( entityPath, rootTableGroup );

		attributeMapping.createDomainResult( entityPath, rootTableGroup, null, sqlAstCreationState );

		// Wrap expressions with aliases
		final var sqlSelections = rootQuerySpec.getSelectClause().getSqlSelections();
		int i = 0;
		for ( String keyAlias : keyColumnAliases ) {
			sqlSelections.set( i,
					sqlSelection( columnSuffix, keyAlias, i, sqlSelections ) );
			i++;
		}

		if ( hasIndex() ) {
			for ( String indexAlias : castNonNull( indexColumnAliases ) ) {
				sqlSelections.set( i,
						sqlSelection( columnSuffix, indexAlias, i, sqlSelections ) );
				i++;
			}
		}
		if ( hasId() ) {
			sqlSelections.set( i,
					sqlSelection( columnSuffix, castNonNull( identifierColumnAlias ), i, sqlSelections ) );
			i++;
		}

		for ( int columnIndex = 0; i < sqlSelections.size(); i++, columnIndex++ ) {
			sqlSelections.set( i,
					sqlSelection( columnSuffix, elementColumnAliases[columnIndex], i, sqlSelections ) );
		}

		final String sql =
				getSqlAstTranslatorFactory()
						.buildTranslator( new SqlAstTranslationRequest.Select( getFactory(), new SelectStatement( rootQuerySpec ) ) )
						.translate( null, QueryOptions.NONE )
						.getSqlString();
		final int fromIndex = sql.lastIndexOf( " from" );
		final int selectLength = "select ".length();
		return fromIndex < 0
				? sql.substring( selectLength )
				: sql.substring( selectLength, fromIndex );
	}

	@Nonnull
	private static SqlSelectionImpl sqlSelection(@Nonnull String columnSuffix, @Nonnull String keyAlias, int i, @Nonnull List<SqlSelection> sqlSelections) {
		return new SqlSelectionImpl( sqlSelections.get( i ).getValuesArrayPosition(),
				new AliasedExpression( sqlSelections.get( i ).getExpression(),
						keyAlias + columnSuffix ) );
	}

	@Nonnull
	protected String generateSelectSizeString(boolean isIntegerIndexed) {
		final String selectValue = isIntegerIndexed
				? "max(" + castNonNull( getIndexColumnNames() )[0] + ") + 1"  // lists, arrays
				: "count(" + getElementColumnNames()[0] + ")"; // sets, maps, bags
		return new SimpleSelect( getFactory() )
				.setTableName( getTableName() )
				.addRestriction( getKeyColumnNames() )
				.addRestriction( keyFormulas )
				.addWhereToken( sqlWhereString )
				.addColumn( selectValue )
				.toStatementString();
	}

	@Nullable
	protected String generateDetectRowByIndexString() {
		if ( !hasIndex() ) {
			return null;
		}
		else {
			return new SimpleSelect( getFactory() )
					.setTableName( getTableName() )
					.addRestriction( getKeyColumnNames() )
					.addRestriction( getKeyFormulas() )
					.addRestriction( getIndexColumnNames() )
					.addRestriction( getIndexFormulas() )
					.addWhereToken( sqlWhereString )
					.addColumn( "1" )
					.toStatementString();
		}
	}


	@Nonnull
	protected String generateDetectRowByElementString() {
		return new SimpleSelect( getFactory() )
				.setTableName( getTableName() )
				.addRestriction( getKeyColumnNames() )
				.addRestriction( getKeyFormulas() )
				.addRestriction( getElementColumnNames() )
				.addRestriction( getElementFormulas() )
				.addWhereToken( sqlWhereString )
				.addColumn( "1" )
				.toStatementString();
	}

	@Nullable
	public String[] getIndexColumnNames() {
		return indexColumnNames;
	}

	@Nonnull
	public String[] getElementColumnNames() {
		return elementColumnNames; // TODO: something with formulas...
	}

	@Nonnull
	public String[] getKeyColumnNames() {
		return keyColumnNames;
	}

	@Nonnull
	public String[] getKeyFormulas() {
		return keyFormulas;
	}

	@Nonnull
	public String[] getElementFormulas() {
		return elementFormulas;
	}

	@Nullable
	public String[] getIndexFormulas() {
		return indexFormulas;
	}

	@Override
	public boolean hasIndex() {
		return collectionSemantics.getCollectionClassification().isIndexed();
	}

	private boolean hasId() {
		return collectionSemantics.getCollectionClassification() == CollectionClassification.ID_BAG;
	}

	@Override
	public boolean isLazy() {
		return isLazy;
	}

	@Override
	public boolean isInverse() {
		return isInverse;
	}

	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	@Nonnull
	public String getTableName() {
		return qualifiedTableName;
	}

	@Override
	public void remove(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) throws HibernateException {
		getRemoveCoordinator().deleteAllRows( id, session );
	}

	boolean isHistoryStrategy() {
		return getFactory().getSessionFactoryOptions().getTemporalTableStrategy() == HISTORY_TABLE;
	}

	@Override
	public boolean isRowDeleteEnabled() {
		return keyIsUpdateable;
	}

	@Override
	public boolean needsRemove() {
		return !isInverse() && isRowDeleteEnabled();
	}

	@Override
	public boolean isRowInsertEnabled() {
		return keyIsUpdateable;
	}

	@Nullable
	@Override
	public boolean[] getIndexColumnIsSettable() {
		return indexColumnIsSettable;
	}

	@Nonnull
	@Override
	public boolean[] getElementColumnIsSettable() {
		return elementColumnIsSettable;
	}

	@Nonnull
	@Override
	public UnaryOperator<Object> getIndexIncrementer() {
		return this::incrementIndexByBase;
	}

	@Nonnull
	public String getOwnerEntityName() {
		return ownerPersister.getEntityName();
	}

	@Nonnull
	@Override
	public EntityPersister getOwnerEntityPersister() {
		return ownerPersister;
	}

	@Nullable
	@Override
	public BeforeExecutionGenerator getGenerator() {
		return identifierGenerator;
	}

	@Override
	public boolean hasOrphanDelete() {
		return hasOrphanDelete;
	}

	@Override
	public void applyBaseRestrictions(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nonnull Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			@Nullable Set<String> treatAsDeclarations,
			@Nullable SqlAstCreationState creationState) {
		applyFilterRestrictions( predicateConsumer, tableGroup, useQualifier, enabledFilters, onlyApplyLoadByKeyFilters, creationState );
		applyWhereRestrictions( predicateConsumer, tableGroup, useQualifier, creationState );
	}

	@Override
	public boolean hasWhereRestrictions() {
		return hasWhere || manyToManyWhereTemplate != null;
	}

	@Nullable
	private static String aliasForWhereRestriction(@Nullable TableReference tableReference, boolean useQualifier) {
		if ( tableReference == null ) {
			return null;
		}
		else if ( useQualifier && tableReference.getIdentificationVariable() != null ) {
			return tableReference.getIdentificationVariable();
		}
		else {
			return tableReference.getTableId();
		}
	}

	@Override
	public void applyWhereRestrictions(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nullable SqlAstCreationState creationState) {
		final var tableReference =
				isManyToMany()
						? tableGroup.getPrimaryTableReference()
						: tableGroup.getTableReference( tableGroup.getNavigablePath(),
								elementPersister != null ? elementPersister.getTableName() : qualifiedTableName );
		final String alias = aliasForWhereRestriction( tableReference, useQualifier );
		applyWhereFragments( predicateConsumer, alias, tableGroup, useQualifier, creationState );
	}

	protected void applyWhereFragments(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nullable String alias,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nullable SqlAstCreationState astCreationState) {
		if ( sqlWhereStringTemplate != null && !isManyToMany() && elementPersister != null ) {
			predicateConsumer.accept( new SqlFragmentPredicate( getSqlWhereRendering().render(
					alias, useQualifier, tableGroup, astCreationState ) ) );
		}
		else {
			applyWhereFragments( predicateConsumer, alias, sqlWhereStringTemplate );
		}
	}

	// Initialized on first use because eager entity loaders can render these restrictions
	// before collection postInstantiate(). The immutable renderers are shared by all sessions.
	private RestrictionRendering getSqlWhereRendering() {
		var rendering = sqlWhereRendering;
		if ( rendering == null ) {
			rendering = RestrictionRendering.compile( sqlWhereStringTemplate, elementPersister );
			sqlWhereRendering = rendering;
		}
		return rendering;
	}

	private RestrictionRendering getManyToManyWhereRendering() {
		var rendering = manyToManyWhereRendering;
		if ( rendering == null ) {
			rendering = RestrictionRendering.compile( manyToManyWhereTemplate, elementPersister );
			manyToManyWhereRendering = rendering;
		}
		return rendering;
	}

	/**
	 * Applies all defined {@link org.hibernate.annotations.SQLRestriction}
	 */
	private static void applyWhereFragments(@Nonnull Consumer<Predicate> predicateConsumer, @Nullable String alias, @Nullable String template) {
		if ( template != null ) {
			final String fragment = replace( template, Template.TEMPLATE, alias );
			if ( !isEmpty( fragment ) ) {
				predicateConsumer.accept( new SqlFragmentPredicate( fragment ) );
			}
		}
	}

	@Override
	public void applyFilterRestrictions(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nonnull Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			@Nullable SqlAstCreationState creationState) {
		if ( filterHelper != null ) {
			filterHelper.applyEnabledFilters(
					predicateConsumer,
					getFilterAliasGenerator( tableGroup ),
					enabledFilters,
					onlyApplyLoadByKeyFilters,
					tableGroup,
					creationState
			);
		}
	}

	@Override
	public abstract boolean isManyToMany();

	@Override
	public void applyBaseManyToManyRestrictions(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nonnull Map<String, Filter> enabledFilters,
			@Nullable Set<String> treatAsDeclarations,
			@Nullable SqlAstCreationState creationState) {
		if ( manyToManyFilterHelper != null || manyToManyWhereTemplate != null ) {
			if ( manyToManyFilterHelper != null ) {
				manyToManyFilterHelper.applyEnabledFilters(
						predicateConsumer,
						castNonNull( elementPersister ).getFilterAliasGenerator( tableGroup ),
						enabledFilters,
						false,
						tableGroup,
						creationState
				);
			}
			if ( manyToManyWhereString != null ) {
				final var tableReference = tableGroup.resolveTableReference( castNonNull( elementPersister ).getTableName() );
				final String alias = aliasForWhereRestriction( tableReference, useQualifier );
				predicateConsumer.accept( new SqlFragmentPredicate( getManyToManyWhereRendering().render(
						alias, useQualifier, tableGroup, creationState ) ) );
			}
		}
	}

	@Nonnull
	@Override
	public EntityPersister getElementPersister() {
		if ( elementPersister == null ) {
			throw new AssertionFailure( "not an association" );
		}
		return elementPersister;
	}

	@Nullable
	protected EntityPersister getElementPersisterInternal() {
		return elementPersister;
	}

	@Nonnull
	@Override
	public String[] getCollectionSpaces() {
		return spaces;
	}

	@Override
	public void processQueuedOps(@Nonnull PersistentCollection<?> collection, @Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		if ( collection.hasQueuedOperations() ) {
			doProcessQueuedOps( collection, key, session );
		}
	}

	protected abstract void doProcessQueuedOps(@Nonnull PersistentCollection<?> collection, @Nonnull Object key, @Nonnull SharedSessionContractImplementor session)
			throws HibernateException;

	@Nonnull
	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Nonnull
	@Override
	public String toString() {
		return unqualify( getClass().getName() ) + '(' + navigableRole.getFullPath() + ')';
	}

	@Override
	public boolean isVersioned() {
		return isVersioned && getOwnerEntityPersister().isVersioned();
	}

	// TODO: needed???
	@Nonnull
	protected SqlExceptionHelper getSQLExceptionHelper() {
		return sqlExceptionHelper;
	}

	@Override
	@Nonnull
	public CacheEntryStructure getCacheEntryStructure() {
		return cacheEntryStructure;
	}

	@Override
	public boolean isAffectedByEnabledFilters(@Nonnull SharedSessionContractImplementor session) {
		return isAffectedByEnabledFilters( session.getLoadQueryInfluencers() );
	}

	@Override
	public boolean isSubselectLoadable() {
		return subselectLoadable;
	}

	@Override
	public boolean isMutable() {
		return isMutable;
	}

	@Nullable
	@Override
	public String[] getCollectionPropertyColumnAliases(@Nonnull String propertyName, @Nonnull String suffix) {
		final String[] rawAliases = collectionPropertyColumnAliases.get( propertyName );
		if ( rawAliases == null ) {
			return null;
		}
		else {
			final String[] result = new String[rawAliases.length];
			final var alias = new Alias( suffix );
			for ( int i = 0; i < rawAliases.length; i++ ) {
				result[i] = alias.toUnquotedAliasString( rawAliases[i] );
			}
			return result;
		}
	}

	// TODO: formulas ?
	public void initCollectionPropertyMap() {
		initCollectionPropertyMap( "key", keyType, keyColumnAliases );
		initCollectionPropertyMap( "element", elementType, elementColumnAliases );
		if ( hasIndex() ) {
			initCollectionPropertyMap( "index", castNonNull( indexType ), castNonNull( indexColumnAliases ) );
		}
		if ( hasId() ) {
			initCollectionPropertyMap( "id", castNonNull( identifierType ), new String[] { identifierColumnAlias } );
		}
	}

	private void initCollectionPropertyMap(@Nonnull String aliasName, @Nonnull Type type, @Nonnull String[] columnAliases) {
		collectionPropertyColumnAliases.put( aliasName, columnAliases );

		//TODO: this code is almost certainly obsolete and can be removed
		if ( type instanceof ComponentType || type instanceof AnyType ) {
			final var compositeType = (CompositeType) type;
			final String[] propertyNames = compositeType.getPropertyNames();
			for ( int i = 0; i < propertyNames.length; i++ ) {
				final String name = propertyNames[i];
				collectionPropertyColumnAliases.put( aliasName + "." + name, new String[] {columnAliases[i]} );
			}
		}
	}

	int baseIndex() {
		final int listIndexBase = getAttributeMapping().getIndexMetadata().getListIndexBase();
		//noinspection ManualMinMaxCalculation
		return listIndexBase < 0 ? 0 : listIndexBase;
	}

	@Override
	public int getSize(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		try {
			final var jdbcCoordinator = session.getJdbcCoordinator();
			final var statement = jdbcCoordinator.getStatementPreparer().prepareStatement( sqlSelectSizeString );
			final var resourceRegistry = jdbcCoordinator.getLogicalConnection().getResourceRegistry();
			try {
				getKeyType().nullSafeSet( statement, key, 1, session );
				final var resultSet = jdbcCoordinator.getResultSetReturn().extract( statement, sqlSelectSizeString );
				try {
					return resultSet.next() ? resultSet.getInt( 1 ) - baseIndex() : 0;
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
		catch ( SQLException sqle ) {
			throw getSQLExceptionHelper().convert(
					sqle,
					"could not retrieve collection size: " +
							collectionInfoString( this, key, getFactory() ),
					sqlSelectSizeString
			);
		}
	}

	@Override
	public boolean indexExists(@Nonnull Object key, @Nonnull Object index, @Nonnull SharedSessionContractImplementor session) {
		return exists( key, incrementIndexByBase( index ), castNonNull( getIndexType() ), castNonNull( sqlDetectRowByIndexString ), session );
	}

	@Override
	public boolean elementExists(@Nonnull Object key, @Nullable Object element, @Nonnull SharedSessionContractImplementor session) {
		return exists( key, element, getElementType(), sqlDetectRowByElementString, session );
	}

	private boolean exists(@Nonnull Object key, @Nullable Object indexOrElement, @Nonnull Type indexOrElementType, @Nonnull String sql, @Nonnull SharedSessionContractImplementor session) {
		try {
			final var jdbcCoordinator = session.getJdbcCoordinator();
			final var statement = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
			final var resourceRegistry = jdbcCoordinator.getLogicalConnection().getResourceRegistry();
			try {
				getKeyType().nullSafeSet( statement, key, 1, session );
				indexOrElementType.nullSafeSet( statement, indexOrElement, keyColumnNames.length + 1, session );
				final var resultSet = jdbcCoordinator.getResultSetReturn().extract( statement, sql );
				try {
					return resultSet.next();
				}
				finally {
					resourceRegistry.release( resultSet, statement );
				}
			}
			catch ( TransientObjectException e ) {
				return false;
			}
			finally {
				resourceRegistry.release( statement );
				jdbcCoordinator.afterStatementExecution();
			}
		}
		catch ( SQLException sqle ) {
			throw getSQLExceptionHelper().convert(
					sqle,
					"could not check row existence: " +
							collectionInfoString( this, key, getFactory() ),
					sqlSelectSizeString
			);
		}
	}

	@Nullable
	@Override
	public Object getElementByIndex(@Nonnull Object key, @Nonnull Object index, @Nonnull SharedSessionContractImplementor session, @Nullable Object owner) {
		final var influencers = session.getLoadQueryInfluencers();
		if ( influencers.hasEnabledFilters()
			&& isAffectedByFilters( new HashSet<>(), attributeMapping.getElementDescriptor(), influencers, true ) ) {
			return new CollectionElementLoaderByIndex( attributeMapping, influencers, factory )
					.load( key, index, session );
		}
		else {
			return castNonNull( collectionElementLoaderByIndex ).load( key, index, session );
		}
	}

	@Override
	public boolean isExtraLazy() {
		return isExtraLazy;
	}

	@Nonnull
	protected Dialect getDialect() {
		return dialect;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Nullable
	@Override
	public String getMappedByProperty() {
		return mappedByProperty;
	}

	@Nonnull
	public abstract FilterAliasGenerator getFilterAliasGenerator(@Nonnull String rootAlias);

	@Nonnull
	public abstract FilterAliasGenerator getFilterAliasGenerator(@Nonnull TableGroup tableGroup);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// "mapping model"

	/**
	 * Allows injection of the corresponding {@linkplain PluralAttributeMapping plural-attribute mapping}.
	 *
	 * @implNote Helps solve the chicken-egg problem of which to create first.  Ultimately we could
	 * make this work in a similar fashion to how this works in the relationship between
	 * {@link org.hibernate.metamodel.mapping.EmbeddableMappingType} and {@link EmbeddableValuedModelPart}.
	 */
	@Override
	public void injectAttributeMapping(@Nonnull PluralAttributeMapping attributeMapping) {
		this.attributeMapping = attributeMapping;
	}

	@Nonnull
	@Override
	public PluralAttributeMapping getAttributeMapping() {
		return attributeMapping;
	}

	@Override
	public void registerAffectingFetchProfile(@Nonnull String fetchProfileName) {
		if ( affectingFetchProfiles == null ) {
			affectingFetchProfiles = new HashSet<>();
		}
		affectingFetchProfiles.add( fetchProfileName );
	}

	@Override
	public boolean isAffectedByEnabledFetchProfiles(@Nonnull LoadQueryInfluencers influencers) {
		if ( affectingFetchProfiles != null && influencers.hasEnabledFetchProfiles() ) {
			for ( String profileName : affectingFetchProfiles ) {
				if ( influencers.isFetchProfileEnabled( profileName ) ) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isAffectedByEnabledFilters(@Nonnull LoadQueryInfluencers influencers) {
		return isAffectedByEnabledFilters( influencers, false );
	}

	@Override
	public boolean isAffectedByEnabledFilters(@Nonnull LoadQueryInfluencers influencers, boolean onlyApplyForLoadByKeyFilters) {
		if ( influencers.hasEnabledFilters() ) {
			final var enabledFilters = influencers.getEnabledFilters();
			return filterHelper != null && filterHelper.isAffectedBy( enabledFilters )
				|| manyToManyFilterHelper != null && manyToManyFilterHelper.isAffectedBy( enabledFilters )
				|| isKeyOrElementAffectedByFilters( new HashSet<>(), influencers, onlyApplyForLoadByKeyFilters );
		}
		else {
			return false;
		}
	}

	@Override
	public boolean isAffectedByEnabledFilters(
			@Nonnull Set<ManagedMappingType> visitedTypes,
			@Nonnull LoadQueryInfluencers influencers,
			boolean onlyApplyForLoadByKeyFilters) {
		assert influencers.hasEnabledFilters();
		final var enabledFilters = influencers.getEnabledFilters();
		return filterHelper != null && filterHelper.isAffectedBy( enabledFilters )
			|| manyToManyFilterHelper != null && manyToManyFilterHelper.isAffectedBy( enabledFilters )
			|| isKeyOrElementAffectedByFilters( visitedTypes, influencers, onlyApplyForLoadByKeyFilters );
	}

	private boolean isKeyOrElementAffectedByFilters(
			@Nonnull Set<ManagedMappingType> visitedTypes,
			@Nonnull LoadQueryInfluencers influencers,
			boolean onlyApplyForLoadByKey) {
		return isAffectedByFilters( visitedTypes, attributeMapping.getIndexDescriptor(), influencers, onlyApplyForLoadByKey )
			|| isAffectedByFilters( visitedTypes, attributeMapping.getElementDescriptor(), influencers, onlyApplyForLoadByKey );
	}

	private boolean isAffectedByFilters(
			@Nonnull Set<ManagedMappingType> visitedTypes,
			@Nonnull CollectionPart collectionPart,
			@Nonnull LoadQueryInfluencers influencers,
			boolean onlyApplyForLoadByKey) {
		if ( collectionPart instanceof EntityCollectionPart entityCollectionPart ) {
			return entityCollectionPart.getEntityMappingType()
					.isAffectedByEnabledFilters( visitedTypes, influencers, onlyApplyForLoadByKey );
		}
		else if ( collectionPart instanceof EmbeddedCollectionPart embeddedCollectionPart ) {
			return embeddedCollectionPart.getEmbeddableTypeDescriptor()
					.isAffectedByEnabledFilters( visitedTypes, influencers, onlyApplyForLoadByKey );
		}
		else {
			return false;
		}
	}

	@Override
	public boolean isAffectedByEntityGraph(@Nonnull LoadQueryInfluencers influencers) {
		// todo (6.0) : anything to do here?
		return false;
	}

	@Nonnull
	@Override
	public CollectionSemantics<?,?> getCollectionSemantics() {
		return collectionSemantics;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CollectionMutationTarget

	@Nonnull
	@Override
	public PluralAttributeMapping getTargetPart() {
		return attributeMapping;
	}

	@Nonnull
	@Override
	public String getIdentifierTableName() {
		return tableMapping.getTableName();
	}

	@Nonnull
	@Override
	public CollectionTableMapping getCollectionTableMapping() {
		return tableMapping;
	}

	@Nonnull
	@Override
	public CollectionTableDescriptor getCollectionTableDescriptor() {
		return collectionTableDescriptor;
	}

	@Override
	public void forEachMutableTableDescriptor(@Nonnull Consumer<CollectionTableDescriptor> consumer) {
		consumer.accept( getCollectionTableDescriptor() );
	}

	@Override
	public void forEachMutableTableDescriptorReverse(@Nonnull Consumer<CollectionTableDescriptor> consumer) {
		consumer.accept( getCollectionTableDescriptor() );
	}

	@Override
	public boolean hasPhysicalIndexColumn() {
		return hasIndex() && !indexContainsFormula;
	}

	@Override
	public void forEachMutableTable(@Nonnull Consumer<CollectionTableMapping> consumer) {
		consumer.accept( tableMapping );
	}

	@Override
	public void forEachMutableTableReverse(@Nonnull Consumer<CollectionTableMapping> consumer) {
		consumer.accept( tableMapping );
	}

	@Nonnull
	private static CollectionTableDescriptor buildCollectionTableDescriptor(
			@Nonnull CollectionTableMapping tableMapping,
			@Nonnull PluralAttributeMapping attributeMapping,
			@Nonnull SessionFactoryImplementor factory) {
		final String qualifiedTableName = tableMapping.getTableName();
		// NOTE: if ActionQueue is not the graph-based one, isSelfReferential will have no impact
		final boolean isSelfReferential;
		final boolean hasUniqueKeys;
		if ( factory.getActionQueueFactory()
				instanceof GraphBasedActionQueueFactory graphBasedActionQueueFactory ) {
			isSelfReferential =
					graphBasedActionQueueFactory.getConstraintModel()
							.hasSelfReferentialTable( qualifiedTableName );
			// Collection-table uniqueness is still not modeled completely enough to use as a negative signal.
			hasUniqueKeys = true;
		}
		else {
			isSelfReferential = false;
			hasUniqueKeys = false;
		}
		return new CollectionTableDescriptor(
				qualifiedTableName,
				attributeMapping.getNavigableRole(),
				tableMapping.isJoinTable(),
				tableMapping.isInverse(),
				isSelfReferential,
				hasUniqueKeys,
				tableMapping.isCascadeDeleteEnabled(),
				tableMapping.getInsertDetails(),
				tableMapping.getUpdateDetails(),
				tableMapping.getDeleteRowDetails(),
				tableMapping.getDeleteDetails(),
				buildTableKeyDescriptor( attributeMapping )
		);
	}

	@Nonnull
	private static TableKeyDescriptor buildTableKeyDescriptor(@Nonnull PluralAttributeMapping attributeMapping) {
		var keyColumns = new ArrayList<ColumnDescriptor>();
		attributeMapping.getKeyDescriptor().visitKeySelectables( (index, selectableMapping)
				-> keyColumns.add( ColumnDescriptor.from( selectableMapping ) ) );
		return new TableKeyDescriptor( keyColumns );
	}

	@Nonnull
	private static CollectionTableMapping buildCollectionTableMapping(
			@Nonnull Collection collectionBootDescriptor,
			@Nonnull String qualifiedTableName,
			@Nonnull String[] spaces) {
		return new CollectionTableMapping(
				qualifiedTableName,
				spaces,
				!collectionBootDescriptor.isOneToMany(),
				collectionBootDescriptor.isInverse(),
				buildInsertMutationDetails( collectionBootDescriptor ),
				buildUpdateMutationDetails( collectionBootDescriptor ),
				collectionBootDescriptor.getKey().isCascadeDeleteEnabled(),
				buildDeleteAllMutationDetails( collectionBootDescriptor ),
				buildDeleteMutationDetails( collectionBootDescriptor )
		);
	}

	@Nonnull
	private static MutationDetails buildUpdateMutationDetails(@Nonnull Collection collectionBootDescriptor) {
		final boolean customUpdateCallable = collectionBootDescriptor.isCustomUpdateCallable();
		return new MutationDetails(
				MutationType.UPDATE,
				createExpectation( collectionBootDescriptor.getUpdateExpectation(),
						customUpdateCallable ),
				collectionBootDescriptor.getCustomSQLUpdate(),
				customUpdateCallable
		);
	}

	@Nonnull
	private static MutationDetails buildInsertMutationDetails(@Nonnull Collection collectionBootDescriptor) {
		return new MutationDetails(
				MutationType.INSERT,
				createExpectation( collectionBootDescriptor.getInsertExpectation(),
						collectionBootDescriptor.isCustomInsertCallable() ),
				collectionBootDescriptor.getCustomSQLInsert(),
				collectionBootDescriptor.isCustomInsertCallable()
		);
	}

	@Nonnull
	private static MutationDetails buildDeleteMutationDetails(@Nonnull Collection collectionBootDescriptor) {
		final boolean customDeleteCallable = collectionBootDescriptor.isCustomDeleteCallable();
		return new MutationDetails(
				MutationType.DELETE,
				createExpectation( collectionBootDescriptor.getDeleteExpectation(),
						customDeleteCallable ),
				collectionBootDescriptor.getCustomSQLDelete(),
				customDeleteCallable
		);
	}

	@Nonnull
	private static MutationDetails buildDeleteAllMutationDetails(@Nonnull Collection collectionBootDescriptor) {
		final boolean customDeleteAllCallable = collectionBootDescriptor.isCustomDeleteAllCallable();
		final var deleteAllExpectation = collectionBootDescriptor.getDeleteAllExpectation();
		return new MutationDetails(
				MutationType.DELETE,
				customDeleteAllCallable || deleteAllExpectation != null
						? createExpectation( deleteAllExpectation,
						customDeleteAllCallable )
						: new Expectation.None(),
				collectionBootDescriptor.getCustomSQLDeleteAll(),
				customDeleteAllCallable
		);
	}

	@Nonnull
	protected JdbcMutationOperation buildDeleteAllOperation(@Nonnull MutatingTableReference tableReference) {
		return tableMapping.getDeleteDetails().getCustomSql() != null
				? buildCustomSqlDeleteAllOperation( tableReference )
				: buildGeneratedDeleteAllOperation( tableReference );
	}

	@Nonnull
	private JdbcDeleteMutation buildCustomSqlDeleteAllOperation(@Nonnull MutatingTableReference tableReference) {
		final var keyDescriptor = getAttributeMapping().getKeyDescriptor();
		final var parameterBinders =
				new ColumnValueParameterList( tableReference, ParameterUsage.RESTRICT, keyDescriptor.getJdbcTypeCount() );
		keyDescriptor.getKeyPart().forEachSelectable( parameterBinders );
		final var tableMapping = tableReference.getTableMapping();
		final var deleteDetails = tableMapping.getDeleteDetails();
		return new JdbcDeleteMutation(
				tableMapping,
				this,
				deleteDetails.getCustomSql(),
				deleteDetails.isCallable(),
				deleteDetails.getExpectation(),
				parameterBinders
		);
	}

	@Nonnull
	private JdbcMutationOperation buildGeneratedDeleteAllOperation(@Nonnull MutatingTableReference tableReference) {
		return getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.ModelMutation<>( getFactory(), generateDeleteAllAst( tableReference ) ) )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	@Nonnull
	public RestrictedTableMutation<JdbcMutationOperation> generateDeleteAllAst(@Nonnull MutatingTableReference tableReference) {
		assert getAttributeMapping() != null;
		final var foreignKeyDescriptor = getAttributeMapping().getKeyDescriptor();
		assert foreignKeyDescriptor != null;
		final int keyColumnCount = foreignKeyDescriptor.getJdbcTypeCount();
		final var parameterBinders =
				new ColumnValueParameterList( tableReference, ParameterUsage.RESTRICT, keyColumnCount );
		final List<ColumnValueBinding> restrictionBindings = arrayList( keyColumnCount );
		applyKeyRestrictions( parameterBinders, restrictionBindings );
		//noinspection unchecked,rawtypes
		return (RestrictedTableMutation) new TableDeleteStandard(
				tableReference,
				this,
				"one-shot delete for " + getRolePath(),
				restrictionBindings,
				emptyList(),
				parameterBinders,
				sqlWhereString
		);
	}

	protected void applyKeyRestrictions(
			@Nonnull ColumnValueParameterList parameterList,
			@Nonnull List<ColumnValueBinding> restrictionBindings) {
		final var foreignKeyDescriptor = getAttributeMapping().getKeyDescriptor();
		assert foreignKeyDescriptor != null;
		foreignKeyDescriptor.getKeyPart().forEachSelectable( (selectionIndex, selectableMapping) -> {
			final var columnValueParameter = parameterList.addColumValueParameter( selectableMapping );
			restrictionBindings.add(
					new ColumnValueBinding(
							columnValueParameter.getColumnReference(),
							new ColumnWriteFragment(
									"?",
									columnValueParameter,
									selectableMapping
							)
					)
			);
		});
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Types (the methods are already deprecated on CollectionPersister)
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Deprecated private final CollectionType collectionType;
	@Deprecated private final Type keyType;
	@Nullable
	@Deprecated private final Type identifierType;
	@Nullable
	@Deprecated private final Type indexType;
	@Deprecated protected final Type elementType;

	@Nonnull
	@Override @Deprecated(forRemoval = true)
	public CollectionType getCollectionType() {
		return collectionType;
	}

	@Nonnull
	@Override @Deprecated(forRemoval = true)
	public Type getKeyType() {
		return keyType;
	}

	@Nullable
	@Override @Deprecated(forRemoval = true)
	public Type getIdentifierType() {
		return identifierType;
	}

	@Nullable
	@Override @Deprecated(forRemoval = true)
	public Type getIndexType() {
		return indexType;
	}

	@Nonnull
	@Override @Deprecated(forRemoval = true)
	public Type getElementType() {
		return elementType;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// State related to this we handle differently in 6+.  In other words, state
	// that is no longer needed
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Deprecated protected final String[] keyColumnAliases;
	@Nullable
	@Deprecated private final String identifierColumnAlias;
	@Nullable
	@Deprecated protected final String[] indexColumnAliases;
	@Deprecated protected final String[] elementColumnAliases;
	@Deprecated private final Map<String,String[]> collectionPropertyColumnAliases = new HashMap<>();

	@Nonnull
	@Override @Deprecated(forRemoval = true)
	public String[] getKeyColumnAliases(@Nonnull String suffix) {
		return new Alias( suffix ).toAliasStrings( keyColumnAliases );
	}

	@Nonnull
	@Override @Deprecated(forRemoval = true)
	public String[] getElementColumnAliases(@Nonnull String suffix) {
		return new Alias( suffix ).toAliasStrings( elementColumnAliases );
	}

	@Nullable
	@Override @Deprecated(forRemoval = true)
	public String[] getIndexColumnAliases(@Nonnull String suffix) {
		return hasIndex() ? new Alias( suffix ).toAliasStrings( indexColumnAliases ) : null;
	}

	@Nullable
	@Override @Deprecated(forRemoval = true)
	public String getIdentifierColumnAlias(@Nonnull String suffix) {
		return hasId() ? new Alias( suffix ).toAliasString( identifierColumnAlias ) : null;
	}

	@Nonnull
	@Override
	public String getRolePath() {
		return getNavigableRole().getFullPath();
	}

//	protected Object lockCacheItem(CollectionAction action, SharedSessionContractImplementor session) {
//		if (!action.getPersister().hasCache()) {
//			return null;
//		}
//
//		final CollectionDataAccess cache = action.getPersister().getCacheAccessStrategy();
//		return cache.generateCacheKey(
//				action.getKey(),
//				action.getPersister(),
//				session.getFactory(),
//				session.getTenantIdentifier()
//		);
//		// Note: The actual lock is obtained in CollectionAction.beforeExecutions()
//		// We just generate the cache key here for use in post-execution
//	}

}
