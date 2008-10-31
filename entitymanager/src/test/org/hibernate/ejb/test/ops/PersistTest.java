//$Id$
package org.hibernate.ejb.test.ops;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.hibernate.PersistentObjectException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.test.EJB3TestCase;
import org.hibernate.exception.ConstraintViolationException;

/**
 * @author Gavin King
 */
public class PersistTest extends EJB3TestCase {

	public PersistTest(String str) {
		super( str );
	}

	public void testCreateTree() {

		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		root.addChild( child );
		s.persist( root );
		tx.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );

		s = openSession();
		tx = s.beginTransaction();
		root = (Node) s.get( Node.class, "root" );
		Node child2 = new Node( "child2" );
		root.addChild( child2 );
		tx.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
	}

	public void testCreateTreeWithGeneratedId() {

		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );
		s.persist( root );
		tx.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );

		s = openSession();
		tx = s.beginTransaction();
		root = (NumberedNode) s.get( NumberedNode.class, new Long( root.getId() ) );
		NumberedNode child2 = new NumberedNode( "child2" );
		root.addChild( child2 );
		tx.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
	}

	public void testCreateException() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node dupe = new Node( "dupe" );
		s.persist( dupe );
		s.persist( dupe );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		s.persist( dupe );
		try {
			tx.commit();
			fail( "Cannot persist() twice the same entity" );
		}
		catch (ConstraintViolationException cve) {
			//verify that an exception is thrown!
		}
		tx.rollback();
		s.close();

		Node nondupe = new Node( "nondupe" );
		nondupe.addChild( dupe );

		s = openSession();
		tx = s.beginTransaction();
		s.persist( nondupe );
		try {
			tx.commit();
			assertFalse( true );
		}
		catch (ConstraintViolationException cve) {
			//verify that an exception is thrown!
		}
		tx.rollback();
		s.close();
	}

	public void testCreateExceptionWithGeneratedId() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode dupe = new NumberedNode( "dupe" );
		s.persist( dupe );
		s.persist( dupe );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		try {
			s.persist( dupe );
			assertFalse( true );
		}
		catch (PersistentObjectException poe) {
			//verify that an exception is thrown!
		}
		tx.rollback();
		s.close();

		NumberedNode nondupe = new NumberedNode( "nondupe" );
		nondupe.addChild( dupe );

		s = openSession();
		tx = s.beginTransaction();
		try {
			s.persist( nondupe );
			assertFalse( true );
		}
		catch (PersistentObjectException poe) {
			//verify that an exception is thrown!
		}
		tx.rollback();
		s.close();
	}

	public void testBasic() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Employer er = new Employer();
		Employee ee = new Employee();
		s.persist( ee );
		Collection erColl = new ArrayList();
		Collection eeColl = new ArrayList();
		erColl.add( ee );
		eeColl.add( er );
		er.setEmployees( erColl );
		ee.setEmployers( eeColl );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		er = (Employer) s.load( Employer.class, er.getId() );
		assertNotNull( er );
		assertNotNull( er.getEmployees() );
		assertEquals( 1, er.getEmployees().size() );
		Employee eeFromDb = (Employee) er.getEmployees().iterator().next();
		assertEquals( ee.getId(), eeFromDb.getId() );
		tx.commit();
		s.close();
	}

	private void clearCounts() {
		getSessions().getStatistics().clear();
	}

	private void assertInsertCount(int count) {
		int inserts = (int) getSessions().getStatistics().getEntityInsertCount();
		assertEquals( count, inserts );
	}

	private void assertUpdateCount(int count) {
		int updates = (int) getSessions().getStatistics().getEntityUpdateCount();
		assertEquals( count, updates );
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
		return new TestSuite( PersistTest.class );
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

}

