/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import org.hibernate.PessimisticLockException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Zhenlei Huang
 */
@TestForIssue(jiraKey = "HHH-10649")
public class RefreshUpdatedDataTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CacheableItem.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_PROVIDER_CONFIG, "true" );
	}

	@Test
	public void testUpdateAndFlushThenRefresh() {
		// prepare data
		Session s = openSession();
		s.beginTransaction();
		CacheableItem item = new CacheableItem( "data" );
		s.save( item );
		s.getTransaction().commit();
		s.close();

		Session s1 = openSession();
		s1.beginTransaction();

		CacheableItem item1 = s1.get( CacheableItem.class, item.getId() ); // into persistent context
		item1.setName( "some name" );

		s1.flush();
		s1.refresh( item1 );

		item1 = s1.get( CacheableItem.class, item.getId() );
		assertEquals( "some name", item1.getName() );

		// open another session
		Session s2 = sessionFactory().openSession();
		try {
			s2.beginTransaction();
			CacheableItem item2 = s2.get( CacheableItem.class, item.getId() );

			assertEquals( "data", item2.getName() );

			s2.getTransaction().commit();
		} catch (PessimisticLockException expected) {
			// expected if mvcc is not used
		} catch (Exception e) {
			throw e;
		} finally {
			if ( s2.getTransaction().getStatus().canRollback() ) {
				s2.getTransaction().rollback();
			}
			s2.close();
		}

		s1.getTransaction().rollback();
		s1.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete CacheableItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
}
