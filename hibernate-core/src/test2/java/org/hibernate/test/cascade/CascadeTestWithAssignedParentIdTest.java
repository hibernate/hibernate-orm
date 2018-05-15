/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cascade;

import java.util.Collections;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test case to illustrate that when a child table attempts to cascade to a parent and the parent's Id
 * is set to assigned, an exception thrown (not-null property references a null or transient value). 
 * This error only occurs if the parent link in marked as not nullable.
 *
 * @author Wallace Wadge (based on code by Gail Badner)
 */
public class CascadeTestWithAssignedParentIdTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] {
				"cascade/ChildForParentWithAssignedId.hbm.xml",
				"cascade/ParentWithAssignedId.hbm.xml"
		};
	}

	@Test
	public void testSaveChildWithParent() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		Parent parent = new Parent();
		Child child = new Child();
		child.setParent( parent );
		parent.setChildren( Collections.singleton( child ) );
		parent.setId( Long.valueOf(123L) );
		// this should figure out that the parent needs saving first since id is assigned.
		session.save( child );
		txn.commit();
		session.close();

		session = openSession();
		txn = session.beginTransaction();
		parent = ( Parent ) session.get( Parent.class, parent.getId() );
		assertEquals( 1, parent.getChildren().size() );
		txn.commit();
		session.close();
	}
}
