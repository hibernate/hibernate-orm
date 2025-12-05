/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {EmbeddableTypeElementCollectionTest.Person.class} )
public class EmbeddableTypeElementCollectionTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person();
			person.id = 1L;
			//tag::collections-embeddable-type-collection-lifecycle-example[]
			person.getPhones().add(new Phone("landline", "028-234-9876"));
			person.getPhones().add(new Phone("mobile", "072-122-9876"));
			//end::collections-embeddable-type-collection-lifecycle-example[]
			entityManager.persist(person);
		});
	}

	@Table( name = "persons" )
	//tag::ex-collection-elemental-basic-model[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@ElementCollection
		private List<Phone> phones = new ArrayList<>();

		//Getters and setters are omitted for brevity

	//end::ex-collection-elemental-basic-model[]

		public List<Phone> getPhones() {
			return phones;
		}
	//tag::ex-collection-elemental-basic-model[]
	}

	@Embeddable
	public static class Phone {

		private String type;

		@Column(name = "`number`")
		private String number;

		//Getters and setters are omitted for brevity

	//end::ex-collection-elemental-basic-model[]

		public Phone() {
		}

		public Phone(String type, String number) {
			this.type = type;
			this.number = number;
		}

		public String getType() {
			return type;
		}

		public String getNumber() {
			return number;
		}
	//tag::ex-collection-elemental-basic-model[]
	}
	//end::ex-collection-elemental-basic-model[]
}
