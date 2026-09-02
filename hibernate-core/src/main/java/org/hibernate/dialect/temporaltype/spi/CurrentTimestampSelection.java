/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporaltype.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Describes the JDBC command used to obtain the database current timestamp.
///
/// @param command the non-blank JDBC command, preserved verbatim
/// @param callable whether Hibernate executes the command as a callable statement
///
/// @author Steve Ebersole
/// @since 8.0
/// @see CurrentTemporalSupport#getCurrentTimestampSelection()
@SPI({ USE, SUPPLY })
public record CurrentTimestampSelection(String command, boolean callable) {
	public CurrentTimestampSelection {
		if ( command == null || command.isBlank() ) {
			throw new IllegalArgumentException( "Current-timestamp command must not be null or blank" );
		}
	}

	/// Create a selection executed as a prepared statement.
	public static CurrentTimestampSelection prepared(String command) {
		return new CurrentTimestampSelection( command, false );
	}

	/// Create a selection executed as a callable statement.
	public static CurrentTimestampSelection callable(String command) {
		return new CurrentTimestampSelection( command, true );
	}
}
