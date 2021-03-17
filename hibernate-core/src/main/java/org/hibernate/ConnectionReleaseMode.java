/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.util.Locale;

import org.hibernate.internal.util.StringHelper;

/**
 * Defines the various policies by which Hibernate might release its underlying
 * JDBC connection.  Inverse of {@link ConnectionAcquisitionMode}.
 *
 * @author Steve Ebersole
 */
public enum ConnectionReleaseMode{
	/**
	 * Indicates that JDBC connection should be aggressively released after each
	 * SQL statement is executed. In this mode, the application <em>must</em>
	 * explicitly close all iterators and scrollable results. This mode may
	 * only be used with a JTA datasource.
	 */
	AFTER_STATEMENT,

	/**
	 * Indicates that JDBC connections should be released before each transaction
	 * commits/rollbacks (works with both JTA-registered synch and HibernateTransaction API).
	 * This mode may be used with an application server JTA datasource.
	 */
	BEFORE_TRANSACTION_COMPLETION,

	/**
	 * Indicates that JDBC connections should be released after each transaction
	 * ends (works with both JTA-registered synch and HibernateTransaction API).
	 * This mode may not be used with an application server JTA datasource.
	 * <p/>
	 * This is the default mode starting in 3.1; was previously {@link #ON_CLOSE}.
	 */
	AFTER_TRANSACTION,

	/**
	 * Indicates that connections should only be released when the Session is explicitly closed 
	 * or disconnected; this is the legacy (Hibernate2 and pre-3.1) behavior.
	 */
	ON_CLOSE;

	/**
	 * Alias for {@link ConnectionReleaseMode#valueOf(String)} using upper-case version of the incoming name.
	 *
	 * @param name The name to parse
	 *
	 * @return The matched enum value.
	 */
	public static ConnectionReleaseMode parse(final String name) {
		return ConnectionReleaseMode.valueOf( name.toUpperCase(Locale.ROOT) );
	}

	public static ConnectionReleaseMode interpret(Object setting) {
		if ( setting == null ) {
			return null;
		}

		if ( setting instanceof ConnectionReleaseMode ) {
			return (ConnectionReleaseMode) setting;
		}

		final String value = setting.toString();
		if ( StringHelper.isEmpty( value ) ) {
			return null;
		}

		// here we disregard "auto"
		if ( value.equalsIgnoreCase( "auto" ) ) {
			return null;
		}

		return parse( value );
	}
}
