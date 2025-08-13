/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.TemporalType;
import jakarta.persistence.metamodel.Type;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.UnknownProfileException;
import org.hibernate.graph.GraphSemantic;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
 * A query which returns multiple results should be executed via
 * {@link #getResultList()}:
 * <pre>
 * List&lt;Book&gt; books =
 *         session.createSelectionQuery("from Book left join fetch authors where title like :title")
 *                 .setParameter("title", title)
 *                 .setMaxResults(50)
 *                 .getResultList();
 * </pre>
 * A query which is expected to return exactly one on result should be executed
 * via {@link #getSingleResult()}, or, if it might not return a result,
 * {@link #getSingleResultOrNull()}:
 * <pre>
 * Book book =
 *         session.createSelectionQuery("from Book where isbn = ?1")
 *                 .setParameter(1, isbn)
 *                 .getSingleResultOrNull();
 * </pre>
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
 * {@value org.hibernate.engine.profile.DefaultFetchProfile#HIBERNATE_DEFAULT_PROFILE}
 * adds a fetch join for every {@link jakarta.persistence.FetchType#EAGER eager}
 * {@code @ManyToOne} or {@code @OneToOne} association belonging to an entity
 * returned by the query.
 * <p>
 * Finally, three alternative approaches to pagination are available:
 * <ol>
 * <li>
 * The ancient but dependable operations {@link #setFirstResult(int)} and
 * {@link #setMaxResults(int)} are the standard approach blessed by the JPA
 * specification.
 * <li>
 * {@link org.hibernate.query.specification.SelectionSpecification SelectionSpecification}
 * and {@link #setPage(Page)}, together with {@link Order} and {@link Page}, provide
 * a streamlined API for offset-based pagination, at a slightly higher semantic level.
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
	 *
	 * @apiNote Synonym for {@link #list()}
	 */
	default List<R> getResultList() {
		return list();
	}

	/**
	 * Returns scrollable access to the query results, using the
	 * {@linkplain org.hibernate.dialect.Dialect#defaultScrollMode
	 * default scroll mode of the SQL dialect.}
	 *
	 * @see #scroll(ScrollMode)
	 */
	ScrollableResults<R> scroll();

	/**
	 * Returns scrollable access to the query results. The capabilities
	 * of the returned {@link ScrollableResults} depend on the specified
	 * {@link ScrollMode}.
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
	 * @apiNote Synonym for {@link #stream()}
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
	 * @implNote The default implementation defined here simply returns
	 *           {@link #list()}{@link List#stream() .stream()}.
	 *           Overriding implementations are typically more efficient.
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

	@Override
	SelectionQuery<R> setHint(String hintName, Object value);

	/**
	 * Apply an {@link EntityGraph} to the query.
	 * <p>
	 * This is an alternative way to specify the associations which
	 * should be fetched as part of the initial query.
	 *
	 * @since 6.3
	 */
	SelectionQuery<R> setEntityGraph(EntityGraph<? super R> graph, GraphSemantic semantic);

	/**
	 * Enable the {@linkplain org.hibernate.annotations.FetchProfile fetch
	 * profile} with the given name during execution of this query. If the
	 * requested fetch profile is already enabled, the call has no effect.
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
	 * Disable the {@linkplain org.hibernate.annotations.FetchProfile fetch
	 * profile} with the given name in this session. If the fetch profile is
	 * not currently enabled, the call has no effect.
	 *
	 * @param profileName the name of the fetch profile to be disabled
	 *
	 * @throws UnknownProfileException Indicates that the given name does not
	 *                                 match any known fetch profile names
	 *
	 * @see org.hibernate.annotations.FetchProfile
	 */
	SelectionQuery<R> disableFetchProfile(String profileName);

	@Override @Deprecated(since = "7")
	SelectionQuery<R> setFlushMode(FlushModeType flushMode);

	@Override @Deprecated(since = "7")
	SelectionQuery<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	SelectionQuery<R> setQueryFlushMode(QueryFlushMode queryFlushMode);

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
	 * In some ways, Hibernate treats read-only entities the same as
	 * entities that are not read-only; for example, it cascades
	 * operations to associations as defined in the entity mapping.
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
	 * The maximum number of query result rows to return.
	 *
	 * @return the maximum length of the query result list
	 */
	int getMaxResults();

	/**
	 * Set the maximum number of query result rows to return.
	 *
	 * @param maxResults the maximum length of the query result list
	 */
	SelectionQuery<R> setMaxResults(int maxResults);

	/**
	 * The first query result row to return. The very first row
	 * of the query result list is considered the zeroth row.
	 *
	 * @return the position of the first row to return,
	 *         indexed from zero
	 */
	int getFirstResult();

	/**
	 * Set the first query result row to return. The very first
	 * row of the query result list is considered the zeroth row.
	 *
	 * @param startPosition the position of the first row to return,
	 *                      indexed from zero
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
	 * The {@link CacheMode} here affects the use of entity and collection
	 * caches as the query result set is processed. For caching of the actual
	 * query results, use {@link #isCacheable()} and {@link #getCacheRegion()}.
	 * <p>
	 * In order for this setting to have any affect, second-level caching
	 * must be enabled and the entities and collections must be eligible
	 * for storage in the second-level cache.
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
	 * <p>
	 * Set it to {@code null} to indicate that the {@code CacheMode}
	 * of the {@link Session#getCacheMode() session} should be used.
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
	 *
	 * @deprecated Since {@link LockOptions} is transitioning to
	 *             a new role as an SPI.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
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
	 * Apply a scope to any pessimistic locking applied to the query.
	 *
	 * @param lockScope The lock scope to apply
	 *
	 * @return {@code this}, for method chaining
	 */
	SelectionQuery<R> setLockScope(Locking.Scope lockScope);

	/**
	 * Apply a scope to any pessimistic locking applied to the query.
	 *
	 * @param lockScope The lock scope to apply
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@linkplain #setLockScope(Locking.Scope)} instead.
	 */
	@Deprecated(since = "7.1")
	SelectionQuery<R> setLockScope(PessimisticLockScope lockScope);

	/**
	 * Specify a {@link LockMode} to apply to a specific alias defined in the query
	 *
	 * @see #setHibernateLockMode
	 * @see #setLockScope(Locking.Scope)
	 *
	 * @deprecated Use {@linkplain #setLockScope(Locking.Scope)} instead.
	 */
	@Deprecated(since = "7")
	SelectionQuery<R> setLockMode(String alias, LockMode lockMode);

	/**
	 * Specifies whether follow-on locking should be applied
	 */
	SelectionQuery<R> setFollowOnStrategy(Locking.FollowOn followOnStrategy);

	/**
	 * Specifies whether follow-on locking should be applied
	 *
	 * @deprecated Use {@linkplain #setFollowOnStrategy(Locking.FollowOn)} instead
	 */
	@Deprecated(since = "7.1")
	SelectionQuery<R> setFollowOnLocking(boolean enable);

	/**
	 * Set a {@link TupleTransformer}.
	 */
	<T> SelectionQuery<T> setTupleTransformer(TupleTransformer<T> transformer);

	/**
	 * Set a {@link ResultListTransformer}.
	 */
	SelectionQuery<R> setResultListTransformer(ResultListTransformer<R> transformer);

	@Override
	SelectionQuery<R> setParameter(String name, Object value);

	@Override
	<P> SelectionQuery<R> setParameter(String name, P value, Class<P> type);

	@Override
	<P> SelectionQuery<R> setParameter(String name, P value, Type<P> type);

	@Override @Deprecated
	SelectionQuery<R> setParameter(String name, Instant value, TemporalType temporalType);

	@Override @Deprecated
	SelectionQuery<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	SelectionQuery<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	SelectionQuery<R> setParameter(int position, Object value);

	@Override
	<P> SelectionQuery<R> setParameter(int position, P value, Class<P> type);

	@Override
	<P> SelectionQuery<R> setParameter(int position, P value, Type<P> type);

	@Override @Deprecated
	SelectionQuery<R> setParameter(int position, Instant value, TemporalType temporalType);

	@Override @Deprecated
	SelectionQuery<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override @Deprecated
	SelectionQuery<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	<T> SelectionQuery<R> setParameter(QueryParameter<T> parameter, T value);

	@Override
	<P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	@Override
	<P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P val, Type<P> type);

	@Override
	<T> SelectionQuery<R> setParameter(Parameter<T> param, T value);

	@Override @Deprecated
	SelectionQuery<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	SelectionQuery<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	SelectionQuery<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SelectionQuery<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SelectionQuery<R> setParameterList(String name, Collection<? extends P> values, Type<P> type);

	@Override
	SelectionQuery<R> setParameterList(String name, Object[] values);

	@Override
	<P> SelectionQuery<R> setParameterList(String name, P[] values, Class<P> javaType);

	@Override
	<P> SelectionQuery<R> setParameterList(String name, P[] values, Type<P> type);

	@Override
	SelectionQuery<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> SelectionQuery<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SelectionQuery<R> setParameterList(int position, Collection<? extends P> values, Type<P> type);

	@Override
	SelectionQuery<R> setParameterList(int position, Object[] values);

	@Override
	<P> SelectionQuery<R> setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> SelectionQuery<R> setParameterList(int position, P[] values, Type<P> type);

	@Override
	<P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type);

	@Override
	<P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type);

	@Override
	SelectionQuery<R> setProperties(Object bean);

	@Override
	SelectionQuery<R> setProperties(@SuppressWarnings("rawtypes") Map bean);
}
