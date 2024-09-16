/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.cleaner;

/**
 * @author Christian Beikov
 */
public final class DatabaseCleanerContext {

	public static final DatabaseCleaner CLEANER;

	static {
		CLEANER = JdbcConnectionContext.workReturning(
				connection -> {
					final DatabaseCleaner[] cleaners = new DatabaseCleaner[] {
							new DB2DatabaseCleaner(),
							new H2DatabaseCleaner(),
							new SQLServerDatabaseCleaner(),
							new MySQL5DatabaseCleaner(),
							new MySQL8DatabaseCleaner(),
							new MariaDBDatabaseCleaner(),
							new OracleDatabaseCleaner(),
							new PostgreSQLDatabaseCleaner()
					};
					for ( DatabaseCleaner cleaner : cleaners ) {
						if ( cleaner.isApplicable( connection ) ) {
							return cleaner;
						}
					}
					return null;
				}
		);
	}

	private DatabaseCleanerContext() {
	}
}
