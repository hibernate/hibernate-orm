/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.detached.reference;

import jakarta.persistence.*;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
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
		DetachedReferenceInitializationEagerAnyFetchTest.EntityA.class,
		DetachedReferenceInitializationEagerAnyFetchTest.EntityB.class,
})
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-19910")
public class DetachedReferenceInitializationEagerAnyFetchTest {
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
		entityA.bType = bId == null ? null : "B";
		entityA.b = entityB;
		session.persist( entityA );

		final var wasDetachedInitialized = Hibernate.isInitialized( entityB );
		final var wasManagedInitialized = Hibernate.isInitialized( managedB );

		final var result = session.createQuery(
				"from EntityA a",
				EntityA.class
		).getSingleResult();

		assertThat( Hibernate.isInitialized( entityB ) ).isEqualTo( wasDetachedInitialized );
		assertThat( result.b ).isSameAs( entityB );

		if ( bId == null ) {
			assertThat( Hibernate.isInitialized( managedB ) ).isSameAs( wasManagedInitialized );
		}
		else {
			// We cannot create a proxy for the non-enhanced case
			assertThat( Hibernate.isInitialized( managedB ) ).isTrue();
		}
		assertThat( managedB ).isNotSameAs( entityB );
	}

	@Entity(name = "EntityA")
	static class EntityA {
		@Id
		private Long id;
		@Column(name = "b_id")
		private Long bId;
		@Column(name = "b_type")
		private String bType;
		@Any(fetch = FetchType.EAGER)
		@AnyKeyJavaClass(Long.class)
		@JoinColumn(name = "b_id", insertable = false, updatable = false) //the foreign key column
		@Column(name = "b_type", insertable = false, updatable = false)   //the discriminator column
		@AnyDiscriminatorValue(discriminator = "B", entity = EntityB.class)
		private Object b;
	}

	@Entity(name = "EntityB")
	static class EntityB {
		@Id
		private Long id;

		private String name;
	}
}
