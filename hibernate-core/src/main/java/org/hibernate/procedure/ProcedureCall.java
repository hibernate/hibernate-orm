/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure;

import java.util.List;
import java.util.Map;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.BasicQueryContract;
import org.hibernate.MappingException;
import org.hibernate.SynchronizeableQuery;
import org.hibernate.query.CommonQueryContract;

/**
 * Defines support for executing database stored procedures and functions
 *
 * @author Steve Ebersole
 */
public interface ProcedureCall extends BasicQueryContract<CommonQueryContract>, SynchronizeableQuery, StoredProcedureQuery {
	@Override
	ProcedureCall addSynchronizedQuerySpace(String querySpace);

	@Override
	ProcedureCall addSynchronizedEntityName(String entityName) throws MappingException;

	@Override
	ProcedureCall addSynchronizedEntityClass(Class entityClass) throws MappingException;

	/**
	 * Get the name of the stored procedure to be called.
	 *
	 * @return The procedure name.
	 */
	String getProcedureName();

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
	<T> ParameterRegistration<T> registerParameter(int position, Class<T> type, ParameterMode mode);

	/**
	 * Chained form of {@link #registerParameter(int, Class, javax.persistence.ParameterMode)}
	 *
	 * @param position The position
	 * @param type The Java type of the parameter
	 * @param mode The parameter mode (in, out, inout)
	 *
	 * @return {@code this}, for method chaining
	 */
	ProcedureCall registerParameter0(int position, Class type, ParameterMode mode);

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
	ParameterRegistration getParameterRegistration(int position);

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
	<T> ParameterRegistration<T> registerParameter(String parameterName, Class<T> type, ParameterMode mode)
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
	ParameterRegistration getParameterRegistration(String name);

	/**
	 * Retrieve all registered parameters.
	 *
	 * @return The (immutable) list of all registered parameters.
	 */
	List<ParameterRegistration> getRegisteredParameters();

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

	/**
	 * Extract the disconnected representation of this call.  Used in HEM to allow redefining a named query
	 *
	 * @param hints The hints to incorporate into the memento
	 *
	 * @return The memento
	 */
	ProcedureCallMemento extractMemento(Map<String, Object> hints);

	/**
	 * Extract the disconnected representation of this call.  Used in HEM to allow redefining a named query
	 * *
	 * @return The memento
	 */
	ProcedureCallMemento extractMemento();
}
