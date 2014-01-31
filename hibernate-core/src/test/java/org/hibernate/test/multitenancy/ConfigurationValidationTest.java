package org.hibernate.test.multitenancy;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7311")
public class ConfigurationValidationTest extends BaseUnitTestCase {
	@Test
	public void testReleaseMode() {
		// todo : this really does not test anythign.  The service is not used here, and so not initialized
		Configuration cfg = new Configuration();
		cfg.getProperties().put( Environment.MULTI_TENANT, MultiTenancyStrategy.SCHEMA );
		cfg.getProperties().put( Environment.RELEASE_CONNECTIONS, ConnectionReleaseMode.AFTER_STATEMENT.name() );
		cfg.buildMappings();

		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) new StandardServiceRegistryBuilder()
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
				.build();

		cfg.buildSessionFactory( serviceRegistry ).close();
		serviceRegistry.destroy();
	}
}
