/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class EnumerationCustomTypeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person( );
			person.setId( 1L );
			person.setName( "John Doe" );
			person.setGender( Gender.MALE );
			entityManager.persist( person );
		} );
	}

	//tag::basic-enums-custom-type-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		@Type( type = "org.hibernate.userguide.mapping.basic.GenderType" )
		public Gender gender;

		//Getters and setters are omitted for brevity

	//end::basic-enums-custom-type-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Gender getGender() {
			return gender;
		}

		public void setGender(Gender gender) {
			this.gender = gender;
		}
	//tag::basic-enums-custom-type-example[]
	}
	//end::basic-enums-custom-type-example[]
}
