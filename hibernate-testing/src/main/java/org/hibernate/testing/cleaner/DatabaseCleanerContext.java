/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
