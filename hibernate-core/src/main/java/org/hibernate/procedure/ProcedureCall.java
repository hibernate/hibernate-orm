/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure;

import java.util.List;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.Incubating;
import org.hibernate.MappingException;
import org.hibernate.query.CommonQueryContract;
import org.hibernate.query.SynchronizeableQuery;

/**
 * Defines support for executing database stored procedures and functions.
 * <p/>
 * Note that here we use the terms "procedure" and "function" as follows:<ul>
 *     <li>procedure is a named database executable we expect to call via : {@code {call procedureName(...)}}</li>
 *     <li>function is a named database executable we expect to call via : {@code {? = call functionName(...)}}</li>
 * </ul>
 * Unless explicitly specified, the ProcedureCall is assumed to follow the
 * procedure call syntax.  To explicitly specify that this should be a function
 * call, use {@link #markAsFunctionCall}.  JPA users could either:<ul>
 *     <li>use {@code storedProcedureQuery.unwrap( ProcedureCall.class }.markAsFunctionCall()</li>
 *     <li>set the {@link #FUNCTION_RETURN_TYPE_HINT} hint (avoids casting to Hibernate-specific classes)</li>
 * </ul>
 * <p/>
 * When using function-call syntax:<ul>
 *     <li>parameters must be registered by position (not name)</li>
 *     <li>The first parameter is considered to be the function return (the `?` before the call)</li>
 *     <li>the first parameter must have mode of OUT, INOUT or REF_CURSOR; IN is invalid</li>
 * </ul>
 * <p/>
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
@Incubating
public interface ProcedureCall extends CommonQueryContract, SynchronizeableQuery, StoredProcedureQuery {
	/**
	 * The hint key (for use with JPA's "hint system") indicating the function's return JDBC type code
	 * (aka, {@link java.sql.Types} code)
	 */
	String FUNCTION_RETURN_TYPE_HINT = "hibernate.procedure.function_return_jdbc_type_code";

	@Override
	ProcedureCall addSynchronizedQuerySpace(String querySpace);

	@Override
	ProcedureCall addSynchronizedEntityName(String entityName) throws MappingException;

	@Override
	ProcedureCall addSynchronizedEntityClass(Class entityClass) throws MappingException;

	/**
	 * Get the name of the stored procedure (or function) to be called.
	 *
	 * @return The procedure name.
	 */
	String getProcedureName();

	/**
	 * Does this ProcedureCall represent a call to a database FUNCTION (as opposed
	 * to a PROCEDURE call)?
	 *
	 * NOTE : this will only report whether this ProcedureCall was marked
	 * as a function via call to {@link #markAsFunctionCall}.  Specifically
	 * will not return {@code true} when using JPA query hint.
	 *
	 * @return {@code true} indicates that this ProcedureCall represents a
	 * function call; {@code false} indicates a procedure call.
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
	 * Chained form of {@link #registerParameter(int, Class, javax.persistence.ParameterMode)}
	 *
	 * @param position The position
	 * @param type The Java type of the parameter
	 * @param mode The parameter mode (in, out, inout)
	 *
	 * @return {@code this}, for method chaining
	 */
	<T> ProcedureCall registerParameter0(int position, Class<T> type, ParameterMode mode);

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
	 * Chained form of {@link #registerParameter(String, Class, javax.persistence.ParameterMode)}
	 *
	 * @param parameterName The parameter name
	 * @param type The Java type of the parameter
	 * @param mode The parameter mode (in, out, inout)
	 *
	 * @return The parameter registration memento
	 *
	 * @throws NamedParametersNotSupportedException When the underlying database is known to not support
	 * named procedure parameters.
	 */
	ProcedureCall registerParameter0(String parameterName, Class type, ParameterMode mode)
			throws NamedParametersNotSupportedException;

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
	 * <p/>
	 * If the procedure call has not actually be executed yet, it will be executed and then the ProcedureOutputs
	 * will be returned.
	 *
	 * @return The ProcedureOutputs representation
	 */
	ProcedureOutputs getOutputs();
}
