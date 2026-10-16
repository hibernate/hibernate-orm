package org.hibernate.test.hikaricp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Hikari configuration and ownership used by the Database Access guide.
///
/// @author Steve Ebersole
@BaseUnitTest
@RequiresDialect(H2Dialect.class)
public class DatabaseAccessHikariTests {
	@Test
	void managedPool() throws Exception {
		String jdbcUrl = "jdbc:h2:mem:hikari_" + UUID.randomUUID();
		HikariDataSource pool;
		// tag::database-hikari[]
		try ( var factory = new HibernatePersistenceConfiguration( "pooled" )
				.property( "hibernate.connection.provider_class", "hikaricp" )
				.property( "hibernate.connection.url", jdbcUrl )
				.property( "jakarta.persistence.jdbc.user", "sa" )
				.property( "jakarta.persistence.jdbc.password", "" )
				.property( "hibernate.hikari.maximumPoolSize", 4 )
				.property( "hibernate.hikari.autoCommit", false )
				.property( "hibernate.connection.provider_disables_autocommit", true )
				.createEntityManagerFactory() ) {
			factory.runInTransaction( entityManager -> {
				// Perform persistence operations here.
			} );
			// end::database-hikari[]
			var provider = factory.unwrap( SessionFactoryImplementor.class ).getServiceRegistry()
					.requireService( ConnectionProvider.class );
			pool = provider.unwrap( HikariDataSource.class );
			assertThat( pool.getMaximumPoolSize() ).isEqualTo( 4 );
			try ( var connection = provider.getConnection() ) {
				assertThat( connection.getAutoCommit() ).isFalse();
				assertThat( connection.isValid( 1 ) ).isTrue();
			}
			// tag::database-hikari[]
		}
		// end::database-hikari[]
		assertThat( pool.isClosed() ).isTrue();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void poolSizeTranslationAndOverride(boolean explicitMaximum) throws Exception {
		var properties = new HashMap<String, Object>( Map.of(
				JdbcSettings.JAKARTA_JDBC_URL, "jdbc:h2:mem:hikari_override_" + UUID.randomUUID(),
				JdbcSettings.POOL_SIZE, 4,
				"hibernate.hikari.minimumIdle", 0
		) );
		if ( explicitMaximum ) {
			properties.put( "hibernate.hikari.maximumPoolSize", 2 );
		}
		var provider = new HikariCPConnectionProvider();
		provider.configure( properties );
		try {
			assertThat( provider.unwrap( HikariDataSource.class ).getMaximumPoolSize() ).isEqualTo( explicitMaximum ? 2 : 4 );
			try ( var connection = provider.getConnection() ) {
				assertThat( connection.isValid( 1 ) ).isTrue();
			}
		}
		finally {
			provider.stop();
		}
	}
}
