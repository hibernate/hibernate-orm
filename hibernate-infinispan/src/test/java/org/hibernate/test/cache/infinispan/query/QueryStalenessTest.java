package org.hibernate.test.cache.infinispan.query;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cache.infinispan.timestamp.ClusteredTimestampsRegionImpl;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.CacheImplementor;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.test.cache.infinispan.functional.entities.Person;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.hibernate.test.cache.infinispan.util.TestTimeService;
import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.CustomRunner;
import org.infinispan.AdvancedCache;
import org.infinispan.remoting.transport.Address;
import org.infinispan.test.fwk.TestResourceTracker;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CustomRunner.class)
public class QueryStalenessTest {
	TestTimeService timeService = new TestTimeService();
	SessionFactory sf1, sf2;

	@BeforeClassOnce
	public void init() {
		TestResourceTracker.testStarted(getClass().getSimpleName());
		sf1 = createSessionFactory();
		sf2 = createSessionFactory();
	}

	@AfterClassOnce
	public void destroy() {
		sf1.close();
		sf2.close();
		TestResourceTracker.testFinished(getClass().getSimpleName());
	}

	public SessionFactory createSessionFactory() {
		Configuration configuration = new Configuration()
				.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true")
				.setProperty(Environment.USE_QUERY_CACHE, "true")
				.setProperty(Environment.CACHE_REGION_FACTORY, TestInfinispanRegionFactory.class.getName())
				.setProperty(Environment.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "transactional")
				.setProperty(Environment.ALLOW_UPDATE_OUTSIDE_TRANSACTION, "true")
				.setProperty(AvailableSettings.SHARED_CACHE_MODE, "ALL")
				.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
		Properties testProperties = new Properties();
		testProperties.put(TestInfinispanRegionFactory.TIME_SERVICE, timeService);
		configuration.addProperties(testProperties);
		configuration.addAnnotatedClass(Person.class);
		return configuration.buildSessionFactory();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10677")
	public void testLocalQueryInvalidatedImmediately() {
		Person person = new Person("John", "Smith", 29);
		
		try (Session s1 = sf1.openSession()) {
			s1.persist(person);
			s1.flush();
		}

		TimestampsRegion timestampsRegion = ((CacheImplementor) sf1.getCache()).getUpdateTimestampsCache().getRegion();
		AdvancedCache timestampsCache = ((ClusteredTimestampsRegionImpl) timestampsRegion).getCache();
		Address primaryOwner = timestampsCache.getDistributionManager().getPrimaryLocation(Person.class.getSimpleName());
		SessionFactory qsf = primaryOwner.equals(timestampsCache.getCacheManager().getAddress()) ? sf2 : sf1;

		// The initial insert invalidates the queries for 60s to the future
		timeService.advance(timestampsRegion.getTimeout() + 1);

		try (Session s2 = qsf.openSession()) {
			List<Person> list1 = s2.createQuery("from Person where age <= 29").setCacheable(true).list();
			assertEquals(1, list1.size());
		}

		try (Session s3 = qsf.openSession()) {
			Person p2 = s3.load(Person.class, person.getName());
			p2.setAge(30);
			s3.persist(p2);
			s3.flush();
			List<Person> list2 = s3.createQuery("from Person where age <= 29").setCacheable(true).list();
			assertEquals(0, list2.size());
		}
	}
}
