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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LazyInitializationException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.boot.Metadata;
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
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
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
import org.hibernate.id.Assigned;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.PostInsertIdentifierGenerator;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.insert.Binder;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.FilterHelper;
import org.hibernate.internal.util.LazyValue;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.LockModeEnumMap;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.jdbc.TooManyRowsAffectedException;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.loader.ast.internal.LoaderSqlAstCreationState;
import org.hibernate.loader.ast.internal.MultiIdLoaderStandard;
import org.hibernate.loader.ast.internal.Preparable;
import org.hibernate.loader.ast.internal.SingleIdArrayLoadPlan;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderDynamicBatch;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderProvidedQueryImpl;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderStandardImpl;
import org.hibernate.loader.ast.internal.SingleUniqueKeyEntityLoaderStandard;
import org.hibernate.loader.ast.spi.Loader;
import org.hibernate.loader.ast.spi.MultiIdEntityLoader;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.loader.ast.spi.SingleIdEntityLoader;
import org.hibernate.loader.ast.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.loader.entity.CacheEntityLoaderHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.AttributeMetadataAccess;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingModelHelper;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.Queryable;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.mapping.internal.BasicEntityIdentifierMappingImpl;
import org.hibernate.metamodel.mapping.internal.CompoundNaturalIdMapping;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityRowIdMappingImpl;
import org.hibernate.metamodel.mapping.internal.EntityVersionMappingImpl;
import org.hibernate.metamodel.mapping.internal.ExplicitColumnDiscriminatorMappingImpl;
import org.hibernate.metamodel.mapping.internal.GeneratedValuesProcessor;
import org.hibernate.metamodel.mapping.internal.InFlightEntityMappingType;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.internal.ImmutableAttributeMappingList;
import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.SemanticException;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sql.internal.SQLQueryParser;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.Alias;
import org.hibernate.sql.Delete;
import org.hibernate.sql.Insert;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.Template;
import org.hibernate.sql.Update;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.AliasedExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.entity.internal.EntityResultImpl;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.InDatabaseValueGenerationStrategy;
import org.hibernate.tuple.InMemoryValueGenerationStrategy;
import org.hibernate.tuple.NonIdentifierAttribute;
import org.hibernate.tuple.ValueGeneration;
import org.hibernate.tuple.entity.EntityBasedAssociationAttribute;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.processIfPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.processIfSelfDirtinessTracker;

/**
 * Basic functionality for persisting an entity via JDBC
 * through either generated or custom SQL
 *
 * @author Gavin King
 */
public abstract class AbstractEntityPersister
		implements OuterJoinLoadable, Queryable, ClassMetadata, UniqueKeyLoadable,
				SQLLoadable, LazyPropertyInitializer, PostInsertIdentityPersister, Lockable,
				org.hibernate.persister.entity.Queryable, InFlightEntityMappingType {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractEntityPersister.class );

	public static final String ENTITY_CLASS = "class";
	public static final String VERSION_COLUMN_ALIAS = "version_";

	private final String sqlAliasStem;

	private final SingleIdEntityLoader<?> singleIdEntityLoader;
	private final MultiIdEntityLoader<?> multiIdEntityLoader;
	private NaturalIdLoader<?> naturalIdLoader;
	private MultiNaturalIdLoader<?> multiNaturalIdLoader;

	private SqmMultiTableMutationStrategy sqmMultiTableMutationStrategy;
	private SqmMultiTableInsertStrategy sqmMultiTableInsertStrategy;

	private final NavigableRole navigableRole;

	// moved up from AbstractEntityPersister ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private final SessionFactoryImplementor factory;
	private final boolean canReadFromCache;
	private final boolean canWriteToCache;
	private final boolean invalidateCache;
	private final EntityDataAccess cacheAccessStrategy;
	private final NaturalIdDataAccess naturalIdRegionAccessStrategy;
	private final boolean isLazyPropertiesCacheable;
	private final CacheEntryHelper cacheEntryHelper;
	private final EntityMetamodel entityMetamodel;
	private final EntityEntryFactory entityEntryFactory;
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final String[] rootTableKeyColumnNames;
	private final String[] rootTableKeyColumnReaders;
	private final String[] rootTableKeyColumnReaderTemplates;
	private final String[] identifierAliases;
	private final int identifierColumnSpan;
	private final String versionColumnName;
	private final boolean hasFormulaProperties;
	protected final int batchSize;
	private final boolean hasSubselectLoadableCollections;
	protected final String rowIdName;

	// The optional SQL string defined in the where attribute
	private final String sqlWhereStringTableExpression;
	private final String sqlWhereStringTemplate;

	//information about properties of this class,
	//including inherited properties
	//(only really needed for updatable/insertable properties)
	private final int[] propertyColumnSpans;
//	private final String[] propertySubclassNames;
	private final String[][] propertyColumnAliases;
	private final String[][] propertyColumnNames;
	private final String[][] propertyColumnFormulaTemplates;
//	private final String[][] propertyColumnReaderTemplates;
	private final String[][] propertyColumnWriters;
	private final boolean[][] propertyColumnUpdateable;
	private final boolean[][] propertyColumnInsertable;
//	private final boolean[] propertyUniqueness;
	private final boolean[] propertySelectable;

	private final List<Integer> lobProperties;

	//information about lazy properties of this class
	private final String[] lazyPropertyNames;
	private final int[] lazyPropertyNumbers;
	private final Type[] lazyPropertyTypes;
	private final String[][] lazyPropertyColumnAliases;

	//information about all properties in class hierarchy
	private final String[] subclassPropertyNameClosure;
//	private final String[] subclassPropertySubclassNameClosure;
	private final Type[] subclassPropertyTypeClosure;
	private final String[][] subclassPropertyFormulaTemplateClosure;
	private final String[][] subclassPropertyColumnNameClosure;
	private final String[][] subclassPropertyColumnReaderClosure;
	private final String[][] subclassPropertyColumnReaderTemplateClosure;
	private final FetchMode[] subclassPropertyFetchModeClosure;
	private final boolean[] subclassPropertyNullabilityClosure;
	private final boolean[] propertyDefinedOnSubclass;
//	private final int[][] subclassPropertyColumnNumberClosure;
//	private final int[][] subclassPropertyFormulaNumberClosure;
	private final CascadeStyle[] subclassPropertyCascadeStyleClosure;

	//information about all columns/formulas in class hierarchy
//	private final String[] subclassColumnClosure;
//	private final boolean[] subclassColumnLazyClosure;
	private final String[] subclassColumnAliasClosure;
//	private final String[] subclassColumnReaderTemplateClosure;
//	private final String[] subclassFormulaClosure;
//	private final String[] subclassFormulaTemplateClosure;
	private final String[] subclassFormulaAliasClosure;
