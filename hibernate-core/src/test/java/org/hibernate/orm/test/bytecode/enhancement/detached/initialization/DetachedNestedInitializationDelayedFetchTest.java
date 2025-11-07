/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.detached.initialization;

import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		DetachedNestedInitializationDelayedFetchTest.EntityA.class,
		DetachedNestedInitializationDelayedFetchTest.EntityB.class,
})
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
public class DetachedNestedInitializationDelayedFetchTest {
	@Test
	public void test1(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.find( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.find( EntityB.class, 1L );

			fetchQuery( entityB, session );
		} );
	}

	@Test
	public void test2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.find( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( ignored );

			fetchQuery( entityB, session );
		} );
	}

	@Test
	public void test3(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.find( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );

			fetchQuery( entityB, session );
		} );
	}

	@Test
	public void test4(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.find( EntityB.class, 1L );

			fetchQuery( entityB, session );
		} );
	}

	@Test
	public void test5(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( ignored );

			fetchQuery( entityB, session );
		} );
	}

	@Test
	public void test6(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );


			fetchQuery( entityB, session );
		} );
	}

	@Test
	public void testDetachedEntityJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			Hibernate.initialize(  entityB );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.find( EntityB.class, 1L );

			fetchQuery( entityB, session );
		} );
	}

	@Test
	public void testDetachedInitializedProxyJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			Hibernate.initialize(  entityB );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( ignored );

			fetchQuery( entityB, session );
		} );
	}

	@Test
	public void testDetachedUninitializedProxyJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			Hibernate.initialize(  entityB );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );

			fetchQuery( entityB, session );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = new EntityB();
			entityB.id = 1L;
			entityB.name = "b_1";
			session.persist( entityB );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
		} );
	}

	private void fetchQuery(EntityB entityB, SessionImplementor session) {
		final var entityA = new EntityA();
		entityA.id = 1L;
		entityA.b = entityB;
		session.persist( entityA );

		final var wasDetachedInitialized = Hibernate.isInitialized( entityB );

		final var id = session.getSessionFactory().getPersistenceUnitUtil().getIdentifier( entityB );
		final var reference = session.getReference( EntityB.class, id );
		final var wasManagedInitialized = Hibernate.isInitialized( reference );

		final var result = session.createQuery(
				"from EntityA a",
				EntityA.class
		).getSingleResult();

		assertThat( Hibernate.isInitialized( entityB ) ).isEqualTo( wasDetachedInitialized );
		assertThat( result.b ).isSameAs( entityB );


		assertThat( Hibernate.isInitialized( reference ) ).isEqualTo( wasManagedInitialized );
		assertThat( reference ).isNotSameAs( entityB );
	}

	@Entity(name = "EntityA")
	static class EntityA {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private EntityB b;
	}

	@Entity(name = "EntityB")
	static class EntityB {
		@Id
		private Long id;

		private String name;
	}
}
