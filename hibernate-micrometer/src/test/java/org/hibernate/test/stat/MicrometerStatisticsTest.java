/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.stat;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.hibernate.stat.HibernateMetrics;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.CacheSettings.CACHE_REGION_FACTORY;
import static org.hibernate.cfg.CacheSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.cfg.PersistenceSettings.SESSION_FACTORY_NAME_IS_JNDI;
import static org.hibernate.cfg.StatisticsSettings.GENERATE_STATISTICS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 *  @author Erin Schnabel
 *  @author Donnchadh O Donnabhain
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(
		settings = {
				@Setting( name = USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = USE_QUERY_CACHE, value = "false" ),
				@Setting( name = GENERATE_STATISTICS, value = "true" ),
				@Setting( name = SESSION_FACTORY_NAME_IS_JNDI, value = "false" ),
		},
		settingProviders = @SettingProvider( settingName = CACHE_REGION_FACTORY,
				provider = CachingRegionFactory.SettingProvider.class )
)
@DomainModel(annotatedClasses = {Account.class, AccountId.class})
@SessionFactory( sessionFactoryName = "something" )
public class MicrometerStatisticsTest {

	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

	@BeforeEach
	public void setUpMetrics(SessionFactoryScope factoryScope) {
		final var sessionFactory = factoryScope.getSessionFactory();
		var hibernateMetrics = new HibernateMetrics(
				sessionFactory,
				sessionFactory.getName(),
				Tags.empty()
		);
		hibernateMetrics.bindTo( registry );
	}

	@AfterEach
	public void cleanUpMetrics(SessionFactoryScope factoryScope) {
		registry.clear();
		factoryScope.dropData();
	}

	@Test
	public void testMicrometerMetrics(SessionFactoryScope factoryScope) {
		assertNotNull(registry.get("hibernate.sessions.open").functionCounter());
		assertNotNull(registry.get("hibernate.sessions.closed").functionCounter());

		assertNotNull(registry.get("hibernate.transactions").tags("result", "success").functionCounter());
		assertNotNull(registry.get("hibernate.transactions").tags("result", "failure").functionCounter());

		assertNotNull(registry.get("hibernate.optimistic.failures").functionCounter());
		assertNotNull(registry.get("hibernate.flushes").functionCounter());
		assertNotNull(registry.get("hibernate.connections.obtained").functionCounter());

		assertNotNull(registry.get("hibernate.statements").tags("status", "prepared").functionCounter());
		assertNotNull(registry.get("hibernate.statements").tags("status", "closed").functionCounter());

		// Second level cache disabled
		verifyMeterNotFoundException("hibernate.second.level.cache.requests");
		verifyMeterNotFoundException("hibernate.second.level.cache.puts");

		assertNotNull(registry.get("hibernate.entities.deletes").functionCounter());
		assertNotNull(registry.get("hibernate.entities.fetches").functionCounter());
		assertNotNull(registry.get("hibernate.entities.inserts").functionCounter());
		assertNotNull(registry.get("hibernate.entities.loads").functionCounter());
		assertNotNull(registry.get("hibernate.entities.updates").functionCounter());
		assertNotNull(registry.get("hibernate.entities.upserts").functionCounter());

		assertNotNull(registry.get("hibernate.collections.deletes").functionCounter());
		assertNotNull(registry.get("hibernate.collections.fetches").functionCounter());
		assertNotNull(registry.get("hibernate.collections.loads").functionCounter());
		assertNotNull(registry.get("hibernate.collections.recreates").functionCounter());
		assertNotNull(registry.get("hibernate.collections.updates").functionCounter());

		assertNotNull(registry.get("hibernate.cache.natural.id.requests").tags("result", "hit").functionCounter());
		assertNotNull(registry.get("hibernate.cache.natural.id.requests").tags("result", "miss").functionCounter());
		assertNotNull(registry.get("hibernate.cache.natural.id.puts").functionCounter());
		assertNotNull(registry.get("hibernate.query.natural.id.executions").functionCounter());
		assertNotNull(registry.get("hibernate.query.natural.id.executions.max").timeGauge());

		assertNotNull(registry.get("hibernate.query.executions").functionCounter());
		assertNotNull(registry.get("hibernate.query.executions.max").timeGauge());

		assertNotNull(registry.get("hibernate.cache.update.timestamps.requests").tags("result", "hit").functionCounter());
		assertNotNull(registry.get("hibernate.cache.update.timestamps.requests").tags("result", "miss").functionCounter());
		assertNotNull(registry.get("hibernate.cache.update.timestamps.puts").functionCounter());

		assertNotNull(registry.get("hibernate.cache.query.requests").tags("result", "hit").functionCounter());
		assertNotNull(registry.get("hibernate.cache.query.requests").tags("result", "miss").functionCounter());
		assertNotNull(registry.get("hibernate.cache.query.puts").functionCounter());
		assertNotNull(registry.get("hibernate.cache.query.plan").tags("result", "hit").functionCounter());
		assertNotNull(registry.get("hibernate.cache.query.plan").tags("result", "miss").functionCounter());

		// prepare some test data...
		factoryScope.inTransaction( (session) -> {
			Account account = new Account( new AccountId(1), "testAcct");
			session.persist( account );
		} );

		assertEquals( 1, registry.get("hibernate.sessions.open").functionCounter().count(), 0 );
		assertEquals( 1, registry.get("hibernate.sessions.closed").functionCounter().count(), 0 );
		assertEquals( 1, registry.get("hibernate.entities.inserts").functionCounter().count(), 0 );
		assertEquals( 1, registry.get("hibernate.transactions").tags("result", "success").functionCounter().count(), 0 );

		// clean up
		factoryScope.inTransaction( (session) -> {
			session.remove( session.find( Account.class, new AccountId(1) ) );
		} );

		assertEquals( 2, registry.get("hibernate.sessions.open").functionCounter().count(), 0 );
		assertEquals( 2, registry.get("hibernate.sessions.closed").functionCounter().count(), 0 );
		assertEquals( 1, registry.get("hibernate.entities.deletes").functionCounter().count(), 0 );
		assertEquals( 2, registry.get("hibernate.transactions").tags("result", "success").functionCounter().count(), 0 );
	}

	void verifyMeterNotFoundException(String name) {
		MeterNotFoundException ex = assertThrows(
				MeterNotFoundException.class,
				() -> registry.get( name ).meter(), name + " should not have been found"
		);
		assertTrue( ex.getMessage().contains( name ) );

	}
}
