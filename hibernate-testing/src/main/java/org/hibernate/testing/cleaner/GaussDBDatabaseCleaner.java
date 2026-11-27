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
		// The driver metadata check avoids running the datcompatibility probe (and any error
		// handling for it) on databases that are not GaussDB at all; the probe itself is what
		// separates the compatibility modes. Without a GaussDB check, this cleaner would apply
		// to any database that is not PostgreSQL-compatible.
		try {
			if ( !connection.getMetaData().getDatabaseProductName().startsWith( "GaussDB" ) ) {
				return false;
			}
		}
		catch (SQLException e) {
			throw new RuntimeException( "Could not resolve the database metadata!", e );
		}
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
	 * The datcompatibility mode of the connected database, or {@code null} if the probe fails.
	 * The probe is only reached when the driver metadata says the product is a GaussDB, so a
	 * failure here would be unexpected; roll the transaction back regardless, so a failed
	 * statement cannot leave the current transaction in an aborted state.
	 */
	static String compatibilityMode(Connection connection) {
		try (Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(
						"select datcompatibility from pg_database where datname = current_database()" )) {
			return rs.next() ? rs.getString( 1 ) : null;
		}
		catch (SQLException e) {
			try {
				connection.rollback();
			}
			catch (SQLException e2) {
				// Ignore
			}
			return null;
		}
	}
}
