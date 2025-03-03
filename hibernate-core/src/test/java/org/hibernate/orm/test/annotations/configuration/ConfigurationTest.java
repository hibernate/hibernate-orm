/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.configuration;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class ConfigurationTest  {
	@Test
	public void testMixPackageAndResourceOrdering() throws Exception {
		try (BootstrapServiceRegistry serviceRegistry = new BootstrapServiceRegistryBuilder().build()) {
			Configuration config = new Configuration( serviceRegistry );
			config.addResource( "org/hibernate/orm/test/annotations/configuration/orm.xml" );
			config.addPackage( "org.hibernate.orm/test.annotations.configuration" );
		}
		catch( Exception e ) {
			Assert.fail( "Processing package first when ORM.xml is used should not fail" );
		}
	}
}
