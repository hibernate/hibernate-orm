//$Id: $

package org.hibernate.test.cascade;

import java.util.Collections;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gail Badner (based on annotations test case submitted by Edward Costello)
 */

/**
 * Test case to illustrate that when a delete-orphan cascade is used on a
 * one-to-many collection and the many-to-one side is also cascaded a
 * TransientObjectException is thrown.
 */
public class BidirectionalOneToManyCascadeTest extends FunctionalTestCase {

	public BidirectionalOneToManyCascadeTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] {
				"cascade/Child.hbm.xml",
				"cascade/DeleteOrphanChild.hbm.xml",
				"cascade/Parent.hbm.xml"
		};
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( BidirectionalOneToManyCascadeTest.class );
	}

	/**
	 * Saves the parent object with a child when both the one-to-many and
	 * many-to-one associations use cascade="all"
	 */
	public void testSaveParentWithChild() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		Parent parent = new Parent();
		Child child = new Child();
		child.setParent( parent );
		parent.setChildren( Collections.singleton( child ) );
		session.save( parent );
		txn.commit();
		session.close();

		session = openSession();
		txn = session.beginTransaction();
		parent = ( Parent ) session.get( Parent.class, parent.getId() );
		assertEquals( 1, parent.getChildren().size() );
		assertEquals( 0, parent.getDeleteOrphanChildren().size() );
		session.delete( parent );
		txn.commit();
		session.close();
	}

	/**
	 * Saves the child object with the parent when both the one-to-many and
	 * many-to-one associations use cascade="all"
	 */
	public void testSaveChildWithParent() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		Parent parent = new Parent();
		Child child = new Child();
		child.setParent( parent );
		parent.setChildren( Collections.singleton( child ) );
		session.save( child );
		txn.commit();
		session.close();

		session = openSession();
		txn = session.beginTransaction();
		parent = ( Parent ) session.get( Parent.class, parent.getId() );
		assertEquals( 1, parent.getChildren().size() );
		assertEquals( 0, parent.getDeleteOrphanChildren().size() );
		session.delete( parent );
		txn.commit();
		session.close();
	}

	/**
	 * Saves the parent object with a child when the one-to-many association
	 * uses cascade="all-delete-orphan" and the many-to-one association uses
	 * cascade="all"
	 */
	public void testSaveParentWithOrphanDeleteChild() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		Parent parent = new Parent();
		DeleteOrphanChild child = new DeleteOrphanChild();
		child.setParent( parent );
		parent.setDeleteOrphanChildren( Collections.singleton( child ) );
		session.save( parent );
		txn.commit();
		session.close();

		session = openSession();
		txn = session.beginTransaction();
		parent = ( Parent ) session.get( Parent.class, parent.getId() );
		assertEquals( 0, parent.getChildren().size() );
		assertEquals( 1, parent.getDeleteOrphanChildren().size() );
		session.delete( parent );
		txn.commit();
		session.close();
	}

	/**
	 * Saves the child object with the parent when the one-to-many association
	 * uses cascade="all-delete-orphan" and the many-to-one association uses
	 * cascade="all"
	 * <p/>
	 * This test is known to fail. See HHH-2269.
	 */
	public void testSaveOrphanDeleteChildWithParentFailureExpected() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		Parent parent = new Parent();
		DeleteOrphanChild child = new DeleteOrphanChild();
		child.setParent( parent );
		parent.setDeleteOrphanChildren( Collections.singleton( child ) );
		session.save( child );
		txn.commit();
		session.close();

		session = openSession();
		txn = session.beginTransaction();
		parent = ( Parent ) session.get( Parent.class, parent.getId() );
		assertEquals( 0, parent.getChildren().size() );
		assertEquals( 1, parent.getDeleteOrphanChildren().size() );
		session.delete( parent );
		txn.commit();
		session.close();
	}

}
