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
import org.hibernate.SharedSessionContract;
import org.hibernate.dialect.Dialect;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.transform.ResultTransformer;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;

/**
 * Represents an HQL/JPQL query or a compiled Criteria query.  Also acts as the Hibernate
 * extension to the JPA Query/TypedQuery contract
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @param <R> The query result type, for typed queries, or {@code Object} for untyped queries
 */
@Incubating
public interface Query<R> extends TypedQuery<R>, CommonQueryContract {
	/**
	 * Get the QueryProducer this Query originates from.  Generally speaking,
	 * this is the Session/StatelessSession that was used to create the Query
	 * instance.
	 *
	 * @return The producer of this query
	 */
	SharedSessionContract getSession();

	/**
	 * Get the query string.  Note that this may be {@code null} or some other
	 * less-than-useful return because the source of the query might not be a
	 * String (e.g., a Criteria query).
	 *
	 * @return the query string.
	 */
	String getQueryString();

	/**
	 * Apply the given graph using the given semantic
	 *
	 * @param graph The graph the apply.
	 * @param semantic The semantic to use when applying the graph
	 *
	 * @return this - for method chaining
	 */
	Query<R> applyGraph(@SuppressWarnings("rawtypes") RootGraph graph, GraphSemantic semantic);

	/**
	 * Apply the given graph using {@linkplain GraphSemantic#FETCH fetch semantics}
	 *
	 * @apiNote This method calls {@link #applyGraph(RootGraph, GraphSemantic)} using
	 * {@link GraphSemantic#FETCH} as the semantic
	 */
	default Query<R> applyFetchGraph(@SuppressWarnings("rawtypes") RootGraph graph) {
		return applyGraph( graph, GraphSemantic.FETCH );
	}

	/**
	 * Apply the given graph using {@linkplain GraphSemantic#LOAD load semantics}
	 *
	 * @apiNote This method calls {@link #applyGraph(RootGraph, GraphSemantic)} using
	 * {@link GraphSemantic#LOAD} as the semantic
	 */
	@SuppressWarnings("UnusedDeclaration")
	default Query<R> applyLoadGraph(@SuppressWarnings("rawtypes") RootGraph graph) {
		return applyGraph( graph, GraphSemantic.LOAD );
	}

