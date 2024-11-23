/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.stat;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.stat.HibernateMetrics;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.junit.Assert.assertEquals;

/**
 *  @author Erin Schnabel
 *  @author Donnchadh O Donnabhain
 */
public class MicrometerStatisticsTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Account.class, AccountId.class };
	}

	private SimpleMeterRegistry registry = new SimpleMeterRegistry();
	private HibernateMetrics hibernateMetrics;

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );

		configuration.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		configuration.setProperty( Environment.USE_QUERY_CACHE, "false" );
		configuration.setProperty( Environment.GENERATE_STATISTICS, "true" );
		configuration.setProperty( Environment.SESSION_FACTORY_NAME, "something" );
		configuration.setProperty( Environment.SESSION_FACTORY_NAME_IS_JNDI, "false" );
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

		// Second level cache disabled
		verifyMeterNotFoundException("hibernate.second.level.cache.requests");
		verifyMeterNotFoundException("hibernate.second.level.cache.puts");

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
		Account account = new Account( new AccountId( 1), "testAcct");
		session.save( account );
		session.getTransaction().commit();
		session.close();

		Assert.assertEquals( 1, registry.get("hibernate.sessions.open").functionCounter().count(), 0 );
		Assert.assertEquals( 1, registry.get("hibernate.sessions.closed").functionCounter().count(), 0 );
		Assert.assertEquals( 1, registry.get("hibernate.entities.inserts").functionCounter().count(), 0 );
		Assert.assertEquals( 1, registry.get("hibernate.transactions").tags("result", "success").functionCounter().count(), 0 );

		// clean up
		session = openSession();
		session.beginTransaction();
		session.delete( account );
		session.getTransaction().commit();
		session.close();

		Assert.assertEquals( 2, registry.get("hibernate.sessions.open").functionCounter().count(), 0 );
		Assert.assertEquals( 2, registry.get("hibernate.sessions.closed").functionCounter().count(), 0 );
		Assert.assertEquals( 1, registry.get("hibernate.entities.deletes").functionCounter().count(), 0 );
		Assert.assertEquals( 2, registry.get("hibernate.transactions").tags("result", "success").functionCounter().count(), 0 );
	}

	void verifyMeterNotFoundException(String name) {
		try {
			registry.get(name).meter();
			Assert.fail(name + " should not have been found");
		} catch(MeterNotFoundException mnfe) {
		}
	}
}
