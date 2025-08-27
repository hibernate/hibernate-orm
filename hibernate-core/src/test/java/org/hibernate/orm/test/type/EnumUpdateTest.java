/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = EnumUpdateTest.Person.class)
public class EnumUpdateTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Person( HairColor.BLACK ) );
			session.persist( new Person( HairColor.BROWN ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Person" ).executeUpdate() );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createMutationQuery( "update Person set hairColor = BROWN" ).executeUpdate()
		);
		scope.inTransaction( session -> {
			List<Person> resultList = session.createQuery( "from Person", Person.class ).getResultList();
			assertEquals( 2, resultList.size() );
			assertEquals( HairColor.BROWN, resultList.get( 0 ).getHairColor() );
			assertEquals( HairColor.BROWN, resultList.get( 1 ).getHairColor() );
		} );
	}

	public enum HairColor {
		BLACK, BLONDE, BROWN;
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;

		private HairColor hairColor;

		public Person() {
		}

		public Person(HairColor hairColor) {
			this.hairColor = hairColor;
		}

		public HairColor getHairColor() {
			return hairColor;
		}
	}
}
