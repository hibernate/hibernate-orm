/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.util.Locale;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Enumerates various policies for releasing JDBC {@linkplain java.sql.Connection
 * connections}. Complementary to {@link ConnectionAcquisitionMode}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode
 */
public enum ConnectionReleaseMode{
	/**
	 * Indicates that the JDBC connection should be aggressively released after
	 * each SQL statement is executed. In this mode, the application must
	 * <em>explicitly</em> close all iterators and scrollable results.
	 * <p>
	 * This mode may only be used with a JTA datasource.
	 */
	AFTER_STATEMENT,

	/**
	 * Indicates that the JDBC connection should be released before each transaction
	 * commits or rolls back.
	 * <p>
	 * This works with both resource-local transactions and with JTA-registered
	 * synchronizations. It may be used with an application server JTA datasource.
	 */
	BEFORE_TRANSACTION_COMPLETION,

	/**
	 * Indicates that the JDBC connection should be released after each transaction
	 * ends.
	 * <p>
	 * This works with both resource-local transactions and with JTA-registered
	 * synchronizations. But it may not be used with an application server JTA
	 * datasource.
	 * <p>
	 * This is the default mode.
	 */
	AFTER_TRANSACTION,

	/**
	 * Indicates that connections should only be released when the session is
	 * explicitly closed or disconnected.
	 * <p>
	 * Prior to Hibernate 3.1, this was the default mode.
	 */
	ON_CLOSE;

	/**
	 * Alias for {@link ConnectionReleaseMode#valueOf(String)} using uppercase
	 * version of the incoming name.
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
		else if ( setting instanceof ConnectionReleaseMode mode ) {
			return mode;
		}
		else {
			final String value = setting.toString();
			if ( isEmpty( value ) ) {
				return null;
			}
			// here we disregard "auto"
			else if ( value.equalsIgnoreCase( "auto" ) ) {
				return null;
			}
			else {
				return parse( value );
			}
		}
	}
}
