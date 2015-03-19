/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.orphan.onetoone;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.fail;

/**
 * @author Martin Simka
 * @author Gail Badner
 */
public class OneToOneOrphanTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				A.class,
				B.class
		};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9568")
	public void testFlushTransientOneToOneNoCascade() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		B b = new B();
		A a = new A();

		a.setB(b);
		try {
			em.persist(a);
			em.flush();
			em.getTransaction().commit();
			fail("should have raised an IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// IllegalStateException caught as expected
		}
		em.close();
	}
}
