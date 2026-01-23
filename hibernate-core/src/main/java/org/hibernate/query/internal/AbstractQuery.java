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
import jakarta.persistence.NoResultException;
import jakarta.persistence.Parameter;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Timeouts;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.Query;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.NONNULL;
import static java.util.Spliterators.spliteratorUnknownSize;

/// Base support for [QueryImplementor] implementors.
///
/// @author Steve Ebersole
public abstract class AbstractQuery<T> extends AbstractCommonQueryContract implements QueryImplementor<T> {

	public AbstractQuery(SharedSessionContractImplementor session) {
		super( session );
	}

	protected AbstractQuery(AbstractQuery<T> original) {
		super( original );
	}

	protected AbstractQuery(SharedSessionContractImplementor session, MutableQueryOptions queryOptions) {
		super( session, queryOptions );
	}

	protected <X> X unwrapDelegates(Class<X> type) {
		if ( type.isInstance( getParameterMetadata() ) ) {
			return type.cast( getParameterMetadata() );
		}

		if ( type.isInstance( getQueryParameterBindings() ) ) {
			return type.cast( getQueryParameterBindings() );
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

		return null;
	}

	@Override
	public final <X> X unwrap(Class<X> type) {
		if ( type.isInstance( this ) ) {
			return type.cast( this );
		}

		final X unwrappedDelegate = unwrapDelegates( type );
		if ( unwrappedDelegate != null ) {
			return unwrappedDelegate;
		}

		throw new PersistenceException( "Unrecognized unwrap type [" + type.getName() + "]" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Casts

	@Override
	public SelectionQueryImplementor<T> asSelectionQuery() {
		throw new IllegalSelectQueryException( "Not a select query", getQueryString() );
	}

	@Override
	public <X> SelectionQueryImplementor<X> asSelectionQuery(Class<X> type) {
		throw new IllegalSelectQueryException( "Not a select query", getQueryString() );
	}

	@Override
	public <X> SelectionQueryImplementor<X> asSelectionQuery(EntityGraph<X> entityGraph) {
		throw new IllegalSelectQueryException( "Not a select query", getQueryString() );
	}

	@Override
	public <X> SelectionQuery<X> asSelectionQuery(EntityGraph<X> entityGraph, GraphSemantic graphSemantic) {
		throw new IllegalSelectQueryException( "Not a select query", getQueryString() );
	}

	@Override
	public <X> SelectionQueryImplementor<X> ofType(Class<X> type) {
		try {
			return asSelectionQuery( type );
		}
		catch (IllegalSelectQueryException e) {
			final IllegalStateException wrapped = new IllegalStateException( e.getMessage() );
			wrapped.addSuppressed( wrapped );
			throw wrapped;
		}
	}
	@Override
	public <X> SelectionQueryImplementor<X> withEntityGraph(EntityGraph<X> entityGraph) {
		try {
			return asSelectionQuery( entityGraph );
		}
		catch (IllegalSelectQueryException e) {
			final IllegalStateException wrapped = new IllegalStateException( e.getMessage() );
			wrapped.addSuppressed( wrapped );
			throw wrapped;
		}
	}

	@Override
	public MutationQueryImplementor<T> asMutationQuery() {
		throw new IllegalMutationQueryException( "Not a mutation query", getQueryString() );
	}

	/**
	 * The Jakarta Persistence defined form of {@link #asMutationQuery()}
	 *
	 * @see jakarta.persistence.Query#asStatement
	 */
	@Override
	public MutationQueryImplementor<T> asStatement() {
		try {
			return asMutationQuery();
		}
		catch (IllegalMutationQueryException e) {
			final IllegalArgumentException wrapped = new IllegalArgumentException( e.getMessage() );
			wrapped.addSuppressed( e );
			throw wrapped;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	@Override
	public List<T> list() {
		return getResultList();
	}

	@Override @SuppressWarnings("deprecation")
	public List<T> getResultList() {
		//noinspection unchecked
		return (List<T>) super.getResultList();
	}

	@Override
	public ScrollableResultsImplementor<T> scroll() {
		return scroll( getSessionFactory().getJdbcServices().getDialect().defaultScrollMode() );
	}

	@Override
	public ScrollableResultsImplementor<T> scroll(ScrollMode scrollMode) {
		return withContext( () -> doScroll( scrollMode ) );
	}

	protected abstract ScrollableResultsImplementor<T> doScroll(ScrollMode scrollMode);

	@Override @SuppressWarnings("deprecation")
	public Stream<T> stream() {
		final ScrollableResults<T> results = scroll( ScrollMode.FORWARD_ONLY );
		final var spliterator = spliteratorUnknownSize( new ScrollableResultsIterator<>( results ), NONNULL );
		return StreamSupport.stream( spliterator, false ).onClose( results::close );
	}

	@Override
	public T uniqueResult() {
		return uniqueElement( list() );
	}

	@Override @SuppressWarnings("removal")
	public T getSingleResult() {
		try {
			final List<T> list = list();
			if ( list.isEmpty() ) {
				throw new NoResultException( "No result found for query [" + getQueryString() + "]" );
			}
			return uniqueElement( list );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e, getQueryOptions().getLockOptions() );
		}
	}

	protected static <T> T uniqueElement(List<T> list) throws NonUniqueResultException {
		final int size = list.size();
		return switch ( size ) {
			case 0 -> null;
			case 1 -> list.get( 0 );
			default -> throw new NonUniqueResultException( size );
		};
	}

	@Override
	public Optional<T> uniqueResultOptional() {
		return Optional.ofNullable( uniqueResult() );
	}

	@Override @SuppressWarnings("removal")
	public T getSingleResultOrNull() {
		try {
			return uniqueElement( list() );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e, queryOptions.getLockOptions() );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override @SuppressWarnings("removal")
	public MutableQueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public QueryImplementor<T> setHint(String hintName, Object value) {
		super.setHint( hintName, value );
		return this;
	}

	@Override
	public String getComment() {
		return getQueryOptions().getComment();
	}

	@Override
	public QueryImplementor<T> setComment(String comment) {
		getQueryOptions().setComment( comment );
		return this;
	}

	@Override
	public QueryImplementor<T> addQueryHint(String hint) {
		queryOptions.addDatabaseHint( hint );
		return this;
	}

	@Override
	public QueryFlushMode getQueryFlushMode() {
		return QueryFlushMode.fromHibernateMode( getQueryOptions().getFlushMode() );
	}

	@Override
	public QueryImplementor<T> setQueryFlushMode(QueryFlushMode queryFlushMode) {
		getQueryOptions().setFlushMode( FlushModeTypeHelper.getFlushMode(queryFlushMode) );
		return this;
	}

	@Override @SuppressWarnings("deprecation")
	public QueryImplementor<T> setFlushMode(FlushModeType flushMode) {
		session.checkOpen();
		queryOptions.setFlushMode( FlushMode.fromJpaFlushMode( flushMode ) );
		return this;
	}

	@Override
	public Integer getTimeout() {
		return Timeouts.getEffectiveTimeoutInSeconds( queryOptions.getTimeout() );
	}

	@Override
	public QueryImplementor<T> setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public QueryImplementor<T> setTimeout(Integer timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public QueryImplementor<T> setTimeout(Timeout timeout) {
		super.setTimeout( timeout );
		return this;
	}

	/// Easy hook for non-select queries to disallow select-only options
	protected void verifySelectionOption(String name) {
	}

	@Override
	public Integer getFetchSize() {
		return queryOptions.getFetchSize();
	}

	@Override
	public QueryImplementor<T> setFetchSize(int fetchSize) {
		verifySelectionOption( "Fetch size" );
		queryOptions.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public boolean isReadOnly() {
		return queryOptions.isReadOnly() == Boolean.TRUE;
	}

	@Override
	public Query<T> setReadOnly(boolean readOnly) {
		verifySelectionOption( "Fetch size" );
		queryOptions.setReadOnly( readOnly );
		return this;
	}

	@Override @SuppressWarnings("removal")
	public int getMaxResults() {
		session.checkOpen();
		return getQueryOptions().getLimit().getMaxRowsJpa();
	}

	@Override
	public QueryImplementor<T> setMaxResults(int maxResult) {
		verifySelectionOption( "Max results" );
		//noinspection unchecked
		return (QueryImplementor<T>) super.setMaxResults( maxResult );
	}

	@Override @SuppressWarnings("removal")
	public int getFirstResult() {
		session.checkOpen();
		return getQueryOptions().getLimit().getFirstRowJpa();
	}

	@Override
	public QueryImplementor<T> setFirstResult(int startPosition) {
		verifySelectionOption( "First result" );
		//noinspection unchecked
		return (QueryImplementor<T>) super.setFirstResult( startPosition );
	}

	@Override
	public boolean isCacheable() {
		return queryOptions.isResultCachingEnabled() == Boolean.TRUE;
	}

	@Override
	public Query<T> setCacheable(boolean cacheable) {
		verifySelectionOption( "Result caching" );
		queryOptions.setResultCachingEnabled( cacheable );
		return this;
	}

	@Override
	public CacheMode getCacheMode() {
		return queryOptions.getCacheMode();
	}

	@Override
	public Query<T> setCacheMode(CacheMode cacheMode) {
		verifySelectionOption( "Result caching" );
		queryOptions.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public String getCacheRegion() {
		return queryOptions.getResultCacheRegionName();
	}

	@Override
	public Query<T> setCacheRegion(String cacheRegion) {
		verifySelectionOption( "Result caching" );
		queryOptions.setResultCacheRegionName( cacheRegion );
		return this;
	}

	@Override @SuppressWarnings("removal")
	public CacheRetrieveMode getCacheRetrieveMode() {
		throw new IllegalStateException( "Cache retrieval not supported" );
	}

	@Override
	public QueryImplementor<T> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		verifySelectionOption( "Result caching" );
		//noinspection unchecked
		return (QueryImplementor<T>) super.setCacheRetrieveMode( cacheRetrieveMode );
	}

	@Override @SuppressWarnings("removal")
	public CacheStoreMode getCacheStoreMode() {
		throw new IllegalStateException( "Cache storage not supported" );
	}

	@Override
	public QueryImplementor<T> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		verifySelectionOption( "Result caching" );
		//noinspection unchecked
		return (QueryImplementor<T>) super.setCacheStoreMode( cacheStoreMode );
	}

	@Override @SuppressWarnings("removal")
	public LockModeType getLockMode() {
		return queryOptions.getLockOptions().getLockMode().toJpaLockMode();
	}

	@Override
	public QueryImplementor<T> setLockMode(LockModeType lockMode) {
		verifySelectionOption( "Locking" );
		return (SelectionQueryImplementor<T>) super.setLockMode( lockMode );
	}

	@Override
	public LockMode getHibernateLockMode() {
		return queryOptions.getLockOptions().getLockMode();
	}

	@Override
	public QueryImplementor<T> setHibernateLockMode(LockMode lockMode) {
		verifySelectionOption( "Locking" );
		queryOptions.getLockOptions().setLockMode( lockMode );
		return this;
	}

	@Override
	public Timeout getLockTimeout() {
		return queryOptions.getLockOptions().getTimeout();
	}

	@Override
	public Query<T> setLockTimeout(Timeout lockTimeout) {
		verifySelectionOption( "Locking" );
		queryOptions.getLockOptions().setTimeout( lockTimeout );
		return this;
	}

	@Override
	public QueryImplementor<T> setLockScope(PessimisticLockScope lockScope) {
		verifySelectionOption( "Locking" );
		queryOptions.getLockOptions().setLockScope( lockScope );
		return this;
	}

	@Override
	public QueryImplementor<T> setFollowOnLockingStrategy(Locking.FollowOn strategy) {
		verifySelectionOption( "Locking" );
		queryOptions.getLockOptions().setFollowOnStrategy( strategy );
		return this;
	}

	@Override
	public QueryImplementor<T> setFollowOnStrategy(Locking.FollowOn strategy) {
		return setFollowOnLockingStrategy( strategy );
	}

	@Override
	public <X> QueryImplementor<X> setTupleTransformer(TupleTransformer<X> transformer) {
		verifySelectionOption( "Result transformation" );
		getQueryOptions().setTupleTransformer( transformer );
		return (QueryImplementor<X>) this;
	}

	@Override
	public QueryImplementor<T> setResultListTransformer(ResultListTransformer<T> transformer) {
		verifySelectionOption( "Result transformation" );
		getQueryOptions().setResultListTransformer( transformer );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter binding

	@Override
	public QueryImplementor<T> setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameter(String name, P value, Class<P> javaType) {
		super.setParameter( name, value, javaType );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameter(String name, P value, Type<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public QueryImplementor<T> setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameter(int position, P value, Class<P> javaType) {
		super.setParameter( position, value, javaType );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameter(int position, P value, Type<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameter(QueryParameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameter(QueryParameter<P> parameter, P value, Class<P> javaType) {
		super.setParameter( parameter, value, javaType );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameter(QueryParameter<P> parameter, P value, Type<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameter(Parameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public QueryImplementor<T> setProperties(@SuppressWarnings("rawtypes") Map map) {
		super.setProperties( map );
		return this;
	}

	@Override
	public QueryImplementor<T> setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converterClass) {
		super.setConvertedParameter( name, value, converterClass );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converterClass) {
		super.setConvertedParameter( position,value, converterClass );
		return this;
	}

	@Override
	public QueryImplementor<T> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	public <P> QueryImplementor<T> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameterList(String name, Collection<? extends P> values, Type<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public QueryImplementor<T> setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameterList(String name, P[] values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	public <P> QueryImplementor<T> setParameterList(String name, P[] values, Type<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public QueryImplementor<T> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameterList(int position, Collection<? extends P> values, Type<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public QueryImplementor<T> setParameterList(int position, Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameterList(int position, P[] values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	public <P> QueryImplementor<T> setParameterList(int position, P[] values, Type<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}


	@Override
	public <P> QueryImplementor<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameterList(QueryParameter<P> parameter, P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> QueryImplementor<T> setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override @Deprecated(since = "7") @SuppressWarnings("deprecation")
	public QueryImplementor<T> setParameter(String name, Instant value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7")
	public QueryImplementor<T> setParameter(int position, Instant value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7")
	public QueryImplementor<T> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7")
	public QueryImplementor<T> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7")
	public QueryImplementor<T> setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7")
	public QueryImplementor<T> setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7")
	public QueryImplementor<T> setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7")
	public QueryImplementor<T> setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Utilities

	protected int getIntegerLiteral(JpaExpression<Number> expression, int defaultValue) {
		if ( expression == null ) {
			return defaultValue;
		}
		else if ( expression instanceof SqmLiteral<Number> numericLiteral ) {
			return numericLiteral.getLiteralValue().intValue();
		}
		else if ( expression instanceof SqmParameter<Number> parameterExpression ) {
			final Number number = getParameterValue( parameterExpression );
			return number == null ? defaultValue : number.intValue();
		}
		else {
			throw new IllegalArgumentException( "Not an integer literal: " + expression );
		}
	}

	protected int getMaxRows(SqmSelectStatement<?> selectStatement, int size) {
		final var fetchExpression = selectStatement.getFetch();
		if ( fetchExpression != null ) {
			final var fetchValue = fetchValue( fetchExpression );
			if ( fetchValue != null ) {
				// Note that we can never have ties because this is only used when we deduplicate results
				return switch ( selectStatement.getFetchClauseType() ) {
					case ROWS_ONLY, ROWS_WITH_TIES -> fetchValue.intValue();
					case PERCENT_ONLY, PERCENT_WITH_TIES ->
							(int) Math.ceil( (((double) size) * fetchValue.doubleValue()) / 100d );
				};
			}
		}
		return -1;
	}

	private Number fetchValue(JpaExpression<Number> expression) {
		if ( expression instanceof SqmLiteral<Number> numericLiteral ) {
			return numericLiteral.getLiteralValue();
		}
		else if ( expression instanceof SqmParameter<Number> numericParameter ) {
			return getParameterValue( numericParameter );
		}
		else {
			throw new IllegalArgumentException( "Can't get max rows value from: " + expression );
		}
	}
}
