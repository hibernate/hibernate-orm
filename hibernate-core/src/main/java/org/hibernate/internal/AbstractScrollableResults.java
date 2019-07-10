/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.Loader;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.sql.results.spi.RowReader;

/**
 * Base implementation of the ScrollableResults interface.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractScrollableResults<R> implements ScrollableResultsImplementor<R> {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractScrollableResults.class );

	private final ResultSet resultSet;
	private final PreparedStatement ps;
	private final SharedSessionContractImplementor session;
	private final Loader loader;
	private final QueryParameters queryParameters;

	private final RowReader<R> rowReader;

	private boolean closed;

	@SuppressWarnings("WeakerAccess")
	protected AbstractScrollableResults(
			ResultSet rs,
			PreparedStatement ps,
			SharedSessionContractImplementor sess,
			Loader loader,
			QueryParameters queryParameters,
			RowReader<R> rowReader) {
		this.resultSet = rs;
		this.ps = ps;
		this.session = sess;
		this.loader = loader;
		this.queryParameters = queryParameters;
		this.rowReader = rowReader;
	}

	protected abstract R getCurrentRow();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Access to state fr sub-types

	protected ResultSet getResultSet() {
		return resultSet;
	}

	protected PreparedStatement getPs() {
		return ps;
	}

	protected SharedSessionContractImplementor getSession() {
		return session;
	}

	protected Loader getLoader() {
		return loader;
	}

	protected QueryParameters getQueryParameters() {
		return queryParameters;
	}

	protected RowReader<R> getRowReader() {
		return rowReader;
	}

	@Override
	public final R get() throws HibernateException {
		if ( closed ) {
			throw new IllegalStateException( "ScrollableResults is closed" );
		}
		return getCurrentRow();
	}

	protected void afterScrollOperation() {
		session.afterScrollOperation();
	}

	@Override
	public boolean isClosed() {
		return this.closed;
	}

	@Override
	public final void close() {
		if ( this.closed ) {
			// noop if already closed
			return;
		}

//		getJdbcValues().finishUp();
//		getPersistenceContext().getJdbcCoordinator().afterStatementExecution();

//		// not absolutely necessary, but does help with aggressive release
//		//session.getJDBCContext().getConnectionManager().closeQueryStatement( ps, resultSet );
//		session.getJdbcCoordinator().getResourceRegistry().release( ps );
//		session.getJdbcCoordinator().afterStatementExecution();
//		try {
//			session.getPersistenceContext().getLoadContexts().cleanup( resultSet );
//		}
//		catch (Throwable ignore) {
//			// ignore this error for now
//			if ( LOG.isTraceEnabled() ) {
//				LOG.tracev( "Exception trying to cleanup load context : {0}", ignore.getMessage() );
//			}
//		}

		this.closed = true;
	}
}
