/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.configuration;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Fail.fail;

/**
 * @author Emmanuel Bernard
 */
@BaseUnitTest
public class ConfigurationTest  {
	@Test
	public void testMixPackageAndResourceOrdering() {
		try (BootstrapServiceRegistry serviceRegistry = new BootstrapServiceRegistryBuilder().build()) {
			Configuration config = new Configuration( serviceRegistry );
			config.addResource( "org/hibernate/orm/test/annotations/configuration/orm.xml" );
			config.addPackage( "org.hibernate.orm/test.annotations.configuration" );
		}
		catch( Exception e ) {
			fail( "Processing package first when ORM.xml is used should not fail" );
		}
	}
}
