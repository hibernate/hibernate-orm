/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.SqmQuery;

import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.metamodel.Type;

/**
 * @author Steve Ebersole
 */
public interface SqmSelectionQuery<R> extends SqmQuery<R>, SelectionQuery<R> {

	@Override
	SqmSelectionQuery<R> setParameter(String name, Object value);

	@Override
	<P> SqmSelectionQuery<R> setParameter(String name, P value, Class<P> type);

	@Override
	<P> SqmSelectionQuery<R> setParameter(String name, P value, Type<P> type);

	@Override @Deprecated(since = "7")
	SqmSelectionQuery<R> setParameter(String name, Instant value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	SqmSelectionQuery<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	SqmSelectionQuery<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	SqmSelectionQuery<R> setParameter(int position, Object value);

	@Override
	<P> SqmSelectionQuery<R> setParameter(int position, P value, Class<P> type);

	@Override
	<P> SqmSelectionQuery<R> setParameter(int position, P value, Type<P> type);

	@Override @Deprecated(since = "7")
	SqmSelectionQuery<R> setParameter(int position, Instant value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	SqmSelectionQuery<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	SqmSelectionQuery<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> SqmSelectionQuery<R> setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> SqmSelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> SqmSelectionQuery<R> setParameter(QueryParameter<P> parameter, P val, Type<P> type);

	@Override
	<T> SqmSelectionQuery<R> setParameter(Parameter<T> param, T value);

	@Override @Deprecated(since = "7")
	SqmSelectionQuery<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	SqmSelectionQuery<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	SqmSelectionQuery<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(String name, Collection<? extends P> values, Type<P> type);

	@Override
	SqmSelectionQuery<R> setParameterList(String name, Object[] values);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(String name, P[] values, Type<P> type);

	@Override
	SqmSelectionQuery<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(int position, Collection<? extends P> values, Type<P> type);

	@Override
	SqmSelectionQuery<R> setParameterList(int position, Object[] values);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(int position, P[] values, Type<P> type);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type);

	@Override
	SqmSelectionQuery<R> setProperties(Object bean);

	@Override
	SqmSelectionQuery<R> setProperties(@SuppressWarnings("rawtypes") Map bean);

	@Override @Deprecated(since = "7")
	SqmSelectionQuery<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	SqmSelectionQuery<R> setQueryFlushMode(QueryFlushMode queryFlushMode);

	@Override
	SqmSelectionQuery<R> setCacheMode(CacheMode cacheMode);

	@Override
	SqmSelectionQuery<R> setCacheable(boolean cacheable);

	@Override
	SqmSelectionQuery<R> setCacheRegion(String cacheRegion);

	@Override
	SqmSelectionQuery<R> setTimeout(int timeout);

	@Override
	SqmSelectionQuery<R> setFetchSize(int fetchSize);

	@Override
	SqmSelectionQuery<R> setReadOnly(boolean readOnly);

	@Override
	<T> SqmSelectionQuery<T> setTupleTransformer(TupleTransformer<T> transformer);

	@Override
	SqmSelectionQuery<R> setResultListTransformer(ResultListTransformer<R> transformer);
}
