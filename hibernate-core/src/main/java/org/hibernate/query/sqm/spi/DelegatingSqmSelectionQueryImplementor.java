/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.spi;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.Page;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.SqmSelectionQuery;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.sql.results.spi.ResultsConsumer;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;

@Incubating
public abstract class DelegatingSqmSelectionQueryImplementor<R> implements SqmSelectionQueryImplementor<R> {

	protected abstract SqmSelectionQueryImplementor<R> getDelegate();

	@Override @Deprecated
	public FlushModeType getFlushMode() {
		return getDelegate().getFlushMode();
	}

	@Override @Deprecated
	public SqmSelectionQueryImplementor<R> setFlushMode(FlushModeType flushMode) {
		getDelegate().setFlushMode( flushMode );
		return this;
	}

	@Override @Deprecated
	public FlushMode getHibernateFlushMode() {
		return getDelegate().getHibernateFlushMode();
	}

	@Override
	public QueryFlushMode getQueryFlushMode() {
		return getDelegate().getQueryFlushMode();
	}

	@Override
	public SqmSelectionQuery<R> setQueryFlushMode(QueryFlushMode queryFlushMode) {
		return getDelegate().setQueryFlushMode( queryFlushMode );
	}

	@Override
	public Integer getTimeout() {
		return getDelegate().getTimeout();
	}

	@Override
	public String getComment() {
		return getDelegate().getComment();
	}

