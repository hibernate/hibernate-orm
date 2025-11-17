/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.stat;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.Cacheable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
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

import java.util.List;

import static org.hibernate.cfg.CacheSettings.CACHE_REGION_FACTORY;
import static org.hibernate.cfg.CacheSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.cfg.PersistenceSettings.SESSION_FACTORY_NAME_IS_JNDI;
import static org.hibernate.cfg.StatisticsSettings.GENERATE_STATISTICS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Erin Schnabel
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(
		settings = {
				@Setting( name = USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = USE_QUERY_CACHE, value = "true" ),
				@Setting( name = GENERATE_STATISTICS, value = "true" ),
				@Setting( name = SESSION_FACTORY_NAME_IS_JNDI, value = "false" ),
		},
		settingProviders = @SettingProvider( settingName = CACHE_REGION_FACTORY,
				provider = CachingRegionFactory.SettingProvider.class )
)
@DomainModel(annotatedClasses = {
		MicrometerCacheStatisticsTest.Person.class,
		Account.class,
		AccountId.class
})
@SessionFactory( sessionFactoryName = "something" )
public class MicrometerCacheStatisticsTest {

	private static final String REGION = "TheRegion";

	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

	@BeforeEach
	public void setUpMetrics(SessionFactoryScope factoryScope) {
		var sessionFactory = factoryScope.getSessionFactory();
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

		assertNotNull(registry.get("hibernate.second.level.cache.requests").tags("result", "hit", "region", REGION));
		assertNotNull(registry.get("hibernate.second.level.cache.requests").tags("result", "miss", "region", REGION));
		assertNotNull(registry.get("hibernate.second.level.cache.puts").tags("region", REGION).functionCounter());

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
			Person person = new Person( 1, "testAcct");
			session.persist( person );
		} );

		assertEquals( 1, registry.get("hibernate.sessions.open").functionCounter().count(), 0 );
		assertEquals( 1, registry.get("hibernate.sessions.closed").functionCounter().count(), 0 );
		assertEquals( 1, registry.get("hibernate.entities.inserts").functionCounter().count(), 0 );
		assertEquals( 1, registry.get("hibernate.transactions").tags("result", "success").functionCounter().count(), 0 );
		assertEquals( 1, registry.get("hibernate.cache.natural.id.puts").functionCounter().count(), 0);
		assertEquals(2, registry.get("hibernate.second.level.cache.puts").tags("region", REGION).functionCounter().count(), 0);

		factoryScope.inTransaction( (session) -> {
			final String queryString = "select p from Person p";
			// Only way to generate query region (to be accessible via stats) is to execute the query
			session.createQuery( queryString ).setCacheable( true ).setCacheRegion( REGION ).list();
		} );

		assertEquals( 2, registry.get("hibernate.sessions.open").functionCounter().count(), 0 );
		assertEquals( 2, registry.get("hibernate.sessions.closed").functionCounter().count(), 0 );
		assertEquals( 0, registry.get("hibernate.entities.deletes").functionCounter().count(), 0 );
		assertEquals( 2, registry.get("hibernate.transactions").tags("result", "success").functionCounter().count(), 0 );
		assertEquals( 1, registry.get("hibernate.cache.natural.id.puts").functionCounter().count(), 0);
		assertEquals(3, registry.get("hibernate.second.level.cache.puts").tags("region", REGION).functionCounter().count(), 0);

		// clean up
		factoryScope.inTransaction( (session) -> {
			session.remove( session.find( Person.class, 1 ) );
		});

		assertEquals( 3, registry.get("hibernate.sessions.open").functionCounter().count(), 0 );
		assertEquals( 3, registry.get("hibernate.sessions.closed").functionCounter().count(), 0 );
		assertEquals( 1, registry.get("hibernate.entities.deletes").functionCounter().count(), 0 );
		assertEquals( 3, registry.get("hibernate.transactions").tags("result", "success").functionCounter().count(), 0 );
	}

	@Entity( name = "Person" )
	@Table( name = "persons" )
	@Cacheable
	@Cache( region = REGION, usage = CacheConcurrencyStrategy.READ_WRITE )
	@NaturalIdCache( region = REGION )
	public static class Person {
		@Id
		public Integer id;

		@NaturalId
		public String name;

		protected Person() {
		}

		public Person(int id, String name) {
			this.id = id;
			this.name = name;
		}

		@ElementCollection
		@Cache( region = REGION, usage = CacheConcurrencyStrategy.READ_WRITE )
		public List<String> nickNames;
	}
}
