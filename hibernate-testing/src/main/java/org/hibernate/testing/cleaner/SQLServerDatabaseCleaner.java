/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
public class SQLServerDatabaseCleaner implements DatabaseCleaner {

	private static final Logger LOG = Logger.getLogger( SQLServerDatabaseCleaner.class.getName() );

	private final List<String> ignoredTables = new ArrayList<>();
	private final Map<String, List<String>> cachedTableNamesPerSchema = new HashMap<>();

	@Override
	public boolean isApplicable(Connection connection) {
		try {
			return connection.getMetaData().getDatabaseProductName().startsWith( "Microsoft SQL Server" );
		}
		catch (SQLException e) {
			throw new RuntimeException( "Could not resolve the database metadata!", e );
		}
	}

	@Override
	public void addIgnoredTable(String tableName) {
		ignoredTables.add( tableName );
	}

	@Override
	public void clearAllSchemas(Connection c) {
		cachedTableNamesPerSchema.clear();
		try (Statement s = c.createStatement()) {
			ResultSet rs;
			List<String> sqls = new ArrayList<>();

			// Collect schema objects
			LOG.log( Level.FINEST, "Collect schema objects: START" );
			rs = s.executeQuery(
					"SELECT 'ALTER TABLE [' + TABLE_SCHEMA + '].[' + TABLE_NAME + '] DROP CONSTRAINT [' + CONSTRAINT_NAME + ']' FROM INFORMATION_SCHEMA.CONSTRAINT_TABLE_USAGE " +
							"WHERE EXISTS (SELECT 1 FROM sys.Tables t JOIN sys.Schemas s ON t.schema_id = s.schema_id WHERE t.is_ms_shipped = 0 AND s.name = TABLE_SCHEMA AND t.name = TABLE_NAME) " +
							"AND EXISTS (SELECT 1 FROM sys.Foreign_keys WHERE name = CONSTRAINT_NAME)" );
			while ( rs.next() ) {
				sqls.add( rs.getString( 1 ) );
			}

			rs = s.executeQuery(
					"SELECT 'DROP VIEW [' + TABLE_SCHEMA + '].[' + TABLE_NAME + ']' FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'VIEW' " +
							"AND EXISTS (SELECT 1 FROM sys.Views t JOIN sys.Schemas s ON t.schema_id = s.schema_id WHERE t.is_ms_shipped = 0 AND s.name = TABLE_SCHEMA AND t.name = TABLE_NAME)" );
			while ( rs.next() ) {
				sqls.add( rs.getString( 1 ) );
			}

			rs = s.executeQuery(
					"SELECT 'DROP TABLE [' + TABLE_SCHEMA + '].[' + TABLE_NAME + ']' FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' " +
							"AND EXISTS (SELECT 1 FROM sys.Tables t JOIN sys.Schemas s ON t.schema_id = s.schema_id WHERE t.is_ms_shipped = 0 AND s.name = TABLE_SCHEMA AND t.name = TABLE_NAME)" );
			while ( rs.next() ) {
				sqls.add( rs.getString( 1 ) );
			}

			rs = s.executeQuery(
					"SELECT 'DROP SEQUENCE [' + SEQUENCE_SCHEMA + '].[' + SEQUENCE_NAME + ']' FROM INFORMATION_SCHEMA.SEQUENCES" );
			while ( rs.next() ) {
				sqls.add( rs.getString( 1 ) );
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
	public void clearSchema(Connection c, String schemaName) {
		cachedTableNamesPerSchema.remove( schemaName );
		try (Statement s = c.createStatement()) {
			ResultSet rs;
			List<String> sqls = new ArrayList<>();

			// Collect schema objects
			LOG.log( Level.FINEST, "Collect schema objects: START" );
			rs = s.executeQuery(
					"SELECT 'ALTER TABLE [' + TABLE_SCHEMA + '].[' + TABLE_NAME + '] DROP CONSTRAINT [' + CONSTRAINT_NAME + ']' FROM INFORMATION_SCHEMA.CONSTRAINT_TABLE_USAGE " +
							"WHERE EXISTS (SELECT 1 FROM sys.Tables t JOIN sys.Schemas s ON t.schema_id = s.schema_id WHERE t.is_ms_shipped = 0 AND s.name = TABLE_SCHEMA AND t.name = TABLE_NAME) " +
							"AND EXISTS (SELECT 1 FROM sys.Foreign_keys WHERE name = CONSTRAINT_NAME) " +
							"AND TABLE_SCHEMA = N'" + schemaName + "'" );
			while ( rs.next() ) {
				sqls.add( rs.getString( 1 ) );
			}

			rs = s.executeQuery(
					"SELECT 'DROP VIEW [' + TABLE_SCHEMA + '].[' + TABLE_NAME + ']' FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'VIEW' " +
							"AND EXISTS (SELECT 1 FROM sys.Views t JOIN sys.Schemas s ON t.schema_id = s.schema_id WHERE t.is_ms_shipped = 0 AND s.name = TABLE_SCHEMA AND t.name = TABLE_NAME) " +
							"AND TABLE_SCHEMA = N'" + schemaName + "'" );
			while ( rs.next() ) {
				sqls.add( rs.getString( 1 ) );
			}

			rs = s.executeQuery(
					"SELECT 'DROP TABLE [' + TABLE_SCHEMA + '].[' + TABLE_NAME + ']' FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' " +
							"AND EXISTS (SELECT 1 FROM sys.Tables t JOIN sys.Schemas s ON t.schema_id = s.schema_id WHERE t.is_ms_shipped = 0 AND s.name = TABLE_SCHEMA AND t.name = TABLE_NAME) " +
							"AND TABLE_SCHEMA = N'" + schemaName + "'" );
			while ( rs.next() ) {
				sqls.add( rs.getString( 1 ) );
			}

			rs = s.executeQuery(
					"SELECT 'DROP SEQUENCE [' + SEQUENCE_SCHEMA + '].[' + SEQUENCE_NAME + ']' FROM INFORMATION_SCHEMA.SEQUENCES WHERE " +
							"SEQUENCE_SCHEMA = N'" + schemaName + "'" );
			while ( rs.next() ) {
				sqls.add( rs.getString( 1 ) );
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
								"SELECT s.name, t.name FROM sys.Tables t JOIN sys.Schemas s ON t.schema_id = s.schema_id WHERE t.is_ms_shipped = 0" );
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
								"SELECT s.name, t.name FROM sys.Tables t JOIN sys.Schemas s ON t.schema_id = s.schema_id WHERE t.is_ms_shipped = 0 AND s.name = N'" + schemaName + "'" );
					}
					catch (SQLException sqlException) {
						throw new RuntimeException( sqlException );
					}
				}
		);
	}

