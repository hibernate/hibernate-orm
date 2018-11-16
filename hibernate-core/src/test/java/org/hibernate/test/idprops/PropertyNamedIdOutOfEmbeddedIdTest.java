/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idprops;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public class PropertyNamedIdOutOfEmbeddedIdTest extends BaseCoreFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13084")
	public void testHql() {
		Session s = openSession();
		s.beginTransaction();
		s.persist( new Person( "John Doe", 0, 6 ) );
		s.persist( new Person( "John Doe", 1, 6 ) );
		s.persist( new Person( "Jane Doe", 0 ) );
		s.flush();

		assertEquals(
				2, s.createQuery( "from Person p where p.id = :id" )
						.setParameter( "id", 6 )
						.list()
						.size()
		);

		s.createQuery( "delete from Person" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
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
