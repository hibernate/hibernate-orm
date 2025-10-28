/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idprops;

import java.io.Serializable;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = PropertyNamedIdInEmbeddedIdTest.Person.class
)
@SessionFactory
public class PropertyNamedIdInEmbeddedIdTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Person( "John Doe", 0 ) );
			session.persist( new Person( "John Doe", 1 ) );
			session.persist( new Person( "Jane Doe", 0 ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-13084")
	public void testHql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			assertEquals(
					1,
					session.createQuery( "from Person p where p.id = :id", Person.class )
							.setParameter( "id", new PersonId( "John Doe", 0 ) )
							.list()
							.size()
			);

			assertEquals(
					2,
					session.createQuery( "from Person p where p.id.id = :id", Person.class )
							.setParameter( "id", 0 )
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

		public Person(String name, int id) {
			this();
			personId = new PersonId( name, id );
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
		private Integer id;

		public PersonId() {
		}

		public PersonId(String name, int id) {
			setName( name );
			setId( id );
		}

		public String getName() {
			return name;
		}

		public Integer getId() {
			return id;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setId(Integer id) {
			this.id = id;
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

			if ( id != personId.id ) {
				return false;
			}
			return name.equals( personId.name );
		}

		@Override
		public int hashCode() {
			int result = name.hashCode();
			result = 31 * result + id;
			return result;
		}
	}
}
