package org.hibernate.test.annotations.fetchprofile;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.test.annotations.fetchprofile.mappedby.Address;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

@TestForIssue( jiraKey = "HHH-14071" )
public class MappedByFetchProfileUnitTest extends BaseUnitTestCase {

	private ServiceRegistry serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@After
	public void tearDown() {
		if (serviceRegistry != null) ServiceRegistryBuilder.destroy(serviceRegistry);
	}

	@Test
	public void testFetchProfileConfigured() {
		Configuration config = new Configuration();
		config.addAnnotatedClass( Customer6.class );
		config.addAnnotatedClass( Address.class );
		try (SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) config.buildSessionFactory(
				serviceRegistry
		)) {

			assertTrue(
					"fetch profile not parsed properly",
					sessionImpl.containsFetchProfileDefinition( "address-with-customer" )
			);
			assertTrue(
					"fetch profile not parsed properly",
					sessionImpl.containsFetchProfileDefinition( "customer-with-address" )
			);
		}
	}

	@Test
	public void testPackageConfiguredFetchProfile() {
		Configuration config = new Configuration();
		config.addAnnotatedClass( Customer6.class );
		config.addAnnotatedClass( Address.class );
		config.addPackage( Address.class.getPackage().getName() );
		try (SessionFactoryImplementor sessionImpl = ( SessionFactoryImplementor ) config.buildSessionFactory(
				serviceRegistry
		)) {

			assertTrue(
					"fetch profile not parsed properly",
					sessionImpl.containsFetchProfileDefinition( "mappedBy-package-profile-1" )
			);
			assertTrue(
					"fetch profile not parsed properly",
					sessionImpl.containsFetchProfileDefinition( "mappedBy-package-profile-2" )
			);
		}
	}

}
