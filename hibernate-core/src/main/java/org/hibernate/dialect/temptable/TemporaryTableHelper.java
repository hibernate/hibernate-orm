/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.temptable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.function.Function;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jdbc.AbstractWork;

/**
 * @author Steve Ebersole
 */
public class TemporaryTableHelper {
	private final static CoreMessageLogger log = CoreLogging.messageLogger( TemporaryTableHelper.class );

	public static final String SESSION_ID_COLUMN_NAME = "hib_sess_id";

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Creation

	public static class TemporaryTableCreationWork extends AbstractWork {
		private final TemporaryTable temporaryTable;
		private final TemporaryTableExporter exporter;
		private final SessionFactoryImplementor sessionFactory;

		public TemporaryTableCreationWork(
				TemporaryTable temporaryTable,
				SessionFactoryImplementor sessionFactory) {
			this(
					temporaryTable,
					sessionFactory.getJdbcServices().getDialect().getTemporaryTableExporter(),
					sessionFactory
			);
		}

		public TemporaryTableCreationWork(
				TemporaryTable temporaryTable,
				TemporaryTableExporter exporter,
				SessionFactoryImplementor sessionFactory) {
			this.temporaryTable = temporaryTable;
			this.exporter = exporter;
			this.sessionFactory = sessionFactory;
		}

		@Override
		public void execute(Connection connection) {
			final JdbcServices jdbcServices = sessionFactory.getJdbcServices();

			try {
				final String creationCommand = exporter.getSqlCreateCommand( temporaryTable );
				logStatement( creationCommand, jdbcServices );

				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( creationCommand );
					jdbcServices.getSqlExceptionHelper().handleAndClearWarnings( statement, WARNING_HANDLER );
				}
				catch (SQLException e) {
					log.debugf(
							"unable to create temporary table [%s]; `%s` failed : %s",
							temporaryTable.getQualifiedTableName(),
							creationCommand,
							e.getMessage()
					);
				}
			}
			catch( Exception e ) {
				log.debugf( "Error creating temporary table(s) : %s", e.getMessage() );
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Drop

	public static class TemporaryTableDropWork extends AbstractWork {
		private final TemporaryTable temporaryTable;
		private final TemporaryTableExporter exporter;
		private final SessionFactoryImplementor sessionFactory;

		public TemporaryTableDropWork(
				TemporaryTable temporaryTable,
				SessionFactoryImplementor sessionFactory) {
			this(
					temporaryTable,
					sessionFactory.getJdbcServices().getDialect().getTemporaryTableExporter(),
					sessionFactory
			);
		}

		public TemporaryTableDropWork(
				TemporaryTable temporaryTable,
				TemporaryTableExporter exporter,
				SessionFactoryImplementor sessionFactory) {
			this.temporaryTable = temporaryTable;
			this.exporter = exporter;
			this.sessionFactory = sessionFactory;
		}

		@Override
		public void execute(Connection connection) {
			final JdbcServices jdbcServices = sessionFactory.getJdbcServices();

			try {
				final String dropCommand = exporter.getSqlDropCommand( temporaryTable );
				logStatement( dropCommand, jdbcServices );

				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( dropCommand );
					jdbcServices.getSqlExceptionHelper().handleAndClearWarnings( statement, WARNING_HANDLER );
				}
				catch (SQLException e) {
					log.debugf(
							"unable to drop temporary table [%s]; `%s` failed : %s",
							temporaryTable.getQualifiedTableName(),
							dropCommand,
							e.getMessage()
					);
				}
			}
			catch( Exception e ) {
				log.debugf( "Error dropping temporary table(s) : %s", e.getMessage() );
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Clean

	public static void cleanTemporaryTableRows(
			TemporaryTable temporaryTable,
			TemporaryTableExporter exporter,
			Function<SharedSessionContractImplementor,String> sessionUidAccess,
			SharedSessionContractImplementor session) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		PreparedStatement ps = null;
		try {
			final String sql = exporter.getSqlTruncateCommand( temporaryTable, sessionUidAccess, session );

			ps = jdbcCoordinator.getStatementPreparer().prepareStatement( sql, false );

			if ( temporaryTable.getSessionUidColumn() != null ) {
				final String sessionUid = sessionUidAccess.apply( session );
				ps.setString( 1, sessionUid );
			}

			jdbcCoordinator.getResultSetReturn().executeUpdate( ps, sql );
		}
		catch( Throwable t ) {
			log.unableToCleanupTemporaryIdTable(t);
		}
		finally {
			if ( ps != null ) {
				try {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( ps );
				}
				catch( Throwable ignore ) {
					// ignore
				}
				jdbcCoordinator.afterStatementExecution();
			}
		}
	}


	private static SqlExceptionHelper.WarningHandler WARNING_HANDLER = new SqlExceptionHelper.WarningHandlerLoggingSupport() {
		public boolean doProcess() {
			return log.isDebugEnabled();
		}

		public void prepare(SQLWarning warning) {
			log.warningsCreatingTempTable( warning );
		}

		@Override
		protected void logWarning(String description, String message) {
			log.debug( description );
			log.debug( message );
		}
	};


	private static void logStatement(String sql, JdbcServices jdbcServices) {
		final SqlStatementLogger statementLogger = jdbcServices.getSqlStatementLogger();
		statementLogger.logStatement( sql, FormatStyle.BASIC.getFormatter() );
	}
}
