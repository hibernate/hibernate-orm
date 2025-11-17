/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
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
				CascadeOnDeleteCollectionTest.Person.class,
				CascadeOnDeleteCollectionTest.Phone.class
		}
)
public class CascadeOnDeleteCollectionTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person();
			person.setId( 1L );
			person.setName( "John Doe" );
			entityManager.persist( person );

			Phone phone1 = new Phone();
			phone1.setId( 1L );
			phone1.setNumber( "123-456-7890" );
			phone1.setOwner( person );
			person.addPhone( phone1 );

			Phone phone2 = new Phone();
			phone2.setId( 2L );
			phone2.setNumber( "101-010-1234" );
			phone2.setOwner( person );
			person.addPhone( phone2 );
		} );

		scope.inTransaction( entityManager -> {
			//tag::pc-cascade-on-delete-collection-example[]
			Person person = entityManager.find( Person.class, 1L );
			entityManager.remove( person );
			//end::pc-cascade-on-delete-collection-example[]
		} );
	}

	//tag::pc-cascade-on-delete-collection-mapping-Person-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
		@OnDelete(action = OnDeleteAction.CASCADE)
		private List<Phone> phones = new ArrayList<>();

		//Getters and setters are omitted for brevity

		//end::pc-cascade-on-delete-collection-mapping-Person-example[]

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

		public void addPhone(Phone phone) {
			phone.setOwner( this );
			phones.add( phone );
		}

		//tag::pc-cascade-on-delete-collection-mapping-Person-example[]
	}
	//end::pc-cascade-on-delete-collection-mapping-Person-example[]

	//tag::pc-cascade-on-delete-collection-mapping-Phone-example[]
	@Entity(name = "Phone")
	public static class Phone {

		@Id
		private Long id;

		@Column(name = "`number`")
		private String number;

		@ManyToOne(fetch = FetchType.LAZY)
		private Person owner;

		//Getters and setters are omitted for brevity

		//end::pc-cascade-on-delete-collection-mapping-Phone-example[]

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
		//tag::pc-cascade-on-delete-collection-mapping-Phone-example[]
	}
	//end::pc-cascade-on-delete-collection-mapping-Phone-example[]
}
