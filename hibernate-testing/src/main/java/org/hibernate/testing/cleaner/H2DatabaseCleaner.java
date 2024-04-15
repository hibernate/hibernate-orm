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
public class H2DatabaseCleaner implements DatabaseCleaner {

	private static final Logger LOG = Logger.getLogger( H2DatabaseCleaner.class.getName() );
	private static final String SYSTEM_SCHEMAS = "'INFORMATION_SCHEMA'";

	private final List<String> ignoredTables = new ArrayList<>();
	private final Map<String, List<String>> cachedTableNamesPerSchema = new HashMap<>();

	@Override
	public boolean isApplicable(Connection connection) {
		try {
			return connection.getMetaData().getDatabaseProductName().startsWith( "H2" );
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
			LOG.log( Level.FINEST, "Dropping schema objects: START" );
			s.execute( "DROP ALL OBJECTS" );
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
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearAllData(Connection connection) {
		clearData0(
				connection,
				null,
				statement -> {
					try {
						return statement.executeQuery(
								"SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA NOT IN (" + SYSTEM_SCHEMAS + ")" );
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
			// Disable foreign keys
			LOG.log( Level.FINEST, "Disable foreign keys: START" );
			s.execute( "SET REFERENTIAL_INTEGRITY FALSE" );
			LOG.log( Level.FINEST, "Disable foreign keys: END" );

			// Delete data
			LOG.log( Level.FINEST, "Deleting data: START" );
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
			for ( String table : cachedTableNames ) {
				s.execute( "TRUNCATE TABLE " + table );
			}
			LOG.log( Level.FINEST, "Deleting data: END" );

			// Enable foreign keys
			LOG.log( Level.FINEST, "Enabling foreign keys: START" );
			s.execute( "SET REFERENTIAL_INTEGRITY TRUE" );
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
