//$Id: GetLoadTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.ops;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;


/**
 * @author Gavin King
 */
public class GetLoadTest extends FunctionalTestCase {
	
	public GetLoadTest(String str) {
		super(str);
	}
	
	public void testGetLoad() {
		clearCounts();
		
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Employer emp = new Employer();
		s.persist(emp);
		Node node = new Node("foo");
		Node parent = new Node("bar");
		parent.addChild(node);
		s.persist(parent);
		tx.commit();
		s.close();
		
		s = openSession();
		tx = s.beginTransaction();
		emp = (Employer) s.get(Employer.class, emp.getId());
		assertTrue( Hibernate.isInitialized(emp) );
		assertFalse( Hibernate.isInitialized(emp.getEmployees()) );
		node = (Node) s.get(Node.class, node.getName());
		assertTrue( Hibernate.isInitialized(node) );
		assertFalse( Hibernate.isInitialized(node.getChildren()) );
		assertFalse( Hibernate.isInitialized(node.getParent()) );
		assertNull( s.get(Node.class, "xyz") );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		emp = (Employer) s.load(Employer.class, emp.getId());
		emp.getId();
		assertFalse( Hibernate.isInitialized(emp) );
		node = (Node) s.load(Node.class, node.getName());
		assertEquals( node.getName(), "foo" );
		assertFalse( Hibernate.isInitialized(node) );
		tx.commit();
		s.close();
	
		s = openSession();
		tx = s.beginTransaction();
		emp = (Employer) s.get("org.hibernate.test.ops.Employer", emp.getId());
		assertTrue( Hibernate.isInitialized(emp) );
		node = (Node) s.get("org.hibernate.test.ops.Node", node.getName());
		assertTrue( Hibernate.isInitialized(node) );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		emp = (Employer) s.load("org.hibernate.test.ops.Employer", emp.getId());
		emp.getId();
		assertFalse( Hibernate.isInitialized(emp) );
		node = (Node) s.load("org.hibernate.test.ops.Node", node.getName());
		assertEquals( node.getName(), "foo" );
		assertFalse( Hibernate.isInitialized(node) );
		tx.commit();
		s.close();
		
		assertFetchCount(0);
	}

	public void testGetAfterDelete() {
		clearCounts();

		Session s = openSession();
		s.beginTransaction();
		Employer emp = new Employer();
		s.persist( emp );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( emp );
		emp = ( Employer ) s.get( Employee.class, emp.getId() );
		s.getTransaction().commit();
		s.close();

		assertNull( "get did not return null after delete", emp );
	}

	private void clearCounts() {
		getSessions().getStatistics().clear();
	}
	
	private void assertFetchCount(int count) {
		int fetches = (int) getSessions().getStatistics().getEntityFetchCount();
		assertEquals(count, fetches);
	}
		
	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
		cfg.setProperty(Environment.STATEMENT_BATCH_SIZE, "0");		
	}
	
	public String[] getMappings() {
		return new String[] { "ops/Node.hbm.xml", "ops/Employer.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite(GetLoadTest.class);
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

}

