/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * A specialized {@link ConnectionCreator} which uses {@link DriverManager#getConnection(String, Properties)}
 * to obtain JDBC connections.
 *
 * @author Steve Ebersole
 */
public class DriverManagerConnectionCreator extends BasicConnectionCreator {
	public DriverManagerConnectionCreator(
			ServiceRegistryImplementor serviceRegistry,
			String url,
			Properties connectionProps,
			Boolean autocommit,
			Integer isolation,
			String initSql) {
		super( serviceRegistry, url, connectionProps, autocommit, isolation, initSql );
	}

	@Override
	protected Connection makeConnection(String url, Properties connectionProps) {
		try {
			return DriverManager.getConnection( url, connectionProps );
		}
		catch (SQLException e) {
			throw convertSqlException( "Error calling DriverManager.getConnection()", e );
		}
	}
}
