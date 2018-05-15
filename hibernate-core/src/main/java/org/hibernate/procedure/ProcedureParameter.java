/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure;

import javax.persistence.ParameterMode;

import org.hibernate.query.QueryParameter;

/**
 * @author Steve Ebersole
 */
public interface ProcedureParameter<T> extends QueryParameter<T> {
	/**
	 * Retrieves the parameter "mode".  Only really pertinent in regards to procedure/function calls.
	 * In all other cases the mode would be {@link ParameterMode#IN}
	 *
	 * @return The parameter mode.
	 */
	ParameterMode getMode();
}
