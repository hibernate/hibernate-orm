/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ops;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {
		Mammal.class,
		Reptile.class,
		Animal.class
})
public class FindTest {
	@Test
	public void testSubclassWrongId(EntityManagerFactoryScope scope) {
		Mammal mammal = new Mammal();
		mammal.setMamalNbr( 2 );
		mammal.setName( "Human" );
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						entityManager.persist( mammal );
						entityManager.flush();
						Assertions.assertNull( entityManager.find( Reptile.class, 1l ) );
						entityManager.getTransaction().rollback();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9856")
	public void testNonEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						entityManager.find( String.class, 1 );
						Assertions.fail( "Expecting a failure" );
					}
					catch (IllegalArgumentException ignore) {
						// expected
					}
					finally {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
					}
				}
		);
	}
}
