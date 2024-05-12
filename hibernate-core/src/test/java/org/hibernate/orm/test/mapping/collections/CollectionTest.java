/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Christian Beikov
 */
@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@DomainModel(annotatedClasses = CollectionTest.Person.class)
@SessionFactory
public class CollectionTest {

	@Test
	public void testLifecycle(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person(1L);
			List<String> phones = new ArrayList<>();
			phones.add( "028-234-9876" );
			phones.add( "072-122-9876" );
			person.setPhones(phones);
			entityManager.persist(person);
		});
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			List<String> phones = new ArrayList<>();
			phones.add( "072-122-9876" );
			person.setPhones(phones);
		});
	}

	//tag::collections-as-basic-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private List<String> phones;

		//Getters and setters are omitted for brevity

	//end::collections-as-basic-example[]

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public List<String> getPhones() {
			return phones;
		}

		public void setPhones(List<String> phones) {
			this.phones = phones;
		}
	//tag::collections-as-basic-example[]
	}
	//end::collections-as-basic-example[]
}
