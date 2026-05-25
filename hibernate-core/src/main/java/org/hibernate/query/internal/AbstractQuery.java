/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Parameter;
import jakarta.persistence.PersistenceException;
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
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import jakarta.persistence.QueryFlushMode;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.MutationOrSelectionQuery;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.QueryImplementor;
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
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterators.spliteratorUnknownSize;
import static org.hibernate.jpa.internal.util.FlushModeTypeHelper.queryFlushModeFromFlushMode;

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
	@Nonnull
	public final <X> X unwrap(Class<X> type) {
		if ( type.isInstance( this ) ) {
			return type.cast( this );
		}

		if ( type == MutationOrSelectionQuery.class ) {
			return type.cast( MutationOrSelectionQueryImpl.from( this ) );
		}

		final X unwrappedDelegate = unwrapDelegates( type );
		if ( unwrappedDelegate != null ) {
			return unwrappedDelegate;
		}

		throw new PersistenceException( "Unrecognized unwrap type [" + type.getName() + "]" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	protected <R> R executeQuery(Supplier<? extends R> supplier) {
		final var fetchProfiles = beforeQueryHandlingFetchProfiles();
		boolean success = false;
		try {
			final var result = supplier.get();
			success = true;
			return result;
		}
		catch (IllegalQueryOperationException e) {
			throw new IllegalStateException( e );
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he, getQueryOptions().getLockOptions() );
		}
		finally {
			afterQueryHandlingFetchProfiles( success, fetchProfiles );
		}
	}

	protected ScrollableResults<T> executeQuery(
			ScrollMode scrollMode,
			Function<ScrollMode,ScrollableResults<T>> supplier) {
		final var fetchProfiles = beforeQueryHandlingFetchProfiles();
		try {
			return supplier.apply( scrollMode );
		}
		finally {
			afterQueryHandlingFetchProfiles( fetchProfiles );
		}
	}

	protected int executeMutation(IntSupplier mutation) {
		session.checkTransactionNeededForUpdateOperation( "No active transaction for update or delete query" );
		final var fetchProfiles = beforeQueryHandlingFetchProfiles();
		boolean success = false;
		try {
			final int result = mutation.getAsInt();
			success = true;
			return result;
		}
		catch (IllegalQueryOperationException e) {
			throw new IllegalStateException( e );
		}
		catch (HibernateException e) {
			throw getExceptionConverter().convert( e );
		}
		finally {
			afterQueryHandlingFetchProfiles( success, fetchProfiles );
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
	@SuppressWarnings("removal")
	@Nonnull
	public List<T> list() {
		return getResultList();
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public abstract List<T> getResultList();

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public ScrollableResults<T> scroll() {
		return scroll( getSessionFactory().getJdbcServices().getDialect().defaultScrollMode() );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public abstract ScrollableResults<T> scroll(@Nonnull ScrollMode scrollMode);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public Stream<T> stream() {
		return getResultStream();
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public Stream<T> getResultStream() {
		final var results = scroll( ScrollMode.FORWARD_ONLY );
		final var spliterator = spliteratorUnknownSize( new ScrollableResultsIterator<>( results ), NONNULL );
		return StreamSupport.stream( spliterator, false ).onClose( results::close );
	}

	@Override
	@SuppressWarnings("removal")
	@Nullable
	public T uniqueResult() {
		// note: throws different exception type
		//       to getSingleResultOrNull()
		return uniqueElement( getResultList() );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public Optional<T> uniqueResultOptional() {
		return ofNullable( uniqueResult() );
	}

	@Override @SuppressWarnings("removal")
	public T getSingleResult() {
		try {
			final var list = getResultList();
			if ( list.isEmpty() ) {
				throw new NoResultException( "No result found for query [" + getQueryString() + "]" );
			}
			return uniqueElement( list );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e, getQueryOptions().getLockOptions() );
		}
	}

	@Override @SuppressWarnings("removal")
	@Nullable
	public T getSingleResultOrNull() {
		try {
			return uniqueElement( getResultList() );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e, queryOptions.getLockOptions() );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override @SuppressWarnings("removal")
	@Nonnull
	public MutableQueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setHint(@Nonnull String hintName, @Nullable Object value) {
		super.setHint( hintName, value );
		return this;
	}

	@Override
	@Nullable
	public String getComment() {
		return getQueryOptions().getComment();
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setComment(@Nullable String comment) {
		getQueryOptions().setComment( comment );
		return this;
	}

	@Override
	@Nonnull
	public QueryImplementor<T> addQueryHint(@Nonnull String hint) {
		queryOptions.addDatabaseHint( hint );
		return this;
	}

	@Override
	@Nonnull
	public QueryFlushMode getQueryFlushMode() {
		return queryFlushModeFromFlushMode( getQueryOptions().getFlushMode() );
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode) {
		getQueryOptions().setFlushMode( interpretQueryFlushMode(queryFlushMode) );
		return this;
	}

	protected FlushMode interpretQueryFlushMode(QueryFlushMode queryFlushMode) {
		return FlushModeTypeHelper.getFlushMode(queryFlushMode);
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setFlushMode(@Nonnull FlushModeType flushMode) {
		super.setFlushMode( flushMode );
		return this;
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setTimeout(@Nullable Integer timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setTimeout(@Nullable Timeout timeout) {
		super.setTimeout( timeout );
		return this;
	}

	/// Easy hook for non-select queries to disallow select-only options
	protected void verifySelectionOption(String name) {
	}

	@Override
	@SuppressWarnings("removal")
	@Nullable
	public Integer getFetchSize() {
		return queryOptions.getFetchSize();
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public QueryImplementor<T> setFetchSize(int fetchSize) {
		verifySelectionOption( "Fetch size" );
		queryOptions.setFetchSize( fetchSize );
		return this;
	}

	@Override
	@SuppressWarnings("removal")
	public boolean isReadOnly() {
		return queryOptions.isReadOnly() == Boolean.TRUE;
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public Query<T> setReadOnly(boolean readOnly) {
		verifySelectionOption( "Fetch size" );
		queryOptions.setReadOnly( readOnly );
		return this;
	}

	@Override
	public int getMaxResults() {
		session.checkOpen();
		return getQueryOptions().getLimit().getMaxRowsJpa();
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setMaxResults(int maxResult) {
		verifySelectionOption( "Max results" );
		super.setMaxResults( maxResult );
		return this;
	}

	@Override
	public int getFirstResult() {
		session.checkOpen();
		return getQueryOptions().getLimit().getFirstRowJpa();
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setFirstResult(int startPosition) {
		verifySelectionOption( "First result" );
		super.setFirstResult( startPosition );
		return this;
	}

	@Override @SuppressWarnings("removal")
	public boolean isCacheable() {
		return queryOptions.isResultCachingEnabled() == Boolean.TRUE;
	}

	@Override @SuppressWarnings("removal")
	@Nonnull
	public Query<T> setCacheable(boolean cacheable) {
		verifySelectionOption( "Result caching" );
		queryOptions.setResultCachingEnabled( cacheable );
		return this;
	}

	@Override @SuppressWarnings("removal")
	@Nonnull
	public CacheMode getCacheMode() {
		return queryOptions.getCacheMode();
	}

	@Override @SuppressWarnings("removal")
	@Nonnull
	public Query<T> setCacheMode(@Nonnull CacheMode cacheMode) {
		verifySelectionOption( "Result caching" );
		queryOptions.setCacheMode( cacheMode );
		return this;
	}

	@Override
	@SuppressWarnings("removal")
	@Nullable
	public String getCacheRegion() {
		return queryOptions.getResultCacheRegionName();
	}

	@Override
	@SuppressWarnings("removal")
	public Query<T> setCacheRegion(@Nullable String cacheRegion) {
		verifySelectionOption( "Result caching" );
		queryOptions.setResultCacheRegionName( cacheRegion );
		return this;
	}

	@Override
	@Nonnull
	public CacheRetrieveMode getCacheRetrieveMode() {
		throw new IllegalStateException( "Cache retrieval not supported" );
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode) {
		verifySelectionOption( "Result caching" );
		super.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	@Nonnull
	public CacheStoreMode getCacheStoreMode() {
		throw new IllegalStateException( "Cache storage not supported" );
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode) {
		verifySelectionOption( "Result caching" );
		super.setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override @SuppressWarnings("removal")
	@Nullable
	public LockModeType getLockMode() {
		return queryOptions.getLockOptions().getLockMode().toJpaLockMode();
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setLockMode(@Nonnull LockModeType lockMode) {
		verifySelectionOption( "Locking" );
		super.setLockMode( lockMode );
		return this;
	}

	@Override
	@Nonnull
	public LockMode getHibernateLockMode() {
		//noinspection removal
		return queryOptions.getLockOptions().getLockMode();
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setHibernateLockMode(@Nonnull LockMode lockMode) {
		verifySelectionOption( "Locking" );
		//noinspection removal
		queryOptions.getLockOptions().setLockMode( lockMode );
		return this;
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setFollowOnStrategy(@Nonnull Locking.FollowOn strategy) {
		verifySelectionOption( "Locking" );
		//noinspection removal
		queryOptions.getLockOptions().setFollowOnStrategy( strategy );
		return this;
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public <X> QueryImplementor<X> setTupleTransformer(@Nonnull TupleTransformer<X> transformer) {
		verifySelectionOption( "Result transformation" );
		getQueryOptions().setTupleTransformer( transformer );
		//noinspection unchecked
		return (QueryImplementor<X>) this;
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public QueryImplementor<T> setResultListTransformer(@Nonnull ResultListTransformer<T> transformer) {
		verifySelectionOption( "Result transformation" );
		getQueryOptions().setResultListTransformer( transformer );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter binding
	@Override
	@Nonnull
	public QueryImplementor<T> setParameter(@Nonnull String name, @Nullable Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<P> javaType) {
		super.setParameter( name, value, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameter(@Nonnull String name, @Nullable P value, @Nonnull Type<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setParameter(int position, @Nullable Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameter(int position, @Nullable P value, @Nonnull Class<P> javaType) {
		super.setParameter( position, value, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameter(int position, @Nullable P value, @Nonnull Type<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value, @Nonnull Class<P> javaType) {
		super.setParameter( parameter, value, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value, @Nonnull Type<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameter(@Nonnull Parameter<P> parameter, @Nullable P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setProperties(@Nonnull @SuppressWarnings("rawtypes") Map map) {
		super.setProperties( map );
		return this;
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setProperties(@Nonnull Object bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setConvertedParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converterClass) {
		super.setConvertedParameter( name, value, converterClass );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setConvertedParameter(int position, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converterClass) {
		super.setConvertedParameter( position,value, converterClass );
		return this;
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setParameterList(@Nonnull String name, @Nonnull @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Nonnull
	public <P> QueryImplementor<T> setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setParameterList(@Nonnull String name, @Nonnull Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Nonnull
	public <P> QueryImplementor<T> setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Type<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setParameterList(int position, @Nonnull @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setParameterList(int position, @Nonnull Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameterList(int position, @Nonnull P[] values, @Nonnull Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Nonnull
	public <P> QueryImplementor<T> setParameterList(int position, @Nonnull P[] values, @Nonnull Type<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> QueryImplementor<T> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Type<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override @Deprecated(since = "7") @SuppressWarnings("deprecation")
	@Nonnull
	public QueryImplementor<T> setParameter(@Nonnull String name, @Nullable Instant value, @Nonnull TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7") @SuppressWarnings("deprecation")
	@Nonnull
	public QueryImplementor<T> setParameter(int position, @Nullable Instant value, @Nonnull TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7") @SuppressWarnings("deprecation")
	@Nonnull
	public QueryImplementor<T> setParameter(@Nonnull Parameter<Calendar> param, @Nullable Calendar value, @Nonnull TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7") @SuppressWarnings("deprecation")
	@Nonnull
	public QueryImplementor<T> setParameter(@Nonnull Parameter<Date> param, @Nullable Date value, @Nonnull TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7") @SuppressWarnings("deprecation")
	@Nonnull
	public QueryImplementor<T> setParameter(@Nonnull String name, @Nullable Calendar value, @Nonnull TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7") @SuppressWarnings("deprecation")
	@Nonnull
	public QueryImplementor<T> setParameter(@Nonnull String name, @Nullable Date value, @Nonnull TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7") @SuppressWarnings("deprecation")
	@Nonnull
	public QueryImplementor<T> setParameter(int position, @Nullable Calendar value, @Nonnull TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated(since = "7") @SuppressWarnings("deprecation")
	@Nonnull
	public QueryImplementor<T> setParameter(int position, @Nullable Date value, @Nonnull TemporalType temporalType) {
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
