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
package org.hibernate.hql.ast.exec;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.List;
import java.util.Collections;

import org.hibernate.HibernateException;
import org.hibernate.action.BulkOperationCleanupAction;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.transaction.Isolater;
import org.hibernate.engine.transaction.IsolatedWork;
import org.hibernate.event.EventSource;
import org.hibernate.hql.ast.HqlSqlWalker;
import org.hibernate.hql.ast.SqlGenerator;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.InsertSelect;
import org.hibernate.sql.Select;
import org.hibernate.sql.SelectFragment;
import org.hibernate.util.JDBCExceptionReporter;
import org.hibernate.util.StringHelper;

import antlr.RecognitionException;
import antlr.collections.AST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of AbstractStatementExecutor.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractStatementExecutor implements StatementExecutor {
	private static final Logger LOG = LoggerFactory.getLogger( AbstractStatementExecutor.class );

	private final Logger log;
	private final HqlSqlWalker walker;
	private List idSelectParameterSpecifications = Collections.EMPTY_LIST;

	public AbstractStatementExecutor(HqlSqlWalker walker, Logger log) {
		this.walker = walker;
		this.log = log;
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

	protected void createTemporaryTableIfNecessary(final Queryable persister, final SessionImplementor session) {
		// Don't really know all the codes required to adequately decipher returned jdbc exceptions here.
		// simply allow the failure to be eaten and the subsequent insert-selects/deletes should fail
		IsolatedWork work = new IsolatedWork() {
			public void doWork(Connection connection) throws HibernateException {
				try {
					Statement statement = connection.createStatement();
					try {
						statement.executeUpdate( persister.getTemporaryIdTableDDL() );
						JDBCExceptionReporter.handleAndClearWarnings( statement, CREATION_WARNING_HANDLER );
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
		};
		if ( shouldIsolateTemporaryTableDDL() ) {
			if ( getFactory().getSettings().isDataDefinitionInTransactionSupported() ) {
				Isolater.doIsolatedWork( work, session );
			}
			else {
				Isolater.doNonTransactedWork( work, session );
			}
		}
		else {
			work.doWork( session.getJDBCContext().getConnectionManager().getConnection() );
			session.getJDBCContext().getConnectionManager().afterStatement();
		}
	}

	private static JDBCExceptionReporter.WarningHandler CREATION_WARNING_HANDLER = new JDBCExceptionReporter.WarningHandlerLoggingSupport() {
		public boolean doProcess() {
			return LOG.isDebugEnabled();
		}

		public void prepare(SQLWarning warning) {
			LOG.debug( "Warnings creating temp table", warning );
		}

		@Override
		protected void logWarning(String description, String message) {
			LOG.debug( description );
			LOG.debug( message );
		}
	};

	protected void dropTemporaryTableIfNecessary(final Queryable persister, final SessionImplementor session) {
		if ( getFactory().getDialect().dropTemporaryTableAfterUse() ) {
			IsolatedWork work = new IsolatedWork() {
				public void doWork(Connection connection) throws HibernateException {
					final String command = session.getFactory().getSettings().getDialect().getDropTemporaryTableString()
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
						log.warn( "unable to drop temporary id table after use [" + e.getMessage() + "]" );
					}
				}
			};

			if ( shouldIsolateTemporaryTableDDL() ) {
				if ( getFactory().getSettings().isDataDefinitionInTransactionSupported() ) {
					Isolater.doIsolatedWork( work, session );
				}
				else {
					Isolater.doNonTransactedWork( work, session );
				}
			}
			else {
				work.doWork( session.getJDBCContext().getConnectionManager().getConnection() );
				session.getJDBCContext().getConnectionManager().afterStatement();
			}
		}
		else {
			// at the very least cleanup the data :)
			PreparedStatement ps = null;
			try {
				ps = session.getBatcher().prepareStatement( "delete from " + persister.getTemporaryIdTableName() );
				ps.executeUpdate();
			}
			catch( Throwable t ) {
				log.warn( "unable to cleanup temporary id table after use [" + t + "]" );
			}
			finally {
				if ( ps != null ) {
					try {
						session.getBatcher().closeStatement( ps );
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
		if ( dialectVote != null ) {
			return dialectVote.booleanValue();
		}
		else {
			return getFactory().getSettings().isDataDefinitionImplicitCommit();
		}
	}
}
