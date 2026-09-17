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
 * Database cleaner for GaussDB, whose gsjdbc4 driver is PostgreSQL-based (the
 * {@link PostgreSQLDatabaseCleaner} would otherwise match), but which rejects
 * {@code TRUNCATE ... RESTART IDENTITY} in MySQL-compatible mode.
 *
 * @author plafaith
 */
public class GaussDBDatabaseCleaner extends PostgreSQLDatabaseCleaner {

	@Override
	public boolean isApplicable(Connection connection) {
		// Distinguish GaussDB from real PostgreSQL by the GaussDB-only
		// `pg_database.datcompatibility` column, which real PostgreSQL does not have.
		try (Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(
						"select datcompatibility from pg_database where datname = current_database()" )) {
			return rs.next();
		}
		catch (SQLException e) {
			return false;
		}
	}

	/**
	 * GaussDB (centralized) exposes many system schemas of its own (dbe_*, pkg_*, cstore,
	 * snapshot, blockchain, db4ai, etc.) that are owned by a system user, so like for the
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

	/**
	 * GaussDB in MySQL-compatible mode (datcompatibility "B"/"M") rejects
	 * {@code TRUNCATE ... RESTART IDENTITY} with a syntax error, so only {@code CASCADE}
	 * is used there. A mode (Oracle-compatible) supports it.
	 */
	@Override
	protected boolean useRestartIdentity(Connection connection) {
		try (Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(
						"select datcompatibility from pg_database where datname = current_database()" )) {
			if ( rs.next() ) {
				final String mode = rs.getString( 1 );
				return !"M".equals( mode ) && !"B".equals( mode );
			}
		}
		catch (SQLException e) {
			// not GaussDB — assume real PostgreSQL, which supports RESTART IDENTITY
		}
		return true;
	}

	/**
	 * GaussDB in MySQL-compatible mode (datcompatibility "B"/"M") treats double quotes as string
	 * literals, not identifier quoting, so backticks (MySQL-style) are used there; A mode keeps
	 * the PostgreSQL-style double quotes.
	 */
	@Override
	protected String quoteIdentifier(Connection connection, String identifier) {
		if ( isMMode( connection ) ) {
			return "`" + identifier + "`";
		}
		return super.quoteIdentifier( connection, identifier );
	}

	/**
	 * GaussDB in MySQL-compatible mode rejects the {@code CASCADE} keyword on
	 * {@code DROP SCHEMA}, but drops schema contents anyway (MySQL semantics).
	 */
	@Override
	protected boolean dropSchemaCascade(Connection connection) {
		return !isMMode( connection );
	}

	private boolean isMMode(Connection connection) {
		try (Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(
						"select datcompatibility from pg_database where datname = current_database()" )) {
			if ( rs.next() ) {
				final String mode = rs.getString( 1 );
				return "M".equals( mode ) || "B".equals( mode );
			}
		}
		catch (SQLException e) {
			// probe failed — keep the PostgreSQL-style default
		}
		return false;
	}
}
