/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.spi;
import java.sql.Connection;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.JDBCException;

/**
 * The "internal" contract for LogicalConnection
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public interface LogicalConnectionImplementor extends LogicalConnection {
	/**
	 * Obtains the JDBC services associated with this logical connection.
	 *
	 * @return JDBC services
	 */
	public JdbcServices getJdbcServices();

	/**
	 * Add an observer interested in notification of connection events.
	 *
	 * @param observer The observer.
	 */
	public void addObserver(ConnectionObserver observer);

	/**
	 * Remove an observer
	 *
	 * @param connectionObserver The observer to remove.
	 */
	public void removeObserver(ConnectionObserver connectionObserver);

	/**
	 * The release mode under which this logical connection is operating.
	 *
	 * @return the release mode.
	 */
	public ConnectionReleaseMode getConnectionReleaseMode();

	/**
	 * Manually disconnect the underlying JDBC Connection.  The assumption here
	 * is that the manager will be reconnected at a later point in time.
	 *
	 * @return The connection maintained here at time of disconnect.  Null if
	 * there was no connection cached internally.
	 */
	public Connection manualDisconnect();

	/**
	 * Manually reconnect the underlying JDBC Connection.  Should be called at some point after manualDisconnect().
	 *
	 * @param suppliedConnection For user supplied connection strategy the user needs to hand us the connection
	 * with which to reconnect.  It is an error to pass a connection in the other strategies.
	 */
	public void manualReconnect(Connection suppliedConnection);

	/**
	 * Perform an aggressive release
	 */
	public void aggressiveRelease();

	/**
	 * Release any held connection.
	 *
	 * @throws JDBCException Indicates a problem releasing the connection
	 */
	public void releaseConnection() throws JDBCException;

	/**
	 * Is this logical connection in auto-commit mode?
	 *
	 * @return {@code true} if auto-commit
	 */
	public boolean isAutoCommit();

	/**
	 * Callback to notify all registered observers of a connection being prepared.
	 */
	public void notifyObserversStatementPrepared();

	/**
	 * Does this logical connection wrap a user/application supplied connection?
	 *
	 * @return {@code true} if the underlying connection was user supplied.
	 */
	public boolean isUserSuppliedConnection();
}
