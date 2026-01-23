/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.UnknownProfileException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.hql.internal.QuerySplitter;
import org.hibernate.query.named.NamedSqmQueryMemento;
import org.hibernate.query.named.internal.CriteriaSelectionMementoImpl;
import org.hibernate.query.named.internal.HqlSelectionMementoImpl;
import org.hibernate.query.named.internal.SqmSelectionMemento;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.AggregatedSelectQueryPlanImpl;
import org.hibernate.query.sqm.internal.ConcreteSqmSelectQueryPlan;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.spi.InterpretationsKeySource;
import org.hibernate.query.sqm.spi.SqmStatementAccess;
import org.hibernate.query.sqm.tree.AbstractSqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.sql.results.spi.SingleResultConsumer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static java.lang.Boolean.TRUE;
import static org.hibernate.cfg.QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH;
import static org.hibernate.query.KeyedPage.KeyInterpretation.KEY_OF_FIRST_ON_NEXT_PAGE;
import static org.hibernate.query.QueryLogging.QUERY_MESSAGE_LOGGER;
import static org.hibernate.query.internal.KeyBasedPagination.paginate;
import static org.hibernate.query.internal.KeyedResult.collectKeys;
import static org.hibernate.query.internal.KeyedResult.collectResults;
import static org.hibernate.query.internal.QueryHelper.buildTupleMetadata;
import static org.hibernate.query.internal.QueryHelper.determineResultType;
import static org.hibernate.query.spi.SqlOmittingQueryOptions.omitSqlQueryOptions;
import static org.hibernate.query.sqm.internal.SqmInterpretationsKey.createInterpretationsKey;
import static org.hibernate.query.sqm.internal.SqmUtil.validateCriteriaQuery;
import static org.hibernate.query.sqm.tree.SqmCopyContext.noParamCopyContext;
import static org.hibernate.query.sqm.tree.SqmCopyContext.simpleContext;

