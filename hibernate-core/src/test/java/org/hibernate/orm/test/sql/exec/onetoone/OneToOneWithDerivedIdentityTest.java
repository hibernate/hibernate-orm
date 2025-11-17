/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.onetoone;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				OneToOneWithDerivedIdentityTest.Person.class,
				OneToOneWithDerivedIdentityTest.PersonInfo.class
		}
)
@SessionFactory
public class OneToOneWithDerivedIdentityTest {

	private static final Integer PERSON_ID = 0;

	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					Person p = new Person();
					p.setId( PERSON_ID );
					p.setName( "Alfio" );
					PersonInfo pi = new PersonInfo();
					pi.setId( p );
					pi.setInfo( "Some information" );
					session.persist( p );
					session.persist( pi );

				} );

		scope.inTransaction(
				session -> {
					Person person = session.get( Person.class, PERSON_ID );
					assertEquals( "Alfio", person.getName() );
				} );
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Integer id;

		@Basic
		private String name;

		@OneToOne(mappedBy = "id")
		private PersonInfo personInfo;

		public Integer getId() {
			return this.id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public PersonInfo getPersonInfo() {
			return this.personInfo;
		}

		public void setPersonInfo(PersonInfo personInfo) {
			this.personInfo = personInfo;
		}
	}

	@Entity(name = "PersonInfo")
	public static class PersonInfo {
		@Id
		@OneToOne
		private Person id;

		@Basic
		private String info;

		public Person getId() {
			return this.id;
		}

		public void setId(Person id) {
			this.id = id;
		}

		public String getInfo() {
			return this.info;
		}

		public void setInfo(String info) {
			this.info = info;
		}
	}
}
