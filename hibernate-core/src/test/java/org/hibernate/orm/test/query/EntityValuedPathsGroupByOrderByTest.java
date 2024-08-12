/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		EntityValuedPathsGroupByOrderByTest.EntityA.class,
		EntityValuedPathsGroupByOrderByTest.EntityB.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16409" )
public class EntityValuedPathsGroupByOrderByTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityB entityB = new EntityB( 1L, "entity_b" );
			session.persist( entityB );
			session.persist( new EntityA( 2L, 1, entityB ) );
			session.persist( new EntityA( 3L, 2, entityB ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
			session.createMutationQuery( "delete from EntityB" ).executeUpdate();
		} );
	}

	// Root ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Test
	public void testRootGroupBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select b.name from EntityB b group by b",
				String.class
		).getSingleResult() ).isEqualTo( "entity_b" ) );
	}

	@Test
	public void testRootGroupByAndOrderBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select b.name from EntityB b group by b order by b",
				String.class
		).getSingleResult() ).isEqualTo( "entity_b" ) );
	}

	@Test
	public void testRootSelectAndGroupBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select b from EntityB b group by b",
				EntityB.class
		).getSingleResult().getName() ).isEqualTo( "entity_b" ) );
	}

	@Test
	public void testRootSelectAndGroupByAndOrderBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select b from EntityB b group by b order by b",
				EntityB.class
		).getSingleResult().getName() ).isEqualTo( "entity_b" ) );
	}

	// Explicit join ~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Test
	public void testJoinGroupBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select b.name, sum(a.amount) from EntityA a join a.secondary b group by b order by b.name",
				Tuple.class
		).getSingleResult().get( 1 ) ).isEqualTo( 3L ) );
	}

	@Test
	public void testJoinGroupByAndOrderBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select b.name, sum(a.amount) from EntityA a join a.secondary b group by b order by b",
				Tuple.class
		).getSingleResult().get( 1 ) ).isEqualTo( 3L ) );
	}

	@Test
	public void testJoinSelectAndGroupBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select b, sum(a.amount) from EntityA a join a.secondary b group by b order by b.name",
				Tuple.class
		).getSingleResult().get( 1 ) ).isEqualTo( 3L ) );
	}

	@Test
	public void testJoinSelectAndGroupByAndOrderBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select b, sum(a.amount) from EntityA a join a.secondary b group by b order by b",
				Tuple.class
		).getSingleResult().get( 1 ) ).isEqualTo( 3L ) );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18272" )
	public void testJoinSelectAliasAndGroupByAndOrderBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select b as secondary, sum(a.amount) from EntityA a join a.secondary b where b.id = 1 group by secondary order by secondary",
				Tuple.class
		).getSingleResult().get( 1 ) ).isEqualTo( 3L ) );
	}

	// Implicit join ~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Test
	public void testImplicitJoinGroupBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select a.secondary.name, sum(a.amount) from EntityA a group by a.secondary order by a.secondary.name",
				Tuple.class
		).getSingleResult().get( 1 ) ).isEqualTo( 3L ) );
	}

	@Test
	public void testImplicitJoinGroupByAndOrderBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select a.secondary.name, sum(a.amount) from EntityA a group by a.secondary order by a.secondary",
				Tuple.class
		).getSingleResult().get( 1 ) ).isEqualTo( 3L ) );
	}

	@Test
	public void testImplicitJoinSelectAndGroupBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select a.secondary, sum(a.amount) from EntityA a group by a.secondary",
				Tuple.class
		).getSingleResult().get( 1 ) ).isEqualTo( 3L ) );
	}

	@Test
	public void testImplicitJoinSelectAndGroupByAndOrderBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select a.secondary, sum(a.amount) from EntityA a group by a.secondary order by a.secondary",
				Tuple.class
		).getSingleResult().get( 1 ) ).isEqualTo( 3L ) );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18272" )
	public void testImplicitJoinSelectAliasAndGroupByAndOrderBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select a.secondary as a_secondary, sum(a.amount) from EntityA a where a.secondary.id = 1 group by a_secondary order by a_secondary",
				Tuple.class
		).getSingleResult().get( 1 ) ).isEqualTo( 3L ) );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17415" )
	public void testInSubqueryGroupBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select a from EntityA a where a.secondary in (select b2 from EntityB b2 group by b2)",
				Tuple.class
		).getResultList() ).hasSize( 2 ) );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17415" )
	public void testInSubqueryGroupByImplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select a from EntityA a where a.secondary in" +
						" (select a2.secondary from EntityA a2 group by a2.secondary)",
				Tuple.class
		).getResultList() ).hasSize( 2 ) );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		private Long id;

		private Integer amount;

		@ManyToOne
		@JoinColumn( name = "secondary_id" )
		private EntityB secondary;

		public EntityA() {
		}

		public EntityA(Long id, Integer amount, EntityB secondary) {
			this.id = id;
			this.amount = amount;
			this.secondary = secondary;
		}

		public Long getId() {
			return id;
		}

		public Integer getAmount() {
			return amount;
		}

		public EntityB getSecondary() {
			return secondary;
		}

	}

	@Entity( name = "EntityB" )
	public static class EntityB {
		@Id
		@Column( name = "id_col" )
		private Long id;

		@Column( name = "name_col" )
		private String name;

		public EntityB() {
		}

		public EntityB(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
