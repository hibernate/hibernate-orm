/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.polymorphic;

import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		UnmappedPolymorphismQueryTest.EntityA.class,
		UnmappedPolymorphismQueryTest.EntityB.class,
		UnmappedPolymorphismQueryTest.EntityC.class,
} )
public class UnmappedPolymorphismQueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = new EntityA( 1L );
			session.persist( entityA );
			final EntityB entityB = new EntityB( 2L, entityA );
			session.persist( entityB );
			session.persist( new EntityC( 3L, entityB ) );
			session.persist( new EntityA( 4L ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityC" ).executeUpdate();
			session.createMutationQuery( "delete from EntityB" ).executeUpdate();
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16733" )
	public void testSimpleQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<IEntityA> resultList = session.createQuery(
					String.format( "select a from %s a", IEntityA.class.getName() ),
					IEntityA.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16733" )
	public void testCountQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Long count = session.createQuery(
					String.format( "select count(a) from %s a", IEntityA.class.getName() ),
					Long.class
			).getSingleResult();
			assertThat( count ).isEqualTo( 2L );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16582" )
	public void testCountJoinQuery(SessionFactoryScope scope) {
		final EntityC entityC = scope.fromTransaction( session -> session.find( EntityC.class, 3L ) );
		scope.inTransaction( session -> {
			final Long count = session.createQuery(
					String.format(
							"select count(a) from %s a left join a.b b left join b.c c where c = :entityC",
							IEntityA.class.getName()
					),
					Long.class
			).setParameter( "entityC", entityC ).getSingleResult();
			assertThat( count ).isEqualTo( 1L );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16582" )
	public void testCountJoinQueryTreat(SessionFactoryScope scope) {
		final EntityC entityC = scope.fromTransaction( session -> session.find( EntityC.class, 3L ) );
		scope.inTransaction( session -> {
			final Long count = session.createQuery(
					String.format(
							"select count(a) from %s a left join a.b b left join treat(b as EntityB).c c where c = :entityC",
							IEntityA.class.getName()
					),
					Long.class
			).setParameter( "entityC", entityC ).getSingleResult();
			assertThat( count ).isEqualTo( 1L );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16582" )
	public void testCountJoinSubQuery(SessionFactoryScope scope) {
		final EntityC entityC = scope.fromTransaction( session -> session.find( EntityC.class, 3L ) );
		scope.inTransaction( session -> {
			final Long count = session.createQuery(
					String.format(
							"select count(a) from %s a left join a.b b left join b.c c where c in " +
									"(select c1 from b.c c1 where c1 = :entityC)",
							IEntityA.class.getName()
					),
					Long.class
			).setParameter( "entityC", entityC ).getSingleResult();
			assertThat( count ).isEqualTo( 1L );
		} );
	}

	public interface IEntityA {
		Long getId();
	}

	public interface IEntityB {
		Long getId();
	}

	public interface IEntityC {
		Long getId();
	}

	@Entity( name = "EntityA" )
	public static class EntityA implements IEntityA {
		@Id
		private Long id;

		@OneToMany( mappedBy = "a", targetEntity = EntityB.class )
		private Set<IEntityB> b;

		public EntityA() {
		}

		public EntityA(Long id) {
			this.id = id;
		}

		@Override
		public Long getId() {
			return id;
		}
	}

	@Entity( name = "EntityB" )
	public static class EntityB implements IEntityB {
		@Id
		private Long id;

		@ManyToOne( targetEntity = EntityA.class )
		private IEntityA a;

		@OneToMany( mappedBy = "b", targetEntity = EntityC.class )
		private Set<IEntityC> c;

		@Override
		public Long getId() {
			return id;
		}

		public EntityB() {
		}

		public EntityB(Long id, IEntityA a) {
			this.id = id;
			this.a = a;
		}
	}

	@Entity( name = "EntityC" )
	public static class EntityC implements IEntityC {
		@Id
		private Long id;

		@ManyToOne( targetEntity = EntityB.class )
		private IEntityB b;

		@Override
		public Long getId() {
			return id;
		}

		public EntityC() {
		}

		public EntityC(Long id, IEntityB b) {
			this.id = id;
			this.b = b;
		}
	}
}
