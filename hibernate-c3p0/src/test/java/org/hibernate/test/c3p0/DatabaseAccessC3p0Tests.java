package org.hibernate.test.c3p0;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;
import com.mchange.v2.c3p0.PooledDataSource;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies the legacy c3p0 initial-size override without changing its behavior.
///
/// @author Steve Ebersole
@BaseUnitTest
public class DatabaseAccessC3p0Tests {
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void initialSizeOverride(boolean explicitC3p0Setting) throws Exception {
		var settings = new HashMap<String, Object>( Map.of(
				JdbcSettings.CONNECTION_PROVIDER, C3P0ConnectionProvider.class,
				JdbcSettings.URL, "jdbc:h2:mem:c3p0_" + UUID.randomUUID(),
				JdbcSettings.USER, "sa",
				JdbcSettings.PASS, "",
				"hibernate.c3p0.min_size", "1",
				"hibernate.c3p0.max_size", "4",
				"hibernate.c3p0.initialPoolSize", "3"
		) );
		if ( explicitC3p0Setting ) {
			settings.put( "c3p0.initialPoolSize", "3" );
		}
		try ( var registry = new StandardServiceRegistryBuilder().clearSettings().applySettings( settings ).build() ) {
			var provider = registry.requireService( ConnectionProvider.class );
			var dataSource = (PooledDataSource) provider.unwrap( DataSource.class );
			// The effective property is available from c3p0's PoolBackedDataSource.
			var effective = (com.mchange.v2.c3p0.PoolBackedDataSource) dataSource;
			var config = (com.mchange.v2.c3p0.WrapperConnectionPoolDataSource) effective.getConnectionPoolDataSource();
			assertThat( config.getInitialPoolSize() ).isEqualTo( explicitC3p0Setting ? 3 : 1 );
			try ( var connection = provider.getConnection() ) {
				assertThat( connection.isValid( 1 ) ).isTrue();
			}
		}
	}
}
