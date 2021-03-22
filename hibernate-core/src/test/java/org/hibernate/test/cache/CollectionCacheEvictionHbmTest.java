/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Petr Abrahamczik
 */
@TestForIssue(jiraKey = "toBe")
public class CollectionCacheEvictionHbmTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Person.class, Phone.class };
	}

	// If you use *.hbm.xml mappings, instead of annotations, add the mappings here.
	@Override
	protected String[] getMappings() {
		return new String[]{
				"CollectionCacheEvictionHbmTest.hbm.xml"
		};
	}

	// If those mappings reside somewhere other than resources/org/hibernate/test, change this.
	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/test/cache/";
	}

	@Before
	public void before() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = true;
	}

	@After
	public void after() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = false;
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.AUTO_EVICT_COLLECTION_CACHE, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_PROVIDER_CONFIG, "true" );
	}

	@Override
	protected void prepareTest() throws Exception {
		Session s = openSession();
		s.beginTransaction();

		Person person1 = new Person();
		person1.setId( 1 );
		s.save( person1 );

		Phone phone1 = new Phone();
		phone1.setId( 1 );
		phone1.setPerson( person1 );
		s.save( phone1 );

		Person person2 = new Person();
		person2.setId( 2 );
		s.save( person2 );

		Phone phone2 = new Phone();
		phone2.setId( 2 );
		phone2.setPerson( person2 );
		s.save( phone2 );

		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected void cleanupTest() throws Exception {
		Session s = openSession();
		s.beginTransaction();

		s.createQuery( "delete from " + Phone.class.getName() ).executeUpdate();
		s.createQuery( "delete from " + Person.class.getName() ).executeUpdate();

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testCachedValueFromSameRegionAfterEviction() {
		CollectionPersister persister = sessionFactory().getCollectionPersister( Person.class.getName() + ".phones" );

		Session s = openSession();
		s.beginTransaction();
		SessionImplementor sessionImplementor = (SessionImplementor) s;

		// test if person1's phones is not cached
		CollectionRegionAccessStrategy cache = persister.getCacheAccessStrategy();
		Object key1 = cache.generateCacheKey( 1, persister, sessionFactory(), s.getTenantIdentifier() );
		Object cachedValue1 = cache.get( sessionImplementor, key1, sessionImplementor.getTimestamp() );
		assertNull( cachedValue1 );

		// test if person2's phones is not cached
		Object key2 = cache.generateCacheKey( 2, persister, sessionFactory(), s.getTenantIdentifier() );
		Object cachedValue2 = cache.get( sessionImplementor, key2, sessionImplementor.getTimestamp() );
		assertNull( cachedValue2 );

		Person person1 = s.get( Person.class, 1 );
		// should add in cache
		assertEquals( 1, person1.getPhones().size() );

		Person person2 = s.get( Person.class, 2 );
		// should add in cache
		assertEquals( 1, person2.getPhones().size() );

		// add new phone to person1
		Phone phone3 = new Phone();
		phone3.setId( 3 );
		phone3.setPerson( person1 );
		s.save( phone3 );

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		// test if person1 has 2 phones
		person1 = s.get( Person.class, 1 );
		assertEquals( 2, person1.getPhones().size() );

		// test if person2's phones not evicted
		sessionImplementor = (SessionImplementor) s;
		key2 = cache.generateCacheKey( 2, persister, sessionFactory(), s.getTenantIdentifier() );
		cachedValue2 = cache.get( sessionImplementor, key2, sessionImplementor.getTimestamp() );
		assertNotNull( "Collection from person2 wasn't cached. Invalidated entire region.", cachedValue2 );

		s.getTransaction().commit();
		s.close();
	}

	public static class Person {

		private int id;

		private int version;

		private Set<Phone> phones = new HashSet<Phone>();

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}

		public Set<Phone> getPhones() {
			return phones;
		}

		public void setPhones(Set<Phone> phones) {
			this.phones = phones;
		}

	}

	public static class Phone {

		private int id;

		private int version;

		private Person person;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

	}
}
