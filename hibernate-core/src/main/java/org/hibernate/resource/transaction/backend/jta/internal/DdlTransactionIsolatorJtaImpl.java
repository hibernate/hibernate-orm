/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import java.sql.Connection;
import java.sql.SQLException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

import org.jboss.logging.Logger;

/**
 * DdlExecutor for use in JTA environments
 *
 * @author Steve Ebersole
 */
public class DdlTransactionIsolatorJtaImpl implements DdlTransactionIsolator {
	private static final Logger log = Logger.getLogger( DdlTransactionIsolatorJtaImpl.class );

	private final JdbcContext jdbcContext;

	private final Transaction suspendedTransaction;
	private Connection jdbcConnection;

	public DdlTransactionIsolatorJtaImpl(JdbcContext jdbcContext) {
		this.jdbcContext = jdbcContext;

		try {
			final JtaPlatform jtaPlatform = jdbcContext.getServiceRegistry().requireService( JtaPlatform.class );
			log.tracef( "DdlTransactionIsolatorJtaImpl#prepare: JtaPlatform -> %s", jtaPlatform );

			final TransactionManager tm = jtaPlatform.retrieveTransactionManager();
			if ( tm == null ) {
				throw new HibernateException(
						"DdlTransactionIsolatorJtaImpl could not locate TransactionManager to suspend any current transaction; " +
						"base JtaPlatform impl (" + jtaPlatform + ")?"
				);
			}
			log.tracef( "DdlTransactionIsolatorJtaImpl#prepare: TransactionManager -> %s", tm );

			this.suspendedTransaction = tm.suspend();
			log.tracef( "DdlTransactionIsolatorJtaImpl#prepare: suspended Transaction -> %s", this.suspendedTransaction );
		}
		catch (SystemException e) {
			throw new HibernateException( "Unable to suspend current JTA transaction in preparation for DDL execution" );
		}

	}

	@Override
	public JdbcContext getJdbcContext() {
		return jdbcContext;
	}

	@Override
	public Connection getIsolatedConnection() {
		return getIsolatedConnection(true);
	}

	@Override
	public Connection getIsolatedConnection(boolean autocommit) {
		if ( jdbcConnection == null ) {
			try {
				jdbcConnection = jdbcContext.getJdbcConnectionAccess().obtainConnection();
			}
			catch (SQLException e) {
				throw jdbcContext.getSqlExceptionHelper().convert( e, "Unable to open JDBC Connection for DDL execution" );
			}

			try {
				if ( jdbcConnection.getAutoCommit() != autocommit ) {
					jdbcConnection.setAutoCommit( autocommit );
				}
			}
			catch (SQLException e) {
				throw jdbcContext.getSqlExceptionHelper().convert( e, "Unable to set JDBC Connection for DDL execution to autocommit" );
			}
		}
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
				jdbcContext.getServiceRegistry().requireService( JtaPlatform.class )
						.retrieveTransactionManager()
						.resume( suspendedTransaction );
			}
			catch (Exception e) {
				throw new HibernateException( "Unable to resume JTA transaction after DDL execution" );
			}
		}
	}
}
