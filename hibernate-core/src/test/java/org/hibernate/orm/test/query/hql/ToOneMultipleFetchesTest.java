/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.Hibernate;
import org.hibernate.query.sqm.InterpretationException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ToOneMultipleFetchesTest.EntityA.class,
		ToOneMultipleFetchesTest.EntityB.class,
		ToOneMultipleFetchesTest.EntityC.class,
		ToOneMultipleFetchesTest.EntityD.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17777" )
public class ToOneMultipleFetchesTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityC entityC = new EntityC( 1L, "entity_c" );
			session.persist( entityC );
			final EntityD entityD = new EntityD( 2L, "entity_d" );
			session.persist( entityD );
			final EntityB entityB2 = new EntityB( 3L, entityC, entityD );
			session.persist( entityB2 );
			session.persist( new EntityA( 4L, entityB2 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
			session.createMutationQuery( "delete from EntityB" ).executeUpdate();
			session.createMutationQuery( "delete from EntityC" ).executeUpdate();
			session.createMutationQuery( "delete from EntityD" ).executeUpdate();
		} );
	}

	@Test
	public void testCriteriaMultipleFetches(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder builder = session.getCriteriaBuilder();
			final CriteriaQuery<EntityA> query = builder.createQuery( EntityA.class );
			final Root<EntityA> root = query.from( EntityA.class );
			root.fetch( "entityB", JoinType.INNER ).fetch( "entityC", JoinType.INNER );
			root.fetch( "entityB", JoinType.INNER ).fetch( "entityD", JoinType.INNER );
			final EntityA result = session.createQuery( query ).getSingleResult();
			assertThat( Hibernate.isInitialized( result.getEntityB() ) ).isTrue();
			final EntityB entityB = result.getEntityB();
			assertThat( Hibernate.isInitialized( entityB.getEntityC() ) ).isTrue();
			assertThat( Hibernate.isInitialized( entityB.getEntityD() ) ).isTrue();
		} );
	}

	@Test
	public void testCriteriaFetchReuse(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder builder = session.getCriteriaBuilder();
			final CriteriaQuery<EntityA> query = builder.createQuery( EntityA.class );
			final Root<EntityA> root = query.from( EntityA.class );
			final Fetch<EntityA, EntityB> fetchEntityB = root.fetch( "entityB", JoinType.INNER );
			fetchEntityB.fetch( "entityC", JoinType.INNER );
			fetchEntityB.fetch( "entityD", JoinType.INNER );
			final EntityA result = session.createQuery( query ).getSingleResult();
			assertThat( Hibernate.isInitialized( result.getEntityB() ) ).isTrue();
			final EntityB entityB = result.getEntityB();
			assertThat( Hibernate.isInitialized( entityB.getEntityD() ) ).isTrue();
			assertThat( Hibernate.isInitialized( entityB.getEntityC() ) ).isTrue();
		} );
	}

	@Test
	public void testMultipleFetchesError(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final CriteriaBuilder builder = session.getCriteriaBuilder();
			final CriteriaQuery<EntityA> query = builder.createQuery( EntityA.class );
			final Root<EntityA> root = query.from( EntityA.class );
			root.fetch( "entityB", JoinType.LEFT );
			try {
				root.fetch( "entityB", JoinType.INNER );
				fail( "Multiple fetches with different join types should not be allowed" );
			}
			catch (Exception e) {
				assertThat( e )
						.isInstanceOf( IllegalStateException.class )
						.hasMessage(
								"Requested join fetch with association [entityB] with 'inner' join type, " +
										"but found existing join fetch with 'left outer' join type."
						);
			}
		} );

		scope.inSession( session -> {
			try {
				session.createQuery(
						"from EntityA a " +
								"join fetch a.entityB b1 join fetch b1.entityC " +
								"join fetch a.entityB b2 join fetch b2.entityD",
						EntityA.class
				);
			}
			catch (Exception e) {
				assertThat( e )
						.isInstanceOf( IllegalArgumentException.class )
						.getCause()
						.isInstanceOf( InterpretationException.class )
						.getCause()
						.isInstanceOf( IllegalStateException.class )
						.hasMessage( "Cannot fetch the same association twice with a different alias" );
			}
		} );
	}

	@Test
	public void testHQLMultipleFetches(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA result = session.createQuery(
					"from EntityA a " +
							"join fetch a.entityB b1 join fetch b1.entityC " +
							"join fetch a.entityB join fetch a.entityB.entityD",
					EntityA.class
			).getSingleResult();
			assertThat( Hibernate.isInitialized( result.getEntityB() ) ).isTrue();
			final EntityB entityB = result.getEntityB();
			assertThat( Hibernate.isInitialized( entityB.getEntityD() ) ).isTrue();
			assertThat( Hibernate.isInitialized( entityB.getEntityC() ) ).isTrue();
		} );
	}

	@Test
	public void testHQLFetchReuse(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA result = session.createQuery(
					"from EntityA a " +
							"join fetch a.entityB b left join fetch b.entityC join fetch b.entityD",
					EntityA.class
			).getSingleResult();
			assertThat( Hibernate.isInitialized( result.getEntityB() ) ).isTrue();
			final EntityB entityB = result.getEntityB();
			assertThat( Hibernate.isInitialized( entityB.getEntityD() ) ).isTrue();
			assertThat( Hibernate.isInitialized( entityB.getEntityC() ) ).isTrue();
		} );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY )
		private EntityB entityB;

		public EntityA() {
		}

		public EntityA(Long id, EntityB entityB) {
			this.id = id;
			this.entityB = entityB;
		}

		public EntityB getEntityB() {
			return entityB;
		}
	}

	@Entity( name = "EntityB" )
	public static class EntityB {
		@Id
		private Long id;

		@ManyToOne
		private EntityC entityC;

		@ManyToOne( fetch = FetchType.LAZY )
		private EntityD entityD;

		public EntityB() {
		}

		public EntityB(Long id, EntityC entityC, EntityD entityD) {
			this.id = id;
			this.entityC = entityC;
			this.entityD = entityD;
		}

		public EntityD getEntityD() {
			return entityD;
		}

		public EntityC getEntityC() {
			return entityC;
		}
	}

	@Entity( name = "EntityC" )
	public static class EntityC {
		@Id
		private Long id;

		private String name;

		public EntityC() {
		}

		public EntityC(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name = "EntityD" )
	public static class EntityD {
		@Id
		private Long id;

		private String name;

		public EntityD() {
		}

		public EntityD(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
