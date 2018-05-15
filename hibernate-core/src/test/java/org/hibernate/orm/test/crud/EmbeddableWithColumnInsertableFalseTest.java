/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

/**
 * @author Andrea Boriero
 */
public class EmbeddableWithColumnInsertableFalseTest
		extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Person.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@BeforeEach
	public void setUp() {
		sessionFactoryScope().inTransaction(
				session -> {
					Name name = new Name( "Fabiana", "Fab" );
					Person person = new Person( 1, name, 33 );
					session.save( person );
				} );
	}

	@AfterEach
	public void tearDown() {
		sessionFactoryScope().inTransaction(
				session -> {
					session.createQuery( "from Person p" )
							.list()
							.forEach( person -> session.delete( person ) );
				} );
	}

	@Test
	public void testSaving() {
		sessionFactoryScope().inTransaction(
				session -> {
					Person person = session.get( Person.class, 1 );
					assertThat( person.getName().getSecondName(), nullValue() );
					assertThat( person.getName().getFirstName(), is( "Fabiana" ) );
				} );
	}

	@Test
	public void testUpdating() {
		sessionFactoryScope().inTransaction(
				session -> {
					Person person = session.get( Person.class, 1 );
					assertThat( person.getName().getSecondName(), nullValue() );
					assertThat( person.getName().getFirstName(), is( "Fabiana" ) );
				} );

		sessionFactoryScope().inTransaction(
				session -> {
					Person person = session.get( Person.class, 1 );
					person.setAge( 34 );
					Name name = person.getName();
					name.setSecondName( "Fabi" );
					name.setFirstName( "Fabi" );
				} );

		sessionFactoryScope().inTransaction(
				session -> {
					Person person = session.get( Person.class, 1 );
					Name name = person.getName();
					assertThat( name.getSecondName(), nullValue() );
					assertThat( name.getFirstName(), is( "Fabi" ) );
				} );
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Integer id;

		private Name name;

		private Integer age;

		public Person() {
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		public Person(Integer id, Name name, Integer age) {
			this.id = id;
			this.name = name;
			this.age = age;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Name getName() {
			return name;
		}

		public void setName(Name name) {
			this.name = name;
		}

		public Integer getAge() {
			return age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}
	}


	@Embeddable
	public static class Name {
		private String firstName;
		@Column(insertable = false, updatable = false)
		private String secondName;

		public Name() {
		}

		public Name(String firstName, String secondName) {
			this.firstName = firstName;
			this.secondName = secondName;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getSecondName() {
			return secondName;
		}

		public void setSecondName(String secondName) {
			this.secondName = secondName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}
	}
}
