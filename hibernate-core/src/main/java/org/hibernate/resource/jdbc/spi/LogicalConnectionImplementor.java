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
package org.hibernate.resource.jdbc.spi;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Connection;

import org.hibernate.resource.jdbc.LogicalConnection;

/**
 * SPI contract for LogicalConnection
 *
 * @author Steve Ebersole
 */
public interface LogicalConnectionImplementor extends LogicalConnection {
	/**
	 * Exposes access to the "real" Connection.
	 *
	 * @todo : expose Connection as here?  or accept(WorkInConnection) where WorkInConnection is given access to Connection?
	 *
	 * @return The connection
	 */
	Connection getPhysicalConnection();

	/**
	 * Notification indicating a JDBC statement has been executed to trigger
	 * {@link org.hibernate.ConnectionReleaseMode#AFTER_STATEMENT} releasing if needed
	 */
	void afterStatement();

	/**
	 * Notification indicating a transaction has completed to trigger
	 * {@link org.hibernate.ConnectionReleaseMode#AFTER_TRANSACTION} releasing if needed
	 */
	void afterTransaction();

	/**
	 * Manually disconnect the underlying JDBC Connection.  The assumption here
	 * is that the manager will be reconnected at a later point in time.
	 *
	 * @return The connection maintained here at time of disconnect.  {@code null} if
	 * there was no connection cached internally.
	 */
	Connection manualDisconnect();

	/**
	 * Manually reconnect the underlying JDBC Connection.  Should be called at some point after manualDisconnect().
	 *
	 * @param suppliedConnection For user supplied connection strategy the user needs to hand us the connection
	 * with which to reconnect.  It is an error to pass a connection in the other strategies.
	 */
	void manualReconnect(Connection suppliedConnection);

	/**
	 * Creates a shareable copy of itself for use in "shared sessions"
	 *
	 * @return The shareable copy.
	 */
	LogicalConnectionImplementor makeShareableCopy();

	PhysicalJdbcTransaction getPhysicalJdbcTransaction();

	/**
	 * Serialization hook
	 *
	 * @param oos The stream to write out state to
	 *
	 * @throws java.io.IOException Problem accessing stream
	 */
	void serialize(ObjectOutputStream oos) throws IOException;
}
