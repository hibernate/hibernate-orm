/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ops;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class ContainsTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
		};
	}

	@Test
	public void testLifecycle() {
		Person _person = doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.id = 1L;
			person.name = "John Doe";
			entityManager.persist( person );

			assertTrue(entityManager.contains( person ));

			return person;
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			assertFalse(entityManager.contains( _person ));

			Person person = entityManager.find( Person.class, 1L );

			assertTrue(entityManager.contains( person ));
		} );
	}

	@Entity(name = "PersonEntity")
	public static class Person {

		@Id
		private Long id;

		private String name;
	}

}
