/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.jdbc.AbstractWork;

/**
 * @author Steve Ebersole
 */
public class TemporaryTableHelper {
	private final static CoreMessageLogger LOG = CoreLogging.messageLogger( TemporaryTableHelper.class );

	public static final String SESSION_ID_COLUMN_NAME = "hib_sess_id";

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Creation

	public static class TemporaryTableCreationWork extends AbstractReturningWork<Boolean> {
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
		public Boolean execute(Connection connection) {
			final JdbcServices jdbcServices = sessionFactory.getJdbcServices();

			try {
				final String creationCommand = exporter.getSqlCreateCommand( temporaryTable );
				logStatement( creationCommand, jdbcServices );

				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( creationCommand );
					jdbcServices.getSqlExceptionHelper().handleAndClearWarnings( statement, WARNING_HANDLER );
					return Boolean.TRUE;
				}
				catch (SQLException e) {
					LOG.debugf(
							"Unable to create temporary table [%s]; `%s` failed : %s",
							temporaryTable.getQualifiedTableName(),
							creationCommand,
							e.getMessage()
					);
				}
			}
			catch( Exception e ) {
				LOG.debugf( "Error creating temporary table(s) : %s", e.getMessage() );
			}
			return Boolean.FALSE;
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
					LOG.debugf(
							"Unable to drop temporary table [%s]; `%s` failed : %s",
							temporaryTable.getQualifiedTableName(),
							dropCommand,
							e.getMessage()
					);
				}
			}
			catch( Exception e ) {
				LOG.debugf( "Error dropping temporary table(s) : %s", e.getMessage() );
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
		PreparedStatement preparedStatement = null;
		try {
			final String sql = exporter.getSqlTruncateCommand( temporaryTable, sessionUidAccess, session );
			preparedStatement = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
			if ( temporaryTable.getSessionUidColumn() != null ) {
				final String sessionUid = sessionUidAccess.apply( session );
				preparedStatement.setString( 1, sessionUid );
			}
			jdbcCoordinator.getResultSetReturn().executeUpdate( preparedStatement, sql );
		}
		catch( Throwable t ) {
			LOG.unableToCleanupTemporaryIdTable(t);
		}
		finally {
			if ( preparedStatement != null ) {
				try {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( preparedStatement );
				}
				catch( Throwable ignore ) {
					// ignore
				}
				jdbcCoordinator.afterStatementExecution();
			}
		}
	}

	/**
	 * Differs from
	 * {@link org.hibernate.engine.jdbc.spi.SqlExceptionHelper.StandardWarningHandler}
	 * because it logs only at DEBUG level.
	 */
	private static final SqlExceptionHelper.WarningHandler WARNING_HANDLER =
			new SqlExceptionHelper.WarningHandlerLoggingSupport() {
				public boolean doProcess() {
					return LOG.isDebugEnabled();
				}

				public void prepare(SQLWarning warning) {
					LOG.warningsCreatingTempTable( warning );
				}

				@Override
				protected void logWarning(String description, String message) {
					LOG.debug( description );
					LOG.debug( message );
				}
			};


	private static void logStatement(String sql, JdbcServices jdbcServices) {
		final SqlStatementLogger statementLogger = jdbcServices.getSqlStatementLogger();
		statementLogger.logStatement( sql, FormatStyle.BASIC.getFormatter() );
	}
}
