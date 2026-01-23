/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

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
import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.Page;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.named.TypedQueryReferenceProducer;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * SPI form of {@linkplain SelectionQuery}
 *
 * @author Steve Ebersole
 */
public interface SelectionQueryImplementor<R>
		extends SelectionQuery<R>, QueryImplementor<R>, TypedQueryReferenceProducer {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	@Override
	ScrollableResultsImplementor<R> scroll();

	@Override
	ScrollableResultsImplementor<R> scroll(ScrollMode scrollMode);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override
	SelectionQueryImplementor<R> setEntityGraph(EntityGraph<? super R> entityGraph);

	@Override
	SelectionQueryImplementor<R> setEntityGraph(EntityGraph<? super R> graph, GraphSemantic semantic);

	@Override
	SelectionQueryImplementor<R> enableFetchProfile(String profileName);

	@Override
	SelectionQueryImplementor<R> disableFetchProfile(String profileName);

	@Override
	SelectionQueryImplementor<R> setFlushMode(FlushModeType flushMode);

	@Override
	SelectionQueryImplementor<R> setQueryFlushMode(QueryFlushMode queryFlushMode);

	@Override
	SelectionQueryImplementor<R> setTimeout(int timeout);

	@Override
	SelectionQueryImplementor<R> setTimeout(Integer timeout);

	@Override
	SelectionQueryImplementor<R> setTimeout(Timeout timeout);

	@Override
	SelectionQueryImplementor<R> setComment(String comment);

	@Override
	SelectionQueryImplementor<R> setFetchSize(int fetchSize);

	@Override
	SelectionQueryImplementor<R> setReadOnly(boolean readOnly);

	@Override
	SelectionQueryImplementor<R> setMaxResults(int maxResults);

	@Override
	SelectionQueryImplementor<R> setFirstResult(int startPosition);

	@Override
	SelectionQueryImplementor<R> setPage(Page page);

	@Override
	SelectionQueryImplementor<R> setCacheMode(CacheMode cacheMode);

	@Override
	SelectionQueryImplementor<R> setCacheStoreMode(CacheStoreMode cacheStoreMode);

	@Override
	SelectionQueryImplementor<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode);

	@Override
	SelectionQueryImplementor<R> setCacheable(boolean cacheable);

	@Override
	SelectionQueryImplementor<R> setQueryPlanCacheable(boolean queryPlanCacheable);

	@Override
	SelectionQueryImplementor<R> setCacheRegion(String cacheRegion);

	@Override
	SelectionQueryImplementor<R> setLockMode(LockModeType lockMode);

	@Override
	SelectionQueryImplementor<R> setLockTimeout(Timeout lockTimeout);

	@Override
	SelectionQueryImplementor<R> setHibernateLockMode(LockMode lockMode);

	@Override
	SelectionQueryImplementor<R> setFollowOnLockingStrategy(Locking.FollowOn followOnStrategy);

	@Override
	SelectionQueryImplementor<R> setFollowOnStrategy(Locking.FollowOn followOnStrategy);

	@Override
	<T> SelectionQueryImplementor<T> setTupleTransformer(TupleTransformer<T> transformer);

	@Override
	SelectionQueryImplementor<R> setResultListTransformer(ResultListTransformer<R> transformer);

	@Override
	SelectionQueryImplementor<R> setLockScope(PessimisticLockScope lockScope);

	@Override
	SelectionQueryImplementor<R> addQueryHint(String hint);

	@Override
	SelectionQueryImplementor<R> setHint(String hintName, Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter Handling

	@Override
	SelectionQueryImplementor<R> setParameter(String name, Object value);

	@Override
	<P> SelectionQueryImplementor<R> setParameter(String name, P value, Class<P> type);

	@Override
	<P> SelectionQueryImplementor<R> setParameter(String name, P value, Type<P> type);

	@Override
	SelectionQueryImplementor<R> setParameter(int position, Object value);

	@Override
	<P> SelectionQueryImplementor<R> setParameter(int position, P value, Class<P> type);

	@Override
	<P> SelectionQueryImplementor<R> setParameter(int position, P value, Type<P> type);

	@Override
	<T> SelectionQueryImplementor<R> setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> SelectionQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> SelectionQueryImplementor<R> setParameter(QueryParameter<P> parameter, P val, Type<P> type);

	@Override
	<T> SelectionQueryImplementor<R> setParameter(Parameter<T> param, T value);

	@Override
	SelectionQueryImplementor<R> setProperties(Object bean);

	@Override
	SelectionQueryImplementor<R> setProperties(Map bean);

	@Override
	<P> SelectionQueryImplementor<R> setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	<P> SelectionQueryImplementor<R> setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	SelectionQueryImplementor<R> setParameterList(String name, Collection values);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Type<P> type);

	@Override
	SelectionQueryImplementor<R> setParameterList(String name, Object[] values);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(String name, P[] values, Type<P> type);

	@Override
	SelectionQueryImplementor<R> setParameterList(int position, Collection values);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Type<P> type);

	@Override
	SelectionQueryImplementor<R> setParameterList(int position, Object[] values);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(int position, P[] values, Type<P> type);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> SelectionQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type);

	@Override
	SelectionQueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	SelectionQueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	SelectionQueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	SelectionQueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	SelectionQueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override
	SelectionQueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	SelectionQueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	SelectionQueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// MutationQuery Handling

	@Override
	default MutationQueryImplementor asMutationQuery() {
		throw new IllegalMutationQueryException( "SelectionQuery cannot be treated as a MutationQuery", getQueryString() );
	}

	@Override
	default MutationQueryImplementor asStatement() {
		throw new IllegalStateException( "SelectionQuery cannot be treated as a MutationQuery - " + getQueryString() );
	}

	@Override
	default int executeUpdate() {
		// per JPA, again, needs to be IllegalStateException
		throw new IllegalStateException( "SelectionQuery cannot be treated as a MutationQuery - " + getQueryString() );
	}
}
