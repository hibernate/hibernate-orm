package org.hibernate.test.mapping.usertypes;


import java.util.Arrays;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestMethod;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * Test for read-order independent resolution of user-defined types
 * Testcase for issue HHH-7300
 *
 * @author Stefan Schulze
 */
@TestForIssue(jiraKey = "HHH-7300")
public class UserTypeMappingTest extends BaseCoreFunctionalTestMethod {

	@Test
	public void testFirstTypeThenEntity() {
		assertMappings(
				"org/hibernate/test/mapping/usertypes/TestEnumType.hbm.xml",
				"org/hibernate/test/mapping/usertypes/TestEntity.hbm.xml"
		);
	}

	@Test
	public void testFirstEntityThenType() {
		assertMappings(
				"org/hibernate/test/mapping/usertypes/TestEntity.hbm.xml",
				"org/hibernate/test/mapping/usertypes/TestEnumType.hbm.xml"
		);
	}

	private void assertMappings(String... mappings) {

		Properties p = new Properties();
		p.put( Environment.DIALECT, "org.hibernate.dialect.HSQLDialect" );
		p.put( "hibernate.connection.driver_class", "org.h2.Driver" );
		p.put( "hibernate.connection.url", "jdbc:h2:mem:" );
		p.put( "hibernate.connection.username", "sa" );
		p.put( "hibernate.connection.password", "" );
		getTestConfiguration().getProperties().putAll( p );
		getTestConfiguration().getMappings().addAll( Arrays.asList(mappings) );
			Assert.assertNotNull( getSessionFactoryHelper().getSessionFactory() );
	}

}
