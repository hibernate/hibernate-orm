/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.detached.collection;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		InconsistentUnownedCollectionJoinFetchTest.EntityA.class,
		InconsistentUnownedCollectionJoinFetchTest.EntityB.class,
})
@SessionFactory
@EnhancementOptions(inlineDirtyChecking = true, lazyLoading = true, extendedEnhancement = true)
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-19910")
public class InconsistentUnownedCollectionJoinFetchTest {
	@Test
	public void testEmptyInconsistentEmpty(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Map<String, EntityB> managedB = getEntityBMap( session );

			fetchQuery( new HashMap<>(), managedB, session );
		} );
	}

	@Test
	public void testNullInconsistentEmpty(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Map<String, EntityB> managedB = getEntityBMap( session );

			fetchQuery( null, managedB, session );
		} );
	}

	@Test
	public void testEmptyInconsistentInitialized(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Map<String, EntityB> managedB = getEntityBMap( session );
			for ( Map.Entry<String, EntityB> entry : managedB.entrySet() ) {
				entry.setValue( session.find( EntityB.class, entry.getValue().id ) );
			}

			fetchQuery( new HashMap<>(), managedB, session );
		} );
	}

	@Test
	public void testNullInconsistentInitialized(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Map<String, EntityB> managedB = getEntityBMap( session );
			for ( Map.Entry<String, EntityB> entry : managedB.entrySet() ) {
				entry.setValue( session.find( EntityB.class, entry.getValue().id ) );
			}

			fetchQuery( null, managedB, session );
		} );
	}

	@Test
	public void testEmptyInconsistentProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Map<String, EntityB> managedB = getEntityBMap( session );
			for ( Map.Entry<String, EntityB> entry : managedB.entrySet() ) {
				entry.setValue( session.getReference( EntityB.class, entry.getValue().id ) );
			}

			fetchQuery( new HashMap<>(), managedB, session );
		} );
	}

	@Test
	public void testNullInconsistentProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Map<String, EntityB> managedB = getEntityBMap( session );
			for ( Map.Entry<String, EntityB> entry : managedB.entrySet() ) {
				entry.setValue( session.getReference( EntityB.class, entry.getValue().id ) );
			}

			fetchQuery( null, managedB, session );
		} );
	}

	@Test
	public void testEmptyInconsistentInitializedProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Map<String, EntityB> managedB = getEntityBMap( session );
			for ( Map.Entry<String, EntityB> entry : managedB.entrySet() ) {
				EntityB entity = session.getReference( EntityB.class, entry.getValue().id );
				Hibernate.initialize( entity );
				entry.setValue( entity );
			}

			fetchQuery( new HashMap<>(), managedB, session );
		} );
	}

	@Test
	public void testNullInconsistentInitializedProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Map<String, EntityB> managedB = getEntityBMap( session );
			for ( Map.Entry<String, EntityB> entry : managedB.entrySet() ) {
				EntityB entity = session.getReference( EntityB.class, entry.getValue().id );
				Hibernate.initialize( entity );
				entry.setValue( entity );
			}

			fetchQuery( null, managedB, session );
		} );
	}

	private Map<String, EntityB> getEntityBMap(SessionImplementor session) {
		final var result = session.createQuery(
				"from EntityA a left join fetch a.b where a.id = 1",
				EntityA.class
		).getSingleResult();
		session.clear();
		return result.getB();
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityA = new EntityA();
			entityA.id = 1L;
			session.persist( entityA );

			final Map<String, EntityB> managedB = new HashMap<>();
			managedB.put( "b1", new EntityB( 1L, "b1" ) );
			managedB.put( "b2", new EntityB( 2L, "b2" ) );

			for ( EntityB entityB : managedB.values() ) {
				entityB.a = entityA;
				session.persist( entityB );
			}
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	private void fetchQuery(Map<String, EntityB> b, Map<String, EntityB> managedB, SessionImplementor session) {
		final var persistenceContext = session.getPersistenceContext();
		final var descriptor = session.getFactory().getMappingMetamodel().getEntityDescriptor( EntityB.class );

		final var entityA = session.find( EntityA.class, 1L );
		entityA.setB( b );

		final var originalSize = b == null ? -1 : b.size();
		final var firstB = managedB.values().iterator().next();
		final var wasManaged = persistenceContext.getEntityHolder( entityKey( session, descriptor, firstB ) ) != null;
		session.flush();
		final var originalB = entityA.getB();

		final var result = session.createQuery(
				"from EntityA a left join fetch a.b where a.id = 1",
				EntityA.class
		).getSingleResult();

		assertThat( persistenceContext.getEntitiesByKey().size() ).isEqualTo( 1 + managedB.size() );
		assertThat( entityA.getB() ).isSameAs( originalB );
		assertThat( originalB == null ? -1 : originalB.size() ).isSameAs( originalSize );

		for ( EntityB entityB : managedB.values() ) {
			final var entityHolder = persistenceContext.getEntityHolder( entityKey( session, descriptor, entityB ) );
			assertThat( entityHolder ).isNotNull();
			if ( wasManaged ) {
				assertThat( entityHolder.getManagedObject() ).isSameAs( entityB );
			}
			else {
				assertThat( entityHolder.getManagedObject() ).isNotSameAs( entityB );
			}
			assertThat( Hibernate.isInitialized( entityHolder.getManagedObject() ) ).isTrue();
		}
	}

	private static EntityKey entityKey(SessionImplementor session, EntityPersister descriptor, EntityB entityB) {
		return session.generateEntityKey( session.getFactory().getPersistenceUnitUtil().getIdentifier( entityB ), descriptor );
	}

	@Entity(name = "EntityA")
	static class EntityA {
		@Id
		private Long id;
		private String name;
		@OneToMany(mappedBy = "a")
		@MapKey(name = "name")
		private Map<String, EntityB> b = new HashMap<>();

		public Map<String, EntityB> getB() {
			return b;
		}

		public void setB(Map<String, EntityB> b) {
			this.b = b;
		}
	}

	@Entity(name = "EntityB")
	static class EntityB {
		@Id
		private Long id;
		private String name;
		@ManyToOne(fetch = FetchType.LAZY)
		private EntityA a;

		public EntityB() {
		}

		public EntityB(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
