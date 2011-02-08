package org.hibernate.test.collection.map;
import java.util.HashMap;
import junit.framework.Test;
import org.hibernate.Session;
import org.hibernate.collection.PersistentMap;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * Test various situations using a {@link PersistentMap}.
 *
 * @author Steve Ebersole
 */
public class PersistentMapTest extends FunctionalTestCase {
	public PersistentMapTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "collection/map/Mappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( PersistentMapTest.class );
	}

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
