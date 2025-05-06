/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc.leak;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;


import org.hibernate.testing.jdbc.JdbcProperties;
import org.hibernate.testing.orm.junit.DialectContext;

/**
 * @author Vlad Mihalcea
 */
public class ConnectionLeakUtil {

	private JdbcProperties jdbcProperties = JdbcProperties.INSTANCE;

	private List<IdleConnectionCounter> idleConnectionCounters = Arrays.asList(
			H2IdleConnectionCounter.INSTANCE,
			OracleIdleConnectionCounter.INSTANCE,
			PostgreSQLIdleConnectionCounter.INSTANCE,
			MySQLIdleConnectionCounter.INSTANCE
	);

	private IdleConnectionCounter connectionCounter;

	private int connectionLeakCount;

	public ConnectionLeakUtil() {
		for ( IdleConnectionCounter connectionCounter : idleConnectionCounters ) {
			if ( connectionCounter.appliesTo( DialectContext.getDialect().getClass() ) ) {
				this.connectionCounter = connectionCounter;
				break;
			}
		}
		if ( connectionCounter != null ) {
			connectionLeakCount = countConnectionLeaks();
		}
	}

	public void assertNoLeaks() {
		if ( connectionCounter != null ) {
			int currentConnectionLeakCount = countConnectionLeaks();
			int diff = currentConnectionLeakCount - connectionLeakCount;
			if ( diff > 0 ) {
				throw new ConnectionLeakException( String.format(
						"%d connection(s) have been leaked! Previous leak count: %d, Current leak count: %d",
						diff,
						connectionLeakCount,
						currentConnectionLeakCount
				) );
			}
		}
	}

	private int countConnectionLeaks() {
		try ( Connection connection = newConnection() ) {
			return connectionCounter.count( connection );
		}
		catch ( SQLException e ) {
			throw new IllegalStateException( e );
		}
	}

	/**
	 * Obtain a new JDBC Connection.
	 *
	 * @return JDBC Connection
	 */
	private Connection newConnection() {
		try {
			return DriverManager.getConnection(
					jdbcProperties.getUrl(),
					jdbcProperties.getUser(),
					jdbcProperties.getPassword()
			);
		}
		catch ( SQLException e ) {
			throw new IllegalStateException( e );
		}
	}
}
