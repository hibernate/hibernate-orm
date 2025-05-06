/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.fail;

@Jpa(
		annotatedClasses = { Address.class, Person.class, Farm.class }
)
public class ConverterExceptionsTest {
	@Test
	public void testPersisting(EntityManagerFactoryScope scope) {
		final EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager();
		try {
			entityManager.getTransaction().begin();
			Person b = new Person(
					1,
					"drea",
					new Address( "S. Egidtio", "Gradoli" )
			);
			entityManager.persist( b );
			entityManager.flush();
			entityManager.getTransaction().commit();
			fail( "Expected PersistenceException" );
		}
		catch (PersistenceException pe) {
			if ( !entityManager.getTransaction().getRollbackOnly() ) {
				fail( "Transaction was not marked for rollback" );
			}
		}
		catch (Exception ex) {
			fail( "Expected PersistenceException but thrown:", ex );
		}
		finally {
			try {
				if ( entityManager.getTransaction().isActive() ) {
					entityManager.getTransaction().rollback();
				}
			}
			finally {
				entityManager.close();
			}
		}
	}

	@Test
	public void testLoading(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Farm person = new Farm(
							1, "drea",
							new Address( "S. Egidtio", "Gradoli" )
					);
					entityManager.persist( person );
					entityManager.flush();
				}
		);

		final EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager();

		try {
			entityManager.getTransaction().begin();
			try {
				entityManager.find( Farm.class, 1 );
				fail( "PersistenceException expected" );
			}
			catch (PersistenceException pe) {
				if ( !entityManager.getTransaction().getRollbackOnly() ) {
					fail( "Transaction was not marked for rollback" );
				}
			}
			catch (Exception ex) {
				fail( "Expected PersistenceException but thrown:", ex );
			}
		}
		finally {
			try {
				if ( entityManager.getTransaction().isActive() ) {
					entityManager.getTransaction().rollback();
				}
			}
			finally {
				entityManager.close();
			}
		}
	}
}
