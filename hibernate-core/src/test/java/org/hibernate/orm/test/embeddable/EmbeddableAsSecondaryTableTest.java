/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import jakarta.persistence.*;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-19542")
@DomainModel(annotatedClasses = {
		EmbeddableAsSecondaryTableTest.UserEntity.class
})
@SessionFactory
public class EmbeddableAsSecondaryTableTest {

	private UserEntity user;

	@BeforeAll
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person person = new Person( new FullName( "Sylvain", "Lecoy" ), 38 );
			user = new UserEntity( person );
			session.persist( user );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			UserEntity entity = session.find( UserEntity.class, user.id );
			assertThat( entity ).isNotNull();
			assertThat( entity.id ).isEqualTo( entity.id );
			assertThat( entity.person ).isNotNull();
			assertThat( entity.person.zAge ).isEqualTo( 38 );
			assertThat( entity.person.fullName.firstName ).isEqualTo( "Sylvain" );
			assertThat( entity.person.fullName.lastName ).isEqualTo( "Lecoy" );
		});
	}

	@Entity
	@SecondaryTable(name = "Person")
	public static class UserEntity {
		@Id
		@GeneratedValue
		private Integer id;
		private Person person;

		public UserEntity(
				final Person person) {
			this.person = person;
		}

		protected UserEntity() {

		}
	}

	@Embeddable
	public static class Person {

		@Column(table = "Person")
		private Integer zAge;

		private FullName fullName;

		public Person(final FullName fullName, final Integer age) {
			this.fullName = fullName;
			this.zAge = age;
		}

		protected Person() {
		}
	}

	@Embeddable
	public static class FullName {

		private String firstName;

		private String lastName;

		public FullName(final String firstName, final String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		protected FullName() {
		}
	}
}
