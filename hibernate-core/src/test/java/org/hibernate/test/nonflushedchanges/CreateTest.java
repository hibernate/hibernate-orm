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
package org.hibernate.test.nonflushedchanges;

import java.util.ArrayList;
import java.util.Collection;
import javax.transaction.RollbackException;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.PersistentObjectException;
import org.hibernate.Session;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Gavin King, Gail Badner (adapted this from "ops" tests version)
 */
public class CreateTest extends AbstractOperationTestCase {
	@Test
	@SuppressWarnings( {"unchecked"})
	public void testNoUpdatesOnCreateVersionedWithCollection() throws Exception {
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
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
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( VersionedEntity ) getOldToNewEntityRefMap().get( root );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		assertDeleteCount( 0 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.delete( root );
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateTree() throws Exception {
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		root.addChild( child );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.persist( root );
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		System.out.println( "getting" );
		root = ( Node ) s.get( Node.class, "root" );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( Node ) getOldToNewEntityRefMap().get( root );
		Node child2 = new Node( "child2" );
		root.addChild( child2 );
		System.out.println( "committing" );
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
	}

	@Test
	@SuppressWarnings( {"UnnecessaryBoxing"})
	public void testCreateTreeWithGeneratedId() throws Exception {
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.persist( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) s.get( NumberedNode.class, Long.valueOf( root.getId() ) );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		NumberedNode child2 = new NumberedNode( "child2" );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		root.addChild( child2 );
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
	}

	@Test
	public void testCreateException() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Node dupe = new Node( "dupe" );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.persist( dupe );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		dupe = ( Node ) getOldToNewEntityRefMap().get( dupe );
		s.persist( dupe );
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.persist( dupe );
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		try {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
			Assert.fail();
		}
		catch ( ConstraintViolationException cve ) {
			//verify that an exception is thrown!
		}
		catch ( RollbackException e ) {
			if ( ! ConstraintViolationException.class.isInstance( e.getCause() ) ) {
				throw (Exception) e.getCause();
			}
		}
		if ( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) ) {
			// ugh! really!?!
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
		}

		Node nondupe = new Node( "nondupe" );
		nondupe.addChild( dupe );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.persist( nondupe );
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		try {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
			Assert.fail();
		}
		catch ( ConstraintViolationException cve ) {
			//verify that an exception is thrown!
		}
		catch ( RollbackException e ) {
			if ( ! ConstraintViolationException.class.isInstance( e.getCause() ) ) {
				throw (Exception) e.getCause();
			}
		}
		if ( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) ) {
			// ugh! really!?!
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
		}
	}

	@Test
	public void testCreateExceptionWithGeneratedId() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		NumberedNode dupe = new NumberedNode( "dupe" );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.persist( dupe );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		dupe = ( NumberedNode ) getOldToNewEntityRefMap().get( dupe );
		s.persist( dupe );
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		dupe = ( NumberedNode ) getOldToNewEntityRefMap().get( dupe );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		try {
			s.persist( dupe );
			s.flush();
			assertFalse( true );
		}
		catch ( PersistentObjectException poe ) {
			//verify that an exception is thrown!
		}
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();

		NumberedNode nondupe = new NumberedNode( "nondupe" );
		nondupe.addChild( dupe );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		try {
			s.persist( nondupe );
			assertFalse( true );
		}
		catch ( PersistentObjectException poe ) {
			//verify that an exception is thrown!
		}
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testBasic() throws Exception {
		Session s;
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
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
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		ee = ( Employee ) getOldToNewEntityRefMap().get( ee );
		er = ( Employer ) ee.getEmployers().iterator().next();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
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
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}
}