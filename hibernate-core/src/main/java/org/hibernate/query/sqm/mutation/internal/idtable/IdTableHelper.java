/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.function.Function;

import org.hibernate.engine.jdbc.internal.FormatStyle;
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
@SuppressWarnings("WeakerAccess")
public class IdTableHelper {
	private final static CoreMessageLogger log = CoreLogging.messageLogger( IdTableHelper.class );

	public static final String SESSION_ID_COLUMN_NAME = "hib_sess_id";

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Creation

	public static class IdTableCreationWork extends AbstractWork {
		private final IdTable idTable;
		private final IdTableExporter exporter;
		private final SessionFactoryImplementor sessionFactory;

		public IdTableCreationWork(
				IdTable idTable,
				IdTableExporter exporter,
				SessionFactoryImplementor sessionFactory) {
			this.idTable = idTable;
			this.exporter = exporter;
			this.sessionFactory = sessionFactory;
		}

		@Override
		public void execute(Connection connection) {
			final JdbcServices jdbcServices = sessionFactory.getJdbcServices();

			try {
				final String creationCommand = exporter.getSqlCreateCommand( idTable );
				logStatement( creationCommand, jdbcServices );

				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( creationCommand );
					jdbcServices.getSqlExceptionHelper().handleAndClearWarnings( statement, WARNING_HANDLER );
				}
				catch (SQLException e) {
					log.debugf(
							"unable to create id table [%s]; `%s` failed : %s",
							idTable.getQualifiedTableName(),
							creationCommand,
							e.getMessage()
					);
				}
			}
			catch( Exception e ) {
				log.debugf( "Error creating id table(s) : %s", e.getMessage() );
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Drop

	public static class IdTableDropWork extends AbstractWork {
		private final IdTable idTable;
		private final IdTableExporter exporter;
		private final SessionFactoryImplementor sessionFactory;

		IdTableDropWork(
				IdTable idTable,
				IdTableExporter exporter,
				SessionFactoryImplementor sessionFactory) {
			this.idTable = idTable;
			this.exporter = exporter;
			this.sessionFactory = sessionFactory;
		}

		@Override
		public void execute(Connection connection) {
			final JdbcServices jdbcServices = sessionFactory.getJdbcServices();

			try {
				final String dropCommand = exporter.getSqlDropCommand( idTable );
				logStatement( dropCommand, jdbcServices );

				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( dropCommand );
					jdbcServices.getSqlExceptionHelper().handleAndClearWarnings( statement, WARNING_HANDLER );
				}
				catch (SQLException e) {
					log.debugf(
							"unable to drop id table [%s]; `%s` failed : %s",
							idTable.getQualifiedTableName(),
							dropCommand,
							e.getMessage()
					);
				}
			}
			catch( Exception e ) {
				log.debugf( "Error dropping id table(s) : %s", e.getMessage() );
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Clean

	public static void cleanIdTableRows(
			IdTable idTable,
			IdTableExporter exporter,
			Function<SharedSessionContractImplementor,String> sessionUidAccess,
			SharedSessionContractImplementor session) {
		PreparedStatement ps = null;
		try {
			final String sql = exporter.getSqlTruncateCommand( idTable, sessionUidAccess, session );

			ps = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );

			if ( idTable.getSessionUidColumn() != null ) {
				final String sessionUid = sessionUidAccess.apply( session );
				ps.setString( 1, sessionUid );
			}

			session.getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
		}
		catch( Throwable t ) {
			log.unableToCleanupTemporaryIdTable(t);
		}
		finally {
			if ( ps != null ) {
				try {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( ps );
				}
				catch( Throwable ignore ) {
					// ignore
				}
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
