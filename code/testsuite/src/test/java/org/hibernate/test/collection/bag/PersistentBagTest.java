package org.hibernate.test.collection.bag;

import java.util.ArrayList;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.collection.PersistentBag;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * Tests related to operations on a PersistentBag.
 *
 * @author Steve Ebersole
 */
public class PersistentBagTest extends FunctionalTestCase {
	public PersistentBagTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "collection/bag/Mappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( PersistentBagTest.class );
	}

	public void testWriteMethodDirtying() {
		BagOwner parent = new BagOwner( "root" );
		BagOwner child = new BagOwner( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		BagOwner otherChild = new BagOwner( "c2" );

		Session session = openSession();
		session.beginTransaction();
		session.save( parent );
		session.flush();
		// at this point, the list on parent has now been replaced with a PersistentBag...
		PersistentBag children = ( PersistentBag ) parent.getChildren();

		assertFalse( children.remove( otherChild ) );
		assertFalse( children.isDirty() );

		ArrayList otherCollection = new ArrayList();
		otherCollection.add( child );
		assertFalse( children.retainAll( otherCollection ) );
		assertFalse( children.isDirty() );

		otherCollection = new ArrayList();
		otherCollection.add( otherChild );
		assertFalse( children.removeAll( otherCollection ) );
		assertFalse( children.isDirty() );

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
