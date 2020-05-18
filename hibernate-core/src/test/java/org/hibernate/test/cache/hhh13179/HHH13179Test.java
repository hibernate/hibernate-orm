/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.hhh13179;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.CacheRegionStatistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * Check that second level caching works for hbm mapped joined subclass inheritance structures
 */
@TestForIssue(jiraKey = "HHH-13179")
public class HHH13179Test extends BaseCoreFunctionalTestCase {

	// Add your entities here.
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				JoinedSubclassPerson.class,
				JoinedSubclassUIPerson.class,
				JoinedSubclassNonUIPerson.class,
				UnionSubclassPerson.class,
				UnionSubclassUIPerson.class,
				UnionSubclassNonUIPerson.class,
				DiscriminatorSubclassPerson.class,
				DiscriminatorSubclassUIPerson.class,
				DiscriminatorSubclassNonUIPerson.class
		};
	}

	// If you use *.hbm.xml mappings, instead of annotations, add the mappings here.
	@Override
	protected String[] getMappings() {
		return new String[] {
				"org/hibernate/test/cache/hhh13179/JoinedSubclassPerson.hbm.xml",
				"org/hibernate/test/cache/hhh13179/UnionSubclassPerson.hbm.xml",
				"org/hibernate/test/cache/hhh13179/DiscriminatorSubclassPerson.hbm.xml"
		};
	}

	// If those mappings reside somewhere other than resources/org/hibernate/test, change this.
	@Override
	protected String getBaseForMappings() {
		return "";
	}

	// Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );

		configuration.setProperty( AvailableSettings.SHOW_SQL, Boolean.TRUE.toString() );
		configuration.setProperty( AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString() );
		configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testJoinedSubclassCaching() {
		// BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		String regionName = "org.hibernate.test.cache.hhh13179.JoinedSubclassPerson";

		// sanity check
		CacheRegionStatistics cacheRegionStatistics = s.getSessionFactory().getStatistics().getCacheRegionStatistics(
				regionName );
		Assert.assertEquals( "Cache put should be 0", 0, cacheRegionStatistics.getPutCount() );

		JoinedSubclassPerson person1 = new JoinedSubclassUIPerson();
		person1.setOid( 1L );
		s.save( person1 );

		tx.commit();

		s.close();

		s = openSession();
		tx = s.beginTransaction();

		JoinedSubclassPerson person2 = s.get( JoinedSubclassPerson.class, 1L );

		cacheRegionStatistics = s.getSessionFactory().getStatistics().getCacheRegionStatistics( regionName );
		Assert.assertEquals( "Cache hit should be 1", 1, cacheRegionStatistics.getHitCount() );
		Assert.assertEquals( "Cache put should be 1", 1, cacheRegionStatistics.getPutCount() );

		tx.commit();
		s.close();
	}

	@Test
	public void testUnionSubclassCaching() {
		// BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		String regionName = "org.hibernate.test.cache.hhh13179.UnionSubclassPerson";

		// sanity check
		CacheRegionStatistics cacheRegionStatistics = s.getSessionFactory().getStatistics().getCacheRegionStatistics(
				regionName );
		Assert.assertEquals( "Cache put should be 0", 0, cacheRegionStatistics.getPutCount() );

		UnionSubclassPerson person1 = new UnionSubclassUIPerson();
		person1.setOid( 1L );
		s.save( person1 );

		tx.commit();

		s.close();

		s = openSession();
		tx = s.beginTransaction();

		UnionSubclassPerson person2 = s.get( UnionSubclassPerson.class, 1L );

		cacheRegionStatistics = s.getSessionFactory().getStatistics().getCacheRegionStatistics( regionName );
		Assert.assertEquals( "Cache hit should be 1", 1, cacheRegionStatistics.getHitCount() );
		Assert.assertEquals( "Cache put should be 1", 1, cacheRegionStatistics.getPutCount() );

		tx.commit();
		s.close();
	}

	@Test
	public void testDiscriminatorSubclassCaching() {
		// BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		String regionName = "org.hibernate.test.cache.hhh13179.DiscriminatorSubclassPerson";

		// sanity check
		CacheRegionStatistics cacheRegionStatistics = s.getSessionFactory().getStatistics().getCacheRegionStatistics(
				regionName );
		Assert.assertEquals( "Cache put should be 0", 0, cacheRegionStatistics.getPutCount() );

		DiscriminatorSubclassPerson person1 = new DiscriminatorSubclassUIPerson();
		person1.setOid( 1L );
		s.save( person1 );

		tx.commit();

		s.close();

		s = openSession();
		tx = s.beginTransaction();

		DiscriminatorSubclassPerson person2 = s.get( DiscriminatorSubclassPerson.class, 1L );

		cacheRegionStatistics = s.getSessionFactory().getStatistics().getCacheRegionStatistics( regionName );
		Assert.assertEquals( "Cache hit should be 1", 1, cacheRegionStatistics.getHitCount() );
		Assert.assertEquals( "Cache put should be 1", 1, cacheRegionStatistics.getPutCount() );

		tx.commit();
		s.close();
	}
}
