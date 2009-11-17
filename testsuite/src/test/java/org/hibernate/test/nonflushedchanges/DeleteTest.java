package org.hibernate.test.nonflushedchanges;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.tm.SimpleJtaTransactionManagerImpl;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole, Gail Badner (adapted this from "ops" tests version)
 */
public class DeleteTest extends AbstractOperationTestCase {
	public DeleteTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( DeleteTest.class );
	}

	public void testDeleteVersionedWithCollectionNoUpdate() throws Exception {
		// test adapted from HHH-1564...
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		VersionedEntity c = new VersionedEntity( "c1", "child-1" );
		VersionedEntity p = new VersionedEntity( "root", "root" );
		p.getChildren().add( c );
		c.setParent( p );
		s.save( p );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		VersionedEntity loadedParent = ( VersionedEntity ) s.get( VersionedEntity.class, "root" );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		loadedParent = ( VersionedEntity ) getOldToNewEntityRefMap().get( loadedParent );
		s.delete( loadedParent );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	public void testNoUpdateOnDelete() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Node node = new Node( "test" );
		s.persist( node );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.delete( node );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );
	}

	public void testNoUpdateOnDeleteWithCollection() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Node parent = new Node( "parent" );
		Node child = new Node( "child" );
		parent.getCascadingChildren().add( child );
		s.persist( parent );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		parent = ( Node ) s.get( Node.class, "parent" );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		parent = ( Node ) getOldToNewEntityRefMap().get( parent );
		s.delete( parent );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );
		assertDeleteCount( 2 );
	}
}
