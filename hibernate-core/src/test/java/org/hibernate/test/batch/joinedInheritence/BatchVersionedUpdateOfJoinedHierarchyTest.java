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
package org.hibernate.test.batch.joinedInheritence;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

public class BatchVersionedUpdateOfJoinedHierarchyTest
    extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Animal.class, Lion.class, Tiger.class, Zoo.class };
	}

	@Test
	public void testUpdateVersionedWithWhereClause() {
		Session session = openSession();
		session.beginTransaction();
        session.createQuery("update versioned Animal set weight = 69 where weight = 0").executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testUpdateVersionedWithoutWhereClause() {
		Session session = openSession();
		session.beginTransaction();
        session.createQuery("update versioned Animal set weight = 69").executeUpdate();
		session.getTransaction().commit();
		session.close();
	}
}
