/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.function.Function;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.jdbc.AbstractWork;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * @author Steve Ebersole
 */
public class TemporaryTableHelper {

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
			final var jdbcServices = sessionFactory.getJdbcServices();
			try {
				final String creationCommand = exporter.getSqlCreateCommand( temporaryTable );
				logStatement( creationCommand, jdbcServices );
				try ( var statement = connection.createStatement() ) {
					statement.executeUpdate( creationCommand );
					jdbcServices.getSqlExceptionHelper().handleAndClearWarnings( statement, WARNING_HANDLER );
					return Boolean.TRUE;
				}
				catch (SQLException e) {
					CORE_LOGGER.unableToCreateTempTable(
							temporaryTable.getQualifiedTableName(),
							creationCommand,
							e
					);
				}
			}
			catch( Exception e ) {
				CORE_LOGGER.errorCreatingTempTable( e );
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
			final var jdbcServices = sessionFactory.getJdbcServices();
			try {
				final String dropCommand = exporter.getSqlDropCommand( temporaryTable );
				logStatement( dropCommand, jdbcServices );
				try ( var statement = connection.createStatement() ) {
					statement.executeUpdate( dropCommand );
					jdbcServices.getSqlExceptionHelper()
							.handleAndClearWarnings( statement, WARNING_HANDLER );
				}
				catch (SQLException e) {
					CORE_LOGGER.unableToDropTempTable(
							temporaryTable.getQualifiedTableName(),
							dropCommand,
							e
					);
				}
			}
			catch( Exception e ) {
				CORE_LOGGER.errorDroppingTempTable( e );
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
		final var jdbcCoordinator = session.getJdbcCoordinator();
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
			CORE_LOGGER.unableToCleanupTemporaryIdTable(t);
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
					return CORE_LOGGER.isDebugEnabled();
				}

				public void prepare(SQLWarning warning) {
					CORE_LOGGER.warningsCreatingTempTable( warning );
				}

				@Override
				protected void logWarning(String description, String message) {
					CORE_LOGGER.debug( description );
					CORE_LOGGER.debug( message );
				}
			};


	private static void logStatement(String sql, JdbcServices jdbcServices) {
		jdbcServices.getSqlStatementLogger()
				.logStatement( sql, FormatStyle.BASIC.getFormatter() );
	}
}
