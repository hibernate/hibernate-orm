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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.query.spi.QueryOptions;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

/**
 * Adaptation of {@link jakarta.persistence.Query} to the
 * {@link org.hibernate.query.Query} hierarchy
 *
 * @apiNote Jakarta Persistence defines its {@link jakarta.persistence.Query}
 * contract as modeling both selection and mutation queries,
 * {@link SelectionQuery} and {@link MutationQuery} respectively
 *
 * @author Steve Ebersole
 */
public interface JpaQuery extends SelectionQuery, MutationQuery, jakarta.persistence.Query {
	@Override
	default List<?> getResultList() {
		return SelectionQuery.super.getResultList();
	}

	@Override
	default Stream<?> getResultStream() {
		return SelectionQuery.super.getResultStream();
	}

	QueryOptions getQueryOptions();

	@Override
	JpaQuery setHibernateFlushMode(FlushMode flushMode);

	@Override
	JpaQuery setCacheMode(CacheMode cacheMode);

	@Override
	JpaQuery setCacheable(boolean cacheable);

	@Override
	JpaQuery setCacheRegion(String cacheRegion);

	@Override
	JpaQuery setTimeout(int timeout);

	@Override
	JpaQuery setFetchSize(int fetchSize);

	@Override
	JpaQuery setReadOnly(boolean readOnly);

	@Override
	JpaQuery setMaxResults(int maxResult);

	@Override
	JpaQuery setFirstResult(int startPosition);

	@Override
	JpaQuery setHint(String hintName, Object value);

	@Override
	JpaQuery setFlushMode(FlushModeType flushMode);

	@Override
	JpaQuery setLockMode(LockModeType lockMode);

	@Override
	JpaQuery setParameter(String name, Object value);

	@Override
	<P> JpaQuery setParameter(String name, P value, Class<P> type);

	@Override
	<P> JpaQuery setParameter(String name, P value, BindableType<P> type);

	@Override
	JpaQuery setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	JpaQuery setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	JpaQuery setParameter(String name, Date value, TemporalType temporalType);

	@Override
	JpaQuery setParameter(int position, Object value);

	@Override
	<P> JpaQuery setParameter(int position, P value, Class<P> type);

	@Override
	<P> JpaQuery setParameter(int position, P value, BindableType<P> type);

	@Override
	JpaQuery setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	JpaQuery setParameter(int position, Date value, TemporalType temporalType);

	@Override
	JpaQuery setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> JpaQuery setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> JpaQuery setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> JpaQuery setParameter(QueryParameter<P> parameter, P val, BindableType<P> type);

	@Override
	<T> JpaQuery setParameter(Parameter<T> param, T value);

	@Override
	JpaQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	JpaQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	JpaQuery setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> JpaQuery setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> JpaQuery setParameterList(String name, Collection<? extends P> values, BindableType<P> type);

	@Override
	JpaQuery setParameterList(String name, Object[] values);

	@Override
	<P> JpaQuery setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> JpaQuery setParameterList(String name, P[] values, BindableType<P> type);

	@Override
	JpaQuery setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> JpaQuery setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> JpaQuery setParameterList(int position, Collection<? extends P> values, BindableType<P> type);

	@Override
	JpaQuery setParameterList(int position, Object[] values);

	@Override
	<P> JpaQuery setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> JpaQuery setParameterList(int position, P[] values, BindableType<P> type);

	@Override
	<P> JpaQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> JpaQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> JpaQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type);

	@Override
	<P> JpaQuery setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> JpaQuery setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> JpaQuery setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type);

	@Override
	JpaQuery setProperties(Object bean);

	@Override
	JpaQuery setProperties(Map bean);
}
