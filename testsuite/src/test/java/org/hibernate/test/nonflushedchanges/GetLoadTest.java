//$Id: GetLoadTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.nonflushedchanges;

import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.test.tm.SimpleJtaTransactionManagerImpl;


/**
 * @author Gavin King, Gail Badner (adapted this from "ops" tests version)
 */
public class GetLoadTest extends AbstractOperationTestCase {

	public GetLoadTest(String str) {
		super( str );
	}

	public void testGetLoad() throws Exception {
		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Employer emp = new Employer();
		s.persist( emp );
		Node node = new Node( "foo" );
		Node parent = new Node( "bar" );
		parent.addChild( node );
		s.persist( parent );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
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
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
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
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		emp = ( Employer ) s.get( "org.hibernate.test.nonflushedchanges.Employer", emp.getId() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		emp = ( Employer ) getOldToNewEntityRefMap().get( emp );
		assertTrue( Hibernate.isInitialized( emp ) );
		node = ( Node ) s.get( "org.hibernate.test.nonflushedchanges.Node", node.getName() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		node = ( Node ) getOldToNewEntityRefMap().get( node );
		assertTrue( Hibernate.isInitialized( node ) );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
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
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertFetchCount( 0 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.createQuery( "delete from Employer" ).executeUpdate();
		List list = s.createQuery( "from Node" ).list();
		for ( Iterator it=list.iterator(); it.hasNext(); ) {
			s.delete( it.next() );
		}
		SimpleJtaTransactionManagerImpl.getInstance().commit();
	}

	public void testGetReadOnly() throws Exception {
		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Employer emp = new Employer();
		s.persist( emp );
		Node node = new Node( "foo" );
		Node parent = new Node( "bar" );
		parent.addChild( node );
		s.persist( parent );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
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
		for ( Iterator it=node.getChildren().iterator(); it.hasNext(); ) {
			assertFalse( s.isReadOnly( it.next() ) );
		}
		assertFalse( Hibernate.isInitialized( node.getParent() ) );
		assertNull( s.get( Node.class, "xyz" ) );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
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
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertFetchCount( 0 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.createQuery( "delete from Employer" ).executeUpdate();
		List list = s.createQuery( "from Node" ).list();
		for ( Iterator it=list.iterator(); it.hasNext(); ) {
			s.delete( it.next() );
		}
		SimpleJtaTransactionManagerImpl.getInstance().commit();
	}

	public void testLoadReadOnly() throws Exception {
		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Employer emp = new Employer();
		s.persist( emp );
		Node node = new Node( "foo" );
		Node parent = new Node( "bar" );
		parent.addChild( node );
		s.persist( parent );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
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
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.createQuery( "delete from Employer" ).executeUpdate();
		List list = s.createQuery( "from Node" ).list();
		for ( Iterator it=list.iterator(); it.hasNext(); ) {
			s.delete( it.next() );
		}
		SimpleJtaTransactionManagerImpl.getInstance().commit();
	}

	public void testGetAfterDelete() throws Exception {
		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Employer emp = new Employer();
		s.persist( emp );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.delete( emp );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		emp = ( Employer ) s.get( Employee.class, emp.getId() );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertNull( "get did not return null after delete", emp );
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( GetLoadTest.class );
	}
}

