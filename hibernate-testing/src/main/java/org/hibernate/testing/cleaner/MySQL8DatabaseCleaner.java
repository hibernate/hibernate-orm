/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
