package org.hibernate.test.collection.list;

import java.util.ArrayList;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.collection.PersistentList;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * Tests related to operations on a PersistentList
 *
 * @author Steve Ebersole
 */
public class PersistentListTest extends FunctionalTestCase {
	public PersistentListTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "collection/list/Mappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( PersistentListTest.class );
	}

	public void testWriteMethodDirtying() {
		ListOwner parent = new ListOwner( "root" );
		ListOwner child = new ListOwner( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		ListOwner otherChild = new ListOwner( "c2" );

		Session session = openSession();
		session.beginTransaction();
		session.save( parent );
		session.flush();
		// at this point, the list on parent has now been replaced with a PersistentList...
		PersistentList children = ( PersistentList ) parent.getChildren();

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
