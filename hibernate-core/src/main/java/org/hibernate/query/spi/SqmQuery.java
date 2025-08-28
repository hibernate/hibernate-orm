/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.CommonQueryContract;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.sqm.tree.SqmStatement;

import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.metamodel.Type;

/**
 * Query based on an SQM tree.
 *
 * @author Steve Ebersole
 */
public interface SqmQuery<R> extends CommonQueryContract {
	String getQueryString();

	SqmStatement<R> getSqmStatement();

	ParameterMetadata getParameterMetadata();

	QueryOptions getQueryOptions();

	@Override
	SqmQuery<R> setParameter(String name, Object value);

	@Override
	<P> SqmQuery<R> setParameter(String name, P value, Class<P> type);

	@Override
	<P> SqmQuery<R> setParameter(String name, P value, Type<P> type);

	@Override @Deprecated(since = "7")
	SqmQuery<R> setParameter(String name, Instant value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	SqmQuery<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	SqmQuery<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	SqmQuery<R> setParameter(int position, Object value);

	@Override
	<P> SqmQuery<R> setParameter(int position, P value, Class<P> type);

	@Override
	<P> SqmQuery<R> setParameter(int position, P value, Type<P> type);

	@Override @Deprecated(since = "7")
	SqmQuery<R> setParameter(int position, Instant value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	SqmQuery<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	SqmQuery<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> SqmQuery<R> setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> SqmQuery<R> setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> SqmQuery<R> setParameter(QueryParameter<P> parameter, P val, Type<P> type);

	@Override
	<T> SqmQuery<R> setParameter(Parameter<T> param, T value);

	@Override @Deprecated(since = "7")
	SqmQuery<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	SqmQuery<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	SqmQuery<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SqmQuery<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmQuery<R> setParameterList(String name, Collection<? extends P> values, Type<P> type);

	@Override
	SqmQuery<R> setParameterList(String name, Object[] values);

	@Override
	<P> SqmQuery<R> setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> SqmQuery<R> setParameterList(String name, P[] values, Type<P> type);

	@Override
	SqmQuery<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SqmQuery<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmQuery<R> setParameterList(int position, Collection<? extends P> values, Type<P> type);

	@Override
	SqmQuery<R> setParameterList(int position, Object[] values);

	@Override
	<P> SqmQuery<R> setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> SqmQuery<R> setParameterList(int position, P[] values, Type<P> type);

	@Override
	<P> SqmQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> SqmQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type);

	@Override
	<P> SqmQuery<R> setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> SqmQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> SqmQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type);

	@Override
	SqmQuery<R> setProperties(Object bean);

	@Override
	SqmQuery<R> setProperties(@SuppressWarnings("rawtypes") Map bean);

	@Override @Deprecated(since = "7")
	SqmQuery<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	SqmQuery<R> setQueryFlushMode(QueryFlushMode queryFlushMode);
}
