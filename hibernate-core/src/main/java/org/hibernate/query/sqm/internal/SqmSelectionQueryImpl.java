/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.query.BindableType;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.criteria.internal.NamedCriteriaQueryMementoImpl;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.internal.DelegatingDomainQueryExecutionContext;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.SqmSelectionQuery;
import org.hibernate.query.sqm.spi.InterpretationsKeySource;
import org.hibernate.query.sqm.spi.SqmSelectionQueryImplementor;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.sql.results.spi.ResultsConsumer;
import org.hibernate.sql.results.spi.SingleResultConsumer;
import org.hibernate.type.descriptor.java.JavaType;

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
import static org.hibernate.query.KeyedPage.KeyInterpretation.KEY_OF_FIRST_ON_NEXT_PAGE;
import static org.hibernate.query.spi.SqlOmittingQueryOptions.omitSqlQueryOptions;
import static org.hibernate.query.sqm.internal.KeyBasedPagination.paginate;
import static org.hibernate.query.sqm.internal.SqmInterpretationsKey.createInterpretationsKey;
import static org.hibernate.query.sqm.internal.SqmUtil.isSelectionAssignableToResultType;
import static org.hibernate.query.sqm.tree.SqmCopyContext.noParamCopyContext;

/**
 * @author Steve Ebersole
 */
