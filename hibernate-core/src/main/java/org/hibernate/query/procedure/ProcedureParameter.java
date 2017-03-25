/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure;

import javax.persistence.ParameterMode;

import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
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

	/**
	 * How will an unbound value be handled in terms of the JDBC parameter?
	 *
	 * @return {@code true} here indicates that NULL should be passed; {@code false} indicates
	 * that it is ignored.
	 *
	 * @see ParameterRegistrationImplementor#isPassNullsEnabled()
	 */
	boolean isPassNullsEnabled();

	/**
	 * Controls how unbound values for this IN/INOUT parameter registration will be handled prior to
	 * execution.  For details see {@link org.hibernate.procedure.ParameterRegistration#enablePassingNulls}
	 *
	 * @param enabled {@code true} indicates that the NULL should be passed; {@code false} indicates it should not.
	 *
	 * @see org.hibernate.procedure.ParameterRegistration#enablePassingNulls
	 */
	void enablePassingNulls(boolean enabled);
}
