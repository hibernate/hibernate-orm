/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

/**
 * ParameterRegistration extension specifically for stored procedure parameters
 * exposing some functionality of Hibernate's native {@link ParameterRegistration} contract
 *
 * @author Steve Ebersole
 */
public interface StoredProcedureQueryParameterRegistration<T> extends ParameterRegistration<T> {
	/**
	 * How will an unbound value be handled in terms of the JDBC parameter?
	 *
	 * @return {@code true} here indicates that NULL should be passed; {@code false} indicates
	 * that it is ignored.
	 */
	boolean isPassNullsEnabled();

	/**
	 * Controls how unbound values for this IN/INOUT parameter registration will be handled prior to
	 * execution.
	 *
	 * @param enabled {@code true} indicates that the NULL should be passed; {@code false} indicates it should not.
	 *
	 */
	void enablePassingNulls(boolean enabled);
}
