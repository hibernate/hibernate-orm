/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jdbc.internal;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;

/**
 * @author Andrea Boriero
 */
public class JdbcIsolationDelegate implements IsolationDelegate {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( JdbcIsolationDelegate.class );

	private final JdbcConnectionAccess connectionAccess;
	private final SqlExceptionHelper sqlExceptionHelper;

	public JdbcIsolationDelegate(JdbcConnectionAccess connectionAccess, SqlExceptionHelper sqlExceptionHelper) {
		this.connectionAccess = connectionAccess;
		this.sqlExceptionHelper = sqlExceptionHelper;
	}

	protected JdbcConnectionAccess jdbcConnectionAccess() {
		return this.connectionAccess;
	}

	protected SqlExceptionHelper sqlExceptionHelper() {
		return this.sqlExceptionHelper;
	}

	@Override
	public <T> T delegateWork(WorkExecutorVisitable<T> work, boolean transacted) throws HibernateException {
		boolean wasAutoCommit = false;
		try {
			Connection connection = jdbcConnectionAccess().obtainConnection();
			try {
				if ( transacted ) {
					if ( connection.getAutoCommit() ) {
						wasAutoCommit = true;
						connection.setAutoCommit( false );
					}
				}

				T result = work.accept( new WorkExecutor<T>(), connection );

				if ( transacted ) {
					connection.commit();
				}

				return result;
			}
			catch (Exception e) {
				try {
					if ( transacted && !connection.isClosed() ) {
						connection.rollback();
					}
				}
				catch (Exception ignore) {
					LOG.unableToRollbackConnection( ignore );
				}

				if ( e instanceof HibernateException ) {
					throw (HibernateException) e;
				}
				else if ( e instanceof SQLException ) {
					throw sqlExceptionHelper().convert( (SQLException) e, "error performing isolated work" );
				}
				else {
					throw new HibernateException( "error performing isolated work", e );
				}
			}
			finally {
				if ( transacted && wasAutoCommit ) {
					try {
						connection.setAutoCommit( true );
					}
					catch (Exception ignore) {
						LOG.trace( "was unable to reset connection back to auto-commit" );
					}
				}
				try {
					jdbcConnectionAccess().releaseConnection( connection );
				}
				catch (Exception ignore) {
					LOG.unableToReleaseIsolatedConnection( ignore );
				}
			}
		}
		catch (SQLException sqle) {
			throw sqlExceptionHelper().convert( sqle, "unable to obtain isolated JDBC connection" );
		}
	}
}
