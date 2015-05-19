/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure;

import org.hibernate.result.Outputs;

/**
 * Specialization of the {@link org.hibernate.result.Outputs} contract providing access to the stored procedure's registered
 * output parameters.
 *
 * @author Steve Ebersole
 */
public interface ProcedureOutputs extends Outputs {
	/**
	 * Retrieve the value of an OUTPUT parameter by the parameter's registration memento.
	 * <p/>
	 * Should NOT be called for parameters registered as REF_CURSOR.  REF_CURSOR parameters should be
	 * accessed via the returns (see {@link #getNextOutput}
	 *
	 * @param parameterRegistration The parameter's registration memento.
	 *
	 * @return The output value.
	 *
	 * @see ProcedureCall#registerParameter(String, Class, javax.persistence.ParameterMode)
	 */
	public <T> T getOutputParameterValue(ParameterRegistration<T> parameterRegistration);

	/**
	 * Retrieve the value of an OUTPUT parameter by the name under which the parameter was registered.
	 *
	 * @param name The name under which the parameter was registered.
	 *
	 * @return The output value.
	 *
	 * @throws ParameterStrategyException If the ProcedureCall is defined using positional parameters
	 * @throws NoSuchParameterException If no parameter with that name exists
	 *
	 * @see ProcedureCall#registerParameter(String, Class, javax.persistence.ParameterMode)
	 */
	public Object getOutputParameterValue(String name);

	/**
	 * Retrieve the value of an OUTPUT parameter by the name position under which the parameter was registered.
	 *
	 * @param position The position at which the parameter was registered.
	 *
	 * @return The output value.
	 *
	 * @throws ParameterStrategyException If the ProcedureCall is defined using named parameters
	 * @throws NoSuchParameterException If no parameter with that position exists
	 *
	 * @see ProcedureCall#registerParameter(int, Class, javax.persistence.ParameterMode)
	 */
	public Object getOutputParameterValue(int position);
}
