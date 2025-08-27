/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.service.ServiceRegistry;

/**
 * A specialized {@link ConnectionCreator} which uses {@link DriverManager#getConnection(String, Properties)}
 * to obtain JDBC connections.
 *
 * @author Steve Ebersole
 */
public class DriverManagerConnectionCreator extends BasicConnectionCreator {
	public DriverManagerConnectionCreator(
			ServiceRegistry serviceRegistry,
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
			throw convertSqlException( "Error calling JDBC 'DriverManager.getConnection()'", e );
		}
	}
}
