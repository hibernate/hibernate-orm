/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetoone;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Martin Simka
 * @author Gail Badner
 */
@Jpa(annotatedClasses = {
		A.class,
		B.class
})
public class OneToOneOrphanTest {

	@Test
	@JiraKey(value = "HHH-9568")
	public void testFlushTransientOneToOneNoCascade(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();

					B b = new B();
					A a = new A();
					a.setB(b);

					try {
						entityManager.persist( a );
						entityManager.flush();
						entityManager.getTransaction().commit();
						fail( "should have raised an IllegalStateException" );
					}
					catch (IllegalStateException ex) {
						// IllegalStateException caught as expected
					}
					catch (Exception e) {
						// Re-throw any other Exception that might have happened
						throw e;
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
