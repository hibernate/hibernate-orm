/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jdbc.leak;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQL5Dialect;

/**
 * @author Vlad Mihalcea
 */
public class MySQLIdleConnectionCounter implements IdleConnectionCounter {

	public static final IdleConnectionCounter INSTANCE = new MySQLIdleConnectionCounter();

	@Override
	public boolean appliesTo(Class<? extends Dialect> dialect) {
		return MySQL5Dialect.class.isAssignableFrom( dialect );
	}

	@Override
	public int count(Connection connection) {
		try ( Statement statement = connection.createStatement() ) {
			try ( ResultSet resultSet = statement.executeQuery(
					"SHOW PROCESSLIST" ) ) {
				int count = 0;
				while ( resultSet.next() ) {
					String state = resultSet.getString( "command" );
					if ( "sleep".equalsIgnoreCase( state ) ) {
						count++;
					}
				}
				return count;
			}
		}
		catch ( SQLException e ) {
			throw new IllegalStateException( e );
		}
	}
}
