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
public class OracleDatabaseCleaner implements DatabaseCleaner {

	private static final Logger LOG = Logger.getLogger( OracleDatabaseCleaner.class.getName() );
	private static final String SYSTEM_SEQUENCE_OWNERS = "'SYS'," +
			"'CTXSYS'," +
			"'DVSYS'," +
			"'OJVMSYS'," +
			"'ORDDATA'," +
			"'MDSYS'," +
			"'OLAPSYS'," +
			"'LBACSYS'," +
			"'XDB'," +
			"'WMSYS'";

	private final List<String> ignoredTables = new ArrayList<>();
	private final Map<String, List<String>> cachedTruncateTableSqlPerSchema = new HashMap<>();
	private final Map<String, List<String>> cachedConstraintDisableSqlPerSchema = new HashMap<>();
	private final Map<String, List<String>> cachedConstraintEnableSqlPerSchema = new HashMap<>();

	@Override
	public boolean isApplicable(Connection connection) {
		try {
			return connection.getMetaData().getDatabaseProductName().startsWith( "Oracle" );
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
	public void clearAllSchemas(Connection connection) {
		cachedTruncateTableSqlPerSchema.clear();
		cachedConstraintDisableSqlPerSchema.clear();
		cachedConstraintEnableSqlPerSchema.clear();
		clearSchema0(
				connection,
				statement -> {
					try {
						return statement.executeQuery(
								"SELECT 'DROP TABLE ' || owner || '.\"' || table_name || '\" CASCADE CONSTRAINTS' " +
										"FROM all_tables " +
										// Only look at tables owned by the current user
										"WHERE owner = sys_context('USERENV', 'SESSION_USER')" +
										// Normally, user tables aren't in sysaux
										"      AND tablespace_name NOT IN ('SYSAUX')" +
										// Apparently, user tables have global stats off
										"      AND global_stats = 'NO'" +
										// Exclude the tables with names starting like 'DEF$_'
										"      AND table_name NOT LIKE 'DEF$\\_%' ESCAPE '\\'" +
										" UNION ALL " +
										"SELECT 'DROP SEQUENCE ' || sequence_owner || '.' || sequence_name FROM all_sequences WHERE sequence_owner = sys_context('USERENV', 'SESSION_USER') and sequence_name not like 'ISEQ$$%'"
						);
					}
					catch (SQLException sqlException) {
						throw new RuntimeException( sqlException );
					}
				}
		);
	}

	@Override
	public void clearSchema(Connection connection, String schemaName) {
		cachedTruncateTableSqlPerSchema.remove( schemaName );
		cachedConstraintDisableSqlPerSchema.remove( schemaName );
		cachedConstraintEnableSqlPerSchema.remove( schemaName );
		clearSchema0(
				connection,
				statement -> {
					try {
						return statement.executeQuery(
								"SELECT 'DROP TABLE ' || owner || '.\"' || table_name || '\" CASCADE CONSTRAINTS' " +
										"FROM all_tables " +
										"WHERE owner = '" + schemaName + "'" +
										// Normally, user tables aren't in sysaux
										"      AND tablespace_name NOT IN ('SYSAUX')" +
										// Apparently, user tables have global stats off
										"      AND global_stats = 'NO'" +
										// Exclude the tables with names starting like 'DEF$_'
										"      AND table_name NOT LIKE 'DEF$\\_%' ESCAPE '\\'" +
										" UNION ALL " +
										"SELECT 'DROP SEQUENCE ' || sequence_owner || '.' || sequence_name FROM all_sequences WHERE sequence_owner = '" + schemaName + "'"
						);
					}
					catch (SQLException sqlException) {
						throw new RuntimeException( sqlException );
					}
				}
		);
	}

	private void clearSchema0(Connection c, Function<Statement, ResultSet> sqlProvider) {
		try (Statement s = c.createStatement()) {
			ResultSet rs;
			List<String> sqls = new ArrayList<>();

			// Collect schema objects
			LOG.log( Level.FINEST, "Collect schema objects: START" );
			rs = sqlProvider.apply( s );
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
								"SELECT tbl.owner || '.\"' || tbl.table_name || '\"', c.constraint_name FROM (" +
										"SELECT owner, table_name " +
										"FROM all_tables " +
										// Exclude the tables owner by sys
										"WHERE owner NOT IN ('SYS')" +
										// Normally, user tables aren't in sysaux
										"      AND tablespace_name NOT IN ('SYSAUX')" +
										// Apparently, user tables have global stats off
										"      AND global_stats = 'NO'" +
										// Exclude the tables with names starting like 'DEF$_'
										"      AND table_name NOT LIKE 'DEF$\\_%' ESCAPE '\\'" +
										") tbl LEFT JOIN all_constraints c ON tbl.owner = c.owner AND tbl.table_name = c.table_name AND constraint_type = 'R'"
						);
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
				schemaName, statement -> {
					try {
						return statement.executeQuery(
								"SELECT tbl.owner || '.\"' || tbl.table_name || '\"', c.constraint_name FROM (" +
										"SELECT owner, table_name " +
										"FROM all_tables " +
										"WHERE owner = '" + schemaName + "'" +
										// Normally, user tables aren't in sysaux
										"      AND tablespace_name NOT IN ('SYSAUX')" +
										// Apparently, user tables have global stats off
										"      AND global_stats = 'NO'" +
										// Exclude the tables with names starting like 'DEF$_'
										"      AND table_name NOT LIKE 'DEF$\\_%' ESCAPE '\\'" +
										") tbl LEFT JOIN all_constraints c ON tbl.owner = c.owner AND tbl.table_name = c.table_name AND constraint_type = 'R'"
						);
					}
					catch (SQLException sqlException) {
						throw new RuntimeException( sqlException );
					}
				}
		);
	}

	private void clearData0(Connection connection, String schemaName, Function<Statement, ResultSet> tablesProvider) {
		try (Statement s = connection.createStatement()) {
			List<String> cachedTruncateTableSql = cachedTruncateTableSqlPerSchema.get( schemaName );
			List<String> cachedConstraintDisableSql = cachedConstraintDisableSqlPerSchema.get( schemaName );
			List<String> cachedConstraintEnableSql = cachedConstraintEnableSqlPerSchema.get( schemaName );
			if ( cachedTruncateTableSql == null ) {
				cachedTruncateTableSql = new ArrayList<>();
				cachedConstraintDisableSql = new ArrayList<>();
				cachedConstraintEnableSql = new ArrayList<>();
				ResultSet rs = tablesProvider.apply( s );
				while ( rs.next() ) {
					String tableName = rs.getString( 1 );
					String constraintName = rs.getString( 2 );
					if ( !ignoredTables.contains( tableName ) ) {
						cachedTruncateTableSql.add( "TRUNCATE TABLE \"" + tableName + "\"" );
						if ( constraintName != null ) {
							cachedConstraintDisableSql.add( "ALTER TABLE \"" + tableName + "\" DISABLE CONSTRAINT " + constraintName );
							cachedConstraintEnableSql.add( "ALTER TABLE \"" + tableName + "\" ENABLE CONSTRAINT " + constraintName );
						}
					}
				}
				cachedTruncateTableSqlPerSchema.put( schemaName, cachedTruncateTableSql );
				cachedConstraintDisableSqlPerSchema.put( schemaName, cachedConstraintDisableSql );
				cachedConstraintEnableSqlPerSchema.put( schemaName, cachedConstraintEnableSql );
			}
			// Disable foreign keys
			LOG.log( Level.FINEST, "Disable foreign keys: START" );
			for ( String sql : cachedConstraintDisableSql ) {
				s.execute( sql );
			}
			LOG.log( Level.FINEST, "Disable foreign keys: END" );

			// Delete data
			LOG.log( Level.FINEST, "Deleting data: START" );
			for ( String sql : cachedTruncateTableSql ) {
				s.execute( sql );
			}
			LOG.log( Level.FINEST, "Deleting data: END" );

			// Enable foreign keys
			LOG.log( Level.FINEST, "Enabling foreign keys: START" );
			for ( String sql : cachedConstraintEnableSql ) {
				s.execute( sql );
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
