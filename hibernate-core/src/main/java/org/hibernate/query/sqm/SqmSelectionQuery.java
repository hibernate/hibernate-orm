/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.query.BindableType;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.spi.SqmQuery;

import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
public interface SqmSelectionQuery extends SqmQuery, SelectionQuery {

	@Override
	SqmSelectionQuery setParameter(String name, Object value);

	@Override
	<P> SqmSelectionQuery setParameter(String name, P value, Class<P> type);

	@Override
	<P> SqmSelectionQuery setParameter(String name, P value, BindableType<P> type);

	@Override
	SqmSelectionQuery setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	SqmSelectionQuery setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	SqmSelectionQuery setParameter(String name, Date value, TemporalType temporalType);

	@Override
	SqmSelectionQuery setParameter(int position, Object value);

	@Override
	<P> SqmSelectionQuery setParameter(int position, P value, Class<P> type);

	@Override
	<P> SqmSelectionQuery setParameter(int position, P value, BindableType<P> type);

	@Override
	SqmSelectionQuery setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	SqmSelectionQuery setParameter(int position, Date value, TemporalType temporalType);

	@Override
	SqmSelectionQuery setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> SqmSelectionQuery setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> SqmSelectionQuery setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> SqmSelectionQuery setParameter(QueryParameter<P> parameter, P val, BindableType<P> type);

	@Override
	<T> SqmSelectionQuery setParameter(Parameter<T> param, T value);

	@Override
	SqmSelectionQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	SqmSelectionQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	SqmSelectionQuery setParameterList(String name, Collection values);

	@Override
	<P> SqmSelectionQuery setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery setParameterList(String name, Collection<? extends P> values, BindableType<P> type);

	@Override
	SqmSelectionQuery setParameterList(String name, Object[] values);

	@Override
	<P> SqmSelectionQuery setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery setParameterList(String name, P[] values, BindableType<P> type);

	@Override
	SqmSelectionQuery setParameterList(int position, Collection values);

	@Override
	<P> SqmSelectionQuery setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery setParameterList(int position, Collection<? extends P> values, BindableType<P> type);

	@Override
	SqmSelectionQuery setParameterList(int position, Object[] values);

	@Override
	<P> SqmSelectionQuery setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery setParameterList(int position, P[] values, BindableType<P> type);

	@Override
	<P> SqmSelectionQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> SqmSelectionQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type);

	@Override
	<P> SqmSelectionQuery setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> SqmSelectionQuery setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type);

	@Override
	SqmSelectionQuery setProperties(Object bean);

	@Override
	SqmSelectionQuery setProperties(Map bean);

	@Override
	SqmSelectionQuery setHibernateFlushMode(FlushMode flushMode);

	@Override
	SqmSelectionQuery setCacheMode(CacheMode cacheMode);

	@Override
	SqmSelectionQuery setCacheable(boolean cacheable);

	@Override
	SqmSelectionQuery setCacheRegion(String cacheRegion);

	@Override
	SqmSelectionQuery setTimeout(int timeout);

	@Override
	SqmSelectionQuery setFetchSize(int fetchSize);

	@Override
	SqmSelectionQuery setReadOnly(boolean readOnly);
}
