/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

import jakarta.persistence.EntityGraph;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockOptions;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SharedSessionContract;
import org.hibernate.dialect.Dialect;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.transform.ResultTransformer;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.Type;

/**
 * Within the context of an active {@linkplain org.hibernate.Session session},
 * an instance of this type represents an executable query, either:
 * <ul>
 * <li>a query written in HQL,
 * <li>a named query written in HQL or native SQL, or
 * <li>a {@linkplain jakarta.persistence.criteria.CriteriaBuilder criteria query}.
 * </ul>
 * <p>
 * The subtype {@link NativeQuery} represents a query written in native SQL.
 * <p>
 * This type simply mixes the {@link TypedQuery} interface defined by JPA with
 * {@link SelectionQuery} and {@link MutationQuery}. Unfortunately, JPA does
 * not distinguish between {@linkplain SelectionQuery selection queries} and
 * {@linkplain MutationQuery mutation queries}, so we lose that distinction here.
 * However, every {@code Query} may logically be classified as one or the other.
 * <p>
 * A {@code Query} may be obtained from the {@link org.hibernate.Session} by
 * calling:
 * <ul>
 * <li>{@link QueryProducer#createQuery(String, Class)}, passing the HQL as a
 *     string,
 * <li>{@link QueryProducer#createQuery(jakarta.persistence.criteria.CriteriaQuery)},
 *     passing a {@linkplain jakarta.persistence.criteria.CriteriaQuery criteria
 *     object}, or
 * <li>{@link QueryProducer#createNamedQuery(String, Class)} passing the name
 *     of a query defined using {@link jakarta.persistence.NamedQuery} or
 *     {@link jakarta.persistence.NamedNativeQuery}.
 * </ul>
 * <p>
 * A {@code Query} controls how a query is executed, and allows arguments to be
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
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @param <R> The result type, for typed queries, or {@link Object} for untyped queries
 *
 * @see QueryProducer
 */
@Incubating
public interface Query<R> extends SelectionQuery<R>, MutationQuery, TypedQuery<R> {

	/**
	 * Execute the query and return the query results as a {@link List}.
	 * If the query contains multiple items in the selection list, then
	 * by default each result in the list is packaged in an array of type
	 * {@code Object[]}.
	 *
	 * @return the result list
	 */
	@Override
	List<R> list();

	/**
	 * Execute the query and return the query results as a {@link List}.
	 * If the query contains multiple items in the selection list, then
	 * by default each result in the list is packaged in an array of type
	 * {@code Object[]}.
	 *
	 * @implNote Delegates to {@link #list()}
	 *
	 * @return the results as a list
	 */
	@Override
	default List<R> getResultList() {
		return list();
	}

	/**
	 * Execute the query and return the results in a
	 * {@linkplain ScrollableResults scrollable form}.
	 * <p>
	 * This overload simply calls {@link #scroll(ScrollMode)} using the
	 * {@linkplain Dialect#defaultScrollMode() dialect default scroll mode}.
	 *
	 * @apiNote The exact behavior of this method depends somewhat
	 *          on the level of JDBC driver support for scrollable
	 *          {@link java.sql.ResultSet}s, and so is not very
	 *          portable between database.
	 */
	@Override
	ScrollableResults<R> scroll();

	/**
	 * Execute the query and return the results in a
	 * {@linkplain ScrollableResults scrollable form}. The capabilities
	 * of the returned {@link ScrollableResults} depend on the specified
	 * {@link ScrollMode}.
	 *
	 * @apiNote The exact behavior of this method depends somewhat
	 *          on the level of JDBC driver support for scrollable
	 *          {@link java.sql.ResultSet}s, and so is not very
	 *          portable between database.
	 */
	@Override
	ScrollableResults<R> scroll(ScrollMode scrollMode);

	/**
	 * Execute the query and return the query results as a {@link Stream}.
	 * If the query contains multiple items in the selection list, then
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
	 */
	@Override
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
	@Override
	default Stream<R> stream() {
		return list().stream();
	}

