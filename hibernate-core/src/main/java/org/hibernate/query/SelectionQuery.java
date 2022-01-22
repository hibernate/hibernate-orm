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
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

/**
 * Models a selection query returning results.  It is a slimmed down version
 * of {@link Query}, but providing only methods relevant to selection queries.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SelectionQuery extends CommonQueryContract {
	/**
	 * Execute the query and return the query results as a {@link List}.
	 * If the query contains multiple items in the selection list, then
	 * by default each result in the list is packaged in an array of type
	 * {@code Object[]}.
	 *
	 * @return the result list
	 */
	List<?> list();

	/**
	 * Execute the query and return the query results as a {@link List}.
	 * If the query contains multiple items in the selection list, then
	 * by default each result in the list is packaged in an array of type
	 * {@code Object[]}.
	 *
	 * @return the results as a list
	 */
	default List<?> getResultList() {
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
	ScrollableResults<?> scroll();

	/**
	 * Returns scrollable access to the query results.  The capabilities of the
	 * returned ScrollableResults depend on the specified ScrollMode.
	 *
	 * @apiNote The exact behavior of this method depends somewhat
	 * on the JDBC driver's {@link java.sql.ResultSet} scrolling support
	 */
	ScrollableResults<?> scroll(ScrollMode scrollMode);

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
	default Stream<?> getResultStream() {
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
	default Stream<?> stream() {
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
	Optional<?> uniqueResultOptional();

	SelectionQuery setHint(String hintName, Object value);

	@Override
	SelectionQuery setFlushMode(FlushModeType flushMode);

	@Override
	SelectionQuery setHibernateFlushMode(FlushMode flushMode);

	@Override
	SelectionQuery setTimeout(int timeout);

	/**
	 * Obtain the JDBC fetch size hint in effect for this query.  This value is eventually passed along to the JDBC
	 * query via {@link java.sql.Statement#setFetchSize(int)}.  As defined b y JDBC, this value is a hint to the
	 * driver to indicate how many rows to fetch from the database when more rows are needed.
	 *
	 * NOTE : JDBC expressly defines this value as a hint.  It may or may not have any effect on the actual
	 * query execution and ResultSet processing depending on the driver.
	 *
	 * @return The timeout <b>in seconds</b>
	 *
	 * @see java.sql.Statement#getFetchSize()
	 * @see java.sql.Statement#setFetchSize(int)
	 */
	Integer getFetchSize();

	/**
	 * Sets a JDBC fetch size hint for the query.
	 *
	 * @param fetchSize the fetch size hint
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getFetchSize()
	 */
	SelectionQuery setFetchSize(int fetchSize);

	/**
	 * Should entities and proxies loaded by this Query be put in read-only mode? If the
	 * read-only/modifiable setting was not initialized, then the default
	 * read-only/modifiable setting for the persistence context is returned instead.
	 *
	 * @see #setReadOnly(boolean)
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 *
	 * The read-only/modifiable setting has no impact on entities/proxies returned by the
	 * query that existed in the session beforeQuery the query was executed.
	 *
	 * @return {@code true} if the entities and proxies loaded by the query will be put
	 * in read-only mode; {@code false} otherwise (they will be modifiable)
	 */
	boolean isReadOnly();

	/**
	 * Set the read-only/modifiable mode for entities and proxies
	 * loaded by this Query. This setting overrides the default setting
	 * for the persistence context.
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 *
	 * To set the default read-only/modifiable setting used for
	 * entities and proxies that are loaded into the session:
	 * @see org.hibernate.engine.spi.PersistenceContext#setDefaultReadOnly(boolean)
	 * @see Session#setDefaultReadOnly(boolean)
	 *
	 * Read-only entities are not dirty-checked and snapshots of persistent
	 * state are not maintained. Read-only entities can be modified, but
	 * changes are not persisted.
	 *
	 * When a proxy is initialized, the loaded entity will have the same
	 * read-only/modifiable setting as the uninitialized
	 * proxy has, regardless of the session's current setting.
	 *
	 * The read-only/modifiable setting has no impact on entities/proxies
	 * returned by the query that existed in the session beforeQuery the query was executed.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @param readOnly {@code true} indicates that entities and proxies loaded by the query
	 * are to be put in read-only mode; {@code false} indicates that entities and proxies
	 * loaded by the query will be put in modifiable mode
	 */
	SelectionQuery setReadOnly(boolean readOnly);

	/**
	 * The max number of rows requested for the query results
	 */
	int getMaxResults();

	/**
	 * Set the max number of rows requested for the query results.  Applied
	 * to the SQL query
	 */
	SelectionQuery setMaxResults(int maxResult);

	/**
	 * The first row position to return from the query results.  Applied
	 * to the SQL query
	 */
	int getFirstResult();

	/**
	 * Set the first row position to return from the query results.  Applied
	 * to the SQL query
	 */
	SelectionQuery setFirstResult(int startPosition);

	/**
	 * Obtain the CacheMode in effect for this query.  By default, the query
	 * inherits the CacheMode of the Session from which is originates.
	 * <p/>
	 * NOTE: The CacheMode here describes reading-from/writing-to the
	 * entity/collection caches as we process query results.  For caching of
	 * the actual query results, see {@link #isCacheable()} and
	 * {@link #getCacheRegion()}
	 * <p/>
	 * In order for this setting to have any affect, second-level caching would
	 * have to be enabled and the entities/collections in question configured
	 * for caching.
	 *
	 * @see Session#getCacheMode()
	 */
	CacheMode getCacheMode();

	/**
	 * Set the current CacheMode in effect for this query.
	 *
	 * @implNote Setting to {@code null} ultimately indicates to use the CacheMode of the Session
	 *
	 * @see #getCacheMode()
	 * @see Session#setCacheMode
	 */
	SelectionQuery setCacheMode(CacheMode cacheMode);

	/**
	 * Should the results of the query be stored in the second level cache?
	 * <p/>
	 * This is different than second level caching of any returned entities and collections, which
	 * is controlled by {@link #getCacheMode()}.
	 * <p/>
	 * NOTE: the query being "eligible" for caching does not necessarily mean its results will be cached.  Second level
	 * query caching still has to be enabled on the {@link SessionFactory} for this to happen.  Usually that is
	 * controlled by the {@code hibernate.cache.use_query_cache} configuration setting.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_QUERY_CACHE
	 */
	boolean isCacheable();

	/**
	 * Enable/disable second level query (result) caching for this query.
	 *
	 * @see #isCacheable
	 */
	SelectionQuery setCacheable(boolean cacheable);

	/**
	 * Obtain the name of the second level query cache region in which query results will be stored (if they are
	 * cached, see the discussion on {@link #isCacheable()} for more information).  {@code null} indicates that the
	 * default region should be used.
	 */
	String getCacheRegion();

	/**
	 * Set the name of the cache region where query results should be cached
	 * (assuming {@link #isCacheable}).  {@code null} indicates to use the default region.
	 *
	 * @see #getCacheRegion()
	 */
	SelectionQuery setCacheRegion(String cacheRegion);

	/**
	 * The LockOptions currently in effect for the query
	 */
	LockOptions getLockOptions();

	/**
	 * Specify the root LockModeType for the query
	 *
	 * @see #setHibernateLockMode
	 */
	SelectionQuery setLockMode(LockModeType lockMode);

	/**
	 * Specify the root LockMode for the query
	 */
	SelectionQuery setHibernateLockMode(LockMode lockMode);

	/**
	 * Specify a LockMode to apply to a specific alias defined in the query
	 */
	SelectionQuery setAliasSpecificLockMode(String alias, LockMode lockMode);

	/**
	 * Specifies whether follow-on locking should be applied?
	 */
	SelectionQuery setFollowOnLocking(boolean enable);

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
	SelectionQuery setProperties(@SuppressWarnings("rawtypes") Map bean);
}
