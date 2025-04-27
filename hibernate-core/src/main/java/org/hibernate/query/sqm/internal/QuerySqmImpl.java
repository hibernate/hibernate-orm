/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TemporalType;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.BindableType;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.Page;
import org.hibernate.query.Query;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.criteria.internal.NamedCriteriaQueryMementoImpl;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.hql.internal.QuerySplitter;
import org.hibernate.query.hql.spi.SqmQueryImplementor;
import org.hibernate.query.internal.DelegatingDomainQueryExecutionContext;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.spi.DelegatingQueryOptions;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.spi.InterpretationsKeySource;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;
import org.hibernate.query.sqm.tree.AbstractSqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.SingleResultConsumer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static java.util.Collections.emptyMap;
import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_LOCKING;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_CACHE_RETRIEVE_MODE;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_CACHE_STORE_MODE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_CACHE_RETRIEVE_MODE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_CACHE_STORE_MODE;
import static org.hibernate.query.spi.SqlOmittingQueryOptions.omitSqlQueryOptions;
import static org.hibernate.query.spi.SqlOmittingQueryOptions.omitSqlQueryOptionsWithUniqueSemanticFilter;
import static org.hibernate.query.sqm.internal.AppliedGraphs.containsCollectionFetches;
import static org.hibernate.query.sqm.internal.SqmInterpretationsKey.createInterpretationsKey;
import static org.hibernate.query.sqm.internal.SqmInterpretationsKey.generateNonSelectKey;
import static org.hibernate.query.sqm.internal.SqmUtil.isSelect;
import static org.hibernate.query.sqm.internal.SqmUtil.validateCriteriaQuery;
import static org.hibernate.query.sqm.internal.SqmUtil.verifyIsNonSelectStatement;

/**
 * {@link Query} implementation based on an SQM
 *
 * @author Steve Ebersole
 */