/// Implementation of `SelectionQuery` based on a [SqmSelectStatement] SQM AST.
///
/// @author Steve Ebersole
@SuppressWarnings("unchecked")
public class SelectionQueryImpl<R>
		extends AbstractSqmQuery<R>
		implements SelectionQueryImplementor<R>, SqmStatementAccess<R>, InterpretationsKeySource {
	private final String hql;
	private final Object queryStringCacheKey;
	private final SqmSelectStatement<R> sqm;
	private final Class<R> providedResultType;
	private final Class<?> actualResultType;
	private final TupleMetadata tupleMetadata;

	private final ParameterMetadataImplementor parameterMetadata;
	private final DomainParameterXref domainParameterXref;
	private final QueryParameterBindings parameterBindings;


	/// Constructor used for HQL queries.
	///
	/// @param hql The query string.
	/// @param hqlInterpretation The interpretation of the query string.
	/// @param providedResultType Explicitly specified result type.
	/// @param providedResultGraph Explicitly specified result graph.
	/// @param session The session from which this query originates.
	///
	/// @see jakarta.persistence.EntityHandler#createQuery(String)
	/// @see jakarta.persistence.EntityHandler#createQuery(String,Class)
	/// @see jakarta.persistence.EntityHandler#createQuery(String,EntityGraph)
	public SelectionQueryImpl(
			String hql,
			HqlInterpretation<R> hqlInterpretation,
			Class<R> providedResultType,
			RootGraphImplementor<R> providedResultGraph,
			SharedSessionContractImplementor session) {
		super( session );

		this.hql = hql;
		this.queryStringCacheKey = hql;

		this.parameterMetadata = hqlInterpretation.getParameterMetadata();
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();
		this.parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		this.sqm = SqmUtil.asSelectStatement( hqlInterpretation.getSqmStatement(), hql );

		// todo (jpa4) : unless this is a dynamic model...
		//  	in which case these type checks will be fubar
		if ( providedResultType == null ) {
			if ( providedResultGraph != null ) {
				providedResultType = providedResultGraph.getGraphedType().getJavaType();
			}
		}
		this.providedResultType = providedResultType;
		this.actualResultType = determineResultType( sqm, providedResultType );
		this.tupleMetadata = buildTupleMetadata( sqm, providedResultType, getQueryOptions().getTupleTransformer() );
		if ( providedResultGraph != null ) {
			setEntityGraph( providedResultGraph, GraphSemantic.LOAD );
		}
		hqlInterpretation.validateResultType( actualResultType );

		setComment( hql );
	}


	/// Creates a [org.hibernate.query.SelectionQuery] from a named HQL memento.
	/// @see HqlSelectionMementoImpl#toSelectionQuery
	public SelectionQueryImpl(
			HqlSelectionMementoImpl<?> memento,
			HqlInterpretation<R> interpretation,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		this( memento.getHqlString(),
				interpretation,
				expectedResultType,
				null,
				session );
		applyMementoOptions( memento );
	}

	/// Creates a [org.hibernate.query.SelectionQuery] from a named criteria query memento.
	/// @see CriteriaSelectionMementoImpl#toSelectionQuery
	public SelectionQueryImpl(
			NamedSqmQueryMemento<?> memento,
			SqmSelectStatement<R> statement,
			Class<R> resultType,
			SharedSessionContractImplementor session) {
		this( statement, resultType, session );
		applyMementoOptions( memento );
	}

	/// Form used for criteria queries
	public SelectionQueryImpl(
			SqmSelectStatement<R> criteria,
			Class<R> expectedResultType,
			SharedSessionContractImplementor producer) {
		this( criteria, producer.isCriteriaCopyTreeEnabled(), expectedResultType, producer );
	}

	/**
	 * Used for specifications.
	 */
	public SelectionQueryImpl(
			SqmSelectStatement<R> incomingSqm,
			boolean copyAst,
			Class<R> providedResultType,
			SharedSessionContractImplementor session) {
		super( session );

		hql = CRITERIA_HQL_STRING;
		sqm = copyAst ? incomingSqm.copy( simpleContext() ) : incomingSqm;
		queryStringCacheKey = sqm;
		// Cache immutable query plans by default
		setQueryPlanCacheable( !copyAst || session.isCriteriaPlanCacheEnabled() );

		domainParameterXref = DomainParameterXref.from( sqm );
		parameterMetadata = domainParameterXref.hasParameters()
				? new ParameterMetadataImpl( domainParameterXref.getQueryParameters() )
				: ParameterMetadataImpl.EMPTY;
		parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		// Parameters might be created through HibernateCriteriaBuilder.value which we need to bind here
		bindValueBindCriteriaParameters( domainParameterXref, parameterBindings );

		validateQuery( providedResultType, sqm, hql );

		this.providedResultType = providedResultType;
		actualResultType = providedResultType;
		tupleMetadata = buildTupleMetadata( incomingSqm, providedResultType, getQueryOptions().getTupleTransformer() );
	}

	/// Used for [KeyedResult] handling.
	<E> SelectionQueryImpl(@SuppressWarnings("rawtypes") SelectionQueryImpl original, KeyedPage<E> keyedPage) {
		super( original );

		final var page = keyedPage.getPage();
		final var key = keyedPage.getKey();
		final var keyDefinition = keyedPage.getKeyDefinition();
		final var appliedKeyDefinition =
				keyedPage.getKeyInterpretation() == KEY_OF_FIRST_ON_NEXT_PAGE
						? Order.reverse( keyDefinition )
						: keyDefinition;

		//noinspection unchecked
		sqm = (SqmSelectStatement<R>) paginate(
				appliedKeyDefinition,
				key,
				// Change the query source to CRITERIA, because we will change the query and introduce parameters
				(SqmSelectStatement<KeyedResult<E>>)
						original.getSqmStatement()
								.copy( noParamCopyContext( SqmQuerySource.CRITERIA ) ),
				original.getSqmStatement().nodeBuilder()
		);
		if ( getSession().isCriteriaPlanCacheEnabled() ) {
			queryStringCacheKey = sqm.toHqlString();
			setQueryPlanCacheable( true );
		}
		else {
			queryStringCacheKey = sqm;
		}
		hql = CRITERIA_HQL_STRING;

		domainParameterXref = DomainParameterXref.from( sqm );
		parameterMetadata =
				domainParameterXref.hasParameters()
						? new ParameterMetadataImpl( domainParameterXref.getQueryParameters() )
						: ParameterMetadataImpl.EMPTY;

		// Just use the original parameter bindings since this object is never going to be mutated
		parameterBindings = parameterMetadata.createBindings( original.getSession().getSessionFactory() );
		original.getQueryParameterBindings().visitBindings( this::setBindValues );

		// Parameters might be created through HibernateCriteriaBuilder.value which we need to bind here
		bindValueBindCriteriaParameters( domainParameterXref, parameterBindings );

		//noinspection unchecked
		providedResultType = (Class<R>) KeyedResult.class;
		actualResultType = QueryHelper.determineResultType( sqm, providedResultType );
		tupleMetadata = null;

		setMaxResults( page.getMaxResults() + 1 );
		if ( key == null ) {
			setFirstResult( page.getFirstResult() );
		}
	}

	@Override
	public String getQueryString() {
		return hql;
	}

	@Override
	public SqmSelectStatement<R> getSqmStatement() {
		return sqm;
	}

	@Override
	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	@Override
	public Class<R> getResultType() {
		return (Class<R>) actualResultType;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Casts

	@Override
	public SelectionQueryImplementor<R> asSelectionQuery() {
		return this;
	}

	@Override
	public <X> SelectionQueryImplementor<X> asSelectionQuery(Class<X> type) {
		// todo (jpa4) : type validation
		return (SelectionQueryImplementor<X>) this;
	}

	@Override
	public <X> SelectionQueryImplementor<X> asSelectionQuery(EntityGraph<X> entityGraph) {
		// todo (jpa4) : type validation
		return (SelectionQueryImplementor<X>) this;
	}

	@Override
	public <X> SelectionQuery<X> asSelectionQuery(EntityGraph<X> entityGraph, GraphSemantic graphSemantic) {
		// todo (jpa4) : type validation
		return (SelectionQueryImplementor<X>) this;
	}

	@Override
	public <X> SelectionQueryImplementor<X> ofType(Class<X> type) {
		return asSelectionQuery( type );
	}

	@Override
	public <X> SelectionQueryImplementor<X> withEntityGraph(EntityGraph<X> entityGraph) {
		return (SelectionQueryImplementor<X>) asSelectionQuery( entityGraph, GraphSemantic.LOAD );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override
	public boolean isReadOnly() {
		return queryOptions.isReadOnly() == null
				? session.isDefaultReadOnly()
				: queryOptions.isReadOnly();
	}

	@Override
	public SelectionQueryImplementor<R> setReadOnly(boolean readOnly) {
		queryOptions.setReadOnly( readOnly );
		return this;
	}

	@Override
	public Integer getFetchSize() {
		return getQueryOptions().getFetchSize();
	}

	@Override
	public SelectionQueryImplementor<R> setFetchSize(int fetchSize) {
		queryOptions.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public SelectionQueryImplementor<R> setFirstResult(int startPosition) {
		session.checkOpen();
		if ( startPosition < 0 ) {
			throw new IllegalArgumentException( "First result cannot be negative" );
		}
		queryOptions.getLimit().setFirstRow( startPosition );
		return this;
	}

	@Override
	public SelectionQueryImplementor<R> setMaxResults(int maxResults) {
		if ( maxResults < 0 ) {
			throw new IllegalArgumentException( "Max results cannot be negative" );
		}
		session.checkOpen();
		queryOptions.getLimit().setMaxRows( maxResults );
		return this;
	}

	@Override
	public SelectionQueryImplementor<R> setPage(Page page) {
		setMaxResults( page.getMaxResults() );
		setFirstResult( page.getFirstResult() );
		return this;
	}

	@Override
	public LockModeType getLockMode() {
		session.checkOpen( false );
		return queryOptions.getLockOptions().getLockMode().toJpaLockMode();
	}

	@Override
	public SelectionQueryImplementor<R> setLockMode(LockModeType lockMode) {
		session.checkOpen( false );
		queryOptions.getLockOptions().setLockMode( LockMode.fromJpaLockMode( lockMode ) );
		return this;
	}

	@Override
	public LockMode getHibernateLockMode() {
		return queryOptions.getLockOptions().getLockMode();
	}

	@Override
	public SelectionQueryImplementor<R> setHibernateLockMode(LockMode lockMode) {
		queryOptions.getLockOptions().setLockMode( lockMode );
		return this;
	}

	@Override
	public SelectionQueryImplementor<R> setLockTimeout(Timeout lockTimeout) {
		queryOptions.getLockOptions().setTimeout( lockTimeout );
		return this;
	}

	@Override
	public PessimisticLockScope getLockScope() {
		return queryOptions.getLockOptions().getLockScope();
	}

	@Override
	public SelectionQueryImplementor<R> setLockScope(PessimisticLockScope lockScope) {
		queryOptions.getLockOptions().setLockScope( lockScope );
		return this;
	}

	@Override
	public SelectionQueryImplementor<R> setFollowOnLockingStrategy(Locking.FollowOn strategy) {
		queryOptions.getLockOptions().setFollowOnStrategy( strategy );
		return this;
	}

	@Override
	public SelectionQueryImplementor<R> setFollowOnStrategy(Locking.FollowOn followOnStrategy) {
		queryOptions.getLockOptions().setFollowOnStrategy( followOnStrategy );
		return this;
	}

	@Override
	public boolean isCacheable() {
		return queryOptions.isResultCachingEnabled() == TRUE;
	}

	@Override
	public SelectionQueryImplementor<R> setCacheable(boolean cacheable) {
		queryOptions.setResultCachingEnabled( cacheable );
		return this;
	}

	@Override
	public CacheMode getCacheMode() {
		return queryOptions.getCacheMode();
	}

	@Override
	public SelectionQueryImplementor<R> setCacheMode(CacheMode cacheMode) {
		queryOptions.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return queryOptions.getCacheRetrieveMode();
	}

	@Override
	public SelectionQueryImplementor<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		queryOptions.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return queryOptions.getCacheStoreMode();
	}

	@Override
	public SelectionQueryImplementor<R> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		queryOptions.setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override
	public String getCacheRegion() {
		return queryOptions.getResultCacheRegionName();
	}

	@Override
	public SelectionQueryImplementor<R> setCacheRegion(String cacheRegion) {
		queryOptions.setResultCacheRegionName( cacheRegion );
		return this;
	}

	@Override
	public boolean isQueryPlanCacheable() {
		return queryOptions.getQueryPlanCachingEnabled() == TRUE;
	}

	@Override
	public SelectionQueryImplementor<R> setQueryPlanCacheable(boolean queryPlanCacheable) {
		queryOptions.setQueryPlanCachingEnabled( queryPlanCacheable );
		return this;
	}

	@Override
	public SelectionQueryImplementor<R> setHint(String hintName, Object value) {
		return (SelectionQueryImplementor<R>) super.setHint( hintName, value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityGraph<? super R> getEntityGraph() {
		return (EntityGraph<? super R>) getQueryOptions().getAppliedGraph().getGraph();
	}

	@Override
	public SelectionQueryImplementor<R> setEntityGraph(EntityGraph<? super R> entityGraph) {
		setEntityGraph( entityGraph, GraphSemantic.LOAD );
		return this;
	}

	@Override
	public SelectionQueryImplementor<R> setEntityGraph(EntityGraph<? super R> graph, GraphSemantic semantic) {
		queryOptions.applyGraph( (RootGraphImplementor<?>) graph, semantic );
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> SelectionQueryImplementor<T> setTupleTransformer(TupleTransformer<T> transformer) {
		// todo (jpa4) : not the best option.  similar to notes on `ofType()`.
		queryOptions.setTupleTransformer( transformer );
		//noinspection rawtypes
		return (SelectionQueryImplementor) this;
	}

	@Override
	public SelectionQueryImplementor<R> setResultListTransformer(ResultListTransformer<R> transformer) {
		queryOptions.setResultListTransformer( transformer );
		return this;
	}

	@Override
	public SelectionQueryImplementor<R> enableFetchProfile(String profileName) {
		if ( getSessionFactory().containsFetchProfileDefinition( profileName ) ) {
			getQueryOptions().enableFetchProfile( profileName );
			return this;
		}
		else {
			throw new UnknownProfileException( profileName );
		}
	}

	@Override
	public SelectionQueryImplementor<R> disableFetchProfile(String profileName) {
		getQueryOptions().disableFetchProfile( profileName );
		return this;
	}
	@Override
	public SelectionQueryImplementor<R> setComment(String comment) {
		return (SelectionQueryImplementor<R>) super.setComment( comment );
	}

	@Override
	public SelectionQueryImplementor<R> setQueryFlushMode(QueryFlushMode queryFlushMode) {
		return (SelectionQueryImplementor<R>) super.setQueryFlushMode( queryFlushMode );
	}

	@Override
	public SelectionQueryImplementor<R> setTimeout(int timeout) {
		return (SelectionQueryImplementor<R>) super.setTimeout( timeout );
	}

	@Override
	public SelectionQueryImplementor<R> setTimeout(Integer timeout) {
		return (SelectionQueryImplementor<R>) super.setTimeout( timeout );
	}

	@Override
	public SelectionQueryImplementor<R> setTimeout(Timeout timeout) {
		return (SelectionQueryImplementor<R>) super.setTimeout( timeout );
	}

	@Override
	public SelectionQueryImplementor<R> setFlushMode(FlushModeType flushMode) {
		return (SelectionQueryImplementor<R>) super.setFlushMode( flushMode );
	}

	@Override
	public SelectionQueryImplementor<R> addQueryHint(String hint) {
		return (SelectionQueryImplementor<R>)super.addQueryHint( hint );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hints

	@Override
	protected void applyFollowOnStrategyHint(Object value) {
		queryOptions.getLockOptions().setFollowOnStrategy( Locking.FollowOn.fromHint( value ) );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter handling

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
		return parameterBindings;
	}

	@Override
	public SelectionQueryImplementor<R> setParameter(String name, Object value) {
		return (SelectionQueryImplementor<R>) super.setParameter( name, value );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameter(String name, P value, Class<P> javaType) {
		return (SelectionQueryImplementor<R>) super.setParameter( name, value, javaType );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameter(String name, P value, Type<P> type) {
		return (SelectionQueryImplementor<R>) super.setParameter( name, value, type );
	}

	@Override
	public SelectionQueryImplementor<R> setParameter(int position, Object value) {
		return (SelectionQueryImplementor<R>) super.setParameter( position, value );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameter(int position, P value, Class<P> javaType) {
		return (SelectionQueryImplementor<R>) super.setParameter( position, value, javaType );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameter(int position, P value, Type<P> type) {
		return (SelectionQueryImplementor<R>) super.setParameter( position, value, type );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value) {
		return (SelectionQueryImplementor<R>) super.setParameter( parameter, value );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Class<P> javaType) {
		return (SelectionQueryImplementor<R>) super.setParameter( parameter, value, javaType );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Type<P> type) {
		return (SelectionQueryImplementor<R>) super.setParameter( parameter, value, type );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameter(Parameter<P> parameter, P value) {
		return (SelectionQueryImplementor<R>) super.setParameter( parameter, value );
	}

	@Override
	public SelectionQueryImplementor<R> setProperties(@SuppressWarnings("rawtypes") Map map) {
		return (SelectionQueryImplementor<R>) super.setProperties( map );
	}

	@Override
	public SelectionQueryImplementor<R> setProperties(Object bean) {
		return (SelectionQueryImplementor<R>) super.setProperties( bean );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converterClass) {
		return (SelectionQueryImplementor<R>) super.setConvertedParameter( name, value, converterClass );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converterClass) {
		return (SelectionQueryImplementor<R>) super.setConvertedParameter( position, value, converterClass );
	}

	@Override
	public SelectionQueryImplementor<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		return (SelectionQueryImplementor<R>) super.setParameterList( name, values );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		return (SelectionQueryImplementor<R>) super.setParameterList( name, values, javaType );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Type<P> type) {
		return (SelectionQueryImplementor<R>) super.setParameterList( name, values, type );
	}

	@Override
	public SelectionQueryImplementor<R> setParameterList(String name, Object[] values) {
		return (SelectionQueryImplementor<R>) super.setParameterList( name, values );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(String name, P[] values, Class<P> javaType) {
		return (SelectionQueryImplementor<R>) super.setParameterList( name, values, javaType );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(String name, P[] values, Type<P> type) {
		return (SelectionQueryImplementor<R>) super.setParameterList( name, values, type );
	}

	@Override
	public SelectionQueryImplementor<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		return (SelectionQueryImplementor<R>) super.setParameterList( position, values );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		return (SelectionQueryImplementor<R>) super.setParameterList( position, values, javaType );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Type<P> type) {
		return (SelectionQueryImplementor<R>) super.setParameterList( position, values, type );
	}

	@Override
	public SelectionQueryImplementor<R> setParameterList(int position, Object[] values) {
		return (SelectionQueryImplementor<R>) super.setParameterList( position, values );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaType) {
		return (SelectionQueryImplementor<R>) super.setParameterList( position, values, javaType );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(int position, P[] values, Type<P> type) {
		return (SelectionQueryImplementor<R>) super.setParameterList( position, values, type );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		return (SelectionQueryImplementor<R>) super.setParameterList( parameter, values );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		return (SelectionQueryImplementor<R>) super.setParameterList( parameter, values, javaType );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type) {
		return (SelectionQueryImplementor<R>) super.setParameterList( parameter, values, type );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		return (SelectionQueryImplementor<R>) super.setParameterList( parameter, values );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		return (SelectionQueryImplementor<R>) super.setParameterList( parameter, values, javaType );
	}

	@Override
	public <P> SelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type) {
		return (SelectionQueryImplementor<R>) super.setParameterList( parameter, values, type );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	protected List<R> doList() {
		final var statement = getSqmStatement();
		final boolean containsCollectionFetches =
				//TODO: why is this different from QuerySqmImpl.doList()?
				statement.containsCollectionFetches();
		final boolean hasLimit = hasLimit( statement, getQueryOptions() );
		final boolean needsDistinct = needsDistinct( containsCollectionFetches, hasLimit, statement );
		final var list = resolveQueryPlan()
				.performList( executionContext( hasLimit, containsCollectionFetches ) );
		return needsDistinct ? handleDistinct( hasLimit, statement, list ) : list;
	}

	protected ScrollableResultsImplementor<R> doScroll(ScrollMode scrollMode) {
		return resolveQueryPlan().performScroll( scrollMode, this );
	}

	@Override
	public long getResultCount() {
		final var context = new DelegatingDomainQueryExecutionContext(this) {
			@Override
			public QueryOptions getQueryOptions() {
				return QueryOptions.NONE;
			}
		};
		final ConcreteSqmSelectQueryPlan<Long> queryPlan = buildConcreteQueryPlan(
				getSqmStatement().createCountQuery(),
				Long.class,
				null,
				queryOptions
		);
		return queryPlan.executeQuery( context, SingleResultConsumer.instance() );
	}

	@Override
	public KeyedResultList<R> getKeyedResultList(KeyedPage<R> keyedPage) {
		if ( keyedPage == null ) {
			throw new IllegalArgumentException( "KeyedPage was null" );
		}
		final var results =
				new SelectionQueryImpl<KeyedResult<R>>( this, keyedPage )
						.getResultList();
		final int pageSize = keyedPage.getPage().getSize();
		return new KeyedResultList<>(
				collectResults( results, pageSize, keyedPage.getKeyInterpretation() ),
				collectKeys( results, pageSize ),
				keyedPage,
				nextPage( keyedPage, results ),
				previousPage( keyedPage, results )
		);
	}

	private static <R> KeyedPage<R> nextPage(KeyedPage<R> keyedPage, List<KeyedResult<R>> results) {
		if ( keyedPage.getKeyInterpretation() == KEY_OF_FIRST_ON_NEXT_PAGE ) {
			// the results come in reverse order
			return !results.isEmpty()
					? keyedPage.nextPage( results.get(0).getKey() )
					: null;
		}
		else {
			final int pageSize = keyedPage.getPage().getSize();
			return results.size() == pageSize + 1
					? keyedPage.nextPage( results.get(pageSize - 1).getKey() )
					: null;
		}
	}

	private static <R> KeyedPage<R> previousPage(KeyedPage<R> keyedPage, List<KeyedResult<R>> results) {
		if ( keyedPage.getKeyInterpretation() == KEY_OF_FIRST_ON_NEXT_PAGE ) {
			// the results come in reverse order
			final int pageSize = keyedPage.getPage().getSize();
			return results.size() == pageSize + 1
					? keyedPage.previousPage( results.get(pageSize - 1).getKey() )
					: null;
		}
		else {
			return !results.isEmpty()
					? keyedPage.previousPage( results.get(0).getKey() )
					: null;
		}
	}

	@Override
	protected int doExecuteUpdate() {
		final var msg = "Attempting to get a execute-update a selection query";
		throw new IllegalStateException( msg, new IllegalMutationQueryException( msg, hql ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CacheabilityInfluencers

	@Override
	public Object getQueryStringCacheKey() {
		return queryStringCacheKey;
	}

	@Override
	public int @Nullable [] unnamedParameterIndices() {
		return QueryHelper.unnamedParameterIndices( domainParameterXref );
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return session.getLoadQueryInfluencers();
	}

	@Override
	public BooleanSupplier hasMultiValuedParameterBindingsChecker() {
		return this::hasMultiValuedParameterBindings;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TypedQueryReferenceProducer

	public SqmSelectionMemento<R> toSelectionMemento(String name) {
		// todo (jpa4) : simply pass QueryOptions to the memento constructors?
		if ( CRITERIA_HQL_STRING.equals( hql ) ) {
			//noinspection rawtypes
			return new CriteriaSelectionMementoImpl(
					name,
					actualResultType,
					getEntityGraph() == null ? null : getEntityGraph().getName(),
					sqm,
					queryOptions.getFirstRow(),
					queryOptions.getMaxRows(),
					queryOptions.isResultCachingEnabled(),
					queryOptions.getResultCacheRegionName(),
					queryOptions.getCacheMode(),
					queryOptions.getFlushMode(),
					queryOptions.isReadOnly(),
					queryOptions.getLockOptions().getLockMode(),
					queryOptions.getLockOptions().getScope(),
					queryOptions.getLockOptions().getTimeout(),
					queryOptions.getLockOptions().getFollowOnStrategy(),
					queryOptions.getTimeout(),
					queryOptions.getFetchSize(),
					queryOptions.getComment(),
					Map.of(),
					Map.of()
			);
		}
		else {
			return new HqlSelectionMementoImpl(
					name,
					hql,
					actualResultType,
					getEntityGraph() == null ? null : getEntityGraph().getName(),
					queryOptions.getFlushMode(),
					queryOptions.getTimeout(),
					queryOptions.getComment(),
					queryOptions.isReadOnly(),
					queryOptions.getFetchSize(),
					queryOptions.getFirstRow(),
					queryOptions.getMaxRows(),
					queryOptions.isResultCachingEnabled(),
					queryOptions.getCacheMode(),
					queryOptions.getResultCacheRegionName(),
					queryOptions.getLockOptions().getLockMode(),
					queryOptions.getLockOptions().getScope(),
					queryOptions.getLockOptions().getTimeout(),
					queryOptions.getLockOptions().getFollowOnStrategy(),
					Map.of(),
					Map.of()

			);
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Utilities

	private static <R> void validateQuery(Class<R> expectedResultType, SqmStatement<R> sqm, String hql) {
		if ( sqm instanceof SqmSelectStatement<R> selectStatement ) {
			final var queryPart = selectStatement.getQueryPart();
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

	private SelectQueryPlan<R> resolveQueryPlan() {
		final var cacheKey = createInterpretationsKey( this );
		return cacheKey == null
				? buildSelectQueryPlan()
				: getInterpretationCache()
						.resolveSelectQueryPlan( cacheKey, this::buildSelectQueryPlan );
	}

	protected SelectQueryPlan<R> buildSelectQueryPlan() {
		final var statement = getSqmStatement();
		final var concreteSqmStatements = QuerySplitter.split( statement );
		return concreteSqmStatements.length > 1
				? buildAggregatedQueryPlan( concreteSqmStatements )
				: buildConcreteQueryPlan( concreteSqmStatements[0] );
	}

	private SelectQueryPlan<R> buildAggregatedQueryPlan(SqmSelectStatement<R>[] concreteSqmStatements) {
		@SuppressWarnings("unchecked")
		final SelectQueryPlan<R>[] aggregatedQueryPlans = new SelectQueryPlan[ concreteSqmStatements.length ];
		// todo (6.0) : we want to make sure that certain thing (ResultListTransformer, etc) only get applied at the aggregator-level
		for ( int i = 0, length = concreteSqmStatements.length; i < length; i++ ) {
			aggregatedQueryPlans[i] = buildConcreteQueryPlan( concreteSqmStatements[i] );
		}
		return new AggregatedSelectQueryPlanImpl<>( aggregatedQueryPlans );
	}

	protected SelectQueryPlan<R> buildConcreteQueryPlan(SqmSelectStatement<R> concreteSqmStatement) {
		return buildConcreteQueryPlan(
				concreteSqmStatement,
				providedResultType,
				tupleMetadata,
				queryOptions
		);
	}

	protected <T> ConcreteSqmSelectQueryPlan<T> buildConcreteQueryPlan(
			SqmSelectStatement<T> concreteSqmStatement,
			Class<T> expectedResultType,
			TupleMetadata tupleMetadata,
			QueryOptions queryOptions) {
		return new ConcreteSqmSelectQueryPlan<>(
				concreteSqmStatement,
				getQueryString(),
				domainParameterXref,
				expectedResultType,
				tupleMetadata,
				queryOptions
		);
	}

	protected static boolean hasLimit(SqmSelectStatement<?> sqm, MutableQueryOptions queryOptions) {
		return queryOptions.hasLimit() || sqm.getFetch() != null || sqm.getOffset() != null;
	}

	protected int first(boolean hasLimit, SqmSelectStatement<?> sqmStatement) {
		return !hasLimit || getQueryOptions().getLimit().getFirstRow() == null
				? getIntegerLiteral( sqmStatement.getOffset(), 0 )
				: getQueryOptions().getLimit().getFirstRow();
	}

	protected int max(boolean hasLimit, SqmSelectStatement<?> sqmStatement, List<R> list) {
		return !hasLimit || getQueryOptions().getLimit().getMaxRows() == null
				? getMaxRows( sqmStatement, list.size() )
				: getQueryOptions().getLimit().getMaxRows();
	}

	protected boolean needsDistinct(boolean containsCollectionFetches, boolean hasLimit, SqmSelectStatement<?> sqmStatement) {
		return containsCollectionFetches
			&& ( hasLimit || sqmStatement.usesDistinct() || hasAppliedGraph( getQueryOptions() ) );
	}

	protected static boolean hasAppliedGraph(MutableQueryOptions queryOptions) {
		final var appliedGraph = queryOptions.getAppliedGraph();
		return appliedGraph != null && appliedGraph.getSemantic() != null;
	}

	private DomainQueryExecutionContext executionContext(boolean hasLimit, boolean containsCollectionFetches) {
		if ( hasLimit && containsCollectionFetches ) {
			errorOrLogForPaginationWithCollectionFetch();
			final var originalQueryOptions = getQueryOptions();
			final var normalizedQueryOptions = omitSqlQueryOptions( originalQueryOptions, true, false );
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
		else {
			return this;
		}
	}

	private List<R> handleDistinct(boolean hasLimit, SqmSelectStatement<?> statement, List<R> list) {
		int includedCount = -1;
		// NOTE: 'firstRow' is zero-based
		final int first = first( hasLimit, statement );
		final int max = max( hasLimit, statement, list );
		final List<R> distinctList = new ArrayList<>( list.size() );
		final IdentitySet<Object> distinction = new IdentitySet<>( list.size() );
		for ( final R result : list) {
			if ( distinction.add( result ) ) {
				includedCount++;
				if ( includedCount >= first ) {
					distinctList.add( result );
					// NOTE: 'max-1' because 'first' is zero-based while 'max' is not
					if ( max >= 0 && includedCount - first >= max - 1 ) {
						break;
					}
				}
			}
		}
		return distinctList;
	}

	private <T> void setBindValues(QueryParameter<?> parameter, QueryParameterBinding<T> binding) {
		final var parameterBinding = parameterBindings.getBinding( binding.getQueryParameter() );
		final var explicitTemporalPrecision = binding.getExplicitTemporalPrecision();
		if ( explicitTemporalPrecision != null ) {
			if ( binding.isMultiValued() ) {
				parameterBinding.setBindValues( binding.getBindValues(), explicitTemporalPrecision );
			}
			else {
				parameterBinding.setBindValue( binding.getBindValue(), explicitTemporalPrecision );
			}
		}
		else {
			final var bindType = binding.getBindType();
			if ( binding.isMultiValued() ) {
				parameterBinding.setBindValues( binding.getBindValues(), bindType );
			}
			else {
				parameterBinding.setBindValue( binding.getBindValue(), bindType );
			}
		}
		parameterBinding.setType( binding.getType() );
	}

	protected void errorOrLogForPaginationWithCollectionFetch() {
		if ( getSessionFactory().getSessionFactoryOptions()
				.isFailOnPaginationOverCollectionFetchEnabled() ) {
			throw new HibernateException(
					"setFirstResult() or setMaxResults() specified with collection fetch join "
					+ "(in-memory pagination was about to be applied, but '"
					+ FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH
					+ "' is enabled)"
			);
		}
		else {
			QUERY_MESSAGE_LOGGER.firstOrMaxResultsSpecifiedWithCollectionFetch();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	@Override @Deprecated @SuppressWarnings("deprecation")
	public SelectionQueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType) {
		return (SelectionQueryImplementor<R>) super.setParameter( name, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public SelectionQueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType) {
		return (SelectionQueryImplementor<R>) super.setParameter( position, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public SelectionQueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		return (SelectionQueryImplementor<R>) super.setParameter( param, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public SelectionQueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		return (SelectionQueryImplementor<R>) super.setParameter( param, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public SelectionQueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		return (SelectionQueryImplementor<R>) super.setParameter( name, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public SelectionQueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		return (SelectionQueryImplementor<R>) super.setParameter( name, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public SelectionQueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		return (SelectionQueryImplementor<R>) super.setParameter( position, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public SelectionQueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType) {
		return (SelectionQueryImplementor<R>) super.setParameter( position, value, temporalType );
	}
}
