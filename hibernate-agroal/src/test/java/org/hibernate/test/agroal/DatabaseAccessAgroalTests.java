package org.hibernate.test.agroal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.agroal.api.AgroalDataSource;
import org.hibernate.agroal.internal.AgroalConnectionProvider;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies the Agroal precedence documented in Database Access.
///
/// @author Steve Ebersole
@BaseUnitTest
public class DatabaseAccessAgroalTests {
	@Test
	void commonConnectionSettingsOverrideButExplicitPoolSizeWins() throws Exception {
		String url = "jdbc:h2:mem:agroal_" + UUID.randomUUID();
		var provider = new AgroalConnectionProvider();
		provider.configure( new HashMap<>( Map.of(
				JdbcSettings.URL, url,
				JdbcSettings.DRIVER, "org.h2.Driver",
				JdbcSettings.AUTOCOMMIT, "false",
				JdbcSettings.POOL_SIZE, 4,
				"hibernate.agroal.jdbcUrl", "jdbc:invalid:must-not-be-used",
				"hibernate.agroal.autoCommit", "true",
				"hibernate.agroal.providerClassName", "org.h2.Driver",
				"hibernate.agroal.maxSize", 2
		) ) );
		try {
			var configuration = provider.unwrap( AgroalDataSource.class ).getConfiguration().connectionPoolConfiguration();
			assertThat( configuration.maxSize() ).isEqualTo( 2 );
			try ( var connection = provider.getConnection() ) {
				assertThat( connection.getMetaData().getURL() ).isEqualTo( url );
				assertThat( connection.getAutoCommit() ).isFalse();
			}
		}
		finally {
			provider.stop();
		}
	}
}
