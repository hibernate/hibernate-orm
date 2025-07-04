/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey("HHH-18876")
@DomainModel(
		annotatedClasses = {
				OrderColumnListIndexArrayInitializerTest.Person.class,
				OrderColumnListIndexArrayInitializerTest.Phone.class,
		}
)
@SessionFactory
class OrderColumnListIndexArrayInitializerTest {

	@Test
	void hhh18876Test(SessionFactoryScope scope) {

		// prepare data
		scope.inTransaction( session -> {

			// person
			Person person = new Person();
			person.id = 1L;
			session.persist( person );

			// add phone
			Phone phone = new Phone();
			phone.id = 1L;
			phone.person = person;

			person.phones = new Phone[1];
			person.phones[0] = phone;

			// add children
			Person children = new Person();
			children.id = 2L;
			children.mother = person;

			person.children = new Person[1];
			person.children[0] = children;
		} );

		// load and assert
		scope.inTransaction( session -> {
			Person person = session.createSelectionQuery( "select p from Person p where id=1", Person.class )
					.getSingleResult();
			assertNotNull( person );
			assertEquals( 1, person.phones.length );
			assertNotNull( person.phones[0] );
			assertEquals( 1, person.children.length );
			assertEquals( person, person.children[0].mother );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		Long id;

		@OneToMany(fetch = FetchType.EAGER, mappedBy = "person", cascade = CascadeType.ALL)
		@OrderColumn(name = "order_id")
		@ListIndexBase(100)
		Phone[] phones;

		@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@JoinTable(name = "parent_child_relationships", joinColumns = @JoinColumn(name = "parent_id"),
				inverseJoinColumns = @JoinColumn(name = "child_id"))
		@OrderColumn(name = "pos")
		@ListIndexBase(200)
		Person[] children;

		@ManyToOne
		@JoinColumn(name = "mother_id")
		Person mother;
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		Long id;

		@ManyToOne
		Person person;
	}
}
