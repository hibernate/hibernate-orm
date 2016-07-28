package org.hibernate.test.util;

import java.sql.Connection;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.resource.transaction.backend.jdbc.internal.DdlTransactionIsolatorNonJtaImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.internal.exec.JdbcConnectionAccessConnectionProviderImpl;
import org.hibernate.tool.schema.internal.exec.JdbcConnectionAccessProvidedConnectionImpl;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

/**
 * @author Steve Ebersole
 */
public class DdlTransactionIsolatorTestingImpl extends DdlTransactionIsolatorNonJtaImpl {
	public DdlTransactionIsolatorTestingImpl(ServiceRegistry serviceRegistry, Connection jdbConnection) {
		this( serviceRegistry, createJdbcConnectionAccess( jdbConnection ) );
	}

	public static JdbcConnectionAccess createJdbcConnectionAccess(Connection jdbcConnection) {
		return new JdbcConnectionAccessProvidedConnectionImpl( jdbcConnection );
	}

	public DdlTransactionIsolatorTestingImpl(ServiceRegistry serviceRegistry, JdbcConnectionAccess jdbcConnectionAccess) {
		super( createJdbcContext( jdbcConnectionAccess, serviceRegistry ) );
	}

	public static JdbcContext createJdbcContext(
			JdbcConnectionAccess jdbcConnectionAccess,
			ServiceRegistry serviceRegistry) {
		return new JdbcContext() {
			final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

			@Override
			public JdbcConnectionAccess getJdbcConnectionAccess() {
				return jdbcConnectionAccess;
			}

			@Override
			public Dialect getDialect() {
				return jdbcServices.getJdbcEnvironment().getDialect();
			}

			@Override
			public SqlStatementLogger getSqlStatementLogger() {
				return jdbcServices.getSqlStatementLogger();
			}

			@Override
			public SqlExceptionHelper getSqlExceptionHelper() {
				return jdbcServices.getSqlExceptionHelper();
			}

			@Override
			public ServiceRegistry getServiceRegistry() {
				return serviceRegistry;
			}
		};
	}

	public DdlTransactionIsolatorTestingImpl(ServiceRegistry serviceRegistry, ConnectionProvider connectionProvider) {
		this( serviceRegistry, createJdbcConnectionAccess( connectionProvider ) );
	}

	private static JdbcConnectionAccess createJdbcConnectionAccess(ConnectionProvider connectionProvider) {
		return new JdbcConnectionAccessConnectionProviderImpl( connectionProvider );
	}

}
