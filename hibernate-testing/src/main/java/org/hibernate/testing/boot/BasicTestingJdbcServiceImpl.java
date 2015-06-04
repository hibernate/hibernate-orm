/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.engine.jdbc.internal.ResultSetWrapperImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.ResultSetWrapper;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import org.hibernate.testing.env.ConnectionProviderBuilder;


/**
 * Implementation of the {@link JdbcServices} contract for use by tests.
 * <p/>
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
		return new ResultSetWrapperImpl( serviceRegistry );
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

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
}
