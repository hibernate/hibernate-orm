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
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SharedSessionContract;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.MutationOrSelectionQuery;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryOptions;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Compatibility adapter for APIs which historically returned a query that could
 * be treated as either a selection or mutation query.
 *
 * @author Gavin King
 */
public final class MutationOrSelectionQueryImpl implements MutationOrSelectionQuery {
	private final QueryImplementor<?> delegate;

	public static MutationOrSelectionQuery from(QueryImplementor<?> delegate) {
		return delegate instanceof MutationOrSelectionQuery mutationOrSelectionQuery
				? mutationOrSelectionQuery
				: new MutationOrSelectionQueryImpl( delegate );
	}

	private MutationOrSelectionQueryImpl(QueryImplementor<?> delegate) {
		this.delegate = delegate;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Casts


	@Override
	public boolean isMutationQuery() {
		return delegate instanceof MutationQuery;
	}

	@Override
	public boolean isSelectionQuery() {
		return delegate instanceof SelectionQuery;
	}

	@Override
	@Nonnull
	public SelectionQuery<?> asSelectionQuery() {
		return delegate.asSelectionQuery();
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> asSelectionQuery(Class<R> type) {
		return delegate.asSelectionQuery( type );
	}

	@Override
	@Nonnull
	public <X> SelectionQuery<X> asSelectionQuery(EntityGraph<X> entityGraph) {
		return delegate.asSelectionQuery( entityGraph );
	}

	@Override
	@Nonnull
	public <X> SelectionQuery<X> asSelectionQuery(EntityGraph<X> entityGraph, GraphSemantic graphSemantic) {
		return delegate.asSelectionQuery( entityGraph, graphSemantic );
	}

	@Override
	@Nonnull
	public MutationQuery asMutationQuery() {
		return delegate.asMutationQuery();
	}

	@Override
	@Nonnull
	public MutationQuery asStatement() {
		try {
			return delegate.asMutationQuery();
		}
		catch (IllegalMutationQueryException e) {
			throw new IllegalStateException( e.getMessage(), e );
		}
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> ofType(@Nonnull Class<R> type) {
		try {
			return delegate.asSelectionQuery( type );
		}
		catch (IllegalSelectQueryException e) {
			throw new IllegalStateException( e.getMessage(), e );
		}
		catch (QueryTypeMismatchException e) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> withEntityGraph(@Nonnull EntityGraph<R> entityGraph) {
		try {
			return delegate.asSelectionQuery( entityGraph );
		}
		catch (IllegalSelectQueryException e) {
			throw new IllegalStateException( e.getMessage(), e );
		}
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> withResultSetMapping(@Nonnull ResultSetMapping<R> mapping) {
		try {
			return delegate.withResultSetMapping( mapping );
		}
		catch (IllegalSelectQueryException e) {
			throw new IllegalStateException( e.getMessage(), e );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query identity

	@Override
	@Nullable
	public String getQueryString() {
		return delegate.getQueryString();
	}

	@Override
	@Nonnull
	public SharedSessionContract getSession() {
		return delegate.getSession();
	}

	@Override @Deprecated @SuppressWarnings("removal")
	@Nonnull
	public QueryOptions getQueryOptions() {
		return delegate.getQueryOptions();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	@Override
	@SuppressWarnings("removal")
	public int executeUpdate() {
		return delegate.executeUpdate();
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "removal"})
	@Nonnull
	public List list() {
		return delegate.list();
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "removal"})
	@Nonnull
	public List getResultList() {
		return delegate.getResultList();
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nullable
	public Object uniqueResult() {
		return delegate.uniqueResult();
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	public Object getSingleResult() {
		return delegate.getSingleResult();
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nullable
	public Object getSingleResultOrNull() {
		return delegate.getSingleResultOrNull();
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "removal"})
	@Nonnull
	public Optional uniqueResultOptional() {
		return delegate.uniqueResultOptional();
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "removal"})
	@Nonnull
	public Stream getResultStream() {
		return delegate.getResultStream();
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "removal"})
	@Nonnull
	public Stream stream() {
		return delegate.stream();
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "removal"})
	@Nonnull
	public ScrollableResults scroll() {
		return delegate.scroll();
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "removal"})
	@Nonnull
	public ScrollableResults scroll(@Nonnull ScrollMode scrollMode) {
		return delegate.scroll( scrollMode );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override
	public FlushMode getEffectiveFlushMode() {
		return delegate.getEffectiveFlushMode();
	}

	@Override
	@Nonnull
	public QueryFlushMode getQueryFlushMode() {
		return delegate.getQueryFlushMode();
	}

	@Override @Deprecated
	@SuppressWarnings("deprecation")
	@Nonnull
	public FlushModeType getFlushMode() {
		return delegate.getFlushMode();
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode) {
		delegate.setQueryFlushMode( queryFlushMode );
		return this;
	}

	@Override
	@SuppressWarnings("deprecation")
	@Nonnull
	public MutationOrSelectionQuery setFlushMode(@Nonnull FlushModeType flushMode) {
		delegate.setFlushMode( flushMode );
		return this;
	}

	@Override
	@Nullable
	public String getComment() {
		return delegate.getComment();
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setComment(@Nullable String comment) {
		delegate.setComment( comment );
		return this;
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery addQueryHint(@Nonnull String hint) {
		delegate.addQueryHint( hint );
		return this;
	}

	@Override
	@Nullable
	public Integer getTimeout() {
		return delegate.getTimeout();
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setTimeout(int timeout) {
		delegate.setTimeout( timeout );
		return this;
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setTimeout(@Nullable Integer timeout) {
		delegate.setTimeout( timeout );
		return this;
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setTimeout(@Nullable Timeout timeout) {
		delegate.setTimeout( timeout );
		return this;
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setHint(@Nonnull String hintName, @Nullable Object value) {
		delegate.setHint( hintName, value );
		return this;
	}

	@Override
	@Nonnull
	public Map<String, Object> getHints() {
		return delegate.getHints();
	}

	@Override
	@SuppressWarnings("removal")
	@Nullable
	public Integer getFetchSize() {
		return delegate.getFetchSize();
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery setFetchSize(int fetchSize) {
		//noinspection removal
		delegate.setFetchSize( fetchSize );
		return this;
	}

	@Override
	@SuppressWarnings("removal")
	public boolean isReadOnly() {
		return delegate.isReadOnly();
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery setReadOnly(boolean readOnly) {
		delegate.setReadOnly( readOnly );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("removal")
	public int getMaxResults() {
		return delegate.getMaxResults();
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery setMaxResults(int maxResults) {
		delegate.setMaxResults( maxResults );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	public int getFirstResult() {
		return delegate.getFirstResult();
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery setFirstResult(int startPosition) {
		delegate.setFirstResult( startPosition );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public CacheMode getCacheMode() {
		return delegate.getCacheMode();
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery setCacheMode(@Nonnull CacheMode cacheMode) {
		delegate.setCacheMode( cacheMode );
		return this;
	}

	@Override
	@Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public CacheStoreMode getCacheStoreMode() {
		return delegate.getCacheStoreMode();
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode) {
		delegate.setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override
	@Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public CacheRetrieveMode getCacheRetrieveMode() {
		return delegate.getCacheRetrieveMode();
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode) {
		delegate.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	public boolean isQueryPlanCacheable() {
		return delegate.isQueryPlanCacheable();
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setQueryPlanCacheable(boolean queryPlanCacheable) {
		delegate.setQueryPlanCacheable( queryPlanCacheable );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	public boolean isCacheable() {
		return delegate.isCacheable();
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery setCacheable(boolean cacheable) {
		//noinspection removal
		delegate.setCacheable( cacheable );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nullable
	public String getCacheRegion() {
		return delegate.getCacheRegion();
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	public MutationOrSelectionQuery setCacheRegion(@Nullable String cacheRegion) {
		//noinspection removal
		delegate.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	@Deprecated @SuppressWarnings("removal")
	@Nullable
	public LockModeType getLockMode() {
		return delegate.getLockMode();
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery setLockMode(@Nonnull LockModeType lockMode) {
		delegate.setLockMode( lockMode );
		return this;
	}

	@Override @Deprecated
	@Nonnull
	public LockMode getHibernateLockMode() {
		return delegate.getHibernateLockMode();
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery setHibernateLockMode(@Nonnull LockMode lockMode) {
		delegate.setHibernateLockMode( lockMode );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery setFollowOnStrategy(@Nonnull Locking.FollowOn followOnStrategy) {
		delegate.setFollowOnStrategy( followOnStrategy );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public <X> SelectionQuery<X> setTupleTransformer(@Nonnull TupleTransformer<X> transformer) {
		return (SelectionQuery<X>) delegate.setTupleTransformer( transformer );
	}

	@Override
	@Deprecated
	@SuppressWarnings({"unchecked", "rawtypes", "removal"})
	@Nonnull
	public MutationOrSelectionQuery setResultListTransformer(@Nonnull ResultListTransformer<Object> transformer) {
		final var resultListTransformer = (ResultListTransformer) transformer;
		delegate.setResultListTransformer( resultListTransformer );
		return this;
	}

	@Override
	@Deprecated @SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery setEntityGraph(@Nonnull EntityGraph<? super Object> graph, @Nonnull GraphSemantic semantic) {
		delegate.setEntityGraph( graph, semantic );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery enableFetchProfile(@Nonnull String profileName) {
		delegate.enableFetchProfile( profileName );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public MutationOrSelectionQuery disableFetchProfile(@Nonnull String profileName) {
		delegate.disableFetchProfile( profileName );
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter metadata
	@Override
	@Nonnull
	public ParameterMetadata getParameterMetadata() {
		return delegate.getParameterMetadata();
	}

	@Override
	@Nonnull
	public Set<Parameter<?>> getParameters() {
		return delegate.getParameters();
	}

	@Override
	@Nonnull
	public Parameter<?> getParameter(@Nonnull String name) {
		return delegate.getParameter( name );
	}

	@Override
	@Nonnull
	public <T> Parameter<T> getParameter(@Nonnull String name, @Nonnull Class<T> type) {
		return delegate.getParameter( name, type );
	}

	@Override
	@Nonnull
	public Parameter<?> getParameter(int position) {
		return delegate.getParameter( position );
	}

	@Override
	@Nonnull
	public <T> Parameter<T> getParameter(int position, @Nonnull Class<T> type) {
		return delegate.getParameter( position, type );
	}

	@Override
	public boolean isBound(@Nonnull Parameter<?> param) {
		return delegate.isBound( param );
	}

	@Override
	public <T> T getParameterValue(@Nonnull Parameter<T> param) {
		return delegate.getParameterValue( param );
	}

	@Override
	@Nullable
	public Object getParameterValue(@Nonnull String name) {
		return delegate.getParameterValue( name );
	}

	@Override
	public Object getParameterValue(int position) {
		return delegate.getParameterValue( position );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter binding
	@Override
	@Nonnull
	public MutationOrSelectionQuery setParameter(@Nonnull String name, @Nullable Object value) {
		delegate.setParameter( name, value );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<P> type) {
		delegate.setParameter( name, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameter(@Nonnull String name, @Nullable P value, @Nonnull Type<P> type) {
		delegate.setParameter( name, value, type );
		return this;
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setParameter(int position, @Nullable Object value) {
		delegate.setParameter( position, value );
		return this;
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setParameters(@Nonnull Object... arguments) {
		delegate.setParameters( arguments );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameter(int position, @Nullable P value, @Nonnull Class<P> type) {
		delegate.setParameter( position, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameter(int position, @Nullable P value, @Nonnull Type<P> type) {
		delegate.setParameter( position, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setConvertedParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter) {
		delegate.setConvertedParameter( name, value, converter );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setConvertedParameter(int position, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter) {
		delegate.setConvertedParameter( position, value, converter );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value) {
		delegate.setParameter( parameter, value );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value, @Nonnull Class<P> type) {
		delegate.setParameter( parameter, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P val, @Nonnull Type<P> type) {
		delegate.setParameter( parameter, val, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameter(@Nonnull Parameter<P> param, @Nullable P value) {
		delegate.setParameter( param, value );
		return this;
	}

	@Override @Deprecated
	@Nonnull
	public MutationOrSelectionQuery setParameter(@Nonnull String name, @Nullable Instant value, @Nonnull TemporalType temporalType) {
		delegate.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated
	@Nonnull
	public MutationOrSelectionQuery setParameter(@Nonnull String name, @Nullable Calendar value, @Nonnull TemporalType temporalType) {
		delegate.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated
	@Nonnull
	public MutationOrSelectionQuery setParameter(@Nonnull String name, @Nullable Date value, @Nonnull TemporalType temporalType) {
		delegate.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated
	@Nonnull
	public MutationOrSelectionQuery setParameter(int position, @Nullable Instant value, @Nonnull TemporalType temporalType) {
		delegate.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated
	@Nonnull
	public MutationOrSelectionQuery setParameter(int position, @Nullable Date value, @Nonnull TemporalType temporalType) {
		delegate.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated
	@Nonnull
	public MutationOrSelectionQuery setParameter(int position, @Nullable Calendar value, @Nonnull TemporalType temporalType) {
		delegate.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated
	@Nonnull
	public MutationOrSelectionQuery setParameter(@Nonnull Parameter<Calendar> param, @Nullable Calendar value, @Nonnull TemporalType temporalType) {
		delegate.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated
	@Nonnull
	public MutationOrSelectionQuery setParameter(@Nonnull Parameter<Date> param, @Nullable Date value, @Nonnull TemporalType temporalType) {
		delegate.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setParameterList(@Nonnull String name, @Nonnull @SuppressWarnings("rawtypes") Collection values) {
		delegate.setParameterList( name, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType) {
		delegate.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type) {
		delegate.setParameterList( name, values, type );
		return this;
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setParameterList(@Nonnull String name, @Nonnull Object[] values) {
		delegate.setParameterList( name, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Class<P> javaType) {
		delegate.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Type<P> type) {
		delegate.setParameterList( name, values, type );
		return this;
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setParameterList(int position, @Nonnull @SuppressWarnings("rawtypes") Collection values) {
		delegate.setParameterList( position, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType) {
		delegate.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type) {
		delegate.setParameterList( position, values, type );
		return this;
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setParameterList(int position, @Nonnull Object[] values) {
		delegate.setParameterList( position, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(int position, @Nonnull P[] values, @Nonnull Class<P> javaType) {
		delegate.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(int position, @Nonnull P[] values, @Nonnull Type<P> type) {
		delegate.setParameterList( position, values, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values) {
		delegate.setParameterList( parameter, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType) {
		delegate.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type) {
		delegate.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values) {
		delegate.setParameterList( parameter, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Class<P> javaType) {
		delegate.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationOrSelectionQuery setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Type<P> type) {
		delegate.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setProperties(@Nonnull Object bean) {
		delegate.setProperties( bean );
		return this;
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery setProperties(@Nonnull @SuppressWarnings("rawtypes") Map bean) {
		delegate.setProperties( bean );
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Unwrap

	@Override
	@Nonnull
	public <T> T unwrap(Class<T> cls) {
		return cls.isInstance( this ) ? cls.cast( this ) : delegate.unwrap( cls );
	}
}
