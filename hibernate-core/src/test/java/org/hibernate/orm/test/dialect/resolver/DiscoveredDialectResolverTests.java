/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.resolver;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry.JavaService;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Tests discovery of DialectResolvers via Java's {@link java.util.ServiceLoader}
 */
@BootstrapServiceRegistry(
		javaServices = @JavaService(
				role = DialectResolver.class,
				impl = DiscoveredDialectResolverTests.CustomDialectResolverImpl.class
		)
)
@ServiceRegistry(
		// force discovery
		settings = @Setting( name = AvailableSettings.DIALECT, value = "" )
)
@DomainModel
@SessionFactory
@RequiresDialect(H2Dialect.class)
public class DiscoveredDialectResolverTests {
	@Test
	public void testRegistration(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
		assertThat( dialect, instanceOf( CustomDialect.class ) );
	}

	public static class CustomDialectResolverImpl implements DialectResolver {
		public CustomDialectResolverImpl() {
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			return new CustomDialect();
		}
	}

	public static class CustomDialect extends H2Dialect {
	}
}
