/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.cleaner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Christian Beikov
 */
public class PostgreSQLDatabaseCleaner implements DatabaseCleaner {

	private static final Logger LOG = Logger.getLogger( PostgreSQLDatabaseCleaner.class.getName() );

	private final List<String> ignoredTables = new ArrayList<>();
	protected final Map<String, String> truncateSqlPerSchema = new HashMap<>();

	@Override
	public boolean isApplicable(Connection connection) {
		try {
			return connection.getMetaData().getDatabaseProductName().startsWith( "PostgreSQL" )
					&& isPostgresql( connection );
		}
		catch (SQLException e) {
			throw new RuntimeException( "Could not resolve the database metadata!", e );
		}
	}

	@Override
	public void addIgnoredTable(String tableName) {
		ignoredTables.add( tableName.toLowerCase() );
	}

	@Override
	public void clearAllSchemas(Connection connection) {
		truncateSqlPerSchema.clear();
		clearSchema0(
				connection,
				statement -> {
					try {
						return statement.executeQuery(
								"SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME <> 'information_schema' AND SCHEMA_NAME <> 'sys' AND SCHEMA_NAME <> 'public' AND SCHEMA_NAME NOT LIKE 'pg_%'" );
					}
					catch (SQLException sqlException) {
						throw new RuntimeException( sqlException );
					}
				}
		);
	}

