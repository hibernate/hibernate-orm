/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.query.BindableType;
import org.hibernate.query.Page;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.internal.NamedCriteriaQueryMementoImpl;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.internal.DelegatingDomainQueryExecutionContext;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.SqmSelectionQuery;
import org.hibernate.query.sqm.internal.SqmInterpretationsKey.InterpretationsKeySource;
import org.hibernate.query.sqm.spi.SqmSelectionQueryImplementor;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.sql.results.spi.ResultsConsumer;
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
import static org.hibernate.query.sqm.internal.SqmInterpretationsKey.createInterpretationsKey;
import static org.hibernate.query.sqm.internal.SqmUtil.isSelectionAssignableToResultType;

/**
 * @author Steve Ebersole
 */
public class SqmSelectionQueryImpl<R> extends AbstractSqmSelectionQuery<R>
		implements SqmSelectionQueryImplementor<R>, InterpretationsKeySource {
	private final String hql;
	private SqmSelectStatement<R> sqm;

	private final ParameterMetadataImplementor parameterMetadata;
	private final DomainParameterXref domainParameterXref;
	private final QueryParameterBindingsImpl parameterBindings;

	private final Class<R> expectedResultType;
	private final Class<?> resultType;
	private final TupleMetadata tupleMetadata;

	public SqmSelectionQueryImpl(
			String hql,
			HqlInterpretation<R> hqlInterpretation,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		super( session );
		this.hql = hql;

		this.sqm = (SqmSelectStatement<R>) hqlInterpretation.getSqmStatement();

		this.parameterMetadata = hqlInterpretation.getParameterMetadata();
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();
		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, session.getFactory() );

		this.expectedResultType = expectedResultType;
		visitQueryReturnType( sqm.getQueryPart(), expectedResultType, getSessionFactory() );
		this.resultType = determineResultType( sqm );
		this.tupleMetadata = buildTupleMetadata( sqm, expectedResultType );

		setComment( hql );
	}

	private Class<?> determineResultType(SqmSelectStatement<?> sqm) {
		final List<SqmSelection<?>> selections = sqm.getQuerySpec().getSelectClause().getSelections();
		if ( selections.size() == 1 ) {
			if ( Object[].class.equals( expectedResultType ) ) {
				// for JPA compatibility
				return Object[].class;
			}
			else {
				final SqmSelection<?> selection = selections.get(0);
				if ( isSelectionAssignableToResultType( selection, expectedResultType ) ) {
					return selection.getNodeJavaType().getJavaTypeClass();
				}
				else {
					// let's assume there's some
					// way to instantiate it
					return expectedResultType;
				}
			}
		}
		else if ( expectedResultType != null ) {
			// assume we can repackage the tuple as
			// the given type (worry about how later)
			return expectedResultType;
		}
		else {
			// for JPA compatibility
			return Object[].class;
		}
	}

	public SqmSelectionQueryImpl(
			NamedHqlQueryMementoImpl memento,
			Class<R> resultType,
			SharedSessionContractImplementor session) {
		super( session );
		this.hql = memento.getHqlString();
		this.expectedResultType = resultType;
		this.resultType = resultType;

		final QueryEngine queryEngine = session.getFactory().getQueryEngine();
		final QueryInterpretationCache interpretationCache = queryEngine.getInterpretationCache();
		final HqlInterpretation<R> hqlInterpretation =
				interpretationCache.resolveHqlInterpretation( hql, resultType, queryEngine.getHqlTranslator() );

		SqmUtil.verifyIsSelectStatement( hqlInterpretation.getSqmStatement(), hql );
		this.sqm = (SqmSelectStatement<R>) hqlInterpretation.getSqmStatement();

		this.parameterMetadata = hqlInterpretation.getParameterMetadata();
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();

		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, session.getFactory() );

		setComment( hql );
		applyOptions( memento );

		this.tupleMetadata = buildTupleMetadata( sqm, resultType );
	}

	public SqmSelectionQueryImpl(
			NamedCriteriaQueryMementoImpl memento,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		//noinspection unchecked
		this( (SqmSelectStatement<R>) memento.getSqmStatement(), expectedResultType, session );
		applyOptions( memento );
	}

	public SqmSelectionQueryImpl(
			SqmSelectStatement<R> criteria,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		super( session );
		this.hql = CRITERIA_HQL_STRING;
		if ( session.isCriteriaCopyTreeEnabled() ) {
			this.sqm = criteria.copy( SqmCopyContext.simpleContext() );
		}
		else {
			this.sqm = criteria;
			// Cache immutable query plans by default
			setQueryPlanCacheable( true );
		}

		this.domainParameterXref = DomainParameterXref.from( sqm );
		this.parameterMetadata = domainParameterXref.hasParameters()
				? new ParameterMetadataImpl( domainParameterXref.getQueryParameters() )
				: ParameterMetadataImpl.EMPTY;

		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, session.getFactory() );

		// Parameters might be created through HibernateCriteriaBuilder.value which we need to bind here
		for ( SqmParameter<?> sqmParameter : domainParameterXref.getParameterResolutions().getSqmParameters() ) {
			if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper<?> ) {
				bindCriteriaParameter( (SqmJpaCriteriaParameterWrapper<?>) sqmParameter );
			}
		}

		this.expectedResultType = expectedResultType;
		this.resultType = determineResultType( sqm );
		visitQueryReturnType( sqm.getQueryPart(), expectedResultType, getSessionFactory() );

		setComment( hql );

		this.tupleMetadata = buildTupleMetadata( sqm, expectedResultType );
	}

	private <T> void bindCriteriaParameter(SqmJpaCriteriaParameterWrapper<T> sqmParameter) {
		final JpaCriteriaParameter<T> jpaCriteriaParameter = sqmParameter.getJpaCriteriaParameter();
		final T value = jpaCriteriaParameter.getValue();
		// We don't set a null value, unless the type is also null which
		// is the case when using HibernateCriteriaBuilder.value
		if ( value != null || jpaCriteriaParameter.getNodeType() == null ) {
			// Use the anticipated type for binding the value if possible
			getQueryParameterBindings()
					.getBinding( jpaCriteriaParameter )
					.setBindValue( value, jpaCriteriaParameter.getAnticipatedType() );
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// convenience methods

//	@Override
//	public SelectionQuery<R> setPage(int pageSize, int pageNumber) {
//		setFirstResult( pageNumber * pageSize );
//		setMaxResults( pageSize );
//		return this;
//	}

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
	public Supplier<Boolean> hasMultiValuedParameterBindingsChecker() {
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
	 *
	 * @deprecated use {{@link #setLockMode(String, LockMode)}}
	 */
	@Override @Deprecated
	public SqmSelectionQuery<R> setAliasSpecificLockMode(String alias, LockMode lockMode) {
		super.setAliasSpecificLockMode( alias, lockMode );
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

	@Override
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

	@Override
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

	@Override
	public SqmSelectionQuery<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQuery<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
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
