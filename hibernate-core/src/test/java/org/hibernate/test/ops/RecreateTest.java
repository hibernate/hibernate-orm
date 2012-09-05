/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;

/**
 * @author Gail Badner
 */
public class RecreateTest extends AbstractOperationTestCase {

	@Test
	public void testCreateDeleteRecreate() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		root.addChild( child );
		child.setParent( root );
		s.persist( root );
		tx.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		assertDeleteCount( 0 );
		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		s.delete( root );
		tx.commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		s.persist( root );
		tx.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		assertDeleteCount( 0 );
		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		s.delete( root );
		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4468")
	public void testCreateDeleteRecreateInSameSession() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		root.addChild( child );
		child.setParent( root );
		s.persist( root );
		tx.commit();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		assertDeleteCount( 0 );
		clearCounts();

		tx = s.beginTransaction();
		s.delete( root );
		tx.commit();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
		clearCounts();

		tx = s.beginTransaction();
		s.persist( root );
		tx.commit();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		assertDeleteCount( 0 );
		clearCounts();

		s.close();

		s = openSession();
		tx = s.beginTransaction();
		s.delete( root );
		tx.commit();
		s.close();
	}

}

