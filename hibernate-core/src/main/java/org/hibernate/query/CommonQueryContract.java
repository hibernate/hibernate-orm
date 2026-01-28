/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.dialect.Dialect;
import org.hibernate.graph.GraphSemantic;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Defines the aspects of query execution and parameter binding that apply to all
 * forms of querying:
 * <ul>
 * <li>queries written in HQL,  or JPQL,
 * <li>queries written in the native SQL dialect of the database,
 * <li>{@linkplain jakarta.persistence.criteria.CriteriaBuilder criteria queries},
 *     and
 * <li>{@linkplain org.hibernate.procedure.ProcedureCall stored procedure calls}.
 * </ul>
 * <p>
 * Also acts as the primary extension point for the Jakarta Persistence
 * {@linkplain jakarta.persistence.Query} hierarchy.
 * <p>
 * Queries may have <em>parameters</em>, either ordinal or named, and the various
 * {@code setParameter()} operations of this interface allow an argument to be
 * bound to a parameter. It's not usually necessary to explicitly specify the type
 * of an argument, but in rare cases where this is needed:
 * <ul>
 * <li>an instance of an appropriate metamodel {@link Type} may be passed to
 *     {@link #setParameter(int, Object, Type)}, or
 * <li>the argument may be wrapped in a {@link TypedParameterValue}. (For JPA users,
 *     this second option avoids the need to cast the {@link jakarta.persistence.Query}
 *     to a Hibernate-specific type.)
 * </ul>
 * <p>
 * For example:
 * <pre>
 * session.createSelectionQuery("from Person where address = :address", Person.class)
 *         .setParameter("address", address, Person_.address.getType())
 *         .getResultList()
 * </pre>
 * <pre>
 * entityManager.createQuery( "from Person where address = :address", Person.class)
 *         .setParameter("address", TypedParameterValue.of(Person_.address.getType(), address))
 *         .getResultList()
 * </pre>
 * <p>
 * The operation {@link #setQueryFlushMode(QueryFlushMode)} allows a temporary flush
 * mode to be specified, which is in effect only during the execution of this query.
 * Setting the {@linkplain QueryFlushMode query flush mode} does not affect the flush
 * mode of other operations performed via the parent {@linkplain Session session}.
 * This operation is usually used as follows:
 * <p>
 * <pre>query.setQueryFlushMode(NO_FLUSH).getResultList()</pre>
 * <p>
 * The call to {@code setQueryFlushMode(NO_FLUSH)} disables the usual automatic flush
 * operation that occurs before query execution.
 *
 * @author Steve Ebersole
 * @author Gavin King
 *
 * @see SelectionQuery
 * @see MutationQuery
 * @see org.hibernate.procedure.ProcedureCall
 */
public interface CommonQueryContract extends jakarta.persistence.Query {

	/**
	 * Get the {@link org.hibernate.Session} or
	 * {@link org.hibernate.StatelessSession} that was used to create
	 * this {@code Query} instance.
	 *
	 * @return The producer of this query
	 */
	SharedSessionContract getSession();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	/**
	 * The {@link QueryFlushMode} in effect for this query.
	 * <p>
	 * By default, this is {@link QueryFlushMode#DEFAULT}, and the
	 * {@link FlushMode} of the owning {@link Session} determines whether
	 * it is flushed.
	 *
	 * @see Session#getHibernateFlushMode()
	 *
	 * @since 7.0
	 */
	QueryFlushMode getQueryFlushMode();

	/**
	 * Set the {@link QueryFlushMode} to use for this query.
	 *
	 * @see Session#getHibernateFlushMode()
	 *
	 * @since 7.0
	 */
	CommonQueryContract setQueryFlushMode(QueryFlushMode queryFlushMode);

	/**
	 * The {@link FlushMode} in effect for this query. By default, the query
	 * inherits the {@code FlushMode} of the {@link Session} from which it
	 * originates.
	 *
	 * @see #getQueryFlushMode()
	 * @see Session#getHibernateFlushMode()
	 */
	FlushMode getEffectiveFlushMode();

	/**
	 * Obtain the comment currently associated with this query.
	 * <p>
	 * If SQL commenting is enabled, the comment will be added to the SQL
	 * query sent to the database, which may be useful for identifying the
	 * source of troublesome queries.
	 * <p>
	 * SQL commenting may be enabled using the configuration property
	 * {@value org.hibernate.cfg.AvailableSettings#USE_SQL_COMMENTS}.
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
	 * @see #getComment()
	 */
	CommonQueryContract setComment(String comment);

	/**
	 * Add a database query hint to the SQL query.
	 * <p>
	 * Multiple query hints may be specified. The operation
	 * {@link Dialect#getQueryHintString(String, List)} determines how
	 * the hint is actually added to the SQL query.
	 */
	CommonQueryContract addQueryHint(String hint);

	/**
	 * Obtain the query timeout to be applied to the corresponding database query.
	 * <p>
	 * See {@linkplain org.hibernate.Timeouts} for discussion of "magic values".
	 *
	 * @apiNote As this method is inherited from JPA, the value expected to be <em>in milliseconds</em>.
	 * @implNote This value is eventually passed along to the JDBC statement via
	 * {@link java.sql.Statement#setQueryTimeout(int)}.
	 *
	 * @see java.sql.Statement#getQueryTimeout()
	 * @see java.sql.Statement#setQueryTimeout(int)
	 */
	@Override
	Integer getTimeout();

	/**
	 * Apply a timeout to the corresponding database query.
	 *
	 * @apiNote As a legacy Hibernate method, this form expects a value <em>in seconds</em>.
	 *
	 * @see #getTimeout()
	 * @see org.hibernate.Timeouts
	 * @see #setTimeout(Timeout)
	 */
	CommonQueryContract setTimeout(int timeout);

	/**
	 * Apply a timeout to the corresponding database query.
	 *
	 * @apiNote As this method is inherited from JPA, the value expected to be <em>in milliseconds</em>.
	 *
	 * @see #getTimeout()
	 * @see org.hibernate.Timeouts
	 * @see #setTimeout(Timeout)
	 */
	@Override
	CommonQueryContract setTimeout(Integer timeout);

	/**
	 * Apply a timeout to the corresponding database query.
	 */
	@Override
	CommonQueryContract setTimeout(Timeout timeout);

	/**
	 * Set a hint. Hints are a
	 * {@linkplain jakarta.persistence.Query#setHint JPA-standard way}
	 * to control provider-specific behavior affecting execution of the
	 * query. Clients of native Hibernate API should make use of type-safe
	 * operations of this interface and of its subtypes. For example,
	 * {@link SelectionQuery#setCacheRegion} is preferred over
	 * {@link org.hibernate.jpa.HibernateHints#HINT_CACHE_REGION}.
	 * <p>
	 * The hints understood by Hibernate are enumerated by
	 * {@link org.hibernate.jpa.AvailableHints}.
	 *
	 * @see org.hibernate.jpa.HibernateHints
	 * @see org.hibernate.jpa.SpecHints
	 *
	 * @apiNote Very different from {@linkplain #addQueryHint(String)}
	 * which defines database hints to be applied to the SQL.
	 *
	 */
	CommonQueryContract setHint(String hintName, Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Casts

	/**
	 * Casts this query as a {@code SelectionQuery}.
	 *
	 * @throws IllegalSelectQueryException If the query is not a select query.
	 */
	SelectionQuery<?> asSelectionQuery();

	/**
	 * Casts this query as a {@code SelectionQuery} with the given result type.
	 *
	 * @throws IllegalSelectQueryException If the query is not a select query.
	 * @throws IllegalArgumentException If the given {@code type} is not compatible with the query's defined result type.
	 */
	<R> SelectionQuery<R> asSelectionQuery(Class<R> type);

	/**
	 * Casts this query as a {@code SelectionQuery} with the given result graph.
	 *
	 * @throws IllegalSelectQueryException If the query is not a selection query.
	 * @throws IllegalArgumentException Is the given graph result type is not compatible with the {@code Query} type parameter.
	 */
	<X> SelectionQuery<X> asSelectionQuery(EntityGraph<X> entityGraph);

	/**
	 * Overload of {@linkplain #withEntityGraph(EntityGraph)} allowing a specific semantic
	 * (load/fetch) for the graph.
	 *
	 * @param entityGraph The entity graph.
	 * @param graphSemantic The load/fetch semantic.
	 * @return The cast/converted query.
	 *
	 * @see SharedSessionContract#createSelectionQuery(String, EntityGraph)
	 * @see SharedSessionContract#createQuery(String, EntityGraph)
	 * @see #asSelectionQuery(Class)
	 */
	<X> SelectionQuery<X> asSelectionQuery(EntityGraph<X> entityGraph, GraphSemantic graphSemantic);

	/**
	 * Casts this query as a mutation query.
	 *
	 * @throws IllegalMutationQueryException If the query is not a mutation query.
	 */
	MutationQuery asMutationQuery();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter Handling

	/**
	 * Get the {@link ParameterMetadata} object representing the parameters
	 * of this query, and providing access to the {@link QueryParameter}s.
	 *
	 * @since 7.0
	 */
	ParameterMetadata getParameterMetadata();

	/**
	 * Bind an argument to the query parameter represented by the given
	 * {@link QueryParameter}.
	 * <p>
	 * If the type of the parameter cannot be inferred from the context in
	 * which it occurs, use one of the overloads which accepts a "type".
	 *
	 * @see #setParameter(QueryParameter, Object, Type)
	 *
	 * @param parameter the query parameter memento
	 * @param value the argument, which might be null
	 *
	 * @return {@code this}, for method chaining
	 */
	<T> CommonQueryContract setParameter(QueryParameter<T> parameter, T value);

	/**
	 * @see jakarta.persistence.Query#setParameter(Parameter, Object)
	 */
	<T> CommonQueryContract setParameter(Parameter<T> param, T value);

	/**
	 * Bind the given argument to a named query parameter.
	 * <p>
	 * If the type of the parameter cannot be inferred from the context in
	 * which it occurs, use one of the overloads which accepts a "type",
	 * or pass a {@link TypedParameterValue}.
	 *
	 * @see #setParameter(String, Object, Class)
	 * @see #setParameter(String, Object, Type)
	 * @see #setParameter(int, Object)
	 *
	 * @see TypedParameterValue
	 */
	CommonQueryContract setParameter(String parameter, Object value);

	/**
	 * Bind the given argument to an ordinal query parameter.
	 * <p>
	 * If the type of the parameter cannot be inferred from the context in
	 * which it occurs, use one of the overloads which accepts a "type",
	 * or pass a {@link TypedParameterValue}.
	 *
	 * @see #setParameter(int, Object, Class)
	 * @see #setParameter(int, Object, Type)
	 * @see #setParameter(String, Object)
	 *
	 * @see TypedParameterValue
	 */
	CommonQueryContract setParameter(int parameter, Object value);

	/**
	 * Bind an argument to the query parameter represented by the given
	 * {@link QueryParameter}, using the given {@link Class} reference to attempt
	 * to infer the {@link Type} to use.  If unable to infer an appropriate
	 * {@link Type}, fall back to {@link #setParameter(QueryParameter, Object)}.
	 *
	 * @param parameter the query parameter memento
	 * @param value the argument, which might be null
	 * @param type a {@link Type} representing the type of the parameter
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #setParameter(QueryParameter, Object, Type)
	 */
	<P> CommonQueryContract setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	/**
	 * Bind the given argument to a named query parameter using the given
	 * {@link Class} reference to attempt to infer the {@link Type}.
	 * If unable to infer an appropriate {@link Type}, fall back to
	 * {@link #setParameter(String, Object)}.
	 *
	 * @see #setParameter(String, Object, Type)
	 * @see #setParameter(int, Object, Class)
	 */
	<P> CommonQueryContract setParameter(String parameter, P value, Class<P> type);

	/**
	 * Bind the given argument to an ordinal query parameter using the given
	 * {@link Class} reference to attempt to infer the {@link Type}.
	 * If unable to infer an appropriate {@link Type}, fall back to
	 * {@link #setParameter(int, Object)}.
	 *
	 * @see #setParameter(int, Object, Type)
	 */
	<P> CommonQueryContract setParameter(int parameter, P value, Class<P> type);

	/**
	 * Bind an argument to the query parameter represented by the given
	 * {@link QueryParameter}, using the given {@link Type}.
	 *
	 * @param parameter the query parameter memento
	 * @param val the argument, which might be null
	 * @param type a {@link Type} representing the type of the parameter
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameter(QueryParameter<P> parameter, P val, Type<P> type);

	/**
	 * Bind the given argument to a named query parameter using the given
	 * {@link Type}.
	 */
	<P> CommonQueryContract setParameter(String parameter, P value, Type<P> type);

	/**
	 * Bind the given argument to an ordinal query parameter using the given
	 * {@link Type}.
	 */
	<P> CommonQueryContract setParameter(int parameter, P value, Type<P> type);

	/**
	 * @see jakarta.persistence.Query#setConvertedParameter(String, Object, Class)
	 */
	<P> CommonQueryContract setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter);

	/**
	 * @see jakarta.persistence.Query#setConvertedParameter(int, Object, Class)
	 */
	<P> CommonQueryContract setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter);

	/**
	 * Bind multiple arguments to a named query parameter.
	 * <p>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element.
	 *
	 * @see #setParameterList(java.lang.String, java.util.Collection, Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonQueryContract setParameterList(String parameter, @SuppressWarnings("rawtypes") Collection values);

	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * {@link Class} reference to attempt to infer the {@link Type}
	 * If unable to infer an appropriate {@link Type}, fall back to
	 * {@link #setParameterList(String, Collection)}.
	 *
	 * @see #setParameterList(java.lang.String, java.util.Collection, Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(String parameter, Collection<? extends P> values, Class<P> javaType);

	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(String parameter, Collection<? extends P> values, Type<P> type);

	/**
	 * Bind multiple arguments to a named query parameter.
	 * <p>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonQueryContract setParameterList(String parameter, Object[] values);

	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * Class reference to attempt to determine the {@link Type}
	 * to use.  If unable to determine an appropriate {@link Type},
	 * {@link #setParameterList(String, Collection)} is used
	 *
	 * @see #setParameterList(java.lang.String, Object[], Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(String parameter, P[] values, Class<P> javaType);

	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(String parameter, P[] values, Type<P> type);

	/**
	 * Bind multiple arguments to an ordinal query parameter.
	 * <p>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonQueryContract setParameterList(int parameter, @SuppressWarnings("rawtypes") Collection values);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the given
	 * {@link Class} reference to attempt to infer the {@link Type}.
	 * If unable to infer an appropriate {@link Type}, fall back to
	 * {@link #setParameterList(String, Collection)}.
	 *
	 * @see #setParameterList(int, Collection, Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(int parameter, Collection<? extends P> values, Class<P> javaType);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the given
	 * {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(int parameter, Collection<? extends P> values, Type<P> type);

	/**
	 * Bind multiple arguments to an ordinal query parameter.
	 * <p>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonQueryContract setParameterList(int parameter, Object[] values);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the given
	 * {@link Class} reference to attempt to infer the {@link Type}.
	 * If unable to infer an appropriate {@link Type}, fall back to
	 * {@link #setParameterList(String, Collection)}.
	 *
	 * @see #setParameterList(int, Object[], Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(int parameter, P[] values, Class<P> javaType);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the given
	 * {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(int parameter, P[] values, Type<P> type);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}.
	 * <p>
	 * The type of the parameter is inferred from the context in which it
	 * occurs, and from the type of the first given argument.
	 *
	 * @param parameter the parameter memento
	 * @param values a collection of arguments
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter} using the given {@link Class} reference
	 * to attempt to infer the {@link Type} to use.  If unable to
	 * infer an appropriate {@link Type}, fall back to using
	 * {@link #setParameterList(String, Collection)}.
	 *
	 * @see #setParameterList(QueryParameter, java.util.Collection, Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}, using the given {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}.
	 * <p>
	 * The type of the parameter is inferred between the context in which it
	 * occurs, the type associated with the {@code QueryParameter} and the
	 * type of the first given argument.
	 *
	 * @param parameter the parameter memento
	 * @param values a collection of arguments
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(QueryParameter<P> parameter, P[] values);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter} using the given {@link Class} reference
	 * to attempt to infer the {@link Type} to use.  If unable to
	 * infer an appropriate {@link Type}, fall back to using
	 * {@link #setParameterList(String, Collection)}.
	 *
	 * @see #setParameterList(QueryParameter, Object[], Type)
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}, using the given the {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type);

	/**
	 * Bind the property values of the given bean to named parameters of
	 * the query, matching property names with parameter names and mapping
	 * property types to Hibernate types using heuristics.
	 *
	 * @param bean any JavaBean or POJO
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonQueryContract setProperties(Object bean);

	/**
	 * Bind the values of the given {@code Map} to named parameters of the
	 * query, matching key names with parameter names and mapping value types
	 * to Hibernate types using heuristics.
	 *
	 * @param bean a {@link Map} of names to arguments
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonQueryContract setProperties(@SuppressWarnings("rawtypes") Map bean);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	/**
	 * Bind an {@link Instant} to the named query parameter using just the
	 * portion indicated by the given {@link TemporalType}.
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	CommonQueryContract setParameter(String parameter, Instant value, TemporalType temporalType);

	/**
	 * @see jakarta.persistence.Query#setParameter(String, Calendar, TemporalType)
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	CommonQueryContract setParameter(String parameter, Calendar value, TemporalType temporalType);

	/**
	 * @see jakarta.persistence.Query#setParameter(String, Date, TemporalType)
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	CommonQueryContract setParameter(String parameter, Date value, TemporalType temporalType);
	/**
	 * Bind an {@link Instant} to an ordinal query parameter using just the
	 * portion indicated by the given {@link TemporalType}.
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	CommonQueryContract setParameter(int parameter, Instant value, TemporalType temporalType);

	/**
	 * @see jakarta.persistence.Query#setParameter(int, Date, TemporalType)
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	CommonQueryContract setParameter(int parameter, Date value, TemporalType temporalType);

	/**
	 * @see jakarta.persistence.Query#setParameter(int, Calendar, TemporalType)
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	CommonQueryContract setParameter(int parameter, Calendar value, TemporalType temporalType);

	/**
	 * @see jakarta.persistence.Query#setParameter(Parameter, Calendar, TemporalType)
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	CommonQueryContract setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	/**
	 * @see jakarta.persistence.Query#setParameter(Parameter, Date, TemporalType)
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	CommonQueryContract setParameter(Parameter<Date> param, Date value, TemporalType temporalType);
}