	@Override
	public void clearSchema(Connection connection, String schemaName) {
		truncateSqlPerSchema.remove( schemaName );
		clearSchema0(
				connection,
				statement -> {
					try {
						return statement.executeQuery(
								"SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + schemaName + "'" );
					}
					catch (SQLException sqlException) {
						throw new RuntimeException( sqlException );
					}
				}
		);
	}

	protected void clearSchema0(Connection c, Function<Statement, ResultSet> schemasProvider) {
		try (Statement s = c.createStatement()) {
			ResultSet rs;
			final List<String> sqls = new ArrayList<>();

			// Collect schema objects
			String user = c.getMetaData().getUserName();
			LOG.log( Level.FINEST, "Collect schema objects: START" );
			Map<String, List<String>> schemaExtensions = new HashMap<>();
			try (Statement s2 = c.createStatement()) {
				// Select the raw names and build the statements in Java: `||` string
				// concatenation is not available in GaussDB MySQL-compatible (M) mode.
				rs = s2.executeQuery(
						"SELECT ns.nspname, e.extname FROM pg_extension e JOIN pg_catalog.pg_namespace ns ON e.extnamespace = ns.oid WHERE e.extname <> 'plpgsql'"
				);
				while ( rs.next() ) {
					schemaExtensions.computeIfAbsent( rs.getString( 1 ), k -> new ArrayList<>() )
							.add( "CREATE EXTENSION " + rs.getString( 2 ) + " SCHEMA " + quoteIdentifier( c, rs.getString( 1 ) ) );
				}
			}
			rs = schemasProvider.apply( s );
			while ( rs.next() ) {
				String schema = rs.getString( 1 );
				sqls.add( "DROP SCHEMA " + quoteIdentifier( c, schema ) + ( dropSchemaCascade( c ) ? " CASCADE" : "" ) );
				sqls.add( "CREATE SCHEMA " + quoteIdentifier( c, schema ) );
				sqls.add( "GRANT ALL ON SCHEMA " + quoteIdentifier( c, schema ) + " TO " + quoteIdentifier( c, user ) );
				List<String> extensions = schemaExtensions.get( schema );
				if ( extensions != null ) {
					sqls.addAll( extensions );
				}
			}
			LOG.log( Level.FINEST, "Collect schema objects: END" );

			LOG.log( Level.FINEST, "Dropping schema objects: START" );
			for ( String sql : sqls ) {
				s.execute( sql );
			}
			LOG.log( Level.FINEST, "Dropping schema objects: END" );

			LOG.log( Level.FINEST, "Committing: START" );
			c.commit();
			LOG.log( Level.FINEST, "Committing: END" );
		}
		catch (SQLException e) {
			try {
				c.rollback();
			}
			catch (SQLException e1) {
				e.addSuppressed( e1 );
			}

			throw new RuntimeException( e );
		}
	}

	@Override
	public void clearAllData(Connection connection) {
		clearData0(
				connection,
				null,
				statement -> {
					try {
						return statement.executeQuery(
								"SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA <> 'information_schema' AND SCHEMA_NAME NOT LIKE 'pg_%'" );
					}
					catch (SQLException sqlException) {
						throw new RuntimeException( sqlException );
					}
				}
		);
	}

	@Override
	public void clearData(Connection connection, String schemaName) {
		clearData0(
				connection,
				schemaName,
				statement -> {
					try {
						return statement.executeQuery(
								"SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '" + schemaName + "'" );
					}
					catch (SQLException sqlException) {
						throw new RuntimeException( sqlException );
					}
				}
		);
	}

	private void clearData0(Connection connection, String schemaName, Function<Statement, ResultSet> tablesProvider) {
		try (Statement s = connection.createStatement()) {
			// Delete data
			LOG.log( Level.FINEST, "Deleting data: START" );
			String truncateSql = truncateSqlPerSchema.get( schemaName );
			if ( truncateSql == null ) {
				StringBuilder sb = new StringBuilder();
				sb.append( "TRUNCATE TABLE " );
				ResultSet rs = tablesProvider.apply( s );
				while ( rs.next() ) {
					String tableSchema = rs.getString( 1 );
					String tableName = rs.getString( 2 );
					if ( !ignoredTables.contains( tableName ) ) {
						sb.append( quoteIdentifier( connection, tableSchema ) );
						sb.append( '.' );
						sb.append( quoteIdentifier( connection, tableName ) );
						sb.append( ',' );
					}
				}
				sb.setCharAt( sb.length() - 1, ' ' );
				sb.append( useRestartIdentity( connection ) ? "RESTART IDENTITY CASCADE" : "CASCADE" );
				truncateSql = sb.toString();
				truncateSqlPerSchema.put( schemaName, truncateSql );
			}
			s.execute( truncateSql );
			LOG.log( Level.FINEST, "Deleting data: END" );

			LOG.log( Level.FINEST, "Committing: START" );
			connection.commit();
			LOG.log( Level.FINEST, "Committing: END" );
		}
		catch (SQLException e) {
			try {
				connection.rollback();
			}
			catch (SQLException e1) {
				e.addSuppressed( e1 );
			}

			throw new RuntimeException( e );
		}
	}

	// We need this check to differentiate between Postgresql and Cockroachdb
	private boolean isPostgresql(Connection connection) {
		try (Statement stmt = connection.createStatement()) {
			ResultSet rs = stmt.executeQuery( "select version() " );
			while ( rs.next() ) {
				String version = rs.getString( 1 );
				return version.contains( "PostgreSQL" );
			}
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
		return false;
	}

	/**
	 * Whether {@code TRUNCATE ... RESTART IDENTITY} can be appended. Real PostgreSQL supports it;
	 * subclasses for databases that do not (e.g. {@link GaussDBDatabaseCleaner} for GaussDB in
	 * MySQL-compatible mode) may override this.
	 */
	protected boolean useRestartIdentity(Connection connection) {
		return true;
	}

	/**
	 * Quotes the given identifier for DDL statements. Real PostgreSQL uses double quotes;
	 * subclasses may override this, e.g. for databases that interpret double quotes as string
	 * literals (like GaussDB in MySQL-compatible mode, which needs backticks).
	 */
	protected String quoteIdentifier(Connection connection, String identifier) {
		return "\"" + identifier + "\"";
	}

	/**
	 * Whether {@code DROP SCHEMA ... CASCADE} can be used. Real PostgreSQL requires it to drop
	 * non-empty schemas; subclasses may override this for databases that reject the CASCADE
	 * keyword but drop schema contents anyway (like GaussDB in MySQL-compatible mode).
	 */
	protected boolean dropSchemaCascade(Connection connection) {
		return true;
	}

}
