/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import java.io.Serializable;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.AllowableParameterType;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;

import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface QueryImplementor<R> extends Query<R> {
	@Override
	SharedSessionContractImplementor getSession();

	void setOptionalId(Serializable id);

	void setOptionalEntityName(String entityName);

	void setOptionalObject(Object optionalObject);

	QueryParameterBindings getParameterBindings();

	@Override
	ScrollableResultsImplementor<R> scroll();

	@Override
	ScrollableResultsImplementor<R> scroll(ScrollMode scrollMode);


	@Override
	QueryImplementor<R> setParameter(String name, Object value);

	@Override
	<P> QueryImplementor<R> setParameter(String name, P value, Class<P> type);

	@Override
	<P> QueryImplementor<R> setParameter(String name, P value, AllowableParameterType<P> type);

	@Override
	QueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(int position, Object value);

	@Override
	<P> QueryImplementor<R> setParameter(int position, P value, Class<P> type);

	@Override
	<P> QueryImplementor<R> setParameter(int position, P value, AllowableParameterType<P> type);

	@Override
	QueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> QueryImplementor<R> setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> QueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> QueryImplementor<R> setParameter(QueryParameter<P> parameter, P val, AllowableParameterType<P> type);

	@Override
	<T> QueryImplementor<R> setParameter(Parameter<T> param, T value);

	@Override
	QueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	QueryImplementor<R> setParameterList(String name, Collection values);

	@Override
	<P> QueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> QueryImplementor<R> setParameterList(String name, Collection<? extends P> values, AllowableParameterType<P> type);

	@Override
	QueryImplementor<R> setParameterList(String name, Object[] values);

	@Override
	<P> QueryImplementor<R> setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> QueryImplementor<R> setParameterList(String name, P[] values, AllowableParameterType<P> type);

	@Override
	QueryImplementor<R> setParameterList(int position, Collection values);

	@Override
	<P> QueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> QueryImplementor<R> setParameterList(int position, Collection<? extends P> values, AllowableParameterType<P> type);

	@Override
	QueryImplementor<R> setParameterList(int position, Object[] values);

	@Override
	<P> QueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> QueryImplementor<R> setParameterList(int position, P[] values, AllowableParameterType<P> type);

	@Override
	<P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, AllowableParameterType<P> type);

	@Override
	<P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> QueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, AllowableParameterType<P> type);

	@Override
	QueryImplementor<R> setProperties(Object bean);

	@Override
	QueryImplementor<R> setProperties(Map bean);
}
