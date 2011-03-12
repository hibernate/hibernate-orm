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
 *
 */
package org.hibernate.test.onetomany;

import java.util.ArrayList;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.test.readonly.VersionedNode;

/**
 * @author Burkhard Graves, Gail Badner
 */

public abstract class AbstractVersionedRecursiveBidirectionalOneToManyTest extends AbstractRecursiveBidirectionalOneToManyTest {

    /*
	 *  What is done:
	 *    ___                   ___
	 *   |   |                 |   |
	 *    -> 1                  -> 1
	 *       |   -transform->     / \
	 *       2                   2   3
	 *       |
	 *     	 3
	 *
	 */

	public AbstractVersionedRecursiveBidirectionalOneToManyTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "onetomany/VersionedNode.hbm.xml" };
	}

	void check(boolean simplePropertyUpdated) {
		super.check( simplePropertyUpdated );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node node1 = ( Node ) s.get( Node.class, new Integer( 1 ) );
		Node node2 = ( Node ) s.get( Node.class, new Integer( 2 ) );
		Node node3 = ( Node ) s.get( Node.class, new Integer( 3 ) );
		assertEquals( 1, node1.getVersion() );
		assertEquals( 1, node2.getVersion() );
		assertEquals( 1, node3.getVersion() );
		tx.commit();
		s.close();
	}
}
