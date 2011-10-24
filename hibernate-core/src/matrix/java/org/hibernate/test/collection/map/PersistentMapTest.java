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
package org.hibernate.test.collection.map;
import java.util.HashMap;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test various situations using a {@link PersistentMap}.
 *
 * @author Steve Ebersole
 */
public class PersistentMapTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "collection/map/Mappings.hbm.xml" };
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testWriteMethodDirtying() {
		Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.getChildren().put( child.getName(), child );
		child.setParent( parent );
		Child otherChild = new Child( "c2" );

		Session session = openSession();
		session.beginTransaction();
		session.save( parent );
		session.flush();
		// at this point, the map on parent has now been replaced with a PersistentMap...
		PersistentMap children = ( PersistentMap ) parent.getChildren();

		Object old = children.put( child.getName(), child );
		assertTrue( old == child );
		assertFalse( children.isDirty() );

		old = children.remove( otherChild.getName() );
		assertNull( old );
		assertFalse( children.isDirty() );

		HashMap otherMap = new HashMap();
		otherMap.put( child.getName(), child );
		children.putAll( otherMap );
		assertFalse( children.isDirty() );

		otherMap = new HashMap();
		otherMap.put( otherChild.getName(), otherChild );
		children.putAll( otherMap );
		assertTrue( children.isDirty() );

		children.clearDirty();
		session.delete( child );
		children.clear();
		assertTrue( children.isDirty() );
		session.flush();

		children.clear();
		assertFalse( children.isDirty() );

		session.delete( parent );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPutAgainstUninitializedMap() {
		// prepare map owner...
		Session session = openSession();
		session.beginTransaction();
		Parent parent = new Parent( "p1" );
		session.save( parent );
		session.getTransaction().commit();
		session.close();

		// Now, reload the parent and test adding children
		session = openSession();
		session.beginTransaction();
		parent = ( Parent ) session.get( Parent.class, parent.getName() );
		parent.addChild( "c1" );
		parent.addChild( "c2" );
		session.getTransaction().commit();
		session.close();

		assertEquals( 2, parent.getChildren().size() );

		session = openSession();
		session.beginTransaction();
		session.delete( parent );
		session.getTransaction().commit();
		session.close();
	}

	@Test
    public void testRemoveAgainstUninitializedMap() {
        Parent parent = new Parent( "p1" );
        Child child = new Child( "c1" );
        parent.addChild( child );

        Session session = openSession();
        session.beginTransaction();
        session.save( parent );
        session.getTransaction().commit();
        session.close();

        // Now reload the parent and test removing the child
        session = openSession();
        session.beginTransaction();
        parent = ( Parent ) session.get( Parent.class, parent.getName() );
        Child child2 = ( Child ) parent.getChildren().remove( child.getName() );
		child2.setParent( null );
		assertNotNull( child2 );
		assertTrue( parent.getChildren().isEmpty() );
        session.getTransaction().commit();
        session.close();

		// Load the parent once again and make sure child is still gone
		//		then cleanup
        session = openSession();
        session.beginTransaction();
		parent = ( Parent ) session.get( Parent.class, parent.getName() );
		assertTrue( parent.getChildren().isEmpty() );
		session.delete( child2 );
		session.delete( parent );
        session.getTransaction().commit();
        session.close();
    }
}
