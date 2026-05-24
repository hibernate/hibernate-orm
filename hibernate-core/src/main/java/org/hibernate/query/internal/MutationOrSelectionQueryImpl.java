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
import org.hibernate.query.MutationOrSelectionQuery;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
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
	public SelectionQuery<?> asSelectionQuery() {
		return delegate.asSelectionQuery();
	}

	@Override
	public <R> SelectionQuery<R> asSelectionQuery(Class<R> type) {
		return delegate.asSelectionQuery( type );
	}

	@Override
	public <X> SelectionQuery<X> asSelectionQuery(EntityGraph<X> entityGraph) {
		return delegate.asSelectionQuery( entityGraph );
	}

	@Override
	public <X> SelectionQuery<X> asSelectionQuery(EntityGraph<X> entityGraph, GraphSemantic graphSemantic) {
		return delegate.asSelectionQuery( entityGraph, graphSemantic );
	}

	@Override
	public MutationQuery asMutationQuery() {
		return delegate.asMutationQuery();
	}

	@Override
	public MutationQuery asStatement() {
		return delegate.asMutationQuery();
	}

	@Override
	public <R> SelectionQuery<R> ofType(Class<R> type) {
		return delegate.asSelectionQuery( type );
	}

	@Override
	public <R> SelectionQuery<R> withEntityGraph(EntityGraph<R> entityGraph) {
		return delegate.asSelectionQuery( entityGraph );
	}

	@Override
	public <R> SelectionQuery<R> withResultSetMapping(ResultSetMapping<R> mapping) {
		return delegate.withResultSetMapping( mapping );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query identity

	@Override
	public String getQueryString() {
		return delegate.getQueryString();
	}

	@Override
	public SharedSessionContract getSession() {
		return delegate.getSession();
	}

	@Override @Deprecated @SuppressWarnings("removal")
	public QueryOptions getQueryOptions() {
		return delegate.getQueryOptions();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	@Override
	public int executeUpdate() {
		return delegate.asMutationQuery().execute();
	}

	@Override @Deprecated
	@SuppressWarnings("rawtypes")
	public List list() {
		return delegate.list();
	}

	@Override @Deprecated
	@SuppressWarnings("rawtypes")
	public List getResultList() {
		return delegate.getResultList();
	}

	@Override @Deprecated
	public Object uniqueResult() {
		return delegate.uniqueResult();
	}

	@Override @Deprecated
	public Object getSingleResult() {
		return delegate.getSingleResult();
	}

	@Override @Deprecated
	public Object getSingleResultOrNull() {
		return delegate.getSingleResultOrNull();
	}

	@Override @Deprecated
	@SuppressWarnings("rawtypes")
	public Optional uniqueResultOptional() {
		return delegate.uniqueResultOptional();
	}

	@Override @Deprecated
	@SuppressWarnings("rawtypes")
	public Stream getResultStream() {
		return delegate.getResultStream();
	}

	@Override @Deprecated
	@SuppressWarnings("rawtypes")
	public Stream stream() {
		return delegate.stream();
	}

	@Override @Deprecated
	@SuppressWarnings("rawtypes")
	public ScrollableResults scroll() {
		return delegate.scroll();
	}

	@Override @Deprecated
	@SuppressWarnings("rawtypes")
	public ScrollableResults scroll(ScrollMode scrollMode) {
		return delegate.scroll( scrollMode );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override
	public FlushMode getEffectiveFlushMode() {
		return delegate.getEffectiveFlushMode();
	}

	@Override
	public QueryFlushMode getQueryFlushMode() {
		return delegate.getQueryFlushMode();
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public FlushModeType getFlushMode() {
		return delegate.getFlushMode();
	}

	@Override
	public MutationOrSelectionQuery setQueryFlushMode(QueryFlushMode queryFlushMode) {
		delegate.setQueryFlushMode( queryFlushMode );
		return this;
	}

	@Override @SuppressWarnings("deprecation")
	public MutationOrSelectionQuery setFlushMode(FlushModeType flushMode) {
		delegate.setFlushMode( flushMode );
		return this;
	}

	@Override
	public String getComment() {
		return delegate.getComment();
	}

	@Override
	public MutationOrSelectionQuery setComment(String comment) {
		delegate.setComment( comment );
		return this;
	}

	@Override
	public MutationOrSelectionQuery addQueryHint(String hint) {
		delegate.addQueryHint( hint );
		return this;
	}

	@Override
	public Integer getTimeout() {
		return delegate.getTimeout();
	}

	@Override
	public MutationOrSelectionQuery setTimeout(int timeout) {
		delegate.setTimeout( timeout );
		return this;
	}

	@Override
	public MutationOrSelectionQuery setTimeout(Integer timeout) {
		delegate.setTimeout( timeout );
		return this;
	}

	@Override
	public MutationOrSelectionQuery setTimeout(Timeout timeout) {
		delegate.setTimeout( timeout );
		return this;
	}

	@Override
	public MutationOrSelectionQuery setHint(String hintName, Object value) {
		delegate.setHint( hintName, value );
		return this;
	}

	@Override
	public Map<String, Object> getHints() {
		return delegate.getHints();
	}

	@Override
	@SuppressWarnings("removal")
	public Integer getFetchSize() {
		return delegate.getFetchSize();
	}

	@Override
	public MutationOrSelectionQuery setFetchSize(int fetchSize) {
		//noinspection removal
		delegate.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public boolean isReadOnly() {
		return delegate.isReadOnly();
	}

	@Override
	public MutationOrSelectionQuery setReadOnly(boolean readOnly) {
		delegate.setReadOnly( readOnly );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("removal")
	public int getMaxResults() {
		return delegate.getMaxResults();
	}

	@Override
	public MutationOrSelectionQuery setMaxResults(int maxResults) {
		delegate.setMaxResults( maxResults );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("removal")
	public int getFirstResult() {
		return delegate.getFirstResult();
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setFirstResult(int startPosition) {
		delegate.setFirstResult( startPosition );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("removal")
	public CacheMode getCacheMode() {
		return delegate.getCacheMode();
	}

	@Override @Deprecated @SuppressWarnings("removal")
	public MutationOrSelectionQuery setCacheMode(CacheMode cacheMode) {
		delegate.setCacheMode( cacheMode );
		return this;
	}

	@Override
	@Deprecated @SuppressWarnings("removal")
	public CacheStoreMode getCacheStoreMode() {
		return delegate.getCacheStoreMode();
	}

	@Override
	public MutationOrSelectionQuery setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		delegate.setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override
	@Deprecated @SuppressWarnings("removal")
	public CacheRetrieveMode getCacheRetrieveMode() {
		return delegate.getCacheRetrieveMode();
	}

	@Override
	public MutationOrSelectionQuery setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		delegate.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	public boolean isQueryPlanCacheable() {
		return delegate.isQueryPlanCacheable();
	}

	@Override
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
	public MutationOrSelectionQuery setCacheable(boolean cacheable) {
		//noinspection removal
		delegate.setCacheable( cacheable );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	public String getCacheRegion() {
		return delegate.getCacheRegion();
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setCacheRegion(String cacheRegion) {
		//noinspection removal
		delegate.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	@Deprecated @SuppressWarnings("removal")
	public LockModeType getLockMode() {
		return delegate.getLockMode();
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setLockMode(LockModeType lockMode) {
		delegate.setLockMode( lockMode );
		return this;
	}

	@Override @Deprecated
	public LockMode getHibernateLockMode() {
		return delegate.getHibernateLockMode();
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setHibernateLockMode(LockMode lockMode) {
		delegate.setHibernateLockMode( lockMode );
		return this;
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setLockScope(PessimisticLockScope lockScope) {
		delegate.setLockScope( lockScope );
		return this;
	}

	@Override @Deprecated
	public Timeout getLockTimeout() {
		return delegate.getLockTimeout();
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setLockTimeout(Timeout lockTimeout) {
		delegate.setLockTimeout( lockTimeout );
		return this;
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setFollowOnLockingStrategy(Locking.FollowOn strategy) {
		delegate.setFollowOnLockingStrategy( strategy );
		return this;
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setFollowOnStrategy(Locking.FollowOn followOnStrategy) {
		delegate.setFollowOnStrategy( followOnStrategy );
		return this;
	}

	@Override @Deprecated
	public <X> SelectionQuery<X> setTupleTransformer(TupleTransformer<X> transformer) {
		return (SelectionQuery<X>) delegate.setTupleTransformer( transformer );
	}

	@Override
	@Deprecated @SuppressWarnings({"unchecked","rawtypes"})
	public MutationOrSelectionQuery setResultListTransformer(ResultListTransformer<Object> transformer) {
		final var resultListTransformer = (ResultListTransformer) transformer;
		delegate.setResultListTransformer( resultListTransformer );
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter metadata

	@Override
	public ParameterMetadata getParameterMetadata() {
		return delegate.getParameterMetadata();
	}

	@Override
	public Set<Parameter<?>> getParameters() {
		return delegate.getParameters();
	}

	@Override
	public Parameter<?> getParameter(String name) {
		return delegate.getParameter( name );
	}

	@Override
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		return delegate.getParameter( name, type );
	}

	@Override
	public Parameter<?> getParameter(int position) {
		return delegate.getParameter( position );
	}

	@Override
	public <T> Parameter<T> getParameter(int position, Class<T> type) {
		return delegate.getParameter( position, type );
	}

	@Override
	public boolean isBound(Parameter<?> param) {
		return delegate.isBound( param );
	}

	@Override
	public <T> T getParameterValue(Parameter<T> param) {
		return delegate.getParameterValue( param );
	}

	@Override
	public Object getParameterValue(String name) {
		return delegate.getParameterValue( name );
	}

	@Override
	public Object getParameterValue(int position) {
		return delegate.getParameterValue( position );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter binding

	@Override
	public MutationOrSelectionQuery setParameter(String name, Object value) {
		delegate.setParameter( name, value );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameter(String name, P value, Class<P> type) {
		delegate.setParameter( name, value, type );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameter(String name, P value, Type<P> type) {
		delegate.setParameter( name, value, type );
		return this;
	}

	@Override
	public MutationOrSelectionQuery setParameter(int position, Object value) {
		delegate.setParameter( position, value );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameter(int position, P value, Class<P> type) {
		delegate.setParameter( position, value, type );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameter(int position, P value, Type<P> type) {
		delegate.setParameter( position, value, type );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter) {
		delegate.setConvertedParameter( name, value, converter );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter) {
		delegate.setConvertedParameter( position, value, converter );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameter(QueryParameter<P> parameter, P value) {
		delegate.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameter(QueryParameter<P> parameter, P value, Class<P> type) {
		delegate.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameter(QueryParameter<P> parameter, P val, Type<P> type) {
		delegate.setParameter( parameter, val, type );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameter(Parameter<P> param, P value) {
		delegate.setParameter( param, value );
		return this;
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setParameter(String name, Instant value, TemporalType temporalType) {
		delegate.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setParameter(String name, Calendar value, TemporalType temporalType) {
		delegate.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setParameter(String name, Date value, TemporalType temporalType) {
		delegate.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setParameter(int position, Instant value, TemporalType temporalType) {
		delegate.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setParameter(int position, Date value, TemporalType temporalType) {
		delegate.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setParameter(int position, Calendar value, TemporalType temporalType) {
		delegate.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		delegate.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public MutationOrSelectionQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		delegate.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public MutationOrSelectionQuery setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		delegate.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		delegate.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(String name, Collection<? extends P> values, Type<P> type) {
		delegate.setParameterList( name, values, type );
		return this;
	}

	@Override
	public MutationOrSelectionQuery setParameterList(String name, Object[] values) {
		delegate.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(String name, P[] values, Class<P> javaType) {
		delegate.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(String name, P[] values, Type<P> type) {
		delegate.setParameterList( name, values, type );
		return this;
	}

	@Override
	public MutationOrSelectionQuery setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		delegate.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		delegate.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(int position, Collection<? extends P> values, Type<P> type) {
		delegate.setParameterList( position, values, type );
		return this;
	}

	@Override
	public MutationOrSelectionQuery setParameterList(int position, Object[] values) {
		delegate.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(int position, P[] values, Class<P> javaType) {
		delegate.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(int position, P[] values, Type<P> type) {
		delegate.setParameterList( position, values, type );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		delegate.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		delegate.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type) {
		delegate.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(QueryParameter<P> parameter, P[] values) {
		delegate.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		delegate.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> MutationOrSelectionQuery setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type) {
		delegate.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public MutationOrSelectionQuery setProperties(Object bean) {
		delegate.setProperties( bean );
		return this;
	}

	@Override
	public MutationOrSelectionQuery setProperties(@SuppressWarnings("rawtypes") Map bean) {
		delegate.setProperties( bean );
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Unwrap

	@Override
	public <T> T unwrap(Class<T> cls) {
		return cls.isInstance( this ) ? cls.cast( this ) : delegate.unwrap( cls );
	}
}
