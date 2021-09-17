package org.hibernate.test.onetoone.cache;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class OneToOneCacheTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {
            "onetoone/cache/Details.hbm.xml",
            "onetoone/cache/Person.hbm.xml",
        };
    }
    
	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty(AvailableSettings.USE_SECOND_LEVEL_CACHE, "true");
		configuration.setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
    }

	private <TPerson extends Person, TDetails extends Details> void OneToOneTest(Class<TPerson> personClass,
			Class<TDetails> detailsClass) throws Exception {

		// Initialize the database with data.
		List<Serializable> ids = createPersonsAndDetails(personClass, detailsClass);

		// Clear the second level cache and the statistics.
		SessionFactoryImplementor sfi = sessionFactory();
		CacheImplementor cache = sfi.getCache();
		StatisticsImplementor statistics = sfi.getStatistics();

		cache.evictEntityData(personClass);
		cache.evictEntityData(detailsClass);
		cache.evictQueryRegions();

		statistics.clear();

		// Fill the empty caches with data.
		this.getPersons(personClass, ids);

		// Verify that no data was retrieved from the cache.
		assertEquals("Second level cache hit count", 0, statistics.getSecondLevelCacheHitCount());

		statistics.clear();

		this.getPersons(personClass, ids);

		// Verify that all data was retrieved from the cache.
		assertEquals("Second level cache miss count", 0, statistics.getSecondLevelCacheMissCount());
	}

	private <TPerson extends Person, TDetails extends Details> List<Serializable> createPersonsAndDetails(Class<TPerson> personClass,
			Class<TDetails> detailsClass) throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Constructor<TPerson> ctorPerson = personClass.getConstructor();
		Constructor<TDetails> ctorDetails = detailsClass.getConstructor();
		List<Serializable> ids = new ArrayList<Serializable>();

		for (int i = 0; i < 6; i++) {
			Person person = ctorPerson.newInstance();

			if (i % 2 == 0) {
				Details details = ctorDetails.newInstance();

				details.setData(String.format("%s%d", detailsClass.getName(), i));
				person.setDetails(details);
			}

			person.setName(String.format("%s%d", personClass.getName(), i));

			ids.add(s.save(person));
		}

		tx.commit();
		s.close();

		return ids;
	}

	private <TPerson extends Person> List<TPerson> getPersons(Class<TPerson> personClass, List<Serializable> ids) {		
		Session s = openSession();
		Transaction tx  = s.beginTransaction();
		List<TPerson> people = new ArrayList<TPerson>();

		for (Serializable id : ids) {
			people.add(s.get(personClass, id));	
		}
		
		tx.commit();
		s.close();

		return people;
	}

	@Test
	public void OneToOneCacheByForeignKey() throws Exception {
		OneToOneTest(PersonByFK.class, DetailsByFK.class);
	}

	@Test
	public void OneToOneCacheByRef() throws Exception {
		OneToOneTest(PersonByRef.class, DetailsByRef.class);
	}
}
