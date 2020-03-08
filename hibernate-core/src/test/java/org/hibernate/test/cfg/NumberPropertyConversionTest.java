package org.hibernate.test.cfg;

import java.util.Collections;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.boot.ServiceRegistryTestingImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-13893")
public class NumberPropertyConversionTest extends BaseUnitTestCase {

	@Test
	public void testIntegerToLongConversion() {
		ServiceRegistry sr = ServiceRegistryTestingImpl.forUnitTesting(
				Collections.singletonMap( AvailableSettings.LOG_SLOW_QUERY, Integer.valueOf( 25 ) )
		);
		sr.getService( JdbcServices.class );
		StandardServiceRegistryBuilder.destroy( sr );
	}
}
