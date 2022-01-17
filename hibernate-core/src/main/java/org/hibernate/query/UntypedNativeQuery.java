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

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
public interface UntypedNativeQuery extends UntypedQuery {
	@Override
	UntypedNativeQuery setParameter(String name, Object value);

	@Override
	<P> UntypedNativeQuery setParameter(String name, P value, Class<P> type);

	@Override
	<P> UntypedNativeQuery setParameter(String name, P value, BindableType<P> type);

	@Override
	UntypedNativeQuery setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	UntypedNativeQuery setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	UntypedNativeQuery setParameter(String name, Date value, TemporalType temporalType);

	@Override
	UntypedNativeQuery setParameter(int position, Object value);

	@Override
	<P> UntypedNativeQuery setParameter(int position, P value, Class<P> type);

	@Override
	<P> UntypedNativeQuery setParameter(int position, P value, BindableType<P> type);

	@Override
	UntypedNativeQuery setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	UntypedNativeQuery setParameter(int position, Date value, TemporalType temporalType);

	@Override
	UntypedNativeQuery setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> UntypedNativeQuery setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> UntypedNativeQuery setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> UntypedNativeQuery setParameter(QueryParameter<P> parameter, P val, BindableType<P> type);

	@Override
	<T> UntypedNativeQuery setParameter(Parameter<T> param, T value);

	@Override
	UntypedNativeQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	UntypedNativeQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	UntypedNativeQuery setParameterList(String name, Collection values);

	@Override
	<P> UntypedNativeQuery setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> UntypedNativeQuery setParameterList(String name, Collection<? extends P> values, BindableType<P> type);

	@Override
	UntypedNativeQuery setParameterList(String name, Object[] values);

	@Override
	<P> UntypedNativeQuery setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> UntypedNativeQuery setParameterList(String name, P[] values, BindableType<P> type);

	@Override
	UntypedNativeQuery setParameterList(int position, Collection values);

	@Override
	<P> UntypedNativeQuery setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> UntypedNativeQuery setParameterList(int position, Collection<? extends P> values, BindableType<P> type);

	@Override
	UntypedNativeQuery setParameterList(int position, Object[] values);

	@Override
	<P> UntypedNativeQuery setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> UntypedNativeQuery setParameterList(int position, P[] values, BindableType<P> type);

	@Override
	<P> UntypedNativeQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> UntypedNativeQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> UntypedNativeQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type);

	@Override
	<P> UntypedNativeQuery setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> UntypedNativeQuery setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> UntypedNativeQuery setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type);

	@Override
	UntypedNativeQuery setProperties(Object bean);

	@Override
	UntypedNativeQuery setProperties(Map bean);

	@Override
	UntypedNativeQuery setHibernateFlushMode(FlushMode flushMode);

	@Override
	UntypedNativeQuery setCacheMode(CacheMode cacheMode);

	@Override
	UntypedNativeQuery setCacheable(boolean cacheable);

	@Override
	UntypedNativeQuery setCacheRegion(String cacheRegion);

	@Override
	UntypedNativeQuery setTimeout(int timeout);

	@Override
	UntypedNativeQuery setFetchSize(int fetchSize);

	@Override
	UntypedNativeQuery setReadOnly(boolean readOnly);

	@Override
	UntypedNativeQuery setMaxResults(int maxResult);

	@Override
	UntypedNativeQuery setFirstResult(int startPosition);

	@Override
	UntypedNativeQuery setHint(String hintName, Object value);

	@Override
	UntypedNativeQuery setFlushMode(FlushModeType flushMode);

	@Override
	UntypedNativeQuery setLockMode(LockModeType lockMode);
}
