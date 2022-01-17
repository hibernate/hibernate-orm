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
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.dialect.Dialect;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public interface SelectionQuery extends CommonQueryContract {
	/**
	 * Execute the query and return the query results as a {@link List}.
	 * If the query contains multiple items in the selection list, then
	 * by default each result in the list is packaged in an array of type
	 * {@code Object[]}.
	 *
	 * @return the result list
	 */
	List list();

	/**
	 * Execute the query and return the query results as a {@link List}.
	 * If the query contains multiple items in the selection list, then
	 * by default each result in the list is packaged in an array of type
	 * {@code Object[]}.
	 *
	 * @return the results as a list
	 */
	default List getResultList() {
		return list();
	}

	/**
	 * Returns scrollable access to the query results.
	 *
	 * This form calls {@link #scroll(ScrollMode)} using {@link Dialect#defaultScrollMode()}
	 *
	 * @apiNote The exact behavior of this method depends somewhat
	 * on the JDBC driver's {@link java.sql.ResultSet} scrolling support
	 */
	ScrollableResults scroll();

	/**
	 * Returns scrollable access to the query results.  The capabilities of the
	 * returned ScrollableResults depend on the specified ScrollMode.
	 *
	 * @apiNote The exact behavior of this method depends somewhat
	 * on the JDBC driver's {@link java.sql.ResultSet} scrolling support
	 */
	ScrollableResults scroll(ScrollMode scrollMode);

	/**
	 * Execute the query and return the query results as a {@link Stream}.
	 * If the query contains multiple items in the selection list, then
	 * by default each result in the stream is packaged in an array of type
	 * {@code Object[]}.
	 * <p>
	 * The client should call {@link Stream#close()} after processing the
	 * stream so that resources are freed as soon as possible.
	 *
	 * @return The results as a {@link Stream}
	 */
	default Stream getResultStream() {
		return stream();
	}

	/**
	 * Execute the query and return the query results as a {@link Stream}.
	 * If the query contains multiple items in the selection list, then
	 * by default each result in the stream is packaged in an array of type
	 * {@code Object[]}.
	 * <p>
	 * The client should call {@link Stream#close()} after processing the
	 * stream so that resources are freed as soon as possible.
	 *
	 * @return The results as a {@link Stream}
	 *
	 * @since 5.2
	 */
	default Stream stream() {
		return getResultStream();
	}

	/**
	 * Execute the query and return the single result of the query,
	 * or {@code null} if the query returns no results.
	 *
	 * @return the single result or {@code null}
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 */
	Object uniqueResult();

	/**
	 * Execute the query and return the single result of the query,
	 * throwing an exception if the query returns no results.
	 *
	 * @return the single result, only if there is exactly one
	 *
	 * @throws jakarta.persistence.NonUniqueResultException if there is more than one matching result
	 * @throws jakarta.persistence.NoResultException if there is no result to return
	 */
	Object getSingleResult();

	/**
	 * Execute the query and return the single result of the query,
	 * as an {@link Optional}.
	 *
	 * @return the single result as an {@code Optional}
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 */
	Optional uniqueResultOptional();

	LockOptions getLockOptions();

	SelectionQuery setMaxResults(int maxResult);

	SelectionQuery setFirstResult(int startPosition);

	SelectionQuery setHint(String hintName, Object value);

	SelectionQuery setFlushMode(FlushModeType flushMode);

	SelectionQuery setLockMode(LockModeType lockMode);

	@Override
	SelectionQuery setParameter(String name, Object value);

	@Override
	<P> SelectionQuery setParameter(String name, P value, Class<P> type);

	@Override
	<P> SelectionQuery setParameter(String name, P value, BindableType<P> type);

	@Override
	SelectionQuery setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	SelectionQuery setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	SelectionQuery setParameter(String name, Date value, TemporalType temporalType);

	@Override
	SelectionQuery setParameter(int position, Object value);

	@Override
	<P> SelectionQuery setParameter(int position, P value, Class<P> type);

	@Override
	<P> SelectionQuery setParameter(int position, P value, BindableType<P> type);

	@Override
	SelectionQuery setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	SelectionQuery setParameter(int position, Date value, TemporalType temporalType);

	@Override
	SelectionQuery setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> SelectionQuery setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> SelectionQuery setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> SelectionQuery setParameter(QueryParameter<P> parameter, P val, BindableType<P> type);

	@Override
	<T> SelectionQuery setParameter(Parameter<T> param, T value);

	@Override
	SelectionQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	SelectionQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	SelectionQuery setParameterList(String name, Collection values);

	@Override
	<P> SelectionQuery setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SelectionQuery setParameterList(String name, Collection<? extends P> values, BindableType<P> type);

	@Override
	SelectionQuery setParameterList(String name, Object[] values);

	@Override
	<P> SelectionQuery setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> SelectionQuery setParameterList(String name, P[] values, BindableType<P> type);

	@Override
	SelectionQuery setParameterList(int position, Collection values);

	@Override
	<P> SelectionQuery setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SelectionQuery setParameterList(int position, Collection<? extends P> values, BindableType<P> type);

	@Override
	SelectionQuery setParameterList(int position, Object[] values);

	@Override
	<P> SelectionQuery setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> SelectionQuery setParameterList(int position, P[] values, BindableType<P> type);

	@Override
	<P> SelectionQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> SelectionQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SelectionQuery setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type);

	@Override
	<P> SelectionQuery setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> SelectionQuery setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> SelectionQuery setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type);

	@Override
	SelectionQuery setProperties(Object bean);

	@Override
	SelectionQuery setProperties(Map bean);

	@Override
	SelectionQuery setHibernateFlushMode(FlushMode flushMode);

	@Override
	SelectionQuery setCacheMode(CacheMode cacheMode);

	@Override
	SelectionQuery setCacheable(boolean cacheable);

	@Override
	SelectionQuery setCacheRegion(String cacheRegion);

	@Override
	SelectionQuery setTimeout(int timeout);

	@Override
	SelectionQuery setFetchSize(int fetchSize);

	@Override
	SelectionQuery setReadOnly(boolean readOnly);
}
