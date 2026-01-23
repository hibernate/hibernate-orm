/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.Type;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.dialect.Dialect;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.spi.QueryOptions;

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
 * an instance of this type represents an executable query, either:
 * <ul>
 * <li>a Query<T> written in HQL or native SQL,
 * <li>a named Query<T> written in HQL or native SQL, or
 * <li>a {@linkplain jakarta.persistence.criteria.CriteriaBuilder criteria query}.
 * </ul>
 * <p>
 * The subtype {@link NativeQuery} represents a Query<T> written in native SQL.
 * <p>
 * This type simply mixes the {@link TypedQuery} interface defined by JPA with
 * {@link SelectionQuery} and {@link MutationQuery}. Unfortunately, JPA does
 * not distinguish between {@linkplain SelectionQuery<T> selection queries} and
 * {@linkplain MutationQuery<T> mutation queries}, so we lose that distinction here.
 * However, every {@code Query} may logically be classified as one or the other.
 * <p>
 * A {@code Query} may be obtained from the {@link org.hibernate.Session} by
 * calling:
 * <ul>
 * <li>{@link org.hibernate.SharedSessionContract#createQuery(String, Class)}, passing the HQL as a
 *     string,
 * <li>{@link org.hibernate.SharedSessionContract#createQuery(jakarta.persistence.criteria.CriteriaQuery)},
 *     passing a {@linkplain jakarta.persistence.criteria.CriteriaQuery<T> criteria
 *     object}, or
 * <li>{@link org.hibernate.SharedSessionContract#createNamedQuery(String, Class)} passing the name
 *     of a Query<T> defined using {@link jakarta.persistence.NamedQuery} or
 *     {@link jakarta.persistence.NamedNativeQuery}.
 * </ul>
 * <p>
 * A {@code Query} controls how a Query<T> is executed, and allows arguments to be
 * bound to its parameters.
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
 * Note that this interface offers no real advantages over {@link SelectionQuery}
 * except for compatibility with the JPA-defined {@link TypedQuery} interface.
 *
 * @param <T> The {@linkplain SelectionQuery#getResultType() result type} for selection queries
 * or the {@linkplain MutationQuery#getTargetType() target type} for mutation queries.
 *
 * @see SelectionQuery
 * @see MutationQuery
 * @see org.hibernate.procedure.ProcedureCall
 * @see jakarta.persistence.Query
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@Incubating
public interface Query<T> extends CommonQueryContract {

	/**
	 * The Query<T> as a string, or {@code null} in the case of a criteria query.
	 */
	String getQueryString();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Casts

	@Override
	SelectionQuery<T> asSelectionQuery();

	/**
	 * Covariant override of {@linkplain jakarta.persistence.Query#ofType}.
	 *
	 * @apiNote Jakarta Persistence declares that the generic {@linkplain IllegalStateException}
	 * exception be thrown, as opposed to something more meaningful like Hibernate's
	 * {@linkplain IllegalSelectQueryException}.
	 *
	 * @see #asSelectionQuery(Class)
	 */
	@Override
	<R> SelectionQuery<R> ofType(Class<R> type);

	/**
	 * Covariant override of {@linkplain jakarta.persistence.Query#withEntityGraph}.
	 *
	 * @apiNote Jakarta Persistence declares that the generic {@linkplain IllegalStateException}
	 * exception be thrown, as opposed to something more meaningful like Hibernate's
	 * {@linkplain IllegalSelectQueryException}.
	 *
	 * @see #asSelectionQuery(EntityGraph, GraphSemantic)
	 * @see SharedSessionContract#createSelectionQuery(String, EntityGraph)
	 * @see SharedSessionContract#createQuery(String, EntityGraph)
	 * @see #asSelectionQuery(Class)
	 */
	@Override
	<R> SelectionQuery<R> withEntityGraph(EntityGraph<R> entityGraph);

	/**
	 * Covariant override of {@linkplain jakarta.persistence.Query#asStatement}.
	 *
	 * @apiNote Jakarta Persistence declares that the generic {@linkplain IllegalStateException}
	 * exception be thrown, as opposed to something more meaningful like Hibernate's
	 * {@linkplain IllegalMutationQueryException}.
	 *
	 * @see #asMutationQuery()
	 */
	@Override
	MutationQuery asStatement();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> setComment(String comment);

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> addQueryHint(String hint);

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> setTimeout(int timeout);

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> setTimeout(Integer timeout);

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> setTimeout(Timeout timeout);

	/**
	 * Whether the execution plan for this Query<T> is cached.
	 */
	boolean isQueryPlanCacheable();

	/**
	 * Enable/disable Query<T> plan caching for this query, if available.
	 *
	 * @see #isQueryPlanCacheable
	 */
	Query<T> setQueryPlanCacheable(boolean queryPlanCacheable);

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> setHint(String hintName, Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter Handling

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> setParameter(String parameter, Object argument);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameter(String parameter, P argument, Class<P> type);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameter(String parameter, P argument, Type<P> type);

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> setParameter(int parameter, Object argument);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameter(int parameter, P argument, Class<P> type);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameter(int parameter, P argument, Type<P> type);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameter(QueryParameter<P> parameter, P argument);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameter(QueryParameter<P> parameter, P argument, Class<P> type);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameter(QueryParameter<P> parameter, P argument, Type<P> type);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameter(Parameter<P> parameter, P argument);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter);

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> setProperties(Object bean);

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> setProperties(@SuppressWarnings("rawtypes") Map bean);

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> setParameterList(String parameter, @SuppressWarnings("rawtypes") Collection arguments);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(String parameter, Collection<? extends P> arguments, Class<P> javaType);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(String parameter, Collection<? extends P> arguments, Type<P> type);

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> setParameterList(String parameter, Object[] values);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(String parameter, P[] arguments, Class<P> javaType);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(String parameter, P[] arguments, Type<P> type);

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> setParameterList(int parameter, @SuppressWarnings("rawtypes") Collection arguments);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(int parameter, Collection<? extends P> arguments, Class<P> javaType);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(int parameter, Collection<? extends P> arguments, Type<P> type);

	/**
	 * {@inheritDoc}
	 */
	@Override
	Query<T> setParameterList(int parameter, Object[] arguments);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(int parameter, P[] arguments, Class<P> javaType);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(int parameter, P[] arguments, Type<P> type);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> arguments);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> arguments, Class<P> javaType);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> arguments, Type<P> type);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(QueryParameter<P> parameter, P[] arguments);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(QueryParameter<P> parameter, P[] arguments, Class<P> javaType);

	/**
	 * {@inheritDoc}
	 */
	@Override
	<P> Query<T> setParameterList(QueryParameter<P> parameter, P[] arguments, Type<P> type);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Methods which inherently imply either selection or mutation queries

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
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as fetch size is only relevant for
	 * selection queries
	 */
	@Deprecated(since = "8.0", forRemoval = true)
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
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as fetch size is only relevant for
	 * selection queries
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	Query<T> setFetchSize(int fetchSize);

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
	Query<T> setReadOnly(boolean readOnly);

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
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as caching is only relevant for
	 * selection queries
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	boolean isCacheable();

	/**
	 * Enable/disable second level query (result) caching for this query.
	 *
	 * @see #isCacheable
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as caching is only relevant for
	 * selection queries
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	Query<T> setCacheable(boolean cacheable);

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
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as caching is only relevant for
	 * selection queries
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	CacheMode getCacheMode();

	/**
	 * Set the current {@link CacheMode} in effect for this query.
	 * <p>
	 * Set it to {@code null} to indicate that the {@code CacheMode}
	 * of the {@link Session#getCacheMode() session} should be used.
	 *
	 * @see #getCacheMode()
	 * @see Session#setCacheMode(CacheMode)
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as caching is only relevant for
	 * selection queries
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	Query<T> setCacheMode(CacheMode cacheMode);

	/**
	 * Obtain the name of the second level query cache region in which query
	 * results will be stored (if they are cached, see the discussion on
	 * {@link #isCacheable()} for more information). {@code null} indicates
	 * that the default region should be used.
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as caching is only relevant for
	 * selection queries
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	String getCacheRegion();

	/**
	 * Set the name of the cache region where query results should be cached
	 * (assuming {@link #isCacheable}). {@code null} indicates to use the
	 * default region.
	 *
	 * @see #getCacheRegion()
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as caching is only relevant for
	 * selection queries
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	Query<T> setCacheRegion(String cacheRegion);

	/**
	 * @deprecated Use {@linkplain SelectionQuery} instead as second-level cache
	 * interaction is only relevant for queries which return results.
	 */
	@Deprecated(since = "8.0")
	@Override
	Query<T> setCacheStoreMode(CacheStoreMode cacheStoreMode);

	/**
	 * @deprecated Use {@linkplain SelectionQuery} instead as second-level cache
	 * interaction is only relevant for queries which return results.
	 */
	@Override @Deprecated(since = "8.0")
	Query<T> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode);

	/**
	 * @deprecated Use {@linkplain SelectionQuery} instead as applying result limits
	 * is only relevant for queries which return results.
	 */
	@Deprecated(since = "8.0")
	@Override
	Query<T> setMaxResults(int maxResults);

	/**
	 * @deprecated Use {@linkplain SelectionQuery} instead as applying result limits
	 * is only relevant for queries which return results.
	 */
	@Deprecated(since = "8.0")
	@Override
	Query<T> setFirstResult(int startPosition);


	/***
	 * @deprecated Use {@linkplain SelectionQuery} instead as locking
	 * are only relevant for queries which return results.
	 */
	@Override
	@Deprecated(since = "8.0")
	Query<T> setLockMode(LockModeType lockMode);

	/**
	 * Get the root {@link LockMode} for the query, expressed using Hibernate's
	 * native {@linkplain LockMode} instead of JPA's {@linkplain LockModeType}.
	 *
	 * @see #getLockMode()
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as locking
	 * are only relevant for queries which return results.
	 */
	@Deprecated(since = "8.0")
	LockMode getHibernateLockMode();

	/**
	 * Specify the root {@link LockMode} for the query using Hibernate's
	 * native {@linkplain LockMode}.
	 *
	 * @see #setLockMode(LockModeType)
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as locking
	 * are only relevant for queries which return results.
	 */
	@Deprecated(since = "8.0")
	Query<T> setHibernateLockMode(LockMode lockMode);

	/**
	 * Timeout applied specifically to pessimistic lock acquisition.
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as locking
	 * is only relevant for queries which return results.
	 */
	@Deprecated
	Timeout getLockTimeout();

	/**
	 * Set a timeout to be applied specifically to pessimistic lock acquisition.
	 *
	 * @see #getLockTimeout()
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as locking
	 * is only relevant for queries which return results.
	 */
	@Deprecated
	Query<T> setLockTimeout(Timeout lockTimeout);

	/**
	 * Apply a scope to any pessimistic locking applied to the query.
	 *
	 * @param lockScope The lock scope to apply
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as locking is only
	 * relevant for queries which return results.
	 */
	@Deprecated(since = "8.0")
	Query<T> setLockScope(PessimisticLockScope lockScope);

	/**
	 * Control how Hibernate should handle cases where it is determined
	 * subsequent SQL queries would be needs to completely accomplish
	 * locking as requested.
	 *
	 * @param strategy The strategy for follow-on locking.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as locking is only
	 * relevant for queries which return results.
	 */
	@Deprecated(since = "8.0")
	Query<T> setFollowOnLockingStrategy(Locking.FollowOn strategy);

	/**
	 * Control how Hibernate should handle cases where it is determined
	 * subsequent SQL queries would be needs to completely accomplish
	 * locking as requested.
	 *
	 * @param strategy The strategy for follow-on locking.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead as locking is only
	 * relevant for queries which return results.
	 */
	@Deprecated(since = "8.0")
	Query<T> setFollowOnStrategy(Locking.FollowOn strategy);

	/**
	 * Set a {@link TupleTransformer}.
	 */
	<X> Query<X> setTupleTransformer(TupleTransformer<X> transformer);

	/**
	 * Set a {@link ResultListTransformer}.
	 */
	Query<T> setResultListTransformer(ResultListTransformer<T> transformer);

	/**
	 * Execute the Query<T> and return the Query<T> results as a {@link List}.
	 * If the Query<T> contains multiple items in the selection list, then
	 * by default each result in the list is packaged in an array of type
	 * {@code Object[]}.
	 *
	 * @return the result list
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead for queries which return results.
	 */
	@Deprecated(since = "8.0")
	List<T> list();

	/**
	 * Execute the Query<T> and return the Query<T> results as a {@link List}.
	 * If the Query<T> contains multiple items in the selection list, then
	 * by default each result in the list is packaged in an array of type
	 * {@code Object[]}.
	 *
	 * @implNote Delegates to {@link #list()}
	 *
	 * @return the results as a list
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead for queries which return results.
	 */
	@Override
	@Deprecated(since = "8.0")
	default List<T> getResultList() {
		return list();
	}

	/**
	 * Execute the Query<T> and return the results in a
	 * {@linkplain ScrollableResults scrollable form}.
	 * <p>
	 * This overload simply calls {@link #scroll(ScrollMode)} using the
	 * {@linkplain Dialect#defaultScrollMode() dialect default scroll mode}.
	 *
	 * @apiNote The exact behavior of this method depends somewhat
	 *          on the level of JDBC driver support for scrollable
	 *          {@link java.sql.ResultSet}s, and so is not very
	 *          portable between database.
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead for queries which return results.
	 */
	@Deprecated(since = "8.0")
	ScrollableResults<T> scroll();

	/**
	 * Execute the Query<T> and return the results in a
	 * {@linkplain ScrollableResults scrollable form}. The capabilities
	 * of the returned {@link ScrollableResults} depend on the specified
	 * {@link ScrollMode}.
	 *
	 * @apiNote The exact behavior of this method depends somewhat
	 *          on the level of JDBC driver support for scrollable
	 *          {@link java.sql.ResultSet}s, and so is not very
	 *          portable between database.
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead for queries which return results.
	 */
	@Deprecated(since = "8.0")
	ScrollableResults<T> scroll(ScrollMode scrollMode);

	/**
	 * Execute the Query<T> and return the Query<T> results as a {@link Stream}.
	 * If the Query<T> contains multiple items in the selection list, then
	 * by default each result in the stream is packaged in an array of
	 * type {@code Object[]}.
	 * <p>
	 * The client should call {@link Stream#close()} after processing the
	 * stream so that resources are freed as soon as possible.
	 *
	 * @implNote Delegates to {@link #stream()}, which in turn delegates
	 *           to this method. Implementors should implement at least
	 *           one of these methods.
	 *
	 * @return The results as a {@link Stream}
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead for queries which return results.
	 */
	@Override
	@Deprecated(since = "8.0")
	default Stream<T> getResultStream() {
		return stream();
	}

	/**
	 * Execute the Query<T> and return the Query<T> results as a {@link Stream}.
	 * If the Query<T> contains multiple items in the selection list, then
	 * by default each result in the stream is packaged in an array of type
	 * {@code Object[]}.
	 * <p>
	 * The client should call {@link Stream#close()} after processing the
	 * stream so that resources are freed as soon as possible.
	 *
	 * @return The results as a {@link Stream}
	 *
	 * @since 5.2
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead for queries which return results.
	 */
	@Deprecated(since = "8.0")
	default Stream<T> stream() {
		return list().stream();
	}

	/**
	 * Execute the Query<T> and return the single result of the query, or
	 * {@code null} if the Query<T> returns no results.
	 *
	 * @return the single result or {@code null} if there is no result to return
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead for queries which return results.
	 */
	@Deprecated(since = "8.0")
	T uniqueResult();

	/**
	 * Execute the Query<T> and return the single result of the query,
	 * throwing an exception if the Query<T> returns no results.
	 *
	 * @return the single result, only if there is exactly one
	 *
	 * @throws jakarta.persistence.NonUniqueResultException if there is more than one matching result
	 * @throws jakarta.persistence.NoResultException if there is no result to return
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead for queries which return results.
	 */
	@Override
	@Deprecated(since = "8.0")
	T getSingleResult();

	/**
	 * Execute the Query<T> and return the single result of the Query<T> as
	 * an instance of {@link Optional}.
	 *
	 * @return the single result as an {@code Optional}
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 *
	 * @deprecated Use {@linkplain SelectionQuery} instead for queries which return results.
	 */
	@Deprecated(since = "8.0")
	Optional<T> uniqueResultOptional();

	/**
	 * Execute an insert, update, or delete statement, and return the
	 * number of affected entities.
	 * <p>
	 * For use with instances of {@link MutationQuery} created using
	 * {@link org.hibernate.SharedSessionContract#createMutationQuery(String)},
	 * {@link org.hibernate.SharedSessionContract#createNamedMutationQuery(String)},
	 * {@link org.hibernate.SharedSessionContract#createNativeMutationQuery(String)},
	 * {@link org.hibernate.SharedSessionContract#createMutationQuery(jakarta.persistence.criteria.CriteriaUpdate)}, or
	 * {@link org.hibernate.SharedSessionContract#createMutationQuery(jakarta.persistence.criteria.CriteriaDelete)}.
	 *
	 * @return the number of affected entity instances
	 *         (may differ from the number of affected rows)
	 *
	 * @apiNote This method is needed because this interface extends
	 *          {@link jakarta.persistence.Query}, which defines this method.
	 *          See {@link MutationQuery} and {@link SelectionQuery}.
	 *
	 * @see jakarta.persistence.Query#executeUpdate()
	 *
	 * @deprecated Use {@linkplain MutationQuery} instead for queries which mutate data.
	 */
	@Override
	@Deprecated(since = "8.0")
	int executeUpdate();




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// deprecated methods

	/**
	 * The JPA {@link FlushModeType} in effect for this query.  By default, the
	 * Query<T> inherits the {@link FlushMode} of the {@link Session} from which
	 * it originates.
	 *
	 * @apiNote Inherited from Jakarta Persistence.  Prefer {@linkplain #getQueryFlushMode()}
	 * @see #getQueryFlushMode()
	 */
	@Override @Deprecated(since = "7")
	default FlushModeType getFlushMode() {
		final QueryFlushMode queryFlushMode = getQueryFlushMode();
		if ( queryFlushMode == null ) {
			return FlushModeType.AUTO;
		}
		return queryFlushMode.toJpaFlushMode();
	}

	/**
	 * Set the {@link FlushModeType} to use for this query.
	 * <p>
	 * Setting this to {@code null} ultimately indicates to use the
	 * {@link FlushMode} of the session. Use {@link #setQueryFlushMode}
	 * passing {@link QueryFlushMode#NO_FLUSH} instead to indicate that no automatic
	 * flushing should occur.
	 *
	 * @apiNote Inherited from Jakarta Persistence.  Prefer {@linkplain #setQueryFlushMode(QueryFlushMode)}
	 * @see #getQueryFlushMode()
	 * @see #setQueryFlushMode(QueryFlushMode)
	 * @see Session#getHibernateFlushMode()
	 *
	 * @deprecated Use {@linkplain #setQueryFlushMode(QueryFlushMode)} instead.
	 */
	@Override @Deprecated(since = "7")
	default Query<T> setFlushMode(FlushModeType flushMode) {
		setQueryFlushMode( QueryFlushMode.fromJpaMode( flushMode ) );
		return this;
	}

	/**
	 * Get the execution options for this {@code Query}. Many of the setters
	 * of this object update the state of the returned {@link QueryOptions}.
	 * This is useful because it gives access to s primitive value in its
	 * (nullable) wrapper form, rather than the primitive form as required
	 * by JPA. This allows us to distinguish whether a value has been
	 * explicitly set by the client.
	 *
	 * @return Return the encapsulation of this query's options.
	 *
	 * @deprecated The various Query<T> subtypes already expose all relevant options;
	 * plus exposing QueryOptions is layer-breaking as it is an SPI contract
	 * exposed on an API.
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	QueryOptions getQueryOptions();

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override @Deprecated(since = "7")
	Query<T> setParameter(Parameter<Calendar> parameter, Calendar argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override @Deprecated(since = "7")
	Query<T> setParameter(Parameter<Date> parameter, Date argument, TemporalType temporalType);

	/**
	 * Bind an {@link Instant} value to the named Query<T> parameter using
	 * just the portion indicated by the given {@link TemporalType}.
	 */
	@Deprecated(since = "7")
	Query<T> setParameter(String parameter, Instant argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override @Deprecated(since = "7")
	Query<T> setParameter(String parameter, Calendar argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override @Deprecated(since = "7")
	Query<T> setParameter(String parameter, Date argument, TemporalType temporalType);

	/**
	 * Bind an {@link Instant} value to the ordinal Query<T> parameter using
	 * just the portion indicated by the given {@link TemporalType}.
	 */
	@Deprecated(since = "7")
	Query<T> setParameter(int parameter, Instant argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override @Deprecated(since = "7")
	Query<T> setParameter(int parameter, Date argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override @Deprecated(since = "7")
	Query<T> setParameter(int parameter, Calendar argument, TemporalType temporalType);
}
