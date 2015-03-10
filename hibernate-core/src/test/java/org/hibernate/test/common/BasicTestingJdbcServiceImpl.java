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
package org.hibernate.test.common;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.ResultSetWrapperImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.ResultSetWrapper;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.service.spi.Stoppable;

import org.hibernate.testing.env.ConnectionProviderBuilder;


/**
 * Implementation of the {@link JdbcServices} contract for use by these
 * tests.
 *
 * @author Steve Ebersole
 */
public class BasicTestingJdbcServiceImpl implements JdbcServices {
	private JdbcEnvironment jdbcEnvironment;
	private ConnectionProvider connectionProvider;
	private Dialect dialect;
	private SqlStatementLogger sqlStatementLogger;

	private JdbcConnectionAccess jdbcConnectionAccess;

	public void start() {
	}

	public void stop() {
		release();
	}

	public void prepare(boolean allowAggressiveRelease) throws SQLException {
		dialect = ConnectionProviderBuilder.getCorrespondingDialect();
		connectionProvider = ConnectionProviderBuilder.buildConnectionProvider( allowAggressiveRelease );
		sqlStatementLogger = new SqlStatementLogger( true, false );

		Connection jdbcConnection = connectionProvider.getConnection();
		try {
			jdbcEnvironment = new JdbcEnvironmentImpl( jdbcConnection.getMetaData(), dialect );
		}
		finally {
			try {
				connectionProvider.closeConnection( jdbcConnection );
			}
			catch (SQLException ignore) {
			}
		}

		this.jdbcConnectionAccess = new JdbcConnectionAccessImpl( connectionProvider );
	}

	public void release() {
		if ( connectionProvider instanceof Stoppable ) {
			( (Stoppable) connectionProvider ).stop();
		}
	}

	@Override
	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	public ConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}

	@Override
	public JdbcConnectionAccess getBootstrapJdbcConnectionAccess() {
		return jdbcConnectionAccess;
	}

	public Dialect getDialect() {
		return dialect;
	}

	public LobCreator getLobCreator(LobCreationContext lobCreationContext) {
		return jdbcEnvironment.getLobCreatorBuilder().buildLobCreator( lobCreationContext );
	}

	public ResultSetWrapper getResultSetWrapper() {
		return ResultSetWrapperImpl.INSTANCE;
	}

	public SqlStatementLogger getSqlStatementLogger() {
		return sqlStatementLogger;
	}

	public SqlExceptionHelper getSqlExceptionHelper() {
		return jdbcEnvironment.getSqlExceptionHelper();
	}

	public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
		return jdbcEnvironment.getExtractedDatabaseMetaData();
	}
}
