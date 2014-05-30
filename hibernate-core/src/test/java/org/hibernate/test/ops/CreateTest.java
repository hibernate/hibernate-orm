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
package org.hibernate.test.ops;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.PersistentObjectException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

/**
 * @author Gavin King
 */
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

	@Test
	@SuppressWarnings( {"unchecked"})
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

