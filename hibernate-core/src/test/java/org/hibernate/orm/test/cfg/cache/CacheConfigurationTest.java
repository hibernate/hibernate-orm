/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cfg.cache;

import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

/**
 * Tests using of cacheable configuration files.
 *
 * @author Tair Sabirgaliev
 */
public class CacheConfigurationTest extends BaseUnitTestCase {
	public static final String CFG_XML = "org/hibernate/orm/test/cfg/cache/hibernate.cfg.xml";

	@Test
	public void testCacheConfiguration() throws Exception {
		// we only care if the SF builds successfully.
		Configuration cfg = new Configuration().configure(CFG_XML);
		ServiceRegistryUtil.applySettings( cfg.getStandardServiceRegistryBuilder() );
		cfg.buildSessionFactory().close();
	}
}
