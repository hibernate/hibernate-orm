/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.jdbc.connections.spi;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.service.Service;
import org.hibernate.service.spi.Wrapped;

/**
 * @author Steve Ebersole
 */
public interface MultiTenantConnectionProvider extends Service, Wrapped {
	/**
	 * Allows access to the database metadata of the underlying database(s) in situations where we do not have a
	 * tenant id (like startup processing, for example).
	 *
	 * @return The database metadata.
	 *
	 * @throws SQLException Indicates a problem opening a connection
	 */
	public Connection getAnyConnection() throws SQLException;

	/**
	 * Release a connection obtained from {@link #getAnyConnection}
	 *
	 * @param connection The JDBC connection to release
	 *
	 * @throws SQLException Indicates a problem closing the connection
	 */
	public void releaseAnyConnection(Connection connection) throws SQLException;

	/**
	 * Obtains a connection for Hibernate use according to the underlying strategy of this provider.
	 *
	 * @param tenantIdentifier The identifier of the tenant for which to get a connection
	 *
	 * @return The obtained JDBC connection
	 *
	 * @throws SQLException Indicates a problem opening a connection
	 * @throws org.hibernate.HibernateException Indicates a problem otherwise obtaining a connection.
	 */
	public Connection getConnection(String tenantIdentifier) throws SQLException;

	/**
	 * Release a connection from Hibernate use.
	 *
	 * @param connection The JDBC connection to release
	 * @param tenantIdentifier The identifier of the tenant.
	 *
	 * @throws SQLException Indicates a problem closing the connection
	 * @throws org.hibernate.HibernateException Indicates a problem otherwise releasing a connection.
	 */
	public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException;

	/**
	 * Does this connection provider support aggressive release of JDBC
	 * connections and re-acquisition of those connections (if need be) later?
	 * <p/>
	 * This is used in conjunction with {@link org.hibernate.cfg.Environment#RELEASE_CONNECTIONS}
	 * to aggressively release JDBC connections.  However, the configured ConnectionProvider
	 * must support re-acquisition of the same underlying connection for that semantic to work.
	 * <p/>
	 * Typically, this is only true in managed environments where a container
	 * tracks connections by transaction or thread.
	 *
	 * Note that JTA semantic depends on the fact that the underlying connection provider does
	 * support aggressive release.
	 *
	 * @return {@code true} if aggressive releasing is supported; {@code false} otherwise.
	 */
	public boolean supportsAggressiveRelease();
}
