/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.reattachment;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * Test of collection reattachment semantics
 *
 * @author Steve Ebersole
 */
public class CollectionReattachmentTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "reattachment/Mappings.hbm.xml" };
	}

	@Test
	public void testUpdateOwnerAfterClear() {
		Session s = openSession();
		s.beginTransaction();
		Parent p = new Parent( "p" );
		p.getChildren().add( new Child( "c" ) );
		s.save( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		p = ( Parent ) s.get( Parent.class, "p" );
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
	public void testUpdateOwnerAfterEvict() {
		Session s = openSession();
		s.beginTransaction();
		Parent p = new Parent( "p" );
		p.getChildren().add( new Child( "c" ) );
		s.save( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		p = ( Parent ) s.get( Parent.class, "p" );
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
}
