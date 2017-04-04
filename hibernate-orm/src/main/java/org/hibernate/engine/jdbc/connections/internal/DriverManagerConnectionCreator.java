/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * A specialized ConnectionCreator which uses {@link DriverManager#getConnection} to generate Connections
 *
 * @author Steve Ebersole
 */
public class DriverManagerConnectionCreator extends BasicConnectionCreator {
	public DriverManagerConnectionCreator(
			ServiceRegistryImplementor serviceRegistry,
			String url,
			Properties connectionProps,
			Boolean autocommit,
			Integer isolation) {
		super( serviceRegistry, url, connectionProps, autocommit, isolation );
	}

	@Override
	protected Connection makeConnection(String url, Properties connectionProps) {
		try {
			return DriverManager.getConnection( url, connectionProps );
		}
		catch (SQLException e) {
			throw convertSqlException( "Error calling DriverManager#getConnection", e );
		}
	}
}
