/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		EntityValuedInSubqueryGroupAndOrderTest.EntityA.class,
		EntityValuedInSubqueryGroupAndOrderTest.EntityB.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16770" )
public class EntityValuedInSubqueryGroupAndOrderTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = new EntityA( "entity_a" );
			session.persist( entityA );
			session.persist( new EntityB( entityA, 1 ) );
			session.persist( new EntityB( entityA, 2 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityB" ).executeUpdate();
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
		} );
	}

	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 23, reason = "Oracle 23c bug")
	public void testInSubqueryGroupBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityB result = session.createQuery(
					"select b from EntityB b " +
							"where (b.entityA, b.amount) in " +
							"	(select b2.entityA, max(b2.amount) from EntityB b2 " +
							"	where b2.entityA.unlisted = false " +
							"	group by b2.entityA)",
					EntityB.class
			).getSingleResult();
			assertThat( result.getAmount() ).isEqualTo( 2 );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17231" )
	public void testInSubqueryGroupByProp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityB result = session.createQuery(
					"select b from EntityB b " +
							"where (b.entityA.name, b.amount) in " +
							"	(select b2.entityA.name, max(b2.amount) from EntityB b2 " +
							"	where b2.entityA.unlisted = false " +
							"	group by b2.entityA)",
					EntityB.class
			).getSingleResult();
			assertThat( result.getAmount() ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testTopLevelSelect(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// Here, the selection is top level so the entity valued path will be expanded
			final Tuple result = session.createQuery(
					"select b.entityA, max(b.amount) from EntityB b " +
							"	where b.entityA.unlisted = false " +
							"	group by b.entityA ",
					Tuple.class
			).getSingleResult();
			assertThat( result.get( 0, EntityA.class ).getName() ).isEqualTo( "entity_a" );
			assertThat( result.get( 1, Integer.class ) ).isEqualTo( 2 );
		} );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private boolean unlisted;

		public EntityA() {
		}

		public EntityA(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "EntityB" )
	public static class EntityB {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private EntityA entityA;

		private Integer amount;

		public EntityB() {
		}

		public EntityB(EntityA entityA, Integer amount) {
			this.entityA = entityA;
			this.amount = amount;
		}

		public Integer getAmount() {
			return amount;
		}
	}
}
