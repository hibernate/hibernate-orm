/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc;

import java.sql.Connection;

/**
 * Models the logical notion of a JDBC Connection.  We may release/re-acquire physical JDBC connections under the
 * covers, but this logically represents the overall access to the JDBC Connection.
 *
 * @author Steve Ebersole
 */
public interface LogicalConnection {
	/**
	 * Is this (logical) JDBC Connection still open/active.  In other words, has {@link #close} not been called yet?
	 *
	 * @return {@code true} if still open ({@link #close} has not been called yet); {@code false} if not open
	 * (({@link #close} has been called).
	 */
	public boolean isOpen();

	/**
	 * Closes the JdbcSession, making it inactive and forcing release of any held resources
	 *
	 * @return Legacy :(  Returns the JDBC Connection *if* the user passed in a Connection originally.
	 */
	public Connection close();

	/**
	 * Is this JdbcSession currently physically connected (meaning does it currently hold a JDBC Connection)?
	 *
	 * @return {@code true} if the JdbcSession currently hold a JDBC Connection; {@code false} if it does not.
	 */
	public boolean isPhysicallyConnected();

	/**
	 * Provides access to the registry of JDBC resources associated with this LogicalConnection.
	 *
	 * @return The JDBC resource registry.
	 *
	 * @throws org.hibernate.ResourceClosedException if the LogicalConnection is closed
	 */
	public ResourceRegistry getResourceRegistry();

}
