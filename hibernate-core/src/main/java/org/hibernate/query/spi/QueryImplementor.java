/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.query.Query;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface QueryImplementor<T> extends Query<T>, CommonQueryContractImplementor {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Casts

	@Override
	SelectionQueryImplementor<T> asSelectionQuery();

	@Override
	MutationQueryImplementor<T> asMutationQuery();

	@Override
	MutationQueryImplementor<T> asStatement();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override
	QueryImplementor<T> setQueryFlushMode(QueryFlushMode queryFlushMode);

	@Override
	QueryImplementor<T> setFlushMode(FlushModeType flushMode);

	@Override
	QueryImplementor<T> setComment(String comment);

	@Override
	QueryImplementor<T> addQueryHint(String hint);

	@Override
	QueryImplementor<T> setTimeout(int timeout);

	@Override
	QueryImplementor<T> setTimeout(Integer timeout);

	@Override
	QueryImplementor<T> setTimeout(Timeout timeout);

	@Override
	QueryImplementor<T> setQueryPlanCacheable(boolean queryPlanCacheable);

	@Override
	QueryImplementor<T> setCacheStoreMode(CacheStoreMode cacheStoreMode);

	@Override
	QueryImplementor<T> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode);

	@Override
	QueryImplementor<T> setMaxResults(int maxResults);

	@Override
	QueryImplementor<T> setFirstResult(int startPosition);

	@Override
	QueryImplementor<T> setLockMode(LockModeType lockMode);

	@Override
	QueryImplementor<T> setHibernateLockMode(LockMode lockMode);

	@Override
	QueryImplementor<T> setLockScope(PessimisticLockScope lockScope);

	@Override
	QueryImplementor<T> setFollowOnLockingStrategy(Locking.FollowOn strategy);

	@Override
	QueryImplementor<T> setFollowOnStrategy(Locking.FollowOn strategy);

	@Override
	QueryImplementor<T> setHint(String hintName, Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	@Override
	ScrollableResultsImplementor<T> scroll();

	@Override
	ScrollableResultsImplementor<T> scroll(ScrollMode scrollMode);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter Handling

	QueryParameterBindings getParameterBindings();

	@Override
	QueryImplementor<T> setParameter(String name, Object value);

	@Override
	<P> QueryImplementor<T> setParameter(String name, P value, Class<P> type);

	@Override
	<P> QueryImplementor<T> setParameter(String name, P value, Type<P> type);

	@Override
	QueryImplementor<T> setParameter(int position, Object value);

	@Override
	<P> QueryImplementor<T> setParameter(int position, P value, Class<P> type);

	@Override
	<P> QueryImplementor<T> setParameter(int position, P value, Type<P> type);

	@Override
	<P> QueryImplementor<T> setParameter(QueryParameter<P> parameter, P value);

	@Override
	<P> QueryImplementor<T> setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> QueryImplementor<T> setParameter(QueryParameter<P> parameter, P val, Type<P> type);

	@Override
	<P> QueryImplementor<T> setParameter(Parameter<P> param, P value);

	@Override
	QueryImplementor<T> setProperties(Object bean);

	@Override
	QueryImplementor<T> setProperties(@SuppressWarnings("rawtypes") Map bean);

	@Override
	<P> QueryImplementor<T> setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	<P> QueryImplementor<T> setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	QueryImplementor<T> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> QueryImplementor<T> setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> QueryImplementor<T> setParameterList(String name, Collection<? extends P> values, Type<P> type);

	@Override
	QueryImplementor<T> setParameterList(String name, Object[] values);

	@Override
	<P> QueryImplementor<T> setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> QueryImplementor<T> setParameterList(String name, P[] values, Type<P> type);

	@Override
	QueryImplementor<T> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> QueryImplementor<T> setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> QueryImplementor<T> setParameterList(int position, Collection<? extends P> values, Type<P> type);

	@Override
	QueryImplementor<T> setParameterList(int position, Object[] values);

	@Override
	<P> QueryImplementor<T> setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> QueryImplementor<T> setParameterList(int position, P[] values, Type<P> type);

	@Override
	<P> QueryImplementor<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> QueryImplementor<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> QueryImplementor<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type);

	@Override
	<P> QueryImplementor<T> setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> QueryImplementor<T> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> QueryImplementor<T> setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type);

	@Override @Deprecated(since = "7")
	QueryImplementor<T> setParameter(int position, Instant value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	QueryImplementor<T> setParameter(int position, Date value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	QueryImplementor<T> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	QueryImplementor<T> setParameter(String name, Instant value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	QueryImplementor<T> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	QueryImplementor<T> setParameter(String name, Date value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	QueryImplementor<T> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	QueryImplementor<T> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);
}
