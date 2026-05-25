/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Parameter;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.dialect.Dialect;

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
 * <pre>
 * query.setQueryFlushMode(NO_FLUSH).getResultList()
 * </pre>
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
	@Nonnull
	SharedSessionContract getSession();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	/**
	 * The {@link QueryFlushMode} in effect for this query.
	 * <p>
	 * By default, this is {@link QueryFlushMode#DEFAULT}, and the
	 * {@link FlushMode} of the {@linkplain #getSession owning session}
	 * determines whether the session is flushed before execution of
	 * the query.
	 * <p>
	 * A {@link QueryFlushMode} only affects {@linkplain Session stateful
	 * sessions}. A stateless session has no persistence context to flush.
	 *
	 * @see Session#getHibernateFlushMode()
	 *
	 * @since 7.0
	 */
	@Nonnull
	QueryFlushMode getQueryFlushMode();

	/**
	 * Set the {@link QueryFlushMode} to use for this query.
	 * <p>
	 * A {@link QueryFlushMode} only affects {@linkplain Session stateful
	 * sessions}. A stateless session has no persistence context to flush.
	 *
	 * @see Session#getHibernateFlushMode()
	 *
	 * @since 7.0
	 */
	@Nonnull
	CommonQueryContract setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode);

	/**
	 * The {@link FlushMode} in effect for this query, taking into account
	 * both the {@linkplain #getQueryFlushMode query flush mode} and the
	 * {@linkplain Session#getHibernateFlushMode flush mode of the session}.
	 * By default, the query inherits the {@code FlushMode} of the session
	 * from which it originates.
	 * <p>
	 * A {@link FlushMode} only affects stateful sessions. A stateless
	 * session has no persistence context to flush.
	 *
	 * @see #getQueryFlushMode()
	 * @see Session#getHibernateFlushMode()
	 *
	 * @since 8.0
	 */
	@Incubating
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
	@Nullable
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
	CommonQueryContract setComment(@Nullable String comment);

	/**
	 * Add a database query hint to the SQL query.
	 * <p>
	 * Multiple query hints may be specified. The operation
	 * {@link Dialect#getQueryHintString(String, List)} determines how
	 * the hint is actually added to the SQL query.
	 */
	CommonQueryContract addQueryHint(@Nonnull String hint);

	/**
	 * Obtain the {@linkplain java.sql.Statement#setQueryTimeout JDBC
	 * query timeout} that will be used when executing the query.
	 * <p>
	 * See {@linkplain org.hibernate.Timeouts} for discussion of the
	 * "magic values" accepted here.
	 *
	 * @apiNote Since this method is inherited from JPA, the timeout
	 *          is expressed in <em>milliseconds</em>.
	 *
	 * @see java.sql.Statement#getQueryTimeout()
	 * @see java.sql.Statement#setQueryTimeout(int)
	 */
	@Override
	@Nullable
	Integer getTimeout();

	/**
	 * Specify a {@linkplain java.sql.Statement#setQueryTimeout JDBC
	 * query timeout} to use when executing the query.
	 *
	 * @apiNote Since this is a legacy method of Hibernate, the timeout
	 *          value is expressed in <em>seconds</em>.
	 *
	 * @see #getTimeout()
	 * @see org.hibernate.Timeouts
	 * @see #setTimeout(Timeout)
	 */
	@Nonnull
	CommonQueryContract setTimeout(int timeout);

	/**
	 * Specify a {@linkplain java.sql.Statement#setQueryTimeout JDBC
	 * query timeout} to use when executing the query.
	 *
	 * @apiNote Since this method is inherited from JPA, the timeout
	 *          is expressed in <em>milliseconds</em>.
	 *
	 * @see #getTimeout()
	 * @see org.hibernate.Timeouts
	 * @see #setTimeout(Timeout)
	 *
	 * @since 7.0
	 */
	@Override
	@Nonnull
	CommonQueryContract setTimeout(@Nullable Integer timeout);

	/**
	 * Specify a {@linkplain java.sql.Statement#setQueryTimeout JDBC
	 * query timeout} to use when executing the query.
	 *
	 * @since 7.0
	 */
	@Override
	@Nonnull
	CommonQueryContract setTimeout(@Nullable Timeout timeout);

	/**
	 * Set a hint. Hints are a
	 * {@linkplain jakarta.persistence.Query#setHint JPA-standard way}
	 * to control provider-specific behavior affecting execution of the
	 * query. Clients of this native Hibernate API should make use of
	 * type safe operations of this interface and of its subtypes.
	 * For example, {@link SelectionQuery#setCacheRegion} is preferred
	 * over {@link org.hibernate.jpa.HibernateHints#HINT_CACHE_REGION}.
	 * <p>
	 * Since JPA 4, {@link jakarta.persistence.TypedQuery.Option} and
	 * {@link jakarta.persistence.Statement.Option} compete with query
	 * hints, offering a more type safe way to accommodate vendor
	 * extensions.
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
	@Nonnull
	CommonQueryContract setHint(@Nonnull String hintName, @Nullable Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter Handling

	/**
	 * Get the {@link ParameterMetadata} object representing the parameters
	 * of this query, and providing access to the {@link QueryParameter}s.
	 *
	 * @since 7.0
	 */
	@Nonnull
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
	@Nonnull
	<T> CommonQueryContract setParameter(@Nonnull QueryParameter<T> parameter, @Nullable T value);

	/**
	 * @see jakarta.persistence.Query#setParameter(Parameter, Object)
	 */
	@Nonnull
	<T> CommonQueryContract setParameter(@Nonnull Parameter<T> param, @Nullable T value);

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
	@Nonnull
	CommonQueryContract setParameter(@Nonnull String parameter, @Nullable Object value);

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
	@Nonnull
	CommonQueryContract setParameter(int parameter, @Nullable Object value);

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
	@Nonnull
	<P> CommonQueryContract setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value, @Nonnull Class<P> type);

	/**
	 * Bind the given argument to a named query parameter using the given
	 * {@link Class} reference to attempt to infer the {@link Type}.
	 * If unable to infer an appropriate {@link Type}, fall back to
	 * {@link #setParameter(String, Object)}.
	 *
	 * @see #setParameter(String, Object, Type)
	 * @see #setParameter(int, Object, Class)
	 */
	@Nonnull
	<P> CommonQueryContract setParameter(@Nonnull String parameter, @Nullable P value, @Nonnull Class<P> type);

	/**
	 * Bind the given argument to an ordinal query parameter using the given
	 * {@link Class} reference to attempt to infer the {@link Type}.
	 * If unable to infer an appropriate {@link Type}, fall back to
	 * {@link #setParameter(int, Object)}.
	 *
	 * @see #setParameter(int, Object, Type)
	 */
	@Nonnull
	<P> CommonQueryContract setParameter(int parameter, @Nullable P value, @Nonnull Class<P> type);

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
	@Nonnull
	<P> CommonQueryContract setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P val, @Nonnull Type<P> type);

	/**
	 * Bind the given argument to a named query parameter using the given
	 * {@link Type}.
	 */
	@Nonnull
	<P> CommonQueryContract setParameter(@Nonnull String parameter, @Nullable P value, @Nonnull Type<P> type);

	/**
	 * Bind the given argument to an ordinal query parameter using the given
	 * {@link Type}.
	 */
	@Nonnull
	<P> CommonQueryContract setParameter(int parameter, @Nullable P value, @Nonnull Type<P> type);

	/**
	 * @see jakarta.persistence.Query#setConvertedParameter(String, Object, Class)
	 */
	@Nonnull
	<P> CommonQueryContract setConvertedParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	/**
	 * @see jakarta.persistence.Query#setConvertedParameter(int, Object, Class)
	 */
	@Nonnull
	<P> CommonQueryContract setConvertedParameter(int position, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

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
	@Nonnull
	CommonQueryContract setParameterList(@Nonnull String parameter, @SuppressWarnings("rawtypes") @Nonnull Collection values);

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
	@Nonnull
	<P> CommonQueryContract setParameterList(@Nonnull String parameter, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	<P> CommonQueryContract setParameterList(@Nonnull String parameter, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

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
	@Nonnull
	CommonQueryContract setParameterList(@Nonnull String parameter, @Nonnull Object[] values);

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
	@Nonnull
	<P> CommonQueryContract setParameterList(@Nonnull String parameter, @Nonnull P[] values, @Nonnull Class<P> javaType);

	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	<P> CommonQueryContract setParameterList(@Nonnull String parameter, @Nonnull P[] values, @Nonnull Type<P> type);

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
	@Nonnull
	CommonQueryContract setParameterList(int parameter, @SuppressWarnings("rawtypes") @Nonnull Collection values);

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
	@Nonnull
	<P> CommonQueryContract setParameterList(int parameter, @Nonnull Collection<? extends P> values, Class<P> javaType);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the given
	 * {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	<P> CommonQueryContract setParameterList(int parameter, @Nonnull Collection<? extends P> values, Type<P> type);

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
	@Nonnull
	CommonQueryContract setParameterList(int parameter, @Nonnull Object[] values);

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
	@Nonnull
	<P> CommonQueryContract setParameterList(int parameter, @Nonnull P[] values, @Nonnull Class<P> javaType);

	/**
	 * Bind multiple arguments to an ordinal query parameter using the given
	 * {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	<P> CommonQueryContract setParameterList(int parameter, @Nonnull P[] values, @Nonnull Type<P> type);

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
	@Nonnull
	<P> CommonQueryContract setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values);

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
	@Nonnull
	<P> CommonQueryContract setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}, using the given {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	<P> CommonQueryContract setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type);

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
	@Nonnull
	<P> CommonQueryContract setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values);

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
	@Nonnull
	<P> CommonQueryContract setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Class<P> javaType);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}, using the given the {@link Type}.
	 *
	 * @apiNote This is used for binding a list of values to an expression
	 * such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	<P> CommonQueryContract setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Type<P> type);

	/**
	 * Bind the property values of the given bean to named parameters of
	 * the query, matching property names with parameter names and mapping
	 * property types to Hibernate types using heuristics.
	 *
	 * @param bean any JavaBean or POJO
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	CommonQueryContract setProperties(@Nonnull Object bean);

	/**
	 * Bind the values of the given {@code Map} to named parameters of the
	 * query, matching key names with parameter names and mapping value types
	 * to Hibernate types using heuristics.
	 *
	 * @param bean a {@link Map} of names to arguments
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	CommonQueryContract setProperties(@SuppressWarnings("rawtypes") @Nonnull Map bean);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	/**
	 * Bind an {@link Instant} to the named query parameter using just the
	 * portion indicated by the given {@link TemporalType}.
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	@Nonnull
	CommonQueryContract setParameter(@Nonnull String parameter, @Nullable Instant value, @Nonnull TemporalType temporalType);

	/**
	 * @see jakarta.persistence.Query#setParameter(String, Calendar, TemporalType)
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	@Nonnull
	CommonQueryContract setParameter(@Nonnull String parameter, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	/**
	 * @see jakarta.persistence.Query#setParameter(String, Date, TemporalType)
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	@Nonnull
	CommonQueryContract setParameter(@Nonnull String parameter, @Nullable Date value, @Nonnull TemporalType temporalType);
	/**
	 * Bind an {@link Instant} to an ordinal query parameter using just the
	 * portion indicated by the given {@link TemporalType}.
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	@Nonnull
	CommonQueryContract setParameter(int parameter, @Nullable Instant value, @Nonnull TemporalType temporalType);

	/**
	 * @see jakarta.persistence.Query#setParameter(int, Date, TemporalType)
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	@Nonnull
	CommonQueryContract setParameter(int parameter, @Nullable Date value, @Nonnull TemporalType temporalType);

	/**
	 * @see jakarta.persistence.Query#setParameter(int, Calendar, TemporalType)
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	@Nonnull
	CommonQueryContract setParameter(int parameter, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	/**
	 * @see jakarta.persistence.Query#setParameter(Parameter, Calendar, TemporalType)
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	@Nonnull
	CommonQueryContract setParameter(@Nonnull Parameter<Calendar> param, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	/**
	 * @see jakarta.persistence.Query#setParameter(Parameter, Date, TemporalType)
	 *
	 * @deprecated since {@link TemporalType} is deprecated
	 */
	@Deprecated(since = "7")
	@Nonnull
	CommonQueryContract setParameter(@Nonnull Parameter<Date> param, @Nullable Date value, @Nonnull TemporalType temporalType);
}
