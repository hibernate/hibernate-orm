/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.assertj.core.extractor.Extractors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		CriteriaMutationQueryFkValuesTest.DemoEntity.class,
		CriteriaMutationQueryFkValuesTest.A.class,
		CriteriaMutationQueryFkValuesTest.B.class,
		CriteriaMutationQueryFkValuesTest.C.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18647" )
public class CriteriaMutationQueryFkValuesTest {
	@Test
	public void testInsertValuesFkColumns(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var criteriaInsert = cb.createCriteriaInsertValues( DemoEntity.class );
			criteriaInsert.setInsertionTargetPaths(
					criteriaInsert.getTarget().get( "id" ),
					// insert values into foreign key columns
					criteriaInsert.getTarget().get( "a" ).get( "id" ), // a_id
					criteriaInsert.getTarget().get( "b" ).get( "id" ), // b_id
					criteriaInsert.getTarget().get( "c" ).get( "id" )  // c_id
			);
			criteriaInsert.values( cb.values(
					cb.value( 2L ),
					cb.value( 1 ),
					cb.value( 2 ),
					cb.value( 3 )
			) );
			final var count = session.createMutationQuery( criteriaInsert ).executeUpdate();
			assertThat( count ).isEqualTo( 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "join", 0 );

			assertThat( session.find( DemoEntity.class, 2L ) ).extracting( "a", "b", "c" ).doesNotContainNull();
		} );
	}

	@Test
	public void testUpdateFkColumns(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var criteriaUpdate = cb.createCriteriaUpdate( DemoEntity.class );
			criteriaUpdate.set( criteriaUpdate.getTarget().get( "a" ).<Integer>get( "id" ), cb.value( 4 ) );
			criteriaUpdate.set(
					criteriaUpdate.getTarget().get( "b" ).<Integer>get( "id" ),
					cb.nullLiteral( Integer.class )
			);
			criteriaUpdate.where( cb.equal( criteriaUpdate.getTarget().get( "id" ), 1L ) );
			final var count = session.createMutationQuery( criteriaUpdate ).executeUpdate();
			assertThat( count ).isEqualTo( 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "join", 0 );

			assertThat( session.find( DemoEntity.class, 1L ) )
					.extracting( "a", "b", "c" )
					.extracting( o -> o == null ? null : Extractors.byName( "id" ).apply( o ) )
					.contains( 4, null, 3 );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var a = new A( 1 );
			final var b = new B( 2 );
			final var c = new C( 3 );
			session.persist( new DemoEntity( 1L, a, b, c ) );
			session.persist( new A( 4 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity( name = "DemoEntity" )
	static class DemoEntity {
		@Id
		private Long id;

		@ManyToOne( cascade = CascadeType.PERSIST )
		@JoinColumn( name = "a_id" )
		private A a;

		@ManyToOne( cascade = CascadeType.PERSIST )
		@JoinColumn( name = "b_id" )
		private B b;

		@ManyToOne( cascade = CascadeType.PERSIST )
		@JoinColumn( name = "c_id" )
		private C c;

		public DemoEntity() {
		}

		public DemoEntity(Long id, A a, B b, C c) {
			this.id = id;
			this.a = a;
			this.b = b;
			this.c = c;
		}
	}

	@Entity( name = "AEntity" )
	static class A {
		@Id
		private Integer id;

		public A() {
		}

		public A(Integer id) {
			this.id = id;
		}
	}

	@Entity( name = "BEntity" )
	static class B {
		@Id
		private Integer id;

		public B() {
		}

		public B(Integer id) {
			this.id = id;
		}
	}

	@Entity( name = "CEntity" )
	static class C {
		@Id
		private Integer id;

		public C() {
		}

		public C(Integer id) {
			this.id = id;
		}
	}
}
