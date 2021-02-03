/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cleaner;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Christian Beikov
 */
public class MySQL8DatabaseCleaner extends AbstractMySQLDatabaseCleaner {

	@Override
	public boolean isApplicable(Connection connection) {
		try {
			return connection.getMetaData().getDatabaseProductName().startsWith( "MySQL" ) && connection.getMetaData()
					.getDatabaseMajorVersion() >= 8;
		}
		catch (SQLException e) {
			throw new RuntimeException( "Could not resolve the database metadata!", e );
		}
	}

	@Override
	protected String createClearingStatementForTable(String tableSchema, String tableName) {
		return "TRUNCATE " + tableSchema + "." + tableName;
	}
}
