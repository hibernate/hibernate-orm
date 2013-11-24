/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.procedure;

import java.util.List;
import java.util.Map;
import javax.persistence.ParameterMode;

import org.hibernate.BasicQueryContract;
import org.hibernate.MappingException;
import org.hibernate.SynchronizeableQuery;

/**
 * Defines support for executing database stored procedures and functions
 *
 * @author Steve Ebersole
 */
public interface ProcedureCall extends BasicQueryContract, SynchronizeableQuery {
	@Override
	public ProcedureCall addSynchronizedQuerySpace(String querySpace);

	@Override
	public ProcedureCall addSynchronizedEntityName(String entityName) throws MappingException;

	@Override
	public ProcedureCall addSynchronizedEntityClass(Class entityClass) throws MappingException;

	/**
	 * Get the name of the stored procedure to be called.
	 *
	 * @return The procedure name.
	 */
	public String getProcedureName();

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
	public <T> ParameterRegistration<T> registerParameter(int position, Class<T> type, ParameterMode mode);

	/**
	 * Chained form of {@link #registerParameter(int, Class, javax.persistence.ParameterMode)}
	 *
	 * @param position The position
	 * @param type The Java type of the parameter
	 * @param mode The parameter mode (in, out, inout)
	 *
	 * @return {@code this}, for method chaining
	 */
	public ProcedureCall registerParameter0(int position, Class type, ParameterMode mode);

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
	public ParameterRegistration getParameterRegistration(int position);

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
	public <T> ParameterRegistration<T> registerParameter(String parameterName, Class<T> type, ParameterMode mode)
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
	public ProcedureCall registerParameter0(String parameterName, Class type, ParameterMode mode)
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
	public ParameterRegistration getParameterRegistration(String name);

	/**
	 * Retrieve all registered parameters.
	 *
	 * @return The (immutable) list of all registered parameters.
	 */
	public List<ParameterRegistration> getRegisteredParameters();

	/**
	 * Retrieves access to outputs of this procedure call.  Can be called multiple times, returning the same
	 * ProcedureOutputs instance each time.
	 * <p/>
	 * If the procedure call has not actually be executed yet, it will be executed and then the ProcedureOutputs
	 * will be returned.
	 *
	 * @return The ProcedureOutputs representation
	 */
	public ProcedureOutputs getOutputs();

	/**
	 * Extract the disconnected representation of this call.  Used in HEM to allow redefining a named query
	 *
	 * @param hints The hints to incorporate into the memento
	 *
	 * @return The memento
	 */
	public ProcedureCallMemento extractMemento(Map<String, Object> hints);
}
