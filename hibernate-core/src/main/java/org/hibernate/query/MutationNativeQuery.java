/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.hibernate.FlushMode;

import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
public interface MutationNativeQuery extends MutationQuery {
	@Override
	MutationNativeQuery setParameter(String name, Object value);

	@Override
	<P> MutationNativeQuery setParameter(String name, P value, Class<P> type);

	@Override
	<P> MutationNativeQuery setParameter(String name, P value, BindableType<P> type);

	@Override
	MutationNativeQuery setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	MutationNativeQuery setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	MutationNativeQuery setParameter(String name, Date value, TemporalType temporalType);

	@Override
	MutationNativeQuery setParameter(int position, Object value);

	@Override
	<P> MutationNativeQuery setParameter(int position, P value, Class<P> type);

	@Override
	<P> MutationNativeQuery setParameter(int position, P value, BindableType<P> type);

	@Override
	MutationNativeQuery setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	MutationNativeQuery setParameter(int position, Date value, TemporalType temporalType);

	@Override
	MutationNativeQuery setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> MutationNativeQuery setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> MutationNativeQuery setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> MutationNativeQuery setParameter(QueryParameter<P> parameter, P val, BindableType<P> type);

	@Override
	<T> MutationNativeQuery setParameter(Parameter<T> param, T value);

	@Override
	MutationNativeQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	MutationNativeQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	MutationNativeQuery setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> MutationNativeQuery setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationNativeQuery setParameterList(String name, Collection<? extends P> values, BindableType<P> type);

	@Override
	MutationNativeQuery setParameterList(String name, Object[] values);

	@Override
	<P> MutationNativeQuery setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> MutationNativeQuery setParameterList(String name, P[] values, BindableType<P> type);

	@Override
	MutationNativeQuery setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> MutationNativeQuery setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationNativeQuery setParameterList(int position, Collection<? extends P> values, BindableType<P> type);

	@Override
	MutationNativeQuery setParameterList(int position, Object[] values);

	@Override
	<P> MutationNativeQuery setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> MutationNativeQuery setParameterList(int position, P[] values, BindableType<P> type);

	@Override
	<P> MutationNativeQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> MutationNativeQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> MutationNativeQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type);

	@Override
	<P> MutationNativeQuery setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> MutationNativeQuery setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> MutationNativeQuery setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type);

	@Override
	MutationNativeQuery setProperties(Object bean);

	@Override
	MutationNativeQuery setProperties(Map bean);

	@Override
	MutationNativeQuery setHibernateFlushMode(FlushMode flushMode);

	@Override
	MutationNativeQuery setTimeout(int timeout);
}
