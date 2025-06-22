/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables.generics;

import org.hibernate.annotations.CompositeType;
import org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded.Name;
import org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded.NameCompositeUserType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		CompositeUserTypeInGenericSuperclassTest.GenericSuperclass.class,
		CompositeUserTypeInGenericSuperclassTest.TestEmbeddable.class,
		CompositeUserTypeInGenericSuperclassTest.TestEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17916" )
public class CompositeUserTypeInGenericSuperclassTest {
	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 1L );
			assertThat( result.getGenericEmbedded().getTestProp() ).isEqualTo( 1 );
			assertThat( result.getName().firstName() ).isEqualTo( "Marco" );
			assertThat( result.getName().lastName() ).isEqualTo( "Belladelli" );
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.createQuery(
					"from TestEntity where genericEmbedded.testProp = 2",
					TestEntity.class
			).getSingleResult();
			assertThat( result.getGenericEmbedded().getTestProp() ).isEqualTo( 2 );
			assertThat( result.getName().firstName() ).isEqualTo( "Andrea" );
			assertThat( result.getName().lastName() ).isEqualTo( "Boriero" );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, new Name( "Marco", "Belladelli" ), new TestEmbeddable( 1 ) ) );
			session.persist( new TestEntity( 2L, new Name( "Andrea", "Boriero" ), new TestEmbeddable( 2 ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate() );
	}

	@MappedSuperclass
	static class GenericSuperclass<T> {
		@Embedded
		@CompositeType( NameCompositeUserType.class )
		private Name name;

		@Embedded
		T genericEmbedded;

		public GenericSuperclass() {
		}

		public GenericSuperclass(Name name, T genericEmbedded) {
			this.name = name;
			this.genericEmbedded = genericEmbedded;
		}

		public Name getName() {
			return name;
		}

		public T getGenericEmbedded() {
			return genericEmbedded;
		}
	}

	@Embeddable
	static class TestEmbeddable {
		private Integer testProp;

		public TestEmbeddable() {
		}

		public TestEmbeddable(Integer testProp) {
			this.testProp = testProp;
		}

		public Integer getTestProp() {
			return testProp;
		}
	}

	@Entity( name = "TestEntity" )
	static class TestEntity extends GenericSuperclass<TestEmbeddable> {
		@Id
		private Long id;

		public TestEntity() {
		}

		public TestEntity(Long id, Name name, TestEmbeddable genericEmbedded) {
			super( name, genericEmbedded );
			this.id = id;
		}
	}
}
