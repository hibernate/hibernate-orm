/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * A specialized {@link ConnectionCreator} which uses {@link Driver#connect(String, Properties)}
 * to obtain JDBC connections.
 *
 * @author Steve Ebersole
 */
public class DriverConnectionCreator extends BasicConnectionCreator {
	private final Driver driver;

	public DriverConnectionCreator(
			Driver driver,
			ServiceRegistryImplementor serviceRegistry,
			String url,
			Properties connectionProps,
			Boolean autocommit,
			Integer isolation,
			String initSql) {
		super( serviceRegistry, url, connectionProps, autocommit, isolation, initSql );
		this.driver = driver;
	}

	@Override
	protected Connection makeConnection(String url, Properties connectionProps) {
		try {
			return driver.connect( url, connectionProps );
		}
		catch (SQLException e) {
			throw convertSqlException( "Error calling Driver.connect()", e );
		}
	}
}
