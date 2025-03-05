/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.cfgXml;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.config.ConfigurationException;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class CfgXmlParsingTest extends BaseUnitTestCase {
	@Test
	public void testCfgXmlWithSchemaLocation() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.configure( "org/hibernate/orm/test/boot/cfgXml/hibernate.cfg.xml" )
				.build();
		try {
			final ConfigurationService cs = ssr.getService( ConfigurationService.class );
			// augmented form
			assertNotNull( cs.getSettings().get( "hibernate.cache.provider_class" ) );
			// original form
			assertNotNull( cs.getSettings().get( "cache.provider_class" ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test(expected = ConfigurationException.class )
	public void testCfgXmlWithBadNamespaceAndSchemaLocation() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.configure( "org/hibernate/orm/test/boot/cfgXml/badnamespace.cfg.xml" )
				.build();
		StandardServiceRegistryBuilder.destroy( ssr );
		fail( "Expecting the bad namespace to fail" );
	}
}
