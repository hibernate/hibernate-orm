/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

///
/// Represents the outputs of executing a JDBC [statement][java.sql.CallableStatement].
/// Provides support for accessing [result-sets][ResultSetOutput],
/// [update counts][UpdateCountOutput], and
/// registered output parameters.
///
/// @author Steve Ebersole
public interface ProcedureOutputs extends AutoCloseable{
	/// Retrieve the value of an OUTPUT parameter by the parameter's registration memento.
	///
	/// Should NOT be called for parameters registered as REF_CURSOR.  REF_CURSOR parameters should be
	/// accessed via the returns (see [#goToNext()]).
	///
	/// @param parameter The parameter's registration memento.
	///
	/// @return The output value.
	///
	/// @see ProcedureCall#registerParameter(String, Class, jakarta.persistence.ParameterMode)
	<T> T getOutputParameterValue(ProcedureParameter<T> parameter);

	/// Retrieve the value of an OUTPUT parameter by the name under which the parameter was registered.
	///
	/// Should NOT be called for parameters registered as REF_CURSOR.  REF_CURSOR parameters should be
	/// accessed via the returns (see [#goToNext()]).
	///
	/// @param name The name under which the parameter was registered.
	///
	/// @return The output value.
	///
	/// @throws ParameterStrategyException If the ProcedureCall is defined using positional parameters
	/// @throws NoSuchParameterException If no parameter with that name exists
	///
	/// @see ProcedureCall#registerParameter(String, Class, jakarta.persistence.ParameterMode)
	Object getOutputParameterValue(String name);

	/// Retrieve the value of an OUTPUT parameter by the name position under which the parameter was registered.
	///
	/// Should NOT be called for parameters registered as REF_CURSOR.  REF_CURSOR parameters should be
	/// accessed via the returns (see [#goToNext()]).
	///
	/// @param position The position at which the parameter was registered.
	///
	/// @return The output value.
	///
	/// @throws ParameterStrategyException If the ProcedureCall is defined using named parameters
	/// @throws NoSuchParameterException If no parameter with that position exists
	///
	/// @see ProcedureCall#registerParameter(int, Class, jakarta.persistence.ParameterMode)
	Object getOutputParameterValue(int position);

	/// Retrieve the current Output object.
	///
	/// @return The current Output object.  Can be `null`
	Output getCurrent();

	/// Go to the next Output object (if any), returning an indication of whether there is another.
	///
	/// @return `true` if the next call to [#getCurrent()] will return a non-`null` value.
	boolean goToNext();

	/// Eagerly release any held resources.
	void release();

	@Override
	default void close() throws Exception {
		release();
	}
}