	/**
	 * Execute the query and return the single result of the query, or
	 * {@code null} if the query returns no results.
	 *
	 * @return the single result or {@code null} if there is no result to return
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 */
	@Override
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
	@Override
	R getSingleResult();

	/**
	 * Execute the query and return the single result of the query as
	 * an instance of {@link Optional}.
	 *
	 * @return the single result as an {@code Optional}
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 */
	@Override
	Optional<R> uniqueResultOptional();

	/**
	 * Execute an insert, update, or delete statement, and return the
	 * number of affected entities.
	 * <p>
	 * For use with instances of {@link MutationQuery} created using
	 * {@link QueryProducer#createMutationQuery(String)},
	 * {@link QueryProducer#createNamedMutationQuery(String)},
	 * {@link QueryProducer#createNativeMutationQuery(String)},
	 * {@link QueryProducer#createMutationQuery(jakarta.persistence.criteria.CriteriaUpdate)}, or
	 * {@link QueryProducer#createMutationQuery(jakarta.persistence.criteria.CriteriaDelete)}.
	 *
	 * @return the number of affected entity instances
	 *         (may differ from the number of affected rows)
	 *
	 * @apiNote This method is needed because this interface extends
	 *          {@link jakarta.persistence.Query}, which defines this method.
	 *          See {@link MutationQuery} and {@link SelectionQuery}.
	 *
	 * @see QueryProducer#createMutationQuery
	 * @see QueryProducer#createMutationQuery(String)
	 * @see QueryProducer#createNamedMutationQuery(String)
	 * @see QueryProducer#createNativeMutationQuery(String)
	 * @see QueryProducer#createMutationQuery(jakarta.persistence.criteria.CriteriaUpdate)
	 * @see QueryProducer#createMutationQuery(jakarta.persistence.criteria.CriteriaDelete)
	 *
	 * @see jakarta.persistence.Query#executeUpdate()
	 */
	@Override
	int executeUpdate();

	/**
	 * Get the {@link QueryProducer} which produced this {@code Query},
	 * that is, the {@link org.hibernate.Session} or
	 * {@link org.hibernate.StatelessSession} that was used to create
	 * this {@code Query} instance.
	 *
	 * @return The producer of this query
	 */
	SharedSessionContract getSession();

	/**
	 * The query as a string, or {@code null} in the case of a criteria query.
	 */
	String getQueryString();

	/**
	 * Apply the given graph using the given semantic
	 *
	 * @param graph The graph to apply.
	 * @param semantic The semantic to use when applying the graph
	 *
	 * @deprecated Use {@link #setEntityGraph(EntityGraph, GraphSemantic)}
	 *             which is more type safe
	 */
	@Deprecated(since = "7.0")
	Query<R> applyGraph(@SuppressWarnings("rawtypes") RootGraph graph, GraphSemantic semantic);

	/**
	 * Apply the given graph using {@linkplain GraphSemantic#FETCH fetch semantics}.
	 *
	 * @apiNote This method calls {@link #applyGraph(RootGraph, GraphSemantic)}
	 *          using {@link GraphSemantic#FETCH} as the semantic.
	 *
	 * @deprecated Use {@link #setEntityGraph(EntityGraph, GraphSemantic)}
	 *             which is more type safe
	 */
	@Deprecated(since = "7.0")
	default Query<R> applyFetchGraph(@SuppressWarnings("rawtypes") RootGraph graph) {
		return applyGraph( graph, GraphSemantic.FETCH );
	}

	/**
	 * Apply the given graph using {@linkplain GraphSemantic#LOAD load semantics}.
	 *
	 * @apiNote This method calls {@link #applyGraph(RootGraph, GraphSemantic)}
	 *          using {@link GraphSemantic#LOAD} as the semantic.
	 *
	 * @deprecated Use {@link #setEntityGraph(EntityGraph, GraphSemantic)}
	 *             which is more type safe
	 */
	@Deprecated(since = "7.0")
	default Query<R> applyLoadGraph(@SuppressWarnings("rawtypes") RootGraph graph) {
		return applyGraph( graph, GraphSemantic.LOAD );
	}

