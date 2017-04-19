/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.local;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.hibernate.boot.TempTableDdlTransactionHandling;
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
public class Helper {
	/**
	 * Singleton access
	 */
	public static final Helper INSTANCE = new Helper();

	private static final CoreMessageLogger log = CoreLogging.messageLogger( Helper.class );

	private Helper() {
	}


	public void createTempTable(
			IdTableInfoImpl idTableInfo,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			SharedSessionContractImplementor session) {
		// Don't really know all the codes required to adequately decipher returned jdbc exceptions here.
		// simply allow the failure to be eaten and the subsequent insert-selects/deletes should fail
		TemporaryTableCreationWork work = new TemporaryTableCreationWork( idTableInfo, session.getFactory() );

		if ( ddlTransactionHandling == TempTableDdlTransactionHandling.NONE ) {
			final Connection connection = session.getJdbcCoordinator()
					.getLogicalConnection()
					.getPhysicalConnection();

			work.execute( connection );

			session.getJdbcCoordinator().afterStatementExecution();
		}
		else {
			session.getTransactionCoordinator()
					.createIsolationDelegate()
					.delegateWork( work, ddlTransactionHandling == TempTableDdlTransactionHandling.ISOLATE_AND_TRANSACT );
		}
	}

	private static class TemporaryTableCreationWork extends AbstractWork {
		private final IdTableInfoImpl idTableInfo;
		private final SessionFactoryImplementor factory;

		private TemporaryTableCreationWork(IdTableInfoImpl idTableInfo, SessionFactoryImplementor factory) {
			this.idTableInfo = idTableInfo;
			this.factory = factory;
		}

		@Override
		public void execute(Connection connection) {
			try {
				Statement statement = connection.createStatement();
				try {
					statement.executeUpdate( logStatement(factory, idTableInfo.getIdTableCreationStatement()) );
					factory.getServiceRegistry()
							.getService( JdbcServices.class )
							.getSqlExceptionHelper()
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
			catch( Exception e ) {
				log.debug( "unable to create temporary id table [" + e.getMessage() + "]" );
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

	protected void releaseTempTable(
			IdTableInfoImpl idTableInfo,
			AfterUseAction afterUseAction,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			SharedSessionContractImplementor session) {
		if ( afterUseAction == AfterUseAction.NONE ) {
			return;
		}

		if ( afterUseAction == AfterUseAction.DROP ) {
			TemporaryTableDropWork work = new TemporaryTableDropWork( idTableInfo, session.getFactory() );
			if ( ddlTransactionHandling == TempTableDdlTransactionHandling.NONE ) {
				final Connection connection = session.getJdbcCoordinator()
						.getLogicalConnection()
						.getPhysicalConnection();

				work.execute( connection );

				session.getJdbcCoordinator().afterStatementExecution();
			}
			else {
				session.getTransactionCoordinator()
						.createIsolationDelegate()
						.delegateWork( work, ddlTransactionHandling == TempTableDdlTransactionHandling.ISOLATE_AND_TRANSACT );
			}
		}

		if ( afterUseAction == AfterUseAction.CLEAN ) {
			PreparedStatement ps = null;
			try {
				final String sql = "delete from " + idTableInfo.getQualifiedIdTableName();
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
	}

	private static class TemporaryTableDropWork extends AbstractWork {
		private final IdTableInfoImpl idTableInfo;
		private final SessionFactoryImplementor factory;

		private TemporaryTableDropWork(IdTableInfoImpl idTableInfo, SessionFactoryImplementor factory) {
			this.idTableInfo = idTableInfo;
			this.factory = factory;
		}

		@Override
		public void execute(Connection connection) {
			try {
				Statement statement = connection.createStatement();
				try {
					statement.executeUpdate( logStatement( factory, idTableInfo.getIdTableDropStatement() ) );
					factory.getServiceRegistry()
							.getService( JdbcServices.class )
							.getSqlExceptionHelper()
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
			catch( Exception e ) {
				log.warn( "unable to drop temporary id table after use [" + e.getMessage() + "]" );
			}
		}
	}

	private static String logStatement(SessionFactoryImplementor factory, String sql) {
		final SqlStatementLogger statementLogger = factory.getServiceRegistry()
				.getService( JdbcServices.class )
				.getSqlStatementLogger();

		statementLogger.logStatement( sql, FormatStyle.BASIC.getFormatter() );
		return sql;
	}
}
