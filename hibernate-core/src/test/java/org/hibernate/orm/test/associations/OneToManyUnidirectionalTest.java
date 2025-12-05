/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				OneToManyUnidirectionalTest.Person.class,
				OneToManyUnidirectionalTest.Phone.class,
		}
)
public class OneToManyUnidirectionalTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::associations-one-to-many-unidirectional-lifecycle-example[]
			Person person = new Person();
			Phone phone1 = new Phone( "123-456-7890" );
			Phone phone2 = new Phone( "321-654-0987" );

			person.getPhones().add( phone1 );
			person.getPhones().add( phone2 );
			entityManager.persist( person );
			entityManager.flush();

			person.getPhones().remove( phone1 );
			//end::associations-one-to-many-unidirectional-lifecycle-example[]
		} );
	}

	//tag::associations-one-to-many-unidirectional-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Phone> phones = new ArrayList<>();

		//Getters and setters are omitted for brevity

		//end::associations-one-to-many-unidirectional-example[]

		public Person() {
		}

		public List<Phone> getPhones() {
			return phones;
		}

		//tag::associations-one-to-many-unidirectional-example[]
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`number`")
		private String number;

		//Getters and setters are omitted for brevity

		//end::associations-one-to-many-unidirectional-example[]

		public Phone() {
		}

		public Phone(String number) {
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public String getNumber() {
			return number;
		}
		//tag::associations-one-to-many-unidirectional-example[]
	}
	//end::associations-one-to-many-unidirectional-example[]
}
