/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.cfgXml;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
public class CfgXmlParsingTest {
	@Test
	public void testCfgXmlWithSchemaLocation() {
		try (var ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.configure( "org/hibernate/orm/test/boot/cfgXml/hibernate.cfg.xml" )
				.build()) {
			final ConfigurationService cs = ssr.getService( ConfigurationService.class );
			// augmented form
			Assertions.assertNotNull( cs.getSettings().get( "hibernate.cache.provider_class" ) );
			// original form
			Assertions.assertNotNull( cs.getSettings().get( "cache.provider_class" ) );
		}
	}

	@Test
	@ExpectedException(ConfigurationException.class)
	public void testCfgXmlWithBadNamespaceAndSchemaLocation() {
		try (var ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.configure( "org/hibernate/orm/test/boot/cfgXml/badnamespace.cfg.xml" )
				.build()) {
			Assertions.fail( "Expecting the bad namespace to fail" );
		}
	}
}
