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
		// datcompatibility only exists on GaussDB (PostgreSQL has no such column on pg_database,
		// and other databases do not have the pg_catalog at all), so a successful probe means
		// the connection is to a GaussDB: without this check, this cleaner would apply to any
		// database that is not PostgreSQL-compatible.
		final String mode = compatibilityMode( connection );
		return mode != null && !"M".equals( mode ) && !"B".equals( mode );
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

	/**
	 * The datcompatibility mode of the connected database, or {@code null} if the connection is
	 * not to a GaussDB: the pg_database.datcompatibility column is GaussDB-specific, so the probe
	 * fails (no such table, or no such column as on PostgreSQL) for every other database.
	 */
	static String compatibilityMode(Connection connection) {
		try (Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(
						"select datcompatibility from pg_database where datname = current_database()" )) {
			return rs.next() ? rs.getString( 1 ) : null;
		}
		catch (SQLException e) {
			// not GaussDB
			return null;
		}
	}
}
