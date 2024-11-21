/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.persistence.EntityGraph;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
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
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.BindableType;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.criteria.internal.NamedCriteriaQueryMementoImpl;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.hql.internal.QuerySplitter;
import org.hibernate.query.hql.spi.SqmQueryImplementor;
import org.hibernate.query.internal.DelegatingDomainQueryExecutionContext;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.named.NamedQueryMemento;
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
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.internal.SqmInterpretationsKey.InterpretationsKeySource;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.AbstractSqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
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

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TemporalType;
import org.hibernate.sql.results.spi.SingleResultConsumer;

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
import static org.hibernate.query.sqm.internal.SqmUtil.verifyIsNonSelectStatement;

/**
 * {@link Query} implementation based on an SQM
 *
 * @author Steve Ebersole
 */
public class QuerySqmImpl<R>
		extends AbstractSqmSelectionQuery<R>
		implements SqmQueryImplementor<R>, InterpretationsKeySource, DomainQueryExecutionContext {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( QuerySqmImpl.class );

	private final String hql;
	private SqmStatement<R> sqm;

	private final ParameterMetadataImplementor parameterMetadata;
	private final DomainParameterXref domainParameterXref;

	private final QueryParameterBindings parameterBindings;

	private final Class<R> resultType;
	private final TupleMetadata tupleMetadata;

	/**
	 * Creates a Query instance from a named HQL memento
	 */
	public QuerySqmImpl(
			NamedHqlQueryMementoImpl memento,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		this(
				memento.getHqlString(),
				interpretation( memento, expectedResultType, session ),
				expectedResultType,
				session
		);
		applyOptions( memento );
	}

	public QuerySqmImpl(
			NamedCriteriaQueryMementoImpl memento,
			Class<R> resultType,
			SharedSessionContractImplementor session) {
		this( (SqmStatement<R>) memento.getSqmStatement(), resultType, session );

		applyOptions( memento );
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
		this.resultType = resultType;

		this.sqm = hqlInterpretation.getSqmStatement();

		this.parameterMetadata = hqlInterpretation.getParameterMetadata();
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();

		this.parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		if ( sqm instanceof SqmSelectStatement<?> ) {
			hqlInterpretation.validateResultType( resultType );
		}
		else {
			if ( resultType != null ) {
				throw new IllegalQueryOperationException( "Result type given for a non-SELECT Query", hql, null );
			}
		}
		setComment( hql );

		this.tupleMetadata = buildTupleMetadata( sqm, resultType );
	}

	/**
	 * Form used for criteria queries
	 */
	public QuerySqmImpl(
			SqmStatement<R> criteria,
			Class<R> expectedResultType,
			SharedSessionContractImplementor producer) {
		super( producer );
		hql = CRITERIA_HQL_STRING;
		if ( producer.isCriteriaCopyTreeEnabled() ) {
			sqm = criteria.copy( SqmCopyContext.simpleContext() );
		}
		else {
			sqm = criteria;
			// Cache immutable query plans by default
			setQueryPlanCacheable( true );
		}

		setComment( hql );

		domainParameterXref = DomainParameterXref.from( sqm );
		if ( ! domainParameterXref.hasParameters() ) {
			parameterMetadata = ParameterMetadataImpl.EMPTY;
		}
		else {
			parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );
		}

		this.parameterBindings = parameterMetadata.createBindings( producer.getFactory() );

		// Parameters might be created through HibernateCriteriaBuilder.value which we need to bind here
		for ( SqmParameter<?> sqmParameter : domainParameterXref.getParameterResolutions().getSqmParameters() ) {
			if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper<?> ) {
				bindCriteriaParameter((SqmJpaCriteriaParameterWrapper<?>) sqmParameter);
			}
		}
		if ( sqm instanceof SqmSelectStatement<?> ) {
			final SqmSelectStatement<R> selectStatement = (SqmSelectStatement<R>) sqm;
			final SqmQueryPart<R> queryPart = selectStatement.getQueryPart();
			// For criteria queries, we have to validate the fetch structure here
			queryPart.validateQueryStructureAndFetchOwners();
			validateCriteriaQuery( queryPart );
			selectStatement.validateResultType( expectedResultType );
		}
		else {
			if ( expectedResultType != null ) {
				throw new IllegalQueryOperationException( "Result type given for a non-SELECT Query", hql, null );
			}
			( (AbstractSqmDmlStatement<?>) sqm ).validate( hql );
		}

		resultType = expectedResultType;
		tupleMetadata = buildTupleMetadata( criteria, expectedResultType );
	}

	private <T> void bindCriteriaParameter(SqmJpaCriteriaParameterWrapper<T> sqmParameter) {
		final JpaCriteriaParameter<T> jpaCriteriaParameter = sqmParameter.getJpaCriteriaParameter();
		final T value = jpaCriteriaParameter.getValue();
		// We don't set a null value, unless the type is also null which
		// is the case when using HibernateCriteriaBuilder.value
		if ( value != null || jpaCriteriaParameter.getNodeType() == null ) {
			// Use the anticipated type for binding the value if possible
			getQueryParameterBindings().getBinding( jpaCriteriaParameter )
					.setBindValue( value, jpaCriteriaParameter.getAnticipatedType() );
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
	public SqmStatement<R> getSqmStatement() {
		return sqm;
	}

	@Override
	protected void setSqmStatement(SqmSelectStatement<R> sqm) {
		this.sqm = sqm;
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
	public Supplier<Boolean> hasMultiValuedParameterBindingsChecker() {
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
		if ( cacheKey != null ) {
			return getSession().getFactory().getQueryEngine().getInterpretationCache()
					.resolveSelectQueryPlan( cacheKey, this::buildSelectQueryPlan );
		}
		else {
			return buildSelectQueryPlan();
		}
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
		final QueryInterpretationCache interpretationCache =
				getSessionFactory().getQueryEngine().getInterpretationCache();
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
		if ( getSqmStatement() instanceof SqmDeleteStatement<?> ) {
			return buildDeleteQueryPlan();
		}

		if ( getSqmStatement() instanceof SqmUpdateStatement<?> ) {
			return buildUpdateQueryPlan();
		}

		if ( getSqmStatement() instanceof SqmInsertStatement<?> ) {
			return buildInsertQueryPlan();
		}

		throw new UnsupportedOperationException( "Query#executeUpdate for Statements of type [" + getSqmStatement() + "] not supported" );
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
		final String entityNameToDelete = entityDomainType.getHibernateEntityName();
		final EntityPersister persister = getSessionFactory().getMappingMetamodel().getEntityDescriptor( entityNameToDelete );
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

		final String entityNameToUpdate = sqmUpdate.getTarget().getModel().getHibernateEntityName();
		final EntityPersister persister =
				getSessionFactory().getMappingMetamodel().getEntityDescriptor( entityNameToUpdate );

		final SqmMultiTableMutationStrategy multiTableStrategy = persister.getSqmMultiTableMutationStrategy();
		return multiTableStrategy == null
				? new SimpleUpdateQueryPlan( sqmUpdate, domainParameterXref )
				: new MultiTableUpdateQueryPlan( sqmUpdate, domainParameterXref, multiTableStrategy );
	}

	private NonSelectQueryPlan buildInsertQueryPlan() {
		final SqmInsertStatement<R> sqmInsert = (SqmInsertStatement<R>) getSqmStatement();

		final String entityNameToInsert = sqmInsert.getTarget().getModel().getHibernateEntityName();
		final AbstractEntityPersister persister = (AbstractEntityPersister)
				getSessionFactory().getMappingMetamodel().getEntityDescriptor( entityNameToInsert );

		boolean useMultiTableInsert = persister.isMultiTable();
		if ( !useMultiTableInsert && !isSimpleValuesInsert( sqmInsert, persister ) ) {
			final Generator identifierGenerator = persister.getGenerator();
			if ( identifierGenerator instanceof BulkInsertionCapableIdentifierGenerator
					&& identifierGenerator instanceof OptimizableGenerator ) {
				final Optimizer optimizer = ( (OptimizableGenerator) identifierGenerator ).getOptimizer();
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
		else if ( sqmInsert instanceof SqmInsertValuesStatement<?>
				&& ( (SqmInsertValuesStatement<R>) sqmInsert ).getValuesList().size() != 1
				&& !getSessionFactory().getJdbcServices().getDialect().supportsValuesListForInsert() ) {
			// Split insert-values queries if the dialect doesn't support values lists
			final SqmInsertValuesStatement<R> insertValues = (SqmInsertValuesStatement<R>) sqmInsert;
			final List<SqmValues> valuesList = insertValues.getValuesList();
			final NonSelectQueryPlan[] planParts = new NonSelectQueryPlan[valuesList.size()];
			for ( int i = 0; i < valuesList.size(); i++ ) {
				final SqmInsertValuesStatement<?> subInsert = insertValues.copyWithoutValues( SqmCopyContext.simpleContext() );
				subInsert.values( valuesList.get( i ) );
				planParts[i] = new SimpleInsertQueryPlan( subInsert, domainParameterXref );
			}

			return new AggregatedNonSelectQueryPlanImpl( planParts );
		}

		return new SimpleInsertQueryPlan( sqmInsert, domainParameterXref );
	}

	protected boolean hasIdentifierAssigned(SqmInsertStatement<?> sqmInsert, EntityPersister entityDescriptor) {
		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		final String partName = identifierMapping instanceof SingleAttributeIdentifierMapping
				? identifierMapping.getAttributeName()
				: EntityIdentifierMapping.ID_ROLE_NAME;
		for ( SqmPath<?> insertionTargetPath : sqmInsert.getInsertionTargetPaths() ) {
			final SqmPath<?> lhs = insertionTargetPath.getLhs();
			if ( !( lhs instanceof SqmRoot<?> ) ) {
				continue;
			}
			final SqmPathSource<?> referencedPathSource = insertionTargetPath.getReferencedPathSource();
			if ( referencedPathSource.getPathName().equals( partName ) ) {
				return true;
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
		applyTupleTransformer( transformer );
		//noinspection unchecked
		return (SqmQueryImplementor<T>) this;
	}

	@Override
	public SqmQueryImplementor<R> setResultListTransformer(ResultListTransformer<R> transformer) {
		applyResultListTransformer( transformer );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setMaxResults(int maxResult) {
		applyMaxResults( maxResult );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setFirstResult(int startPosition) {
		applyFirstResult( startPosition );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setHibernateFlushMode(FlushMode flushMode) {
		super.setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setFlushMode(FlushModeType flushMode) {
		applyJpaFlushMode( flushMode );
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
	public FlushModeType getFlushMode() {
		return getJpaFlushMode();
	}

	@Override
	public Query<R> setOrder(Order<? super R> order) {
		super.setOrder(order);
		return this;
	}

	@Override
	public Query<R> setOrder(List<Order<? super R>> orders) {
		super.setOrder(orders);
		return this;
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
		applyHint( hintName, value );
		return this;
	}

	@Override
	public Query<R> setEntityGraph(EntityGraph<R> graph, GraphSemantic semantic) {
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
	public NamedQueryMemento toMemento(String name) {
		if ( CRITERIA_HQL_STRING.equals( getQueryString() ) ) {
			final SqmStatement<R> sqmStatement;
			if ( !getSession().isCriteriaCopyTreeEnabled() ) {
				sqmStatement = getSqmStatement().copy( SqmCopyContext.simpleContext() );
			}
			else {
				// the statement has already been copied
				sqmStatement = getSqmStatement();
			}
			return new NamedCriteriaQueryMementoImpl(
					name,
					getResultType(),
					sqmStatement,
					getQueryOptions().getLimit().getFirstRow(),
					getQueryOptions().getLimit().getMaxRows(),
					isCacheable(),
					getCacheRegion(),
					getCacheMode(),
					getHibernateFlushMode(),
					isReadOnly(),
					getLockOptions(),
					getTimeout(),
					getFetchSize(),
					getComment(),
					Collections.emptyMap(),
					getHints()
			);
		}

		return new NamedHqlQueryMementoImpl(
				name,
				getResultType(),
				getQueryString(),
				getQueryOptions().getLimit().getFirstRow(),
				getQueryOptions().getLimit().getMaxRows(),
				isCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getHibernateFlushMode(),
				isReadOnly(),
				getLockOptions(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				Collections.emptyMap(),
				getHints()
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// unwrap

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( cls.isInstance( this ) ) {
			return (T) this;
		}

		if ( cls.isInstance( parameterMetadata ) ) {
			return (T) parameterMetadata;
		}

		if ( cls.isInstance( parameterBindings ) ) {
			return (T) parameterBindings;
		}

		if ( cls.isInstance( sqm ) ) {
			return (T) sqm;
		}

		if ( cls.isInstance( getQueryOptions() ) ) {
			return (T) getQueryOptions();
		}

		if ( cls.isInstance( getQueryOptions().getAppliedGraph() ) ) {
			return (T) getQueryOptions().getAppliedGraph();
		}

		if ( EntityGraphQueryHint.class.isAssignableFrom( cls ) ) {
			return (T) new EntityGraphQueryHint( getQueryOptions().getAppliedGraph() );
		}

		throw new PersistenceException( "Unrecognized unwrap type [" + cls.getName() + "]" );
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

	@Override
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

	@Override
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

	@Override
	public SqmQueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// optional object loading

	@Override
	public void setOptionalId(Serializable id) {
		throw new UnsupportedOperationException( "Not sure yet how to handle this in SQM based queries, but for sure it will be different" );
	}

	@Override
	public void setOptionalEntityName(String entityName) {
		throw new UnsupportedOperationException( "Not sure yet how to handle this in SQM based queries, but for sure it will be different" );
	}

	@Override
	public void setOptionalObject(Object optionalObject) {
		throw new UnsupportedOperationException( "Not sure yet how to handle this in SQM based queries, but for sure it will be different" );
	}
}
