/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure;

import org.hibernate.Incubating;
import org.hibernate.query.QueryParameter;

import jakarta.persistence.ParameterMode;

/**
 * NOTE: Consider this contract (and its sub-contracts) as incubating as we transition to 6.0 and SQM
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
