/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.collectionelement.embeddables.withcustomenumdef;

import static junit.framework.Assert.assertEquals;

import java.util.Iterator;

import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class TestBasicOps extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Query.class };
	}

	@Test
	public void testLoadAndStore() {
		Session s = openSession();
		s.beginTransaction();
		Query q = new Query( new Location( "first", Location.Type.COUNTY ) );
		s.save( q );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		q = (Query) s.get( Query.class, q.getId() );
		assertEquals( 1, q.getIncludedLocations().size() );
		Location l = q.getIncludedLocations().iterator().next();
		assertEquals( Location.Type.COUNTY, l.getType() );
		s.delete( q );
		s.getTransaction().commit();
		s.close();
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-7072")
	public void testEmbeddableWithNullables() {
		Session s = openSession();
		s.beginTransaction();
		Query q = new Query( new Location( null, Location.Type.COMMUNE ) );
		s.save( q );
		s.getTransaction().commit();
		s.clear();
		
		s.beginTransaction();
		q.getIncludedLocations().add( new Location( null, Location.Type.COUNTY ) );
		s.update( q );
		s.getTransaction().commit();
		s.clear();
		
		s.beginTransaction();
		q = (Query) s.get( Query.class, q.getId() );
//		assertEquals( 2, q.getIncludedLocations().size() );
		s.getTransaction().commit();
		s.clear();
		
		s.beginTransaction();
		Iterator<Location> itr = q.getIncludedLocations().iterator();
		itr.next();
		itr.remove();
		s.update( q );
		s.getTransaction().commit();
		s.clear();
		
		s.beginTransaction();
		q = (Query) s.get( Query.class, q.getId() );
		assertEquals( 1, q.getIncludedLocations().size() );
		s.delete( q );
		s.getTransaction().commit();
		s.close();
	}
}
