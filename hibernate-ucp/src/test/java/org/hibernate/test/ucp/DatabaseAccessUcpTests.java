package org.hibernate.test.ucp;

import java.util.Properties;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.hibernate.ucp.internal.UCPConnectionProvider;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Checks the documented UCP driver-property representation without an Oracle server.
///
/// @author Steve Ebersole
@BaseUnitTest
public class DatabaseAccessUcpTests {
	@Test
	void driverProperties() throws Exception {
		var provider = new UCPConnectionProvider();
		// tag::database-ucp-properties[]
		String connectionProperties = "{oracle.jdbc.implicitStatementCacheSize=50, defaultRowPrefetch=20}";
		// end::database-ucp-properties[]
		var settings = new Properties();
		settings.setProperty( "connectionProperties", connectionProperties );
		PoolDataSource dataSource = PoolDataSourceFactory.getPoolDataSource();
		// Exercise Hibernate's configuration path without starting a pool or contacting Oracle.
		var configure = UCPConnectionProvider.class.getDeclaredMethod(
				"configureDataSource", PoolDataSource.class, Properties.class );
		configure.setAccessible( true );
		configure.invoke( provider, dataSource, settings );
		Properties properties = dataSource.getConnectionProperties();
		assertThat( properties.getProperty( "oracle.jdbc.implicitStatementCacheSize" ) ).isEqualTo( "50" );
		assertThat( properties.getProperty( "defaultRowPrefetch" ) ).isEqualTo( "20" );
	}
}
