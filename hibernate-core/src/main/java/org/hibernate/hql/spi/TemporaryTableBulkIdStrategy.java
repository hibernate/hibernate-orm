/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.hql.spi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Map;

import org.hibernate.cfg.Mappings;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jdbc.AbstractWork;
import org.hibernate.persister.entity.Queryable;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class TemporaryTableBulkIdStrategy implements MultiTableBulkIdStrategy {
	public static final TemporaryTableBulkIdStrategy INSTANCE = new TemporaryTableBulkIdStrategy();

	public static final String SHORT_NAME = "temporary";

	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			TemporaryTableBulkIdStrategy.class.getName()
	);

	@Override
	public void prepare(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess, Mappings mappings, Mapping mapping, Map settings) {
		// nothing to do
	}

	@Override
	public void release(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess) {
		// nothing to do
	}

	@Override
	public UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		return new TableBasedUpdateHandlerImpl( factory, walker ) {
			@Override
			protected void prepareForUse(Queryable persister, SessionImplementor session) {
				createTempTable( persister, session );
			}

			@Override
			protected void releaseFromUse(Queryable persister, SessionImplementor session) {
				releaseTempTable( persister, session );
			}
		};
	}

	@Override
	public DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		return new TableBasedDeleteHandlerImpl( factory, walker ) {
			@Override
			protected void prepareForUse(Queryable persister, SessionImplementor session) {
				createTempTable( persister, session );
			}

			@Override
			protected void releaseFromUse(Queryable persister, SessionImplementor session) {
				releaseTempTable( persister, session );
			}
		};
	}


	protected void createTempTable(Queryable persister, SessionImplementor session) {
		// Don't really know all the codes required to adequately decipher returned jdbc exceptions here.
		// simply allow the failure to be eaten and the subsequent insert-selects/deletes should fail
		TemporaryTableCreationWork work = new TemporaryTableCreationWork( persister );
		if ( shouldIsolateTemporaryTableDDL( session ) ) {
			session.getTransactionCoordinator()
					.getTransaction()
					.createIsolationDelegate()
					.delegateWork( work, shouldTransactIsolatedTemporaryTableDDL( session ) );
		}
		else {
			final Connection connection = session.getTransactionCoordinator()
					.getJdbcCoordinator()
					.getLogicalConnection()
					.getConnection();
			work.execute( connection );
			session.getTransactionCoordinator()
					.getJdbcCoordinator()
					.afterStatementExecution();
		}
	}

	protected void releaseTempTable(Queryable persister, SessionImplementor session) {
		if ( session.getFactory().getDialect().dropTemporaryTableAfterUse() ) {
			TemporaryTableDropWork work = new TemporaryTableDropWork( persister, session );
			if ( shouldIsolateTemporaryTableDDL( session ) ) {
				session.getTransactionCoordinator()
						.getTransaction()
						.createIsolationDelegate()
						.delegateWork( work, shouldTransactIsolatedTemporaryTableDDL( session ) );
			}
			else {
				final Connection connection = session.getTransactionCoordinator()
						.getJdbcCoordinator()
						.getLogicalConnection()
						.getConnection();
				work.execute( connection );
				session.getTransactionCoordinator()
						.getJdbcCoordinator()
						.afterStatementExecution();
			}
		}
		else {
			// at the very least cleanup the data :)
			PreparedStatement ps = null;
			try {
				final String sql = "delete from " + persister.getTemporaryIdTableName();
				ps = session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );
				session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
			}
			catch( Throwable t ) {
				log.unableToCleanupTemporaryIdTable(t);
			}
			finally {
				if ( ps != null ) {
					try {
						session.getTransactionCoordinator().getJdbcCoordinator().release( ps );
					}
					catch( Throwable ignore ) {
						// ignore
					}
				}
			}
		}
	}

	protected boolean shouldIsolateTemporaryTableDDL(SessionImplementor session) {
		Boolean dialectVote = session.getFactory().getDialect().performTemporaryTableDDLInIsolation();
		if ( dialectVote != null ) {
			return dialectVote;
		}
		return session.getFactory().getSettings().isDataDefinitionImplicitCommit();
	}

	protected boolean shouldTransactIsolatedTemporaryTableDDL(SessionImplementor session) {
		// is there ever a time when it makes sense to do this?
//		return session.getFactory().getSettings().isDataDefinitionInTransactionSupported();
		return false;
	}

	private static class TemporaryTableCreationWork extends AbstractWork {
		private final Queryable persister;

		private TemporaryTableCreationWork(Queryable persister) {
			this.persister = persister;
		}

		@Override
		public void execute(Connection connection) {
			try {
				Statement statement = connection.createStatement();
				try {
					statement.executeUpdate( persister.getTemporaryIdTableDDL() );
					persister.getFactory()
							.getServiceRegistry()
							.getService( JdbcServices.class )
							.getSqlExceptionHelper()
							.handleAndClearWarnings( statement, CREATION_WARNING_HANDLER );
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

	private static SqlExceptionHelper.WarningHandler CREATION_WARNING_HANDLER = new SqlExceptionHelper.WarningHandlerLoggingSupport() {
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

	private static class TemporaryTableDropWork extends AbstractWork {
		private final Queryable persister;
		private final SessionImplementor session;

		private TemporaryTableDropWork(Queryable persister, SessionImplementor session) {
			this.persister = persister;
			this.session = session;
		}

		@Override
		public void execute(Connection connection) {
			final String command = session.getFactory().getDialect().getDropTemporaryTableString()
					+ ' ' + persister.getTemporaryIdTableName();
			try {
				Statement statement = connection.createStatement();
				try {
					statement.executeUpdate( command );
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

}
