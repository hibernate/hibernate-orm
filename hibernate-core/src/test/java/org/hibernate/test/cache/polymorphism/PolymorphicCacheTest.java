/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.polymorphism;

import org.hibernate.Session;
import org.hibernate.WrongClassException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Guillaume Smet
 * @author Brett Meyer
 */
@TestForIssue(jiraKey = "HHH-9028")
public class PolymorphicCacheTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { AbstractCachedItem.class, CachedItem1.class, CachedItem2.class };
	}

	@Test
	public void testPolymorphismAndCache() throws Exception {
		final CachedItem1 item1 = new CachedItem1( "name 1" );
		final CachedItem2 item2 = new CachedItem2( "name 2" );

		// create the 2 items
		Session s = openSession();
		s.beginTransaction();
		s.save( item1 );
		s.save( item2 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		// See HHH-9107
		try {
			final CachedItem2 tmp = s.get( CachedItem2.class, item1.getId() );
			fail( "Expected a WrongClassException to be thrown." );
		}
		catch (WrongClassException e) {
			//expected
		}
		s.getTransaction().commit();
		s.close();
		
		// test updating
		s = openSession();
		s.beginTransaction();
		item1.setName( "updated" );
		s.update( item1 );
		s.getTransaction().commit();
		s.clear();
		s.beginTransaction();
		CachedItem1 cachedItem1 = (CachedItem1) s.get( CachedItem1.class, item1.getId() );
		CachedItem2 cachedItem2 = (CachedItem2) s.get( CachedItem2.class, item2.getId() );
		assertEquals( "updated", cachedItem1.getName() );
		assertEquals( item2.getName(), cachedItem2.getName() );
		s.getTransaction().commit();
		s.close();
		
		// test deleting
		s = openSession();
		s.beginTransaction();
		s.delete( item1 );
		s.getTransaction().commit();
		s.clear();
		s.beginTransaction();
		cachedItem1 = (CachedItem1) s.get( CachedItem1.class, item1.getId() );
		cachedItem2 = (CachedItem2) s.get( CachedItem2.class, item2.getId() );
		assertNull( cachedItem1 );
		assertNotNull( cachedItem2 );
		assertEquals( item2.getName(), cachedItem2.getName() );
		s.getTransaction().commit();
		s.close();

		// cleanup
		s = openSession();
		s.beginTransaction();
		s.createQuery( "DELETE FROM AbstractCachedItem" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

}
