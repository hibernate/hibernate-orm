/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;


/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				ManyToOneTest.Person.class,
				ManyToOneTest.Phone.class,
		}
)
public class ManyToOneTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::associations-many-to-one-lifecycle-example[]
			Person person = new Person();
			entityManager.persist( person );

			Phone phone = new Phone( "123-456-7890" );
			phone.setPerson( person );
			entityManager.persist( phone );

			entityManager.flush();
			phone.setPerson( null );
			//end::associations-many-to-one-lifecycle-example[]
		} );
	}

	//tag::associations-many-to-one-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		//Getters and setters are omitted for brevity

	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`number`")
		private String number;

		@ManyToOne
		@JoinColumn(name = "person_id",
				foreignKey = @ForeignKey(name = "PERSON_ID_FK")
		)
		private Person person;

		//Getters and setters are omitted for brevity

		//end::associations-many-to-one-example[]

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

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
		//tag::associations-many-to-one-example[]
	}
	//end::associations-many-to-one-example[]
}
