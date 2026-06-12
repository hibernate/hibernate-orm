/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.metamodel.Type;
import org.hibernate.CacheMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
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
 * a {@code select}. This interface extends the JPA-defined {@link TypedQuery}
 * interface adding additional operations.
 * <p>
 * A {@code SelectionQuery} may be obtained from the session by calling:
 * <ul>
 * <li>{@link SharedSessionContract#createQuery(Class,String)} or
 *     {@link SharedSessionContract#createSelectionQuery(String,Class)},
 *     passing the HQL query as a string and the query result type as a
 *     Java class object,
 * <li>{@link SharedSessionContract#createQuery(CriteriaSelect)} or
 *     {@link SharedSessionContract#createSelectionQuery(CriteriaSelect)},
 *     passing a {@linkplain CriteriaQuery criteria query object},
 *     or
 * <li>{@link SharedSessionContract#createNamedQuery(String, Class)} or
 *     {@link SharedSessionContract#createNamedSelectionQuery(String,Class)}
 *     passing the name of a query declared using
 *     {@link jakarta.persistence.NamedQuery} or
 *     {@link jakarta.persistence.NamedNativeQuery}.
 * </ul>
 * <p>
 * A {@code SelectionQuery} controls how a query is executed, and allows arguments
 * to be bound to its parameters.
 * <ul>
 * <li>Selection queries are usually executed using {@link #getResultList()},
 *     {@link #getSingleResult()}, or {@link #getSingleResultOrNull()}.
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
 *         session.createQuery(Book.class,
 *             "from Book left join fetch authors where title like :title")
 *                 .setParameter("title", title)
 *                 .setMaxResults(50)
 *                 .getResultList();
 * </pre>
 * A query which is expected to return exactly one result should be executed
 * via {@link #getSingleResult()}, or, if it might not return a result,
 * {@link #getSingleResultOrNull()}:
 * <pre>
 * Book book =
 *         session.createQuery(Book.class, "from Book where isbn = ?1")
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
 * @see jakarta.persistence.TypedQuery
 * @see SharedSessionContract#createQuery(Class,String)
 * @see SharedSessionContract#createQuery(String, EntityGraph)
 * @see SharedSessionContract#createSelectionQuery(String,Class)
 * @see SharedSessionContract#createSelectionQuery(String,EntityGraph)
 * @see SharedSessionContract#createQuery(CriteriaSelect)
 * @see SharedSessionContract#createSelectionQuery(CriteriaSelect)
 * @see SharedSessionContract#createNamedQuery(String, Class)
 * @see MutationOrSelectionQuery#asSelectionQuery()
 * @see MutationOrSelectionQuery#asSelectionQuery(Class)
 *
 * @author Steve Ebersole
 * @since 6.0
 */
@Incubating
public interface SelectionQuery<R> extends TypedQuery<R>, Query<R> {
	/**
	 * The type of things returned from the query.
	 */
	Class<R> getResultType();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

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
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	default List<R> getResultList() {
		return list();
	}

	/**
	 * Execute the query and return the query results as a {@link List}.
	 * If the query contains multiple items in the selection list, then
	 * by default each result in the list is packaged in an array of type
	 * {@code Object[]}.
	 *
	 * @return the result list
	 */
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	List<R> list();

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
	@Override
	@SuppressWarnings("removal")
	@Nonnull
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
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	default Stream<R> stream() {
		return list().stream();
	}

	/**
	 * Execute the query and return the single result of the query,
	 * throwing an exception if the query returns no results.
	 *
	 * @return the single result, only if there is exactly one
	 *
	 * @throws jakarta.persistence.NonUniqueResultException if there is more than one matching result
	 * @throws jakarta.persistence.NoResultException if there is no result to return
	 */
	@Override
	@SuppressWarnings("removal")
	R getSingleResult();

	/**
	 * Execute the query and return the single result of the query,
	 * or {@code null} if the query returns no results.
	 *
	 * @return the single result or {@code null}
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 */
	@Override
	@SuppressWarnings("removal")
	@Nullable
	R uniqueResult();

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
	@Override
	@SuppressWarnings("removal")
	@Nullable
	R getSingleResultOrNull();

	/**
	 * Execute the query and return the single result of the query,
	 * as an {@link Optional}.
	 *
	 * @return the single result as an {@code Optional}
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 */
	@Override
	@SuppressWarnings("removal")
	@Nonnull
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
	@Override
	long getResultCount();

	/**
	 * Returns {@linkplain ScrollableResults scrollable access}
	 * to the query results, with capabilities determined by the
	 * {@linkplain org.hibernate.dialect.Dialect#defaultScrollMode
	 * default scroll mode of the SQL dialect}.
	 *
	 * @apiNote The exact behavior of this method depends somewhat
	 *          on the level of JDBC driver support for scrollable
	 *          {@link java.sql.ResultSet}s, and so is not very
	 *          portable between databases.
	 *
	 * @see #scroll(ScrollMode)
	 */
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	ScrollableResults<R> scroll();

	/**
	 * Returns {@linkplain ScrollableResults scrollable access}
	 * to the query results. The capabilities of the returned
	 * {@link ScrollableResults} instance depend on the specified
	 * {@link ScrollMode}.
	 *
	 * @apiNote Not every JDBC driver supports every {@link ScrollMode}.
	 */
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	ScrollableResults<R> scroll(@Nonnull ScrollMode scrollMode);

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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setHint(@Nonnull String hintName, @Nullable Object value);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	SelectionQuery<R> addOption(@Nonnull Option option);

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
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQuery<R> enableFetchProfile(@Nonnull String profileName);

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
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQuery<R> disableFetchProfile(@Nonnull String profileName);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("deprecation")
	@Nonnull
	SelectionQuery<R> setFlushMode(@Nonnull FlushModeType flushMode);

	/**
	 * {@inheritDoc}
	 *
	 * @since 7.0
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode);

	/**
	 * Specify a {@linkplain java.sql.Statement#setQueryTimeout JDBC
	 * query timeout} to use when executing the query.
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setTimeout(int timeout);

	/**
	 * Specify a {@linkplain java.sql.Statement#setQueryTimeout JDBC
	 * query timeout} to use when executing the query.
	 *
	 * @since 7.0
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setTimeout(@Nullable Integer timeout);

	/**
	 * Specify a {@linkplain java.sql.Statement#setQueryTimeout JDBC
	 * query timeout} to use when executing the query.
	 *
	 * @since 7.0
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setTimeout(@Nullable Timeout timeout);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setComment(@Nullable String comment);

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
	 * @see org.hibernate.cfg.JdbcSettings#STATEMENT_FETCH_SIZE
	 */
	@Override
	@SuppressWarnings("removal")
	@Nullable
	Integer getFetchSize();

	/**
	 * Sets a JDBC fetch size hint for the query.
	 *
	 * @param fetchSize the fetch size hint
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getFetchSize()
	 * @see org.hibernate.cfg.JdbcSettings#STATEMENT_FETCH_SIZE
	 */
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQuery<R> setFetchSize(int fetchSize);

	/**
	 * Should entities and proxies loaded by this Query be put in read-only
	 * mode? If the read-only/modifiable setting was not initialized, then
	 * the default read-only/modifiable setting for the persistence context
	 * is returned instead.
	 * <p>
	 * The read-only/modifiable setting has no impact on entities/proxies
	 * returned by the query that existed in the session before the query
	 * was executed.
	 *
	 * @see #setReadOnly(boolean)
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 *
	 * @return {@code true} if the entities and proxies loaded by the query
	 *         will be put in read-only mode; {@code false} otherwise
	 *         (they will be modifiable)
	 */
	@Override
	@SuppressWarnings("removal")
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
	 * Read-only entities are not dirty-checked, and snapshots of persistent
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
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQuery<R> setReadOnly(boolean readOnly);

	/**
	 * {@inheritDoc}
	 */
	@Override
	int getMaxResults();

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setMaxResults(int maxResults);

	/**
	 * {@inheritDoc}
	 */
	@Override
	int getFirstResult();

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
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
	 * In order for this setting to have any effect, second-level caching
	 * must be enabled, and the entities and collections must be eligible
	 * for storage in the second-level cache.
	 *
	 * @see Session#getCacheMode()
	 */
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	CacheMode getCacheMode();

	/**
	 * {@inheritDoc}
	 *
	 * @see #getCacheMode()
	 *
	 * @since 6.2
	 */
	@Override
	@Nonnull
	CacheStoreMode getCacheStoreMode();

	/**
	 * {@inheritDoc}
	 *
	 * @see #getCacheMode()
	 *
	 * @since 6.2
	 */
	@Override
	@Nonnull
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
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQuery<R> setCacheMode(@Nonnull CacheMode cacheMode);

	/**
	 * @see #setCacheMode(CacheMode)
	 *
	 * @since 6.2
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode);

	/**
	 * @see #setCacheMode(CacheMode)
	 *
	 * @since 6.2
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode);

	/**
	 * Should the results of the query be stored in the second-level cache?
	 * <p>
	 * This is different to second level caching of any returned entities and
	 * collections, which is controlled by {@link #getCacheMode()}.
	 * <p>
	 * The query being "eligible" for caching does not necessarily mean its
	 * results will be cached; second-level query caching must also be
	 * explicitly enabled by setting the configuration property
	 * {@value org.hibernate.cfg.AvailableSettings#USE_QUERY_CACHE}.
	 */
	@Override
	@SuppressWarnings("removal")
	boolean isCacheable();

	/**
	 * Enable/disable second-level query (result) caching for this query.
	 *
	 * @see #isCacheable
	 */
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQuery<R> setCacheable(boolean cacheable);

	/**
	 * Enable/disable query plan caching for this query.
	 *
	 * @see #isQueryPlanCacheable
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setQueryPlanCacheable(boolean queryPlanCacheable);

	/**
	 * Obtain the name of the second level query cache region in which query
	 * results will be stored (if they are cached, see the discussion on
	 * {@link #isCacheable()} for more information). {@code null} indicates
	 * that the default region should be used.
	 */
	@Override
	@SuppressWarnings("removal")
	@Nullable
	String getCacheRegion();

	/**
	 * Set the name of the cache region where query results should be cached
	 * (assuming {@link #isCacheable}). {@code null} indicates to use the
	 * default region.
	 *
	 * @see #getCacheRegion()
	 */
	@Override
	@SuppressWarnings("removal")
	SelectionQuery<R> setCacheRegion(@Nullable String cacheRegion);

	/**
	 * {@inheritDoc}
	 *
	 * @see #getHibernateLockMode()
	 */
	@Override
	@Nullable
	LockModeType getLockMode();

	/**
	 * {@inheritDoc}
	 *
	 * @see #setHibernateLockMode
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setLockMode(@Nonnull LockModeType lockMode);

	/**
	 * {@inheritDoc}
	 *
	 * @see #getLockMode()
	 */
	@Override
	@Nonnull
	LockMode getHibernateLockMode();

	/**
	 * {@inheritDoc}
	 *
	 * @see #setLockMode(LockModeType)
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setHibernateLockMode(@Nonnull LockMode lockMode);

	/**
	 * {@inheritDoc}
	 *
	 * @since 8.0
	 */
	@Override
	@Nullable
	PessimisticLockScope getLockScope();

	/**
	 * {@inheritDoc}
	 *
	 * @since 8.0
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setLockScope(@Nonnull PessimisticLockScope lockScope);

	/**
	 * Get the pessimistic lock timeout, if any.
	 *
	 * @since 8.0
	 */
	@Nullable
	Timeout getLockTimeout();

	/**
	 * Specify a pessimistic lock timeout.
	 *
	 * @since 8.0
	 */
	@Nonnull
	SelectionQuery<R> setLockTimeout(@Nullable Timeout lockTimeout);

	/**
	 * Specifies whether follow-on locking should be applied
	 */
	@Override
	@Nonnull
	SelectionQuery<R> setFollowOnStrategy(@Nonnull Locking.FollowOn followOnStrategy);

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	SelectionQuery<R> addQueryHint(@Nonnull String hint);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Transformers

	/**
	 * Set a {@link TupleTransformer}.
	 */
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	<X> SelectionQuery<X> setTupleTransformer(@Nonnull TupleTransformer<X> transformer);

	/**
	 * Set a {@link ResultListTransformer}.
	 */
	@Override
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQuery<R> setResultListTransformer(@Nonnull ResultListTransformer<R> transformer);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecated methods

	/**
	 * Apply an {@link EntityGraph} to the query using {@linkplain GraphSemantic#LOAD load graph} semantics.
	 * <p>
	 * This is an alternative way to specify the associations which
	 * should be fetched as part of the initial query.
	 *
	 * @since 6.3
	 *
	 * @see SharedSessionContract#createSelectionQuery(String, EntityGraph)
	 * @see jakarta.persistence.StatementOrTypedQuery#withEntityGraph(EntityGraph)
	 *
	 * @deprecated Prefer passing the entity-graph while creating the query -
	 * {@linkplain SharedSessionContract#createSelectionQuery(String, EntityGraph)}
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	@Nonnull
	default SelectionQuery<R> setEntityGraph(@Nonnull EntityGraph<? super R> entityGraph) {
		return setEntityGraph( entityGraph, GraphSemantic.LOAD );
	}

	/**
	 * Apply an {@link EntityGraph} to the query.
	 * <p>
	 * This is an alternative way to specify the associations which
	 * should be fetched as part of the initial query.
	 *
	 * @since 6.3
	 *
	 * @see SharedSessionContract#createSelectionQuery(String, EntityGraph)
	 * @see MutationOrSelectionQuery#asSelectionQuery(EntityGraph)
	 * @see MutationOrSelectionQuery#asSelectionQuery(EntityGraph,GraphSemantic)
	 * @see jakarta.persistence.StatementOrTypedQuery#withEntityGraph(EntityGraph)
	 *
	 * @deprecated Prefer passing the entity-graph while creating the query -
	 * {@linkplain SharedSessionContract#createSelectionQuery(String, EntityGraph)}
	 */
	@Override
	@Deprecated(since = "8.0", forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	SelectionQuery<R> setEntityGraph(@Nonnull EntityGraph<? super R> graph, @Nonnull GraphSemantic semantic);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter Handling

	@Override
	@Nonnull
	SelectionQuery<R> setParameter(@Nonnull String name, @Nullable Object value);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameter(@Nonnull String name, @Nullable P value, @Nonnull Type<P> type);

	@Override
	@Nonnull
	SelectionQuery<R> setParameter(int position, @Nullable Object value);

	@Override
	@Nonnull
	SelectionQuery<R> setParameters(@Nonnull Object... arguments);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameter(int position, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameter(int position, @Nullable P value, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<T> SelectionQuery<R> setParameter(@Nonnull QueryParameter<T> parameter, @Nullable T value);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P val, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<T> SelectionQuery<R> setParameter(@Nonnull Parameter<T> param, @Nullable T value);

	@Override
	@Nonnull
	SelectionQuery<R> setProperties(@Nonnull Object bean);

	@Override
	@Nonnull
	SelectionQuery<R> setProperties(@SuppressWarnings("rawtypes") @Nonnull Map bean);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setConvertedParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setConvertedParameter(int position, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Nonnull
	SelectionQuery<R> setParameterList(@Nonnull String name, @SuppressWarnings("rawtypes") @Nonnull Collection values);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	SelectionQuery<R> setParameterList(@Nonnull String name, @Nonnull Object[] values);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	SelectionQuery<R> setParameterList(int position, @SuppressWarnings("rawtypes") @Nonnull Collection values);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	SelectionQuery<R> setParameterList(int position, @Nonnull Object[] values);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(int position, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(int position, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Class<P> javaType);

	@Override
	@Nonnull
	<P> SelectionQuery<R> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Type<P> type);

	@Override @Deprecated
	@Nonnull
	SelectionQuery<R> setParameter(@Nonnull String name, @Nullable Instant value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQuery<R> setParameter(@Nonnull String name, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQuery<R> setParameter(@Nonnull String name, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQuery<R> setParameter(int position, @Nullable Instant value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQuery<R> setParameter(int position, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQuery<R> setParameter(int position, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQuery<R> setParameter(@Nonnull Parameter<Calendar> param, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	SelectionQuery<R> setParameter(@Nonnull Parameter<Date> param, @Nullable Date value, @Nonnull TemporalType temporalType);
}
