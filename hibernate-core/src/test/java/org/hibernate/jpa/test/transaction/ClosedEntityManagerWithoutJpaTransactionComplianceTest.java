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

import org.hamcrest.CoreMatchers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class ClosedEntityManagerWithoutJpaTransactionComplianceTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void shouldReturnNotNullValueTest() {
		EntityManager em = createEntityManager();
		em.close();
		assertThat( em.getTransaction(), CoreMatchers.notNullValue() );
	}

	@Test
	public void testCallTransactionIsActive() {
		EntityManager em = createEntityManager();
		em.close();
		assertFalse( em.getTransaction().isActive() );
	}

	@Test
	public void testCallTransactionIsActive2() {
		EntityManager em = createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		em.close();
		assertFalse( transaction.isActive() );
	}

	@Test(expected = IllegalStateException.class)
	public void testCallCommit() {
		EntityManager em = createEntityManager();
		em.close();
		em.getTransaction().commit();
	}

	@Test(expected = IllegalStateException.class)
	public void testCallCommit2() {
		EntityManager em = createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		em.close();
		transaction.commit();
	}

	@Test(expected = IllegalStateException.class)
	public void testCallBegin() {
		EntityManager em = createEntityManager();
		em.close();
		em.getTransaction().begin();
	}

	@Test(expected = IllegalStateException.class)
	public void testCallBegin2() {
		EntityManager em = createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		em.close();
		transaction.begin();
	}

	@Test
	public void testCallSetRollBackOnly() {
		EntityManager em = createEntityManager();
		em.close();
		em.getTransaction().setRollbackOnly();
	}

	@Test
	public void testCallSetRollBackOnly2() {
		EntityManager em = createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		em.close();
		transaction.setRollbackOnly();
	}

	@Test
	public void testCallRollBack() {
		EntityManager em = createEntityManager();
		em.close();
		em.getTransaction().rollback();
	}

	@Test
	public void testCallRollBack2() {
		EntityManager em = createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		em.close();
		transaction.rollback();
	}

	@Test
	public void testCallGetRollBackOnly() {
		EntityManager em = createEntityManager();
		em.close();
		assertFalse( em.getTransaction().getRollbackOnly() );
	}

	@Test
	public void testCallGetRollBackOnly2() {
		EntityManager em = createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		em.close();
		assertFalse( transaction.getRollbackOnly() );
	}
}
