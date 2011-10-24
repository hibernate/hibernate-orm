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
package org.hibernate.test.cascade;
import java.util.Collections;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Test case to illustrate that when a delete-orphan cascade is used on a
 * one-to-many collection and the many-to-one side is also cascaded a
 * TransientObjectException is thrown.
 *
 * (based on annotations test case submitted by Edward Costello)
 *
 * @author Gail Badner
 */
public class BidirectionalOneToManyCascadeTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {
				"cascade/Child.hbm.xml",
				"cascade/DeleteOrphanChild.hbm.xml",
				"cascade/Parent.hbm.xml"
		};
	}

	/**
	 * Saves the parent object with a child when both the one-to-many and
	 * many-to-one associations use cascade="all"
	 */
	@Test
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
	@Test
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
	@Test
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
	 */
	@Test
	public void testSaveOrphanDeleteChildWithParent() {
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
