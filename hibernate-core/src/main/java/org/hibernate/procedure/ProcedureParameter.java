/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import org.hibernate.Incubating;
import org.hibernate.query.QueryParameter;

import jakarta.persistence.ParameterMode;

/**
 * @apiNote Consider this contract (and its subcontracts) as incubating as we transition to 6.0 and SQM.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ProcedureParameter<T> extends QueryParameter<T> {
	/**
	 * Retrieves the parameter "mode".  Only really pertinent in regards to procedure/function calls.
	 * In all other cases the mode would be {@link ParameterMode#IN}
	 *
	 * @return The parameter mode.
	 */
	ParameterMode getMode();
}
