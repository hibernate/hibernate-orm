/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.OnDelete;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.annotations.OnDeleteAction.CASCADE;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
@Jpa(
		annotatedClasses = {
				CascadeOnDeleteTest.Person.class,
				CascadeOnDeleteTest.Phone.class
		}
)
public class CascadeOnDeleteTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person();
			person.setId( 1L );
			person.setName( "John Doe" );
			entityManager.persist( person );

			Phone phone = new Phone();
			phone.setId( 1L );
			phone.setNumber( "123-456-7890" );
			phone.setOwner( person );
			entityManager.persist( phone );
		} );

		scope.inTransaction( entityManager -> {
			//tag::pc-cascade-on-delete-example[]
			Person person = entityManager.find( Person.class, 1L );
			entityManager.remove( person );
			//end::pc-cascade-on-delete-example[]
		} );
	}

	//tag::pc-cascade-on-delete-mapping-Person-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		//Getters and setters are omitted for brevity

		//end::pc-cascade-on-delete-mapping-Person-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		//tag::pc-cascade-on-delete-mapping-Person-example[]
	}
	//end::pc-cascade-on-delete-mapping-Person-example[]

	//tag::pc-cascade-on-delete-mapping-Phone-example[]
	@Entity(name = "Phone")
	public static class Phone {

		@Id
		private Long id;

		@Column(name = "`number`")
		private String number;

		@ManyToOne(fetch = LAZY)
		@OnDelete(action = CASCADE)
		private Person owner;

		//Getters and setters are omitted for brevity

		//end::pc-cascade-on-delete-mapping-Phone-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public Person getOwner() {
			return owner;
		}

		public void setOwner(Person owner) {
			this.owner = owner;
		}
		//tag::pc-cascade-on-delete-mapping-Phone-example[]
	}
	//end::pc-cascade-on-delete-mapping-Phone-example[]
}
