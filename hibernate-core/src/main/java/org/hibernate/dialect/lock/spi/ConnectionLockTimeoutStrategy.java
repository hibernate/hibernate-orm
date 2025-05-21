/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import jakarta.persistence.Timeout;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.sql.Connection;

/**
 * Contract for reading and setting lock timeouts using the
 * {@linkplain Connection JDBC connection}, generally via execution
 * of a command/statement.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ConnectionLockTimeoutStrategy {
	ConnectionLockTimeoutStrategy NONE = () -> Level.NONE;

	/**
	 * What type, if any, of support this Dialect has for lock timeouts on the JDBC connection.
	 *
	 * @see #getLockTimeout
	 * @see #setLockTimeout
	 */
	Level getSupportedLevel();

	/**
	 * Read the lock timeout associated with the JDBC connection, if supported and there is one.
	 *
	 * @see #getSupportedLevel
	 *
	 * @throws UnsupportedOperationException when {@linkplain #getSupportedLevel} is {@linkplain Level#NONE}
	 */
	default Timeout getLockTimeout(Connection connection, SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException( "Lock timeout on the JDBC connection is not supported" );
	}

	/**
	 * Set the lock timeout associated with the JDBC connection (if supported), in milliseconds.
	 *
	 * @see #getSupportedLevel()
	 *
	 * @throws UnsupportedOperationException when {@linkplain #getSupportedLevel} is {@linkplain Level#NONE}
	 */
	default void setLockTimeout(Timeout timeout, Connection connection, SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException( "Lock timeout on the JDBC connection is not supported" );
	}

	/**
	 * Indicates a Dialect's level of support for lock timeouts on the JDBC connection.
	 *
	 * @apiNote {@linkplain org.hibernate.Timeouts#SKIP_LOCKED skip-locked} is never supported.
	 */
	enum Level {
		/**
		 * Setting lock timeouts on the JDBC connection is not supported.
		 */
		NONE,
		/**
		 * Setting {@linkplain org.hibernate.Timeouts#isRealTimeout real} lock timeouts on
		 * the JDBC connection is supported.  Additionally, setting
		 * {@linkplain org.hibernate.Timeouts#WAIT_FOREVER wait-forever} is generally supported.
		 */
		SUPPORTED,
		/**
		 * In addition to {@linkplain #SUPPORTED}, setting {@linkplain org.hibernate.Timeouts#NO_WAIT no-wait}
		 * is also supported.
		 */
		EXTENDED
	}
}
