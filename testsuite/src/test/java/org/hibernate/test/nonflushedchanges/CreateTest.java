//$Id: CreateTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.nonflushedchanges;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.PersistentObjectException;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.tm.SimpleJtaTransactionManagerImpl;

/**
 * @author Gavin King, Gail Badner (adapted this from "ops" tests version)
 */
public class CreateTest extends AbstractOperationTestCase {

	public CreateTest(String str) {
		super( str );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CreateTest.class );
	}

	public void testNoUpdatesOnCreateVersionedWithCollection() throws Exception {
		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		VersionedEntity root = new VersionedEntity( "root", "root" );
		VersionedEntity child = new VersionedEntity( "c1", "child-1" );
		root.getChildren().add( child );
		child.setParent( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.save( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( VersionedEntity ) getOldToNewEntityRefMap().get( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( VersionedEntity ) getOldToNewEntityRefMap().get( root );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		assertDeleteCount( 0 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.delete( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( VersionedEntity ) getOldToNewEntityRefMap().get( root );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	public void testCreateTree() throws Exception {

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		root.addChild( child );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.persist( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		System.out.println( "getting" );
		root = ( Node ) s.get( Node.class, "root" );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( Node ) getOldToNewEntityRefMap().get( root );
		Node child2 = new Node( "child2" );
		root.addChild( child2 );
		System.out.println( "committing" );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
	}

	public void testCreateTreeWithGeneratedId() throws Exception {

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.persist( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) s.get( NumberedNode.class, new Long( root.getId() ) );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		NumberedNode child2 = new NumberedNode( "child2" );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		root.addChild( child2 );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
	}

	public void testCreateException() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Node dupe = new Node( "dupe" );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.persist( dupe );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		dupe = ( Node ) getOldToNewEntityRefMap().get( dupe );
		s.persist( dupe );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.persist( dupe );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		try {
			SimpleJtaTransactionManagerImpl.getInstance().commit();
			assertFalse( true );
		}
		catch ( ConstraintViolationException cve ) {
			//verify that an exception is thrown!
		}
		SimpleJtaTransactionManagerImpl.getInstance().rollback();

		Node nondupe = new Node( "nondupe" );
		nondupe.addChild( dupe );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.persist( nondupe );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		try {
			SimpleJtaTransactionManagerImpl.getInstance().commit();
			assertFalse( true );
		}
		catch ( ConstraintViolationException cve ) {
			//verify that an exception is thrown!
		}
		SimpleJtaTransactionManagerImpl.getInstance().rollback();
	}

	public void testCreateExceptionWithGeneratedId() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		NumberedNode dupe = new NumberedNode( "dupe" );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.persist( dupe );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		dupe = ( NumberedNode ) getOldToNewEntityRefMap().get( dupe );
		s.persist( dupe );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		try {
			s.persist( dupe );
			assertFalse( true );
		}
		catch ( PersistentObjectException poe ) {
			//verify that an exception is thrown!
		}
		SimpleJtaTransactionManagerImpl.getInstance().rollback();

		NumberedNode nondupe = new NumberedNode( "nondupe" );
		nondupe.addChild( dupe );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		try {
			s.persist( nondupe );
			assertFalse( true );
		}
		catch ( PersistentObjectException poe ) {
			//verify that an exception is thrown!
		}
		SimpleJtaTransactionManagerImpl.getInstance().rollback();
	}

	public void testBasic() throws Exception {
		Session s;
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		Employer er = new Employer();
		Employee ee = new Employee();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.persist( ee );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		ee = ( Employee ) getOldToNewEntityRefMap().get( ee );
		Collection erColl = new ArrayList();
		Collection eeColl = new ArrayList();
		erColl.add( ee );
		eeColl.add( er );
		er.setEmployees( erColl );
		ee.setEmployers( eeColl );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		ee = ( Employee ) getOldToNewEntityRefMap().get( ee );
		er = ( Employer ) ee.getEmployers().iterator().next();
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		er = ( Employer ) s.load( Employer.class, er.getId() );
		assertNotNull( er );
		assertFalse( Hibernate.isInitialized( er ) );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		er = ( Employer ) getOldToNewEntityRefMap().get( er );
		assertNotNull( er );
		assertFalse( Hibernate.isInitialized( er ) );
		assertNotNull( er.getEmployees() );
		assertEquals( 1, er.getEmployees().size() );
		Employee eeFromDb = ( Employee ) er.getEmployees().iterator().next();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		eeFromDb = ( Employee ) getOldToNewEntityRefMap().get( eeFromDb );
		assertEquals( ee.getId(), eeFromDb.getId() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();
	}
}