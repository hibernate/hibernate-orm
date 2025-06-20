/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks;

import java.util.Date;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.Cat;
import org.hibernate.orm.test.jpa.Kitten;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sanne Grinovero
 */
@Jpa(
		annotatedClasses = {
				Cat.class,
				Kitten.class
		},
		properties = { @Setting(name = AvailableSettings.JPA_CALLBACKS_ENABLED, value = "false") }
)
public class CallbacksDisabledTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCallbacksAreDisabled(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Cat c = new Cat();
					c.setName( "Kitty" );
					c.setDateOfBirth( new Date( 90, 11, 15 ) );
					entityManager.persist( c );
					entityManager.getTransaction().commit();
					entityManager.clear();
					entityManager.getTransaction().begin();
					Cat _c = entityManager.find( Cat.class, c.getId() );
					assertTrue( _c.getAge() == 0 ); // With listeners enabled this would be false. Proven by org.hibernate.orm.test.jpa.callbacks.CallbacksTest.testCallbackMethod
				}
		);
	}
}
