package org.hibernate.orm.test.jdbc.env;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.internal.ConfigurationServiceImpl;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.JdbcLogging;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.TriggerOnPrefixLogListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies when a low default JDBC fetch size is worth a warning.
public class LowFetchSizeWarningTest {
	private static final String ORACLE_DRIVER = "Oracle JDBC driver";

	private TriggerOnPrefixLogListener lowFetchSizeWarning;

	@BeforeEach
	void registerListener() {
		lowFetchSizeWarning = new TriggerOnPrefixLogListener( "HHH100123" );
		LogInspectionHelper.registerListener( lowFetchSizeWarning, JdbcLogging.JDBC_LOGGER );
	}

	@AfterEach
	void clearListeners() {
		LogInspectionHelper.clearAllListeners( JdbcLogging.JDBC_LOGGER );
	}

	@Test
	void warnsAboutLowFetchSizeOfDriverWithoutAdaptiveFetching() throws Exception {
		createEnvironment( "Fixture JDBC driver", 1, 10, Map.of() );

		assertThat( lowFetchSizeWarning.wasTriggered() ).isTrue();
	}

	@Test
	void warnsAboutLowFetchSizeOfOracleDriverBefore23() throws Exception {
		createEnvironment( ORACLE_DRIVER, 21, 10, Map.of() );

		assertThat( lowFetchSizeWarning.wasTriggered() ).isTrue();
	}

	@Test
	void doesNotWarnAboutLowFetchSizeOfOracleDriverWithAdaptiveFetching() throws Exception {
		createEnvironment( ORACLE_DRIVER, 23, 10, Map.of() );

		assertThat( lowFetchSizeWarning.wasTriggered() ).isFalse();
	}

	@Test
	void doesNotWarnWhenFetchSizeIsConfigured() throws Exception {
		createEnvironment( "Fixture JDBC driver", 1, 10, Map.of( JdbcSettings.STATEMENT_FETCH_SIZE, "500" ) );

		assertThat( lowFetchSizeWarning.wasTriggered() ).isFalse();
	}

	private static void createEnvironment(
			String driverName,
			int driverMajorVersion,
			int defaultFetchSize,
			Map<String, Object> settings) throws SQLException {
		final ServiceRegistryImplementor serviceRegistry = mock( ServiceRegistryImplementor.class );
		when( serviceRegistry.requireService( ConfigurationService.class ) )
				.thenReturn( new ConfigurationServiceImpl( settings ) );

		final DatabaseMetaData metadata = mock( DatabaseMetaData.class );
		final Connection connection = mock( Connection.class );
		final Statement statement = mock( Statement.class );
		when( metadata.getConnection() ).thenReturn( connection );
		when( metadata.getDriverName() ).thenReturn( driverName );
		when( metadata.getDriverMajorVersion() ).thenReturn( driverMajorVersion );
		when( metadata.getCatalogSeparator() ).thenReturn( "." );
		when( metadata.isCatalogAtStart() ).thenReturn( true );
		when( connection.getMetaData() ).thenReturn( metadata );
		when( connection.createStatement() ).thenReturn( statement );
		when( statement.getFetchSize() ).thenReturn( defaultFetchSize );

		new JdbcEnvironmentImpl(
				serviceRegistry,
				new Dialect( DatabaseVersion.make( 1 ) ) {
				},
				metadata,
				mock( JdbcConnectionAccess.class )
		);
	}
}
