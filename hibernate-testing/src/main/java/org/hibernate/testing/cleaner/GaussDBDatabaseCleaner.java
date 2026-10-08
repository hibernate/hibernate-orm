/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.cleaner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database cleaner for GaussDB in A compatibility mode (openGauss, Oracle-compatible),
 * whose gsjdbc4 driver is PostgreSQL-based (the {@link PostgreSQLDatabaseCleaner} would
 * otherwise match). For M compatibility mode see {@link GaussDBMModeDatabaseCleaner}.
 *
 * @author plafaith
 */
public class GaussDBDatabaseCleaner extends PostgreSQLDatabaseCleaner {

	@Override
	public boolean isApplicable(Connection connection) {
		return !isMCompatibilityMode( connection );
	}

	/**
	 * GaussDB exposes many system schemas of its own (dbe_*, pkg_*, cstore, snapshot,
	 * blockchain, db4ai, etc.) that are owned by a system user, so like for the
	 * PostgreSQL-standard ones they must not be dropped; only schemas owned by the current
	 * user are cleared. The pg_catalog query is used because the M-mode (MySQL-compatible)
	 * {@code INFORMATION_SCHEMA.SCHEMATA} does not expose a {@code SCHEMA_OWNER} column.
	 */
	@Override
	public void clearAllSchemas(Connection connection) {
		truncateSqlPerSchema.clear();
		clearSchema0(
				connection,
				statement -> {
					try {
						return statement.executeQuery(
								"SELECT nspname FROM pg_catalog.pg_namespace WHERE nspname NOT LIKE 'pg_%'"
										+ " AND nspname NOT LIKE 'dbe_%' AND nspname NOT LIKE 'pkg_%'"
										+ " AND nspname <> 'information_schema' AND nspname <> 'sys' AND nspname <> 'public'"
										+ " AND pg_get_userbyid( nspowner ) = current_user" );
					}
					catch (SQLException sqlException) {
						throw new RuntimeException( sqlException );
					}
				}
		);
	}

	@Override
	protected boolean useRestartIdentity(Connection connection) {
		return true;
	}

	static boolean isMCompatibilityMode(Connection connection) {
		try (Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(
						"select datcompatibility from pg_database where datname = current_database()" )) {
			if ( rs.next() ) {
				final String mode = rs.getString( 1 );
				return "M".equals( mode ) || "B".equals( mode );
			}
		}
		catch (SQLException e) {
			// not GaussDB or the probe failed
		}
		return false;
	}
}
