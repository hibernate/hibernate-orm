/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Wallace Wadge (based on code by Gail Badner)
 */

/**
 * Test case to illustrate that when a child table attempts to cascade to a parent and the parent's Id
 * is set to assigned, an exception thrown (not-null property references a null or transient value). 
 * This error only occurs if the parent link in marked as not nullable.
 */
public class CascadeTestWithAssignedParentIdTest extends FunctionalTestCase {

	public CascadeTestWithAssignedParentIdTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] {
				"cascade/ChildForParentWithAssignedId.hbm.xml",
				"cascade/ParentWithAssignedId.hbm.xml"
		};
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CascadeTestWithAssignedParentIdTest.class );
	}

	public void testSaveChildWithParent() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		Parent parent = new Parent();
		Child child = new Child();
		child.setParent( parent );
		parent.setChildren( Collections.singleton( child ) );
		parent.setId(new Long(123L));
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
