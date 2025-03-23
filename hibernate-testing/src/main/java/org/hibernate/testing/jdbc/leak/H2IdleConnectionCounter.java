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
import org.hibernate.dialect.H2Dialect;

/**
 * @author Vlad Mihalcea
 */
public class H2IdleConnectionCounter implements IdleConnectionCounter {

	public static final IdleConnectionCounter INSTANCE = new H2IdleConnectionCounter();

	@Override
	public boolean appliesTo(Class<? extends Dialect> dialect) {
		return H2Dialect.class.isAssignableFrom( dialect );
	}

	@Override
	public int count(Connection connection) {
		try ( Statement statement = connection.createStatement() ) {
			try ( ResultSet resultSet = statement.executeQuery(
					"select count(*) " +
							"from information_schema.sessions " +
							"where statement is null" ) ) {
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
