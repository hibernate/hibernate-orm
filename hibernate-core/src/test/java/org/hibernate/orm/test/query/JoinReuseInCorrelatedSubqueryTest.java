/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		JoinReuseInCorrelatedSubqueryTest.EntityA.class,
		JoinReuseInCorrelatedSubqueryTest.EntityB.class,
		JoinReuseInCorrelatedSubqueryTest.ReferencedEntity.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16537" )
public class JoinReuseInCorrelatedSubqueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ReferencedEntity ref1 = new ReferencedEntity( 1 );
			final ReferencedEntity ref2 = new ReferencedEntity( 2 );
			session.persist( ref1 );
			session.persist( ref2 );
			final EntityB entityB1 = new EntityB( ref1.getId() );
			session.persist( entityB1 );
			session.persist( new EntityA( "entitya_1", entityB1 ) );
			final EntityB entityB2 = new EntityB( ref2.getId() );
			session.persist( entityB2 );
			session.persist( new EntityA( "entitya_2", entityB2 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
			session.createMutationQuery( "delete from EntityB" ).executeUpdate();
			session.createMutationQuery( "delete from ReferencedEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testImplicitJoinReuse(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<EntityA> resultList = session.createQuery(
					"select a from EntityA a join a.entityB ab " +
							"where 0 < (select count(*) from ReferencedEntity r where r.foo = 1 and r.id = a.entityB.reference)",
					EntityA.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getName() ).isEqualTo( "entitya_1" );
		} );
	}

	@Test
	public void testExplicitJoinReuse(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<EntityA> resultList = session.createQuery(
					"select a from EntityA a join a.entityB ab " +
							"where 0 < (select count(*) from ReferencedEntity r where r.foo = 1 and r.id = ab.reference)",
					EntityA.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getName() ).isEqualTo( "entitya_1" );
		} );
	}

	@Test
	public void testImplicitJoinOnly(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<EntityA> resultList = session.createQuery(
					"select a from EntityA a " +
							"where 0 < (select count(*) from ReferencedEntity r where r.foo = 1 and r.id = a.entityB.reference)",
					EntityA.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getName() ).isEqualTo( "entitya_1" );
		} );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@ManyToOne
		@JoinColumn( name = "entityb_id" )
		private EntityB entityB;

		public EntityA() {
		}

		public EntityA(String name, EntityB entityB) {
			this.name = name;
			this.entityB = entityB;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "EntityB" )
	public static class EntityB {
		@Id
		@GeneratedValue
		private Integer id;

		@Column( name = "ref_number" )
		private Integer reference;

		public EntityB() {
		}

		public EntityB(Integer reference) {
			this.reference = reference;
		}
	}

	@Entity( name = "ReferencedEntity" )
	public static class ReferencedEntity {
		@Id
		@GeneratedValue
		private Integer id;

		private Integer foo;

		public ReferencedEntity() {
		}

		public ReferencedEntity(Integer foo) {
			this.foo = foo;
		}

		public Integer getId() {
			return id;
		}
	}
}