	/**
	 * Obtain the comment currently associated with this query.
	 * <p>
	 * If SQL commenting is enabled, the comment will be added to the SQL
	 * query sent to the database, which may be useful for identifying the
	 * source of troublesome queries.
	 * <p>
	 * SQL commenting may be enabled using the configuration property
	 * {@value org.hibernate.cfg.AvailableSettings#USE_SQL_COMMENTS}.
	 *
	 * @return The comment.
	 */
	String getComment();

	/**
	 * Set the comment for this query.
	 * <p>
	 * If SQL commenting is enabled, the comment will be added to the SQL
	 * query sent to the database, which may be useful for identifying the
	 * source of troublesome queries.
	 * <p>
	 * SQL commenting may be enabled using the configuration property
	 * {@value org.hibernate.cfg.AvailableSettings#USE_SQL_COMMENTS}.
	 *
	 * @param comment The human-readable comment
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getComment()
	 */
	Query<R> setComment(String comment);

	/**
	 * Add a database query hint to the SQL query.
	 * <p>
	 * A database hint is a completely different concept to a JPA hint
	 * specified using {@link jakarta.persistence.QueryHint} or
	 * {@link #getHints()}. These are hints to the JPA provider.
	 * <p>
	 * Multiple query hints may be specified. The operation
	 * {@link Dialect#getQueryHintString(String, List)} determines how
	 * the hint is actually added to the SQL query.
	 *
	 * @param hint The database specific query hint to add.
	 */
	Query<R> addQueryHint(String hint);

	/**
	 * Obtains the {@link LockOptions} in effect for this query.
	 *
	 * @return The {@link LockOptions} currently in effect
	 *
	 * @see LockOptions
	 *
	 * @deprecated Since {@link LockOptions} is transitioning to
	 *             a new role as an SPI.
	 */
	@Override
	@Deprecated(since = "7.0", forRemoval = true)
	LockOptions getLockOptions();

	/**
	 * Apply the given {@linkplain LockOptions lock options} to this
	 * query. Alias-specific lock modes in the given lock options are
	 * merged with any alias-specific lock mode which have already been
	 * {@linkplain #setLockMode(String, LockMode) set}. If a lock mode
	 * has already been specified for an alias that is among the aliases
	 * in the given lock options, the lock mode specified in the given
	 * lock options overrides the lock mode that was already set.
	 *
	 * @param lockOptions The lock options to apply to the query.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getLockOptions()
	 *
	 * @deprecated Use one of {@linkplain #setLockMode(LockModeType)},
	 * {@linkplain #setHibernateLockMode}, {@linkplain #setLockScope}
	 * and/or {@linkplain #setTimeout} instead.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	Query<R> setLockOptions(LockOptions lockOptions);

	/**
	 * Apply a timeout to the corresponding database query.
	 *
	 * @param timeout The timeout to apply
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setTimeout(Timeout timeout);

	/**
	 * Apply a scope to any pessimistic locking applied to the query.
	 *
	 * @param lockScope The lock scope to apply
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setLockScope(PessimisticLockScope lockScope);

	/**
	 * Set a {@link TupleTransformer}.
	 */
	<T> Query<T> setTupleTransformer(TupleTransformer<T> transformer);

	/**
	 * Set a {@link ResultListTransformer}.
	 */
	Query<R> setResultListTransformer(ResultListTransformer<R> transformer);

	/**
	 * Get the execution options for this {@code Query}. Many of the setters
	 * of this object update the state of the returned {@link QueryOptions}.
	 * This is useful because it gives access to s primitive value in its
	 * (nullable) wrapper form, rather than the primitive form as required
	 * by JPA. This allows us to distinguish whether a value has been
	 * explicitly set by the client.
	 *
	 * @return Return the encapsulation of this query's options.
	 */
	QueryOptions getQueryOptions();

