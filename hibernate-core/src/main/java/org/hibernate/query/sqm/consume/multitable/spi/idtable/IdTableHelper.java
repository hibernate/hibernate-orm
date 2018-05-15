/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jdbc.AbstractWork;

/**
 * @author Steve Ebersole
 */
public class IdTableHelper {
	private final static CoreMessageLogger log = CoreLogging.messageLogger(IdTableHelper.class );

	private final IdTable idTableInfo;
	private final IdTableSupport idTableSupport;
	private final IdTableManagementTransactionality transactionality;
	private final JdbcServices jdbcServices;

	private final String[] creationCommands;
	private final String[] dropCommands;

	public IdTableHelper(
			IdTable idTableInfo,
			IdTableSupport idTableSupport,
			IdTableManagementTransactionality transactionality,
			JdbcServices jdbcServices) {
		this.idTableInfo = idTableInfo;
		this.idTableSupport = idTableSupport;
		if ( idTableSupport.geIdTableManagementTransactionality() != null ) {
			this.transactionality = idTableSupport.geIdTableManagementTransactionality();
		}
		else {
			this.transactionality = transactionality;
		}
		this.jdbcServices = jdbcServices;

		this.creationCommands = generateIdTableCreationCommands( idTableInfo, idTableSupport, jdbcServices );
		this.dropCommands = generateIdTableDropCommands( idTableInfo, idTableSupport, jdbcServices );
	}

	private static String[] generateIdTableCreationCommands(
			IdTable idTableInfo,
			IdTableSupport idTableSupport,
			JdbcServices jdbcServices) {
		return idTableSupport.getIdTableExporter().getSqlCreateStrings( idTableInfo, jdbcServices );
	}

