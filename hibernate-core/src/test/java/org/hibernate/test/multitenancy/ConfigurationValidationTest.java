package org.hibernate.test.multitenancy;

import org.junit.Test;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7311")
public class ConfigurationValidationTest extends BaseUnitTestCase {
	@Test(expected = ServiceException.class)
	public void testInvalidConnectionProvider() {
		Configuration cfg = new Configuration();
		cfg.getProperties().put( Environment.MULTI_TENANT, MultiTenancyStrategy.SCHEMA );
		cfg.setProperty( Environment.MULTI_TENANT_CONNECTION_PROVIDER, "class.not.present.in.classpath" );
		cfg.buildMappings();
		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) new ServiceRegistryBuilder()
				.applySettings( cfg.getProperties() ).buildServiceRegistry();
		cfg.buildSessionFactory( serviceRegistry );
	}

	@Test
	public void testReleaseMode() {
		Configuration cfg = new Configuration();
		cfg.getProperties().put( Environment.MULTI_TENANT, MultiTenancyStrategy.SCHEMA );
		cfg.getProperties().put( Environment.RELEASE_CONNECTIONS, ConnectionReleaseMode.AFTER_STATEMENT.name() );
		cfg.buildMappings();

		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) new ServiceRegistryBuilder()
				.applySettings( cfg.getProperties() )
				.addService(
						MultiTenantConnectionProvider.class,
						new TestingConnectionProvider(
								new TestingConnectionProvider.NamedConnectionProviderPair(
										"acme",
										ConnectionProviderBuilder.buildConnectionProvider( "acme" )
								)
						)
				)
				.buildServiceRegistry();

		cfg.buildSessionFactory( serviceRegistry );
	}
}
