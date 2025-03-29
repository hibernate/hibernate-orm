/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc.leak;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLIdleConnectionCounter implements IdleConnectionCounter {

	public static final IdleConnectionCounter INSTANCE = new PostgreSQLIdleConnectionCounter();

	@Override
	public boolean appliesTo(Class<? extends Dialect> dialect) {
		return PostgreSQLDialect.class.isAssignableFrom( dialect );
	}

	@Override
	public int count(Connection connection) {
		try ( Statement statement = connection.createStatement() ) {
			try ( ResultSet resultSet = statement.executeQuery(
					"select count(*) " +
							"from pg_stat_activity " +
							"where state ilike '%idle%'" ) ) {
				while ( resultSet.next() ) {
					return resultSet.getInt( 1 );
				}
				return 0;
			}
		}
		catch ( SQLException e ) {
			throw new IllegalStateException( e );
		}
	}
}