	/**
	 * Returns scrollable access to the query results.
	 *
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
	default Stream<R> getResultStream() {
		return stream();
	}

	/**
	 * Execute an insert, update, or delete statement, and return the
	 * number of affected entities.
	 * <p>
	 * For use with instances of {@code Query<Void>} created using
	 * {@link QueryProducer#createStatement(String)},
	 * {@link QueryProducer#createNamedStatement(String)},
	 * {@link QueryProducer#createNativeStatement(String)},
	 * {@link QueryProducer#createQuery(jakarta.persistence.criteria.CriteriaUpdate)}, or
	 * {@link QueryProducer#createQuery(jakarta.persistence.criteria.CriteriaDelete)}.
	 *
	 * @return the number of affected entity instances
	 *         (may differ from the number of affected rows)
	 *
	 * @see QueryProducer#createStatement(String)
	 * @see QueryProducer#createNamedStatement(String)
	 * @see QueryProducer#createNativeStatement(String)
	 *
	 * @see jakarta.persistence.Query#executeUpdate()
	 */
	@Override
	int executeUpdate();

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
	 * as an {@link Optional}.
	 *
	 * @return the single result as an {@code Optional}
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 */
	Optional<R> uniqueResultOptional();

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
		return getResultStream();
	}

	/**
	 * Obtain the comment currently associated with this query.  Provided SQL commenting is enabled
	 * (generally by enabling the {@code hibernate.use_sql_comments} config setting), this comment will also be added
	 * to the SQL query sent to the database.  Often useful for identifying the source of troublesome queries on the
	 * database side.
	 *
	 * @return The comment.
	 */
	String getComment();

	/**
	 * Set the comment for this query.
	 *
	 * @param comment The human-readable comment
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getComment()
	 */
	Query<R> setComment(String comment);

	/**
	 * Add a DB query hint to the SQL.  These differ from JPA's
	 * {@link jakarta.persistence.QueryHint} and {@link #getHints()}, which is
	 * specific to the JPA implementation and ignores DB vendor-specific hints.
	 * Instead, these are intended solely for the vendor-specific hints, such
	 * as Oracle's optimizers.  Multiple query hints are supported; the Dialect
	 * will determine concatenation and placement.
	 *
	 * @param hint The database specific query hint to add.
	 */
	Query<R> addQueryHint(String hint);

	/**
	 * Obtains the LockOptions in effect for this query.
	 *
	 * @return The LockOptions
	 *
	 * @see LockOptions
	 */
	LockOptions getLockOptions();

	/**
	 * Set the lock options for the query.  Specifically only the following are taken into consideration:<ol>
	 * <li>{@link LockOptions#getLockMode()}</li>
	 * <li>{@link LockOptions#getScope()}</li>
	 * <li>{@link LockOptions#getTimeOut()}</li>
	 * </ol>
	 * For alias-specific locking, use {@link #setLockMode(String, LockMode)}.
	 *
	 * @param lockOptions The lock options to apply to the query.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getLockOptions()
	 */
	Query<R> setLockOptions(LockOptions lockOptions);

	/**
	 * Set the LockMode to use for specific alias (as defined in the query's {@code FROM} clause).
	 * <p>
	 * The alias-specific lock modes specified here are added to the query's internal
	 * {@link #getLockOptions() LockOptions}.
	 * <p>
	 * The effect of these alias-specific LockModes is somewhat dependent on the driver/database in use.  Generally
	 * speaking, for maximum portability, this method should only be used to mark that the rows corresponding to
	 * the given alias should be included in pessimistic locking ({@link LockMode#PESSIMISTIC_WRITE}).
	 *
	 * @param alias a query alias, or {@code "this"} for a collection filter
	 * @param lockMode The lock mode to apply.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getLockOptions()
	 */
	Query<R> setLockMode(String alias, LockMode lockMode);

	/**
	 * Set a {@link TupleTransformer}
	 */
	Query<R> setTupleTransformer(TupleTransformer<R> transformer);

	/**
	 * Set a {@link ResultListTransformer}
	 */
	Query<R> setResultListTransformer(ResultListTransformer transformer);

	/**
	 * Get the execution options for this Query.  Many of the setter on the Query
	 * contract update the state of the returned {@link QueryOptions}.  This is
	 * important because it gives access to any primitive data in their wrapper
	 * forms rather than the primitive forms as required by JPA.  For example, that
	 * allows use to know whether a specific value has been set at all by the Query
	 * consumer.
	 *
	 * @return Return the encapsulation of this query's options, which includes access to
	 * firstRow, maxRows, timeout and fetchSize, etc.
	 */
	QueryOptions getQueryOptions();

	/**
	 * Access to information about query parameters.
	 *
	 * @return information about query parameters.
	 */
	ParameterMetadata getParameterMetadata();



	/**
	 * Bind the given argument to a named query parameter
	 * <p>
	 * If the type of the parameter cannot be inferred from the context in which
	 * it occurs, use one of the forms accept a "type".
	 *
	 * @see #setParameter(String, Object, Class)
	 * @see #setParameter(String, Object, BindableType)
	 */
	@Override
	Query<R> setParameter(String parameter, Object argument);

	/**
	 * Bind the given argument to a named query parameter using the given
	 * Class reference to attempt to determine the {@link BindableType}
	 * to use.  If unable to determine an appropriate {@link BindableType},
	 * {@link #setParameter(String, Object)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameter(String, Object, BindableType)
	 */
	<P> Query<R> setParameter(String parameter, P argument, Class<P> type);

	/**
	 * Bind the given argument to a named query parameter using the given
	 * {@link BindableType}.
	 *
	 * @see BindableType#parameterType
	 */
	<P> Query<R> setParameter(String parameter, P argument, BindableType<P> type);

	/**
	 * Bind an {@link Instant} value to the named query parameter using just the portion
	 * indicated by the given {@link TemporalType}.
	 */
	Query<R> setParameter(String parameter, Instant argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override
	Query<R> setParameter(String parameter, Calendar argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override
	Query<R> setParameter(String parameter, Date argument, TemporalType temporalType);



	/**
	 * Bind the given argument to an ordinal query parameter.
	 * <p>
	 * If the type of the parameter cannot be inferred from the context in which
	 * it occurs, use one of the forms which accepts a "type".
	 *
	 * @see #setParameter(int, Object, Class)
	 * @see #setParameter(int, Object, BindableType)
	 */
	@Override
	Query<R> setParameter(int parameter, Object argument);

	/**
	 * Bind the given argument to an ordinal query parameter using the given
	 * Class reference to attempt to determine the {@link BindableType}
	 * to use.  If unable to determine an appropriate {@link BindableType},
	 * {@link #setParameter(int, Object)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameter(int, Object, BindableType)
	 */
	<P> Query<R> setParameter(int parameter, P argument, Class<P> type);

	/**
	 * Bind the given argument to an ordinal query parameter using the given
	 * {@link BindableType}.
	 *
	 * @see BindableType#parameterType
	 */
	<P> Query<R> setParameter(int parameter, P argument, BindableType<P> type);

	/**
	 * Bind an {@link Instant} value to the ordinal query parameter using just the portion
	 * indicated by the given {@link TemporalType}.
	 */
	Query<R> setParameter(int parameter, Instant argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override
	Query<R> setParameter(int parameter, Date argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override
	Query<R> setParameter(int parameter, Calendar argument, TemporalType temporalType);

	/**
	 * Bind an argument to the query parameter represented by the given
	 * {@link QueryParameter}.
	 * <p>
	 * If the type of the parameter cannot be inferred from the context in which
	 * it occurs, use one of the forms which accepts a "type".
	 *
	 * @see #setParameter(QueryParameter, Object, BindableType)
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
	 * determine the {@link BindableType} to use.  If unable to determine
	 * an appropriate {@link BindableType}, {@link #setParameter(QueryParameter, Object)} is used
	 *
	 * @param parameter the query parameter memento
	 * @param argument the argument, which might be null
	 * @param type a {@link BindableType} representing the type of the parameter
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameter(QueryParameter, Object, BindableType)
	 */
	<P> Query<R> setParameter(QueryParameter<P> parameter, P argument, Class<P> type);

	/**
	 * Bind an argument to the query parameter represented by the given
	 * {@link QueryParameter} using the given {@link BindableType}.
	 *
	 * @param parameter the query parameter memento
	 * @param argument the argument, which might be null
	 * @param type an {@link BindableType} representing the type of the parameter
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameter(QueryParameter<P> parameter, P argument, BindableType<P> type);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override
	<T> Query<R> setParameter(Parameter<T> parameter, T argument);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override
	Query<R> setParameter(Parameter<Calendar> parameter, Calendar argument, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	@Override
	Query<R> setParameter(Parameter<Date> parameter, Date argument, TemporalType temporalType);



	/**
	 * Bind multiple arguments to a named query parameter.
	 * <p/>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @see #setParameterList(java.lang.String, java.util.Collection, BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(String parameter, @SuppressWarnings("rawtypes") Collection arguments);

	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * Class reference to attempt to determine the {@link BindableType}
	 * to use.  If unable to determine an appropriate {@link BindableType},
	 * {@link #setParameterList(String, Collection)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameterList(java.lang.String, java.util.Collection, BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(String parameter, Collection<? extends P> arguments, Class<P> javaType);

	/**
	 * Bind multiple arguments to a named query parameter using the passed type-mapping.
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(String parameter, Collection<? extends P> arguments, BindableType<P> type);


	/**
	 * Bind multiple arguments to a named query parameter.
	 * <p/>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(String parameter, Object[] values);

	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * Class reference to attempt to determine the {@link BindableType}
	 * to use.  If unable to determine an appropriate {@link BindableType},
	 * {@link #setParameterList(String, Collection)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameterList(java.lang.String, Object[], BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(String parameter, P[] arguments, Class<P> javaType);


	/**
	 * Bind multiple arguments to a named query parameter using the passed type-mapping.
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(String parameter, P[] arguments, BindableType<P> type);

	/**
	 * Bind multiple arguments to an ordinal query parameter.
	 * <p/>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(int parameter, @SuppressWarnings("rawtypes") Collection arguments);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the given
	 * Class reference to attempt to determine the {@link BindableType}
	 * to use.  If unable to determine an appropriate {@link BindableType},
	 * {@link #setParameterList(String, Collection)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameterList(int, Collection, BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(int parameter, Collection<? extends P> arguments, Class<P> javaType);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the passed type-mapping.
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(int parameter, Collection<? extends P> arguments, BindableType<P> type);

	/**
	 * Bind multiple arguments to an ordinal query parameter.
	 * <p/>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(int parameter, Object[] arguments);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the given
	 * Class reference to attempt to determine the {@link BindableType}
	 * to use.  If unable to determine an appropriate {@link BindableType},
	 * {@link #setParameterList(String, Collection)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameterList(int, Object[], BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(int parameter, P[] arguments, Class<P> javaType);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the passed type-mapping.
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(int parameter, P[] arguments, BindableType<P> type);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}.
	 * <p>
	 * The type of the parameter is inferred from the context in which it
	 * occurs, and from the type of the first given argument.
	 *
	 * @param parameter the parameter memento
	 * @param arguments a collection of arguments
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> arguments);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter} using the given Class reference to attempt
	 * to determine the {@link BindableType} to use.  If unable to
	 * determine an appropriate {@link BindableType},
	 * {@link #setParameterList(String, Collection)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameterList(QueryParameter, java.util.Collection, BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> arguments, Class<P> javaType);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}, inferring the {@link BindableType}.
	 *
	 * Bind multiple arguments to a named query parameter.
	 * <p/>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> arguments, BindableType<P> type);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}
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
	 * given {@link QueryParameter} using the given Class reference to attempt
	 * to determine the {@link BindableType} to use.  If unable to
	 * determine an appropriate {@link BindableType},
	 * {@link #setParameterList(String, Collection)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameterList(QueryParameter, Object[], BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(QueryParameter<P> parameter, P[] arguments, Class<P> javaType);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}, inferring the {@link BindableType}.
	 *
	 * Bind multiple arguments to a named query parameter.
	 * <p/>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(QueryParameter<P> parameter, P[] arguments, BindableType<P> type);

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
	 * Bind the values of the given Map for each named parameters of the query,
	 * matching key names with parameter names and mapping value types to
	 * Hibernate types using heuristics.
	 *
	 * @param bean a {@link Map} of names to arguments
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setProperties(@SuppressWarnings("rawtypes") Map bean);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides - CommonQueryContract

	@Override
	Query<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	Query<R> setCacheable(boolean cacheable);

	@Override
	Query<R> setCacheRegion(String cacheRegion);

	@Override
	Query<R> setCacheMode(CacheMode cacheMode);

	@Override
	Query<R> setTimeout(int timeout);

	@Override
	Query<R> setFetchSize(int fetchSize);

	@Override
	Query<R> setReadOnly(boolean readOnly);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides - jakarta.persistence.Query/TypedQuery

	@Override
	Query<R> setMaxResults(int maxResult);

	@Override
	Query<R> setFirstResult(int startPosition);

	@Override
	Query<R> setHint(String hintName, Object value);

	@Override
	Query<R> setFlushMode(FlushModeType flushMode);

	@Override
	Query<R> setLockMode(LockModeType lockMode);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// deprecated methods

	/**
	 * @deprecated (since 5.2) Use {@link #setTupleTransformer} or {@link #setResultListTransformer}
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setResultTransformer(ResultTransformer transformer) {
		setTupleTransformer( transformer );
		setResultListTransformer( transformer );
		return this;
	}

}
