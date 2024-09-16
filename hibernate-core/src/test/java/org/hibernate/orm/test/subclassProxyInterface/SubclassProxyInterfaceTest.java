/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subclassProxyInterface;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class SubclassProxyInterfaceTest {
	@Test
	public void testSubclassProxyInterfaces() {
		final Configuration cfg = new Configuration()
				.setProperty( Environment.DIALECT, H2Dialect.class )
				.addClass( Person.class );
		try (ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() ) ) {
			cfg.buildSessionFactory( serviceRegistry ).close();
		}
	}
}
