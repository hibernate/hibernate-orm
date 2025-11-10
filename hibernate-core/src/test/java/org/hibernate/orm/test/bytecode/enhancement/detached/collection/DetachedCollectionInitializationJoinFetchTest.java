/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.detached.collection;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.SessionImplementor;

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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		DetachedCollectionInitializationJoinFetchTest.EntityA.class,
		DetachedCollectionInitializationJoinFetchTest.EntityB.class,
})
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-19910")
public class DetachedCollectionInitializationJoinFetchTest {
	@Test
	public void testTransientInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityA = session.find( EntityA.class, 1L );
			Hibernate.initialize( entityA.getB() ); // initialize the collection
			session.clear();

			fetchQuery( new ArrayList<>( entityA.getB() ), session );
		} );
	}

	@Test
	public void testUninitializedDetachedInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityA = session.find( EntityA.class, 1L );
			session.clear();

			fetchQuery( entityA.b, session );
		} );
	}

	@Test
	public void testInitializedDetachedInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityA = session.find( EntityA.class, 1L );
			Hibernate.initialize( entityA.getB() ); // initialize the collection
			session.clear();

			fetchQuery( entityA.getB(), session );
		} );
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entityB1 = new EntityB();
			entityB1.id = 1L;
			entityB1.name = "b_1";
			session.persist( entityB1 );
			final var entityB2 = new EntityB();
			entityB2.id = 2L;
			entityB2.name = "b_2";
			session.persist( entityB2 );
			final var entityA = new EntityA();
			entityA.id = 1L;
			entityA.b.add( entityB1 );
			entityA.b.add( entityB2 );
			session.persist( entityA );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	private void fetchQuery(List<EntityB> b, SessionImplementor session) {
		final var entityA = new EntityA();
		entityA.id = 2L;
		entityA.setB( b );
		session.persist( entityA );

		// If persist triggers lazy initialization, the EntityB instances will be persistent
		final boolean wasB1Managed = session.contains( b.get( 0 ) );
		final boolean wasB2Managed = session.contains( b.get( 1 ) );

		final var result = session.createQuery(
				"from EntityA a left join fetch a.b where a.id = 2",
				EntityA.class
		).getSingleResult();

		// We always need to initialize the collection on flush
		assertThat( Hibernate.isInitialized( b ) ).isTrue();

		final var descriptor = session.getSessionFactory()
				.getMappingMetamodel()
				.getCollectionDescriptor( EntityA.class.getName() + ".b" );
		final PersistentCollection<?> collection = session.getPersistenceContextInternal()
				.getCollection( new CollectionKey( descriptor, entityA.id ) );
		assertThat( Hibernate.isInitialized( collection ) ).isTrue();
		// Currently, the collection instance is re-used if we find a detached PersistentCollection.
		// Not making any assertion here, as also always wrapping the value in a new instance would be acceptable
		// assertThat( collection ).isNotSameAs( b );

		// The detached instances in the collection should not be the same as the
		// managed instances initialized in the persistence context.
		assertThat( result.getB().get( 0 ) == session.getReference( EntityB.class, 1L ) )
				.isEqualTo( wasB1Managed );
		assertThat( result.getB().get( 1 ) == session.getReference( EntityB.class, 2L ) )
				.isEqualTo( wasB2Managed );
	}

	@Entity(name = "EntityA")
	static class EntityA {
		@Id
		private Long id;

		private String name;

		@ManyToMany(fetch = FetchType.LAZY)
		@OrderColumn
		private List<EntityB> b = new ArrayList<>();

		public List<EntityB> getB() {
			return b;
		}

		public void setB(List<EntityB> b) {
			this.b = b;
		}
	}

	@Entity(name = "EntityB")
	static class EntityB {
		@Id
		private Long id;

		private String name;
	}
}
