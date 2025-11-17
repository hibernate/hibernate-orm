/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;

import static org.junit.jupiter.api.Assertions.fail;

@Jpa(
		properties = @Setting( name = AvailableSettings.JPA_TRANSACTION_COMPLIANCE, value = "true")
)
public class EntityManagerCreateQueryRuntimeExceptionTest {

	@Test
	public void testCriteriaDelete(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();

						final CriteriaDelete<NonEntityClass> deleteQuery = entityManager.getCriteriaBuilder()
								.createCriteriaDelete( NonEntityClass.class );
						deleteQuery.from( NonEntityClass.class );
						try {
							entityManager.createQuery( deleteQuery ).executeUpdate();
							fail( "Runtime Exception expected" );
						}
						catch (RuntimeException e) {
							// expected
							if ( !entityManager.getTransaction().getRollbackOnly() ) {
								fail( "Transaction was not marked for ", e );
							}
						}
					}
					catch (RuntimeException e) {
						// expected
					}
					finally {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
					}
				} );
	}

	@Test
	public void testCriteriaQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();

						CriteriaQuery<NonEntityClass> query = entityManager.getCriteriaBuilder()
								.createQuery( NonEntityClass.class );

						entityManager.createQuery( query ).executeUpdate();
						entityManager.getTransaction().commit();
						fail( "RuntimeException expected" );
					}
					catch (RuntimeException e) {
						//expected
						if ( !entityManager.getTransaction().getRollbackOnly() ) {
							fail( "Transaction was not marked for rollback", e );
						}
					}
					finally {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testHqlQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();

						entityManager.createQuery( "select" ).executeUpdate();

						entityManager.getTransaction().commit();
						fail( "RuntimeException expected" );
					}
					catch (RuntimeException e) {
						//expected
						if ( !entityManager.getTransaction().getRollbackOnly() ) {
							fail( "Transaction was not marked for rollback", e );
						}
					}
					finally {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
					}
				}
		);


	}

	public class NonEntityClass {
	}

}
