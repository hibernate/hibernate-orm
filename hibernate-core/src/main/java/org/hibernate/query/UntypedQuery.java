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
import jakarta.persistence.TemporalType;

/**
 * Specialization of CommonQueryContract for untyped queries
 *
 * @author Steve Ebersole
 */
public interface UntypedQuery extends SelectionQuery {
	@Override
	UntypedQuery setParameter(String name, Object value);

	@Override
	<P> UntypedQuery setParameter(String name, P value, Class<P> type);

	@Override
	<P> UntypedQuery setParameter(String name, P value, BindableType<P> type);

	@Override
	UntypedQuery setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	UntypedQuery setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	UntypedQuery setParameter(String name, Date value, TemporalType temporalType);

	@Override
	UntypedQuery setParameter(int position, Object value);

	@Override
	<P> UntypedQuery setParameter(int position, P value, Class<P> type);

	@Override
	<P> UntypedQuery setParameter(int position, P value, BindableType<P> type);

	@Override
	UntypedQuery setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	UntypedQuery setParameter(int position, Date value, TemporalType temporalType);

	@Override
	UntypedQuery setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> UntypedQuery setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> UntypedQuery setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> UntypedQuery setParameter(QueryParameter<P> parameter, P val, BindableType<P> type);

	@Override
	<T> UntypedQuery setParameter(Parameter<T> param, T value);

	@Override
	UntypedQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	UntypedQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	UntypedQuery setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> UntypedQuery setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> UntypedQuery setParameterList(String name, Collection<? extends P> values, BindableType<P> type);

	@Override
	UntypedQuery setParameterList(String name, Object[] values);

	@Override
	<P> UntypedQuery setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> UntypedQuery setParameterList(String name, P[] values, BindableType<P> type);

	@Override
	UntypedQuery setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> UntypedQuery setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> UntypedQuery setParameterList(int position, Collection<? extends P> values, BindableType<P> type);

	@Override
	UntypedQuery setParameterList(int position, Object[] values);

	@Override
	<P> UntypedQuery setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> UntypedQuery setParameterList(int position, P[] values, BindableType<P> type);

	@Override
	<P> UntypedQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> UntypedQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> UntypedQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type);

	@Override
	<P> UntypedQuery setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> UntypedQuery setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> UntypedQuery setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type);

	@Override
	UntypedQuery setProperties(Object bean);

	@Override
	UntypedQuery setProperties(Map bean);

	@Override
	UntypedQuery setHibernateFlushMode(FlushMode flushMode);

	@Override
	UntypedQuery setCacheMode(CacheMode cacheMode);

	@Override
	UntypedQuery setCacheable(boolean cacheable);

	@Override
	UntypedQuery setCacheRegion(String cacheRegion);

	@Override
	UntypedQuery setTimeout(int timeout);

	@Override
	UntypedQuery setFetchSize(int fetchSize);

	@Override
	UntypedQuery setReadOnly(boolean readOnly);

	@Override
	UntypedQuery setMaxResults(int maxResult);

	@Override
	UntypedQuery setFirstResult(int startPosition);

	@Override
	UntypedQuery setHint(String hintName, Object value);

	FlushModeType getFlushMode();

	@Override
	UntypedQuery setFlushMode(FlushModeType flushMode);

	LockModeType getLockMode();

	@Override
	UntypedQuery setLockMode(LockModeType lockMode);
}
