/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.hibernate.Incubating;
import org.hibernate.MappingException;
import org.hibernate.query.CommonQueryContract;
import jakarta.persistence.QueryFlushMode;
import org.hibernate.query.SynchronizeableQuery;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Defines support for executing database stored procedures and functions using the
 * {@linkplain java.sql.CallableStatement JDBC stored procedure SQL escape syntax}.
 * <p>
 * Here we use the terms "procedure" and "function" as follows:<ul>
 *     <li>A <em>procedure</em> is a named database executable called via:
 *         {@code {call procedureName(...)}}</li>
 *     <li>A <em>function</em> is a named database executable called via:
 *         {@code {? = call functionName(...)}}</li>
 * </ul>
 * <p>
 * Unless explicitly specified, the {@code ProcedureCall} is executed using the
 * procedure call syntax. To explicitly specify that the function call syntax
 * should be used, call {@link #markAsFunctionCall}. Clients of the JPA-standard
 * {@link StoredProcedureQuery} interface may choose between:
 * <ul>
 * <li>using {@link #unwrap storedProcedureQuery.unwrap(ProcedureCall.class).markAsFunctionCall(returnType)},
 *     or
 * <li>setting the {@value org.hibernate.jpa.HibernateHints#HINT_CALLABLE_FUNCTION}
 *     or {@value org.hibernate.jpa.HibernateHints#HINT_CALLABLE_FUNCTION_RETURN_TYPE}
 *     {@linkplain #setHint(String, Object) hint} to avoid the cast to a
 *     Hibernate-specific class.
 * </ul>
 * <p>
 * When the function call syntax is used:
 * <ul>
 * <li>parameters must be registered by position (not name),
 * <li>the first parameter is considered to represent the function return value
 *     (corresponding to the {@code ?} which occurs before the {@code =}), and
 * <li>the first parameter must have {@linkplain ParameterMode mode} OUT, INOUT,
 *     or REF_CURSOR; {@linkplain ParameterMode#IN IN} is illegal.
 * </ul>
 * <p>
 * Depending on the {@linkplain org.hibernate.dialect.Dialect SQL dialect},
 * further constraints are enforced or inferred. For example, on PostgreSQL:
 * <ul>
 * <li>If a parameter has mode {@linkplain ParameterMode#REF_CURSOR REF_CURSOR},
 *     it's automatically inferred that the call is a function call because this
 *     is the only context in which PostgreSQL returns REF_CURSOR results.
 *     So it's not necessary to call {@link #markAsFunctionCall} explicitly.
 * <li>The restriction that there may be at most one REF_CURSOR mode parameter
 *     is enforced.
 * </ul>
 *
 * @author Steve Ebersole
 *
 * @see java.sql.CallableStatement
 * @see StoredProcedureQuery
 */
public interface ProcedureCall
		extends CommonQueryContract, SynchronizeableQuery, StoredProcedureQuery {

	/**
	 * Get the name of the stored procedure (or function) to be called.
	 *
	 * @return The procedure name.
	 */
	@Nonnull
	String getProcedureName();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override
	@Nonnull
	ProcedureCall setComment(@Nullable String comment);

	@Override
	@Nonnull
	ProcedureCall addQueryHint(@Nonnull String hint);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	ProcedureCall setMaxResults(int maxResults);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	ProcedureCall setFirstResult(int startPosition);

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	ProcedureCall setLockMode(@Nonnull LockModeType lockMode);

	@Override
	@Nonnull
	ProcedureCall setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter handling

	/**
	 * Does this {@code ProcedureCall} represent a call to a database {@code FUNCTION},
	 * as opposed to a {@code PROCEDURE}?
	 *
	 * @apiNote this will only report whether this {@code ProcedureCall} was marked
	 *           as a function via call to {@link #markAsFunctionCall}. In particular,
	 *           it will not return {@code true} when using JPA query hint.
	 *
	 * @return {@code true} indicates that this ProcedureCall represents a
	 *         function call; {@code false} indicates a procedure call.
	 */
	boolean isFunctionCall();

	/**
	 * Mark this {@code ProcedureCall} as representing a call to a database function,
	 * rather than a database procedure.
	 *
	 * @param sqlType The {@link java.sql.Types} code for the function return
	 *
	 * @return {@code this}, for method chaining
	 */
	@Nonnull
	ProcedureCall markAsFunctionCall(int sqlType);

	/**
	 * Mark this {@code ProcedureCall} as representing a call to a database function,
	 * rather than a database procedure.
	 *
	 * @param resultType The result type for the function return
	 *
	 * @return {@code this}, for method chaining
	 * @since 6.2
	 */
	@Nonnull
	ProcedureCall markAsFunctionCall(@Nonnull Class<?> resultType);

	/**
	 * Mark this {@code ProcedureCall} as representing a call to a database function,
	 * rather than a database procedure.
	 *
	 * @param typeReference The result type for the function return
	 *
	 * @return {@code this}, for method chaining
	 * @since 6.2
	 */
	@Nonnull
	ProcedureCall markAsFunctionCall(@Nonnull Type<?> typeReference);

	/**
	 * Basic form for registering a positional parameter.
	 *
	 * @param position The position
	 * @param type The Java type of the parameter
	 * @param mode The parameter mode (in, out, inout)
	 * @param <T> The parameterized Java type of the parameter.
	 *
	 * @return The parameter registration memento
	 */
	@Nonnull
	<T> ProcedureParameter<T> registerParameter(int position, @Nonnull Class<T> type, @Nonnull ParameterMode mode);

	/**
	 * Basic form for registering a positional parameter.
	 *
	 * @param position The position
	 * @param type The type reference of the parameter type
	 * @param mode The parameter mode (in, out, inout)
	 * @param <T> The parameterized Java type of the parameter.
	 *
	 * @return The parameter registration memento
	 */
	@Nonnull
	<T> ProcedureParameter<T> registerParameter(int position, @Nonnull Type<T> type, @Nonnull ParameterMode mode);

	/**
	 * Like {@link #registerStoredProcedureParameter(int, Class, ParameterMode)} but a type reference is given
	 * instead of a class for the parameter type.
	 */
	@Nonnull
	ProcedureCall registerStoredProcedureParameter(int position, @Nonnull Type<?> type, @Nonnull ParameterMode mode);

	/**
	 * Retrieve a previously registered parameter memento by the position under which it was registered.
	 *
	 * @param position The parameter position
	 *
	 * @return The parameter registration memento
	 *
	 * @throws ParameterStrategyException If the ProcedureCall is defined using named parameters
	 * @throws NoSuchParameterException If no parameter with that position exists
	 */
	@Nonnull
	ProcedureParameter<?> getParameterRegistration(int position);

	/**
	 * Basic form for registering a named parameter.
	 *
	 * @param parameterName The parameter name
	 * @param type The Java type of the parameter
	 * @param mode The parameter mode (in, out, inout)
	 * @param <T> The parameterized Java type of the parameter.
	 *
	 * @return The parameter registration memento
	 *
	 * @throws NamedParametersNotSupportedException When the underlying database is known to not support
	 * named procedure parameters.
	 */
	@Nonnull
	<T> ProcedureParameter<T> registerParameter(@Nonnull String parameterName, @Nonnull Class<T> type, @Nonnull ParameterMode mode)
			throws NamedParametersNotSupportedException;

	/**
	 * Basic form for registering a named parameter.
	 *
	 * @param parameterName The parameter name
	 * @param type The type reference of the parameter type
	 * @param mode The parameter mode (in, out, inout)
	 * @param <T> The parameterized Java type of the parameter.
	 *
	 * @return The parameter registration memento
	 *
	 * @throws NamedParametersNotSupportedException When the underlying database is known to not support
	 * named procedure parameters.
	 */
	@Nonnull
	<T> ProcedureParameter<T> registerParameter(@Nonnull String parameterName, @Nonnull Type<T> type, @Nonnull ParameterMode mode)
			throws NamedParametersNotSupportedException;

	/**
	 * Like {@link #registerStoredProcedureParameter(String, Class, ParameterMode)} but a type reference is given
	 * instead of a class for the parameter type.
	 */
	@Nonnull
	ProcedureCall registerStoredProcedureParameter(@Nonnull String parameterName, @Nonnull Type<?> type, @Nonnull ParameterMode mode);

	/**
	 * Retrieve a previously registered parameter memento by the name under which it was registered.
	 *
	 * @param name The parameter name
	 *
	 * @return The parameter registration memento
	 *
	 * @throws ParameterStrategyException If the ProcedureCall is defined using positional parameters
	 * @throws NoSuchParameterException If no parameter with that name exists
	 */
	@Nonnull
	ProcedureParameter<?> getParameterRegistration(@Nonnull String name);

	/**
	 * Retrieve all registered parameters.
	 *
	 * @return The (immutable) list of all registered parameters.
	 */
	@Nonnull
	List<ProcedureParameter<?>> getRegisteredParameters();

	/**
	 * Retrieves access to outputs of this procedure call.  Can be called multiple times, returning the same
	 * ProcedureOutputs instance each time.
	 * <p>
	 * If the procedure call has not actually been executed yet, it will be executed and then the ProcedureOutputs
	 * will be returned.
	 *
	 * @return The ProcedureOutputs representation
	 */
	@Nonnull
	ProcedureOutputs getOutputs();

	/**
	 * The {@link FunctionReturn} describing the return value of
	 * the function, or {@code null} if this {@code ProcedureCall}
	 * is not a function call.
	 *
	 * @since 7.0
	 */
	@Incubating
	@Nullable
	FunctionReturn<?> getFunctionReturn();

	/**
	 * Release the underlying JDBC {@link java.sql.CallableStatement}
	 */
	@Override
	default void close() {
		getOutputs().release();
	}

	/* Covariant overrides */

	@Override
	@Nonnull
	ProcedureCall setTimeout(@Nullable Integer integer);

	@Override
	@Nonnull
	ProcedureCall setTimeout(int timeout);

	@Override
	@Nonnull
	ProcedureCall setTimeout(@Nullable Timeout timeout);

	@Override
	@Nonnull
	ProcedureCall addSynchronizedQuerySpace(String querySpace);

	@Override
	@Nonnull
	ProcedureCall addSynchronizedEntityName(String entityName) throws MappingException;

	@Override
	@Nonnull
	ProcedureCall addSynchronizedEntityClass(@SuppressWarnings("rawtypes") Class entityClass) throws MappingException;

	@Override
	@Nonnull
	ProcedureCall setHint(@Nonnull String hintName, @Nullable Object value);

	@Override
	@Nonnull
	<T> ProcedureCall setParameter(@Nonnull Parameter<T> param, @Nullable T value);

	@Override @Deprecated
	@Nonnull
	ProcedureCall setParameter(@Nonnull Parameter<Calendar> param, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	ProcedureCall setParameter(@Nonnull Parameter<Date> param, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override
	@Nonnull
	ProcedureCall setParameter(@Nonnull String name, @Nullable Object value);

	@Override @Deprecated
	@Nonnull
	ProcedureCall setParameter(@Nonnull String name, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	ProcedureCall setParameter(@Nonnull String name, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override
	@Nonnull
	<P> ProcedureCall setParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> ProcedureCall setParameter(@Nonnull String name, @Nullable P value, @Nonnull Type<P> type);

	@Override
	@Nonnull
	<P> ProcedureCall setConvertedParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Nonnull
	ProcedureCall setParameter(int position, @Nullable Object value);

	@Override
	@Nonnull
	ProcedureCall setParameters(@Nonnull Object... arguments);

	@Override
	@Nonnull
	<P> ProcedureCall setConvertedParameter(int position, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter);

	@Override
	@Nonnull
	<P> ProcedureCall setParameter(int position, @Nullable P value, @Nonnull Class<P> type);

	@Override
	@Nonnull
	<P> ProcedureCall setParameter(int position, @Nullable P value, @Nonnull Type<P> type);

	@Override @Deprecated
	@Nonnull
	ProcedureCall setParameter(int position, @Nullable Calendar value, @Nonnull TemporalType temporalType);

	@Override @Deprecated
	@Nonnull
	ProcedureCall setParameter(int position, @Nullable Date value, @Nonnull TemporalType temporalType);

	@Override @Deprecated(since = "7")
	@Nonnull
	ProcedureCall setFlushMode(@Nonnull FlushModeType flushMode);

	@Override
	@Nonnull
	ProcedureCall registerStoredProcedureParameter(int position, Class<?> type, ParameterMode mode);

	@Override
	@Nonnull
	ProcedureCall registerStoredProcedureParameter(String parameterName, Class<?> type, ParameterMode mode);

	@Override
	@Nonnull
	@SuppressWarnings("removal")
	ProcedureCall setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode);

	@Override
	@Nonnull
	@SuppressWarnings("removal")
	ProcedureCall setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode);
}
