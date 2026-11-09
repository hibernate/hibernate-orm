package org.hibernate.testing.boot;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.spi.ParameterMarkerStrategy;

import org.hibernate.testing.env.ConnectionProviderBuilder;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of the {@link JdbcServices} contract for use by tests.
 * <p>
 * An alternative approach is to build a {@link ServiceRegistryTestingImpl} and grab the {@link JdbcServices}
 * from that.
 *
 * @author Steve Ebersole
 */
public class BasicTestingJdbcServiceImpl implements JdbcServices, ServiceRegistryAwareService {
	private @Nullable JdbcEnvironment jdbcEnvironment;
	private @Nullable ConnectionProvider connectionProvider;
	private @Nullable SqlStatementLogger sqlStatementLogger;

	private @Nullable JdbcConnectionAccess jdbcConnectionAccess;
	private @Nullable ServiceRegistry serviceRegistry;
	private @Nonnull ParameterMarkerStrategy parameterMarkerStrategy = ParameterMarkerStrategyStandard.INSTANCE;

	public void start() {
	}

	public void stop() {
		release();
	}

	public void prepare(boolean allowAggressiveRelease) throws SQLException {
		final var dialect = ConnectionProviderBuilder.getCorrespondingDialect();
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
	@Nonnull
	public JdbcEnvironment getJdbcEnvironment() {
		return requireNonNull( jdbcEnvironment, "JDBC services have not been prepared" );
	}

	@Override
	@Nonnull
	public JdbcConnectionAccess getBootstrapJdbcConnectionAccess() {
		return requireNonNull( jdbcConnectionAccess, "JDBC services have not been prepared" );
	}

	@Override
	@Nonnull
	public Dialect getDialect() {
		return getJdbcEnvironment().getDialect();
	}

	@Override
	@Nonnull
	public LobCreator getLobCreator(@Nonnull LobCreationContext lobCreationContext) {
		return jdbcEnvironment.getLobCreatorBuilder().buildLobCreator( lobCreationContext );
	}

	@Override
	@Nonnull
	public SqlStatementLogger getSqlStatementLogger() {
		return requireNonNull( sqlStatementLogger, "JDBC services have not been prepared" );
	}

	@Override
	@Nonnull
	public ParameterMarkerStrategy getParameterMarkerStrategy() {
		return parameterMarkerStrategy;
	}

	@Override
	@Nonnull
	public SqlExceptionHelper getSqlExceptionHelper() {
		return jdbcEnvironment.getSqlExceptionHelper();
	}

	@Override
	@Nonnull
	public JdbcMetadata getJdbcMetadata() {
		return jdbcEnvironment.getJdbcMetadata();
	}

	@Deprecated(since = "8.0")
	@Override
	@Nonnull
	public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
		return jdbcEnvironment.getJdbcMetadata().getExtractedDatabaseMetaData();
	}

	@Override
	public void injectServices(@Nonnull ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.parameterMarkerStrategy = serviceRegistry.requireService( ParameterMarkerStrategy.class );
	}
}
