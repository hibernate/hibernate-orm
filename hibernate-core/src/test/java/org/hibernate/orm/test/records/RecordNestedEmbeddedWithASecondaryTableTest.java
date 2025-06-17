/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.records;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@JiraKey("HHH-19542")
@DomainModel(annotatedClasses = {
		RecordNestedEmbeddedWithASecondaryTableTest.UserEntity.class
})
@SessionFactory
class RecordNestedEmbeddedWithASecondaryTableTest {

	private UserEntity user;

	@BeforeAll
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person person = new Person( new FullName( "Sylvain", "Lecoy" ), 38 );
			user = new UserEntity( person );
			session.persist( user );
		} );
	}

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			UserEntity entity = session.find( UserEntity.class, user.id );
			assertThat( entity ).isNotNull();
			assertThat( entity.id ).isEqualTo( user.id );
			assertThat( entity.person ).isNotNull();
			assertThat( entity.person.age ).isEqualTo( 38 );
			assertThat( entity.person.fullName.firstName ).isEqualTo( "Sylvain" );
			assertThat( entity.person.fullName.lastName ).isEqualTo( "Lecoy" );
		});
	}

	@Test
	void test(ServiceRegistryScope scope) {
		final StandardServiceRegistry registry = scope.getRegistry();
		final MetadataSources sources = new MetadataSources( registry ).addAnnotatedClass( UserEntity1.class );

		try {
			sources.buildMetadata();
			fail( "Expecting to fail" );
		} catch (AnnotationException expected) {
			assertThat( expected ).hasMessageContaining( "all properties of the embeddable class must map to the same table" );
		}
	}

	@Entity
	@Table(name = "UserEntity")
	@SecondaryTable(name = "Person")
	static class UserEntity {
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
	record Person(
			FullName fullName,
			@Column(table = "Person")
			Integer age) {

	}

	@Embeddable
	record FullName(
			@Column(table = "Person")
			String firstName,
			@Column(table = "Person")
			String lastName) {

	}

	@Entity
	@Table(name = "UserEntity")
	@SecondaryTable(name = "Person")
	public static class UserEntity1 {
		@Id
		@GeneratedValue
		private Integer id;
		private Person1 person;

		public UserEntity1(
				final Person1 person) {
			this.person = person;
		}

		protected UserEntity1() {

		}
	}

	@Embeddable
	public record Person1(
			FullName1 fullName,
			@Column(table = "Person")
			Integer age) {

	}

	@Embeddable
	public record FullName1(
			@Column(table = "Person")
			String firstName,
			String lastName) {

	}
}
