/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-9864")
public class InsertOrderingWithManyToOne extends BaseInsertOrderingTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Address.class, Person.class };
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

			father.address = home;
			son.address = home;
			mother.address = office;
			daughter.address = office;

			session.persist( home );
			session.persist( office );

			session.persist( father );
			session.persist( mother );
			session.persist( son );
			session.persist( daughter );

			clearBatches();
		} );

		verifyContainsBatches(
				new Batch( "insert into Address (street,ID) values (?,?)", 2 ),
				new Batch( "insert into Person (address_ID,name,ID) values (?,?,?)", 4 )
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
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;

		private String name;

		@ManyToOne
		private Address address;
	}
}
