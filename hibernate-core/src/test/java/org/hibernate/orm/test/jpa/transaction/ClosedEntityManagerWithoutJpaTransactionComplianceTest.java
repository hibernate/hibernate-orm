/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.transaction;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Andrea Boriero
 */
@Jpa
public class ClosedEntityManagerWithoutJpaTransactionComplianceTest {
	@Test
	public void shouldReturnNotNullValueTest(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		em.close();
		assertThat( em.getTransaction(), CoreMatchers.notNullValue() );
	}

	@Test
	public void testCallTransactionIsActive(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		em.close();
		assertFalse( em.getTransaction().isActive() );
	}

	@Test
	public void testCallTransactionIsActive2(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		em.close();
		assertFalse( transaction.isActive() );
	}

	@Test
	public void testCallCommit(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		em.close();
		assertThrows( IllegalStateException.class, () -> em.getTransaction().commit() );
	}

	@Test
	public void testCallCommit2(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		em.close();
		assertThrows( IllegalStateException.class, transaction::commit );
	}

	@Test
	public void testCallBegin(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		em.close();
		assertThrows( IllegalStateException.class, () -> em.getTransaction().begin() );
	}

	@Test
	public void testCallBegin2(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		em.close();
		assertThrows( IllegalStateException.class, transaction::begin );
	}

	@Test
	public void testCallSetRollBackOnly(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		em.close();
		em.getTransaction().setRollbackOnly();
	}

	@Test
	public void testCallSetRollBackOnly2(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		em.close();
		transaction.setRollbackOnly();
	}

	@Test
	public void testCallRollBack(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		em.close();
		em.getTransaction().rollback();
	}

	@Test
	public void testCallRollBack2(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		em.close();
		transaction.rollback();
	}

	@Test
	public void testCallGetRollBackOnly(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		em.close();
		assertFalse( em.getTransaction().getRollbackOnly() );
	}

	@Test
	public void testCallGetRollBackOnly2(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		em.close();
		assertFalse( transaction.getRollbackOnly() );
	}
}
