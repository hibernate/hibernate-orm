/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.callbacks;

import java.util.Date;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.Cat;
import org.hibernate.jpa.test.Kitten;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sanne Grinovero
 */
@SuppressWarnings("unchecked")
@Jpa(
		annotatedClasses = {
				Cat.class,
				Kitten.class,
				Plant.class,
				Television.class,
				RemoteControl.class,
				Translation.class,
				Rythm.class
		},
		properties = { @Setting(name = AvailableSettings.JPA_CALLBACKS_ENABLED, value = "false") }
)
public class CallbacksDisabledTest {

	@Test
	public void testCallbacksAreDisabled(EntityManagerFactoryScope scope) throws Exception {
		int id = scope.fromTransaction(
				entityManager -> {
					Cat c = new Cat();
					c.setName( "Kitty" );
					c.setDateOfBirth( new Date( 90, 11, 15 ) );
					entityManager.persist( c );
					return c.getId();
				}
		);

		scope.inTransaction(
				entityManager -> {
					Cat _c = entityManager.find( Cat.class, id );
					assertTrue( _c.getAge() == 0 ); // With listeners enabled this would be false. Proven by org.hibernate.orm.test.jpa.callbacks.CallbacksTest.testCallbackMethod
				}
		);

		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Cat" ).executeUpdate();
				}
		);
	}
}
