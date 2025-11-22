/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.detached.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		DetachedReferenceInitializationJoinFetchTest.EntityA.class,
		DetachedReferenceInitializationJoinFetchTest.EntityB.class,
})
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-19910")
public class DetachedReferenceInitializationJoinFetchTest {
	@Test
	public void testDetachedAndPersistentEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.find( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.find( EntityB.class, 1L );

			fetchQuery( 1L, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedAndPersistentEntityInconsistentNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.find( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.find( EntityB.class, 1L );

			fetchQuery( null, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedAndPersistentEntityInconsistentNonNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.find( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.find( EntityB.class, 1L );

			fetchQuery( 1L, null, ignored, session );
		} );
	}

	@Test
	public void testDetachedEntityAndPersistentInitializedProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.find( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( ignored );

			fetchQuery( 1L, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedEntityAndPersistentInitializedProxyInconsistentNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.find( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( ignored );

			fetchQuery( null, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedEntityAndPersistentInitializedProxyInconsistentNonNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.find( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( ignored );

			fetchQuery( 1L, null, ignored, session );
		} );
	}

	@Test
	public void testDetachedEntityAndPersistentProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.find( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );

			fetchQuery( 1L, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedEntityAndPersistentProxyInconsistentNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.find( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );

			fetchQuery( null, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedEntityAndPersistentProxyInconsistentNonNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.find( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );

			fetchQuery( 1L, null, ignored, session );
		} );
	}

	@Test
	public void testDetachedProxyAndPersistentEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.find( EntityB.class, 1L );

			fetchQuery( 1L, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedProxyAndPersistentEntityInconsistentNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.find( EntityB.class, 1L );

			fetchQuery( null, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedProxyAndPersistentEntityInconsistentNonNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.find( EntityB.class, 1L );

			fetchQuery( 1L, null, ignored, session );
		} );
	}

	@Test
	public void testDetachedProxyAndPersistentInitializedProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( ignored );

			fetchQuery( 1L, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedProxyAndPersistentInitializedProxyInconsistentNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( ignored );

			fetchQuery( null, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedProxyAndPersistentInitializedProxyInconsistentNonNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( ignored );

			fetchQuery( 1L, null, ignored, session );
		} );
	}

	@Test
	public void testDetachedAndPersistentProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );

			fetchQuery( 1L, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedAndPersistentProxyInconsistentNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );

			fetchQuery( null, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedAndPersistentProxyInconsistentNonNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );

			fetchQuery( 1L, null, ignored, session );
		} );
	}

	@Test
	public void testDetachedInitializedProxyAndPersistentEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( entityB );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.find( EntityB.class, 1L );

			fetchQuery( 1L, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedInitializedProxyAndPersistentEntityInconsistentNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( entityB );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.find( EntityB.class, 1L );

			fetchQuery( null, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedInitializedProxyAndPersistentEntityInconsistentNonNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( entityB );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.find( EntityB.class, 1L );

			fetchQuery( 1L, null, ignored, session );
		} );
	}

	@Test
	public void testDetachedAndPersistentInitializedProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( entityB );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( ignored );

			fetchQuery( 1L, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedAndPersistentInitializedProxyInconsistentNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( entityB );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( ignored );

			fetchQuery( null, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedAndPersistentInitializedProxyInconsistentNonNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( entityB );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( ignored );

			fetchQuery( 1L, null, ignored, session );
		} );
	}

	@Test
	public void testDetachedInitializedProxyAndPersistentProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( entityB );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );

			fetchQuery( 1L, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedInitializedProxyAndPersistentProxyInconsistentNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( entityB );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );

			fetchQuery( null, entityB, ignored, session );
		} );
	}

	@Test
	public void testDetachedInitializedProxyAndPersistentProxyInconsistentNonNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB = session.getReference( EntityB.class, 1L );
			Hibernate.initialize( entityB );
			session.clear();

			// put a different instance of EntityB in the persistence context
			final var ignored = session.getReference( EntityB.class, 1L );

			fetchQuery( 1L, null, ignored, session );
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

	private void fetchQuery(Long bId, EntityB entityB, EntityB managedB, SessionImplementor session) {
		final var entityA = new EntityA();
		entityA.id = 1L;
		entityA.bId = bId;
		entityA.b = entityB;
		session.persist( entityA );

		final var wasInitialized = Hibernate.isInitialized( entityB );
		final var managedWasInitialized = Hibernate.isInitialized( managedB );

		final var result = session.createQuery(
				"from EntityA a left join fetch a.b",
				EntityA.class
		).getSingleResult();

		assertThat( Hibernate.isInitialized( entityB ) ).isEqualTo( wasInitialized );
		assertThat( result.b ).isSameAs( entityB );

		final var id = session.getSessionFactory().getPersistenceUnitUtil().getIdentifier( managedB );
		final var reference = session.getReference( EntityB.class, id );
		if ( bId == null ) {
			assertThat( Hibernate.isInitialized( reference ) ).isSameAs( managedWasInitialized );
		}
		else {
			assertThat( Hibernate.isInitialized( reference ) ).isTrue();
		}
		assertThat( reference ).isNotSameAs( entityB );
	}

	@Entity(name = "EntityA")
	static class EntityA {
		@Id
		private Long id;
		@Column(name = "b_id")
		private Long bId;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "b_id", insertable = false, updatable = false)
		private EntityB b;
	}

	@Entity(name = "EntityB")
	static class EntityB {
		@Id
		private Long id;

		private String name;
	}
}
