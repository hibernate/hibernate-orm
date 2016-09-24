/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import java.util.Iterator;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.QueryProducer;

/**
 * The internal contract for QueryProducer implementations.  Acts as the value passed to
 * produced queries and provides them with access to needed functionality.
 *
 * @author Steve Ebersole
 */
public interface QueryProducerImplementor extends QueryProducer {
	SessionFactoryImplementor getFactory();

	FlushMode getHibernateFlushMode();
	void setHibernateFlushMode(FlushMode effectiveFlushMode);

	CacheMode getCacheMode();
	void setCacheMode(CacheMode effectiveCacheMode);

	boolean isDefaultReadOnly();

	ExceptionConverter getExceptionConverter();

	boolean isTransactionInProgress();

	void checkOpen(boolean rollbackIfNot);

	/**
	 * This is the list function for a SQM-backed query (HQL, JPQL, Criteria)
	 *
	 * @param query The SqmBackedQuery giving access to needed execution information
	 *
	 * @param <R> The query result type
	 *
	 * @return The query result list
	 */
	<R> List<R> performList(SqmBackedQuery<R> query);

	/**
	 * This is the iterate function for a SQM-backed query (HQL, JPQL, Criteria)
	 *
	 * @param query The SqmBackedQuery giving access to needed execution information
	 *
	 * @param <R> The type of the individual query results
	 *
	 * @return The query result Iterator
	 */
	<R> Iterator<R> performIterate(SqmBackedQuery<R> query);

	/**
	 * This is the scroll function for a SQM-backed query (HQL, JPQL, Criteria)
	 *
	 * @param query The SqmBackedQuery giving access to needed execution information
	 *
	 * @return The ScrollableResults over the query results
	 */
	ScrollableResultsImplementor performScroll(SqmBackedQuery query, ScrollMode scrollMode);

	/**
	 * This is the DML execution function for a SQM-backed query (HQL, JPQL, Criteria)
	 *
	 * @param query The SqmBackedQuery giving access to needed execution information
	 *
	 * @return The ScrollableResults over the query results
	 */
	int executeUpdate(SqmBackedQuery query);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides

	@Override
	QueryImplementor createQuery(String queryString);

	@Override
	<R> QueryImplementor<R> createQuery(String queryString, Class<R> resultClass);

	@Override
	NativeQueryImplementor createNativeQuery(String sqlString);

	@Override
	NativeQueryImplementor createNativeQuery(String sqlString, Class resultClass);

	@Override
	NativeQueryImplementor createNativeQuery(String sqlString, String resultSetMapping);

	@Override
	QueryImplementor getNamedQuery(String queryName);

	@Override
	QueryImplementor createNamedQuery(String name);

	@Override
	<R> QueryImplementor<R> createNamedQuery(String name, Class<R> resultClass);

	@Override
	NativeQueryImplementor getNamedNativeQuery(String name);

}
