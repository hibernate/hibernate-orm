package org.hibernate.spatial.cfg;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HSConfigurationTest {

	@Test
	public void testConfigureFailure() {
		HSConfiguration config = new HSConfiguration();
		config.configure( "non-existing-file" );
	}

	@Test
	public void testConfigureFile() {
		HSConfiguration config = new HSConfiguration();
		config.configure( "test.cfg.xml" );
		testResults( config );
	}


	private void testResults(HSConfiguration config) {
		assertEquals(
				"org.hibernate.spatial.postgis.PostgisDialect", config
				.getDefaultDialect()
		);
		assertEquals( "FIXED", config.getPrecisionModel() );
		assertEquals( "5", config.getPrecisionModelScale() );
	}

}
