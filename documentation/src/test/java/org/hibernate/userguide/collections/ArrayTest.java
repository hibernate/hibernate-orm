/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class ArrayTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person( 1L );
			String[] phones = new String[2];
			phones[0] = "028-234-9876";
			phones[1] = "072-122-9876";
			person.setPhones( phones );
			entityManager.persist( person );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			String[] phones = new String[1];
			phones[0] = "072-122-9876";
			person.setPhones( phones );
		} );
	}

	//tag::collections-array-binary-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;
		private String[] phones;

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public String[] getPhones() {
			return phones;
		}

		public void setPhones(String[] phones) {
			this.phones = phones;
		}
	}
	//end::collections-array-binary-example[]
}
