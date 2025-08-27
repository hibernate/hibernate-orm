/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import jakarta.persistence.Timeout;
import org.hibernate.FlushMode;
import org.hibernate.Session;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.metamodel.Type;

/**
 * Defines the aspects of query execution and parameter binding that apply to all
 * forms of querying:
 * <ul>
 * <li>queries written in HQL or JPQL,
 * <li>queries written in the native SQL dialect of the database,
 * <li>{@linkplain jakarta.persistence.criteria.CriteriaBuilder criteria queries},
 *     and
 * <li>{@linkplain org.hibernate.procedure.ProcedureCall stored procedure calls}.
 * </ul>
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
 * @see jakarta.persistence.Query
 * @see SelectionQuery
 * @see MutationQuery
 */
public interface CommonQueryContract {

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
	 * The JPA {@link FlushModeType} in effect for this query.  By default, the
	 * query inherits the {@link FlushMode} of the {@link Session} from which
	 * it originates.
	 *
	 * @see #getQueryFlushMode()
	 * @see #getHibernateFlushMode()
	 * @see Session#getHibernateFlushMode()
	 *
	 * @deprecated use {@link #getQueryFlushMode()}
	 */
	@Deprecated(since = "7")
	FlushModeType getFlushMode();

	/**
	 * Set the {@link FlushMode} to use for this query.
	 * <p>
	 * Setting this to {@code null} ultimately indicates to use the
	 * {@link FlushMode} of the session. Use {@link #setHibernateFlushMode}
	 * passing {@link FlushMode#MANUAL} instead to indicate that no automatic
	 * flushing should occur.
	 *
	 * @see #getQueryFlushMode()
	 * @see #getHibernateFlushMode()
	 * @see Session#getHibernateFlushMode()
	 *
	 * @deprecated use {@link #setQueryFlushMode(QueryFlushMode)}
	 */
	@Deprecated(since = "7")
	CommonQueryContract setFlushMode(FlushModeType flushMode);

	/**
	 * The {@link FlushMode} in effect for this query. By default, the query
	 * inherits the {@code FlushMode} of the {@link Session} from which it
	 * originates.
	 *
	 * @see #getQueryFlushMode()
	 * @see Session#getHibernateFlushMode()
	 *
	 * @deprecated use {@link #getQueryFlushMode()}
	 */
	@Deprecated(since = "7")
	FlushMode getHibernateFlushMode();

	/**
	 * Set the current {@link FlushMode} in effect for this query.
	 *
	 * @implNote Setting to {@code null} ultimately indicates to use the
	 * {@link FlushMode} of the session. Use {@link FlushMode#MANUAL}
	 * instead to indicate that no automatic flushing should occur.
	 *
	 * @see #getQueryFlushMode()
	 * @see #getHibernateFlushMode()
	 * @see Session#getHibernateFlushMode()
	 *
	 * @deprecated use {@link #setQueryFlushMode(QueryFlushMode)}
	 */
	@Deprecated(since = "7")
	CommonQueryContract setHibernateFlushMode(FlushMode flushMode);

	/**
	 * Obtain the query timeout <em>in seconds</em>.
	 * <p>
	 * This value is eventually passed along to the JDBC statement via
	 * {@link java.sql.Statement#setQueryTimeout(int)}.
	 * <p>
	 * A value of zero indicates no timeout.
	 *
	 * @see java.sql.Statement#getQueryTimeout()
	 * @see java.sql.Statement#setQueryTimeout(int)
	 */
	Integer getTimeout();

	/**
	 * Set the query timeout <em>in seconds</em>.
	 * <p>
	 * Any value set here is eventually passed directly along to the
	 * {@linkplain java.sql.Statement#setQueryTimeout(int) JDBC
	 * statement}, which expressly disallows negative values.  So
	 * negative values should be avoided <em>as a general rule</em>,
	 * although certain "magic values" are handled - see
	 * {@linkplain org.hibernate.Timeouts#NO_WAIT}.
	 * <p>
	 * A value of zero indicates no timeout.
	 *
	 * @param timeout the timeout <em>in seconds</em>
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.Timeouts
	 * @see #setTimeout(Timeout)
	 * @see #getTimeout()
	 */
	CommonQueryContract setTimeout(int timeout);

	/**
	 * Apply a timeout to the corresponding database query.
	 *
	 * @param timeout The timeout to apply
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonQueryContract setTimeout(Timeout timeout);

	/**
	 * Get the comment that has been set for this query, if any.
	 */
	String getComment();

	/**
	 * Set a comment for this query.
	 *
	 * @see Query#setComment(String)
	 */
	CommonQueryContract setComment(String comment);

	/**
	 * Set a hint. The hints understood by Hibernate are enumerated by
	 * {@link org.hibernate.jpa.AvailableHints}.
	 *
	 * @see org.hibernate.jpa.HibernateHints
	 * @see org.hibernate.jpa.SpecHints
	 *
	 * @apiNote Hints are a
	 * {@linkplain jakarta.persistence.Query#setHint(String, Object)
	 * JPA-standard way} to control provider-specific behavior
	 * affecting execution of the query. Clients of the native API
	 * defined by Hibernate should make use of type-safe operations
	 * of this interface and of its subtypes. For example,
	 * {@link SelectionQuery#setCacheRegion} is preferred over
	 * {@link org.hibernate.jpa.HibernateHints#HINT_CACHE_REGION}.
	 */
	CommonQueryContract setHint(String hintName, Object value);

	/**
	 * Get the {@link ParameterMetadata} object representing the parameters
	 * of this query, and providing access to the {@link QueryParameter}s.
	 *
	 * @since 7.0
	 */
	ParameterMetadata getParameterMetadata();

	/**
	 * Bind the given argument to a named query parameter.
	 * <p>
	 * If the type of the parameter cannot be inferred from the context in
	 * which it occurs, use one of the overloads which accepts a "type",
	 * or pass a {@link TypedParameterValue}.
	 *
	 * @see #setParameter(String, Object, Class)
	 * @see #setParameter(String, Object, Type)
	 *
	 * @see TypedParameterValue
	 */
	CommonQueryContract setParameter(String parameter, Object value);

	/**
	 * Bind the given argument to a named query parameter using the given
	 * {@link Class} reference to attempt to infer the {@link Type}.
	 * If unable to infer an appropriate {@link Type}, fall back to
	 * {@link #setParameter(String, Object)}.
	 *
	 * @see #setParameter(String, Object, Type)
	 */
	<P> CommonQueryContract setParameter(String parameter, P value, Class<P> type);

	/**
	 * Bind the given argument to a named query parameter using the given
	 * {@link Type}.
	 */
	<P> CommonQueryContract setParameter(String parameter, P value, Type<P> type);

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
	 * Bind the given argument to an ordinal query parameter.
	 * <p>
	 * If the type of the parameter cannot be inferred from the context in
	 * which it occurs, use one of the overloads which accepts a "type",
	 * or pass a {@link TypedParameterValue}.
	 *
	 * @see #setParameter(int, Object, Class)
	 * @see #setParameter(int, Object, Type)
	 *
	 * @see TypedParameterValue
	 */
	CommonQueryContract setParameter(int parameter, Object value);

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
	 * Bind the given argument to an ordinal query parameter using the given
	 * {@link Type}.
	 */
	<P> CommonQueryContract setParameter(int parameter, P value, Type<P> type);

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
	 * @see jakarta.persistence.Query#setParameter(Parameter, Object)
	 */
	<T> CommonQueryContract setParameter(Parameter<T> param, T value);

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
}