	/**
	 * Bind the given argument to a named query parameter.
	 * <p>
	 * If the type of the parameter cannot be inferred from the context
	 * in which it occurs, use one of the forms accept a "type".
	 *
	 * @see #setParameter(String, Object, Class)
	 * @see #setParameter(String, Object, Type)
	 */
	@Override
	Query<R> setParameter(String parameter, Object argument);

	/**
	 * Bind the given argument to a named query parameter using the given
	 * Class reference to attempt to determine the {@link Type}
	 * to use.  If unable to determine an appropriate {@link Type},
	 * {@link #setParameter(String, Object)} is used.
	 *
	 * @see #setParameter(String, Object, Type)
	 */
	<P> Query<R> setParameter(String parameter, P argument, Class<P> type);

	/**
	 * Bind the given argument to a named query parameter using the given
	 * {@link Type}.
	 */
	<P> Query<R> setParameter(String parameter, P argument, Type<P> type);

	/**
	 * Bind an {@link Instant} value to the named query parameter using
	 * just the portion indicated by the given {@link TemporalType}.
	 */
	@Deprecated(since = "7")
	Query<R> setParameter(String parameter, Instant argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override @Deprecated(since = "7")
	Query<R> setParameter(String parameter, Calendar argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override @Deprecated(since = "7")
	Query<R> setParameter(String parameter, Date argument, TemporalType temporalType);



	/**
	 * Bind the given argument to an ordinal query parameter.
	 * <p>
	 * If the type of the parameter cannot be inferred from the context in
	 * which it occurs, use one of the forms which accepts a "type".
	 *
	 * @see #setParameter(int, Object, Class)
	 * @see #setParameter(int, Object, Type)
	 */
	@Override
	Query<R> setParameter(int parameter, Object argument);

	/**
	 * Bind the given argument to an ordinal query parameter using the given
	 * Class reference to attempt to determine the {@link Type}
	 * to use.  If unable to determine an appropriate {@link Type},
	 * {@link #setParameter(int, Object)} is used.
	 *
	 * @see #setParameter(int, Object, Type)
	 */
	<P> Query<R> setParameter(int parameter, P argument, Class<P> type);

	/**
	 * Bind the given argument to an ordinal query parameter using the given
	 * {@link Type}.
	 */
	<P> Query<R> setParameter(int parameter, P argument, Type<P> type);

	/**
	 * Bind an {@link Instant} value to the ordinal query parameter using
	 * just the portion indicated by the given {@link TemporalType}.
	 */
	@Override @Deprecated(since = "7")
	Query<R> setParameter(int parameter, Instant argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override @Deprecated(since = "7")
	Query<R> setParameter(int parameter, Date argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override @Deprecated(since = "7")
	Query<R> setParameter(int parameter, Calendar argument, TemporalType temporalType);

	/**
	 * Bind an argument to the query parameter represented by the given
	 * {@link QueryParameter}.
	 * <p>
	 * If the type of the parameter cannot be inferred from the context in
	 * which it occurs, use one of the forms which accepts a "type".
	 *
	 * @see #setParameter(QueryParameter, Object, Type)
	 *
	 * @param parameter the query parameter memento
	 * @param argument the argument, which might be null
	 *
	 * @return {@code this}, for method chaining
	 */
	<T> Query<R> setParameter(QueryParameter<T> parameter, T argument);

	/**
	 * Bind an argument to the query parameter represented by the given
	 * {@link QueryParameter} using the given Class reference to attempt to
	 * determine the {@link Type} to use.  If unable to determine
	 * an appropriate {@link Type}, {@link #setParameter(QueryParameter, Object)} is used
	 *
	 * @param parameter the query parameter memento
	 * @param argument the argument, which might be null
	 * @param type a {@link Type} representing the type of the parameter
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #setParameter(QueryParameter, Object, Type)
	 */
	<P> Query<R> setParameter(QueryParameter<P> parameter, P argument, Class<P> type);

	/**
	 * Bind an argument to the query parameter represented by the given
	 * {@link QueryParameter} using the given {@link Type}.
	 *
	 * @param parameter the query parameter memento
	 * @param argument the argument, which might be null
	 * @param type an {@link Type} representing the type of the parameter
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameter(QueryParameter<P> parameter, P argument, Type<P> type);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override
	<T> Query<R> setParameter(Parameter<T> parameter, T argument);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override @Deprecated(since = "7")
	Query<R> setParameter(Parameter<Calendar> parameter, Calendar argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override @Deprecated(since = "7")
	Query<R> setParameter(Parameter<Date> parameter, Date argument, TemporalType temporalType);



	/**
	 * Bind multiple arguments to a named query parameter.
	 * <p>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element.
	 *
	 * @see #setParameterList(java.lang.String, java.util.Collection, Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(String parameter, @SuppressWarnings("rawtypes") Collection arguments);

	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * Class reference to attempt to determine the {@link Type}
	 * to use.  If unable to determine an appropriate {@link Type},
	 * {@link #setParameterList(String, Collection)} is used.
	 *
	 * @see #setParameterList(java.lang.String, java.util.Collection, Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(String parameter, Collection<? extends P> arguments, Class<P> javaType);

	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(String parameter, Collection<? extends P> arguments, Type<P> type);


	/**
	 * Bind multiple arguments to a named query parameter.
	 * <p>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(String parameter, Object[] values);

	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * Class reference to attempt to determine the {@link Type}
	 * to use.  If unable to determine an appropriate {@link Type},
	 * {@link #setParameterList(String, Collection)} is used.
	 *
	 * @see #setParameterList(java.lang.String, Object[], Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(String parameter, P[] arguments, Class<P> javaType);


	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(String parameter, P[] arguments, Type<P> type);

	/**
	 * Bind multiple arguments to an ordinal query parameter.
	 * <p>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(int parameter, @SuppressWarnings("rawtypes") Collection arguments);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the given
	 * Class reference to attempt to determine the {@link Type}
	 * to use.  If unable to determine an appropriate {@link Type},
	 * {@link #setParameterList(String, Collection)} is used.
	 *
	 * @see #setParameterList(int, Collection, Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(int parameter, Collection<? extends P> arguments, Class<P> javaType);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the given
	 * {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(int parameter, Collection<? extends P> arguments, Type<P> type);

	/**
	 * Bind multiple arguments to an ordinal query parameter.
	 * <p>
	 * The "type mapping" for the binding is inferred from the type of the
	 * first collection element.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(int parameter, Object[] arguments);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the given
	 * {@link Class} reference to attempt to determine the {@link Type}
	 * to use. If unable to determine an appropriate {@link Type},
	 * {@link #setParameterList(String, Collection)} is used.
	 *
	 * @see #setParameterList(int, Object[], Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(int parameter, P[] arguments, Class<P> javaType);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the given
	 * {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(int parameter, P[] arguments, Type<P> type);

	/**
	 * Bind multiple arguments to the query parameter represented by the given
	 * {@link QueryParameter}.
	 * <p>
	 * The type of the parameter is inferred from the context in which it occurs,
	 * and from the type of the first given argument.
	 *
	 * @param parameter the parameter memento
	 * @param arguments a collection of arguments
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> arguments);

	/**
	 * Bind multiple arguments to the query parameter represented by the given
	 * {@link QueryParameter} using the given Class reference to attempt to
	 * determine the {@link Type} to use. If unable to determine an
	 * appropriate {@link Type}, {@link #setParameterList(String, Collection)}
	 * is used.
	 *
	 * @see #setParameterList(QueryParameter, java.util.Collection, Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression such
	 *          as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> arguments, Class<P> javaType);

	/**
	 * Bind multiple arguments to the query parameter represented by the given
	 * {@link QueryParameter}, inferring the {@link Type}.
	 * <p>
	 * The "type mapping" for the binding is inferred from the type of the first
	 * collection element.
	 *
	 * @apiNote This is used for binding a list of values to an expression such
	 *          as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> arguments, Type<P> type);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}.
	 * <p>
	 * The type of the parameter is inferred between the context in which it
	 * occurs, the type associated with the QueryParameter and the type of
	 * the first given argument.
	 *
	 * @param parameter the parameter memento
	 * @param arguments a collection of arguments
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(QueryParameter<P> parameter, P[] arguments);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter} using the given {@code Class} reference
	 * to attempt to determine the {@link Type} to use. If unable to
	 * determine an appropriate {@link Type},
	 * {@link #setParameterList(String, Collection)} is used.
	 *
	 * @see #setParameterList(QueryParameter, Object[], Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(QueryParameter<P> parameter, P[] arguments, Class<P> javaType);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}, inferring the {@link Type}.
	 * <p>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 *          such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(QueryParameter<P> parameter, P[] arguments, Type<P> type);

	/**
	 * Bind the property values of the given bean to named parameters of the query,
	 * matching property names with parameter names and mapping property types to
	 * Hibernate types using heuristics.
	 *
	 * @param bean any JavaBean or POJO
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setProperties(Object bean);

	/**
	 * Bind the values of the given {@code Map} for each named parameters of
	 * the query, matching key names with parameter names and mapping value
	 * types to Hibernate types using heuristics.
	 *
	 * @param bean a {@link Map} of names to arguments
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setProperties(@SuppressWarnings("rawtypes") Map bean);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides - CommonQueryContract

	@Override @Deprecated(since = "7")
	Query<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	Query<R> setQueryFlushMode(QueryFlushMode queryFlushMode);

	@Override
	Query<R> setCacheable(boolean cacheable);

	@Override
	Query<R> setCacheRegion(String cacheRegion);

	@Override
	Query<R> setCacheMode(CacheMode cacheMode);

	@Override
	Query<R> setCacheStoreMode(CacheStoreMode cacheStoreMode);

	@Override
	Query<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode);

	@Override
	Query<R> setTimeout(int timeout);

	@Override
	Query<R> setFetchSize(int fetchSize);

	@Override
	Query<R> setReadOnly(boolean readOnly);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides - jakarta.persistence.Query/TypedQuery

	@Override
	Query<R> setMaxResults(int maxResults);

	@Override
	Query<R> setFirstResult(int startPosition);

//	@Override @Incubating
//	default Query<R> setPage(int pageSize, int pageNumber) {
//		setFirstResult( pageNumber * pageSize );
//		setMaxResults( pageSize );
//		return this;
//	}

	@Override @Incubating
	default Query<R> setPage(Page page) {
		setMaxResults( page.getMaxResults() );
		setFirstResult( page.getFirstResult() );
		return this;
	}

	@Override
	Query<R> setHint(String hintName, Object value);

	@Override
	Query<R> setEntityGraph(EntityGraph<? super R> graph, GraphSemantic semantic);

	@Override
	Query<R> enableFetchProfile(String profileName);

	@Override
	Query<R> disableFetchProfile(String profileName);

	@Override @Deprecated(since = "7")
	Query<R> setFlushMode(FlushModeType flushMode);

	@Override
	Query<R> setLockMode(LockModeType lockMode);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// deprecated methods

	/**
	 * @deprecated Use {@link #setTupleTransformer} or {@link #setResultListTransformer}
	 */
	@Deprecated(since = "5.2")
	default <T> Query<T> setResultTransformer(ResultTransformer<T> transformer) {
		return setTupleTransformer( transformer ).setResultListTransformer( transformer );
	}

}
