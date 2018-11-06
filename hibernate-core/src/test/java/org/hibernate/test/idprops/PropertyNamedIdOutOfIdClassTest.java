/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idprops;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public class PropertyNamedIdOutOfIdClassTest extends BaseCoreFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {
			session.persist( new Person( "John Doe", 0 ) );
			session.persist( new Person( "John Doe", 1, 1 ) );
			session.persist( new Person( "John Doe", 2, 2 ) );
		} );
	}


	@Test
	@TestForIssue(jiraKey = "HHH-13084")
	public void testHql() {
		doInHibernate( this::sessionFactory, session -> {
			assertEquals( 1, session.createQuery( "from Person p where p.id is null", Person.class ).list().size() );
			assertEquals( 2, session.createQuery( "from Person p where p.id is not null", Person.class ).list().size() );
			assertEquals( 3L, session.createQuery( "select count( p ) from Person p" ).uniqueResult() );
		} );
	}

	@Entity(name = "Person")
	@IdClass(PersonId.class)
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
