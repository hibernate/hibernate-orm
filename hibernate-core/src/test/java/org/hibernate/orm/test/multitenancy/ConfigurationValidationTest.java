/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

/**
 * @author Lukasz Antoniak
 */
@JiraKey(value = "HHH-7311")
@RequiresDialect( H2Dialect.class )
public class ConfigurationValidationTest extends BaseUnitTestCase {

	@Test(expected = ServiceException.class)
	public void testInvalidConnectionProvider() {
		ServiceRegistryImplementor serviceRegistry = null;
		try {
			serviceRegistry	= (ServiceRegistryImplementor) ServiceRegistryUtil.serviceRegistryBuilder()
					.applySetting( Environment.MULTI_TENANT_CONNECTION_PROVIDER, "class.not.present.in.classpath" )
					.build();

			new MetadataSources( serviceRegistry ).buildMetadata().buildSessionFactory().close();
		}
		finally {
			if ( serviceRegistry != null ) {
				try {
					StandardServiceRegistryBuilder.destroy( serviceRegistry );
				}
				catch (Exception ignore) {
				}
			}
		}
	}

	@Test
	public void testReleaseMode() {
		ServiceRegistryImplementor serviceRegistry = null;
		try {
			serviceRegistry	= (ServiceRegistryImplementor) ServiceRegistryUtil.serviceRegistryBuilder()
					.applySetting( Environment.CONNECTION_HANDLING, PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT.name() )
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

			new MetadataSources( serviceRegistry ).buildMetadata().buildSessionFactory().close();
		}
		finally {
			if ( serviceRegistry != null ) {
				try {
					StandardServiceRegistryBuilder.destroy( serviceRegistry );
				}
				catch (Exception ignore) {
				}
			}
		}
	}
}
