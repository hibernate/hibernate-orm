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

import java.io.Serializable;
import java.sql.Connection;

/**
 * LogicalConnection contract
 *
 * @author Steve Ebersole
 */
public interface LogicalConnection extends Serializable {
	/**
	 * Is this logical connection open?  Another phraseology sometimes used is: "are we
	 * logically connected"?
	 *
	 * @return True if logically connected; false otherwise.
	 */
	public boolean isOpen();

	/**
	 * Is this logical connection instance "physically" connected.  Meaning
	 * do we currently internally have a cached connection.
	 *
	 * @return True if physically connected; false otherwise.
	 */
	public boolean isPhysicallyConnected();

	/**
	 * Retrieves the connection currently "logically" managed by this LogicalConnectionImpl.
	 * <p/>
	 * Note, that we may need to obtain a connection to return here if a
	 * connection has either not yet been obtained (non-UserSuppliedConnectionProvider)
	 * or has previously been aggressively released.
	 *
	 * @todo ?? Move this to {@link LogicalConnectionImplementor} in lieu of {@link #getShareableConnectionProxy} and {@link #getDistinctConnectionProxy} ??
	 *
	 * @return The current Connection.
	 */
	public Connection getConnection();

	/**
	 * Retrieves the shareable connection proxy.
	 *
	 * @return The shareable connection proxy.
	 */
	public Connection getShareableConnectionProxy();

	/**
	 * Retrieves a distinct connection proxy.  It is distinct in that it is not shared with others unless the caller
	 * explicitly shares it.
	 *
	 * @return The distinct connection proxy.
	 */
	public Connection getDistinctConnectionProxy();

	/**
	 * Release the underlying connection and clean up any other resources associated
	 * with this logical connection.
	 * <p/>
	 * This leaves the logical connection in a "no longer usable" state.
	 *
	 * @return The application-supplied connection, or {@code null} if Hibernate was managing connection.
	 */
	public Connection close();

	/**
	 * Signals the end of current transaction in which this logical connection operated.
	 */
	public void afterTransaction();
}
