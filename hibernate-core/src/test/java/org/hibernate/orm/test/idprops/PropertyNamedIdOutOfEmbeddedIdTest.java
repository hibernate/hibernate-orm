/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idprops;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = PropertyNamedIdOutOfEmbeddedIdTest.Person.class
)
@SessionFactory
public class PropertyNamedIdOutOfEmbeddedIdTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Person( "John Doe", 0, 6 ) );
			session.persist( new Person( "John Doe", 1, 6 ) );
			session.persist( new Person( "Jane Doe", 0 ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from Person" ).executeUpdate()
		);
	}

	@Test
	@JiraKey(value = "HHH-13084")
	public void testHql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals(
					2,
					session.createQuery( "from Person p where p.id = :id", Person.class )
							.setParameter( "id", 6 )
							.list()
							.size()
			);

			assertEquals( 3L, session.createQuery( "select count( p ) from Person p" ).uniqueResult() );

		} );
	}

	@Entity(name = "Person")
	public static class Person implements Serializable {
		@EmbeddedId
		private PersonId personId;

		private Integer id;

		public Person(String name, int index) {
			this();
			personId = new PersonId( name, index );
		}

		public Person(String name, int index, Integer id) {
			this( name, index );
			this.id = id;
		}

		protected Person() {
			// this form used by Hibernate
		}

		public PersonId getPersonId() {
			return personId;
		}
	}

	@Embeddable
	public static class PersonId implements Serializable {
		private String name;
		@Column(name = "ind")
		private int index;

		public PersonId() {
		}

		public PersonId(String name, int index) {
			setName( name );
			setIndex( index );
		}

		public String getName() {
			return name;
		}

		public int getIndex() {
			return index;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			PersonId personId = (PersonId) o;

			if ( index != personId.index ) {
				return false;
			}
			return name.equals( personId.name );
		}

		@Override
		public int hashCode() {
			int result = name.hashCode();
			result = 31 * result + index;
			return result;
		}
	}
}
