/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;


import jakarta.persistence.Timeout;
import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.sql.Connection;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Reads and sets lock timeouts using the JDBC [Connection], generally by
/// executing a database command.
///
/// Implementations supplied by a [LockingSupport] must be thread-safe and must
/// not retain a connection, factory, or timeout request. Use
/// [ConnectionLockTimeoutOperations] for the standard Hibernate-integrated JDBC
/// execution lifecycle.
///
/// @see LockingSupport#getConnectionLockTimeoutStrategy()
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface ConnectionLockTimeoutStrategy {
	ConnectionLockTimeoutStrategy NONE = () -> Level.NONE;

	/// Reports which connection-level lock-timeout operations this strategy supports.
	///
	/// @see #getLockTimeout
	/// @see #setLockTimeout
	Level getSupportedLevel();

	/// Reads the lock timeout associated with the JDBC connection.
	///
	/// @see #getSupportedLevel
	///
	/// @throws UnsupportedOperationException when [#getSupportedLevel] is [Level#NONE]
	default Timeout getLockTimeout(Connection connection, SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException( "Lock timeout on the JDBC connection is not supported" );
	}

	/// Sets the lock timeout associated with the JDBC connection.
	///
	/// @see #getSupportedLevel()
	///
	/// @throws UnsupportedOperationException when [#getSupportedLevel] is [Level#NONE]
	default void setLockTimeout(Timeout timeout, Connection connection, SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException( "Lock timeout on the JDBC connection is not supported" );
	}

	/// Indicates a Dialect's level of support for lock timeouts on the JDBC connection.
	///
	/// @apiNote [org.hibernate.Timeouts#SKIP_LOCKED skip-locked] is never supported.
	@SPI(USE)
	enum Level {
		/// Setting lock timeouts on the JDBC connection is not supported.
		NONE,
		/// Setting [org.hibernate.Timeouts#isRealTimeout real] lock timeouts on
		/// the JDBC connection is supported. Additionally, setting
		/// [org.hibernate.Timeouts#WAIT_FOREVER wait-forever] is generally supported.
		SUPPORTED,
		/// In addition to [#SUPPORTED], setting
		/// [org.hibernate.Timeouts#NO_WAIT no-wait] is also supported.
		EXTENDED
	}
}
