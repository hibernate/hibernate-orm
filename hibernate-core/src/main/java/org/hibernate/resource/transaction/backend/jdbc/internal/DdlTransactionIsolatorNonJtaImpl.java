/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jdbc.internal;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.internal.log.ConnectionAccessLogger;
import org.hibernate.internal.util.ExceptionHelper;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

/**
 * DdlExecutor for use in non-JTA environments (JDBC transaction)
 *
 * @author Steve Ebersole
 */
public class DdlTransactionIsolatorNonJtaImpl implements DdlTransactionIsolator {
	private final JdbcContext jdbcContext;

	private Connection jdbcConnection;
	private boolean unsetAutoCommit;

	public DdlTransactionIsolatorNonJtaImpl(JdbcContext jdbcContext) {
		this.jdbcContext = jdbcContext;
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
				try {
					if ( jdbcConnection.getAutoCommit() != autocommit ) {
						try {
							if ( autocommit ) {
								ConnectionAccessLogger.INSTANCE.informConnectionLocalTransactionForNonJtaDdl();
								jdbcConnection.commit();
							}
							jdbcConnection.setAutoCommit( autocommit );
							unsetAutoCommit = true;
						}
						catch (SQLException e) {
							throw jdbcContext.getSqlExceptionHelper().convert(
									e,
									"Unable to set JDBC Connection auto-commit mode in preparation for DDL execution"
							);
						}
					}
				}
				catch (SQLException e) {
					throw jdbcContext.getSqlExceptionHelper().convert(
							e,
							"Unable to check JDBC Connection auto-commit in preparation for DDL execution"
					);
				}
			}
			catch (SQLException e) {
				throw jdbcContext.getSqlExceptionHelper().convert(
						e,
						"Unable to open JDBC Connection for DDL execution"
				);
			}
		}

		return jdbcConnection;
	}

	@Override
	public void release() {
		if ( jdbcConnection != null ) {
			Throwable originalException = null;
			try {
				if ( unsetAutoCommit ) {
					try {
						jdbcConnection.setAutoCommit( !jdbcConnection.getAutoCommit() );
					}
					catch (SQLException e) {
						originalException = jdbcContext.getSqlExceptionHelper().convert(
								e,
								"Unable to unset auto-commit mode for JDBC Connection used for DDL execution" );
					}
					catch (Throwable t1) {
						originalException = t1;
					}
				}
			}
			finally {
				Throwable suppressed = null;
				try {
					jdbcContext.getJdbcConnectionAccess().releaseConnection( jdbcConnection );
				}
				catch (SQLException e) {
					suppressed = jdbcContext.getSqlExceptionHelper().convert(
							e,
							"Unable to release JDBC Connection used for DDL execution" );
				}
				catch (Throwable t2) {
					suppressed = t2;
				}
				jdbcConnection = null;
				if ( suppressed != null ) {
					if ( originalException == null ) {
						originalException = suppressed;
					}
					else {
						originalException.addSuppressed( suppressed );
					}
				}
			}
			if ( originalException != null ) {
				ExceptionHelper.rethrow( originalException );
			}
		}
	}
}
