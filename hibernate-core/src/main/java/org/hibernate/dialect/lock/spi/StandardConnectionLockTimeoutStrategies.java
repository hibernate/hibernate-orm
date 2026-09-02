/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.dialect.lock.internal.MySQLLockingSupport;

import static org.hibernate.SPI.Role.USE;

/// Creates standard connection-level lock-timeout strategies.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
@SPI(USE)
public final class StandardConnectionLockTimeoutStrategies {
	private StandardConnectionLockTimeoutStrategies() {
	}

	/// Creates the MySQL strategy using the database's wait-forever sentinel.
	///
	/// @param waitForeverValue the positive timeout value representing an
	/// effectively unbounded wait
	public static ConnectionLockTimeoutStrategy mysql(int waitForeverValue) {
		if ( waitForeverValue <= 0 ) {
			throw new IllegalArgumentException( "waitForeverValue must be positive" );
		}
		return new MySQLLockingSupport.ConnectionLockTimeoutStrategyImpl( waitForeverValue );
	}
}