public class SqmSelectionQueryImpl<R> extends AbstractSqmSelectionQuery<R>
		implements SqmSelectionQueryImplementor<R>, InterpretationsKeySource {
	private final String hql;
	private Object queryStringCacheKey;
	private SqmSelectStatement<R> sqm;

	private ParameterMetadataImplementor parameterMetadata;
	private DomainParameterXref domainParameterXref;
	private QueryParameterBindings parameterBindings;

	private final Class<R> expectedResultType;
	private final Class<?> resultType;
	private final TupleMetadata tupleMetadata;

	/**
	 * Form used for HQL queries
	 */
	public SqmSelectionQueryImpl(
			String hql,
			HqlInterpretation<R> hqlInterpretation,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		super( session );
		this.hql = hql;
		this.queryStringCacheKey = hql;

		SqmUtil.verifyIsSelectStatement( hqlInterpretation.getSqmStatement(), hql );
		this.sqm = (SqmSelectStatement<R>) hqlInterpretation.getSqmStatement();

		this.parameterMetadata = hqlInterpretation.getParameterMetadata();
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();
		this.parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		this.expectedResultType = expectedResultType;
		this.resultType = determineResultType( sqm, expectedResultType );
		this.tupleMetadata = buildTupleMetadata( sqm, expectedResultType );

		hqlInterpretation.validateResultType( resultType );
		setComment( hql );
	}

	/**
	 * Creates a {@link org.hibernate.query.SelectionQuery}
	 * instance from a named HQL memento.
	 * Form used from {@link NamedHqlQueryMementoImpl}.
	 */
	public SqmSelectionQueryImpl(
			NamedHqlQueryMementoImpl<?> memento,
			Class<R> resultType,
			SharedSessionContractImplementor session) {
		this( memento.getHqlString(),
				interpretation( memento, resultType, session ),
				resultType, session );
		applySqmOptions( memento );
	}

	/**
	 * Creates a {@link org.hibernate.query.SelectionQuery}
	 * instance from a named criteria query memento.
	 * Form used from {@link NamedCriteriaQueryMementoImpl}
	 */
	public SqmSelectionQueryImpl(
			NamedCriteriaQueryMementoImpl<?> memento,
			SqmSelectStatement<R> selectStatement,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		this( selectStatement, expectedResultType, session );
		applySqmOptions( memento );
	}

	/**
	 * Form used for criteria queries
	 */
	public SqmSelectionQueryImpl(
			SqmSelectStatement<R> criteria,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		this( criteria, session.isCriteriaCopyTreeEnabled(), expectedResultType, session );
	}

	/**
	 * Form used for criteria queries
	 */
	public SqmSelectionQueryImpl(
			SqmSelectStatement<R> criteria,
			boolean copyAst,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		super( session );
		this.expectedResultType = expectedResultType;
		hql = CRITERIA_HQL_STRING;
		sqm = copyAst ? criteria.copy( SqmCopyContext.simpleContext() ) : criteria;
		queryStringCacheKey = sqm;
		// Cache immutable query plans by default
		setQueryPlanCacheable( !copyAst || session.isCriteriaPlanCacheEnabled() );

		domainParameterXref = DomainParameterXref.from( sqm );
		parameterMetadata = domainParameterXref.hasParameters()
				? new ParameterMetadataImpl( domainParameterXref.getQueryParameters() )
				: ParameterMetadataImpl.EMPTY;

		parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		// Parameters might be created through HibernateCriteriaBuilder.value which we need to bind here
		for ( SqmParameter<?> sqmParameter : domainParameterXref.getParameterResolutions().getSqmParameters() ) {
			if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper<?> wrapper ) {
				bindCriteriaParameter( wrapper );
			}
		}

		resultType = determineResultType( sqm, expectedResultType );

		final SqmQueryPart<R> queryPart = sqm.getQueryPart();
		// For criteria queries, we have to validate the fetch structure here
		queryPart.validateQueryStructureAndFetchOwners();
		SqmUtil.validateCriteriaQuery( queryPart );
		sqm.validateResultType( resultType );

		setComment( hql );

		tupleMetadata = buildTupleMetadata( sqm, expectedResultType );

	}

	<E> SqmSelectionQueryImpl(AbstractSqmSelectionQuery<?> original, KeyedPage<E> keyedPage) {
		super( original );

		final Page page = keyedPage.getPage();
		final List<Comparable<?>> key = keyedPage.getKey();
		final List<Order<? super E>> keyDefinition = keyedPage.getKeyDefinition();
		final List<Order<? super E>> appliedKeyDefinition =
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
		parameterMetadata = domainParameterXref.hasParameters()
				? new ParameterMetadataImpl( domainParameterXref.getQueryParameters() )
				: ParameterMetadataImpl.EMPTY;

		// Just use the original parameter bindings since this object is never going to be mutated
		parameterBindings = parameterMetadata.createBindings( original.getSession().getSessionFactory() );
		original.getQueryParameterBindings().visitBindings( this::setBindValues );

		// Parameters might be created through HibernateCriteriaBuilder.value which we need to bind here
		for ( SqmParameter<?> sqmParameter : domainParameterXref.getParameterResolutions().getSqmParameters() ) {
			if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper<?> parameterWrapper ) {
				bindCriteriaParameter( parameterWrapper );
			}
		}

		//noinspection unchecked
		expectedResultType = (Class<R>) KeyedResult.class;
		resultType = determineResultType( sqm, expectedResultType );
		tupleMetadata = null;

		setMaxResults( page.getMaxResults() + 1 );
		if ( key == null ) {
			setFirstResult( page.getFirstResult() );
		}
	}

	private <T> void setBindValues(QueryParameter<?> parameter, QueryParameterBinding<T> binding) {
		final QueryParameterBinding<T> parameterBinding = parameterBindings.getBinding( binding.getQueryParameter() );
		@SuppressWarnings("deprecation")
		final TemporalType explicitTemporalPrecision = binding.getExplicitTemporalPrecision();
		if ( explicitTemporalPrecision != null ) {
			if ( binding.isMultiValued() ) {
				parameterBinding.setBindValues( binding.getBindValues(), explicitTemporalPrecision,
						getSessionFactory().getTypeConfiguration() );
			}
			else {
				parameterBinding.setBindValue( binding.getBindValue(), explicitTemporalPrecision );
			}
		}
		else {
			//noinspection unchecked
			final BindableType<T> bindType = (BindableType<T>) binding.getBindType();
			if ( binding.isMultiValued() ) {
				parameterBinding.setBindValues( binding.getBindValues(), bindType );
			}
			else {
				parameterBinding.setBindValue( binding.getBindValue(), bindType );
			}
		}
		parameterBinding.setType( binding.getType() );
	}


	private static Class<?> determineResultType(SqmSelectStatement<?> sqm, Class<?> expectedResultType) {
		final List<SqmSelection<?>> selections = sqm.getQuerySpec().getSelectClause().getSelections();
		if ( selections.size() == 1 ) {
			if ( Object[].class.equals( expectedResultType ) ) {
				// for JPA compatibility
				return Object[].class;
			}
			else {
				final SqmSelection<?> selection = selections.get( 0 );
				if ( isSelectionAssignableToResultType( selection, expectedResultType ) ) {
					final JavaType<?> nodeJavaType = selection.getNodeJavaType();
					if ( nodeJavaType != null ) {
						return nodeJavaType.getJavaTypeClass();
					}
				}
				// let's assume there's some way to instantiate it
				return expectedResultType;
			}
		}
		else if ( expectedResultType != null ) {
			// assume we can repackage the tuple as the given type - worry
			// about how later (it's handled using a RowTransformer which is
			// set up in ConcreteSqmSelectQueryPlan.determineRowTransformer)
			return expectedResultType;
		}
		else {
			// for JPA compatibility
			return Object[].class;
		}
	}

	@Override
	public TupleMetadata getTupleMetadata() {
		return tupleMetadata;
	}

	@Override
	public SqmSelectStatement<R> getSqmStatement() {
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
	public String getQueryString() {
		return hql;
	}

	@Override
	public Object getQueryStringCacheKey() {
		return queryStringCacheKey;
	}

	@Override
	public <T> SqmSelectionQuery<T> setTupleTransformer(TupleTransformer<T> transformer) {
		getQueryOptions().setTupleTransformer( transformer );
		//noinspection unchecked
		return (SqmSelectionQuery<T>) this;
	}

	@Override
	public SqmSelectionQuery<R> setResultListTransformer(ResultListTransformer<R> transformer) {
		getQueryOptions().setResultListTransformer( transformer );
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// execution

	@Override
	protected void prepareForExecution() {
		// Reset the callback before every execution
		resetCallback();
	}

	@Override
	public long getResultCount() {
		final DelegatingDomainQueryExecutionContext context = new DelegatingDomainQueryExecutionContext(this) {
			@Override
			public QueryOptions getQueryOptions() {
				return QueryOptions.NONE;
			}
		};
		return buildConcreteQueryPlan( getSqmStatement().createCountQuery(), Long.class, null, getQueryOptions() )
				.executeQuery( context, SingleResultConsumer.instance() );
	}

	protected List<R> doList() {
		final SqmSelectStatement<?> statement = getSqmStatement();
		final boolean containsCollectionFetches =
				//TODO: why is this different from QuerySqmImpl.doList()?
				statement.containsCollectionFetches();
		final boolean hasLimit = hasLimit( statement, getQueryOptions() );
		final boolean needsDistinct = needsDistinct( containsCollectionFetches, hasLimit, statement );
		final List<R> list = resolveQueryPlan()
				.performList( executionContext( hasLimit, containsCollectionFetches ) );
		return needsDistinct ? handleDistinct( hasLimit, statement, list ) : list;
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

	// TODO: very similar to QuerySqmImpl.executionContextForDoList()
	private DomainQueryExecutionContext executionContext(boolean hasLimit, boolean containsCollectionFetches) {
		if ( hasLimit && containsCollectionFetches ) {
			errorOrLogForPaginationWithCollectionFetch();
			final MutableQueryOptions originalQueryOptions = getQueryOptions();
			final QueryOptions normalizedQueryOptions = omitSqlQueryOptions( originalQueryOptions, true, false );
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

	@Override
	protected ScrollableResultsImplementor<R> doScroll(ScrollMode scrollMode) {
		return resolveQueryPlan().performScroll( scrollMode, this );
	}

	@Override
	public <T> T executeQuery(ResultsConsumer<T, R> resultsConsumer) {
		return resolveQueryPlan().executeQuery( this, resultsConsumer );
	}

	@Override
	public Class<R> getExpectedResultType() {
		return expectedResultType;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query plan

	private SelectQueryPlan<R> resolveQueryPlan() {
		final QueryInterpretationCache.Key cacheKey = createInterpretationsKey( this );
		if ( cacheKey != null ) {
			return getSessionFactory().getQueryEngine().getInterpretationCache()
					.resolveSelectQueryPlan( cacheKey, this::buildSelectQueryPlan );
		}
		else {
			return buildSelectQueryPlan();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// InterpretationsKeySource

	@Override
	public Class<?> getResultType() {
		return resultType;
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return getSession().getLoadQueryInfluencers();
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
	// CommonQueryContract

	@Override
	protected boolean resolveJdbcParameterTypeIfNecessary() {
		// No need to resolve JDBC parameter types as we know them from the SQM model
		return false;
	}

	@Override
	public SqmSelectionQuery<R> setFlushMode(FlushModeType flushMode) {
		super.setFlushMode( flushMode );
		return this;
	}

	/**
	 * Specify the root LockModeType for the query
	 *
	 * @see #setHibernateLockMode
	 */
	@Override
	public SqmSelectionQuery<R> setLockMode(LockModeType lockMode) {
		super.setLockMode( lockMode );
		return this;
	}

	/**
	 * Specify the root {@link LockMode} for the query
	 */
	@Override
	public SqmSelectionQuery<R> setHibernateLockMode(LockMode lockMode) {
		super.setHibernateLockMode( lockMode );
		return this;
	}

	/**
	 * Specify a {@link LockMode} to apply to a specific alias defined in the query
	 */
	@Override
	public SqmSelectionQuery<R> setLockMode(String alias, LockMode lockMode) {
		super.setLockMode( alias, lockMode );
		return this;
	}

	/**
	 * Specifies whether follow-on locking should be applied?
	 */
	@Override
	public SqmSelectionQuery<R> setFollowOnLocking(boolean enable) {
		getLockOptions().setFollowOnLocking( enable );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setReadOnly(boolean readOnly) {
		super.setReadOnly( readOnly );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setCacheMode(CacheMode cacheMode) {
		super.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		super.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		super.setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setCacheable(boolean cacheable) {
		super.setCacheable( cacheable );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setCacheRegion(String regionName) {
		super.setCacheRegion( regionName );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setQueryPlanCacheable(boolean queryPlanCacheable) {
		super.setQueryPlanCacheable( queryPlanCacheable );
		return this;
	}

	@Override
	public boolean isQueryPlanCacheable() {
		return CRITERIA_HQL_STRING.equals( hql )
				// For criteria queries, query plan caching requires an explicit opt-in
				? getQueryOptions().getQueryPlanCachingEnabled() == Boolean.TRUE
				: super.isQueryPlanCacheable();
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariance

	@Override
	public SqmSelectionQuery<R> setHibernateFlushMode(FlushMode flushMode) {
		super.setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setQueryFlushMode(QueryFlushMode queryFlushMode) {
		super.setQueryFlushMode(queryFlushMode);
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}


	@Override
	public SqmSelectionQuery<R> setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(String name, P value, Class<P> javaType) {
		super.setParameter( name, value, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(String name, P value, BindableType<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQuery<R> setParameter(String name, Instant value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(int position, P value, Class<P> javaType) {
		super.setParameter( position, value, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(int position, P value, BindableType<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQuery<R> setParameter(int position, Instant value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(QueryParameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, Class<P> javaType) {
		super.setParameter( parameter, value, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, BindableType<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameter(Parameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQuery<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQuery<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQuery<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQuery<R> setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQuery<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQuery<R> setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(String name, P[] values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(String name, P[] values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameterList(int position, Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(int position, P[] values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(int position, P[] values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setProperties(@SuppressWarnings("rawtypes") Map map) {
		super.setProperties( map );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}
}
