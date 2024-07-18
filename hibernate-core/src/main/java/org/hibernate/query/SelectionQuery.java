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

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Remove;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.UnknownProfileException;
import org.hibernate.dialect.Dialect;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import org.hibernate.engine.profile.DefaultFetchProfile;
import org.hibernate.graph.GraphSemantic;

/**
 * Within the context of an active {@linkplain org.hibernate.Session session},
 * an instance of this type represents an executable selection query, that is,
 * a {@code select}. It is a slimmed-down version of {@link Query}, providing
 * only methods relevant to selection queries.
 * <p>
 * A {@code SelectionQuery} may be obtained from the {@link org.hibernate.Session}
 * by calling:
 * <ul>
 * <li>{@link QueryProducer#createSelectionQuery(String, Class)}, passing the
 *     HQL as a string,
 * <li>{@link QueryProducer#createSelectionQuery(jakarta.persistence.criteria.CriteriaQuery)},
 *     passing a {@linkplain jakarta.persistence.criteria.CriteriaQuery criteria
 *     query object}, or
 * <li>{@link QueryProducer#createNamedSelectionQuery(String, Class)} passing
 *     the name of a query defined using {@link jakarta.persistence.NamedQuery}
 *     or {@link jakarta.persistence.NamedNativeQuery}.
 * </ul>
 * <p>
 * A {@code SelectionQuery} controls how a query is executed, and allows arguments
 * to be bound to its parameters.
 * <ul>
 * <li>Selection queries are usually executed using {@link #getResultList()} or
 *     {@link #getSingleResult()}.
 * <li>The methods {@link #setMaxResults(int)} and {@link #setFirstResult(int)}
 *     control limits and pagination.
 * <li>The various overloads of {@link #setParameter(String, Object)} and
 *     {@link #setParameter(int, Object)} allow arguments to be bound to named
 *     and ordinal parameters defined by the query.
 * </ul>
 * <p>
 * A query may have explicit <em>fetch joins</em>, specified using the syntax
 * {@code join fetch} in HQL, or via {@link jakarta.persistence.criteria.From#fetch}
 * in the criteria API. Additional fetch joins may be added by:
 * <ul>
 * <li>setting an {@link EntityGraph} by calling
 *     {@link #setEntityGraph(EntityGraph, GraphSemantic)}, or
 * <li>enabling a fetch profile, using {@link Session#enableFetchProfile(String)}.
 * </ul>
 * <p>
 * The special built-in fetch profile named
 * {@value DefaultFetchProfile#HIBERNATE_DEFAULT_PROFILE} adds a fetch join for
 * every {@link jakarta.persistence.FetchType#EAGER eager} {@code @ManyToOne} or
 * {@code @OneToOne} association belonging to an entity returned by the query.
 * <p>
 * Finally, two alternative approaches to pagination are available:
 * <ol>
 * <li>
 * The operations and {@link #setOrder(List)} and {@link #setPage(Page)}, together
 * with {@link Order} and {@link Page}, provide a streamlined API for offset-based
 * pagination, at a slightly higher semantic level than the ancient but dependable
 * {@link #setFirstResult(int)} and {@link #setMaxResults(int)}.
 * <li>
 * On the other hand, {@link KeyedPage} and {@link KeyedResultList}, along with
 * {@link #getKeyedResultList(KeyedPage)}, provide for <em>key-based pagination</em>,
 * which can help eliminate missed or duplicate results when data is modified
 * between page requests.
 * </ol>
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SelectionQuery<R> extends CommonQueryContract {
	/**
	 * Execute the query and return the query results as a {@link List}.
	 * If the query contains multiple items in the selection list, then
	 * by default each result in the list is packaged in an array of type
	 * {@code Object[]}.
	 *
	 * @return the result list
	 */
	List<R> list();

	/**
	 * Execute the query and return the query results as a {@link List}.
	 * If the query contains multiple items in the selection list, then
	 * by default each result in the list is packaged in an array of type
	 * {@code Object[]}.
	 *
	 * @return the results as a list
	 */
	default List<R> getResultList() {
		return list();
	}

	/**
	 * Returns scrollable access to the query results.
	 * <p>
	 * This form calls {@link #scroll(ScrollMode)} using {@link Dialect#defaultScrollMode()}
	 *
	 * @apiNote The exact behavior of this method depends somewhat
	 * on the JDBC driver's {@link java.sql.ResultSet} scrolling support
	 */
	ScrollableResults<R> scroll();

	/**
	 * Returns scrollable access to the query results.  The capabilities of the
	 * returned ScrollableResults depend on the specified ScrollMode.
	 *
	 * @apiNote The exact behavior of this method depends somewhat
	 * on the JDBC driver's {@link java.sql.ResultSet} scrolling support
	 */
	ScrollableResults<R> scroll(ScrollMode scrollMode);

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
	 * @implNote The default implementation defined here simply returns
	 *           {@link #list()}{@code .stream()}. Concrete implementations
	 *           may be more efficient.
	 */
	default Stream<R> getResultStream() {
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
	default Stream<R> stream() {
		return list().stream();
	}

	/**
	 * Execute the query and return the single result of the query,
	 * or {@code null} if the query returns no results.
	 *
	 * @return the single result or {@code null}
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 */
	R uniqueResult();

	/**
	 * Execute the query and return the single result of the query,
	 * throwing an exception if the query returns no results.
	 *
	 * @return the single result, only if there is exactly one
	 *
	 * @throws jakarta.persistence.NonUniqueResultException if there is more than one matching result
	 * @throws jakarta.persistence.NoResultException if there is no result to return
	 */
	R getSingleResult();

	/**
	 * Execute the query and return the single result of the query,
	 * or {@code null} if the query returns no results.
	 *
	 * @return the single result or {@code null} if there is no result to return
	 *
	 * @throws jakarta.persistence.NonUniqueResultException if there is more than one matching result
	 *
	 * @since 6.0
	 */
	R getSingleResultOrNull();

	/**
	 * Execute the query and return the single result of the query,
	 * as an {@link Optional}.
	 *
	 * @return the single result as an {@code Optional}
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 */
	Optional<R> uniqueResultOptional();

	/**
	 * Determine the size of the query result list that would be
	 * returned by calling {@link #getResultList()} with no
	 * {@linkplain #getFirstResult() offset} or
	 * {@linkplain #getMaxResults() limit} applied to the query.
	 *
	 * @return the size of the list that would be returned
	 *
	 * @since 6.5
	 */
	@Incubating
	long getResultCount();

	/**
	 * Execute the query and return the results for the given
	 * {@linkplain KeyedPage page}, using key-based pagination.
	 *
	 * @param page the key-based specification of the page as
	 *        an instance of {@link KeyedPage}
	 *
	 * @return the query results and the key of the next page
	 *         as an instance of {@link KeyedResultList}
	 *
	 * @since 6.5
	 *
	 * @see KeyedPage
	 * @see KeyedResultList
	 */
	@Incubating
	KeyedResultList<R> getKeyedResultList(KeyedPage<R> page);

	SelectionQuery<R> setHint(String hintName, Object value);

	/**
	 * Apply an {@link EntityGraph} to the query.
	 * <p>
	 * This is an alternative way to specify the associations which
	 * should be fetched as part of the initial query.
	 *
	 * @since 6.3
	 */
	SelectionQuery<R> setEntityGraph(EntityGraph<R> graph, GraphSemantic semantic);

	/**
	 * Enable the {@link org.hibernate.annotations.FetchProfile fetch profile}
	 * for this query. If the requested fetch profile is already enabled,
	 * the call has no effect.
	 * <p>
	 * This is an alternative way to specify the associations which
	 * should be fetched as part of the initial query.
	 *
	 * @param profileName the name of the fetch profile to be enabled
	 *
	 * @throws UnknownProfileException Indicates that the given name does not
	 *                                 match any known fetch profile names
	 *
	 * @see org.hibernate.annotations.FetchProfile
	 */
	SelectionQuery<R> enableFetchProfile(String profileName);

	/**
	 * Disable the {@link org.hibernate.annotations.FetchProfile fetch profile}
	 * with the given name in this session. If the requested fetch profile
	 * is not currently enabled, the call has no effect.
	 *
	 * @param profileName the name of the fetch profile to be disabled
	 *
	 * @throws UnknownProfileException Indicates that the given name does not
	 *                                 match any known fetch profile names
	 *
	 * @see org.hibernate.annotations.FetchProfile
	 */
	SelectionQuery<R> disableFetchProfile(String profileName);

	@Override
	SelectionQuery<R> setFlushMode(FlushModeType flushMode);

	@Override
	SelectionQuery<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	SelectionQuery<R> setTimeout(int timeout);

	@Override
	SelectionQuery<R> setComment(String comment);

	/**
	 * Obtain the JDBC fetch size hint in effect for this query. This value is eventually passed along to the JDBC
	 * query via {@link java.sql.Statement#setFetchSize(int)}. As defined by JDBC, this value is a hint to the
	 * driver to indicate how many rows to fetch from the database when more rows are needed.
	 *
	 * @implNote JDBC expressly defines this value as a hint. Depending on the driver, it may or may not have any
	 *           effect on the actual query execution and {@link java.sql.ResultSet} processing .
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
	SelectionQuery<R> setFetchSize(int fetchSize);

	/**
	 * Should entities and proxies loaded by this Query be put in read-only
	 * mode? If the read-only/modifiable setting was not initialized, then
	 * the default read-only/modifiable setting for the persistence context i
	 * s returned instead.
	 *
	 * @see #setReadOnly(boolean)
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 *
	 * The read-only/modifiable setting has no impact on entities/proxies
	 * returned by the query that existed in the session beforeQuery the
	 * query was executed.
	 *
	 * @return {@code true} if the entities and proxies loaded by the query
	 *         will be put in read-only mode; {@code false} otherwise
	 *         (they will be modifiable)
	 */
	boolean isReadOnly();

	/**
	 * Set the read-only/modifiable mode for entities and proxies loaded
	 *  by this {@code Query}. This setting overrides the default setting
	 * for the persistence context,
	 * {@link org.hibernate.Session#isDefaultReadOnly()}.
	 * <p>
	 * To set the default read-only/modifiable setting used for entities
	 * and proxies that are loaded into the session, use
	 * {@link Session#setDefaultReadOnly(boolean)}.
	 * <p>
	 * Read-only entities are not dirty-checked and snapshots of persistent
	 * state are not maintained. Read-only entities can be modified, but
	 * changes are not persisted.
	 * <p>
	 * When a proxy is initialized, the loaded entity will have the same
	 * read-only/modifiable setting as the uninitialized
	 * proxy has, regardless of the session's current setting.
	 * <p>
	 * The read-only/modifiable setting has no impact on entities/proxies
	 * returned by the query that existed in the session beforeQuery the
	 * query was executed.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @param readOnly {@code true} indicates that entities and proxies
	 *                 loaded by the query are to be put in read-only mode;
	 *                 {@code false} indicates that entities and proxies
	 *                 loaded by the query will be put in modifiable mode
	 */
	SelectionQuery<R> setReadOnly(boolean readOnly);

	/**
	 * The max number of rows requested for the query results
	 */
	int getMaxResults();

	/**
	 * Set the max number of rows requested for the query results. Applied
	 * to the SQL query
	 */
	SelectionQuery<R> setMaxResults(int maxResult);

	/**
	 * The first row position to return from the query results. Applied
	 * to the SQL query.
	 */
	int getFirstResult();

	/**
	 * Set the first row position to return from the query results. Applied
	 * to the SQL query.
	 */
	SelectionQuery<R> setFirstResult(int startPosition);

	/**
	 * Set the {@linkplain Page page} of results to return.
	 *
	 * @see Page
	 *
	 * @since 6.3
	 */
	@Incubating
	SelectionQuery<R> setPage(Page page);

	/**
	 * Obtain the {@link CacheMode} in effect for this query. By default,
	 * the query inherits the {@link CacheMode} of the session from which
	 * it originates.
	 * <p>
	 * The {@link CacheMode} here describes reading-from/writing-to the
	 * entity/collection caches as we process query results. For caching
	 * of the actual query results, see {@link #isCacheable()} and
	 * {@link #getCacheRegion()}
	 * <p>
	 * In order for this setting to have any affect, second-level caching
	 * would have to be enabled and the entities/collections in question
	 * configured for caching.
	 *
	 * @see Session#getCacheMode()
	 */
	CacheMode getCacheMode();

	/**
	 * @see #getCacheMode()
	 *
	 * @since 6.2
	 */
	CacheStoreMode getCacheStoreMode();

	/**
	 * @see #getCacheMode()
	 *
	 * @since 6.2
	 */
	CacheRetrieveMode getCacheRetrieveMode();

	/**
	 * Set the current {@link CacheMode} in effect for this query.
	 *
	 * @implNote Setting it to {@code null} ultimately indicates to use the
	 *           {@code CacheMode} of the session.
	 *
	 * @see #getCacheMode()
	 * @see Session#setCacheMode(CacheMode)
	 */
	SelectionQuery<R> setCacheMode(CacheMode cacheMode);

	/**
	 * @see #setCacheMode(CacheMode)
	 *
	 * @since 6.2
	 */
	SelectionQuery<R> setCacheStoreMode(CacheStoreMode cacheStoreMode);

	/**
	 * @see #setCacheMode(CacheMode)
	 *
	 * @since 6.2
	 */
	SelectionQuery<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode);

	/**
	 * Should the results of the query be stored in the second level cache?
	 * <p>
	 * This is different to second level caching of any returned entities and
	 * collections, which is controlled by {@link #getCacheMode()}.
	 * <p>
	 * The query being "eligible" for caching does not necessarily mean its
	 * results will be cached. Second-level query caching still has to be
	 * enabled on the {@link SessionFactory} for this to happen. Usually that
	 * is controlled by the configuration setting
	 * {@value org.hibernate.cfg.AvailableSettings#USE_QUERY_CACHE}.
	 */
	boolean isCacheable();

	/**
	 * Enable/disable second level query (result) caching for this query.
	 *
	 * @see #isCacheable
	 */
	SelectionQuery<R> setCacheable(boolean cacheable);

	/**
	 * Should the query plan of the query be stored in the query plan cache?
	 */
	boolean isQueryPlanCacheable();

	/**
	 * Enable/disable query plan caching for this query.
	 *
	 * @see #isQueryPlanCacheable
	 */
	SelectionQuery<R> setQueryPlanCacheable(boolean queryPlanCacheable);

	/**
	 * Obtain the name of the second level query cache region in which query
	 * results will be stored (if they are cached, see the discussion on
	 * {@link #isCacheable()} for more information). {@code null} indicates
	 * that the default region should be used.
	 */
	String getCacheRegion();

	/**
	 * Set the name of the cache region where query results should be cached
	 * (assuming {@link #isCacheable}). {@code null} indicates to use the
	 * default region.
	 *
	 * @see #getCacheRegion()
	 */
	SelectionQuery<R> setCacheRegion(String cacheRegion);

	/**
	 * The {@link LockOptions} currently in effect for the query
	 */
	LockOptions getLockOptions();

	/**
	 * Get the root {@link LockModeType} for the query
	 *
	 * @see #getHibernateLockMode()
	 */
	LockModeType getLockMode();

	/**
	 * Specify the root {@link LockModeType} for the query
	 *
	 * @see #setHibernateLockMode
	 */
	SelectionQuery<R> setLockMode(LockModeType lockMode);

	/**
	 * Get the root {@link LockMode} for the query
	 *
	 * @see #getLockMode()
	 */
	LockMode getHibernateLockMode();

	/**
	 * Specify the root {@link LockMode} for the query
	 *
	 * @see #setLockMode(LockModeType)
	 */
	SelectionQuery<R> setHibernateLockMode(LockMode lockMode);

	/**
	 * Specify a {@link LockMode} to apply to a specific alias defined in the query
	 */
	SelectionQuery<R> setLockMode(String alias, LockMode lockMode);

	/**
	 * If the result type of this query is an entity class, add one or more
	 * {@linkplain Order rules} for ordering the query results.
	 *
	 * @param orderList one or more instances of {@link Order}
	 *
	 * @see Order
	 *
	 * @since 6.3
	 */
	@Incubating
	SelectionQuery<R> setOrder(List<Order<? super R>> orderList);

	/**
	 * If the result type of this query is an entity class, add a
	 * {@linkplain Order rule} for ordering the query results.
	 *
	 * @param order an instance of {@link Order}
	 *
	 * @see Order
	 *
	 * @since 6.3
	 */
	@Incubating
	SelectionQuery<R> setOrder(Order<? super R> order);

	/**
	 * Specify a {@link LockMode} to apply to a specific alias defined in the query
	 *
	 * @deprecated use {@link #setLockMode(String, LockMode)}
	 */
	@Deprecated(since = "6.2") @Remove
	SelectionQuery<R> setAliasSpecificLockMode(String alias, LockMode lockMode);

	/**
	 * Specifies whether follow-on locking should be applied
	 */
	SelectionQuery<R> setFollowOnLocking(boolean enable);

	@Override
	SelectionQuery<R> setParameter(String name, Object value);

	@Override
	<P> SelectionQuery<R> setParameter(String name, P value, Class<P> type);

	@Override
	<P> SelectionQuery<R> setParameter(String name, P value, BindableType<P> type);

	@Override
	SelectionQuery<R> setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	SelectionQuery<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	SelectionQuery<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	SelectionQuery<R> setParameter(int position, Object value);

	@Override
	<P> SelectionQuery<R> setParameter(int position, P value, Class<P> type);

	@Override
	<P> SelectionQuery<R> setParameter(int position, P value, BindableType<P> type);

	@Override
	SelectionQuery<R> setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	SelectionQuery<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override
	SelectionQuery<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> SelectionQuery<R> setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P val, BindableType<P> type);

	@Override
	<T> SelectionQuery<R> setParameter(Parameter<T> param, T value);

	@Override
	SelectionQuery<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	SelectionQuery<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	SelectionQuery<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SelectionQuery<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SelectionQuery<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type);

	@Override
	SelectionQuery<R> setParameterList(String name, Object[] values);

	@Override
	<P> SelectionQuery<R> setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> SelectionQuery<R> setParameterList(String name, P[] values, BindableType<P> type);

	@Override
	SelectionQuery<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SelectionQuery<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SelectionQuery<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type);

	@Override
	SelectionQuery<R> setParameterList(int position, Object[] values);

	@Override
	<P> SelectionQuery<R> setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> SelectionQuery<R> setParameterList(int position, P[] values, BindableType<P> type);

	@Override
	<P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type);

	@Override
	<P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type);

	@Override
	SelectionQuery<R> setProperties(Object bean);

	@Override
	SelectionQuery<R> setProperties(@SuppressWarnings("rawtypes") Map bean);
}
