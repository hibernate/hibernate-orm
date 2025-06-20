/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idprops;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

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
		annotatedClasses = PropertyNamedIdOutOfNonJpaCompositeIdTest.Person.class
)
@SessionFactory
public class PropertyNamedIdOutOfNonJpaCompositeIdTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Person( "John Doe", 0 ) );
			session.persist( new Person( "John Doe", 1, 1 ) );
			session.persist( new Person( "John Doe", 2, 2 ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testHql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 1, session.createQuery( "from Person p where p.id = 1", Person.class ).list().size() );

			assertEquals( 3L, session.createQuery( "select count( p ) from Person p" ).uniqueResult() );
		} );
	}

	@Entity(name = "Person")
	public static class Person implements Serializable {
		@Id
		private String name;

		@Id
		@Column(name = "ind")
		private int index;

		private Integer id;

		public Person(String name, int index) {
			this();
			setName( name );
			setIndex( index );
		}


		public Person(String name, int index, int id) {
			this( name, index );
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public int getIndex() {
			return index;
		}

		public Integer getId() {
			return id;
		}

		protected Person() {
			// this form used by Hibernate
		}

		protected void setName(String name) {
			this.name = name;
		}

		protected void setIndex(int index) {
			this.index = index;
		}

		protected void setId(Integer id) {
			this.id = id;
		}
	}

	public static class PersonId implements Serializable {
		private String name;
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
