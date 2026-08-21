/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.service.javaservice;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.internal.StatisticsImpl;
import org.hibernate.stat.spi.StatisticsFactory;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a {@link StatisticsFactory} supplied via the Java service loader
 * mechanism is picked up by {@code StatisticsInitiator} when no explicit
 * {@value org.hibernate.cfg.StatisticsSettings#STATS_BUILDER} setting is configured.
 */
@JiraKey("HHH-18938")
@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(
				role = StatisticsFactory.class,
				impl = StatisticsFactoryServiceLoaderTest.CustomStatisticsFactory.class
		)
)
@DomainModel
@SessionFactory
public class StatisticsFactoryServiceLoaderTest {

	@Test
	void testServiceLoaderDiscovery(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		assertThat( statistics ).isInstanceOf( MarkerStatistics.class );
	}

	public static class CustomStatisticsFactory implements StatisticsFactory {
		@Override
		public StatisticsImplementor buildStatistics(SessionFactoryImplementor sessionFactory) {
			return new MarkerStatistics( sessionFactory );
		}
	}

	public static class MarkerStatistics extends StatisticsImpl {
		public MarkerStatistics(SessionFactoryImplementor sessionFactory) {
			super( sessionFactory );
		}
	}
}
