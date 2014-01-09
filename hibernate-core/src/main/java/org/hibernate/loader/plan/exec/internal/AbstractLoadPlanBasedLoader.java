/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.loader.plan.exec.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.dialect.pagination.NoopLimitHandler;
import org.hibernate.engine.jdbc.ColumnNameCache;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

/**
 * A superclass for loader implementations based on using LoadPlans.
 *
 * @see org.hibernate.loader.entity.plan.EntityLoader
 * @see org.hibernate.loader.collection.plan.CollectionLoader

 * @author Gail Badner
 */
public abstract class AbstractLoadPlanBasedLoader {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( AbstractLoadPlanBasedLoader.class );

	private final SessionFactoryImplementor factory;

	private ColumnNameCache columnNameCache;

	/**
	 * Constructs a {@link AbstractLoadPlanBasedLoader}.
	 *
	 * @param factory The session factory
	 * @see SessionFactoryImplementor
	 */
	public AbstractLoadPlanBasedLoader(
			SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	protected SessionFactoryImplementor getFactory() {
		return factory;
	}

	protected abstract LoadQueryDetails getStaticLoadQuery();

	protected abstract int[] getNamedParameterLocs(String name);

	protected abstract void autoDiscoverTypes(ResultSet rs);

	protected List executeLoad(
			SessionImplementor session,
			QueryParameters queryParameters,
			LoadQueryDetails loadQueryDetails,
			boolean returnProxies,
			ResultTransformer forcedResultTransformer) throws SQLException {
		final List<AfterLoadAction> afterLoadActions = new ArrayList<AfterLoadAction>();
		return executeLoad(
				session,
				queryParameters,
				loadQueryDetails,
				returnProxies,
				forcedResultTransformer,
				afterLoadActions
		);
	}

	protected List executeLoad(
			SessionImplementor session,
			QueryParameters queryParameters,
			LoadQueryDetails loadQueryDetails,
			boolean returnProxies,
			ResultTransformer forcedResultTransformer,
			List<AfterLoadAction> afterLoadActions) throws SQLException {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final boolean defaultReadOnlyOrig = persistenceContext.isDefaultReadOnly();
		if ( queryParameters.isReadOnlyInitialized() ) {
			// The read-only/modifiable mode for the query was explicitly set.
			// Temporarily set the default read-only/modifiable setting to the query's setting.
			persistenceContext.setDefaultReadOnly( queryParameters.isReadOnly() );
		}
		else {
			// The read-only/modifiable setting for the query was not initialized.
			// Use the default read-only/modifiable from the persistence context instead.
			queryParameters.setReadOnly( persistenceContext.isDefaultReadOnly() );
		}
		persistenceContext.beforeLoad();
		try {
			List results = null;
			final String sql = loadQueryDetails.getSqlStatement();
			SqlStatementWrapper wrapper = null;
			try {
				wrapper = executeQueryStatement( sql, queryParameters, false, afterLoadActions, session );
				results = loadQueryDetails.getResultSetProcessor().extractResults(
						wrapper.getResultSet(),
						session,
						queryParameters,
						new NamedParameterContext() {
							@Override
							public int[] getNamedParameterLocations(String name) {
								return AbstractLoadPlanBasedLoader.this.getNamedParameterLocs( name );
							}
						},
						returnProxies,
						queryParameters.isReadOnly(),
						forcedResultTransformer,
						afterLoadActions
				);
			}
			finally {
				if ( wrapper != null ) {
					session.getTransactionCoordinator().getJdbcCoordinator().release(
							wrapper.getResultSet(),
							wrapper.getStatement()
					);
					session.getTransactionCoordinator().getJdbcCoordinator().release( wrapper.getStatement() );
				}
				persistenceContext.afterLoad();
			}
			persistenceContext.initializeNonLazyCollections();
			return results;
		}
		finally {
			// Restore the original default
			persistenceContext.setDefaultReadOnly( defaultReadOnlyOrig );
		}
	}

	protected SqlStatementWrapper executeQueryStatement(
			final QueryParameters queryParameters,
			final boolean scroll,
			List<AfterLoadAction> afterLoadActions,
			final SessionImplementor session) throws SQLException {
		return executeQueryStatement( getStaticLoadQuery().getSqlStatement(), queryParameters, scroll, afterLoadActions, session );
	}

	protected SqlStatementWrapper executeQueryStatement(
			String sqlStatement,
			QueryParameters queryParameters,
			boolean scroll,
			List<AfterLoadAction> afterLoadActions,
			SessionImplementor session) throws SQLException {

		// Processing query filters.
		queryParameters.processFilters( sqlStatement, session );

		// Applying LIMIT clause.
		final LimitHandler limitHandler = getLimitHandler(
				queryParameters.getFilteredSQL(),
				queryParameters.getRowSelection()
		);
		String sql = limitHandler.getProcessedSql();

		// Adding locks and comments.
		sql = preprocessSQL( sql, queryParameters, getFactory().getDialect(), afterLoadActions );

		final PreparedStatement st = prepareQueryStatement( sql, queryParameters, limitHandler, scroll, session );
		return new SqlStatementWrapper( st, getResultSet( st, queryParameters.getRowSelection(), limitHandler, queryParameters.hasAutoDiscoverScalarTypes(), session ) );
	}

	/**
	 * Build LIMIT clause handler applicable for given selection criteria. Returns {@link org.hibernate.dialect.pagination.NoopLimitHandler} delegate
	 * if dialect does not support LIMIT expression or processed query does not use pagination.
	 *
	 * @param sql Query string.
	 * @param selection Selection criteria.
	 * @return LIMIT clause delegate.
	 */
	protected LimitHandler getLimitHandler(String sql, RowSelection selection) {
		final LimitHandler limitHandler = getFactory().getDialect().buildLimitHandler( sql, selection );
		return LimitHelper.useLimit( limitHandler, selection ) ? limitHandler : new NoopLimitHandler( sql, selection );
	}

	private String preprocessSQL(
			String sql,
			QueryParameters queryParameters,
			Dialect dialect,
			List<AfterLoadAction> afterLoadActions) {
		return getFactory().getSettings().isCommentsEnabled()
				? prependComment( sql, queryParameters )
				: sql;
	}

	private String prependComment(String sql, QueryParameters parameters) {
		final String comment = parameters.getComment();
		if ( comment == null ) {
			return sql;
		}
		else {
			return "/* " + comment + " */ " + sql;
		}
	}

	/**
	 * Obtain a <tt>PreparedStatement</tt> with all parameters pre-bound.
	 * Bind JDBC-style <tt>?</tt> parameters, named parameters, and
	 * limit parameters.
	 */
	protected final PreparedStatement prepareQueryStatement(
			final String sql,
			final QueryParameters queryParameters,
			final LimitHandler limitHandler,
			final boolean scroll,
			final SessionImplementor session) throws SQLException, HibernateException {
		final Dialect dialect = getFactory().getDialect();
		final RowSelection selection = queryParameters.getRowSelection();
		final boolean useLimit = LimitHelper.useLimit( limitHandler, selection );
		final boolean hasFirstRow = LimitHelper.hasFirstRow( selection );
		final boolean useLimitOffset = hasFirstRow && useLimit && limitHandler.supportsLimitOffset();
		final boolean callable = queryParameters.isCallable();
		final ScrollMode scrollMode = getScrollMode( scroll, hasFirstRow, useLimitOffset, queryParameters );

		final PreparedStatement st = session.getTransactionCoordinator().getJdbcCoordinator()
				.getStatementPreparer().prepareQueryStatement( sql, callable, scrollMode );

		try {

			int col = 1;
			//TODO: can we limit stored procedures ?!
			col += limitHandler.bindLimitParametersAtStartOfQuery( st, col );

			if (callable) {
				col = dialect.registerResultSetOutParameter( (CallableStatement)st, col );
			}

			col += bindParameterValues( st, queryParameters, col, session );

			col += limitHandler.bindLimitParametersAtEndOfQuery( st, col );

			limitHandler.setMaxRows( st );

			if ( selection != null ) {
				if ( selection.getTimeout() != null ) {
					st.setQueryTimeout( selection.getTimeout() );
				}
				if ( selection.getFetchSize() != null ) {
					st.setFetchSize( selection.getFetchSize() );
				}
			}

			// handle lock timeout...
			final LockOptions lockOptions = queryParameters.getLockOptions();
			if ( lockOptions != null ) {
				if ( lockOptions.getTimeOut() != LockOptions.WAIT_FOREVER ) {
					if ( !dialect.supportsLockTimeouts() ) {
						if ( log.isDebugEnabled() ) {
							log.debugf(
									"Lock timeout [%s] requested but dialect reported to not support lock timeouts",
									lockOptions.getTimeOut()
							);
						}
					}
					else if ( dialect.isLockTimeoutParameterized() ) {
						st.setInt( col++, lockOptions.getTimeOut() );
					}
				}
			}

			if ( log.isTraceEnabled() ) {
				log.tracev( "Bound [{0}] parameters total", col );
			}
		}
		catch ( SQLException sqle ) {
			session.getTransactionCoordinator().getJdbcCoordinator().release( st );
			throw sqle;
		}
		catch ( HibernateException he ) {
			session.getTransactionCoordinator().getJdbcCoordinator().release( st );
			throw he;
		}

		return st;
	}

	protected ScrollMode getScrollMode(boolean scroll, boolean hasFirstRow, boolean useLimitOffSet, QueryParameters queryParameters) {
		final boolean canScroll = getFactory().getSettings().isScrollableResultSetsEnabled();
		if ( canScroll ) {
			if ( scroll ) {
				return queryParameters.getScrollMode();
			}
			if ( hasFirstRow && !useLimitOffSet ) {
				return ScrollMode.SCROLL_INSENSITIVE;
			}
		}
		return null;
	}

	/**
	 * Bind all parameter values into the prepared statement in preparation
	 * for execution.
	 *
	 * @param statement The JDBC prepared statement
	 * @param queryParameters The encapsulation of the parameter values to be bound.
	 * @param startIndex The position from which to start binding parameter values.
	 * @param session The originating session.
	 * @return The number of JDBC bind positions actually bound during this method execution.
	 * @throws SQLException Indicates problems performing the binding.
	 */
	protected int bindParameterValues(
			PreparedStatement statement,
			QueryParameters queryParameters,
			int startIndex,
			SessionImplementor session) throws SQLException {
		int span = 0;
		span += bindPositionalParameters( statement, queryParameters, startIndex, session );
		span += bindNamedParameters( statement, queryParameters.getNamedParameters(), startIndex + span, session );
		return span;
	}

	/**
	 * Bind positional parameter values to the JDBC prepared statement.
	 * <p/>
	 * Positional parameters are those specified by JDBC-style ? parameters
	 * in the source query.  It is (currently) expected that these come
	 * before any named parameters in the source query.
	 *
	 * @param statement The JDBC prepared statement
	 * @param queryParameters The encapsulation of the parameter values to be bound.
	 * @param startIndex The position from which to start binding parameter values.
	 * @param session The originating session.
	 * @return The number of JDBC bind positions actually bound during this method execution.
	 * @throws SQLException Indicates problems performing the binding.
	 * @throws org.hibernate.HibernateException Indicates problems delegating binding to the types.
	 */
	protected int bindPositionalParameters(
			final PreparedStatement statement,
			final QueryParameters queryParameters,
			final int startIndex,
			final SessionImplementor session) throws SQLException, HibernateException {
		final Object[] values = queryParameters.getFilteredPositionalParameterValues();
		final Type[] types = queryParameters.getFilteredPositionalParameterTypes();
		int span = 0;
		for ( int i = 0; i < values.length; i++ ) {
			types[i].nullSafeSet( statement, values[i], startIndex + span, session );
			span += types[i].getColumnSpan( getFactory() );
		}
		return span;
	}

	/**
	 * Bind named parameters to the JDBC prepared statement.
	 * <p/>
	 * This is a generic implementation, the problem being that in the
	 * general case we do not know enough information about the named
	 * parameters to perform this in a complete manner here.  Thus this
	 * is generally overridden on subclasses allowing named parameters to
	 * apply the specific behavior.  The most usual limitation here is that
	 * we need to assume the type span is always one...
	 *
	 * @param statement The JDBC prepared statement
	 * @param namedParams A map of parameter names to values
	 * @param startIndex The position from which to start binding parameter values.
	 * @param session The originating session.
	 * @return The number of JDBC bind positions actually bound during this method execution.
	 * @throws SQLException Indicates problems performing the binding.
	 * @throws org.hibernate.HibernateException Indicates problems delegating binding to the types.
	 */
	protected int bindNamedParameters(
			final PreparedStatement statement,
			final Map namedParams,
			final int startIndex,
			final SessionImplementor session) throws SQLException, HibernateException {
		if ( namedParams != null ) {
			// assumes that types are all of span 1
			final Iterator itr = namedParams.entrySet().iterator();
			final boolean debugEnabled = log.isDebugEnabled();
			int result = 0;
			while ( itr.hasNext() ) {
				final Map.Entry e = (Map.Entry) itr.next();
				final String name = (String) e.getKey();
				final TypedValue typedval = (TypedValue) e.getValue();
				final int[] locs = getNamedParameterLocs( name );
				for ( int loc : locs ) {
					if ( debugEnabled ) {
						log.debugf(
								"bindNamedParameters() %s -> %s [%s]",
								typedval.getValue(),
								name,
								loc + startIndex
						);
					}
					typedval.getType().nullSafeSet( statement, typedval.getValue(), loc + startIndex, session );
				}
				result += locs.length;
			}
			return result;
		}
		else {
			return 0;
		}
	}

	/**
	 * Execute given <tt>PreparedStatement</tt>, advance to the first result and return SQL <tt>ResultSet</tt>.
	 */
	protected final ResultSet getResultSet(
			final PreparedStatement st,
			final RowSelection selection,
			final LimitHandler limitHandler,
			final boolean autodiscovertypes,
			final SessionImplementor session)
			throws SQLException, HibernateException {

		try {
			ResultSet rs = session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().extract( st );
			rs = wrapResultSetIfEnabled( rs , session );

			if ( !limitHandler.supportsLimitOffset() || !LimitHelper.useLimit( limitHandler, selection ) ) {
				advance( rs, selection );
			}

			if ( autodiscovertypes ) {
				autoDiscoverTypes( rs );
			}
			return rs;
		}
		catch ( SQLException sqle ) {
			session.getTransactionCoordinator().getJdbcCoordinator().release( st );
			throw sqle;
		}
	}

	/**
	 * Advance the cursor to the first required row of the <tt>ResultSet</tt>
	 */
	protected void advance(final ResultSet rs, final RowSelection selection) throws SQLException {
		final int firstRow = LimitHelper.getFirstRow( selection );
		if ( firstRow != 0 ) {
			if ( getFactory().getSettings().isScrollableResultSetsEnabled() ) {
				// we can go straight to the first required row
				rs.absolute( firstRow );
			}
			else {
				// we need to step through the rows one row at a time (slow)
				for ( int m = 0; m < firstRow; m++ ) {
					rs.next();
				}
			}
		}
	}

	private synchronized ResultSet wrapResultSetIfEnabled(final ResultSet rs, final SessionImplementor session) {
		// synchronized to avoid multi-thread access issues; defined as method synch to avoid
		// potential deadlock issues due to nature of code.
		if ( session.getFactory().getSettings().isWrapResultSetsEnabled() ) {
			try {
				if ( log.isDebugEnabled() ) {
					log.debugf( "Wrapping result set [%s]", rs );
				}
				return session.getFactory()
						.getJdbcServices()
						.getResultSetWrapper().wrap( rs, retreiveColumnNameToIndexCache( rs ) );
			}
			catch(SQLException e) {
				log.unableToWrapResultSet( e );
				return rs;
			}
		}
		else {
			return rs;
		}
	}

	private ColumnNameCache retreiveColumnNameToIndexCache(ResultSet rs) throws SQLException {
		if ( columnNameCache == null ) {
			log.trace( "Building columnName->columnIndex cache" );
			columnNameCache = new ColumnNameCache( rs.getMetaData().getColumnCount() );
		}

		return columnNameCache;
	}

	/**
	 * Wrapper class for {@link java.sql.Statement} and associated {@link java.sql.ResultSet}.
	 */
	protected static class SqlStatementWrapper {
		private final Statement statement;
		private final ResultSet resultSet;

		private SqlStatementWrapper(Statement statement, ResultSet resultSet) {
			this.resultSet = resultSet;
			this.statement = statement;
		}

		public ResultSet getResultSet() {
			return resultSet;
		}

		public Statement getStatement() {
			return statement;
		}
	}
}
