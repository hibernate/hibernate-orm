package org.hibernate.test.ops;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class DeleteTest extends AbstractOperationTestCase {
	public DeleteTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( DeleteTest.class );
	}

	public void testDeleteVersionedWithCollectionNoUpdate() {
		// test adapted from HHH-1564...
		Session s = openSession();
		s.beginTransaction();
		VersionedEntity c = new VersionedEntity( "c1", "child-1" );
		VersionedEntity p = new VersionedEntity( "root", "root");
		p.getChildren().add( c );
		c.setParent( p );
		s.save( p );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		s = openSession();
		s.beginTransaction();
        VersionedEntity loadedParent = ( VersionedEntity ) s.get( VersionedEntity.class, "root" );
        s.delete( loadedParent );
		s.getTransaction().commit();
        s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	public void testNoUpdateOnDelete() {
		Session s = openSession();
        s.beginTransaction();
		Node node = new Node( "test" );
		s.persist( node );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		s = openSession();
		s.beginTransaction();
		s.delete( node );
		s.getTransaction().commit();
		s.close();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );
	}

	public void testNoUpdateOnDeleteWithCollection() {
		Session s = openSession();
        s.beginTransaction();
		Node parent = new Node( "parent" );
		Node child = new Node( "child" );
		parent.getCascadingChildren().add( child );
		s.persist( parent );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		s = openSession();
		s.beginTransaction();
		parent = ( Node ) s.get( Node.class, "parent" );
		s.delete( parent );
		s.getTransaction().commit();
		s.close();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );
		assertDeleteCount( 2 );
	}
}
