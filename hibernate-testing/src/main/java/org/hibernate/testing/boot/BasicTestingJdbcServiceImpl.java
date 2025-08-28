/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.boot;

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
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

import org.hibernate.testing.env.ConnectionProviderBuilder;


/**
 * Implementation of the {@link JdbcServices} contract for use by tests.
 * <p>
 * An alternative approach is to build a {@link ServiceRegistryTestingImpl} and grab the {@link JdbcServices}
 * from that.
 *
 * @author Steve Ebersole
 */
public class BasicTestingJdbcServiceImpl implements JdbcServices, ServiceRegistryAwareService {
	private JdbcEnvironment jdbcEnvironment;
	private ConnectionProvider connectionProvider;
	private Dialect dialect;
	private SqlStatementLogger sqlStatementLogger;

	private JdbcConnectionAccess jdbcConnectionAccess;
	private ServiceRegistry serviceRegistry;
	private ParameterMarkerStrategy parameterMarkerStrategy;

	public void start() {
	}

	public void stop() {
		release();
	}

	public void prepare(boolean allowAggressiveRelease) throws SQLException {
		dialect = ConnectionProviderBuilder.getCorrespondingDialect();
		connectionProvider = ConnectionProviderBuilder.buildConnectionProvider( allowAggressiveRelease );
		sqlStatementLogger = new SqlStatementLogger( true, false, false );
		this.jdbcConnectionAccess = new JdbcConnectionAccessImpl( connectionProvider );

		Connection jdbcConnection = connectionProvider.getConnection();
		try {
			jdbcEnvironment = new JdbcEnvironmentImpl( jdbcConnection.getMetaData(), dialect, jdbcConnectionAccess );
		}
		finally {
			try {
				connectionProvider.closeConnection( jdbcConnection );
			}
			catch (SQLException ignore) {
			}
		}

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

	public SqlStatementLogger getSqlStatementLogger() {
		return sqlStatementLogger;
	}

	@Override
	public ParameterMarkerStrategy getParameterMarkerStrategy() {
		return parameterMarkerStrategy;
	}

	public SqlExceptionHelper getSqlExceptionHelper() {
		return jdbcEnvironment.getSqlExceptionHelper();
	}

	public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
		return jdbcEnvironment.getExtractedDatabaseMetaData();
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.parameterMarkerStrategy = serviceRegistry.getService( ParameterMarkerStrategy.class );
	}
}
