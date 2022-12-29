/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc;

import java.sql.Connection;

/**
 * Represents a continuous logical connection to the database to the database via JDBC.
 * <p>
 * Under the covers, a physical JDBC {@link Connection} might be acquired and then released multiple times,
 * but those details are hidden from clients.
 *
 * @author Steve Ebersole
 */
public interface LogicalConnection {
	/**
	 * Is this (logical) JDBC connection still open/active?
	 * <p>
	 * That is, has {@link #close} not yet been called?
	 *
	 * @return {@code true} if still open ({@link #close} has not been called yet); {@code false} if not open
	 * ({@link #close} has been called).
	 */
	boolean isOpen();

	/**
	 * Closes the JdbcSession, making it inactive and forcing release of any held resources.
	 *
	 * @return Legacy :(  Returns the JDBC {@code Connection} if the user passed in a {@code Connection} originally.
	 */
	Connection close();

	/**
	 * Is this JdbcSession currently physically connected?
	 * <p>
	 * That is, does it currently hold a physical JDBC {@code Connection}?
	 *
	 * @return {@code true} if the JdbcSession currently hold a JDBC {@code Connection}; {@code false} if it does not.
	 */
	boolean isPhysicallyConnected();

	/**
	 * Provides access to the registry of JDBC resources associated with this {@code LogicalConnection}.
	 *
	 * @return The JDBC resource registry.
	 *
	 * @throws org.hibernate.ResourceClosedException if the {@code LogicalConnection} is closed
	 */
	ResourceRegistry getResourceRegistry();

}
