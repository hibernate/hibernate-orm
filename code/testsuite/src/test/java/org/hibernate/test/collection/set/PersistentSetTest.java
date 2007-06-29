package org.hibernate.test.collection.set;

import java.util.HashSet;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.collection.PersistentSet;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * todo: describe PersistentSetTest
 *
 * @author Steve Ebersole
 */
public class PersistentSetTest extends FunctionalTestCase {
	public PersistentSetTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "collection/set/Mappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( PersistentSetTest.class );
	}

	public void testWriteMethodDirtying() {
		Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		Child otherChild = new Child( "c2" );

		Session session = openSession();
		session.beginTransaction();
		session.save( parent );
		session.flush();
		// at this point, the set on parent has now been replaced with a PersistentSet...
		PersistentSet children = ( PersistentSet ) parent.getChildren();

		assertFalse( children.add( child ) );
		assertFalse( children.isDirty() );

		assertFalse( children.remove( otherChild ) );
		assertFalse( children.isDirty() );

		HashSet otherSet = new HashSet();
		otherSet.add( child );
		assertFalse( children.addAll( otherSet ) );
		assertFalse( children.isDirty() );

		assertFalse( children.retainAll( otherSet ) );
		assertFalse( children.isDirty() );

		otherSet = new HashSet();
		otherSet.add( otherChild );
		assertFalse( children.removeAll( otherSet ) );
		assertFalse( children.isDirty() );

		assertTrue( children.retainAll( otherSet ));
		assertTrue( children.isDirty() );
		assertTrue( children.isEmpty() );

		children.clear();
		session.delete( child );
		assertTrue( children.isDirty() );

		session.flush();

		children.clear();
		assertFalse( children.isDirty() );

		session.delete( parent );
		session.getTransaction().commit();
		session.close();
	}
}
