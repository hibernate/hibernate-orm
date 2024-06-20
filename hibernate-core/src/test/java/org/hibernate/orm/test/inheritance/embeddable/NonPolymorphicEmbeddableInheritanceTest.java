/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Test use of inheritance in embeddables as it was possible in Hibernate ORM 6.5 and below,
 * i.e. without any polymorphism or discriminators: you could have an embeddable extend another,
 * but an `@Embedded` attribute would only accept values of its declared type (not a subtype).
 */
@DomainModel(annotatedClasses = {
		NonPolymorphicEmbeddableInheritanceTest.TestEntity1.class,
		NonPolymorphicEmbeddableInheritanceTest.TestEntity2.class,
		NonPolymorphicEmbeddableInheritanceTest.BaseEmbeddable.class,
		NonPolymorphicEmbeddableInheritanceTest.ExtendedEmbeddable.class
})
@SessionFactory
public class NonPolymorphicEmbeddableInheritanceTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity1( 1L, new BaseEmbeddable( "base_1" ) ) );
			session.persist( new TestEntity2( 2L, new ExtendedEmbeddable( "extended_1", "extended_2" ) ) );
		} );
		scope.inTransaction( session -> {
			TestEntity1 entity1 = session.find( TestEntity1.class, 1L );
			assertThat( entity1 ).isNotNull();
			assertThat( entity1.getBaseEmbeddable() )
					.isNotNull()
					.returns( "base_1", BaseEmbeddable::getBaseText );
			TestEntity2 entity2 = session.find( TestEntity2.class, 2L );
			assertThat( entity2 ).isNotNull();
			assertThat( entity2.getExtendedEmbeddable() )
					.isNotNull()
					.returns( "extended_1", BaseEmbeddable::getBaseText )
					.returns( "extended_2", ExtendedEmbeddable::getExtendedText );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity1" ).executeUpdate() );
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity2" ).executeUpdate() );
	}

	@Entity(name = "TestEntity1")
	static class TestEntity1 {
		@Id
		private Long id;

		@Embedded
		private BaseEmbeddable baseEmbeddable;

		public TestEntity1() {
		}

		public TestEntity1(long id, BaseEmbeddable baseEmbeddable) {
			this.id = id;
			this.baseEmbeddable = baseEmbeddable;
		}

		public BaseEmbeddable getBaseEmbeddable() {
			return baseEmbeddable;
		}

		public void setBaseEmbeddable(BaseEmbeddable baseEmbeddable) {
			this.baseEmbeddable = baseEmbeddable;
		}
	}

	@Entity(name = "TestEntity2")
	static class TestEntity2 {
		@Id
		private Long id;

		@Embedded
		private ExtendedEmbeddable extendedEmbeddable;

		public TestEntity2() {
		}

		public TestEntity2(long id, ExtendedEmbeddable extendedEmbeddable) {
			this.id = id;
			this.extendedEmbeddable = extendedEmbeddable;
		}

		public ExtendedEmbeddable getExtendedEmbeddable() {
			return extendedEmbeddable;
		}

		public void setExtendedEmbeddable(ExtendedEmbeddable extendedEmbeddable) {
			this.extendedEmbeddable = extendedEmbeddable;
		}
	}

	@Embeddable
	@MappedSuperclass
	public static class BaseEmbeddable {
		@Basic
		private String baseText;

		public BaseEmbeddable() {
		}

		public BaseEmbeddable(String baseText) {
			this.baseText = baseText;
		}

		public String getBaseText() {
			return baseText;
		}

		public void setBaseText(String baseText) {
			this.baseText = baseText;
		}
	}

	@Embeddable
	public static class ExtendedEmbeddable extends BaseEmbeddable {
		@Basic
		private String extendedText;

		public ExtendedEmbeddable() {
		}

		public ExtendedEmbeddable(String baseText, String extendedText) {
			super( baseText );
			this.extendedText = extendedText;
		}

		public String getExtendedText() {
			return extendedText;
		}

		public void setExtendedText(String extendedText) {
			this.extendedText = extendedText;
		}
	}
}
