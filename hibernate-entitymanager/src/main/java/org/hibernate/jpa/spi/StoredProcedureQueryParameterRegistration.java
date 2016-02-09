/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import org.hibernate.procedure.spi.ParameterRegistrationImplementor;

/**
 * ParameterRegistration extension specifically for stored procedure parameters
 * exposing some functionality of Hibernate's native
 * {@link org.hibernate.procedure.ParameterRegistration} contract
 *
 * @author Steve Ebersole
 */
public interface StoredProcedureQueryParameterRegistration<T> extends ParameterRegistration<T> {
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
