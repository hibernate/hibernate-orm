/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.transaction;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
public class GetTransactionTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testMultipleCallsReturnTheSameTransaction() {
		EntityManager em = createEntityManager();
		EntityTransaction t = em.getTransaction();
		assertSame( t, em.getTransaction() );
		assertFalse( t.isActive() );
		t.begin();
		assertSame( t, em.getTransaction() );
		assertTrue( t.isActive() );
		t.commit();
		assertSame( t, em.getTransaction() );
		assertFalse( t.isActive() );
		em.close();
		assertSame( t, em.getTransaction() );
		assertFalse( t.isActive() );
	}
}

