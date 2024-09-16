/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TemporalType;

import org.hibernate.MappingException;
import org.hibernate.query.SynchronizeableQuery;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.CommonQueryContract;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.query.named.NameableQuery;
import org.hibernate.type.BasicTypeReference;

/**
 * Defines support for executing database stored procedures and functions.
 * <p>
 * Note that here we use the terms "procedure" and "function" as follows:<ul>
 *     <li>procedure is a named database executable we expect to call via : {@code {call procedureName(...)}}</li>
 *     <li>function is a named database executable we expect to call via : {@code {? = call functionName(...)}}</li>
 * </ul>
 * <p>
 * Unless explicitly specified, the ProcedureCall is assumed to follow the
 * procedure call syntax.  To explicitly specify that this should be a function
 * call, use {@link #markAsFunctionCall}.  JPA users could either:<ul>
 *     <li>use {@code storedProcedureQuery.unwrap( ProcedureCall.class }.markAsFunctionCall()</li>
 *     <li>set the {@link #FUNCTION_RETURN_TYPE_HINT} hint (avoids casting to Hibernate-specific classes)</li>
 * </ul>
 * <p>
 * When using function-call syntax:<ul>
 *     <li>parameters must be registered by position (not name)</li>
 *     <li>The first parameter is considered to be the function return (the `?` before the call)</li>
 *     <li>the first parameter must have mode of OUT, INOUT or REF_CURSOR; IN is invalid</li>
 * </ul>
 * <p>
 * In some cases, based on the Dialect, we will have other validations and
 * assumptions as well.  For example, on PGSQL, whenever we see a REF_CURSOR mode
 * parameter, we know that:<ul>
 *     <li>
 *         this will be a function call (so we call {@link #markAsFunctionCall} implicitly) because
 *         that is the only way PGSQL supports returning REF_CURSOR results.
 *     </li>
 *     <li>there can be only one REF_CURSOR mode parameter</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface ProcedureCall
		extends CommonQueryContract, SynchronizeableQuery, StoredProcedureQuery, NameableQuery, AutoCloseable {
	/**
	 * The hint key (for use with JPA's "hint system") indicating the function's return JDBC type code
	 * (aka, {@link java.sql.Types} code)
	 */
	String FUNCTION_RETURN_TYPE_HINT = "hibernate.procedure.function_return_jdbc_type_code";

	/**
	 * Get the name of the stored procedure (or function) to be called.
	 *
	 * @return The procedure name.
	 */
	String getProcedureName();

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
	 * Mark this ProcedureCall as representing a call to a database function,
	 * rather than a database procedure.
	 *
	 * @param sqlType The {@link java.sql.Types} code for the function return
	 *
	 * @return {@code this}, for method chaining
	 */
	ProcedureCall markAsFunctionCall(int sqlType);

	/**
	 * Mark this ProcedureCall as representing a call to a database function,
	 * rather than a database procedure.
	 *
	 * @param resultType The result type for the function return
	 *
	 * @return {@code this}, for method chaining
	 * @since 6.2
	 */
	ProcedureCall markAsFunctionCall(Class<?> resultType);

	/**
	 * Mark this ProcedureCall as representing a call to a database function,
	 * rather than a database procedure.
	 *
	 * @param typeReference The result type for the function return
	 *
	 * @return {@code this}, for method chaining
	 * @since 6.2
	 */
	ProcedureCall markAsFunctionCall(BasicTypeReference<?> typeReference);

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
	<T> ProcedureParameter<T> registerParameter(int position, Class<T> type, ParameterMode mode);

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
	<T> ProcedureParameter<T> registerParameter(int position, BasicTypeReference<T> type, ParameterMode mode);

	/**
	 * Like {@link #registerStoredProcedureParameter(int, Class, ParameterMode)} but a basic type reference is given
	 * instead of a class for the parameter type.
	 */
	ProcedureCall registerStoredProcedureParameter(int position, BasicTypeReference<?> type, ParameterMode mode);

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
	ProcedureParameter getParameterRegistration(int position);

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
	<T> ProcedureParameter<T> registerParameter(String parameterName, Class<T> type, ParameterMode mode)
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
	<T> ProcedureParameter<T> registerParameter(String parameterName, BasicTypeReference<T> type, ParameterMode mode)
			throws NamedParametersNotSupportedException;

	/**
	 * Like {@link #registerStoredProcedureParameter(String, Class, ParameterMode)} but a basic type reference is given
	 * instead of a class for the parameter type.
	 */
	ProcedureCall registerStoredProcedureParameter(String parameterName, BasicTypeReference<?> type, ParameterMode mode);

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
	ProcedureParameter getParameterRegistration(String name);

	/**
	 * Retrieve all registered parameters.
	 *
	 * @return The (immutable) list of all registered parameters.
	 */
	List<ProcedureParameter> getRegisteredParameters();

	/**
	 * Retrieves access to outputs of this procedure call.  Can be called multiple times, returning the same
	 * ProcedureOutputs instance each time.
	 * <p>
	 * If the procedure call has not actually be executed yet, it will be executed and then the ProcedureOutputs
	 * will be returned.
	 *
	 * @return The ProcedureOutputs representation
	 */
	ProcedureOutputs getOutputs();

	/**
	 * Release the underlying JDBC {@link java.sql.CallableStatement}
	 */
	@Override
	default void close() {
		getOutputs().release();
	}

	/*
	Covariant overrides
	 */

	@Override
	ProcedureCall addSynchronizedQuerySpace(String querySpace);

	@Override
	ProcedureCall addSynchronizedEntityName(String entityName) throws MappingException;

	@Override @SuppressWarnings("rawtypes")
	ProcedureCall addSynchronizedEntityClass(Class entityClass) throws MappingException;

	@Override
	NamedCallableQueryMemento toMemento(String name);

	@Override
	ProcedureCall setHint(String hintName, Object value);

	@Override
	<T> ProcedureCall setParameter( Parameter<T> param, T value);

	@Override
	ProcedureCall setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	ProcedureCall setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	ProcedureCall setParameter(String name, Object value);

	@Override
	ProcedureCall setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	ProcedureCall setParameter(String name, Date value, TemporalType temporalType);

	@Override
	ProcedureCall setParameter(int position, Object value);

	@Override
	ProcedureCall setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	ProcedureCall setParameter(int position, Date value, TemporalType temporalType);

	@Override @Deprecated(since = "7")
	ProcedureCall setFlushMode(FlushModeType flushMode);

	@Override
	ProcedureCall registerStoredProcedureParameter(int position, Class type, ParameterMode mode);

	@Override
	ProcedureCall registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode);
}
