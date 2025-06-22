/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.service.internal;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class ServiceRegistryClosingCascadeTest extends BaseUnitTestCase {
	@Test
	public void testSessionFactoryClosing() {
		BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
		StandardServiceRegistry sr = ServiceRegistryUtil.serviceRegistryBuilder( bsr ).build();
		assertTrue( ( (BootstrapServiceRegistryImpl) bsr ).isActive() );
		Configuration config = new Configuration();
		try (SessionFactory sf = config.buildSessionFactory( sr )) {

		}
		assertFalse( ( (BootstrapServiceRegistryImpl) bsr ).isActive() );
	}
}
