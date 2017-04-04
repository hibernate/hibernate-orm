/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jdbc.internal;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.internal.log.ConnectionAccessLogger;
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

	public DdlTransactionIsolatorNonJtaImpl(JdbcContext jdbcContext) {
		this.jdbcContext = jdbcContext;
	}

	@Override
	public void prepare() {
	}

	@Override
	public JdbcContext getJdbcContext() {
		return jdbcContext;
	}

	@Override
	public Connection getIsolatedConnection() {
		if ( jdbcConnection == null ) {
			try {
				this.jdbcConnection = jdbcContext.getJdbcConnectionAccess().obtainConnection();

				try {
					if ( !jdbcConnection.getAutoCommit() ) {
						ConnectionAccessLogger.INSTANCE.informConnectionLocalTransactionForNonJtaDdl( jdbcContext.getJdbcConnectionAccess() );

						try {
							jdbcConnection.commit();
							jdbcConnection.setAutoCommit( true );
						}
						catch (SQLException e) {
							throw jdbcContext.getSqlExceptionHelper().convert(
									e,
									"Unable to set JDBC Connection into auto-commit mode in preparation for DDL execution"
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
			try {
				jdbcContext.getJdbcConnectionAccess().releaseConnection( jdbcConnection );
			}
			catch (SQLException e) {
				throw jdbcContext.getSqlExceptionHelper().convert( e, "Unable to release JDBC Connection used for DDL execution" );
			}
		}
	}
}
