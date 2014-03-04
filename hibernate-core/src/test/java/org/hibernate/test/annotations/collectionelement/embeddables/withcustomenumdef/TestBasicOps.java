/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
		return new Class[] { Query.class, Location.class };
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
