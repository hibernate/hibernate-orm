package org.hibernate.test.multitenancy;

import org.junit.Test;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.TestForIssue;
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
}