	@Override
	public SqmSelectionQueryImplementor<R> setComment(String comment) {
		getDelegate().setComment( comment );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setHint(String hintName, Object value) {
		getDelegate().setHint( hintName, value );
		return this;
	}

	@Override
	public List<R> list() {
		return getDelegate().list();
	}

	@Override
	public List<R> getResultList() {
		return getDelegate().getResultList();
	}

	@Override
	public long getResultCount() {
		return getDelegate().getResultCount();
	}

	@Override
	public KeyedResultList<R> getKeyedResultList(KeyedPage<R> page) {
		return getDelegate().getKeyedResultList( page );
	}

	@Override
	public ScrollableResults<R> scroll() {
		return getDelegate().scroll();
	}

	@Override
	public ScrollableResults<R> scroll(ScrollMode scrollMode) {
		return getDelegate().scroll( scrollMode );
	}

	@Override
	public Stream<R> getResultStream() {
		return getDelegate().getResultStream();
	}

	@Override
	public Stream<R> stream() {
		return getDelegate().stream();
	}

	@Override
	public R uniqueResult() {
		return getDelegate().uniqueResult();
	}

	@Override
	public R getSingleResult() {
		return getDelegate().getSingleResult();
	}

	@Override
	public R getSingleResultOrNull() {
		return getDelegate().getSingleResultOrNull();
	}

	@Override
	public Optional<R> uniqueResultOptional() {
		return getDelegate().uniqueResultOptional();
	}

	@Override
	public SqmSelectionQueryImplementor<R> setEntityGraph(EntityGraph<? super R> graph, GraphSemantic semantic) {
		getDelegate().setEntityGraph( graph, semantic );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> enableFetchProfile(String profileName) {
		getDelegate().enableFetchProfile( profileName );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> disableFetchProfile(String profileName) {
		getDelegate().disableFetchProfile( profileName );
		return this;
	}

	@Override
	public Integer getFetchSize() {
		return getDelegate().getFetchSize();
	}

	@Override
	public boolean isReadOnly() {
		return getDelegate().isReadOnly();
	}

	@Override
	public int getMaxResults() {
		return getDelegate().getMaxResults();
	}

	@Override
	public SqmSelectionQueryImplementor<R> setMaxResults(int maxResults) {
		getDelegate().setMaxResults( maxResults );
		return this;
	}

	@Override
	public int getFirstResult() {
		return getDelegate().getFirstResult();
	}

	@Override
	public SqmSelectionQueryImplementor<R> setFirstResult(int startPosition) {
		getDelegate().setFirstResult( startPosition );
		return this;
	}

	@Override
	@Incubating
	public SqmSelectionQueryImplementor<R> setPage(Page page) {
		getDelegate().setPage( page );
		return this;
	}

	@Override
	public CacheMode getCacheMode() {
		return getDelegate().getCacheMode();
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return getDelegate().getCacheStoreMode();
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return getDelegate().getCacheRetrieveMode();
	}

	@Override
	public SqmSelectionQueryImplementor<R> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		getDelegate().setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		getDelegate().setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	public boolean isCacheable() {
		return getDelegate().isCacheable();
	}

	@Override
	public boolean isQueryPlanCacheable() {
		return getDelegate().isQueryPlanCacheable();
	}

	@Override
	public SqmSelectionQueryImplementor<R> setQueryPlanCacheable(boolean queryPlanCacheable) {
		getDelegate().setQueryPlanCacheable( queryPlanCacheable );
		return this;
	}

	@Override
	public String getCacheRegion() {
		return getDelegate().getCacheRegion();
	}

	@Override @Deprecated
	public LockOptions getLockOptions() {
		return getDelegate().getLockOptions();
	}

	@Override
	public LockModeType getLockMode() {
		return getDelegate().getLockMode();
	}

	@Override
	public SqmSelectionQueryImplementor<R> setLockMode(LockModeType lockMode) {
		getDelegate().setLockMode( lockMode );
		return this;
	}

	@Override
	public LockMode getHibernateLockMode() {
		return getDelegate().getHibernateLockMode();
	}

	@Override
	public SqmSelectionQueryImplementor<R> setHibernateLockMode(LockMode lockMode) {
		getDelegate().setHibernateLockMode( lockMode );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setTimeout(Timeout timeout) {
		getDelegate().setTimeout( timeout );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setLockScope(PessimisticLockScope lockScope) {
		getDelegate().setLockScope( lockScope );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setLockMode(String alias, LockMode lockMode) {
		getDelegate().setLockMode( alias, lockMode );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setFollowOnLocking(boolean enable) {
		getDelegate().setFollowOnLocking( enable );
		return this;
	}

	@Override
	public String getQueryString() {
		return getDelegate().getQueryString();
	}

	@Override
	public SqmStatement<R> getSqmStatement() {
		return getDelegate().getSqmStatement();
	}

	@Override
	public ParameterMetadata getParameterMetadata() {
		return getDelegate().getParameterMetadata();
	}

	@Override
	public QueryOptions getQueryOptions() {
		return getDelegate().getQueryOptions();
	}

	@Override
	public SqmSelectionQueryImplementor<R> setParameter(String name, Object value) {
		getDelegate().setParameter( name, value );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameter(String name, P value, Class<P> type) {
		getDelegate().setParameter( name, value, type );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameter(String name, P value, Type<P> type) {
		getDelegate().setParameter( name, value, type );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType) {
		getDelegate().setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		getDelegate().setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		getDelegate().setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setParameter(int position, Object value) {
		getDelegate().setParameter( position, value );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameter(int position, P value, Class<P> type) {
		getDelegate().setParameter( position, value, type );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameter(int position, P value, Type<P> type) {
		getDelegate().setParameter( position, value, type );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType) {
		getDelegate().setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType) {
		getDelegate().setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		getDelegate().setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public <T> SqmSelectionQueryImplementor<R> setParameter(QueryParameter<T> parameter, T value) {
		getDelegate().setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Class<P> type) {
		getDelegate().setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameter(QueryParameter<P> parameter, P val, Type<P> type) {
		getDelegate().setParameter( parameter, val, type );
		return this;
	}

	@Override
	public <T> SqmSelectionQueryImplementor<R> setParameter(Parameter<T> param, T value) {
		getDelegate().setParameter( param, value );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		getDelegate().setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		getDelegate().setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		getDelegate().setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		getDelegate().setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(
			String name,
			Collection<? extends P> values,
			Type<P> type) {
		getDelegate().setParameterList( name, values, type );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setParameterList(String name, Object[] values) {
		getDelegate().setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(String name, P[] values, Class<P> javaType) {
		getDelegate().setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(String name, P[] values, Type<P> type) {
		getDelegate().setParameterList( name, values, type );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		getDelegate().setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		getDelegate().setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(
			int position,
			Collection<? extends P> values,
			Type<P> type) {
		getDelegate().setParameterList( position, values, type );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setParameterList(int position, Object[] values) {
		getDelegate().setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaType) {
		getDelegate().setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(int position, P[] values, Type<P> type) {
		getDelegate().setParameterList( position, values, type );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		getDelegate().setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(
			QueryParameter<P> parameter,
			Collection<? extends P> values,
			Class<P> javaType) {
		getDelegate().setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(
			QueryParameter<P> parameter,
			Collection<? extends P> values,
			Type<P> type) {
		getDelegate().setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		getDelegate().setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		getDelegate().setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SqmSelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type) {
		getDelegate().setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setProperties(Object bean) {
		getDelegate().setProperties( bean );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setProperties(@SuppressWarnings("rawtypes") Map bean) {
		getDelegate().setProperties( bean );
		return this;
	}

	@Override @Deprecated
	public SqmSelectionQueryImplementor<R> setHibernateFlushMode(FlushMode flushMode) {
		getDelegate().setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setCacheMode(CacheMode cacheMode) {
		getDelegate().setCacheMode( cacheMode );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setCacheable(boolean cacheable) {
		getDelegate().setCacheable( cacheable );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setCacheRegion(String cacheRegion) {
		getDelegate().setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setTimeout(int timeout) {
		getDelegate().setTimeout( timeout );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setFetchSize(int fetchSize) {
		getDelegate().setFetchSize( fetchSize );
		return this;
	}

	@Override
	public SqmSelectionQueryImplementor<R> setReadOnly(boolean readOnly) {
		getDelegate().setReadOnly( readOnly );
		return this;
	}

	@Override
	public <T> T executeQuery(ResultsConsumer<T, R> resultsConsumer) {
		return getDelegate().executeQuery( resultsConsumer );
	}


	@Override
	public <T> SqmSelectionQuery<T> setTupleTransformer(TupleTransformer<T> transformer) {
		return getDelegate().setTupleTransformer( transformer );
	}

	@Override
	public SqmSelectionQuery<R> setResultListTransformer(ResultListTransformer<R> transformer) {
		return getDelegate().setResultListTransformer( transformer );
	}
}
