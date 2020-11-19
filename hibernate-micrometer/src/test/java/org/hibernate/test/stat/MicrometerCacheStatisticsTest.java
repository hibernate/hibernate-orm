/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.stat;

import java.util.List;
import java.util.Map;
import javax.persistence.Cacheable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.HibernateMetrics;

import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * @author Erin Schnabel
 * @author Steve Ebersole
 */
public class MicrometerCacheStatisticsTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Person.class );
		metadataSources.addAnnotatedClass( Account.class );
		metadataSources.addAnnotatedClass( AccountId.class );
	}

	private static final String REGION = "TheRegion";
	private static final String PREFIX = "test";

	private SimpleMeterRegistry registry = new SimpleMeterRegistry();
	private HibernateMetrics hibernateMetrics;

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, true );
		ssrb.applySetting( AvailableSettings.USE_QUERY_CACHE, true );
		ssrb.applySetting( AvailableSettings.CACHE_REGION_FACTORY, new CachingRegionFactory() );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( AvailableSettings.USE_QUERY_CACHE, "true" );
		settings.put( AvailableSettings.CACHE_REGION_FACTORY, new CachingRegionFactory() );

		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		settings.put( AvailableSettings.SESSION_FACTORY_NAME, "something" );
		settings.put( AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, "false" );
	}

	@Before
	public void setUpMetrics() {
		hibernateMetrics = new HibernateMetrics(sessionFactory(),
												sessionFactory().getName(),
												Tags.empty() );
		hibernateMetrics.bindTo( registry );
	}

	@After
	public void cleanUpMetrics() {
		registry.clear();
	}

	@Test
	public void testMicrometerMetrics() {
		Assert.assertNotNull(registry.get("hibernate.sessions.open").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.sessions.closed").functionCounter());

		Assert.assertNotNull(registry.get("hibernate.transactions").tags("result", "success").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.transactions").tags("result", "failure").functionCounter());

		Assert.assertNotNull(registry.get("hibernate.optimistic.failures").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.flushes").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.connections.obtained").functionCounter());

		Assert.assertNotNull(registry.get("hibernate.statements").tags("status", "prepared").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.statements").tags("status", "closed").functionCounter());

		Assert.assertNotNull(registry.get("hibernate.second.level.cache.requests").tags("result", "hit", "region", REGION));
		Assert.assertNotNull(registry.get("hibernate.second.level.cache.requests").tags("result", "miss", "region", REGION));
		Assert.assertNotNull(registry.get("hibernate.second.level.cache.puts").tags("region", REGION).functionCounter());

		Assert.assertNotNull(registry.get("hibernate.entities.deletes").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.entities.fetches").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.entities.inserts").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.entities.loads").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.entities.updates").functionCounter());

		Assert.assertNotNull(registry.get("hibernate.collections.deletes").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.collections.fetches").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.collections.loads").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.collections.recreates").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.collections.updates").functionCounter());

		Assert.assertNotNull(registry.get("hibernate.cache.natural.id.requests").tags("result", "hit").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.cache.natural.id.requests").tags("result", "miss").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.cache.natural.id.puts").functionCounter());

		Assert.assertNotNull(registry.get("hibernate.query.natural.id.executions").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.query.natural.id.executions.max").timeGauge());

		Assert.assertNotNull(registry.get("hibernate.query.executions").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.query.executions.max").timeGauge());

		Assert.assertNotNull(registry.get("hibernate.cache.update.timestamps.requests").tags("result", "hit").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.cache.update.timestamps.requests").tags("result", "miss").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.cache.update.timestamps.puts").functionCounter());

		Assert.assertNotNull(registry.get("hibernate.cache.query.requests").tags("result", "hit").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.cache.query.requests").tags("result", "miss").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.cache.query.puts").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.cache.query.plan").tags("result", "hit").functionCounter());
		Assert.assertNotNull(registry.get("hibernate.cache.query.plan").tags("result", "miss").functionCounter());

		// prepare some test data...
		Session session = openSession();
		session.beginTransaction();
		Person person = new Person( 1, "testAcct");
		session.save( person );
		session.getTransaction().commit();
		session.close();

		Assert.assertEquals( 1, registry.get("hibernate.sessions.open").functionCounter().count(), 0 );
		Assert.assertEquals( 1, registry.get("hibernate.sessions.closed").functionCounter().count(), 0 );
		Assert.assertEquals( 1, registry.get("hibernate.entities.inserts").functionCounter().count(), 0 );
		Assert.assertEquals( 1, registry.get("hibernate.transactions").tags("result", "success").functionCounter().count(), 0 );
		Assert.assertEquals( 1, registry.get("hibernate.cache.natural.id.puts").functionCounter().count(), 0);
		Assert.assertEquals(2, registry.get("hibernate.second.level.cache.puts").tags("region", REGION).functionCounter().count(), 0);

		final String queryString = "select p from Person p";
		inTransaction(
				// Only way to generate query region (to be accessible via stats) is to execute the query
				s -> s.createQuery( queryString ).setCacheable( true ).setCacheRegion( REGION ).list()
		);

		Assert.assertEquals( 2, registry.get("hibernate.sessions.open").functionCounter().count(), 0 );
		Assert.assertEquals( 2, registry.get("hibernate.sessions.closed").functionCounter().count(), 0 );
		Assert.assertEquals( 0, registry.get("hibernate.entities.deletes").functionCounter().count(), 0 );
		Assert.assertEquals( 2, registry.get("hibernate.transactions").tags("result", "success").functionCounter().count(), 0 );
		Assert.assertEquals( 1, registry.get("hibernate.cache.natural.id.puts").functionCounter().count(), 0);
		Assert.assertEquals(3, registry.get("hibernate.second.level.cache.puts").tags("region", REGION).functionCounter().count(), 0);

		// clean up
		session = openSession();
		session.beginTransaction();
		session.delete( person );
		session.getTransaction().commit();
		session.close();

		Assert.assertEquals( 3, registry.get("hibernate.sessions.open").functionCounter().count(), 0 );
		Assert.assertEquals( 3, registry.get("hibernate.sessions.closed").functionCounter().count(), 0 );
		Assert.assertEquals( 1, registry.get("hibernate.entities.deletes").functionCounter().count(), 0 );
		Assert.assertEquals( 3, registry.get("hibernate.transactions").tags("result", "success").functionCounter().count(), 0 );
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
