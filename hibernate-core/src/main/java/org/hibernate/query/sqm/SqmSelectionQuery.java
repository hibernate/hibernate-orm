/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.query.BindableType;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.spi.SqmQuery;

import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
public interface SqmSelectionQuery<R> extends SqmQuery, SelectionQuery<R> {

	@Override
	SqmSelectionQuery<R> setParameter(String name, Object value);

	@Override
	<P> SqmSelectionQuery<R> setParameter(String name, P value, Class<P> type);

	@Override
	<P> SqmSelectionQuery<R> setParameter(String name, P value, BindableType<P> type);

	@Override
	SqmSelectionQuery<R> setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	SqmSelectionQuery<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	SqmSelectionQuery<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	SqmSelectionQuery<R> setParameter(int position, Object value);

	@Override
	<P> SqmSelectionQuery<R> setParameter(int position, P value, Class<P> type);

	@Override
	<P> SqmSelectionQuery<R> setParameter(int position, P value, BindableType<P> type);

	@Override
	SqmSelectionQuery<R> setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	SqmSelectionQuery<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override
	SqmSelectionQuery<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> SqmSelectionQuery<R> setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> SqmSelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> SqmSelectionQuery<R> setParameter(QueryParameter<P> parameter, P val, BindableType<P> type);

	@Override
	<T> SqmSelectionQuery<R> setParameter(Parameter<T> param, T value);

	@Override
	SqmSelectionQuery<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	SqmSelectionQuery<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	SqmSelectionQuery<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type);

	@Override
	SqmSelectionQuery<R> setParameterList(String name, Object[] values);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(String name, P[] values, BindableType<P> type);

	@Override
	SqmSelectionQuery<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type);

	@Override
	SqmSelectionQuery<R> setParameterList(int position, Object[] values);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(int position, P[] values, BindableType<P> type);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> SqmSelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type);

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
}