//	private final boolean[] subclassFormulaLazyClosure;

	// dynamic filters attached to the class-level
	private final FilterHelper filterHelper;

	private volatile Set<String> affectingFetchProfileNames;

	private final LockModeEnumMap<LockingStrategy> lockers = new LockModeEnumMap<>();

	// SQL strings
	private String sqlVersionSelectString;
	private Map<String, SingleIdArrayLoadPlan> sqlLazySelectStringsByFetchGroup;

	private String sqlIdentityInsertString;
	private String sqlUpdateByRowIdString;
	private String sqlLazyUpdateByRowIdString;

	private String[] sqlDeleteStrings;
	private String[] sqlDeleteNoVersionCheckStrings;
	private String[] sqlInsertStrings;
	private String[] sqlUpdateStrings;
	private String[] sqlLazyUpdateStrings;

	private GeneratedValuesProcessor insertGeneratedValuesProcessor;
	private GeneratedValuesProcessor updateGeneratedValuesProcessor;

	//Custom SQL (would be better if these were private)
	protected boolean[] insertCallable;
	protected boolean[] updateCallable;
	protected boolean[] deleteCallable;
	protected String[] customSQLInsert;
	protected String[] customSQLUpdate;
	protected String[] customSQLDelete;
	protected ExecuteUpdateResultCheckStyle[] insertResultCheckStyles;
	protected ExecuteUpdateResultCheckStyle[] updateResultCheckStyles;
	protected ExecuteUpdateResultCheckStyle[] deleteResultCheckStyles;

	private InsertGeneratedIdentifierDelegate identityDelegate;

	private boolean[] tableHasColumns;

	private final Map<String,String[]> subclassPropertyAliases = new HashMap<>();
	private final Map<String,String[]> subclassPropertyColumnNames = new HashMap<>();

	/**
	 * Warning:
	 * When there are duplicated property names in the subclasses
	 * then propertyMapping will only contain one of those properties.
	 * To ensure correct results, propertyMapping should only be used
	 * for the concrete EntityPersister (since the concrete EntityPersister
	 * cannot have duplicated property names).
	 */
	protected final BasicEntityPropertyMapping propertyMapping;

	private final boolean useReferenceCacheEntries;

	protected void addDiscriminatorToInsert(Insert insert) {
	}

	@Override
	public abstract String getSubclassTableName(int j);

	protected abstract String[] getSubclassTableNames();

	protected abstract String[] getSubclassTableKeyColumns(int j);

	protected abstract boolean isClassOrSuperclassTable(int j);

	protected boolean isClassOrSuperclassJoin(int j) {
		/*
		 * TODO:
		 *  SingleTableEntityPersister incorrectly used isClassOrSuperclassJoin == isClassOrSuperclassTable,
		 *  this caused HHH-12895, as this resulted in the subclass tables always being joined, even if no
		 *  property on these tables was accessed.
		 *
		 *  JoinedTableEntityPersister does not use isClassOrSuperclassJoin at all, probably incorrectly so.
		 *  I however haven't been able to reproduce any quirks regarding <join>s, secondary tables or
		 *  @JoinTable's.
		 *
		 *  Probably this method needs to be properly implemented for the various entity persisters,
		 *  but this at least fixes the SingleTableEntityPersister, while maintaining the previous
		 *  behaviour for other persisters.
		 */
		return isClassOrSuperclassTable( j );
	}

	public abstract int getSubclassTableSpan();

	public abstract int getTableSpan();

	public abstract boolean isTableCascadeDeleteEnabled(int j);

	public abstract boolean hasDuplicateTables();

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
		if ( getEntityMetamodel().getSubclassEntityNames().size() == 1 ) {
			return getDiscriminatorSQLValue();
		}
		else {
			return Template.TEMPLATE + "." + DISCRIMINATOR_ALIAS;
		}
	}

	public String getDiscriminatorAlias() {
		return DISCRIMINATOR_ALIAS;
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

	protected boolean isInverseSubclassTable(int j) {
		return false;
	}

	@Override
	public boolean isSubclassEntityName(String entityName) {
		return entityMetamodel.getSubclassEntityNames().contains( entityName );
	}

	protected boolean[] getTableHasColumns() {
		return tableHasColumns;
	}

	@Override
	public String[] getRootTableKeyColumnNames() {
		return rootTableKeyColumnNames;
	}

	public String[] getSQLUpdateByRowIdStrings() {
		if ( sqlUpdateByRowIdString == null ) {
			throw new AssertionFailure( "no update by row id" );
		}
		String[] result = new String[getTableSpan() + 1];
		result[0] = sqlUpdateByRowIdString;
		System.arraycopy( sqlUpdateStrings, 0, result, 1, getTableSpan() );
		return result;
	}

	public String[] getSQLLazyUpdateByRowIdStrings() {
		if ( sqlLazyUpdateByRowIdString == null ) {
			throw new AssertionFailure( "no update by row id" );
		}
		String[] result = new String[getTableSpan()];
		result[0] = sqlLazyUpdateByRowIdString;
		System.arraycopy( sqlLazyUpdateStrings, 1, result, 1, getTableSpan() - 1 );
		return result;
	}

	SingleIdArrayLoadPlan getSQLLazySelectLoadPlan(String fetchGroup) {
		return sqlLazySelectStringsByFetchGroup.get( fetchGroup );
	}

	public String[] getSQLDeleteStrings() {
		return sqlDeleteStrings;
	}

	public String[] getSQLDeleteNoVersionCheckStrings() {
		return sqlDeleteNoVersionCheckStrings;
	}

	public String[] getSQLInsertStrings() {
		return sqlInsertStrings;
	}

	public String[] getSQLUpdateStrings() {
		return sqlUpdateStrings;
	}

	public String[] getSQLLazyUpdateStrings() {
		return sqlLazyUpdateStrings;
	}

	protected ExecuteUpdateResultCheckStyle[] getInsertResultCheckStyles() {
		return insertResultCheckStyles;
	}

	protected ExecuteUpdateResultCheckStyle[] getUpdateResultCheckStyles() {
		return updateResultCheckStyles;
	}

	protected ExecuteUpdateResultCheckStyle[] getDeleteResultCheckStyles() {
		return deleteResultCheckStyles;
	}

	/**
	 * The query that inserts a row, letting the database generate an id
	 *
	 * @return The IDENTITY-based insertion query.
	 */
	public String getSQLIdentityInsertString() {
		return sqlIdentityInsertString;
	}

	public String getVersionSelectString() {
		return sqlVersionSelectString;
	}

	public boolean isInsertCallable(int j) {
		return insertCallable[j];
	}

	public boolean isUpdateCallable(int j) {
		return updateCallable[j];
	}

	public boolean isDeleteCallable(int j) {
		return deleteCallable[j];
	}

	@Deprecated(forRemoval = true)
	protected boolean isSubclassTableSequentialSelect(int j) {
		return false;
	}

	/**
	 * Decide which tables need to be updated.
	 * <p/>
	 * The return here is an array of boolean values with each index corresponding
	 * to a given table in the scope of this persister.
	 *
	 * @param dirtyProperties The indices of all the entity properties considered dirty.
	 * @param hasDirtyCollection Whether any collections owned by the entity which were considered dirty.
	 *
	 * @return Array of booleans indicating which table require updating.
	 */
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
				tableUpdateNeeded[table] = tableUpdateNeeded[table] ||
						( getPropertyColumnSpan( property ) > 0 && updateability[property] );

				if ( getPropertyColumnSpan( property ) > 0 && !updateability[property] ) {
					LOG.ignoreImmutablePropertyModification( getPropertyNames()[property], getEntityName() );
				}
			}
			if ( isVersioned() ) {
				tableUpdateNeeded[0] = tableUpdateNeeded[0] ||
						Versioning.isVersionIncrementRequired(
								dirtyProperties,
								hasDirtyCollection,
								getPropertyVersionability()
						);
			}
			return tableUpdateNeeded;
		}
	}

	@Override
	public boolean hasRowId() {
		return rowIdName != null;
	}

	//used by Hibernate Reactive
	@SuppressWarnings("unused")
	public boolean[][] getPropertyColumnUpdateable() {
		return propertyColumnUpdateable;
	}

	//used by Hibernate Reactive
	@SuppressWarnings("unused")
	public boolean[][] getPropertyColumnInsertable() {
		return propertyColumnInsertable;
	}

	public String[] getTableNames() {
		String[] tableNames = new String[getTableSpan()];
		for ( int i = 0; i < tableNames.length; i++ ) {
			tableNames[i] = getTableName( i );
		}
		return tableNames;
	}

	@Deprecated(since = "6.0")
	public AbstractEntityPersister(
			final PersistentClass bootDescriptor,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final PersisterCreationContext creationContext) throws HibernateException {
		this( bootDescriptor, cacheAccessStrategy, naturalIdRegionAccessStrategy,
				(RuntimeModelCreationContext) creationContext );
	}

	public AbstractEntityPersister(
			final PersistentClass bootDescriptor,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final RuntimeModelCreationContext creationContext) throws HibernateException {

		factory = creationContext.getSessionFactory();
		sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromEntityName( bootDescriptor.getEntityName() );

		navigableRole = new NavigableRole( bootDescriptor.getEntityName() );

		final SessionFactoryOptions sessionFactoryOptions = creationContext.getSessionFactory().getSessionFactoryOptions();

		if ( sessionFactoryOptions.isSecondLevelCacheEnabled() ) {
			this.cacheAccessStrategy = cacheAccessStrategy;
			this.naturalIdRegionAccessStrategy = naturalIdRegionAccessStrategy;
			canWriteToCache = determineCanWriteToCache( bootDescriptor, cacheAccessStrategy );
			canReadFromCache = determineCanReadFromCache( bootDescriptor, cacheAccessStrategy );
			isLazyPropertiesCacheable = bootDescriptor.getRootClass().isLazyPropertiesCacheable();
		}
		else {
			this.cacheAccessStrategy = null;
			this.naturalIdRegionAccessStrategy = null;
			canWriteToCache = false;
			canReadFromCache = false;
			isLazyPropertiesCacheable = true;
		}

		entityMetamodel = new EntityMetamodel( bootDescriptor, this, creationContext );

		entityEntryFactory = entityMetamodel.isMutable()
				? MutableEntityEntryFactory.INSTANCE
				: ImmutableEntityEntryFactory.INSTANCE;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		this.representationStrategy = creationContext.getBootstrapContext().getRepresentationStrategySelector()
				.resolveStrategy( bootDescriptor, this, creationContext );

		this.javaType = representationStrategy.getLoadJavaType();
		assert javaType != null;

		final JdbcServices jdbcServices = factory.getServiceRegistry().getService( JdbcServices.class );
		final Dialect dialect = jdbcServices.getJdbcEnvironment().getDialect();

		int batch = bootDescriptor.getBatchSize();
		if ( batch == -1 ) {
			batch = factory.getSessionFactoryOptions().getDefaultBatchFetchSize();
		}
		batchSize = batch;
		hasSubselectLoadableCollections = bootDescriptor.hasSubselectLoadableCollections();

		propertyMapping = new BasicEntityPropertyMapping( this );

		// IDENTIFIER

		identifierColumnSpan = bootDescriptor.getIdentifier().getColumnSpan();
		rootTableKeyColumnNames = new String[identifierColumnSpan];
		rootTableKeyColumnReaders = new String[identifierColumnSpan];
		rootTableKeyColumnReaderTemplates = new String[identifierColumnSpan];
		identifierAliases = new String[identifierColumnSpan];

		rowIdName = bootDescriptor.getRootTable().getRowId();

		if ( bootDescriptor.getLoaderName() != null ) {
			// We must resolve the named query on-demand through the boot model because it isn't initialized yet
			final NamedQueryMemento namedQueryMemento = factory.getQueryEngine().getNamedObjectRepository()
					.resolve( factory, creationContext.getBootModel(), bootDescriptor.getLoaderName() );
			if ( namedQueryMemento == null ) {
				throw new IllegalArgumentException( "Could not resolve named load-query [" + getEntityName() + "] : " + bootDescriptor.getLoaderName() );
			}
			singleIdEntityLoader = new SingleIdEntityLoaderProvidedQueryImpl<>(
					this,
					namedQueryMemento
			);
		}
		else if ( batchSize > 1 ) {
			singleIdEntityLoader = createBatchingIdEntityLoader( this, batchSize, factory );
		}
		else {
			singleIdEntityLoader = new SingleIdEntityLoaderStandardImpl<>( this, factory );
		}

		multiIdEntityLoader = new MultiIdLoaderStandard<>( this, bootDescriptor, factory );

		SqmFunctionRegistry functionRegistry = factory.getQueryEngine().getSqmFunctionRegistry();

		List<Column> columns = bootDescriptor.getIdentifier().getColumns();
		for ( int i = 0; i < columns.size(); i++ ) {
			Column column = columns.get(i);
			rootTableKeyColumnNames[i] = column.getQuotedName( dialect );
			rootTableKeyColumnReaders[i] = column.getReadExpr( dialect );
			rootTableKeyColumnReaderTemplates[i] = column.getTemplate(
					dialect,
					factory.getTypeConfiguration(),
					functionRegistry
			);
			identifierAliases[i] = column.getAlias( dialect, bootDescriptor.getRootTable() );
		}

		// VERSION

		versionColumnName = bootDescriptor.isVersioned()
				? bootDescriptor.getVersion().getColumns().get(0).getQuotedName(dialect)
				: null;

		//WHERE STRING

		if ( StringHelper.isEmpty( bootDescriptor.getWhere() ) ) {
			sqlWhereStringTableExpression = null;
			sqlWhereStringTemplate = null;
		}
		else {
			PersistentClass containingClass = bootDescriptor;
			while ( containingClass.getSuperclass() != null ) {
				final PersistentClass superclass = containingClass.getSuperclass();
				if ( !Objects.equals( bootDescriptor.getWhere(), superclass.getWhere() ) ) {
					break;
				}
				containingClass = superclass;
			}
			this.sqlWhereStringTableExpression = determineTableName( containingClass.getTable() );
			sqlWhereStringTemplate = Template.renderWhereStringTemplate(
					"(" + bootDescriptor.getWhere() + ")",
					dialect,
					factory.getTypeConfiguration(),
					functionRegistry
			);
		}
		// PROPERTIES

		int hydrateSpan = entityMetamodel.getPropertySpan();
		propertyColumnSpans = new int[hydrateSpan];
//		propertySubclassNames = new String[hydrateSpan];
		propertyColumnAliases = new String[hydrateSpan][];
		propertyColumnNames = new String[hydrateSpan][];
		propertyColumnFormulaTemplates = new String[hydrateSpan][];
//		propertyColumnReaderTemplates = new String[hydrateSpan][];
		propertyColumnWriters = new String[hydrateSpan][];
//		propertyUniqueness = new boolean[hydrateSpan];
		propertySelectable = new boolean[hydrateSpan];
		propertyColumnUpdateable = new boolean[hydrateSpan][];
		propertyColumnInsertable = new boolean[hydrateSpan][];
		HashSet<Property> thisClassProperties = new HashSet<>();

		ArrayList<String> lazyNames = new ArrayList<>();
		ArrayList<Integer> lazyNumbers = new ArrayList<>();
		ArrayList<Type> lazyTypes = new ArrayList<>();
		ArrayList<String[]> lazyColAliases = new ArrayList<>();

		final ArrayList<Integer> lobPropertiesLocalCollector = new ArrayList<>();
		boolean foundFormula = false;
		List<Property> propertyClosure = bootDescriptor.getPropertyClosure();
		for ( int i = 0; i < propertyClosure.size(); i++ ) {
			Property prop = propertyClosure.get(i);
			thisClassProperties.add( prop );

			int span = prop.getColumnSpan();
			propertyColumnSpans[i] = span;
//			propertySubclassNames[i] = prop.getPersistentClass().getEntityName();
			String[] colNames = new String[span];
			String[] colAliases = new String[span];
//			String[] colReaderTemplates = new String[span];
			String[] colWriters = new String[span];
			String[] formulaTemplates = new String[span];
			List<Selectable> selectables = prop.getSelectables();
			for ( int k = 0; k < selectables.size(); k++ ) {
				Selectable selectable = selectables.get(k);
				colAliases[k] = selectable.getAlias( dialect, prop.getValue().getTable() );
				if ( selectable.isFormula() ) {
					foundFormula = true;
					Formula formula = (Formula) selectable;
					formula.setFormula( substituteBrackets( formula.getFormula() ) );
					formulaTemplates[k] = selectable.getTemplate(
							dialect,
							factory.getTypeConfiguration(),
							functionRegistry
					);
				}
				else {
					Column column = (Column) selectable;
					colNames[k] = column.getQuotedName( dialect );
//					colReaderTemplates[k] = column.getTemplate( dialect, factory.getQueryEngine().getSqmFunctionRegistry() );
					colWriters[k] = column.getWriteExpr();
				}
			}
			propertyColumnNames[i] = colNames;
			propertyColumnFormulaTemplates[i] = formulaTemplates;
//			propertyColumnReaderTemplates[i] = colReaderTemplates;
			propertyColumnWriters[i] = colWriters;
			propertyColumnAliases[i] = colAliases;

			final boolean lazy = !EnhancementHelper.includeInBaseFetchGroup(
					prop,
					entityMetamodel.isInstrumented(),
					entityName -> {
						final PersistentClass entityBinding =
								creationContext.getMetadata().getEntityBinding( entityName );
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

			propertyColumnUpdateable[i] = prop.getValue().getColumnUpdateability();
			propertyColumnInsertable[i] = prop.getValue().getColumnInsertability();

			propertySelectable[i] = prop.isSelectable();

//			propertyUniqueness[i] = prop.getValue().isAlternateUniqueKey();

			if ( prop.isLob() && dialect.forceLobAsLastValue() ) {
				lobPropertiesLocalCollector.add( i );
			}
		}
		this.lobProperties = CollectionHelper.toSmallList( lobPropertiesLocalCollector );
		hasFormulaProperties = foundFormula;
		lazyPropertyColumnAliases = ArrayHelper.to2DStringArray( lazyColAliases );
		lazyPropertyNames = ArrayHelper.toStringArray( lazyNames );
		lazyPropertyNumbers = ArrayHelper.toIntArray( lazyNumbers );
		lazyPropertyTypes = ArrayHelper.toTypeArray( lazyTypes );

		// SUBCLASS PROPERTY CLOSURE

//		ArrayList<String> columns = new ArrayList<>();
//		ArrayList<Boolean> columnsLazy = new ArrayList<>();
//		ArrayList<String> columnReaderTemplates = new ArrayList<>();
		ArrayList<String> aliases = new ArrayList<>();
//		ArrayList<String> formulas = new ArrayList<>();
		ArrayList<String> formulaAliases = new ArrayList<>();
//		ArrayList<String> formulaTemplates = new ArrayList<>();
//		ArrayList<Boolean> formulasLazy = new ArrayList<>();
		ArrayList<Type> types = new ArrayList<>();
		ArrayList<String> names = new ArrayList<>();
//		ArrayList<String> classes = new ArrayList<>();
		ArrayList<String[]> templates = new ArrayList<>();
		ArrayList<String[]> propColumns = new ArrayList<>();
		ArrayList<String[]> propColumnReaders = new ArrayList<>();
		ArrayList<String[]> propColumnReaderTemplates = new ArrayList<>();
		ArrayList<FetchMode> joinedFetchesList = new ArrayList<>();
		ArrayList<CascadeStyle> cascades = new ArrayList<>();
		ArrayList<Boolean> definedBySubclass = new ArrayList<>();
//		ArrayList<int[]> propColumnNumbers = new ArrayList<>();
//		ArrayList<int[]> propFormulaNumbers = new ArrayList<>();
		ArrayList<Boolean> propNullables = new ArrayList<>();

		for ( Property prop : bootDescriptor.getSubclassPropertyClosure() ) {
			names.add( prop.getName() );
//			classes.add( prop.getPersistentClass().getEntityName() );
			types.add( prop.getType() );

			final boolean isDefinedBySubclass = !thisClassProperties.contains( prop );
			definedBySubclass.add( isDefinedBySubclass );
			propNullables.add( prop.isOptional() || isDefinedBySubclass ); //TODO: is this completely correct?

			String[] cols = new String[ prop.getColumnSpan() ];
			String[] readers = new String[ prop.getColumnSpan() ];
			String[] readerTemplates = new String[ prop.getColumnSpan() ];
			String[] forms = new String[ prop.getColumnSpan() ];
//			int[] colnos = new int[ prop.getColumnSpan() ];
//			int[] formnos = new int[ prop.getColumnSpan() ];

//			final boolean lazy = ! EnhancementHelper.includeInBaseFetchGroup(
//					prop,
//					entityMetamodel.isInstrumented(),
//					entityName -> {
//						final MetadataImplementor metadata = creationContext.getMetadata();
//						final PersistentClass entityBinding = metadata.getEntityBinding( entityName );
//						assert entityBinding != null;
//						return entityBinding.hasSubclasses();
//					},
//					sessionFactoryOptions.isCollectionsInDefaultFetchGroupEnabled()
//			);
			List<Selectable> selectables = prop.getSelectables();
			for ( int i = 0; i < selectables.size(); i++ ) {
				Selectable selectable = selectables.get(i);
				if ( selectable.isFormula() ) {
					String template = selectable.getTemplate(
							dialect,
							factory.getTypeConfiguration(),
							functionRegistry
					);
//					formnos[l] = formulaTemplates.size();
//					colnos[l] = -1;
//					formulaTemplates.add( template );
					forms[i] = template;
//					formulas.add( selectable.getText( dialect ) );
					final String formulaAlias = selectable.getAlias( dialect );
					if ( prop.isSelectable() && !formulaAliases.contains( formulaAlias ) ) {
						formulaAliases.add( formulaAlias );
					}
//					formulasLazy.add( lazy );
				}
				else {
					Column column = (Column) selectable;
					String colName = column.getQuotedName(dialect);
//					colnos[l] = columns.size(); //before add :-)
//					formnos[l] = -1;
//					columns.add( colName );
					cols[i] = colName;
					final String columnAlias = selectable.getAlias( dialect, prop.getValue().getTable() );
					if ( prop.isSelectable() && !aliases.contains( columnAlias ) ) {
						aliases.add( columnAlias );
					}
//					columnsLazy.add( lazy );

					readers[i] = column.getReadExpr( dialect );
					readerTemplates[i] = column.getTemplate(
							dialect,
							factory.getTypeConfiguration(),
							functionRegistry
					);
//					columnReaderTemplates.add( readerTemplate );
				}
			}
			propColumns.add( cols );
			propColumnReaders.add( readers );
			propColumnReaderTemplates.add( readerTemplates );
			templates.add( forms );
//			propColumnNumbers.add( colnos );
//			propFormulaNumbers.add( formnos );

			joinedFetchesList.add( prop.getValue().getFetchMode() );
			cascades.add( prop.getCascadeStyle() );
		}
//		subclassColumnClosure = ArrayHelper.toStringArray( columns );
		subclassColumnAliasClosure = ArrayHelper.toStringArray( aliases );
//		subclassColumnLazyClosure = ArrayHelper.toBooleanArray( columnsLazy );
//		subclassColumnReaderTemplateClosure = ArrayHelper.toStringArray( columnReaderTemplates );

//		subclassFormulaClosure = ArrayHelper.toStringArray( formulas );
//		subclassFormulaTemplateClosure = ArrayHelper.toStringArray( formulaTemplates );
		subclassFormulaAliasClosure = ArrayHelper.toStringArray( formulaAliases );
//		subclassFormulaLazyClosure = ArrayHelper.toBooleanArray( formulasLazy );

		subclassPropertyNameClosure = ArrayHelper.toStringArray( names );
//		subclassPropertySubclassNameClosure = ArrayHelper.toStringArray( classes );
		subclassPropertyTypeClosure = ArrayHelper.toTypeArray( types );
		subclassPropertyNullabilityClosure = ArrayHelper.toBooleanArray( propNullables );
		subclassPropertyFormulaTemplateClosure = ArrayHelper.to2DStringArray( templates );
		subclassPropertyColumnNameClosure = ArrayHelper.to2DStringArray( propColumns );
		subclassPropertyColumnReaderClosure = ArrayHelper.to2DStringArray( propColumnReaders );
		subclassPropertyColumnReaderTemplateClosure = ArrayHelper.to2DStringArray( propColumnReaderTemplates );
//		subclassPropertyColumnNumberClosure = ArrayHelper.to2DIntArray( propColumnNumbers );
//		subclassPropertyFormulaNumberClosure = ArrayHelper.to2DIntArray( propFormulaNumbers );

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

		propertyDefinedOnSubclass = ArrayHelper.toBooleanArray( definedBySubclass );

		// Handle any filters applied to the class level
		filterHelper = CollectionHelper.isNotEmpty( bootDescriptor.getFilters() )
				? new FilterHelper(bootDescriptor.getFilters(), factory)
				: null;

		useReferenceCacheEntries = useReferenceCacheEntries();

		cacheEntryHelper = buildCacheEntryHelper();

		invalidateCache = sessionFactoryOptions.isSecondLevelCacheEnabled()
				&& canWriteToCache
				&& shouldInvalidateCache(bootDescriptor, creationContext);

	}

	private boolean useReferenceCacheEntries() {
		// Check if we can use Reference Cached entities in 2lc
		// todo : should really validate that the cache access type is read-only
		if ( !factory.getSessionFactoryOptions().isDirectReferenceCacheEntriesEnabled() ) {
			return false;
		}

		// for now, limit this to just entities that:
		else if ( entityMetamodel.isMutable() ) {
			// 		1) are immutable
			return false;
		}
		else {
			//		2) have no associations.
			//		Eventually we want to be a little more lenient with associations.
			for ( Type type : getSubclassPropertyTypeClosure() ) {
				if ( type.isAssociationType() ) {
					return false;
				}
			}
			return true;
		}
	}

	private static SingleIdEntityLoader<?> createBatchingIdEntityLoader(
			EntityMappingType entityDescriptor,
			int batchSize,
			SessionFactoryImplementor factory) {
		return new SingleIdEntityLoaderDynamicBatch<>( entityDescriptor, batchSize, factory );
	}

	/**
	 * We might need to use cache invalidation is we have formulas,
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
		else if ( isCacheComplianceEnabled(creationContext) ) {
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
		return creationContext.getSessionFactory()
				.getSessionFactoryOptions()
				.getJpaCompliance()
				.isJpaCacheComplianceEnabled();
	}

	private boolean determineCanWriteToCache(PersistentClass persistentClass, EntityDataAccess cacheAccessStrategy) {
		if ( cacheAccessStrategy == null ) {
			return false;
		}

		return persistentClass.isCached();
	}

	private boolean determineCanReadFromCache(PersistentClass persistentClass, EntityDataAccess cacheAccessStrategy) {
		if ( cacheAccessStrategy == null ) {
			return false;
		}

		if ( persistentClass.isCached() ) {
			return true;
		}

		for ( Subclass subclass : persistentClass.getSubclasses() ) {
			if ( subclass.isCached() ) {
				return true;
			}
		}
		return false;
	}

	protected CacheEntryHelper buildCacheEntryHelper() {
		if ( cacheAccessStrategy == null ) {
			// the entity defined no caching...
			return NoopCacheEntryHelper.INSTANCE;
		}

		if ( canUseReferenceCacheEntries() ) {
			entityMetamodel.setLazy( false );
			// todo : do we also need to unset proxy factory?
			return new ReferenceCacheEntryHelper( this );
		}

		return factory.getSessionFactoryOptions().isStructuredCacheEntriesEnabled()
				? new StructuredCacheEntryHelper( this )
				: new StandardCacheEntryHelper( this );
	}

	@Override
	public boolean canUseReferenceCacheEntries() {
		return useReferenceCacheEntries;
	}

	protected Map<String, SingleIdArrayLoadPlan> generateLazySelectStringsByFetchGroup() {
		final BytecodeEnhancementMetadata enhancementMetadata = entityMetamodel.getBytecodeEnhancementMetadata();
		if ( !enhancementMetadata.isEnhancedForLazyLoading()
				|| !enhancementMetadata.getLazyAttributesMetadata().hasLazyAttributes() ) {
			return Collections.emptyMap();
		}

		Map<String, SingleIdArrayLoadPlan> result = new HashMap<>();

		final LazyAttributesMetadata lazyAttributesMetadata = enhancementMetadata.getLazyAttributesMetadata();
		for ( String groupName : lazyAttributesMetadata.getFetchGroupNames() ) {
			final List<LazyAttributeDescriptor> fetchGroupAttributeDescriptors = lazyAttributesMetadata.getFetchGroupAttributeDescriptors(
					groupName
			);
			final List<ModelPart> partsToSelect = new ArrayList<>( fetchGroupAttributeDescriptors.size() );

			for ( LazyAttributeDescriptor lazyAttributeDescriptor : fetchGroupAttributeDescriptors ) {
				// all this only really needs to consider properties
				// of this class, not its subclasses, but since we
				// are reusing code used for sequential selects, we
				// use the subclass closure
				partsToSelect.add( getAttributeMapping( getSubclassPropertyIndex( lazyAttributeDescriptor.getName() ) ) );
			}

			if ( partsToSelect.isEmpty() ) {
				// only one-to-one is lazily fetched
				continue;
			}
			final List<JdbcParameter> jdbcParameters = new ArrayList<>();

			final SelectStatement sqlAst = LoaderSelectBuilder.createSelect(
					this,
					partsToSelect,
					getIdentifierMapping(),
					null,
					1,
					LoadQueryInfluencers.NONE,
					LockOptions.NONE,
					jdbcParameters::add,
					factory
			);

			result.put(
					groupName,
					new SingleIdArrayLoadPlan(
							getIdentifierMapping(),
							sqlAst,
							jdbcParameters,
							LockOptions.NONE,
							factory
					)
			);
		}

		return result;
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public boolean containsTableReference(String tableExpression) {
		if ( getTableName().equals( tableExpression ) ) {
			return true;
		}

		for ( int i = 0; i < getSubclassTableSpan(); i++ ) {
			if ( getSubclassTableName( i ).equals( tableExpression ) ) {
				return true;
			}
		}

		return false;
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
				resultVariable,
				creationState
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
	public EntityMappingType getEntityMappingType() {
		return this;
	}

	@Override
	public TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAliasBase sqlAliasBase,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		final TableReference primaryTableReference = createPrimaryTableReference(
				sqlAliasBase,
				sqlExpressionResolver,
				creationContext
		);

		return new StandardTableGroup(
				canUseInnerJoins,
				navigablePath,
				this,
				explicitSourceAlias,
				primaryTableReference,
				true,
				sqlAliasBase,
				(tableExpression) -> ArrayHelper.contains( getSubclassTableNames(), tableExpression ),
				(tableExpression, tg) -> {
					final String[] subclassTableNames = getSubclassTableNames();
					for ( int i = 0; i < subclassTableNames.length; i++ ) {
						if ( tableExpression.equals( subclassTableNames[ i ] ) ) {
							final NamedTableReference joinedTableReference = new NamedTableReference(
									tableExpression,
									sqlAliasBase.generateNewAlias(),
									isNullableSubclassTable( i ),
									getFactory()
							);

							return new TableReferenceJoin(
									shouldInnerJoinSubclassTable( i, Collections.emptySet() ),
									joinedTableReference,
									additionalPredicateCollectorAccess == null
											? null
											: generateJoinPredicate(
													primaryTableReference,
													joinedTableReference,
													getSubclassTableKeyColumns( i ),
													sqlExpressionResolver
											)
							);
						}
					}

					return null;
				},
				getFactory()
		);
	}

	@Override
	public TableReference createPrimaryTableReference(
			SqlAliasBase sqlAliasBase,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		return resolvePrimaryTableReference( sqlAliasBase );
	}


	@Override
	public TableReferenceJoin createTableReferenceJoin(
			String joinTableExpression,
			SqlAliasBase sqlAliasBase,
			TableReference lhs,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		for ( int i = 1; i < getSubclassTableSpan(); i++ ) {
			final String subclassTableName = getSubclassTableName( i );
			if ( subclassTableName.equals( joinTableExpression ) ) {
				return generateTableReferenceJoin(
						lhs,
						joinTableExpression,
						sqlAliasBase,
						shouldInnerJoinSubclassTable( i, Collections.emptySet() ),
						getSubclassTableKeyColumns( i ),
						sqlExpressionResolver
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
			SqlExpressionResolver sqlExpressionResolver) {
		final NamedTableReference joinedTableReference = new NamedTableReference(
				joinTableExpression,
				sqlAliasBase.generateNewAlias(),
				!innerJoin,
				getFactory()
		);

		return new TableReferenceJoin(
				innerJoin,
				joinedTableReference,
				generateJoinPredicate(
						lhs,
						joinedTableReference,
						targetColumns,
						sqlExpressionResolver
				)
		);
	}

	protected TableReference resolvePrimaryTableReference(SqlAliasBase sqlAliasBase) {
		return new NamedTableReference(
				getTableName(),
				sqlAliasBase.generateNewAlias(),
				false,
				getFactory()
		);
	}

	protected Predicate generateJoinPredicate(
			TableReference rootTableReference,
			TableReference joinedTableReference,
			String[] fkColumnNames,
			SqlExpressionResolver sqlExpressionResolver) {
		final EntityIdentifierMapping identifierMapping = getIdentifierMapping();

		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );

		final String[] rootPkColumnNames = getKeyColumnNames();

		assert rootPkColumnNames.length == fkColumnNames.length;
		assert rootPkColumnNames.length == identifierMapping.getJdbcTypeCount();

		identifierMapping.forEachSelectable(
				(columnIndex, selection) -> {
					final String rootPkColumnName = rootPkColumnNames[ columnIndex ];
					final Expression pkColumnExpression = sqlExpressionResolver.resolveSqlExpression(
							SqlExpressionResolver.createColumnReferenceKey(
									rootTableReference,
									rootPkColumnName
							),
							sqlAstProcessingState -> new ColumnReference(
									rootTableReference.getIdentificationVariable(),
									rootPkColumnName,
									false,
									null,
									null,
									selection.getJdbcMapping(),
									getFactory()
							)
					);

					final String fkColumnName = fkColumnNames[ columnIndex ];
					final Expression fkColumnExpression = sqlExpressionResolver.resolveSqlExpression(
							SqlExpressionResolver.createColumnReferenceKey(
									joinedTableReference,
									fkColumnName
							),
							sqlAstProcessingState -> new ColumnReference(
									joinedTableReference.getIdentificationVariable(),
									fkColumnName,
									false,
									null,
									null,
									selection.getJdbcMapping(),
									getFactory()
							)
					);

					conjunction.add( new ComparisonPredicate( pkColumnExpression, ComparisonOperator.EQUAL, fkColumnExpression ) );
				}
		);

		return conjunction;
	}

	protected Predicate generateJoinPredicate(
			TableReference rootTableReference,
			TableReference joinedTableReference,
			int subClassTablePosition,
			SqlExpressionResolver sqlExpressionResolver) {
		return generateJoinPredicate(
				rootTableReference,
				joinedTableReference,
				getSubclassTableKeyColumns( subClassTablePosition ),
				sqlExpressionResolver
		);
	}

	@Override
	public Object initializeLazyProperty(String fieldName, Object entity, SharedSessionContractImplementor session) {
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final EntityEntry entry = persistenceContext.getEntry( entity );
		final PersistentAttributeInterceptor interceptor = asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
		assert interceptor != null : "Expecting bytecode interceptor to be non-null";

		if ( hasCollections() ) {
			final Type type = getPropertyType( fieldName );
			if ( type.isCollectionType() ) {
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
					"Initializing lazy properties of: {0}, field access: {1}", MessageHelper.infoString(
							this,
							id,
							getFactory()
					), fieldName
			);
		}

		if ( session.getCacheMode().isGetEnabled() && canReadFromCache() && isLazyPropertiesCacheable() ) {
			final EntityDataAccess cacheAccess = getCacheAccessStrategy();
			final Object cacheKey = cacheAccess.generateCacheKey(id, this, session.getFactory(), session.getTenantIdentifier() );
			final Object ce = CacheHelper.fromSharedCache( session, cacheKey, cacheAccess );
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

	public Object getCollectionKey(
			CollectionPersister persister,
			Object owner,
			EntityEntry ownerEntry,
			SharedSessionContractImplementor session) {
		final CollectionType collectionType = persister.getCollectionType();

		if ( ownerEntry != null ) {
			// this call only works when the owner is associated with the Session, which is not always the case
			return collectionType.getKeyOfOwner( owner, session );
		}

		if ( collectionType.getLHSPropertyName() == null ) {
			// collection key is defined by the owning entity identifier
			return persister.getOwnerEntityPersister().getIdentifier( owner, session );
		}
		else {
			return persister.getOwnerEntityPersister().getPropertyValue( owner, collectionType.getLHSPropertyName() );
		}
	}

	protected Object initializeLazyPropertiesFromDatastore(
			final Object entity,
			final Object id,
			final EntityEntry entry,
			final String fieldName,
			final SharedSessionContractImplementor session) {

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
				final boolean previousInitialized = initializedLazyAttributeNames.contains( fetchGroupAttributeDescriptor.getName() );

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
						fetchGroupAttributeDescriptor.getLazyIndex(),
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
					"could not initialize lazy properties: " + MessageHelper.infoString( this, id, getFactory() ),
					lazySelect.getJdbcSelect().getSql()
			);
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
		Object[] disassembledValues = cacheEntry.getDisassembledState();
		for ( int j = 0; j < lazyPropertyNames.length; j++ ) {
			final Object cachedValue = disassembledValues[lazyPropertyNumbers[j]];
			final Type lazyPropertyType = lazyPropertyTypes[j];
			final String propertyName = lazyPropertyNames[j];
			if (cachedValue == LazyPropertyInitializer.UNFETCHED_PROPERTY) {
				if (fieldName.equals(propertyName)) {
					result = LazyPropertyInitializer.UNFETCHED_PROPERTY;
				}
				// don't try to initialize the unfetched property
			}
			else {
				final Object propValue = lazyPropertyType.assemble(
						cachedValue,
						session,
						entity
				);
				if ( initializeLazyProperty( fieldName, entity, entry, j, propValue ) ) {
					result = propValue;
				}
			}
		}

		LOG.trace( "Done initializing lazy properties" );

		return result;
	}

	// called by Hibernate Reactive
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

	public boolean isBatchable() {
		return optimisticLockStyle().isNone()
				|| !isVersioned() && optimisticLockStyle().isVersion()
				|| getFactory().getSessionFactoryOptions().isJdbcBatchVersionedData();
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
				getFactory()
		);

		final NavigablePath entityPath = new NavigablePath( getRootPathName() );
		final TableGroup rootTableGroup = createRootTableGroup(
				true,
				entityPath,
				null,
				() -> p -> {},
				new SqlAliasBaseConstant( alias ),
				sqlAstCreationState.getSqlExpressionResolver(),
				sqlAstCreationState.getFromClauseAccess(),
				getFactory()
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( entityPath, rootTableGroup );

		createDomainResult( entityPath, rootTableGroup, null, sqlAstCreationState );

		// Wrap expressions with aliases
		final SelectClause selectClause = rootQuerySpec.getSelectClause();
		final List<SqlSelection> sqlSelections = selectClause.getSqlSelections();
		int i = 0;
		for ( String identifierAlias : identifierAliases ) {
			sqlSelections.set(
					i,
					new SqlSelectionImpl(
							i,
							i + 1,
							new AliasedExpression( sqlSelections.get( i ).getExpression(), identifierAlias + suffix )
					)
			);
			i++;
		}

		if ( entityMetamodel.hasSubclasses() ) {
			sqlSelections.set(
					i,
					new SqlSelectionImpl(
							i,
							i + 1,
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
							i + 1,
							new AliasedExpression( sqlSelections.get( i ).getExpression(), ROWID_ALIAS + suffix )
					)
			);
			i++;
		}

		final String[] columnAliases = getSubclassColumnAliasClosure();
		final String[] formulaAliases = getSubclassFormulaAliasClosure();
		int columnIndex = 0;
		int formulaIndex = 0;
		for ( ; i < sqlSelections.size(); i++ ) {
			final SqlSelection sqlSelection = sqlSelections.get( i );
			final ColumnReference columnReference = (ColumnReference) sqlSelection.getExpression();
			final String selectAlias;
			if ( !columnReference.isColumnExpressionFormula() ) {
				selectAlias = columnAliases[columnIndex++] + suffix;
			}
			else {
				selectAlias = formulaAliases[formulaIndex++] + suffix;
			}
			sqlSelections.set(
					i,
					new SqlSelectionImpl(
							sqlSelection.getValuesArrayPosition(),
							sqlSelection.getJdbcResultSetIndex(),
							new AliasedExpression( sqlSelection.getExpression(), selectAlias )
					)
			);
		}

		final String sql = getFactory().getJdbcServices()
				.getDialect()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( getFactory(), new SelectStatement( rootQuerySpec ) )
				.translate( null, QueryOptions.NONE )
				.getSql();
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

	private List<Fetch> fetchProcessor(FetchParent fetchParent, QuerySpec querySpec, LoaderSqlAstCreationState creationState) {
		final FetchableContainer fetchableContainer = fetchParent.getReferencedMappingContainer();
		final int size = fetchableContainer.getNumberOfFetchables();
		final List<Fetch> fetches = new ArrayList<>( size );

		for ( int i = 0; i < size; i++ ) {
			final Fetchable fetchable = fetchableContainer.getFetchable( i );
			// Ignore plural attributes
			if ( !( fetchable instanceof PluralAttributeMapping ) ) {
				final FetchTiming fetchTiming;
				if ( fetchable instanceof BasicValuedModelPart ) {
					// Ignore lazy basic columns
					fetchTiming = fetchable.getMappedFetchOptions().getTiming();
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
					fetchTiming = FetchTiming.DELAYED;
				}
				else {
					fetchTiming = fetchable.getMappedFetchOptions().getTiming();
				}

				if ( fetchTiming == null ) {
					throw new AssertionFailure("fetchTiming was null");
				}

				if ( isSelectable( fetchParent, fetchable ) ) {
					final Fetch fetch = fetchParent.generateFetchableFetch(
							fetchable,
							fetchParent.resolveNavigablePath( fetchable ),
							fetchTiming,
							true,
							null,
							creationState
					);
					fetches.add( fetch );
				}
			}
		}

		return fetches;
	}

	private boolean isSelectable(FetchParent fetchParent, Fetchable fetchable) {
		if ( fetchParent instanceof EmbeddableResultGraphNode ) {
			return true;
		}
		else {
			final AttributeMapping attributeMapping = fetchable.asAttributeMapping();
			if ( attributeMapping != null ) {
				final int propertyNumber = attributeMapping.getStateArrayPosition();
//				final int tableNumber = getSubclassPropertyTableNumber( propertyNumber );
//				return !isSubclassTableSequentialSelect( tableNumber ) && propertySelectable[propertyNumber];
				return propertySelectable[propertyNumber];
			}
			else {
				return true;
			}
		}
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
		return entityMetamodel.hasSubclasses() ?
				new Alias( suffix ).toAliasString( getDiscriminatorAlias() ) :
				null;
	}

	@Override
	public Object[] getDatabaseSnapshot(Object id, SharedSessionContractImplementor session) throws HibernateException {
		return singleIdEntityLoader.loadDatabaseSnapshot( id, session );
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

		return getUniqueKeyLoader( uniquePropertyName ).resolveId( key, session );
	}


	/**
	 * Generate the SQL that selects the version number by id
	 */
	public String generateSelectVersionString() {
		SimpleSelect select = new SimpleSelect( getFactory().getJdbcServices().getDialect() )
				.setTableName( getVersionedTableName() );
		if ( isVersioned() ) {
			select.addColumn( getVersionColumnName(), VERSION_COLUMN_ALIAS );
		}
		else {
			select.addColumns( rootTableKeyColumnNames );
		}
		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			select.setComment( "get version " + getEntityName() );
		}
		return select.addCondition( rootTableKeyColumnNames, "=?" ).toStatementString();
	}

	private GeneratedValuesProcessor createGeneratedValuesProcessor(GenerationTiming timing) {
		return new GeneratedValuesProcessor( this, timing, getFactory() );
	}

	@Override
	public Object forceVersionIncrement(Object id, Object currentVersion, SharedSessionContractImplementor session) {
		if ( !isVersioned() ) {
			throw new AssertionFailure( "cannot force version increment on non-versioned entity" );
		}

		if ( isVersionPropertyGenerated() ) {
			// the difficulty here is exactly what we update in order to
			// force the version to be incremented in the db...
			throw new HibernateException( "LockMode.FORCE is currently not supported for generated version properties" );

		}
		final EntityVersionMapping versionMapping = getVersionMapping();
		final Object nextVersion = getVersionJavaType().next(
				currentVersion,
				versionMapping.getLength(),
				versionMapping.getPrecision(),
				versionMapping.getScale(),
				session
		);
		if ( LOG.isTraceEnabled() ) {
			LOG.trace(
					"Forcing version increment [" + MessageHelper.infoString( this, id, getFactory() ) + "; "
							+ getVersionType().toLoggableString( currentVersion, getFactory() ) + " -> "
							+ getVersionType().toLoggableString( nextVersion, getFactory() ) + "]"
			);
		}

		// todo : cache this sql...
		String versionIncrementString = generateVersionIncrementUpdateString();
		PreparedStatement st;
		try {
			st = session
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( versionIncrementString, false );
			try {
				getVersionType().nullSafeSet( st, nextVersion, 1, session );
				getIdentifierType().nullSafeSet( st, id, 2, session );
				getVersionType().nullSafeSet( st, currentVersion, 2 + getIdentifierColumnSpan(), session );
				int rows = session.getJdbcCoordinator().getResultSetReturn().executeUpdate( st );
				if ( rows != 1 ) {
					throw new StaleObjectStateException( getEntityName(), id );
				}
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not retrieve version: " +
							MessageHelper.infoString( this, id, getFactory() ),
					getVersionSelectString()
			);
		}

		return nextVersion;
	}

	private String generateVersionIncrementUpdateString() {
		Update update = createUpdate().setTableName( getTableName( 0 ) );
		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			update.setComment( "forced version increment" );
		}
		update.addColumn( getVersionColumnName() );
		update.addPrimaryKeyColumns( rootTableKeyColumnNames );
		update.setVersionColumnName( getVersionColumnName() );
		return update.toStatementString();
	}

	/**
	 * Retrieve the version number
	 */
	@Override
	public Object getCurrentVersion(Object id, SharedSessionContractImplementor session) throws HibernateException {

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Getting version: {0}", MessageHelper.infoString( this, id, getFactory() ) );
		}

		try {
			PreparedStatement st = session
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( getVersionSelectString() );
			try {
				getIdentifierType().nullSafeSet( st, id, 1, session );
				ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( st );
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
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, st );
				}
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"could not retrieve version: " + MessageHelper.infoString( this, id, getFactory() ),
					getVersionSelectString()
			);
		}
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
		String rootPropertyName = StringHelper.root( propertyPath );
		Type type = propertyMapping.toType( rootPropertyName );
		if ( type.isAssociationType() ) {
			AssociationType assocType = (AssociationType) type;
			if ( assocType.useLHSPrimaryKey() ) {
				// performance op to avoid the array search
				return 0;
			}
			else if ( type.isCollectionType() ) {
				// properly handle property-ref-based associations
				rootPropertyName = assocType.getLHSPropertyName();
			}
		}
		//Enable for HHH-440, which we don't like:
		/*if ( type.isComponentType() && !propertyName.equals(rootPropertyName) ) {
			String unrooted = StringHelper.unroot(propertyName);
			int idx = ArrayHelper.indexOf( getSubclassColumnClosure(), unrooted );
			if ( idx != -1 ) {
				return getSubclassColumnTableNumberClosure()[idx];
			}
		}*/
		int index = ArrayHelper.indexOf( getSubclassPropertyNameClosure(), rootPropertyName ); //TODO: optimize this better!
		return index == -1 ? 0 : getSubclassPropertyTableNumber( index );
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
			return ( (EmbeddableValuedModelPart) attributeMapping ).getContainingTableExpression();
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

	private DiscriminatorMetadata discriminatorMetadata;

	@Override
	public DiscriminatorMetadata getTypeDiscriminatorMetadata() {
		if ( discriminatorMetadata == null ) {
			discriminatorMetadata = buildTypeDiscriminatorMetadata();
		}
		return discriminatorMetadata;
	}

	private DiscriminatorMetadata buildTypeDiscriminatorMetadata() {
		return () -> {
			final BasicType<?> type = (BasicType<?>) getDiscriminatorType();
			return type == null ? null : new DiscriminatorType<>( type, AbstractEntityPersister.this );
		};
	}

	public static String generateTableAlias(String rootAlias, int tableNumber) {
		if ( tableNumber == 0 ) {
			return rootAlias;
		}
		StringBuilder buf = new StringBuilder().append( rootAlias );
		if ( !rootAlias.endsWith( "_" ) ) {
			buf.append( '_' );
		}
		return buf.append( tableNumber ).append( '_' ).toString();
	}

	@Override
	public String[] toColumns(String name, final int i) {
		final String alias = generateTableAlias( name, getSubclassPropertyTableNumber( i ) );
		String[] cols = getSubclassPropertyColumnNames( i );
		String[] templates = getSubclassPropertyFormulaTemplateClosure()[i];
		String[] result = new String[cols.length];
		for ( int j = 0; j < cols.length; j++ ) {
			if ( cols[j] == null ) {
				result[j] = StringHelper.replace( templates[j], Template.TEMPLATE, alias );
			}
			else {
				result[j] = StringHelper.qualify( alias, cols[j] );
			}
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
		for ( final AttributeMapping attributeMapping : attributeMappings.iterateAsAttributeMappings() ) {
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

		return ArrayHelper.toIntArray( fields );
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

			// Sort attribute names so that we can traverse mappings efficiently
			Arrays.sort( attributeNames );
			int index = 0;
			for ( final AttributeMapping attributeMapping : attributeMappings.iterateAsAttributeMappings() ) {
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

		return ArrayHelper.toIntArray( fields );
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

	// returns the aliases of the selectable columns
	protected String[] getSubclassColumnAliasClosure() {
		return subclassColumnAliasClosure;
	}

	protected String[] getSubclassFormulaAliasClosure() {
		return subclassFormulaAliasClosure;
	}

	@Override
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

	@Override
	public String[] getSubclassPropertyColumnNames(String propertyName) {
		//TODO: should we allow suffixes on these ?
		return subclassPropertyColumnNames.get( propertyName );
	}


	//This is really ugly, but necessary:

	/**
	 * Must be called by subclasses, at the end of their constructors
	 */
	protected void initSubclassPropertyAliasesMap(PersistentClass model) throws MappingException {

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
		if ( getIdentifierType().isComponentType() ) {
			// Fetch embedded identifiers property names from the "virtual" identifier component
			CompositeType componentId = (CompositeType) getIdentifierType();
			String[] idPropertyNames = componentId.getPropertyNames();
			String[] idAliases = getIdentifierAliases();
			String[] idColumnNames = getIdentifierColumnNames();

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
			String name = path == null ? property.getName() : path + "." + property.getName();
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

	// used by Hibernate Reactive
	@SuppressWarnings("unused")
	protected String[][] getLazyPropertyColumnAliases() {
		return lazyPropertyColumnAliases;
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
		return getUniqueKeyLoader( propertyName ).load( uniqueKey, LockOptions.NONE, readOnly, session );
	}

	private Map<SingularAttributeMapping, SingleUniqueKeyEntityLoader<?>> uniqueKeyLoadersNew;

	protected SingleUniqueKeyEntityLoader<?> getUniqueKeyLoader(String attributeName) {
		final SingularAttributeMapping attribute = (SingularAttributeMapping) findByPath( attributeName );
		final SingleUniqueKeyEntityLoader<?> existing;
		if ( uniqueKeyLoadersNew == null ) {
			uniqueKeyLoadersNew = new IdentityHashMap<>();
			existing = null;
		}
		else {
			existing = uniqueKeyLoadersNew.get( attribute );
		}

		if ( existing != null ) {
			return existing;
		}

		final SingleUniqueKeyEntityLoader<?> loader = new SingleUniqueKeyEntityLoaderStandard<>( this, attribute );
		uniqueKeyLoadersNew.put( attribute, loader );

		return loader;
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
		catch (StaleStateException e) {
			if ( !isNullableTable( tableNumber ) ) {
				final StatisticsImplementor statistics = getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.optimisticFailure( getEntityName() );
				}
				throw new StaleObjectStateException( getEntityName(), id );
			}
			return false;
		}
		catch (TooManyRowsAffectedException e) {
			throw new HibernateException(
					"Duplicate identifier in table for: " +
							MessageHelper.infoString( this, id, getFactory() )
			);
		}
		catch (Throwable t) {
			return false;
		}
		return true;
	}

	private static final boolean[] SINGLE_TRUE = new boolean[] { true };

	public String generateUpdateString(boolean[] includeProperty, int j, boolean useRowId) {
		return generateUpdateString( includeProperty, j, null, useRowId );
	}

	/**
	 * Generate the SQL that updates a row by id (and version)
	 */
	public String generateUpdateString(
			final boolean[] includeProperty,
			final int j,
			final Object[] oldFields,
			final boolean useRowId) {
		final Update update = createUpdate().setTableName( getTableName( j ) );

		boolean hasColumns = false;
		for ( int index = 0; index < attributeMappings.size(); index++ ) {
			final AttributeMapping attributeMapping = attributeMappings.getAttributeMapping( index );
			if ( isPropertyOfTable( index, j ) ) {
				// `attributeMapping` is an attribute of the table we are updating

				if ( ! lobProperties.contains( index ) ) {
					// HHH-4635
					// Oracle expects all Lob properties to be last in inserts
					// and updates.  Insert them at the end - see below

					if ( includeProperty[ index ] ) {
						update.addColumns(
								getPropertyColumnNames( index ),
								propertyColumnUpdateable[index ],
								propertyColumnWriters[index]
						);
						hasColumns = true;
					}
					else {
						final ValueGeneration valueGeneration = attributeMapping.getValueGeneration();
						if ( valueGeneration.getGenerationTiming().includesUpdate()
								&& valueGeneration.getValueGenerator() == null
								&& valueGeneration.referenceColumnInSql() ) {
							update.addColumns(
									getPropertyColumnNames( index ),
									SINGLE_TRUE,
									new String[] { valueGeneration.getDatabaseGeneratedReferencedColumnValue() }
							);
							hasColumns = true;
						}
					}
				}
			}
		}

		// HHH-4635
		// Oracle expects all Lob properties to be last in inserts
		// and updates.  Insert them at the end.
		for ( int i : lobProperties ) {
			if ( includeProperty[i] && isPropertyOfTable( i, j ) ) {
				// this property belongs on the table and is to be inserted
				update.addColumns(
						getPropertyColumnNames( i ),
						propertyColumnUpdateable[i], propertyColumnWriters[i]
				);
				hasColumns = true;
			}
		}

		// select the correct row by either pk or row id
		if ( useRowId ) {
			update.addPrimaryKeyColumns( new String[] {rowIdName} ); //TODO: eventually, rowIdName[j]
		}
		else {
			update.addPrimaryKeyColumns( getKeyColumns( j ) );
		}

		if ( j == 0 && isVersioned() && entityMetamodel.getOptimisticLockStyle().isVersion() ) {
			// this is the root (versioned) table, and we are using version-based
			// optimistic locking;  if we are not updating the version, also don't
			// check it (unless this is a "generated" version column)!
			if ( checkVersion( includeProperty ) ) {
				update.setVersionColumnName( getVersionColumnName() );
				hasColumns = true;
			}
		}
		else if ( isAllOrDirtyOptLocking() && oldFields != null ) {
			// we are using "all" or "dirty" property-based optimistic locking

			boolean[] includeInWhere = entityMetamodel.getOptimisticLockStyle().isAll()
					//optimistic-lock="all", include all updatable properties
					? getPropertyUpdateability()
					//optimistic-lock="dirty", include all properties we are updating this time
					: includeProperty;

			boolean[] versionability = getPropertyVersionability();
			Type[] types = getPropertyTypes();
			for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
				boolean include = includeInWhere[i] &&
						isPropertyOfTable( i, j ) &&
						versionability[i];
				if ( include ) {
					// this property belongs to the table, and it is not specifically
					// excluded from optimistic locking by optimistic-lock="false"
					String[] propertyColumnNames = getPropertyColumnNames( i );
					String[] propertyColumnWriters = getPropertyColumnWriters( i );
					boolean[] propertyNullness = types[i].toColumnNullness( oldFields[i], getFactory() );
					for ( int k = 0; k < propertyNullness.length; k++ ) {
						if ( propertyNullness[k] ) {
							update.addWhereColumn( propertyColumnNames[k], "=" + propertyColumnWriters[k] );
						}
						else {
							update.addWhereColumn( propertyColumnNames[k], " is null" );
						}
					}
				}
			}

		}

		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			update.setComment( "update " + getEntityName() );
		}

		return hasColumns ? update.toStatementString() : null;
	}

	public final boolean checkVersion(final boolean[] includeProperty) {
		return includeProperty[getVersionProperty()]
				|| entityMetamodel.isVersionGenerated();
	}

	public String generateInsertString(boolean[] includeProperty) {
		return generateInsertString( includeProperty, 0 );
	}

	/**
	 * Generate the SQL that inserts a row
	 */
	public String generateInsertString(boolean[] includeProperty, int j) {

		final Insert insert = createInsert().setTableName( getTableName( j ) );

		for ( int index = 0; index < attributeMappings.size(); index++ ) {
			final AttributeMapping attributeMapping = attributeMappings.getAttributeMapping( index );
			if ( isPropertyOfTable( index, j ) ) {
				// `attributeMapping` is an attribute of the table we are updating

				if ( ! lobProperties.contains( index ) ) {
					// HHH-4635
					// Oracle expects all Lob properties to be last in inserts
					// and updates.  Insert them at the end - see below

					if ( includeProperty[ index ] ) {
						insert.addColumns(
								getPropertyColumnNames( index ),
								propertyColumnInsertable[index ],
								propertyColumnWriters[index]
						);
					}
					else {
						final ValueGeneration valueGeneration = attributeMapping.getValueGeneration();
						if ( valueGeneration.getGenerationTiming().includesInsert()
								&& valueGeneration.getValueGenerator() == null
								&& valueGeneration.referenceColumnInSql() ) {
							insert.addColumns(
									getPropertyColumnNames( index ),
									SINGLE_TRUE,
									new String[] { valueGeneration.getDatabaseGeneratedReferencedColumnValue() }
							);
						}
					}
				}
			}
		}

		// add the discriminator
		if ( j == 0 ) {
			addDiscriminatorToInsert( insert );
		}

		// add the primary key
		insert.addColumns( getKeyColumns( j ) );

		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			insert.setComment( "insert " + getEntityName() );
		}

		// HHH-4635
		// Oracle expects all Lob properties to be last in inserts
		// and updates.  Insert them at the end.
		for ( int i : lobProperties ) {
			if ( includeProperty[i] && isPropertyOfTable( i, j ) ) {
				// this property belongs on the table and is to be inserted
				insert.addColumns(
						getPropertyColumnNames( i ),
						propertyColumnInsertable[i],
						propertyColumnWriters[i]
				);
			}
		}

		return insert.toStatementString();
	}

	/**
	 * Used to generate an insert statement against the root table in the
	 * case of identifier generation strategies where the insert statement
	 * executions actually generates the identifier value.
	 *
	 * @param includeProperty indices of the properties to include in the
	 * insert statement.
	 *
	 * @return The insert SQL statement string
	 */
	public String generateIdentityInsertString(boolean[] includeProperty) {
		Insert insert = identityDelegate.prepareIdentifierGeneratingInsert( factory.getSqlStringGenerationContext() );
		insert.setTableName( getTableName( 0 ) );

		// add normal properties except lobs
		for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
			if ( isPropertyOfTable( i, 0 ) && !lobProperties.contains( i ) ) {
				final InDatabaseValueGenerationStrategy generationStrategy = entityMetamodel.getInDatabaseValueGenerationStrategies()[i];

				if ( includeProperty[i] ) {
					insert.addColumns(
							getPropertyColumnNames( i ),
							propertyColumnInsertable[i],
							propertyColumnWriters[i]
					);
				}
				else if ( generationStrategy != null &&
						generationStrategy.getGenerationTiming().includesInsert() &&
						generationStrategy.referenceColumnsInSql() ) {

					final String[] values;

					if ( generationStrategy.getReferencedColumnValues() == null ) {
						values = propertyColumnWriters[i];
					}
					else {
						values = new String[propertyColumnWriters[i].length];

						for ( int j = 0; j < values.length; j++ ) {
							values[j] = ( generationStrategy.getReferencedColumnValues()[j] != null ) ?
									generationStrategy.getReferencedColumnValues()[j] :
									propertyColumnWriters[i][j];
						}
					}
					insert.addColumns(
							getPropertyColumnNames( i ),
							propertyColumnInsertable[i],
							values
					);
				}
			}
		}

		// HHH-4635 & HHH-8103
		// Oracle expects all Lob properties to be last in inserts
		// and updates.  Insert them at the end.
		for ( int i : lobProperties ) {
			if ( includeProperty[i] && isPropertyOfTable( i, 0 ) ) {
				insert.addColumns( getPropertyColumnNames( i ), propertyColumnInsertable[i], propertyColumnWriters[i] );
			}
		}

		// add the discriminator
		addDiscriminatorToInsert( insert );

		// delegate already handles PK columns

		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			insert.setComment( "insert " + getEntityName() );
		}

		return insert.toStatementString();
	}

	/**
	 * Generate the SQL that deletes a row by id (and version)
	* @deprecated Use {@link #generateDeleteString(int, boolean)} instead
	 */
	@Deprecated
	public String generateDeleteString(int j) {
		return generateDeleteString( j, true );
	}

	/**
	 * Generate the SQL that deletes a row by id (and version)
	 */
	public String generateDeleteString(int j, boolean includeVersion) {
		final Delete delete = createDelete().setTableName( getTableName( j ) )
				.addPrimaryKeyColumns( getKeyColumns( j ) );
		if ( includeVersion && j == 0 ) {
			delete.setVersionColumnName( getVersionColumnName() );
		}
		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			delete.setComment( "delete " + getEntityName() );
		}
		return delete.toStatementString();
	}

	public int dehydrate(
			Object id,
			Object[] fields,
			boolean[] includeProperty,
			boolean[][] includeColumns,
			int j,
			PreparedStatement st,
			SharedSessionContractImplementor session,
			boolean isUpdate) throws HibernateException, SQLException {
		return dehydrate( id, fields, null, includeProperty, includeColumns, j, st, session, 1, isUpdate );
	}

	/**
	 * Marshall the fields of a persistent instance to a prepared statement
	 */
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
			LOG.tracev( "Dehydrating entity: {0}", MessageHelper.infoString( this, id, getFactory() ) );
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

	public boolean useGetGeneratedKeys() {
		return getFactory().getSessionFactoryOptions().isGetGeneratedKeysEnabled();
	}

	/**
	 * Perform an SQL INSERT, and then retrieve a generated identifier.
	 * <p/>
	 * This form is used for PostInsertIdentifierGenerator-style ids (IDENTITY,
	 * select, etc).
	 */
	public Object insert(
			final Object[] fields,
			final boolean[] notNull,
			String sql,
			final Object object,
			final SharedSessionContractImplementor session) throws HibernateException {

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Inserting entity: {0} (native id)", getEntityName() );
			if ( isVersioned() ) {
				LOG.tracev( "Version: {0}", Versioning.getVersion( fields, this ) );
			}
		}

		Binder binder = new Binder() {
			@Override
			public void bindValues(PreparedStatement ps) throws SQLException {
				dehydrate( null, fields, notNull, propertyColumnInsertable, 0, ps, session, false );
			}

			@Override
			public Object getEntity() {
				return object;
			}
		};

		return identityDelegate.performInsert( sql, session, binder );
	}

	@Override
	public String getIdentitySelectString() {
		//TODO: cache this in an instvar
		return getFactory().getJdbcServices().getDialect().getIdentityColumnSupport()
				.getIdentitySelectString(
						getTableName( 0 ),
						getKeyColumns( 0 )[0],
						( (BasicType<?>) getIdentifierType() ).getJdbcType().getDefaultSqlTypeCode()
				);
	}

	@Override
	public String getSelectByUniqueKeyString(String propertyName) {
		return new SimpleSelect( getFactory().getJdbcServices().getDialect() )
				.setTableName( getTableName( 0 ) )
				.addColumns( getKeyColumns( 0 ) )
				.addCondition( getPropertyColumnNames( propertyName ), "=?" )
				.toStatementString();
	}

	private BasicBatchKey insertBatchKey;

	/**
	 * Perform an SQL INSERT.
	 * <p/>
	 * This for is used for all non-root tables as well as the root table
	 * in cases where the identifier value is known before the insert occurs.
	 */
	public void insert(
			final Object id,
			final Object[] fields,
			final boolean[] notNull,
			final int j,
			final String sql,
			final Object object,
			final SharedSessionContractImplementor session) throws HibernateException {

		if ( isInverseTable( j ) ) {
			return;
		}

		//note: it is conceptually possible that a UserType could map null to
		//	  a non-null value, so the following is arguable:
		if ( isNullableTable( j ) && isAllNull( fields, j ) ) {
			return;
		}

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Inserting entity: {0}", MessageHelper.infoString( this, id, getFactory() ) );
			if ( j == 0 && isVersioned() ) {
				LOG.tracev( "Version: {0}", Versioning.getVersion( fields, this ) );
			}
		}

		// TODO : shouldn't inserts be Expectations.NONE?
		final Expectation expectation = Expectations.appropriateExpectation( insertResultCheckStyles[j] );
		final boolean useBatch = expectation.canBeBatched()
				&& session.getConfiguredJdbcBatchSize() > 1
				&& getIdentifierGenerator().supportsJdbcBatchInserts();
		if ( useBatch && insertBatchKey == null ) {
			insertBatchKey = new BasicBatchKey(
					getEntityName() + "#INSERT",
					expectation
			);
		}
		final boolean callable = isInsertCallable( j );

		try {
			// Render the SQL query
			final PreparedStatement insert;
			if ( useBatch ) {
				insert = session
						.getJdbcCoordinator()
						.getBatch( insertBatchKey )
						.getBatchStatement( sql, callable );
			}
			else {
				insert = session
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql, callable );
			}

			try {
				int index = 1;
				index += expectation.prepare( insert );

				// Write the values of fields onto the prepared statement - we MUST use the state at the time the
				// insert was issued (cos of foreign key constraints). Not necessarily the object's current state

				dehydrate( id, fields, null, notNull, propertyColumnInsertable, j, insert, session, index, false );

				if ( useBatch ) {
					session.getJdbcCoordinator().getBatch( insertBatchKey ).addToBatch();
				}
				else {
					expectation.verifyOutcome(
							session.getJdbcCoordinator()
									.getResultSetReturn()
									.executeUpdate( insert ), insert, -1, sql
					);
				}
			}
			catch (SQLException | RuntimeException e) {
				if ( useBatch ) {
					session.getJdbcCoordinator().abortBatch();
				}
				throw e;
			}
			finally {
				if ( !useBatch ) {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( insert );
					session.getJdbcCoordinator().afterStatementExecution();
				}
			}
		}
		catch (SQLException e) {
			throw getFactory().getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"could not insert: " + MessageHelper.infoString( this ),
					sql
			);
		}

	}

	/**
	 * Perform an SQL UPDATE or SQL INSERT
	 */
	public void updateOrInsert(
			final Object id,
			final Object[] fields,
			final Object[] oldFields,
			final Object rowId,
			final boolean[] includeProperty,
			final int j,
			final Object oldVersion,
			final Object object,
			final String sql,
			final SharedSessionContractImplementor session) throws HibernateException {

		if ( !isInverseTable( j ) ) {

			final boolean isRowToUpdate;
			if ( isNullableTable( j ) && oldFields != null && isAllNull( oldFields, j ) ) {
				//don't bother trying to update, we know there is no row there yet
				isRowToUpdate = false;
			}
			else if ( isNullableTable( j ) && isAllNull( fields, j ) ) {
				//if all fields are null, we might need to delete existing row
				isRowToUpdate = true;
				delete( id, oldVersion, j, object, getSQLDeleteStrings()[j], session, null );
			}
			else {
				//there is probably a row there, so try to update
				//if no rows were updated, we will find out
				isRowToUpdate = update(
						id,
						fields,
						oldFields,
						rowId,
						includeProperty,
						j,
						oldVersion,
						object,
						sql,
						session
				);
			}

			if ( !isRowToUpdate && !isAllNull( fields, j ) ) {
				// assume that the row was not there since it previously had only null
				// values, so do an INSERT instead
				//TODO: does not respect dynamic-insert
				insert( id, fields, getPropertyInsertability(), j, getSQLInsertStrings()[j], object, session );
			}

		}

	}

	private BasicBatchKey updateBatchKey;

	public boolean update(
			final Object id,
			final Object[] fields,
			final Object[] oldFields,
			final Object rowId,
			final boolean[] includeProperty,
			final int j,
			final Object oldVersion,
			final Object object,
			final String sql,
			final SharedSessionContractImplementor session) throws HibernateException {

		final Expectation expectation = Expectations.appropriateExpectation( updateResultCheckStyles[j] );
		// IMPLEMENTATION NOTE: If Session#saveOrUpdate or #update is used to update an entity, then
		//                      Hibernate does not have a database snapshot of the existing entity.
		//                      As a result, oldFields will be null.
		// Don't use a batch if oldFields == null and the jth table is optional (isNullableTable( j ),
		// because there is no way to know that there is actually a row to update. If the update
		// was batched in this case, the batch update would fail and there is no way to fallback to
		// an insert.
		final boolean useBatch = expectation.canBeBatched()
				&& isBatchable()
				&& session.getConfiguredJdbcBatchSize() > 1
				&& ( oldFields != null || !isNullableTable( j ) );
		if ( useBatch && updateBatchKey == null ) {
			updateBatchKey = new BasicBatchKey(
					getEntityName() + "#UPDATE",
					expectation
			);
		}
		final boolean callable = isUpdateCallable( j );
		final boolean useVersion = j == 0 && isVersioned();

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Updating entity: {0}", MessageHelper.infoString( this, id, getFactory() ) );
			if ( useVersion ) {
				LOG.tracev( "Existing version: {0} -> New version:{1}", oldVersion, fields[getVersionProperty()] );
			}
		}

		try {
			int index = 1; // starting index
			final PreparedStatement update;
			if ( useBatch ) {
				update = session
						.getJdbcCoordinator()
						.getBatch( updateBatchKey )
						.getBatchStatement( sql, callable );
			}
			else {
				update = session
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql, callable );
			}

			try {
				index += expectation.prepare( update );

				//Now write the values of fields onto the prepared statement
				index = dehydrate(
						id,
						fields,
						rowId,
						includeProperty,
						propertyColumnUpdateable,
						j,
						update,
						session,
						index,
						true
				);

				// Write any appropriate versioning conditional parameters
				if ( useVersion && entityMetamodel.getOptimisticLockStyle().isVersion()) {
					if ( checkVersion( includeProperty ) ) {
						getVersionType().nullSafeSet( update, oldVersion, index, session );
					}
				}
				else if ( isAllOrDirtyOptLocking() && oldFields != null ) {
					boolean[] versionability = getPropertyVersionability(); //TODO: is this really necessary????
					boolean[] includeOldField = entityMetamodel.getOptimisticLockStyle().isAll()
							? getPropertyUpdateability()
							: includeProperty;
					Type[] types = getPropertyTypes();
					for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
						boolean include = includeOldField[i] &&
								isPropertyOfTable( i, j ) &&
								versionability[i]; //TODO: is this really necessary????
						if ( include ) {
							boolean[] settable = types[i].toColumnNullness( oldFields[i], getFactory() );
							types[i].nullSafeSet(
									update,
									oldFields[i],
									index,
									settable,
									session
							);
							index += ArrayHelper.countTrue( settable );
						}
					}
				}

				if ( useBatch ) {
					session.getJdbcCoordinator().getBatch( updateBatchKey ).addToBatch();
					return true;
				}
				else {
					return check(
							session.getJdbcCoordinator().getResultSetReturn().executeUpdate( update ),
							id,
							j,
							expectation,
							update,
							sql
					);
				}

			}
			catch (SQLException | RuntimeException e) {
				if ( useBatch ) {
					session.getJdbcCoordinator().abortBatch();
				}
				throw e;
			}
			finally {
				if ( !useBatch ) {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( update );
					session.getJdbcCoordinator().afterStatementExecution();
				}
			}

		}
		catch (SQLException e) {
			throw getFactory().getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"could not update: " + MessageHelper.infoString( this, id, getFactory() ),
					sql
			);
		}
	}

	private BasicBatchKey deleteBatchKey;

	/**
	 * Perform an SQL DELETE
	 */
	public void delete(
			final Object id,
			final Object version,
			final int j,
			final Object object,
			final String sql,
			final SharedSessionContractImplementor session,
			final Object[] loadedState) throws HibernateException {

		if ( isInverseTable( j ) ) {
			return;
		}

		final boolean useVersion = j == 0 && isVersioned()
				&& object != null; // null object signals that we're deleting an unloaded proxy
		final boolean callable = isDeleteCallable( j );
		final Expectation expectation = Expectations.appropriateExpectation( deleteResultCheckStyles[j] );
		final boolean useBatch = j == 0
				&& isBatchable()
				&& expectation.canBeBatched()
				&& session.getConfiguredJdbcBatchSize() > 1;
		if ( useBatch && deleteBatchKey == null ) {
			deleteBatchKey = new BasicBatchKey(
					getEntityName() + "#DELETE",
					expectation
			);
		}

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Deleting entity: {0}", MessageHelper.infoString( this, id, getFactory() ) );
			if ( useVersion ) {
				LOG.tracev( "Version: {0}", version );
			}
		}

		if ( isTableCascadeDeleteEnabled( j ) ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Delete handled by foreign key constraint: {0}", getTableName( j ) );
			}
			return; //EARLY EXIT!
		}

		try {
			//Render the SQL query
			PreparedStatement delete;
			int index = 1;
			if ( useBatch ) {
				delete = session
						.getJdbcCoordinator()
						.getBatch( deleteBatchKey )
						.getBatchStatement( sql, callable );
			}
			else {
				delete = session
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql, callable );
			}

			try {

				index += expectation.prepare( delete );

				// Do the key. The key is immutable, so we can use the _current_ object state,
				// not necessarily the state at the time the delete operation was issued
				getIdentifierType().nullSafeSet( delete, id, index, session );
				index += getIdentifierColumnSpan();

				// We should use the _current_ object state (after any updates that occurred during flush)

				if ( useVersion ) {
					getVersionType().nullSafeSet( delete, version, index, session );
				}
				else if ( isAllOrDirtyOptLocking() && loadedState != null ) {
					boolean[] versionability = getPropertyVersionability();
					Type[] types = getPropertyTypes();
					for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
						if ( isPropertyOfTable( i, j ) && versionability[i] ) {
							// this property belongs to the table and it is not specifically
							// excluded from optimistic locking by optimistic-lock="false"
							boolean[] settable = types[i].toColumnNullness( loadedState[i], getFactory() );
							types[i].nullSafeSet( delete, loadedState[i], index, settable, session );
							index += ArrayHelper.countTrue( settable );
						}
					}
				}

				if ( useBatch ) {
					session.getJdbcCoordinator().getBatch( deleteBatchKey ).addToBatch();
				}
				else {
					check(
							session.getJdbcCoordinator().getResultSetReturn().executeUpdate( delete ),
							id,
							j,
							expectation,
							delete,
							sql
					);
				}

			}
			catch (SQLException | RuntimeException e) {
				if ( useBatch ) {
					session.getJdbcCoordinator().abortBatch();
				}
				throw e;
			}
			finally {
				if ( !useBatch ) {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( delete );
					session.getJdbcCoordinator().afterStatementExecution();
				}
			}

		}
		catch (SQLException sqle) {
			throw getFactory().getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not delete: " +
							MessageHelper.infoString( this, id, getFactory() ),
					sql
			);

		}

	}

	protected String[] getUpdateStrings(boolean byRowId, boolean lazy) {
		if ( byRowId ) {
			return lazy ? getSQLLazyUpdateByRowIdStrings() : getSQLUpdateByRowIdStrings();
		}
		else {
			return lazy ? getSQLLazyUpdateStrings() : getSQLUpdateStrings();
		}
	}

	/**
	 * Update an object
	 */
	@Override
	public void update(
			final Object id,
			final Object[] fields,
			int[] dirtyFields,
			final boolean hasDirtyCollection,
			final Object[] oldFields,
			final Object oldVersion,
			final Object object,
			final Object rowId,
			final SharedSessionContractImplementor session) throws HibernateException {

		// apply any pre-update in-memory value generation
		if ( getEntityMetamodel().hasPreUpdateGeneratedValues() ) {
			final InMemoryValueGenerationStrategy[] valueGenerationStrategies = getEntityMetamodel().getInMemoryValueGenerationStrategies();
			int valueGenerationStrategiesSize = valueGenerationStrategies.length;
			if ( valueGenerationStrategiesSize != 0 ) {
				int[] fieldsPreUpdateNeeded = new int[valueGenerationStrategiesSize];
				int count = 0;
				for ( int i = 0; i < valueGenerationStrategiesSize; i++ ) {
					if ( valueGenerationStrategies[i] != null && valueGenerationStrategies[i].getGenerationTiming()
							.includesUpdate() ) {
						fields[i] = valueGenerationStrategies[i].getValueGenerator().generateValue(
								(Session) session,
								object
						);
						setPropertyValue( object, i, fields[i] );
						fieldsPreUpdateNeeded[count++] = i;
					}
				}
//				if ( fieldsPreUpdateNeeded.length != 0 ) {
//					if ( dirtyFields != null ) {
//						dirtyFields = ArrayHelper.join( fieldsPreUpdateNeeded, dirtyFields );
//					}
//					else if ( hasDirtyCollection ) {
//						dirtyFields = fieldsPreUpdateNeeded;
//					}
//					// no dirty fields and no dirty collections so no update needed ???
//				}
				if ( dirtyFields != null ) {
					dirtyFields = ArrayHelper.join( dirtyFields, ArrayHelper.trim( fieldsPreUpdateNeeded, count ) );
				}
			}
		}

		//note: dirtyFields==null means we had no snapshot, and we couldn't get one using select-before-update
		//	  oldFields==null just means we had no snapshot to begin with (we might have used select-before-update to get the dirtyFields)

		final boolean[] tableUpdateNeeded = getTableUpdateNeeded( dirtyFields, hasDirtyCollection );
		final int span = getTableSpan();

		final boolean[] propsToUpdate;
		final String[] updateStrings;
		EntityEntry entry = session.getPersistenceContextInternal().getEntry( object );

		// Ensure that an immutable or non-modifiable entity is not being updated unless it is
		// in the process of being deleted.
		if ( entry == null && !isMutable() ) {
			throw new IllegalStateException( "Updating immutable entity that is not in session yet" );
		}
		if ( dirtyFields != null && entityMetamodel.isDynamicUpdate() ) {
			// We need to generate the UPDATE SQL when dynamic-update="true"
			propsToUpdate = getPropertiesToUpdate( dirtyFields, hasDirtyCollection );
			// don't need to check laziness (dirty checking algorithm handles that)
			updateStrings = new String[span];
			for ( int j = 0; j < span; j++ ) {
				updateStrings[j] = tableUpdateNeeded[j] ?
						generateUpdateString( propsToUpdate, j, oldFields, j == 0 && rowId != null ) :
						null;
			}
		}
		else if ( dirtyFields != null && hasUninitializedLazyProperties( object ) && hasLazyDirtyFields( dirtyFields ) ) {
			// We need to generate the UPDATE SQL when there are dirty lazy fields
			propsToUpdate = getPropertiesToUpdate( dirtyFields, hasDirtyCollection );
			// don't need to check laziness (dirty checking algorithm handles that)
			final boolean[] propertyLaziness = getPropertyLaziness();
			// we add also all the non lazy properties because dynamic update is false
			for ( int i = 0; i < propertyLaziness.length; i++ ) {
				if ( propertyLaziness[i] == false ) {
					propsToUpdate[i] = true;
				}
			}
			updateStrings = new String[span];
			for ( int j = 0; j < span; j++ ) {
				updateStrings[j] = tableUpdateNeeded[j] ?
						generateUpdateString( propsToUpdate, j, oldFields, j == 0 && rowId != null ) :
						null;
			}
		}
		else if ( !isModifiableEntity( entry ) ) {
			// We need to generate UPDATE SQL when a non-modifiable entity (e.g., read-only or immutable)
			// needs:
			// - to have references to transient entities set to null before being deleted
			// - to have version incremented do to a "dirty" association
			// If dirtyFields == null, then that means that there are no dirty properties to
			// to be updated; an empty array for the dirty fields needs to be passed to
			// getPropertiesToUpdate() instead of null.
			propsToUpdate = getPropertiesToUpdate(
					( dirtyFields == null ? ArrayHelper.EMPTY_INT_ARRAY : dirtyFields ),
					hasDirtyCollection
			);
			// don't need to check laziness (dirty checking algorithm handles that)
			updateStrings = new String[span];
			for ( int j = 0; j < span; j++ ) {
				updateStrings[j] = tableUpdateNeeded[j] ?
						generateUpdateString( propsToUpdate, j, oldFields, j == 0 && rowId != null ) :
						null;
			}
		}
		else {
			// For the case of dynamic-update="false", or no snapshot, we use the static SQL
			updateStrings = getUpdateStrings(
					rowId != null,
					hasUninitializedLazyProperties( object )
			);
			propsToUpdate = getPropertyUpdateability( object );
		}

		for ( int j = 0; j < span; j++ ) {
			// Now update only the tables with dirty properties (and the table with the version number)
			if ( tableUpdateNeeded[j] ) {
				updateOrInsert(
						id,
						fields,
						oldFields,
						j == 0 ? rowId : null,
						propsToUpdate,
						j,
						oldVersion,
						object,
						updateStrings[j],
						session
				);
			}
		}
	}

	private boolean hasLazyDirtyFields(int[] dirtyFields) {
		final boolean[] propertyLaziness = getPropertyLaziness();
		for ( int i = 0; i < dirtyFields.length; i++ ) {
			if ( propertyLaziness[dirtyFields[i]] ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object insert(Object[] fields, Object object, SharedSessionContractImplementor session)
			throws HibernateException {
		// apply any pre-insert in-memory value generation
		preInsertInMemoryValueGeneration( fields, object, session );

		final int span = getTableSpan();
		final Object id;
		if ( entityMetamodel.isDynamicInsert() ) {
			// For the case of dynamic-insert="true", we need to generate the INSERT SQL
			boolean[] notNull = getPropertiesToInsert( fields );
			id = insert( fields, notNull, generateIdentityInsertString( notNull ), object, session );
			for ( int j = 1; j < span; j++ ) {
				insert( id, fields, notNull, j, generateInsertString( notNull, j ), object, session );
			}
		}
		else {
			// For the case of dynamic-insert="false", use the static SQL
			id = insert( fields, getPropertyInsertability(), getSQLIdentityInsertString(), object, session );
			for ( int j = 1; j < span; j++ ) {
				insert( id, fields, getPropertyInsertability(), j, getSQLInsertStrings()[j], object, session );
			}
		}
		return id;
	}

	@Override
	public void insert(Object id, Object[] fields, Object object, SharedSessionContractImplementor session) {
		// apply any pre-insert in-memory value generation
		preInsertInMemoryValueGeneration( fields, object, session );

		final int span = getTableSpan();
		if ( entityMetamodel.isDynamicInsert() ) {
			// For the case of dynamic-insert="true", we need to generate the INSERT SQL
			boolean[] notNull = getPropertiesToInsert( fields );
			if ( hasDuplicateTables() ) {
				final String[] insertedTables = new String[span];
				for ( int j = 0; j < span; j++ ) {
					if ( isInverseTable( j ) ) {
						continue;
					}

					//note: it is conceptually possible that a UserType could map null to
					//	  a non-null value, so the following is arguable:
					if ( isNullableTable( j ) && isAllNull( fields, j ) ) {
						continue;
					}
					final String tableName = getTableName( j );
					insertedTables[j] = tableName;
					if ( ArrayHelper.indexOf( insertedTables, j, tableName ) != -1 ) {
						update(
								id,
								fields,
								null,
								null,
								notNull,
								j,
								null,
								object,
								generateUpdateString( notNull, j, false ),
								session
						);
					}
					else {
						insert( id, fields, notNull, j, generateInsertString( notNull, j ), object, session );
					}
				}
			}
			else {
				for ( int j = 0; j < span; j++ ) {
					insert( id, fields, notNull, j, generateInsertString( notNull, j ), object, session );
				}
			}
		}
		else {
			// For the case of dynamic-insert="false", use the static SQL
			if ( hasDuplicateTables() ) {
				final String[] insertedTables = new String[span];
				for ( int j = 0; j < span; j++ ) {
					if ( isInverseTable( j ) ) {
						continue;
					}

					//note: it is conceptually possible that a UserType could map null to
					//	  a non-null value, so the following is arguable:
					if ( isNullableTable( j ) && isAllNull( fields, j ) ) {
						continue;
					}
					final String tableName = getTableName( j );
					insertedTables[j] = tableName;
					if ( ArrayHelper.indexOf( insertedTables, j, tableName ) != -1 ) {
						update(
								id,
								fields,
								null,
								null,
								getPropertyInsertability(),
								j,
								null,
								object,
								getSQLUpdateStrings()[j],
								session
						);
					}
					else {
						insert( id, fields, getPropertyInsertability(), j, getSQLInsertStrings()[j], object, session );
					}
				}
			}
			else {
				for ( int j = 0; j < span; j++ ) {
					insert( id, fields, getPropertyInsertability(), j, getSQLInsertStrings()[j], object, session );
				}
			}
		}
	}

	protected void preInsertInMemoryValueGeneration(Object[] fields, Object object, SharedSessionContractImplementor session) {
		if ( getEntityMetamodel().hasPreInsertGeneratedValues() ) {
			final InMemoryValueGenerationStrategy[] strategies = getEntityMetamodel().getInMemoryValueGenerationStrategies();
			for ( int i = 0; i < strategies.length; i++ ) {
				if ( strategies[i] != null && strategies[i].getGenerationTiming().includesInsert() ) {
					fields[i] = strategies[i].getValueGenerator().generateValue( (Session) session, object, fields[i] );
					setPropertyValue( object, i, fields[i] );
				}
			}
		}
	}

	/**
	 * Delete an object
	 */
	@Override
	public void delete(Object id, Object version, Object object, SharedSessionContractImplementor session)
			throws HibernateException {
		boolean isImpliedOptimisticLocking = !entityMetamodel.isVersioned() && isAllOrDirtyOptLocking()
				&& object != null; // null object signals that we're deleting an unloaded proxy

		Object[] loadedState = null;
		if ( isImpliedOptimisticLocking ) {
			// need to treat this as if it where optimistic-lock="all" (dirty does *not* make sense);
			// first we need to locate the "loaded" state
			//
			// Note, it potentially could be a proxy, so doAfterTransactionCompletion the location the safe way...
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			Object entity = persistenceContext.getEntity( session.generateEntityKey( id, this ) );
			if ( entity != null ) {
				EntityEntry entry = persistenceContext.getEntry( entity );
				loadedState = entry.getLoadedState();
			}
		}

		final String[] deleteStrings = getSQLDeleteStrings( object, isImpliedOptimisticLocking, loadedState );
		for ( int j = getTableSpan() - 1; j >= 0; j-- ) {
			delete( id, version, j, object, deleteStrings[j], session, loadedState );
		}
	}

	private String[] getSQLDeleteStrings(Object object, boolean isImpliedOptimisticLocking, Object[] loadedState) {
		if ( isImpliedOptimisticLocking && loadedState != null ) {
			// we need to utilize dynamic delete statements
			return generateSQLDeleteStrings(loadedState);
		}
		else if ( object != null ) {
			// otherwise, utilize the static delete statements
			return getSQLDeleteStrings();
		}
		else {
			// deleting an unloaded proxy
			return getSQLDeleteNoVersionCheckStrings();
		}
	}

	protected boolean isAllOrDirtyOptLocking() {
		return entityMetamodel.getOptimisticLockStyle().isAllOrDirty();
	}

	protected String[] generateSQLDeleteStrings(Object[] loadedState) {
		int span = getTableSpan();
		String[] deleteStrings = new String[span];
		for ( int j = span - 1; j >= 0; j-- ) {
			Delete delete = createDelete().setTableName( getTableName( j ) )
					.addPrimaryKeyColumns( getKeyColumns( j ) );
			if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
				delete.setComment( "delete " + getEntityName() + " [" + j + "]" );
			}

			boolean[] versionability = getPropertyVersionability();
			Type[] types = getPropertyTypes();
			for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
				if ( isPropertyOfTable( i, j ) && versionability[i] ) {
					// This property belongs to the table, and it's not explicitly
					// excluded from optimistic locking by optimistic-lock="false"
					String[] propertyColumnNames = getPropertyColumnNames( i );
					boolean[] propertyNullness = types[i].toColumnNullness( loadedState[i], getFactory() );
					for ( int k = 0; k < propertyNullness.length; k++ ) {
						if ( propertyNullness[k] ) {
							delete.addWhereFragment( propertyColumnNames[k] + " = ?" );
						}
						else {
							delete.addWhereFragment( propertyColumnNames[k] + " is null" );
						}
					}
				}
			}
			deleteStrings[j] = delete.toStatementString();
		}
		return deleteStrings;
	}

	protected void logStaticSQL() {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Static SQL for entity: %s", getEntityName() );
			for ( Map.Entry<String, SingleIdArrayLoadPlan> entry : sqlLazySelectStringsByFetchGroup.entrySet() ) {
				LOG.debugf( " Lazy select (%s) : %s", entry.getKey(), entry.getValue().getJdbcSelect().getSql() );
			}
			if ( sqlVersionSelectString != null ) {
				LOG.debugf( " Version select: %s", sqlVersionSelectString );
			}
			for ( int j = 0; j < getTableSpan(); j++ ) {
				LOG.debugf( " Insert %s: %s", j, getSQLInsertStrings()[j] );
				LOG.debugf( " Update %s: %s", j, getSQLUpdateStrings()[j] );
				LOG.debugf( " Delete %s: %s", j, getSQLDeleteStrings()[j] );
			}
			if ( sqlIdentityInsertString != null ) {
				LOG.debugf( " Identity insert: %s", sqlIdentityInsertString );
			}
			if ( sqlUpdateByRowIdString != null ) {
				LOG.debugf( " Update by row id (all fields): %s", sqlUpdateByRowIdString );
			}
			if ( sqlLazyUpdateByRowIdString != null ) {
				LOG.debugf( " Update by row id (non-lazy fields): %s", sqlLazyUpdateByRowIdString );
			}
		}
	}

	@Override
	public void applyDiscriminator(
			Consumer<Predicate> predicateConsumer,
			String alias,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		// by default, nothing to do
	}

	@Override
	public void applyFilterRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			SqlAstCreationState creationState) {
		if ( filterHelper == null ) {
			return;
		}

		final FilterAliasGenerator filterAliasGenerator = useQualifier && tableGroup != null
				? getFilterAliasGenerator( tableGroup )
				: null;

		filterHelper.applyEnabledFilters( predicateConsumer, filterAliasGenerator, enabledFilters );
	}

	@Override
	public void applyBaseRestrictions(Consumer<Predicate> predicateConsumer, TableGroup tableGroup, boolean useQualifier, Map<String, Filter> enabledFilters, Set<String> treatAsDeclarations, SqlAstCreationState creationState) {
		applyFilterRestrictions( predicateConsumer, tableGroup, useQualifier, enabledFilters, creationState );
		applyWhereRestrictions( predicateConsumer, tableGroup, useQualifier, creationState );
	}

	@Override
	public void applyWhereRestrictions(Consumer<Predicate> predicateConsumer, TableGroup tableGroup, boolean useQualifier, SqlAstCreationState creationState) {
		if ( sqlWhereStringTemplate == null ) {
			return;
		}

		final String alias;
		final TableReference tableReference;
		if ( tableGroup == null || ( tableReference = tableGroup.resolveTableReference( sqlWhereStringTableExpression ) ) == null ) {
			alias = null;
		}
		else {
			if ( useQualifier && tableReference.getIdentificationVariable() != null ) {
				alias = tableReference.getIdentificationVariable();
			}
			else {
				alias = tableReference.getTableId();
			}
		}

		final String fragment = StringHelper.replace( sqlWhereStringTemplate, Template.TEMPLATE, alias );
		predicateConsumer.accept( new SqlFragmentPredicate( fragment ) );
	}

	@Override
	public String generateFilterConditionAlias(String rootAlias) {
		return rootAlias;
	}

	protected boolean shouldInnerJoinSubclassTable(
			int subclassTableNumber,
			Set<String> treatAsDeclarations) {
		if ( isClassOrSuperclassJoin( subclassTableNumber ) ) {
			final boolean shouldInnerJoin = !isInverseTable( subclassTableNumber )
					&& !isNullableTable( subclassTableNumber );
			// the table is either this persister's driving table or (one of) its super class persister's driving
			// tables which can be inner joined as long as the `shouldInnerJoin` condition resolves to true
			return shouldInnerJoin;
		}

		// otherwise we have a subclass table and need to look a little deeper...

		// IMPL NOTE : By default includeSubclasses indicates that all subclasses should be joined and that each
		// subclass ought to be joined by outer-join.  However, TREAT-AS always requires that an inner-join be used
		// so we give TREAT-AS higher precedence...

		return isSubclassTableIndicatedByTreatAsDeclarations(subclassTableNumber, treatAsDeclarations);
	}

	protected boolean isSubclassTableIndicatedByTreatAsDeclarations(
			int subclassTableNumber,
			Set<String> treatAsDeclarations) {
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

		//doLateInit();
	}

	private void doLateInit() {
		//insert/update/delete SQL
		final int joinSpan = getTableSpan();
		sqlDeleteStrings = new String[joinSpan];
		sqlDeleteNoVersionCheckStrings = new String[joinSpan];
		sqlInsertStrings = new String[joinSpan];
		sqlUpdateStrings = new String[joinSpan];
		sqlLazyUpdateStrings = new String[joinSpan];

		sqlUpdateByRowIdString = rowIdName == null ?
				null :
				generateUpdateString( getPropertyUpdateability(), 0, true );
		sqlLazyUpdateByRowIdString = rowIdName == null ?
				null :
				generateUpdateString( getNonLazyPropertyUpdateability(), 0, true );

		for ( int j = 0; j < joinSpan; j++ ) {
			sqlInsertStrings[j] = customSQLInsert[j] == null
					? generateInsertString( getPropertyInsertability(), j )
					: substituteBrackets( customSQLInsert[j] );
			sqlUpdateStrings[j] = customSQLUpdate[j] == null
					? generateUpdateString( getPropertyUpdateability(), j, false )
					: substituteBrackets( customSQLUpdate[j] );
			sqlLazyUpdateStrings[j] = customSQLUpdate[j] == null
					? generateUpdateString( getNonLazyPropertyUpdateability(), j, false )
					: substituteBrackets( customSQLUpdate[j] );
			sqlDeleteStrings[j] = customSQLDelete[j] == null
					? generateDeleteString( j, true )
					: substituteBrackets( customSQLDelete[j] );
			sqlDeleteNoVersionCheckStrings[j] = customSQLDelete[j] == null
					? generateDeleteString( j, false )
					: substituteBrackets( customSQLDelete[j] ); //TODO: oops, fix!
		}

		tableHasColumns = new boolean[joinSpan];
		for ( int j = 0; j < joinSpan; j++ ) {
			tableHasColumns[j] = sqlUpdateStrings[j] != null;
		}

		//select SQL
		sqlLazySelectStringsByFetchGroup = generateLazySelectStringsByFetchGroup();
		sqlVersionSelectString = generateSelectVersionString();
		if ( isIdentifierAssignedByInsert() ) {
			identityDelegate = ( (PostInsertIdentifierGenerator) getIdentifierGenerator() )
					.getInsertGeneratedIdentifierDelegate(
							this,
							getFactory().getJdbcServices().getDialect(),
							useGetGeneratedKeys()
					);
			sqlIdentityInsertString = customSQLInsert[0] == null
					? generateIdentityInsertString( getPropertyInsertability() )
					: substituteBrackets( customSQLInsert[0] );
		}
		else {
			sqlIdentityInsertString = null;
		}

		logStaticSQL();
	}

	private String substituteBrackets(String sql) {
		return new SQLQueryParser( sql, null, getFactory() ).process();
	}

	@Override
	public final void postInstantiate() throws MappingException {
		doLateInit();
		prepareLoader( singleIdEntityLoader );
		prepareLoader( multiIdEntityLoader );

		doPostInstantiate();
	}

	private void prepareLoader(Loader loader) {
		if ( loader instanceof Preparable ) {
			( (Preparable) loader ).prepare();
		}
	}

	@Deprecated(forRemoval = true)
	protected void doPostInstantiate() {
	}

	/**
	 * Load an instance using either the {@code forUpdateLoader} or the outer joining {@code loader},
	 * depending upon the value of the {@code lock} parameter
	 */
	@Override
	public Object load(Object id, Object optionalObject, LockMode lockMode, SharedSessionContractImplementor session) {
		return load( id, optionalObject, new LockOptions().setLockMode( lockMode ), session );
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
			LOG.tracev( "Fetching entity: {0}", MessageHelper.infoString( this, id, getFactory() ) );
		}

		if ( optionalObject == null ) {
			return singleIdEntityLoader.load( id, lockOptions, readOnly, session );
		}
		else {
			return singleIdEntityLoader.load( id, optionalObject, lockOptions, readOnly, session );
		}
	}

	public SingleIdEntityLoader<?> getSingleIdEntityLoader() {
		return singleIdEntityLoader;
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
				loaded = singleIdEntityLoader.load(
						identifier,
						entity,
						LockOptions.NONE,
						session
				);
			}

			if ( loaded == null ) {
				final PersistenceContext persistenceContext = session.getPersistenceContext();
				persistenceContext.removeEntry( entity );
				persistenceContext.removeEntity( entityKey );
				session.getFactory().getEntityNotFoundDelegate().handleEntityNotFound(
						entityKey.getEntityName(),
						identifier
				);
			}

			final LazyAttributeLoadingInterceptor interceptor = enhancementMetadata.injectInterceptor(
					entity,
					identifier,
					session
			);

			final Object value;
			if ( nameOfAttributeBeingAccessed == null ) {
				return null;
			}
			else if ( interceptor.isAttributeLoaded( nameOfAttributeBeingAccessed ) ) {
				value = getPropertyValue( entity, nameOfAttributeBeingAccessed );
			}
			else {
				value = ( (LazyPropertyInitializer) this ).initializeLazyProperty( nameOfAttributeBeingAccessed, entity, session );
			}

			return interceptor.readObject(
					entity,
					nameOfAttributeBeingAccessed,
					value
			);
		}

		throw new IllegalStateException(  );
	}

	@Override
	public List<?> multiLoad(Object[] ids, EventSource session, MultiIdLoadOptions loadOptions) {
		return multiIdEntityLoader.load( ids, loadOptions, session );
	}

	@Override
	public void registerAffectingFetchProfile(String fetchProfileName) {
		if ( affectingFetchProfileNames == null ) {
			this.affectingFetchProfileNames = new HashSet<>();
		}
		affectingFetchProfileNames.add( fetchProfileName );
	}

	@Override
	public boolean isAffectedByEntityGraph(LoadQueryInfluencers loadQueryInfluencers) {
		if ( loadQueryInfluencers.getEffectiveEntityGraph().getGraph() == null ) {
			return false;
		}

		return loadQueryInfluencers.getEffectiveEntityGraph().getGraph().appliesTo( getEntityName() );
	}

	@Override
	public boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers loadQueryInfluencers) {
		final Set<String> fetchProfileNames = this.affectingFetchProfileNames;
		if ( fetchProfileNames != null ) {
			for ( String s : loadQueryInfluencers.getEnabledFetchProfileNames() ) {
				if ( fetchProfileNames.contains( s ) ) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isAffectedByEnabledFilters(LoadQueryInfluencers loadQueryInfluencers) {
		if ( loadQueryInfluencers.hasEnabledFilters() && filterHelper != null ) {
			if ( filterHelper.isAffectedBy( loadQueryInfluencers.getEnabledFilters() ) ) {
				return true;
			}
			// we still need to verify collection fields to be eagerly loaded by 'join'
			for ( NonIdentifierAttribute attribute : entityMetamodel.getProperties() ) {
				if ( attribute instanceof EntityBasedAssociationAttribute ) {
					final AssociationType associationType = ( (EntityBasedAssociationAttribute) attribute ).getType();
					if ( associationType instanceof CollectionType ) {
						final Joinable joinable = associationType.getAssociatedJoinable( getFactory() );
						if ( joinable.isCollection() ) {
							final QueryableCollection collectionPersister = (QueryableCollection) joinable;
							if ( collectionPersister.getFetchMode() == FetchMode.JOIN
									&& collectionPersister.isAffectedByEnabledFilters( loadQueryInfluencers ) ) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	public final boolean isAllNull(Object[] array, int tableNumber) {
		for ( int i = 0; i < array.length; i++ ) {
			if ( isPropertyOfTable( i, tableNumber ) && array[i] != null ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSubclassPropertyNullable(int i) {
		return subclassPropertyNullabilityClosure[i];
	}

	/**
	 * Transform the array of property indexes to an array of booleans,
	 * true when the property is dirty
	 */
	public final boolean[] getPropertiesToUpdate(final int[] dirtyProperties, final boolean hasDirtyCollection) {
		final boolean[] propsToUpdate = new boolean[entityMetamodel.getPropertySpan()];
		final boolean[] updateability = getPropertyUpdateability(); //no need to check laziness, dirty checking handles that
		for ( int property: dirtyProperties ) {
			if (updateability[property]) {
				propsToUpdate[property] = true;
			}
		}
		if ( isVersioned() && updateability[getVersionProperty()] ) {
			propsToUpdate[getVersionProperty()] =
					Versioning.isVersionIncrementRequired(
							dirtyProperties,
							hasDirtyCollection,
							getPropertyVersionability()
					);
		}
		return propsToUpdate;
	}

	/**
	 * Transform the array of property indexes to an array of booleans,
	 * true when the property is insertable and non-null
	 */
	public boolean[] getPropertiesToInsert(Object[] fields) {
		boolean[] notNull = new boolean[fields.length];
		boolean[] insertable = getPropertyInsertability();
		for ( int i = 0; i < fields.length; i++ ) {
			notNull[i] = insertable[i] && fields[i] != null;
		}
		return notNull;
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
				entityMetamodel.getProperties(),
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
				String propertyName = entityMetamodel.getProperties()[prop].getName();
				LOG.trace(StringHelper.qualify(getEntityName(), propertyName) + " is dirty");
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

	public Comparator<?> getVersionComparator() {
		return isVersioned() ? getVersionType().getJavaTypeDescriptor().getComparator() : null;
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
		if ( getEntityMetamodel().getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() ) {
			final BytecodeLazyAttributeInterceptor interceptor = getEntityMetamodel().getBytecodeEnhancementMetadata()
					.extractLazyInterceptor( entity );
			if ( interceptor == null ) {
				getEntityMetamodel().getBytecodeEnhancementMetadata().injectInterceptor(
						entity,
						getIdentifier( entity, session ),
						session
				);
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
				: naturalIdMapping.extractNaturalIdFromEntityState(entitySnapshot, session);

		naturalIdResolutions.removeSharedResolution( id, naturalIdSnapshot, this );
		naturalIdResolutions.manageLocalResolution(
				id,
				naturalIdMapping.extractNaturalIdFromEntity( entity, session ),
				this,
				CachedNaturalIdValueSource.UPDATE
		);
	}

	@Override
	public Boolean isTransient(Object entity, SharedSessionContractImplementor session) throws HibernateException {
		final Object id = canExtractIdOutOfEntity() ? getIdentifier(entity, session) : null;
		// we *always* assume an instance with a null
		// identifier or no identifier property is unsaved!
		if ( id == null ) {
			return true;
		}

		// check the version unsaved-value, if appropriate
		final Object version = getVersion( entity );
		if ( isVersioned() ) {
			// let this take precedence if defined, since it works for
			// assigned identifiers
			Boolean result = versionMapping.getUnsavedStrategy().isUnsaved( version );
			if ( result != null ) {
				return result;
			}
		}

		// check the id unsaved-value
		Boolean result = identifierMapping.getUnsavedStrategy().isUnsaved( id );
		if ( result != null ) {
			return result;
		}

		// check to see if it is in the second-level cache
		if ( session.getCacheMode().isGetEnabled() && canReadFromCache() ) {
			final EntityDataAccess cache = getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey( id, this, session.getFactory(), session.getTenantIdentifier() );
			final Object ce = CacheHelper.fromSharedCache( session, ck, getCacheAccessStrategy() );
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

	public final boolean isModifiableEntity(EntityEntry entry) {
		return ( entry == null ? isMutable() : entry.isModifiableEntity() );
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

	@Override
	public IdentifierGenerator getIdentifierGenerator() throws HibernateException {
		return entityMetamodel.getIdentifierProperty().getIdentifierGenerator();
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
	public boolean canExtractIdOutOfEntity() {
		return hasIdentifierProperty()
			|| entityMetamodel.getIdentifierProperty().isEmbedded()
			|| entityMetamodel.getIdentifierProperty().hasIdentifierMapper();
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
	 */
	@Override
	public Type getPropertyType(String propertyName) throws MappingException {
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

	protected final OptimisticLockStyle optimisticLockStyle() {
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
		return isVersioned() && getEntityMetamodel().isVersionGenerated();
	}

	@Override
	public boolean isVersionPropertyInsertable() {
		return isVersioned() && getPropertyInsertability()[getVersionProperty()];
	}

	@Override
	public void afterInitialize(Object entity, SharedSessionContractImplementor session) {
		if ( isPersistentAttributeInterceptable( entity ) && getRepresentationStrategy().getMode() == RepresentationMode.POJO ) {
			final BytecodeLazyAttributeInterceptor interceptor = getEntityMetamodel().getBytecodeEnhancementMetadata()
					.extractLazyInterceptor( entity );
			assert interceptor != null;
			if ( interceptor.getLinkedSession() == null ) {
				interceptor.setSession( session );
			}
		}

		// clear the fields that are marked as dirty in the dirtiness tracker
		processIfSelfDirtinessTracker( entity, AbstractEntityPersister::clearDirtyAttributes );
	}

	private static void clearDirtyAttributes(final SelfDirtinessTracker entity) {
		entity.$$_hibernate_clearDirtyAttributes();
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
	public final Class<?> getMappedClass() {
		return this.getMappedJavaType().getJavaTypeClass();
	}

	@Override
	public boolean implementsLifecycle() {
		return Lifecycle.class.isAssignableFrom( getMappedClass() );
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
			if ( hasSubclasses() ) {
				for ( int i = 0; i < attributeMappings.size(); i++ ) {
					final Object value = values[i];
					if ( value != UNFETCHED_PROPERTY ) {
						final Setter setter = attributeMappings.getAttributeMapping(i).getPropertyAccess().getSetter();
						setter.set( object, value );
					}
				}
			}
			else {
				for ( int i = 0; i < staticFetchableList.size(); i++ ) {
					final AttributeMapping attribute = staticFetchableList.getAttributeMapping( i );
					final Object value = values[i];
					if ( value != UNFETCHED_PROPERTY ) {
						final Setter setter = attribute.getPropertyAccess().getSetter();
						setter.set( object, value );
					}
				}
			}
		}
	}

	@Override
	public void setPropertyValue(Object object, int i, Object value) {
		final String propertyName = getPropertyNames()[i];
		setPropertyValue( object, propertyName, value );
	}

	@Override
	public Object[] getPropertyValues(Object object) {
		if ( accessOptimizer != null ) {
			return accessOptimizer.getPropertyValues( object );
		}
		else {
			final BytecodeEnhancementMetadata enhancementMetadata = entityMetamodel.getBytecodeEnhancementMetadata();
			final LazyAttributesMetadata lazyAttributesMetadata = enhancementMetadata.getLazyAttributesMetadata();
			final Object[] values = new Object[ getNumberOfAttributeMappings() ];
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.getAttributeMapping( i );
				final AttributeMetadataAccess attributeMetadataAccess = attributeMapping.getAttributeMetadataAccess();
				if ( ! lazyAttributesMetadata.isLazyAttribute( attributeMapping.getAttributeName() )
						|| enhancementMetadata.isAttributeLoaded( object, attributeMapping.getAttributeName() ) ) {
					values[i] = attributeMetadataAccess
							.resolveAttributeMetadata( this )
							.getPropertyAccess()
							.getGetter()
							.get( object );
				}
				else {
					values[i] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
				}
			}

			return values;
		}
	}

	@Override
	public Object getPropertyValue(Object object, int i) {
		return attributeMappings.getAttributeMapping( i ).getAttributeMetadataAccess()
				.resolveAttributeMetadata( this )
				.getPropertyAccess()
				.getGetter()
				.get( object );
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
			baseValue = attributeMapping.getAttributeMetadataAccess()
					.resolveAttributeMetadata( this )
					.getPropertyAccess()
					.getGetter()
					.get( object );
			if ( dotIndex != -1 ) {
				baseValueType = (ManagedMappingType) attributeMapping.getMappedType();
			}
		}
		else if ( identifierMapping instanceof NonAggregatedIdentifierMapping ) {
			final EmbeddedAttributeMapping embeddedAttributeMapping = (EmbeddedAttributeMapping) findAttributeMapping( NavigableRole.IDENTIFIER_MAPPER_PROPERTY );
			final AttributeMapping mapping = embeddedAttributeMapping == null
					? null
					: embeddedAttributeMapping.getMappedType().findAttributeMapping( basePropertyName );
			if ( mapping != null ) {
				baseValue = mapping.getAttributeMetadataAccess()
						.resolveAttributeMetadata( this )
						.getPropertyAccess()
						.getGetter()
						.get( object );
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
		final int nextDotIndex = propertyName.indexOf( '.', dotIndex + 1 );
		final int endIndex = nextDotIndex == -1 ? propertyName.length() : nextDotIndex;
		final AttributeMapping attributeMapping;
		attributeMapping = baseValueType.findAttributeMapping(
				propertyName.substring( dotIndex + 1, endIndex )
		);
		baseValue = attributeMapping.getAttributeMetadataAccess()
				.resolveAttributeMetadata( this )
				.getPropertyAccess()
				.getGetter()
				.get( baseValue );
		baseValueType = nextDotIndex == -1 ? null : (ManagedMappingType) attributeMapping.getMappedType();
		return getPropertyValue( baseValue, baseValueType, propertyName, nextDotIndex );
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return identifierMapping.getIdentifier( entity );
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		identifierMapping.setIdentifier( entity, id, session );
	}

	@Override
	public Object getVersion(Object object) {
		if ( getVersionMapping() == null ) {
			return null;
		}

		return getVersionMapping().getVersionAttribute().getPropertyAccess().getGetter().get( object );
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
		if ( session == null ) {
			return;
		}
		processIfPersistentAttributeInterceptable( entity, this::setSession, session );
	}

	private void setSession(PersistentAttributeInterceptable entity, SharedSessionContractImplementor session) {
		final BytecodeLazyAttributeInterceptor interceptor = getEntityMetamodel().getBytecodeEnhancementMetadata()
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
		if ( entityMetamodel.getIdentifierProperty().getIdentifierGenerator() instanceof Assigned ) {
			return;
		}

		// reset the identifier
		setIdentifier(
				entity,
				identifierMapping.getUnsavedStrategy().getDefaultValue( currentId ),
				session
		);

		// reset the version
		if ( versionMapping != null ) {
			versionMapping.getVersionAttribute().getPropertyAccess().getSetter().set(
					entity,
					versionMapping.getUnsavedStrategy().getDefaultValue( currentVersion )
			);
		}
	}

	@Override
	public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory) {
		if ( instance == null || !hasSubclasses() ) {
			return this;
		}
		else {
			// todo (6.0) : this previously used `org.hibernate.tuple.entity.EntityTuplizer#determineConcreteSubclassEntityName`
			//		- we may need something similar here...

			if ( getRepresentationStrategy().getInstantiator().isSameClass( instance, factory ) ) {
				return this;
			}

			for ( EntityMappingType sub : subclassMappingTypes.values() ) {
				if ( sub.getEntityPersister().getRepresentationStrategy()
						.getInstantiator().isSameClass( instance, factory ) ) {
					return sub.getEntityPersister();
				}
			}

			return this;
		}
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

		final Object[] result = new Object[this.attributeMappings.size()];
		for ( int i = 0; i < this.attributeMappings.size(); i++ ) {
			result[i] = this.attributeMappings.getAttributeMapping( i ) .getPropertyAccess().getGetter().getForInsert(
					entity,
					mergeMap,
					session
			);
		}
		return result;
	}

	protected boolean shouldGetAllProperties(Object entity) {
		final BytecodeEnhancementMetadata bytecodeEnhancementMetadata = getEntityMetamodel().getBytecodeEnhancementMetadata();
		if ( !bytecodeEnhancementMetadata.isEnhancedForLazyLoading() ) {
			return true;
		}

		return !bytecodeEnhancementMetadata.hasUnFetchedAttributes( entity );
	}

	@Override
	public void processInsertGeneratedProperties(
			Object id,
			Object entity,
			Object[] state,
			SharedSessionContractImplementor session) {
		if ( insertGeneratedValuesProcessor == null ) {
			throw new UnsupportedOperationException( "Entity has no insert-generated properties - `" + getEntityName() + "`" );
		}

		insertGeneratedValuesProcessor.processGeneratedValues(
				entity,
				id,
				state,
				session
		);
	}

	@Override
	public void processUpdateGeneratedProperties(
			Object id,
			Object entity,
			Object[] state,
			SharedSessionContractImplementor session) {
		if ( updateGeneratedValuesProcessor == null ) {
			throw new AssertionFailure( "Entity has no update-generated properties - `" + getEntityName() + "`" );
		}
		updateGeneratedValuesProcessor.processGeneratedValues(
				entity,
				id,
				state,
				session
		);
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
	public int[] getNaturalIdentifierProperties() {
		return entityMetamodel.getNaturalIdentifierProperties();
	}

	private void verifyHasNaturalId() {
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
					MessageHelper.infoString( this )
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
		final AttributeMetadata attributeMetadata = attributeMapping.getAttributeMetadataAccess().resolveAttributeMetadata( this );
		attributeMetadata.getPropertyAccess().getSetter().set( object, value );
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
			return new StandardCacheEntryImpl(
					state,
					persister,
					version,
					session,
					entity
			);
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
			return new StandardCacheEntryImpl(
					state,
					persister,
					version,
					session,
					entity
			);
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

	private final JavaType<?> javaType;
	private final EntityRepresentationStrategy representationStrategy;

	private EntityMappingType superMappingType;
	private SortedMap<String, EntityMappingType> subclassMappingTypes;

	private EntityIdentifierMapping identifierMapping;
	private NaturalIdMapping naturalIdMapping;
	private EntityVersionMapping versionMapping;
	private EntityRowIdMapping rowIdMapping;
	private EntityDiscriminatorMapping discriminatorMapping;

	private AttributeMappingsList attributeMappings;
	protected AttributeMappingsMap declaredAttributeMappings = AttributeMappingsMap.builder().build();
	protected AttributeMappingsList staticFetchableList; //Needs to NOT use a generic List as it causes a critical level of type pollution on each read!

	protected ReflectionOptimizer.AccessOptimizer accessOptimizer;

	@Override
	public void visitAttributeMappings(Consumer<? super AttributeMapping> action) {
		this.attributeMappings.forEachAttributeMapping( action );
	}

	@Override
	public void forEachAttributeMapping(final IndexedConsumer<AttributeMapping> consumer) {
		attributeMappings.forEachAttributeMapping( consumer );
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
				this.discriminatorMapping = superMappingType.getDiscriminatorMapping();
				this.identifierMapping = superMappingType.getIdentifierMapping();
				this.naturalIdMapping = superMappingType.getNaturalIdMapping();
				this.versionMapping = superMappingType.getVersionMapping();
				this.rowIdMapping = superMappingType.getRowIdMapping();
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

		final EntityMetamodel currentEntityMetamodel = this.getEntityMetamodel();
		int stateArrayPosition = getStateArrayInitialPosition( creationProcess );

		NonIdentifierAttribute[] properties = currentEntityMetamodel.getProperties();
		ImmutableAttributeMappingsMap.Builder mappingsBuilder = AttributeMappingsMap.builder();
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
								creationProcess
						)
				);
			}
			this.declaredAttributeMappings = mappingsBuilder.build();
//			else {
				// its defined on the supertype, skip it here
//			}
		}

		getAttributeMappings();

		postProcessAttributeMappings( creationProcess, bootEntityDescriptor );

		final ReflectionOptimizer reflectionOptimizer = representationStrategy.getReflectionOptimizer();

		if ( reflectionOptimizer != null ) {
			accessOptimizer = reflectionOptimizer.getAccessOptimizer();
		}
		else {
			accessOptimizer = null;
		}

		// register a callback for after all `#prepareMappingModel` calls have finished.  here we want to delay the
		// generation of `staticFetchableList` because we need to wait until after all sub-classes have had their
		// `#prepareMappingModel` called (and their declared attribute mappings resolved)
		creationProcess.registerInitializationCallback(
				"Entity(" + getEntityName() + ") `staticFetchableList` generator",
				() -> {
					if ( hasInsertGeneratedProperties() ) {
						insertGeneratedValuesProcessor = createGeneratedValuesProcessor( GenerationTiming.INSERT );
					}
					if ( hasUpdateGeneratedProperties() ) {
						updateGeneratedValuesProcessor = createGeneratedValuesProcessor( GenerationTiming.ALWAYS );
					}
					final ImmutableAttributeMappingList.Builder builder = new ImmutableAttributeMappingList.Builder( attributeMappings.size() );
					visitSubTypeAttributeMappings( attributeMapping -> builder.add( attributeMapping ) );
					staticFetchableList = builder.build();
					return true;
				}
		);

		boolean needsMultiTableInsert = hasMultipleTables();
		if ( needsMultiTableInsert ) {
			creationProcess.registerInitializationCallback(
					"Entity(" + getEntityName() + ") `sqmMultiTableMutationStrategy` interpretation",
					() -> {
						sqmMultiTableMutationStrategy = interpretSqmMultiTableStrategy(
								this,
								creationProcess
						);
						if ( sqmMultiTableMutationStrategy == null ) {
							return false;
						}
						sqmMultiTableMutationStrategy.prepare(
								creationProcess,
								creationContext.getSessionFactory()
										.getJdbcServices()
										.getBootstrapJdbcConnectionAccess()
						);
						return true;
					}
			);

		}
		else {
			sqmMultiTableMutationStrategy = null;
		}

		if ( !needsMultiTableInsert && getIdentifierGenerator() instanceof BulkInsertionCapableIdentifierGenerator ) {
			if ( getIdentifierGenerator() instanceof OptimizableGenerator ) {
				final Optimizer optimizer = ( (OptimizableGenerator) getIdentifierGenerator() ).getOptimizer();
				needsMultiTableInsert = optimizer != null && optimizer.getIncrementSize() > 1;
			}
		}

		if ( needsMultiTableInsert ) {
			creationProcess.registerInitializationCallback(
					"Entity(" + getEntityName() + ") `sqmMultiTableInsertStrategy` interpretation",
					() -> {
						sqmMultiTableInsertStrategy = interpretSqmMultiTableInsertStrategy(
								this,
								creationProcess
						);
						if ( sqmMultiTableInsertStrategy == null ) {
							return false;
						}
						sqmMultiTableInsertStrategy.prepare(
								creationProcess,
								creationContext.getSessionFactory()
										.getJdbcServices()
										.getBootstrapJdbcConnectionAccess()
						);
						return true;
					}
			);

		}
		else {
			sqmMultiTableInsertStrategy = null;
		}
	}

	private void prepareMappingModel(MappingModelCreationProcess creationProcess, PersistentClass bootEntityDescriptor) {
		final EntityInstantiator instantiator = getRepresentationStrategy().getInstantiator();
		final Supplier<?> templateInstanceCreator;
		if ( ! instantiator.canBeInstantiated() ) {
			templateInstanceCreator = null;
		}
		else {
			final LazyValue<?> templateCreator = new LazyValue<>(
					() -> instantiator.instantiate( creationProcess.getCreationContext().getSessionFactory() )
			);
			templateInstanceCreator = templateCreator::getValue;
		}

		identifierMapping = creationProcess.processSubPart(
				EntityIdentifierMapping.ROLE_LOCAL_NAME,
				(role, process) ->
						generateIdentifierMapping( templateInstanceCreator, bootEntityDescriptor, process )
		);

		versionMapping = generateVersionMapping( templateInstanceCreator, bootEntityDescriptor, creationProcess );

		if ( rowIdName == null ) {
			rowIdMapping = null;
		}
		else {
			rowIdMapping = creationProcess.processSubPart(
					rowIdName,
					(role, process) -> new EntityRowIdMappingImpl( rowIdName, this.getTableName(), this )
			);
		}

		discriminatorMapping = generateDiscriminatorMapping( bootEntityDescriptor, creationProcess );
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

	private NaturalIdMapping generateNaturalIdMapping(MappingModelCreationProcess creationProcess, PersistentClass bootEntityDescriptor) {
		assert bootEntityDescriptor.hasNaturalId();

		final int[] naturalIdAttributeIndexes = entityMetamodel.getNaturalIdentifierProperties();
		assert naturalIdAttributeIndexes.length > 0;

		if ( naturalIdAttributeIndexes.length == 1 ) {
			final String propertyName = entityMetamodel.getPropertyNames()[ naturalIdAttributeIndexes[ 0 ] ];
			final AttributeMapping attributeMapping = findAttributeMapping( propertyName );

			return new SimpleNaturalIdMapping(
					(SingularAttributeMapping) attributeMapping,
					this,
					creationProcess
			);
		}

		// collect the names of the attributes making up the natural-id.
		final Set<String> attributeNames = CollectionHelper.setOfSize( naturalIdAttributeIndexes.length );
		for ( int naturalIdAttributeIndex : naturalIdAttributeIndexes ) {
			attributeNames.add( this.getPropertyNames()[ naturalIdAttributeIndex ] );
		}

		// then iterate over the attribute mappings finding the ones having names
		// in the collected names.  iterate here because it is already alphabetical

		final List<SingularAttributeMapping> collectedAttrMappings = new ArrayList<>();
		for ( AttributeMapping attributeMapping : attributeMappings.iterateAsAttributeMappings() ) {
			if ( attributeNames.contains( attributeMapping.getAttributeName() ) ) {
				collectedAttrMappings.add( (SingularAttributeMapping) attributeMapping );
			}
		}

		if ( collectedAttrMappings.size() <= 1 ) {
			throw new MappingException( "Expected multiple natural-id attributes, but found only one: " + getEntityName() );
		}

		return new CompoundNaturalIdMapping(
				this,
				collectedAttrMappings,
				creationProcess
		);
	}

	protected static SqmMultiTableMutationStrategy interpretSqmMultiTableStrategy(
			AbstractEntityPersister entityMappingDescriptor,
			MappingModelCreationProcess creationProcess) {
		assert entityMappingDescriptor.hasMultipleTables();

		EntityMappingType superMappingType = entityMappingDescriptor.getSuperMappingType();
		if ( superMappingType != null ) {
			SqmMultiTableMutationStrategy sqmMultiTableMutationStrategy = superMappingType
					.getSqmMultiTableMutationStrategy();
			if ( sqmMultiTableMutationStrategy != null ) {
				return sqmMultiTableMutationStrategy;
			}
		}

		// we need the boot model so we can have access to the Table
		final RootClass entityBootDescriptor = (RootClass) creationProcess.getCreationContext()
				.getBootModel()
				.getEntityBinding( entityMappingDescriptor.getRootEntityName() );

		return SqmMutationStrategyHelper.resolveStrategy(
				entityBootDescriptor,
				entityMappingDescriptor,
				creationProcess
		);
	}

	protected static SqmMultiTableInsertStrategy interpretSqmMultiTableInsertStrategy(
			AbstractEntityPersister entityMappingDescriptor,
			MappingModelCreationProcess creationProcess) {
		// we need the boot model so we can have access to the Table
		final RootClass entityBootDescriptor = (RootClass) creationProcess.getCreationContext()
				.getBootModel()
				.getEntityBinding( entityMappingDescriptor.getRootEntityName() );

		return SqmMutationStrategyHelper.resolveInsertStrategy(
				entityBootDescriptor,
				entityMappingDescriptor,
				creationProcess
		);
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
		// todo (6.0) not sure this is correct in case of SingleTable Inheritance and for Table per class when the selection is the root
		int stateArrayPosition;
		if ( superMappingType != null ) {
			( (InFlightEntityMappingType) superMappingType ).prepareMappingModel( creationProcess );
			stateArrayPosition = superMappingType.getNumberOfAttributeMappings();
		}
		else {
			stateArrayPosition = 0;
		}
		return stateArrayPosition;
	}

	protected boolean isPhysicalDiscriminator() {
		return getDiscriminatorFormulaTemplate() == null;
	}

	protected EntityDiscriminatorMapping generateDiscriminatorMapping(
			PersistentClass bootEntityDescriptor,
			MappingModelCreationProcess modelCreationProcess) {
		if ( getDiscriminatorType() == null) {
			return null;
		}
		else {
			final String discriminatorColumnExpression;
			final String columnDefinition;
			final Long length;
			final Integer precision;
			final Integer scale;
			if ( getDiscriminatorFormulaTemplate() == null ) {
				Column column = bootEntityDescriptor.getDiscriminator() == null
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
			return new ExplicitColumnDiscriminatorMappingImpl (
					this,
					(DiscriminatorType<?>) getTypeDiscriminatorMetadata().getResolutionType(),
					getTableName(),
					discriminatorColumnExpression,
					getDiscriminatorFormulaTemplate() != null,
					isPhysicalDiscriminator(),
					columnDefinition, length, precision, scale, modelCreationProcess
			);
		}
	}

	protected EntityVersionMapping generateVersionMapping(
			Supplier<?> templateInstanceCreator,
			PersistentClass bootEntityDescriptor,
			MappingModelCreationProcess creationProcess) {
		if ( getVersionType() == null ) {
			return null;
		}
		else {
			final int versionPropertyIndex = getVersionProperty();
			final String versionPropertyName = getPropertyNames()[ versionPropertyIndex ];

			return creationProcess.processSubPart(
					versionPropertyName,
					(role, creationProcess1) -> generateVersionMapping(
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
		if ( getMappedSuperclass() == null ) {
			return;
		}

		this.superMappingType = creationProcess.getEntityPersister( getMappedSuperclass() );
		final InFlightEntityMappingType inFlightEntityMappingType = (InFlightEntityMappingType) superMappingType;
		inFlightEntityMappingType.linkWithSubType( this, creationProcess );
		if ( subclassMappingTypes != null ) {
			subclassMappingTypes.values().forEach(
					sub -> inFlightEntityMappingType.linkWithSubType( sub, creationProcess)
			);
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
		return attributeMappings.getAttributeMapping( position );
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
		if ( subclassMappingTypes == null ) {
			return Collections.emptyList();
		}
		return subclassMappingTypes.values();
	}

	@Override
	public boolean isTypeOrSuperType(EntityMappingType targetType) {
		if ( targetType == null ) {
			// todo (6.0) : need to think through what this ought to indicate (if we allow it at all)
			//		- see `org.hibernate.metamodel.mapping.internal.AbstractManagedMappingType#isTypeOrSuperType`
			return true;
		}

		if ( targetType == this ) {
			return true;
		}

		if ( superMappingType != null ) {
			return superMappingType.isTypeOrSuperType( targetType );
		}

		return false;
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

			final boolean encapsulated = ! cidType.isEmbedded();
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
		final Dialect dialect = creationProcess.getCreationContext().getSessionFactory().getJdbcServices().getDialect();

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
				basicTypeResolution.getLegacyResolvedBasicType(),
				entityPersister,
				creationProcess
		);
	}

	private AttributeMapping generateNonIdAttributeMapping(
			NonIdentifierAttribute tupleAttrDefinition,
			Property bootProperty,
			int stateArrayPosition,
			MappingModelCreationProcess creationProcess) {
		final SessionFactoryImplementor sessionFactory = creationProcess.getCreationContext().getSessionFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		final String attrName = tupleAttrDefinition.getName();
		final Type attrType = tupleAttrDefinition.getType();

		final int propertyIndex = getPropertyIndex( bootProperty.getName() );

		final String tableExpression = getTableName( getPropertyTableNumbers()[propertyIndex] );
		final String[] attrColumnNames = getPropertyColumnNames( propertyIndex );

		final PropertyAccess propertyAccess = getRepresentationStrategy().resolvePropertyAccess( bootProperty );

		if ( propertyIndex == getVersionProperty() ) {
			Column column = bootProperty.getValue().getColumns().get( 0 );
			return MappingModelCreationHelper.buildBasicAttributeMapping(
					attrName,
					getNavigableRole().append( bootProperty.getName() ),
					stateArrayPosition,
					bootProperty,
					this,
					(BasicType<?>) attrType,
					tableExpression,
					attrColumnNames[0],
					false,
					null,
					null,
					column.getSqlType(),
					column.getLength(),
					column.getPrecision(),
					column.getScale(),
					propertyAccess,
					tupleAttrDefinition.getCascadeStyle(),
					creationProcess
			);
		}

		if ( attrType instanceof BasicType ) {
			final Value bootValue = bootProperty.getValue();

			final String attrColumnExpression;
			final boolean isAttrColumnExpressionFormula;
			final String customReadExpr;
			final String customWriteExpr;
			final String columnDefinition;
			final Long length;
			final Integer precision;
			final Integer scale;

			if ( bootValue instanceof DependantValue ) {
				attrColumnExpression = attrColumnNames[0];
				isAttrColumnExpressionFormula = false;
				customReadExpr = null;
				customWriteExpr = null;
				Column column = bootValue.getColumns().get( 0 );
				columnDefinition = column.getSqlType();
				length = column.getLength();
				precision = column.getPrecision();
				scale = column.getScale();
			}
			else {
				final BasicValue basicBootValue = (BasicValue) bootValue;

				if ( attrColumnNames[ 0 ] != null ) {
					attrColumnExpression = attrColumnNames[ 0 ];
					isAttrColumnExpressionFormula = false;

					final List<Selectable> selectables = basicBootValue.getSelectables();
					assert !selectables.isEmpty();
					final Selectable selectable = selectables.get(0);

					assert attrColumnExpression.equals( selectable.getText(sessionFactory.getJdbcServices().getDialect()) );

					customReadExpr = selectable.getTemplate(
							dialect,
							sessionFactory.getTypeConfiguration(),
							sessionFactory.getQueryEngine().getSqmFunctionRegistry()
					);
					customWriteExpr = selectable.getCustomWriteExpression();
					Column column = bootValue.getColumns().get( 0 );
					columnDefinition = column.getSqlType();
					length = column.getLength();
					precision = column.getPrecision();
					scale = column.getScale();
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
					scale = null;
				}
			}

			return MappingModelCreationHelper.buildBasicAttributeMapping(
					attrName,
					getNavigableRole().append( bootProperty.getName() ),
					stateArrayPosition,
					bootProperty,
					this,
					(BasicType<?>) attrType,
					tableExpression,
					attrColumnExpression,
					isAttrColumnExpressionFormula,
					customReadExpr,
					customWriteExpr,
					columnDefinition,
					length,
					precision,
					scale,
					propertyAccess,
					tupleAttrDefinition.getCascadeStyle(),
					creationProcess
			);
		}
		else if ( attrType instanceof AnyType ) {
			final JavaType<Object> baseAssociationJtd = sessionFactory
					.getTypeConfiguration()
					.getJavaTypeRegistry()
					.getDescriptor( Object.class );

			final AnyType anyType = (AnyType) attrType;

			return new DiscriminatedAssociationAttributeMapping(
					navigableRole.append( bootProperty.getName() ),
					baseAssociationJtd,
					this,
					stateArrayPosition,
					entityMappingType -> new AttributeMetadata() {

						private final MutabilityPlan<?> mutabilityPlan = new DiscriminatedAssociationAttributeMapping.MutabilityPlanImpl( anyType );

						private final boolean nullable = bootProperty.isOptional();
						private final boolean insertable = bootProperty.isInsertable();
						private final boolean updateable = bootProperty.isUpdateable();
						private final boolean optimisticallyLocked = bootProperty.isOptimisticLocked();

						@Override
						public PropertyAccess getPropertyAccess() {
							return propertyAccess;
						}

						@Override
						public MutabilityPlan<?> getMutabilityPlan() {
							return mutabilityPlan;
						}

						@Override
						public boolean isNullable() {
							return nullable;
						}

						@Override
						public boolean isInsertable() {
							return insertable;
						}

						@Override
						public boolean isUpdatable() {
							return updateable;
						}

						@Override
						public boolean isIncludedInDirtyChecking() {
							return updateable;
						}

						@Override
						public boolean isIncludedInOptimisticLocking() {
							return optimisticallyLocked;
						}
					},
					bootProperty.isLazy() ? FetchTiming.DELAYED : FetchTiming.IMMEDIATE,
					propertyAccess,
					bootProperty,
					(AnyType) attrType,
					(Any) bootProperty.getValue(),
					creationProcess
			);
		}
		else if ( attrType instanceof CompositeType ) {
			return MappingModelCreationHelper.buildEmbeddedAttributeMapping(
					attrName,
					stateArrayPosition,
					bootProperty,
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
			return MappingModelCreationHelper.buildPluralAttributeMapping(
					attrName,
					stateArrayPosition,
					bootProperty,
					this,
					propertyAccess,
					tupleAttrDefinition.getCascadeStyle(),
					getFetchMode( stateArrayPosition ),
					creationProcess
			);
		}
		else if ( attrType instanceof EntityType ) {
			return MappingModelCreationHelper.buildSingularAssociationAttributeMapping(
					attrName,
					getNavigableRole().append( attrName ),
					stateArrayPosition,
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
	public AttributeMappingsList getAttributeMappings() {
		if ( attributeMappings == null ) {
			int sizeHint = declaredAttributeMappings.size();
			sizeHint += (superMappingType == null ? 0 : superMappingType.getAttributeMappings().size() );
			ImmutableAttributeMappingList.Builder builder = new ImmutableAttributeMappingList.Builder( sizeHint );

			if ( superMappingType != null ) {
				superMappingType.visitAttributeMappings( builder::add );
			}

			for ( AttributeMapping am : declaredAttributeMappings.valueIterator() ) {
				builder.add( am );
			}
			this.attributeMappings = builder.build();
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
				if ( superDefinedAttribute instanceof EntityIdentifierMapping ) {
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
							throw new IllegalArgumentException(
									new SemanticException(
											String.format(
													Locale.ROOT,
													"Could not resolve attribute '%s' of '%s' due to the attribute being declared in multiple sub types: ['%s', '%s']",
													name,
													getJavaType().getJavaType().getTypeName(),
													attribute.asAttributeMapping().getDeclaringType()
															.getJavaType()
															.getJavaType()
															.getTypeName(),
													subDefinedAttribute.asAttributeMapping().getDeclaringType()
															.getJavaType()
															.getJavaType()
															.getTypeName()
											)
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

		for ( AttributeMapping attribute : declaredAttributeMappings.valueIterator() ) {
			if ( attribute instanceof EmbeddableValuedModelPart && attribute instanceof VirtualModelPart ) {
				final ModelPart subPart = ( (EmbeddableValuedModelPart) attribute ).findSubPart( name, null );
				if ( subPart != null ) {
					return subPart;
				}
			}
		}

		return null;
	}

	@Override
	public ModelPart findSubTypesSubPart(String name, EntityMappingType treatTargetType) {
		final AttributeMapping declaredAttribute = declaredAttributeMappings.get( name );
		if ( declaredAttribute != null ) {
			return declaredAttribute;
		}

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

	private ModelPart getIdentifierModelPart(String name, EntityMappingType treatTargetType) {
		if ( identifierMapping instanceof NonAggregatedIdentifierMapping ) {
			final ModelPart subPart = ( (NonAggregatedIdentifierMapping) identifierMapping ).findSubPart(
					name,
					treatTargetType
			);
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
		if ( EntityIdentifierMapping.ROLE_LOCAL_NAME.equals( name ) ) {
			return true;
		}

		if ( hasIdentifierProperty() && getIdentifierPropertyName().equals( name ) ) {
			return true;
		}

		return !entityMetamodel.hasNonIdentifierPropertyNamedId() && "id".equals( name );
	}

	@Override
	public void visitSubParts(
			Consumer<ModelPart> consumer,
			EntityMappingType treatTargetType) {
		consumer.accept( identifierMapping );

		declaredAttributeMappings.forEachValue( consumer );
	}

	@Override
	public void visitKeyFetchables(Consumer<Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		// No-op
	}

	@Override
	public void visitKeyFetchables(IndexedConsumer<Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		// No-op
	}

	@Override
	public int getNumberOfFetchables() {
		return getStaticFetchableList().size();
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
		return getStaticFetchableList().getFetchable( position );
	}

	@Override
	public void visitFetchables(
			Consumer<Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		if ( treatTargetType == null ) {
			getStaticFetchableList().forEachFetchable( fetchableConsumer );
//			staticFetchableList.forEach( fetchableConsumer );
			// EARLY EXIT!!!
			return;
		}

		if ( treatTargetType.isTypeOrSuperType( this ) ) {
			visitSubTypeAttributeMappings( fetchableConsumer );
		}
		else {
			attributeMappings.forEachFetchable( fetchableConsumer );
		}
	}

	@Override
	public void visitFetchables(IndexedConsumer<Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		if ( treatTargetType == null ) {
			getStaticFetchableList().forEachFetchable( fetchableConsumer );
			// EARLY EXIT!!!
			return;
		}

		attributeMappings.forEachFetchable( fetchableConsumer );
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

	protected AttributeMappingsList getStaticFetchableList() {
		return staticFetchableList;
	}

	@Override
	public void visitAttributeMappings(
			Consumer<? super AttributeMapping> action,
			EntityMappingType targetType) {
		attributeMappings.forEachAttributeMapping( action );
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
		for ( AttributeMapping attributeMapping : attributeMappings.iterateAsAttributeMappings() ) {
			span += attributeMapping.forEachSelectable( span + offset, selectableConsumer );
		}
		return span;
	}

	@Override
	public void visitSubTypeAttributeMappings(Consumer<? super AttributeMapping> action) {
		visitAttributeMappings( action );
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
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return getIdentifierMapping().forEachDisassembledJdbcValue(
				value,
				clause,
				offset,
				valuesConsumer,
				session
				);
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer consumer,
			SharedSessionContractImplementor session) {
		final EntityIdentifierMapping identifierMapping = getIdentifierMapping();
		final Object identifier;
		if ( value == null ) {
			identifier = null;
		}
		else {
			identifier = identifierMapping.disassemble( identifierMapping.getIdentifier( value ), session );
		}
		return identifierMapping.forEachDisassembledJdbcValue(
				identifier,
				clause,
				offset,
				consumer,
				session
		);
	}

	protected Insert createInsert() {
		return new Insert( getFactory().getJdbcServices().getDialect() );
	}

	protected Update createUpdate() {
		return new Update( getFactory().getJdbcServices().getDialect() );
	}

	protected Delete createDelete() {
		return new Delete();
	}
}
