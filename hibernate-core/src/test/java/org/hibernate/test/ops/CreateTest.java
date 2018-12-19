/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import javax.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.PersistentObjectException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Gavin King
 */
@RequiresDialectFeature(DialectChecks.SupportsNoColumnInsert.class)
public class CreateTest extends AbstractOperationTestCase {
	@Test
	@SuppressWarnings( {"unchecked"})
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

	@Test
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

	@Test
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
		root = (NumberedNode) s.get( NumberedNode.class, Long.valueOf( root.getId() ) );
		NumberedNode child2 = new NumberedNode("child2");
		root.addChild(child2);
		tx.commit();
		s.close();

		assertInsertCount(3);
		assertUpdateCount(0);
	}

	@Test
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
			fail( "Expecting constraint failure" );
		}
		catch (PersistenceException e){
			//verify that an exception is thrown!
			assertTyping(ConstraintViolationException.class, e.getCause());
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
		catch (PersistenceException e){
			//verify that an exception is thrown!
			assertTyping(ConstraintViolationException.class, e.getCause());
		}
		tx.rollback();
		s.close();
	}

	@Test
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
		catch (PersistenceException e){
			//verify that an exception is thrown!
			assertTyping(PersistentObjectException.class, e.getCause());
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
		catch (PersistenceException e){
			//verify that an exception is thrown!
			assertTyping(PersistentObjectException.class, e.getCause());
		}
		tx.rollback();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
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

