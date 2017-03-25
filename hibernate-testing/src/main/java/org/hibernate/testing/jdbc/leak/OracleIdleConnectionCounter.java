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
import org.hibernate.dialect.Oracle10gDialect;

/**
 * @author Vlad Mihalcea
 */
public class OracleIdleConnectionCounter implements IdleConnectionCounter {

	public static final IdleConnectionCounter INSTANCE = new OracleIdleConnectionCounter();

	@Override
	public boolean appliesTo(Class<? extends Dialect> dialect) {
		return Oracle10gDialect.class.isAssignableFrom( dialect );
	}

	@Override
	public int count(Connection connection) {
		try ( Statement statement = connection.createStatement() ) {
			try ( ResultSet resultSet = statement.executeQuery(
					"SELECT count(*) " +
							"FROM v$session " +
							"where status = 'INACTIVE'" ) ) {
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
