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
package org.hibernate.test.reattachment;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test of proxy reattachment semantics
 *
 * @author Steve Ebersole
 */
public class ProxyReattachmentTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "reattachment/Mappings.hbm.xml" };
	}

	@Test
	public void testUpdateAfterEvict() {
		Session s = openSession();
		s.beginTransaction();
		Parent p = new Parent( "p" );
		s.save( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		p = ( Parent ) s.load( Parent.class, "p" );
		// evict...
		s.evict( p );
		// now try to reattach...
		s.update( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( p );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testUpdateAfterClear() {
		Session s = openSession();
		s.beginTransaction();
		Parent p = new Parent( "p" );
		s.save( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		p = ( Parent ) s.load( Parent.class, "p" );
		// clear...
		s.clear();
		// now try to reattach...
		s.update( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( p );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testIterateWithClearTopOfLoop() {
		Session s = openSession();
		s.beginTransaction();
		Set parents = new HashSet();
		for (int i=0; i<5; i++) {
			Parent p = new Parent( String.valueOf( i ) );
			Child child = new Child( "child" + i );
			child.setParent( p );
			p.getChildren().add( child );
			s.save( p );
			parents.add(p);
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		int i = 0;
		for ( Iterator it = s.createQuery( "from Parent" ).iterate(); it.hasNext(); ) {
			i++;
			if (i % 2 == 0) {
				s.flush();
				s.clear();
			}
			Parent p = (Parent) it.next();
			assertEquals( 1, p.getChildren().size() );
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		for ( Object parent : parents ) {
			s.delete( parent );
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testIterateWithClearBottomOfLoop() {
		Session s = openSession();
		s.beginTransaction();
		Set parents = new HashSet();
		for (int i=0; i<5; i++) {
			Parent p = new Parent( String.valueOf( i ) );
			Child child = new Child( "child" + i );
			child.setParent( p );
			p.getChildren().add( child );
			s.save( p );
			parents.add(p);
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		int i = 0;
		for (Iterator it = s.createQuery( "from Parent" ).iterate(); it.hasNext(); ) {
			Parent p = (Parent) it.next();
			assertEquals( 1, p.getChildren().size() );
			i++;
			if (i % 2 == 0) {
				s.flush();
				s.clear();
			}
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		for ( Object parent : parents ) {
			s.delete( parent );
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testIterateWithEvictTopOfLoop() {
		Session s = openSession();
		s.beginTransaction();
		Set parents = new HashSet();
		for (int i=0; i<5; i++) {
			Parent p = new Parent( String.valueOf( i + 100 ) );
			Child child = new Child( "child" + i );
			child.setParent( p );
			p.getChildren().add( child );
			s.save( p );
			parents.add(p);
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Parent p = null;
		for (Iterator it = s.createQuery( "from Parent" ).iterate(); it.hasNext(); ) {
			if ( p != null) { s.evict(p); }
			p = (Parent) it.next();
			assertEquals( 1, p.getChildren().size() );
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		for ( Object parent : parents ) {
			s.delete( parent );
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testIterateWithEvictBottomOfLoop() {
		Session s = openSession();
		s.beginTransaction();
		Set parents = new HashSet();
		for (int i=0; i<5; i++) {
			Parent p = new Parent( String.valueOf( i + 100 ) );
			Child child = new Child( "child" + i );
			child.setParent( p );
			p.getChildren().add( child );
			s.save( p );
			parents.add(p);
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		for (Iterator it = s.createQuery( "from Parent" ).iterate(); it.hasNext(); ) {
			Parent p = (Parent) it.next();
			assertEquals( 1, p.getChildren().size() );
			s.evict(p);
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		for ( Object parent : parents ) {
			s.delete( parent );
		}
		s.getTransaction().commit();
		s.close();
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8374")
	public void testRemoveAndReattachProxyEntity() {
		Session s = openSession();
		s.beginTransaction();
		Parent p = new Parent("foo");
		s.persist( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		p = (Parent) s.load( Parent.class, p.getName() );
		s.delete( p );
		// re-attach
		s.persist( p );
		s.getTransaction().commit();
		s.close();
	}
}
