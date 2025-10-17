/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.List;

import org.hibernate.annotations.processing.Exclude;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Exclude
@DomainModel(annotatedClasses = QuotedIdentifierTest.Person.class)
@SessionFactory
public class QuotedIdentifierTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var person = new Person( 1, "Chuck", "Norris" );
			session.persist( person );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testQuotedIdentifier(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			TypedQuery<Tuple> query = session.createQuery(
					"select `the person`.`name` as `The person name` " +
					"from `The Person` `the person`",
					Tuple.class
			);
			List<Tuple> resultList = query.getResultList();
			assertEquals( 1, resultList.size() );
			assertEquals( "Chuck", resultList.get( 0 ).get( "The person name" ) );
		} );
	}

	@Test
	public void testQuotedFunctionName(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			TypedQuery<Tuple> query = session.createQuery(
					"select `upper`(`the person`.`name`) as `The person name` " +
							"from `The Person` `the person`",
					Tuple.class
			);
			List<Tuple> resultList = query.getResultList();
			assertEquals( 1, resultList.size() );
			assertEquals( "CHUCK", resultList.get( 0 ).get( "The person name" ) );
		} );
	}

	@Entity(name = "The Person")
	public static class Person {
		@Id
		private Integer id;
		@Column(name = "the name")
		private String name;
		private String surname;

		public Person() {
		}

		public Person(Integer id, String name, String surname) {
			this.id = id;
			this.name = name;
			this.surname = surname;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getSurname() {
			return surname;
		}

		public void setSurname(String surname) {
			this.surname = surname;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof Person ) ) {
				return false;
			}

			Person person = (Person) o;

			return id != null ? id.equals( person.id ) : person.id == null;

		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}
	}

}
