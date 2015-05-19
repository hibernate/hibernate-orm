/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;

/**
 * Contract for creating JDBC Connections on demand
 *
 * @author Steve Ebersole
 */
interface ConnectionCreator {
	/**
	 * Obtain the URL to which this creator connects.  Intended just for informational (logging) purposes.
	 *
	 * @return The connection URL.
	 */
	public String getUrl();

	/**
	 * Create a Connection
	 *
	 * @return The created Connection
	 */
	public Connection createConnection();
}
