/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.cleaner;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database cleaner for GaussDB in M compatibility mode (MySQL-compatible), which differs
 * from A mode (see {@link GaussDBDatabaseCleaner}) in that:
 * <ul>
 *     <li>{@code TRUNCATE ... RESTART IDENTITY} is rejected with a syntax error</li>
 *     <li>double quotes are string literals, not identifier quoting, so backticks
 *     (MySQL-style) are used for DDL identifiers</li>
 *     <li>{@code DROP SCHEMA ... CASCADE} is rejected, but plain {@code DROP SCHEMA}
 *     drops the schema contents anyway</li>
 * </ul>
 *
 * @author plafaith
 */
public class GaussDBMModeDatabaseCleaner extends PostgreSQLDatabaseCleaner {

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
		final String mode = GaussDBDatabaseCleaner.compatibilityMode( connection );
		return "M".equals( mode ) || "B".equals( mode );
	}

	/**
	 * Same as in A mode: GaussDB exposes many system schemas of its own (dbe_*, pkg_*,
	 * cstore, snapshot, blockchain, db4ai, etc.) that are owned by a system user, so only
	 * schemas owned by the current user are cleared.
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
		return false;
	}

	@Override
	protected String quoteIdentifier(Connection connection, String identifier) {
		// M mode treats double quotes as string literals, not identifier quoting
		return "`" + identifier + "`";
	}

	@Override
	protected boolean dropSchemaCascade(Connection connection) {
		// M mode rejects the CASCADE keyword on DROP SCHEMA, but plain DROP SCHEMA
		// drops the schema contents anyway (MySQL semantics)
		return false;
	}
}
