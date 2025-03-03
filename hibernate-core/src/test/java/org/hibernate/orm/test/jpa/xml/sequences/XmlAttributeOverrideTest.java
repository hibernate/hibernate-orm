/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.xml.sequences;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@Jpa(
		xmlMappings = {"org/hibernate/orm/test/jpa/xml/sequences/orm3.xml"}
)
public class XmlAttributeOverrideTest {
	@Test
	public void testAttributeOverriding(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();

						Employee e = new Employee();
						e.setId( 100L );
						e.setName("Bubba");
						e.setHomeAddress(new Address("123 Main St", "New York", "NY", "11111"));
						e.setMailAddress(new Address("P.O. Box 123", "New York", "NY", "11111"));

						entityManager.persist(e);

						entityManager.flush();

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
}
