/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-9864")
public class InsertOrderingWithBidirectionalManyToMany extends BaseInsertOrderingTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Address.class, Person.class };
	}

	@Test
	public void testBatching() {
		sessionFactoryScope().inTransaction( session -> {
			Person father = new Person();
			Person mother = new Person();
			Person son = new Person();
			Person daughter = new Person();

			Address home = new Address();
			Address office = new Address();

			home.addPerson( father );
			home.addPerson( mother );
			home.addPerson( son );
			home.addPerson( daughter );

			office.addPerson( father );
			office.addPerson( mother );

			session.persist( home );
			session.persist( office );

			clearBatches();
		} );

		verifyContainsBatches(
				new Batch( "insert into Address (street,ID) values (?,?)", 2 ),
				new Batch( "insert into Person (name,ID) values (?,?)", 4 ),
				new Batch( "insert into Person_Address (persons_ID,addresses_ID) values (?,?)", 6 )
		);
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;

		private String street;

		@ManyToMany(mappedBy = "addresses", cascade = CascadeType.PERSIST)
		private List<Person> persons = new ArrayList<>();

		public void addPerson(Person person) {
			persons.add( person );
			person.addresses.add( this );
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;

		private String name;

		@ManyToMany
		private List<Address> addresses = new ArrayList<>();
	}
}
