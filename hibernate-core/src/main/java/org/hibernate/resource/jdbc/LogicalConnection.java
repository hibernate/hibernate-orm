/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
