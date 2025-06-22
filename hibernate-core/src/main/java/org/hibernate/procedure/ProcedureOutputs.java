/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import org.hibernate.result.Outputs;

/**
 * Specialization of the {@link Outputs} contract providing access to the stored procedure's registered
 * output parameters.
 *
 * @author Steve Ebersole
 */
public interface ProcedureOutputs extends Outputs {
	/**
	 * Retrieve the value of an OUTPUT parameter by the parameter's registration memento.
	 * <p>
	 * Should NOT be called for parameters registered as REF_CURSOR.  REF_CURSOR parameters should be
	 * accessed via the returns (see {@link #goToNext()}
	 *
	 * @param parameter The parameter's registration memento.
	 *
	 * @return The output value.
	 *
	 * @see ProcedureCall#registerParameter(String, Class, jakarta.persistence.ParameterMode)
	 */
	<T> T getOutputParameterValue(ProcedureParameter<T> parameter);

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
	 * @see ProcedureCall#registerParameter(String, Class, jakarta.persistence.ParameterMode)
	 */
	Object getOutputParameterValue(String name);

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
	 * @see ProcedureCall#registerParameter(int, Class, jakarta.persistence.ParameterMode)
	 */
	Object getOutputParameterValue(int position);
}
