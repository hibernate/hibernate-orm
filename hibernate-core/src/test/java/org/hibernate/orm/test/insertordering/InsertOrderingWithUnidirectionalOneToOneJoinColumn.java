/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12569")
public class InsertOrderingWithUnidirectionalOneToOneJoinColumn extends BaseInsertOrderingTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, Address.class };
	}

	@Test
	public void testBatchingWithEmbeddableId() {
		sessionFactoryScope().inTransaction( session -> {
			final PersonAddressId id = new PersonAddressId();
			id.setId( 1 );

			Person person = new Person();
			person.setId( id );
			session.persist( person );

			Address address = new Address();
			address.setId( id );
			address.setPerson( person );
			session.persist( address );

			clearBatches();
		} );

		verifyContainsBatches(
				new Batch( "insert into Person (name,id) values (?,?)" ),
				new Batch( "insert into Address (street,id) values (?,?)" )
		);

		sessionFactoryScope().inTransaction( session -> {
			Query query = session.createQuery( "FROM Person" );
			assertEquals( 1, query.getResultList().size() );
		} );
	}

	@Embeddable
	public static class PersonAddressId implements Serializable {
		@Column(name = "id")
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@EmbeddedId
		private PersonAddressId id;

		private String name;

		public PersonAddressId getId() {
			return id;
		}

		public void setId(PersonAddressId id) {
			this.id = id;
		}
	}

	@Entity(name = "Address")
	public static class Address {
		@EmbeddedId
		private PersonAddressId id;

		@OneToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
		private Person person;

		private String street;

		public PersonAddressId getId() {
			return id;
		}

		public void setId(PersonAddressId id) {
			this.id = id;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}

}
