//$Id$
package org.hibernate.ejb.test.ops;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.test.EJB3TestCase;

/**
 * @author Gavin King
 */
public class GetLoadTest extends EJB3TestCase {

	public GetLoadTest(String str) {
		super( str );
	}

	public void testGetLoad() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Employer emp = new Employer();
		s.persist( emp );
		Node node = new Node( "foo" );
		Node parent = new Node( "bar" );
		parent.addChild( node );
		s.persist( parent );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		emp = (Employer) s.get( Employer.class, emp.getId() );
		assertTrue( Hibernate.isInitialized( emp ) );
		assertFalse( Hibernate.isInitialized( emp.getEmployees() ) );
		node = (Node) s.get( Node.class, node.getName() );
		assertTrue( Hibernate.isInitialized( node ) );
		assertFalse( Hibernate.isInitialized( node.getChildren() ) );
		assertFalse( Hibernate.isInitialized( node.getParent() ) );
		assertNull( s.get( Node.class, "xyz" ) );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		emp = (Employer) s.load( Employer.class, emp.getId() );
		emp.getId();
		assertFalse( Hibernate.isInitialized( emp ) );
		node = (Node) s.load( Node.class, node.getName() );
		assertEquals( node.getName(), "foo" );
		assertFalse( Hibernate.isInitialized( node ) );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		emp = (Employer) s.get( "org.hibernate.ejb.test.ops.Employer", emp.getId() );
		assertTrue( Hibernate.isInitialized( emp ) );
		node = (Node) s.get( "org.hibernate.ejb.test.ops.Node", node.getName() );
		assertTrue( Hibernate.isInitialized( node ) );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		emp = (Employer) s.load( "org.hibernate.ejb.test.ops.Employer", emp.getId() );
		emp.getId();
		assertFalse( Hibernate.isInitialized( emp ) );
		node = (Node) s.load( "org.hibernate.ejb.test.ops.Node", node.getName() );
		assertEquals( node.getName(), "foo" );
		assertFalse( Hibernate.isInitialized( node ) );
		tx.commit();
		s.close();

		assertFetchCount( 0 );
	}

	private void clearCounts() {
		getSessions().getStatistics().clear();
	}

	private void assertFetchCount(int count) {
		int fetches = (int) getSessions().getStatistics().getEntityFetchCount();
		assertEquals( count, fetches );
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	protected String[] getMappings() {
		return new String[]{
				"ops/Node.hbm.xml",
				"ops/Employer.hbm.xml"
		};
	}

	public static Test suite() {
		return new TestSuite( GetLoadTest.class );
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

}

