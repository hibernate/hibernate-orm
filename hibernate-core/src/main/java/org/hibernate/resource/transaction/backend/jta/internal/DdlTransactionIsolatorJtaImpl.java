/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import java.sql.Connection;
import java.sql.SQLException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

/**
 * DdlExecutor for use in JTA environments
 *
 * @author Steve Ebersole
 */
public class DdlTransactionIsolatorJtaImpl implements DdlTransactionIsolator {
	private final JdbcContext jdbcContext;

	private Transaction suspendedTransaction;
	private Connection jdbcConnection;

	public DdlTransactionIsolatorJtaImpl(JdbcContext jdbcContext) {
		this.jdbcContext = jdbcContext;

		try {
			this.suspendedTransaction = jdbcContext.getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().suspend();
		}
		catch (SystemException e) {
			throw new HibernateException( "Unable to suspend current JTA transaction in preparation for DDL execution" );
		}

		try {
			this.jdbcConnection = jdbcContext.getJdbcConnectionAccess().obtainConnection();
		}
		catch (SQLException e) {
			throw jdbcContext.getSqlExceptionHelper().convert( e, "Unable to open JDBC Connection for DDL execution" );
		}

		try {
			jdbcConnection.setAutoCommit( true );
		}
		catch (SQLException e) {
			throw jdbcContext.getSqlExceptionHelper().convert( e, "Unable set JDBC Connection for DDL execution to autocommit" );
		}
	}

	@Override
	public JdbcContext getJdbcContext() {
		return jdbcContext;
	}

	@Override
	public void prepare() {
	}

	@Override
	public Connection getIsolatedConnection() {
		return jdbcConnection;
	}

	@Override
	public void release() {
		if ( jdbcConnection != null ) {
			try {
				jdbcContext.getJdbcConnectionAccess().releaseConnection( jdbcConnection );
			}
			catch (SQLException e) {
				throw jdbcContext.getSqlExceptionHelper().convert( e, "Unable to release JDBC Connection used for DDL execution" );
			}
		}

		if ( suspendedTransaction != null ) {
			try {
				jdbcContext.getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().resume( suspendedTransaction );
			}
			catch (Exception e) {
				throw new HibernateException( "Unable to resume JTA transaction after DDL execution" );
			}
		}
	}
}
