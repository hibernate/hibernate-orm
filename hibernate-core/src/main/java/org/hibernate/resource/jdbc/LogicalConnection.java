/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	 * @return {@code true} if still open, since {@link #close} has not yet been called;
	 *         {@code false} if not open, since {@link #close} was called.
	 */
	boolean isOpen();

	/**
	 * Closes the logical connection, making it inactive and forcing release of any held resources.
	 *
	 * @return the JDBC {@code Connection} if the user passed in a {@code Connection} originally
	 *
	 * @apiNote The return type accommodates legacy functionality for user-supplied connections.
	 */
	Connection close();

	/**
	 * Is this logical connection currently physically connected?
	 * <p>
	 * That is, does it currently hold a physical JDBC {@link Connection}?
	 *
	 * @return {@code true} if currently holding a JDBC {@code Connection};
	 *         {@code false} if not.
	 */
	boolean isPhysicallyConnected();

	/**
	 * Provides access to the registry of JDBC resources associated with this logical connection.
	 *
	 * @return The JDBC resource registry.
	 *
	 * @throws org.hibernate.ResourceClosedException if the {@code LogicalConnection} is closed
	 */
	ResourceRegistry getResourceRegistry();

}
