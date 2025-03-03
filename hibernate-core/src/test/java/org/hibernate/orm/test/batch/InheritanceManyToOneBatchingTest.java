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
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel( annotatedClasses = {
		InheritanceManyToOneBatchingTest.Parent.class,
		InheritanceManyToOneBatchingTest.ChildA.class,
		InheritanceManyToOneBatchingTest.ChildB.class,
		InheritanceManyToOneBatchingTest.ChildC.class
} )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "10" ) )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16248" )
public class InheritanceManyToOneBatchingTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ChildA childA = new ChildA( 1L, null );
			final ChildB childB = new ChildB( 2L, childA );
			final ChildC childC = new ChildC( 3L, childA );
			session.persist( childA );
			session.persist( childB );
			session.persist( childC );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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
			assertThat( resultB.getReference() ).isInstanceOf( ChildA.class );
			assertThat( resultB.getReference().getId() ).isEqualTo( 1L );
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
			assertThat( resultC.getAnotherReference() ).isInstanceOf( ChildA.class );
			assertThat( resultC.getAnotherReference().getId() ).isEqualTo( 1L );
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

	@Entity( name = "ChildA" )
	@DiscriminatorValue( "A" )
	public static class ChildA extends Parent {
		@ManyToOne
		@JoinColumn( name = "reference_id" )
		private Parent reference;

		public ChildA() {
		}

		public ChildA(Long id, Parent reference) {
			super( id );
			this.reference = reference;
		}

		public Parent getReference() {
			return reference;
		}
	}

	@Entity( name = "ChildB" )
	@DiscriminatorValue( "B" )
	public static class ChildB extends Parent {
		@ManyToOne
		@JoinColumn( name = "reference_id" )
		private Parent reference;

		public ChildB() {
		}

		public ChildB(Long id, Parent reference) {
			super( id );
			this.reference = reference;
		}

		public Parent getReference() {
			return reference;
		}
	}

	@Entity( name = "ChildC" )
	@DiscriminatorValue( "C" )
	public static class ChildC extends Parent {
		@ManyToOne
		@JoinColumn( name = "reference_id" )
		private Parent anotherReference;

		public ChildC() {
		}

		public ChildC(Long id, Parent anotherReference) {
			super( id );
			this.anotherReference = anotherReference;
		}

		public Parent getAnotherReference() {
			return anotherReference;
		}
	}
}
