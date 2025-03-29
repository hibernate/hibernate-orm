/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel( annotatedClasses = {
		InheritanceManyToOneEmbeddedBatchingTest.Parent.class,
		InheritanceManyToOneEmbeddedBatchingTest.ChildA.class,
		InheritanceManyToOneEmbeddedBatchingTest.ChildB.class,
		InheritanceManyToOneEmbeddedBatchingTest.ChildC.class,
		InheritanceManyToOneEmbeddedBatchingTest.ChildD.class
} )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "10" ) )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16248" )
public class InheritanceManyToOneEmbeddedBatchingTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ChildA childA = new ChildA( 1L, null );
			final ChildB childB = new ChildB( 2L, new ReferenceEmbeddable( childA ) );
			final ChildC childC = new ChildC( 3L, new AnotherReferenceEmbeddable( childA ) );
			final ChildD childD = new ChildD( 4L, new AnotherReferenceEmbeddable( childA ) );
			session.persist( childA );
			session.persist( childB );
			session.persist( childC );
			session.persist( childD );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ChildD" ).executeUpdate();
			session.createMutationQuery( "delete from ChildC" ).executeUpdate();
			session.createMutationQuery( "delete from ChildB" ).executeUpdate();
			session.createMutationQuery( "delete from ChildA" ).executeUpdate();
		} );
	}

	@Test
	public void testSamePropertyName(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent result = session.createQuery(
					"from Parent where id = 2",
					Parent.class
			).getSingleResult();
			assertThat( result ).isInstanceOf( ChildB.class );
			final ChildB resultB = (ChildB) result;
			assertThat( resultB.getEmbeddable().getReference() ).isInstanceOf( ChildA.class );
			assertThat( resultB.getEmbeddable().getReference().getId() ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testDifferentPropertyName(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent result = session.createQuery(
					"from Parent where id = 3",
					Parent.class
			).getSingleResult();
			assertThat( result ).isInstanceOf( ChildC.class );
			final ChildC resultC = (ChildC) result;
			assertThat( resultC.getAnotherEmbeddable().getAnotherReference() ).isInstanceOf( ChildA.class );
			assertThat( resultC.getAnotherEmbeddable().getAnotherReference().getId() ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testSamePropertyNameDifferentEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent result = session.createQuery(
					"from Parent where id = 4",
					Parent.class
			).getSingleResult();
			assertThat( result ).isInstanceOf( ChildD.class );
			final ChildD resultD = (ChildD) result;
			assertThat( resultD.getEmbeddable().getAnotherReference() ).isInstanceOf( ChildA.class );
			assertThat( resultD.getEmbeddable().getAnotherReference().getId() ).isEqualTo( 1L );
		} );
	}

	@Entity( name = "Parent" )
	@DiscriminatorValue( "X" )
	@DiscriminatorColumn( name = "discriminator", discriminatorType = DiscriminatorType.CHAR )
	public static class Parent {
		@Id
		private Long id;

		public Parent() {
		}

		public Parent(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@Embeddable
	public static class ReferenceEmbeddable {
		@ManyToOne
		@JoinColumn( name = "reference_id" )
		private Parent reference;

		public ReferenceEmbeddable() {
		}

		public ReferenceEmbeddable(Parent reference) {
			this.reference = reference;
		}

		public Parent getReference() {
			return reference;
		}
	}

	@Entity( name = "ChildA" )
	@DiscriminatorValue( "A" )
	public static class ChildA extends Parent {
		@Embedded
		private ReferenceEmbeddable embeddable;

		public ChildA() {
		}

		public ChildA(Long id, ReferenceEmbeddable embeddable) {
			super( id );
			this.embeddable = embeddable;
		}

		public ReferenceEmbeddable getEmbeddable() {
			return embeddable;
		}
	}

	@Entity( name = "ChildB" )
	@DiscriminatorValue( "B" )
	public static class ChildB extends Parent {
		@Embedded
		private ReferenceEmbeddable embeddable;

		public ChildB() {
		}

		public ChildB(Long id, ReferenceEmbeddable embeddable) {
			super( id );
			this.embeddable = embeddable;
		}

		public ReferenceEmbeddable getEmbeddable() {
			return embeddable;
		}
	}

	@Embeddable
	public static class AnotherReferenceEmbeddable {
		@ManyToOne
		@JoinColumn( name = "reference_id" )
		private Parent anotherReference;

		public AnotherReferenceEmbeddable() {
		}

		public AnotherReferenceEmbeddable(Parent anotherReference) {
			this.anotherReference = anotherReference;
		}

		public Parent getAnotherReference() {
			return anotherReference;
		}
	}

	@Entity( name = "ChildC" )
	@DiscriminatorValue( "C" )
	public static class ChildC extends Parent {
		@Embedded
		private AnotherReferenceEmbeddable anotherEmbeddable;

		public ChildC() {
		}

		public ChildC(Long id, AnotherReferenceEmbeddable anotherEmbeddable) {
			super( id );
			this.anotherEmbeddable = anotherEmbeddable;
		}

		public AnotherReferenceEmbeddable getAnotherEmbeddable() {
			return anotherEmbeddable;
		}
	}

	@Entity( name = "ChildD" )
	@DiscriminatorValue( "D" )
	public static class ChildD extends Parent {
		@Embedded
		private AnotherReferenceEmbeddable embeddable;

		public ChildD() {
		}

		public ChildD(Long id, AnotherReferenceEmbeddable embeddable) {
			super( id );
			this.embeddable = embeddable;
		}

		public AnotherReferenceEmbeddable getEmbeddable() {
			return embeddable;
		}
	}
}
