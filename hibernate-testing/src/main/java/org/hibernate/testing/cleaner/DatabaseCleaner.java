/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cleaner;

import java.sql.Connection;

/**
 * @author Christian Beikov
 */
public interface DatabaseCleaner {

	static void clearSchemas() {
		final DatabaseCleaner cleaner = DatabaseCleanerContext.CLEANER;
		if ( cleaner != null ) {
			JdbcConnectionContext.work( cleaner::clearAllSchemas );
		}
	}

	static void clearData() {
		final DatabaseCleaner cleaner = DatabaseCleanerContext.CLEANER;
		if ( cleaner != null ) {
			JdbcConnectionContext.work( cleaner::clearAllData );
		}
	}

	void addIgnoredTable(String tableName);

	boolean isApplicable(Connection connection);

	void clearAllSchemas(Connection connection);

	void clearSchema(Connection connection, String schemaName);

	void clearAllData(Connection connection);

	void clearData(Connection connection, String schemaName);

}