public class QuerySqmImpl<R>
		extends AbstractSqmSelectionQuery<R>
		implements SqmQueryImplementor<R>, InterpretationsKeySource, DomainQueryExecutionContext {

	private final String hql;
	private Object queryStringCacheKey;
	private SqmStatement<R> sqm;

	private ParameterMetadataImplementor parameterMetadata;
	private DomainParameterXref domainParameterXref;

	private QueryParameterBindings parameterBindings;

	private final Class<R> resultType;
	private final TupleMetadata tupleMetadata;

	/**
	 * Creates a {@link org.hibernate.query.Query}
	 * instance from a named HQL memento.
	 * Form used from {@link NamedHqlQueryMementoImpl}.
	 */
	public QuerySqmImpl(
			NamedSqmQueryMemento<?> memento,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		this( memento.getHqlString(),
				interpretation( memento, expectedResultType, session ),
				expectedResultType, session );
		applySqmOptions( memento );
	}

	/**
	 * Creates a {@link org.hibernate.query.Query}
	 * instance from a named criteria query memento.
	 * Form used from {@link NamedCriteriaQueryMementoImpl}
	 */
	public QuerySqmImpl(
			NamedSqmQueryMemento<?> memento,
			SqmStatement<R> statement,
			Class<R> resultType,
			SharedSessionContractImplementor session) {
		this( statement, resultType, session );
		applySqmOptions( memento );
	}

	/**
	 * Form used for HQL queries
	 */
	public QuerySqmImpl(
			String hql,
			HqlInterpretation<R> hqlInterpretation,
			Class<R> resultType,
			SharedSessionContractImplementor session) {
		super( session );
		this.hql = hql;
		this.queryStringCacheKey = hql;
		this.resultType = resultType;

		sqm = hqlInterpretation.getSqmStatement();

		parameterMetadata = hqlInterpretation.getParameterMetadata();
		domainParameterXref = hqlInterpretation.getDomainParameterXref();
		parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		if ( sqm instanceof SqmSelectStatement<?> ) {
			hqlInterpretation.validateResultType( resultType );
		}
		else if ( resultType != null ) {
			throw new IllegalQueryOperationException( "Result type given for a non-SELECT Query", hql, null );
		}
		setComment( hql );

		tupleMetadata = buildTupleMetadata( sqm, resultType );
	}

	/**
	 * Form used for criteria queries
	 */
	public QuerySqmImpl(
			SqmStatement<R> criteria,
			Class<R> expectedResultType,
			SharedSessionContractImplementor producer) {
		this( criteria, producer.isCriteriaCopyTreeEnabled(), expectedResultType, producer );
	}

	/**
	 * Used for specifications.
	 */
	public QuerySqmImpl(
			SqmStatement<R> criteria,
			boolean copyAst,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		super( session );
		hql = CRITERIA_HQL_STRING;
		sqm = copyAst ? criteria.copy( SqmCopyContext.simpleContext() ) : criteria;
		queryStringCacheKey = sqm;
		// Cache immutable query plans by default
		setQueryPlanCacheable( !copyAst || session.isCriteriaPlanCacheEnabled() );

		setComment( hql );

		domainParameterXref = DomainParameterXref.from( sqm );
		parameterMetadata = !domainParameterXref.hasParameters()
				? ParameterMetadataImpl.EMPTY
				: new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );
		parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		// Parameters might be created through HibernateCriteriaBuilder.value which we need to bind here
		for ( SqmParameter<?> sqmParameter : domainParameterXref.getParameterResolutions().getSqmParameters() ) {
			if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper<?> wrapper ) {
				bindCriteriaParameter( wrapper );
			}
		}

		validateQuery( expectedResultType, sqm, hql );

		resultType = expectedResultType;
		tupleMetadata = buildTupleMetadata( criteria, expectedResultType );
	}

	private static <R> void validateQuery(Class<R> expectedResultType, SqmStatement<R> sqm, String hql) {
		if ( sqm instanceof SqmSelectStatement<R> selectStatement ) {
			final SqmQueryPart<R> queryPart = selectStatement.getQueryPart();
			// For criteria queries, we have to validate the fetch structure here
			queryPart.validateQueryStructureAndFetchOwners();
			validateCriteriaQuery( queryPart );
			selectStatement.validateResultType( expectedResultType );
		}
		else if ( sqm instanceof AbstractSqmDmlStatement<R> update ) {
			if ( expectedResultType != null ) {
				throw new IllegalQueryOperationException( "Result type given for a non-SELECT Query", hql, null );
			}
			update.validate( hql );
		}
	}

	@Override
	public TupleMetadata getTupleMetadata() {
		return tupleMetadata;
	}

	@Override
	public String getQueryString() {
		return hql;
	}

	@Override
	public Object getQueryStringCacheKey() {
		return queryStringCacheKey;
	}

	@Override
	public SqmStatement<R> getSqmStatement() {
		return sqm;
	}

	@Override
	protected void setSqmStatement(SqmSelectStatement<R> sqm) {
		this.sqm = sqm;
		this.queryStringCacheKey = sqm;

		final QueryParameterBindings oldParameterBindings = parameterBindings;
		domainParameterXref = DomainParameterXref.from( sqm );
		parameterMetadata = !domainParameterXref.hasParameters()
				? ParameterMetadataImpl.EMPTY
				: new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );
		parameterBindings = parameterMetadata.createBindings( getSessionFactory() );
		copyParameterBindings( oldParameterBindings );
	}

	@Override
	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	@Override
	public ParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	@Override
	public QueryParameterBindings getParameterBindings() {
		return getQueryParameterBindings();
	}

	@Override
	public Class<R> getResultType() {
		return resultType;
	}

	@Override
	public Class<R> getExpectedResultType() {
		return resultType;
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return getSession().getLoadQueryInfluencers();
	}

	@Override
	protected boolean resolveJdbcParameterTypeIfNecessary() {
		// No need to resolve JDBC parameter types as we know them from the SQM model
		return false;
	}

	@Override
	public BooleanSupplier hasMultiValuedParameterBindingsChecker() {
		return this::hasMultiValuedParameterBindings;
	}

	protected boolean hasMultiValuedParameterBindings() {
		return getQueryParameterBindings().hasAnyMultiValuedBindings()
			|| getParameterMetadata().hasAnyMatching( QueryParameter::allowsMultiValuedBinding );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// select execution

	@Override
	protected void prepareForExecution() {
		// Reset the callback before every execution
		resetCallback();
	}

	protected void verifySelect() {
		try {
			SqmUtil.verifyIsSelectStatement( getSqmStatement(), hql );
		}
		catch (IllegalQueryOperationException e) {
			// per JPA
			throw new IllegalStateException( "Query executed via 'getResultList()' or 'getSingleResult()' must be a 'select' query ["
					+ hql + "]", e );
		}
	}

	@Override
	public long getResultCount() {
		verifySelect();
		final DelegatingDomainQueryExecutionContext context = new DelegatingDomainQueryExecutionContext(this) {
			@Override
			public QueryOptions getQueryOptions() {
				return QueryOptions.NONE;
			}
		};
		final SqmSelectStatement<?> sqmStatement = (SqmSelectStatement<?>) getSqmStatement();
		return buildConcreteQueryPlan( sqmStatement.createCountQuery(), Long.class, null, getQueryOptions() )
				.executeQuery( context, SingleResultConsumer.instance() );
	}

	protected List<R> doList() {
		verifySelect();
		final SqmSelectStatement<?> statement = (SqmSelectStatement<?>) getSqmStatement();
		final boolean containsCollectionFetches =
				statement.containsCollectionFetches()
						|| containsCollectionFetches( getQueryOptions() );
		final boolean hasLimit = hasLimit( statement, getQueryOptions() );
		final boolean needsDistinct = needsDistinct( containsCollectionFetches, hasLimit, statement );
		final List<R> list = resolveSelectQueryPlan()
				.performList( executionContextForDoList( containsCollectionFetches, hasLimit, needsDistinct ) );
		return needsDistinct ? handleDistinct( hasLimit, statement, list ) : list;
	}

	private List<R> handleDistinct(boolean hasLimit, SqmSelectStatement<?> statement, List<R> list) {
		final int first = first( hasLimit, statement );
		final int max = max( hasLimit, statement, list );
		if ( first > 0 || max != -1 ) {
			final int resultSize = list.size();
			if ( first > resultSize ) {
				return new ArrayList<>(0);
			}
			else {
				final int toIndex = max != -1 ? first + max : resultSize;
				return list.subList( first, Math.min( toIndex, resultSize ) );
			}
		}
		else {
			return list;
		}
	}

	// TODO: very similar to SqmSelectionQueryImpl.executionContext()
	protected DomainQueryExecutionContext executionContextForDoList(
			boolean containsCollectionFetches, boolean hasLimit, boolean needsDistinct) {
		final MutableQueryOptions originalQueryOptions;
		final QueryOptions normalizedQueryOptions;
		if ( hasLimit && containsCollectionFetches ) {
			errorOrLogForPaginationWithCollectionFetch();

			originalQueryOptions = getQueryOptions();
			normalizedQueryOptions = needsDistinct
					? omitSqlQueryOptionsWithUniqueSemanticFilter( originalQueryOptions, true, false )
					: omitSqlQueryOptions( originalQueryOptions, true, false );
		}
		else {
			if ( needsDistinct ) {
				originalQueryOptions = getQueryOptions();
				normalizedQueryOptions = uniqueSemanticQueryOptions( originalQueryOptions );
			}
			else {
				return this;
			}
		}

		if ( originalQueryOptions == normalizedQueryOptions ) {
			return this;
		}
		else {
			return new DelegatingDomainQueryExecutionContext( this ) {
				@Override
				public QueryOptions getQueryOptions() {
					return normalizedQueryOptions;
				}
			};
		}
	}

	public static QueryOptions uniqueSemanticQueryOptions(QueryOptions originalOptions) {
		return originalOptions.getUniqueSemantic() == ListResultsConsumer.UniqueSemantic.FILTER
				? originalOptions
				: new UniqueSemanticFilterQueryOption( originalOptions );
	}

	private static class UniqueSemanticFilterQueryOption extends DelegatingQueryOptions{
		private UniqueSemanticFilterQueryOption(QueryOptions queryOptions) {
			super( queryOptions );
		}
		@Override
		public ListResultsConsumer.UniqueSemantic getUniqueSemantic() {
			return ListResultsConsumer.UniqueSemantic.FILTER;
		}
	}

	@Override
	protected ScrollableResultsImplementor<R> doScroll(ScrollMode scrollMode) {
		return resolveSelectQueryPlan().performScroll( scrollMode, this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Select query plan

	@Override
	public boolean isQueryPlanCacheable() {
		return CRITERIA_HQL_STRING.equals( hql )
				// For criteria queries, query plan caching requires an explicit opt-in
				? getQueryOptions().getQueryPlanCachingEnabled() == Boolean.TRUE
				: super.isQueryPlanCacheable();
	}

	private SelectQueryPlan<R> resolveSelectQueryPlan() {
		final QueryInterpretationCache.Key cacheKey = createInterpretationsKey( this );
		return cacheKey != null
				? interpretationCache().resolveSelectQueryPlan( cacheKey, this::buildSelectQueryPlan )
				: buildSelectQueryPlan();
	}

	private QueryInterpretationCache interpretationCache() {
		return getSessionFactory().getQueryEngine().getInterpretationCache();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Update / delete / insert query execution

	@Override
	public int executeUpdate() {
		verifyUpdate();
		getSession().checkTransactionNeededForUpdateOperation( "Executing an update/delete query" );
		final HashSet<String> fetchProfiles = beforeQueryHandlingFetchProfiles();
		boolean success = false;
		try {
			final int result = doExecuteUpdate();
			success = true;
			return result;
		}
		catch (IllegalQueryOperationException e) {
			throw new IllegalStateException( e );
		}
		catch (HibernateException e) {
			throw getSession().getExceptionConverter().convert( e );
		}
		finally {
			afterQueryHandlingFetchProfiles( success, fetchProfiles );
		}
	}

	protected void verifyUpdate() {
		try {
			verifyIsNonSelectStatement( getSqmStatement(), hql );
		}
		catch (IllegalQueryOperationException e) {
			// per JPA
			throw new IllegalStateException( "Query executed via 'executeUpdate()' must be an 'insert', 'update', or 'delete' statement ["
					+ hql + "]", e );
		}
	}

	protected int doExecuteUpdate() {
		try {
			return resolveNonSelectQueryPlan().executeUpdate( this );
		}
		finally {
			domainParameterXref.clearExpansions();
		}
	}

	private NonSelectQueryPlan resolveNonSelectQueryPlan() {
		// resolve (or make) the QueryPlan.

		NonSelectQueryPlan queryPlan = null;

		final QueryInterpretationCache.Key cacheKey = generateNonSelectKey( this );
		final QueryInterpretationCache interpretationCache = interpretationCache();
		if ( cacheKey != null ) {
			queryPlan = interpretationCache.getNonSelectQueryPlan( cacheKey );
		}

		if ( queryPlan == null ) {
			queryPlan = buildNonSelectQueryPlan();
			if ( cacheKey != null ) {
				interpretationCache.cacheNonSelectQueryPlan( cacheKey, queryPlan );
			}
		}

		return queryPlan;
	}

	private NonSelectQueryPlan buildNonSelectQueryPlan() {
		// to get here the SQM statement has already been validated to be
		// a non-select variety...
		final SqmStatement<R> sqmStatement = getSqmStatement();
		if ( sqmStatement instanceof SqmDeleteStatement<?> ) {
			return buildDeleteQueryPlan();
		}
		else if ( sqmStatement instanceof SqmUpdateStatement<?> ) {
			return buildUpdateQueryPlan();
		}
		else if ( sqmStatement instanceof SqmInsertStatement<?> ) {
			return buildInsertQueryPlan();
		}
		else {
			throw new UnsupportedOperationException( "Query#executeUpdate for Statements of type [" + sqmStatement + "] not supported" );
		}
	}

	private NonSelectQueryPlan buildDeleteQueryPlan() {
		final SqmDeleteStatement<R>[] concreteSqmStatements =
				QuerySplitter.split( (SqmDeleteStatement<R>) getSqmStatement() );
		return concreteSqmStatements.length > 1
				? buildAggregatedDeleteQueryPlan( concreteSqmStatements )
				: buildConcreteDeleteQueryPlan( concreteSqmStatements[0] );
	}

	private NonSelectQueryPlan buildConcreteDeleteQueryPlan(SqmDeleteStatement<?> sqmDelete) {
		final EntityDomainType<?> entityDomainType = sqmDelete.getTarget().getModel();
		final EntityPersister persister =
				getSessionFactory().getMappingMetamodel()
						.getEntityDescriptor( entityDomainType.getHibernateEntityName() );
		final SqmMultiTableMutationStrategy multiTableStrategy = persister.getSqmMultiTableMutationStrategy();
		if ( multiTableStrategy != null ) {
			// NOTE : MultiTableDeleteQueryPlan and SqmMultiTableMutationStrategy already handle soft-deletes internally
			return new MultiTableDeleteQueryPlan( sqmDelete, domainParameterXref, multiTableStrategy );
		}
		else {
			return new SimpleDeleteQueryPlan( persister, sqmDelete, domainParameterXref );
		}
	}

	private NonSelectQueryPlan buildAggregatedDeleteQueryPlan(SqmDeleteStatement<?>[] concreteSqmStatements) {
		final NonSelectQueryPlan[] aggregatedQueryPlans = new NonSelectQueryPlan[ concreteSqmStatements.length ];
		for ( int i = 0, x = concreteSqmStatements.length; i < x; i++ ) {
			aggregatedQueryPlans[i] = buildConcreteDeleteQueryPlan( concreteSqmStatements[i] );
		}
		return new AggregatedNonSelectQueryPlanImpl( aggregatedQueryPlans );
	}

	private NonSelectQueryPlan buildUpdateQueryPlan() {
		final SqmUpdateStatement<R> sqmUpdate = (SqmUpdateStatement<R>) getSqmStatement();
		final EntityPersister persister =
				getSessionFactory().getMappingMetamodel()
						.getEntityDescriptor( sqmUpdate.getTarget().getModel().getHibernateEntityName() );
		final SqmMultiTableMutationStrategy multiTableStrategy = persister.getSqmMultiTableMutationStrategy();
		return multiTableStrategy == null
				? new SimpleUpdateQueryPlan( sqmUpdate, domainParameterXref )
				: new MultiTableUpdateQueryPlan( sqmUpdate, domainParameterXref, multiTableStrategy );
	}

	private NonSelectQueryPlan buildInsertQueryPlan() {
		final SqmInsertStatement<R> sqmInsert = (SqmInsertStatement<R>) getSqmStatement();
		final EntityPersister persister =
				getSessionFactory().getMappingMetamodel()
						.getEntityDescriptor( sqmInsert.getTarget().getModel().getHibernateEntityName() );

		boolean useMultiTableInsert = persister.hasMultipleTables();
		if ( !useMultiTableInsert && !isSimpleValuesInsert( sqmInsert, persister ) ) {
			final Generator identifierGenerator = persister.getGenerator();
			if ( identifierGenerator instanceof BulkInsertionCapableIdentifierGenerator
					&& identifierGenerator instanceof OptimizableGenerator optimizableGenerator ) {
				final Optimizer optimizer = optimizableGenerator.getOptimizer();
				if ( optimizer != null && optimizer.getIncrementSize() > 1 ) {
					useMultiTableInsert = !hasIdentifierAssigned( sqmInsert, persister );
				}
			}
		}
		if ( useMultiTableInsert ) {
			return new MultiTableInsertQueryPlan(
					sqmInsert,
					domainParameterXref,
					persister.getSqmMultiTableInsertStrategy()
			);
		}
		else if ( sqmInsert instanceof SqmInsertValuesStatement<R> insertValues
				&& insertValues.getValuesList().size() != 1
				&& !getSessionFactory().getJdbcServices().getDialect().supportsValuesListForInsert() ) {
			// Split insert-values queries if the dialect doesn't support values lists
			final List<SqmValues> valuesList = insertValues.getValuesList();
			final NonSelectQueryPlan[] planParts = new NonSelectQueryPlan[valuesList.size()];
			for ( int i = 0; i < valuesList.size(); i++ ) {
				final SqmInsertValuesStatement<?> subInsert =
						insertValues.copyWithoutValues( SqmCopyContext.simpleContext() );
				subInsert.values( valuesList.get( i ) );
				planParts[i] = new SimpleInsertQueryPlan( subInsert, domainParameterXref );
			}

			return new AggregatedNonSelectQueryPlanImpl( planParts );
		}

		return new SimpleInsertQueryPlan( sqmInsert, domainParameterXref );
	}

	protected boolean hasIdentifierAssigned(SqmInsertStatement<?> sqmInsert, EntityPersister entityDescriptor) {
		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		final String partName =
				identifierMapping instanceof SingleAttributeIdentifierMapping
						? identifierMapping.getAttributeName()
						: EntityIdentifierMapping.ID_ROLE_NAME;
		for ( SqmPath<?> insertionTargetPath : sqmInsert.getInsertionTargetPaths() ) {
			if ( insertionTargetPath.getLhs() instanceof SqmRoot<?> ) {
				if ( insertionTargetPath.getReferencedPathSource().getPathName().equals( partName ) ) {
					return true;
				}
			}
		}

		return false;
	}

	protected boolean isSimpleValuesInsert(SqmInsertStatement<?> sqmInsert, EntityPersister entityDescriptor) {
		// Simple means that we can translate the statement to a single plain insert
		return sqmInsert instanceof SqmInsertValuesStatement
			// An insert is only simple if no SqmMultiTableMutation strategy is available,
			// as the presence of it means the entity has multiple tables involved,
			// in which case we currently need to use the MultiTableInsertQueryPlan
			&& entityDescriptor.getSqmMultiTableMutationStrategy() == null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// QueryOptions

	@Override
	public SqmQueryImplementor<R> addQueryHint(String hint) {
		getQueryOptions().addDatabaseHint( hint );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setLockOptions(LockOptions lockOptions) {
		// No verifySelect call, because in Hibernate we support locking in subqueries
		getQueryOptions().getLockOptions().overlay( lockOptions );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setLockMode(String alias, LockMode lockMode) {
		// No verifySelect call, because in Hibernate we support locking in subqueries
		super.setLockMode( alias, lockMode );
		return this;
	}

	@Override
	public <T> SqmQueryImplementor<T> setTupleTransformer(TupleTransformer<T> transformer) {
		getQueryOptions().setTupleTransformer( transformer );
		//noinspection unchecked
		return (SqmQueryImplementor<T>) this;
	}

	@Override
	public SqmQueryImplementor<R> setResultListTransformer(ResultListTransformer<R> transformer) {
		getQueryOptions().setResultListTransformer( transformer );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setMaxResults(int maxResults) {
		super.setMaxResults( maxResults );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setFirstResult(int startPosition) {
		super.setFirstResult( startPosition );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setHibernateFlushMode(FlushMode flushMode) {
		super.setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setQueryFlushMode(QueryFlushMode queryFlushMode) {
		super.setQueryFlushMode( queryFlushMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setFlushMode(FlushModeType flushMode) {
		super.setFlushMode( flushMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setLockMode(LockModeType lockMode) {
		if ( lockMode != LockModeType.NONE ) {
			// JPA requires an exception to be thrown when this is not a select statement
			verifySelect();
		}
		getSession().checkOpen( false );
		getQueryOptions().getLockOptions().setLockMode( LockMode.fromJpaLockMode( lockMode ) );
		return this;
	}

	@Override
	public LockModeType getLockMode() {
		verifySelect();
		getSession().checkOpen( false );
		return getLockOptions().getLockMode().toJpaLockMode();
	}

	@Override
	public Query<R> setPage(Page page) {
		super.setPage(page);
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// hints

	protected void collectHints(Map<String, Object> hints) {
		super.collectHints( hints );

		if ( isReadOnly() ) {
			hints.put( HINT_READ_ONLY, true );
		}

		putIfNotNull( hints, HINT_FETCH_SIZE, getFetchSize() );

		if ( isCacheable() ) {
			hints.put( HINT_CACHEABLE, true );
			putIfNotNull( hints, HINT_CACHE_REGION, getCacheRegion() );
			putIfNotNull( hints, HINT_CACHE_MODE, getCacheMode() );

			putIfNotNull( hints, HINT_SPEC_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			putIfNotNull( hints, HINT_SPEC_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
			putIfNotNull( hints, HINT_JAVAEE_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			putIfNotNull( hints, HINT_JAVAEE_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
		}

		final AppliedGraph appliedGraph = getQueryOptions().getAppliedGraph();
		if ( appliedGraph != null && appliedGraph.getSemantic() != null ) {
			hints.put( appliedGraph.getSemantic().getJakartaHintName(), appliedGraph );
			hints.put( appliedGraph.getSemantic().getJpaHintName(), appliedGraph );
		}

		putIfNotNull( hints, HINT_FOLLOW_ON_LOCKING, getQueryOptions().getLockOptions().getFollowOnLocking() );
	}

	@Override
	public SqmQueryImplementor<R> setHint(String hintName, Object value) {
		super.setHint( hintName, value );
		return this;
	}

	@Override
	public Query<R> setEntityGraph(EntityGraph<? super R> graph, GraphSemantic semantic) {
		super.setEntityGraph( graph, semantic );
		return this;
	}

	@Override
	public Query<R> enableFetchProfile(String profileName) {
		super.enableFetchProfile( profileName );
		return this;
	}

	@Override
	public Query<R> disableFetchProfile(String profileName) {
		super.disableFetchProfile( profileName );
		return this;
	}

	@Override
	protected void applyLockTimeoutHint(Integer timeout) {
		if ( isSelect( sqm ) ) {
			super.applyLockTimeoutHint( timeout );
		}
	}

	@Override
	protected void applyLockTimeoutHint(int timeout) {
		if ( isSelect( sqm ) ) {
			super.applyLockTimeoutHint( timeout );
		}
	}

	@Override
	protected void applyHibernateLockMode(LockMode value) {
		if ( isSelect( sqm ) ) {
			super.applyHibernateLockMode( value );
		}
	}

	@Override
	protected void applyLockModeType(LockModeType value) {
		if ( isSelect( sqm ) ) {
			super.applyLockModeType( value );
		}
	}

	@Override
	protected void applyAliasSpecificLockModeHint(String hintName, Object value) {
		if ( isSelect( sqm ) ) {
			super.applyAliasSpecificLockModeHint( hintName, value );
		}
	}

	@Override
	protected void applyFollowOnLockingHint(Boolean followOnLocking) {
		if ( isSelect( sqm ) ) {
			super.applyFollowOnLockingHint( followOnLocking );
		}
	}

	@Override
	public SqmQueryImplementor<R> applyGraph(@SuppressWarnings("rawtypes") RootGraph graph, GraphSemantic semantic) {
		getQueryOptions().applyGraph( (RootGraphImplementor<?>) graph, semantic );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named query externalization

	@Override
	public NamedSqmQueryMemento<R> toMemento(String name) {
		if ( CRITERIA_HQL_STRING.equals( getQueryString() ) ) {
			return new NamedCriteriaQueryMementoImpl<>(
					name,
					getResultType(),
					getSession().isCriteriaCopyTreeEnabled()
							? getSqmStatement() // the statement has already been copied
							: getSqmStatement().copy( SqmCopyContext.simpleContext() ),
					getQueryOptions().getLimit().getFirstRow(),
					getQueryOptions().getLimit().getMaxRows(),
					isCacheable(),
					getCacheRegion(),
					getCacheMode(),
					getQueryOptions().getFlushMode(),
					isReadOnly(),
					getLockOptions(),
					getTimeout(),
					getFetchSize(),
					getComment(),
					emptyMap(),
					getHints()
			);
		}
		else {
			return new NamedHqlQueryMementoImpl<>(
					name,
					getResultType(),
					getQueryString(),
					getQueryOptions().getLimit().getFirstRow(),
					getQueryOptions().getLimit().getMaxRows(),
					isCacheable(),
					getCacheRegion(),
					getCacheMode(),
					getQueryOptions().getFlushMode(),
					isReadOnly(),
					getLockOptions(),
					getTimeout(),
					getFetchSize(),
					getComment(),
					emptyMap(),
					getHints()
			);
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// unwrap

	public <T> T unwrap(Class<T> type) {
		if ( type.isInstance( this ) ) {
			return type.cast( this );
		}

		if ( type.isInstance( parameterMetadata ) ) {
			return type.cast( parameterMetadata );
		}

		if ( type.isInstance( parameterBindings ) ) {
			return type.cast( parameterBindings );
		}

		if ( type.isInstance( sqm ) ) {
			return type.cast( sqm );
		}

		if ( type.isInstance( getQueryOptions() ) ) {
			return type.cast( getQueryOptions() );
		}

		if ( type.isInstance( getQueryOptions().getAppliedGraph() ) ) {
			return type.cast( getQueryOptions().getAppliedGraph() );
		}

		if ( type.isInstance( getSession() ) ) {
			return type.cast( getSession() );
		}

		throw new PersistenceException( "Unrecognized unwrap type [" + type.getName() + "]" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariance


	@Override
	public SqmQueryImplementor<R> setComment(String comment) {
		super.setComment( comment );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setCacheMode(CacheMode cacheMode) {
		super.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		super.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		super.setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setTimeout(Integer timeout) {
		if ( timeout == null ) {
			timeout = -1;
		}
		setTimeout( (int) timeout );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setCacheable(boolean cacheable) {
		super.setCacheable( cacheable );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setCacheRegion(String cacheRegion) {
		super.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setQueryPlanCacheable(boolean queryPlanCacheable) {
		super.setQueryPlanCacheable( queryPlanCacheable );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setReadOnly(boolean readOnly) {
		super.setReadOnly( readOnly );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setProperties(@SuppressWarnings("rawtypes") Map bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(String name, P value, Class<P> javaType) {
		super.setParameter( name, value, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(String name, P value, BindableType<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override @Deprecated
	public SqmQueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(int position, P value, Class<P> javaType) {
		super.setParameter( position, value, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(int position, P value, BindableType<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override @Deprecated
	public SqmQueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Class<P> javaType) {
		super.setParameter( parameter, value, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, BindableType<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(Parameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override @Deprecated
	public SqmQueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmQueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmQueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmQueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmQueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmQueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(String name, P[] values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(String name, P[] values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameterList(int position, Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(int position, P[] values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

}