	private static String[] generateIdTableDropCommands(
			IdTable idTableInfo,
			IdTableSupport idTableSupport,
			JdbcServices jdbcServices) {
		return idTableSupport.getIdTableExporter().getSqlDropStrings( idTableInfo, jdbcServices );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Creation

	private class IdTableCreationWork extends AbstractWork {
		@Override
		public void execute(Connection connection) {
			try {
				Statement statement = connection.createStatement();
				for ( String creationCommand : creationCommands ) {
					try {
						logStatement( creationCommand );
						statement.executeUpdate( creationCommand );
						jdbcServices.getSqlExceptionHelper()
								.handleAndClearWarnings( statement, WARNING_HANDLER );
					}
					catch( SQLException e ) {
						log.debugf(
								"unable to create id table [%s]; `%s` failed : %s",
								idTableInfo.getQualifiedTableName().render(),
								creationCommand,
								e.getMessage()
						);
					}
					finally {
						try {
							statement.close();
						}
						catch( Throwable ignore ) {
							// ignore
						}
					}

				}
			}
			catch( Exception e ) {
				log.debugf( "Error creating id table(s) : %s", e.getMessage() );
			}
		}

	}

	public void createIdTable(SharedSessionContractImplementor session) {
		executeWork( new IdTableCreationWork(), session );
	}

	private void executeWork(AbstractWork work, SharedSessionContractImplementor session) {
		if ( transactionality == IdTableManagementTransactionality.NONE ) {
			// simply execute the work using a Connection obtained from JdbcConnectionAccess
			//
			// NOTE : we do not (potentially) release the Connection here
			//		via LogicalConnectionImplementor#afterStatement because
			// 		for sure we will be immediately using it again to
			//		populate the id table and use it...

			try {
				work.execute( session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection() );
			}
			catch (SQLException e) {
				log.error( "Unable to use JDBC Connection to create perform id table management", e );
			}
		}
		else {
			session.getTransactionCoordinator()
					.createIsolationDelegate()
					.delegateWork( work, transactionality == IdTableManagementTransactionality.ISOLATE_AND_TRANSACT );
		}

	}

	public void createIdTable(JdbcConnectionAccess jdbcConnectionAccess) {
		// Don't really know all the codes required to adequately decipher returned jdbc exceptions here.
		// simply allow the failure to be eaten and the subsequent insert-selects/deletes should fail
		final IdTableCreationWork work = new IdTableCreationWork();

		if ( transactionality != IdTableManagementTransactionality.NONE ) {
			log.debugf( "IdTableManagementTransactionality#%s was requested but not supported, skipping transactionality" );
		}

		// simply execute the work using a Connection obtained from JdbcConnectionAccess
		try {
			final Connection connection = jdbcConnectionAccess.obtainConnection();

			try {
				work.execute( connection );
			}
			finally {
				try {
					jdbcConnectionAccess.releaseConnection( connection );
				}
				catch (SQLException ignore) {
				}
			}
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper()
					.convert( e, "Unable to obtain JDBC Connection for id-table [" + idTableInfo.getQualifiedTableName().render() + "]" );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Drop


	public void dropIdTable(SharedSessionContractImplementor session) {
		final IdTableDropWork work = new IdTableDropWork();
		if ( transactionality == IdTableManagementTransactionality.NONE ) {
			final Connection connection = session.getJdbcCoordinator()
					.getLogicalConnection()
					.getPhysicalConnection();

			work.execute( connection );

			session.getJdbcCoordinator().afterStatementExecution();
		}
		else {
			session.getTransactionCoordinator()
					.createIsolationDelegate()
					.delegateWork( work, transactionality == IdTableManagementTransactionality.ISOLATE_AND_TRANSACT );
		}
	}


	public void dropIdTable(JdbcConnectionAccess jdbcConnectionAccess) {
		if ( transactionality != IdTableManagementTransactionality.NONE ) {
			log.debugf( "IdTableManagementTransactionality#%s was requested but not supported, skipping transactionality" );
		}

		final IdTableDropWork work = new IdTableDropWork();

		// simply execute the work using a Connection obtained from JdbcConnectionAccess
		try {
			final Connection connection = jdbcConnectionAccess.obtainConnection();

			try {
				work.execute( connection );
			}
			finally {
				try {
					jdbcConnectionAccess.releaseConnection( connection );
				}
				catch (SQLException ignore) {
				}
			}
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper()
					.convert( e, "Unable to obtain JDBC Connection for id-table [" + idTableInfo.getQualifiedTableName().render() + "]" );
		}
	}

	private class IdTableDropWork extends AbstractWork {
		@Override
		public void execute(Connection connection) {
			try {

				final Statement statement = connection.createStatement();

				for ( String dropCommand : dropCommands ) {
					try {
						logStatement( dropCommand );
						statement.executeUpdate( dropCommand );
						jdbcServices.getSqlExceptionHelper()
								.handleAndClearWarnings( statement, WARNING_HANDLER );
					}
					finally {
						try {
							statement.close();
						}
						catch( Throwable ignore ) {
							// ignore
						}
					}
				}
			}
			catch( Exception e ) {
				log.warn( "unable to drop temporary id table afterQuery use [" + e.getMessage() + "]" );
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Clean

	public void cleanIdTableRows(SharedSessionContractImplementor session) {
		final JdbcServices jdbcServices = session.getJdbcServices();

		// todo (6.0) : need to account for session uid if support is enabled.

		PreparedStatement ps = null;
		try {
			final String tableName = jdbcServices.getJdbcEnvironment()
					.getQualifiedObjectNameFormatter()
					.format( idTableInfo.getQualifiedTableName(), jdbcServices.getJdbcEnvironment().getDialect() );

			final String sql = ((IdTableExporterImpl)this.idTableSupport.getIdTableExporter()).getTruncateIdTableCommand() + " " + tableName;
			ps = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Misc

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


	private void logStatement(String sql) {
		final SqlStatementLogger statementLogger = jdbcServices.getSqlStatementLogger();
		statementLogger.logStatement( sql, FormatStyle.BASIC.getFormatter() );
	}
}
