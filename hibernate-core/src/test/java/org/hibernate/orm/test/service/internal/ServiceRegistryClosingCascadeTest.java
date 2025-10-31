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
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class ServiceRegistryClosingCascadeTest {
	@Test
	public void testSessionFactoryClosing() {
		BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
		StandardServiceRegistry sr = ServiceRegistryUtil.serviceRegistryBuilder( bsr ).build();
		assertThat( ((BootstrapServiceRegistryImpl) bsr).isActive() ).isTrue();
		Configuration config = new Configuration();
		try (SessionFactory sf = config.buildSessionFactory( sr )) {

		}
		assertThat( ((BootstrapServiceRegistryImpl) bsr).isActive() ).isFalse();
	}
}
