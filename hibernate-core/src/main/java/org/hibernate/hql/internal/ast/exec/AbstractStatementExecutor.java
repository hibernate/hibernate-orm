/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.hql.internal.ast.exec;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import antlr.RecognitionException;
import antlr.collections.AST;
import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.SqlGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jdbc.AbstractWork;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.InsertSelect;
import org.hibernate.sql.Select;
import org.hibernate.sql.SelectFragment;

/**
 * Implementation of AbstractStatementExecutor.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractStatementExecutor implements StatementExecutor {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       AbstractStatementExecutor.class.getName());

	private final HqlSqlWalker walker;
	private List idSelectParameterSpecifications = Collections.EMPTY_LIST;

    public AbstractStatementExecutor( HqlSqlWalker walker,
                                      CoreMessageLogger log ) {
		this.walker = walker;
	}

	protected HqlSqlWalker getWalker() {
		return walker;
	}

	protected SessionFactoryImplementor getFactory() {
		return walker.getSessionFactoryHelper().getFactory();
	}

	protected List getIdSelectParameterSpecifications() {
		return idSelectParameterSpecifications;
	}

	protected abstract Queryable[] getAffectedQueryables();

	protected String generateIdInsertSelect(Queryable persister, String tableAlias, AST whereClause) {
		Select select = new Select( getFactory().getDialect() );
		SelectFragment selectFragment = new SelectFragment()
				.addColumns( tableAlias, persister.getIdentifierColumnNames(), persister.getIdentifierColumnNames() );
		select.setSelectClause( selectFragment.toFragmentString().substring( 2 ) );

		String rootTableName = persister.getTableName();
		String fromJoinFragment = persister.fromJoinFragment( tableAlias, true, false );
		String whereJoinFragment = persister.whereJoinFragment( tableAlias, true, false );

		select.setFromClause( rootTableName + ' ' + tableAlias + fromJoinFragment );

		if ( whereJoinFragment == null ) {
			whereJoinFragment = "";
		}
		else {
			whereJoinFragment = whereJoinFragment.trim();
			if ( whereJoinFragment.startsWith( "and" ) ) {
				whereJoinFragment = whereJoinFragment.substring( 4 );
			}
		}

		String userWhereClause = "";
		if ( whereClause.getNumberOfChildren() != 0 ) {
			// If a where clause was specified in the update/delete query, use it to limit the
			// returned ids here...
			try {
				SqlGenerator sqlGenerator = new SqlGenerator( getFactory() );
				sqlGenerator.whereClause( whereClause );
				userWhereClause = sqlGenerator.getSQL().substring( 7 );  // strip the " where "
				idSelectParameterSpecifications = sqlGenerator.getCollectedParameters();
			}
			catch ( RecognitionException e ) {
				throw new HibernateException( "Unable to generate id select for DML operation", e );
			}
			if ( whereJoinFragment.length() > 0 ) {
				whereJoinFragment += " and ";
			}
		}

		select.setWhereClause( whereJoinFragment + userWhereClause );

		InsertSelect insert = new InsertSelect( getFactory().getDialect() );
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			insert.setComment( "insert-select for " + persister.getEntityName() + " ids" );
		}
		insert.setTableName( persister.getTemporaryIdTableName() );
		insert.setSelect( select );
		return insert.toStatementString();
	}

	protected String generateIdSubselect(Queryable persister) {
		return "select " + StringHelper.join( ", ", persister.getIdentifierColumnNames() ) +
			        " from " + persister.getTemporaryIdTableName();
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
				LOG.debug( "unable to create temporary id table [" + e.getMessage() + "]" );
			}
		}
	}
	protected void createTemporaryTableIfNecessary(final Queryable persister, final SessionImplementor session) {
		// Don't really know all the codes required to adequately decipher returned jdbc exceptions here.
		// simply allow the failure to be eaten and the subsequent insert-selects/deletes should fail
		TemporaryTableCreationWork work = new TemporaryTableCreationWork( persister );
		if ( shouldIsolateTemporaryTableDDL() ) {
			session.getTransactionCoordinator()
					.getTransaction()
					.createIsolationDelegate()
					.delegateWork( work, getFactory().getSettings().isDataDefinitionInTransactionSupported() );
		}
		else {
			final Connection connection = session.getTransactionCoordinator()
					.getJdbcCoordinator()
					.getLogicalConnection()
					.getShareableConnectionProxy();
			work.execute( connection );
			session.getTransactionCoordinator()
					.getJdbcCoordinator()
					.getLogicalConnection()
					.afterStatementExecution();
		}
	}

	private static SqlExceptionHelper.WarningHandler CREATION_WARNING_HANDLER = new SqlExceptionHelper.WarningHandlerLoggingSupport() {
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
					statement = connection.createStatement();
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
				LOG.warn( "unable to drop temporary id table after use [" + e.getMessage() + "]" );
			}
		}
	}

	protected void dropTemporaryTableIfNecessary(final Queryable persister, final SessionImplementor session) {
		if ( getFactory().getDialect().dropTemporaryTableAfterUse() ) {
			TemporaryTableDropWork work = new TemporaryTableDropWork( persister, session );
			if ( shouldIsolateTemporaryTableDDL() ) {
				session.getTransactionCoordinator()
						.getTransaction()
						.createIsolationDelegate()
						.delegateWork( work, getFactory().getSettings().isDataDefinitionInTransactionSupported() );
			}
			else {
				final Connection connection = session.getTransactionCoordinator()
						.getJdbcCoordinator()
						.getLogicalConnection()
						.getShareableConnectionProxy();
				work.execute( connection );
				session.getTransactionCoordinator()
						.getJdbcCoordinator()
						.getLogicalConnection()
						.afterStatementExecution();
			}
		}
		else {
			// at the very least cleanup the data :)
			PreparedStatement ps = null;
			try {
				final String sql = "delete from " + persister.getTemporaryIdTableName();
				ps = session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );
				ps.executeUpdate();
			}
			catch( Throwable t ) {
                LOG.unableToCleanupTemporaryIdTable(t);
			}
			finally {
				if ( ps != null ) {
					try {
						ps.close();
					}
					catch( Throwable ignore ) {
						// ignore
					}
				}
			}
		}
	}

	protected void coordinateSharedCacheCleanup(SessionImplementor session) {
		BulkOperationCleanupAction action = new BulkOperationCleanupAction( session, getAffectedQueryables() );

		if ( session.isEventSource() ) {
			( ( EventSource ) session ).getActionQueue().addAction( action );
		}
		else {
			action.getAfterTransactionCompletionProcess().doAfterTransactionCompletion( true, session );
		}
	}

	@SuppressWarnings({ "UnnecessaryUnboxing" })
	protected boolean shouldIsolateTemporaryTableDDL() {
		Boolean dialectVote = getFactory().getDialect().performTemporaryTableDDLInIsolation();
        if (dialectVote != null) return dialectVote.booleanValue();
        return getFactory().getSettings().isDataDefinitionImplicitCommit();
	}
}