	private void clearData0(Connection connection, String schemaName, Function<Statement, ResultSet> tablesProvider) {
		try (Statement s = connection.createStatement()) {
			List<String> cachedTableNames = cachedTableNamesPerSchema.get( schemaName );
			if ( cachedTableNames == null ) {
				cachedTableNames = new ArrayList<>();
				ResultSet rs = tablesProvider.apply( s );
				while ( rs.next() ) {
					String tableSchema = rs.getString( 1 );
					String tableName = rs.getString( 2 );
					if ( !ignoredTables.contains( tableName ) ) {
						cachedTableNames.add( tableSchema + "." + tableName );
					}
				}
				cachedTableNamesPerSchema.put( schemaName, cachedTableNames );
			}
			// Disable foreign keys
			LOG.log( Level.FINEST, "Disable foreign keys: START" );
			for ( String table : cachedTableNames ) {
				s.execute( "ALTER TABLE " + table + " NOCHECK CONSTRAINT ALL" );
			}
			LOG.log( Level.FINEST, "Disable foreign keys: END" );

			// Delete data
			LOG.log( Level.FINEST, "Deleting data: START" );
			for ( String table : cachedTableNames ) {
				s.execute( "DELETE FROM " + table );
			}
			LOG.log( Level.FINEST, "Deleting data: END" );

			// Enable foreign keys
			LOG.log( Level.FINEST, "Enabling foreign keys: START" );
			for ( String table : cachedTableNames ) {
				s.execute( "ALTER TABLE " + table + " WITH CHECK CHECK CONSTRAINT ALL" );
			}
			LOG.log( Level.FINEST, "Enabling foreign keys: END" );

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

}
