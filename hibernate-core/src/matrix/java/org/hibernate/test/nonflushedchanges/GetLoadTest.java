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

import java.util.List;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * adapted this from "ops" tests version
 *
 * @author Gail Badner
 * @author Gavin King
 */
public class GetLoadTest extends AbstractOperationTestCase {
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	@Test
	@SuppressWarnings( {"UnusedAssignment"})
	public void testGetLoad() throws Exception {
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Employer emp = new Employer();
		s.persist( emp );
		Node node = new Node( "foo" );
		Node parent = new Node( "bar" );
		parent.addChild( node );
		s.persist( parent );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		emp = ( Employer ) s.get( Employer.class, emp.getId() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		emp = ( Employer ) getOldToNewEntityRefMap().get( emp );
		assertTrue( Hibernate.isInitialized( emp ) );
		assertFalse( Hibernate.isInitialized( emp.getEmployees() ) );
		node = ( Node ) s.get( Node.class, node.getName() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		node = ( Node ) getOldToNewEntityRefMap().get( node );
		emp = ( Employer ) getOldToNewEntityRefMap().get( emp );
		assertTrue( Hibernate.isInitialized( node ) );
		assertFalse( Hibernate.isInitialized( node.getChildren() ) );
		assertFalse( Hibernate.isInitialized( node.getParent() ) );
		assertNull( s.get( Node.class, "xyz" ) );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		emp = ( Employer ) s.load( Employer.class, emp.getId() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		emp = ( Employer ) getOldToNewEntityRefMap().get( emp );
		emp.getId();
		assertFalse( Hibernate.isInitialized( emp ) );
		node = ( Node ) s.load( Node.class, node.getName() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		node = ( Node ) getOldToNewEntityRefMap().get( node );
		assertEquals( node.getName(), "foo" );
		assertFalse( Hibernate.isInitialized( node ) );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		emp = ( Employer ) s.get( "org.hibernate.test.nonflushedchanges.Employer", emp.getId() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		emp = ( Employer ) getOldToNewEntityRefMap().get( emp );
		assertTrue( Hibernate.isInitialized( emp ) );
		node = ( Node ) s.get( "org.hibernate.test.nonflushedchanges.Node", node.getName() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		node = ( Node ) getOldToNewEntityRefMap().get( node );
		assertTrue( Hibernate.isInitialized( node ) );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		emp = ( Employer ) s.load( "org.hibernate.test.nonflushedchanges.Employer", emp.getId() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		emp = ( Employer ) getOldToNewEntityRefMap().get( emp );
		emp.getId();
		assertFalse( Hibernate.isInitialized( emp ) );
		node = ( Node ) s.load( "org.hibernate.test.nonflushedchanges.Node", node.getName() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		emp = ( Employer ) getOldToNewEntityRefMap().get( emp );
		node = ( Node ) getOldToNewEntityRefMap().get( node );
		assertEquals( node.getName(), "foo" );
		assertFalse( Hibernate.isInitialized( node ) );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertFetchCount( 0 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Employer" ).executeUpdate();
		List list = s.createQuery( "from Node" ).list();
		for ( Object aList : list ) {
			s.delete( aList );
		}
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	public void testGetReadOnly() throws Exception {
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Employer emp = new Employer();
		s.persist( emp );
		Node node = new Node( "foo" );
		Node parent = new Node( "bar" );
		parent.addChild( node );
		s.persist( parent );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		s.setDefaultReadOnly( true );
		emp = ( Employer ) s.get( Employer.class, emp.getId() );
		assertTrue( s.isDefaultReadOnly() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		assertTrue( s.isDefaultReadOnly() );
		emp = ( Employer ) getOldToNewEntityRefMap().get( emp );
		assertTrue( Hibernate.isInitialized( emp ) );
		assertFalse( Hibernate.isInitialized( emp.getEmployees() ) );
		node = ( Node ) s.get( Node.class, node.getName() );
		assertTrue( s.isReadOnly( emp ) );
		assertTrue( s.isReadOnly( node ) );
		s.setDefaultReadOnly( false );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		assertFalse( s.isDefaultReadOnly() );
		node = ( Node ) getOldToNewEntityRefMap().get( node );
		emp = ( Employer ) getOldToNewEntityRefMap().get( emp );
		assertTrue( Hibernate.isInitialized( node ) );
		assertTrue( s.isReadOnly( node ) );
		assertFalse( Hibernate.isInitialized( node.getParent() ) );
		assertTrue( s.isReadOnly( emp ) );
		assertFalse( Hibernate.isInitialized( node.getChildren() ) );
		Hibernate.initialize( node.getChildren() );
		for ( Object o : node.getChildren() ) {
			assertFalse( s.isReadOnly( o ) );
		}
		assertFalse( Hibernate.isInitialized( node.getParent() ) );
		assertNull( s.get( Node.class, "xyz" ) );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		emp = ( Employer ) s.get( "org.hibernate.test.nonflushedchanges.Employer", emp.getId() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		assertFalse( s.isDefaultReadOnly() );
		emp = ( Employer ) getOldToNewEntityRefMap().get( emp );
		assertTrue( Hibernate.isInitialized( emp ) );
		assertFalse( s.isReadOnly( emp ) );
		s.setReadOnly( emp, true );
		node = ( Node ) s.get( "org.hibernate.test.nonflushedchanges.Node", node.getName() );
		assertFalse( s.isReadOnly( node ) );
		s.setReadOnly( node, true );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		emp = ( Employer ) getOldToNewEntityRefMap().get( emp );
		assertTrue( s.isReadOnly( emp ) );
		node = ( Node ) getOldToNewEntityRefMap().get( node );
		assertTrue( Hibernate.isInitialized( node ) );
		assertTrue( s.isReadOnly( node ) );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertFetchCount( 0 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Employer" ).executeUpdate();
		List list = s.createQuery( "from Node" ).list();
		for ( Object aList : list ) {
			s.delete( aList );
		}
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	public void testLoadReadOnly() throws Exception {
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Employer emp = new Employer();
		s.persist( emp );
		Node node = new Node( "foo" );
		Node parent = new Node( "bar" );
		parent.addChild( node );
		s.persist( parent );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		assertFalse( s.isDefaultReadOnly() );
		s.setDefaultReadOnly( true );
		emp = ( Employer ) s.load( Employer.class, emp.getId() );
		assertFalse( Hibernate.isInitialized( emp ) );
		assertTrue( s.isReadOnly( emp ) );
		assertTrue( s.isDefaultReadOnly() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		assertTrue( s.isDefaultReadOnly() );
		emp = ( Employer ) getOldToNewEntityRefMap().get( emp );
		assertFalse( Hibernate.isInitialized( emp ) );
		assertTrue( s.isReadOnly( emp ) );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Employer" ).executeUpdate();
		List list = s.createQuery( "from Node" ).list();
		for ( Object aList : list ) {
			s.delete( aList );
		}
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	public void testGetAfterDelete() throws Exception {
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Employer emp = new Employer();
		s.persist( emp );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.delete( emp );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		emp = ( Employer ) s.get( Employee.class, emp.getId() );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertNull( "get did not return null after delete", emp );
	}

}

