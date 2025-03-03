/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = EnumSingleCharTest.Person.class)
@JiraKey("HHH-17106")
public class EnumSingleCharTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Person( 1L, SinglecharEnum.B ) );
			session.persist( new Person( 2L, SinglecharEnum.R ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Person" ).executeUpdate() );
	}

	@Test
	public void testRead(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Person> resultList = session.createQuery( "from Person order by id", Person.class ).getResultList();
			assertEquals( 2, resultList.size() );
			assertEquals( SinglecharEnum.B, resultList.get( 0 ).getSingleChar() );
			assertEquals( SinglecharEnum.R, resultList.get( 1 ).getSingleChar() );
		} );
	}

	public enum SinglecharEnum {
		B("first"),
		R("second"),
		O("third"),
		K("fourth"),
		E("fifth"),
		N("sixth");

		private final String item;

		SinglecharEnum(String item) {
			this.item = item;
		}

		public String getItem() {
			return item;
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Long id;

		@Column(name="SINGLE_CHAR", length = 1)
		@Enumerated(EnumType.STRING)
		private SinglecharEnum singleChar;

		public Person() {
		}

		public Person(Long id, SinglecharEnum singleChar) {
			this.id = id;
			this.singleChar = singleChar;
		}

		public SinglecharEnum getSingleChar() {
			return singleChar;
		}
	}
}
