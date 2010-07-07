//$Id: CreateTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.ops;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;

import org.hibernate.PersistentObjectException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.exception.ConstraintViolationException;

/**
 * @author Gavin King
 */
public class CreateTest extends AbstractOperationTestCase {

	public CreateTest(String str) {
		super( str );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CreateTest.class );
	}

	public void testNoUpdatesOnCreateVersionedWithCollection() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		VersionedEntity root = new VersionedEntity( "root", "root" );
		VersionedEntity child = new VersionedEntity( "c1", "child-1" );
		root.getChildren().add( child );
		child.setParent( root );
		s.save(root);
		tx.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		assertDeleteCount( 0 );

		s = openSession();
		tx = s.beginTransaction();
		s.delete( root );
		tx.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	public void testCreateTree() {

		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node root = new Node("root");
		Node child = new Node("child");
		root.addChild(child);
		s.persist(root);
		tx.commit();
		s.close();

		assertInsertCount(2);
		assertUpdateCount(0);

		s = openSession();
		tx = s.beginTransaction();
		System.out.println("getting");
		root = (Node) s.get(Node.class, "root");
		Node child2 = new Node("child2");
		root.addChild(child2);
		System.out.println("committing");
		tx.commit();
		s.close();

		assertInsertCount(3);
		assertUpdateCount(0);
	}

	public void testCreateTreeWithGeneratedId() {

		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode root = new NumberedNode("root");
		NumberedNode child = new NumberedNode("child");
		root.addChild(child);
		s.persist(root);
		tx.commit();
		s.close();

		assertInsertCount(2);
		assertUpdateCount(0);

		s = openSession();
		tx = s.beginTransaction();
		root = (NumberedNode) s.get( NumberedNode.class, new Long( root.getId() ) );
		NumberedNode child2 = new NumberedNode("child2");
		root.addChild(child2);
		tx.commit();
		s.close();

		assertInsertCount(3);
		assertUpdateCount(0);
	}

	public void testCreateException() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node dupe = new Node("dupe");
		s.persist(dupe);
		s.persist(dupe);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		s.persist(dupe);
		try {
			tx.commit();
			assertFalse(true);
		}
		catch (ConstraintViolationException cve) {
			//verify that an exception is thrown!
		}
		tx.rollback();
		s.close();

		Node nondupe = new Node("nondupe");
		nondupe.addChild(dupe);

		s = openSession();
		tx = s.beginTransaction();
		s.persist(nondupe);
		try {
			tx.commit();
			assertFalse(true);
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
		NumberedNode dupe = new NumberedNode("dupe");
		s.persist(dupe);
		s.persist(dupe);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		try {
			s.persist(dupe);
			assertFalse(true);
		}
		catch (PersistentObjectException poe) {
			//verify that an exception is thrown!
		}
		tx.rollback();
		s.close();

		NumberedNode nondupe = new NumberedNode("nondupe");
		nondupe.addChild(dupe);

		s = openSession();
		tx = s.beginTransaction();
		try {
			s.persist(nondupe);
			assertFalse(true);
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
		s.persist(ee);
		Collection erColl = new ArrayList();
		Collection eeColl = new ArrayList();
		erColl.add(ee);
		eeColl.add(er);
		er.setEmployees(erColl);
		ee.setEmployers(eeColl);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		er = (Employer) s.load(Employer.class, er.getId() );
		assertNotNull(er);
		assertNotNull( er.getEmployees() );
		assertEquals( 1, er.getEmployees().size() );
		Employee eeFromDb = (Employee) er.getEmployees().iterator().next();
		assertEquals( ee.getId(), eeFromDb.getId() );
		tx.commit();
		s.close();
	}
}

