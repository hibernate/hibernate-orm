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
package org.hibernate.test.annotations.cascade;

import java.util.HashSet;

import org.junit.Test;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.fail;

/**
 * @author Jeff Schnitzer
 * @author Gail Badner
 */
@SuppressWarnings("unchecked")
public class NonNullableCircularDependencyCascadeTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testIdClassInSuperclass() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Parent p = new Parent();
		p.setChildren( new HashSet<Child>() );

		Child ch = new Child(p);
		p.getChildren().add(ch);
		p.setDefaultChild(ch);

		try {
			s.persist(p);
			s.flush();
			fail( "should have failed because of transient entities have non-nullable, circular dependency." );
		}
		catch ( HibernateException ex) {
			// expected
		}
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Child.class,
				Parent.class
		};
	}
}
