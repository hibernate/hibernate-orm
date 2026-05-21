/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpsert;
import jakarta.persistence.PreDelete;
import jakarta.persistence.PreInsert;
import jakarta.persistence.PreMerge;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpsert;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		Jpa4LifecycleCallbackTests.LifecycleEntity.class,
		Jpa4LifecycleCallbackTests.MutatingEntity.class,
		Jpa4LifecycleCallbackTests.PlainEntity.class,
		Jpa4LifecycleCallbackTests.TypedListenerEntity.class
})
@SessionFactory
public class Jpa4LifecycleCallbackTests {
	@BeforeEach
	void setUp() {
		Events.reset();
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void entityManagerCallbacksUseDistinctLifecycleEvents(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new LifecycleEntity( 1, "created" ) ) );

		assertThat( Events.names ).containsExactly(
				"pre-persist:created",
				"pre-insert:created",
				"post-persist:created",
				"post-insert:created"
		);

		Events.reset();

		scope.inTransaction( session -> {
			final LifecycleEntity entity = session.get( LifecycleEntity.class, 1 );
			entity.name = "deleted";
			session.remove( entity );
		} );

		assertThat( Events.names ).containsExactly(
				"pre-remove:deleted",
				"pre-delete:deleted",
				"post-remove:deleted",
				"post-delete:deleted"
		);
	}

	@Test
	void preMergeRunsBeforeStateIsCopied(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new LifecycleEntity( 1, "original" ) ) );
		Events.reset();

		scope.inTransaction( session -> session.merge( new LifecycleEntity( 1, "merged" ) ) );

		assertThat( Events.names ).containsExactly( "pre-merge:merged" );
		scope.inTransaction( session -> {
			final LifecycleEntity entity = session.get( LifecycleEntity.class, 1 );
			assertThat( entity.name ).isEqualTo( "merged-pre-merge" );
		} );
	}

	@Test
	void preInsertMutationIsInserted(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new MutatingEntity( 1, "created" ) ) );

		assertThat( Events.names ).containsExactly( "mutating-pre-insert:created" );
		scope.inTransaction( session -> {
			final MutatingEntity entity = session.get( MutatingEntity.class, 1 );
			assertThat( entity.name ).isEqualTo( "created-pre-insert" );
		} );
	}

	@Test
	void entityAgentUpsertCallbacksFire(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> session.upsert( new LifecycleEntity( 1, "upserted" ) ) );

		assertThat( Events.names ).containsExactly(
				"pre-upsert:upserted",
				"post-upsert:upserted"
		);
	}

	@Test
	void preUpsertMutationIsUpserted(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> session.upsert( new MutatingEntity( 1, "upserted" ) ) );

		assertThat( Events.names ).containsExactly( "mutating-pre-upsert:upserted" );
		scope.inTransaction( session -> {
			final MutatingEntity entity = session.get( MutatingEntity.class, 1 );
			assertThat( entity.name ).isEqualTo( "upserted-pre-upsert" );
		} );
	}

	@Test
	void runtimeListenerCanTargetEntitySupertype(SessionFactoryScope scope) {
		final var registration = scope.getSessionFactory()
				.addListener( Object.class, PreInsert.class, Events::runtimePreInsert );

		scope.inTransaction( session -> {
			session.persist( new PlainEntity( 1, "plain" ) );
			session.persist( new LifecycleEntity( 2, "lifecycle" ) );
		} );

		assertThat( Events.names ).contains(
				"runtime-pre-insert:PlainEntity",
				"runtime-pre-insert:LifecycleEntity"
		);

		registration.cancel();
		Events.reset();

		scope.inTransaction( session -> session.persist( new PlainEntity( 3, "plain-after-cancel" ) ) );

		assertThat( Events.names ).isEmpty();
	}

	@Test
	void entityListenerCanHaveMultipleCallbacksForSameEventWhenParameterTypesDiffer(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TypedListenerEntity( 1, "typed" ) ) );

		assertThat( Events.names ).containsExactlyInAnyOrder(
				"listener-object:TypedListenerEntity",
				"listener-typed:typed"
		);
	}

	public static class Events {
		static final List<String> names = new ArrayList<>();

		static void reset() {
			names.clear();
		}

		static void runtimePreInsert(Object entity) {
			names.add( "runtime-pre-insert:" + entity.getClass().getSimpleName() );
		}
	}

	@Entity(name = "Jpa4LifecycleEntity")
	public static class LifecycleEntity {
		@Id
		private Integer id;
		private String name;

		public LifecycleEntity() {
		}

		public LifecycleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@PrePersist
		void prePersist() {
			Events.names.add( "pre-persist:" + name );
		}

		@PostPersist
		void postPersist() {
			Events.names.add( "post-persist:" + name );
		}

		@PreMerge
		void preMerge() {
			Events.names.add( "pre-merge:" + name );
			name = name + "-pre-merge";
		}

		@PreInsert
		void preInsert() {
			Events.names.add( "pre-insert:" + name );
		}

		@PostInsert
		void postInsert() {
			Events.names.add( "post-insert:" + name );
		}

		@PreUpsert
		void preUpsert() {
			Events.names.add( "pre-upsert:" + name );
		}

		@PostUpsert
		void postUpsert() {
			Events.names.add( "post-upsert:" + name );
		}

		@PreRemove
		void preRemove() {
			Events.names.add( "pre-remove:" + name );
		}

		@PostRemove
		void postRemove() {
			Events.names.add( "post-remove:" + name );
		}

		@PreDelete
		void preDelete() {
			Events.names.add( "pre-delete:" + name );
		}

		@PostDelete
		void postDelete() {
			Events.names.add( "post-delete:" + name );
		}
	}

	@Entity(name = "Jpa4LifecycleMutatingEntity")
	public static class MutatingEntity {
		@Id
		private Integer id;
		private String name;

		public MutatingEntity() {
		}

		public MutatingEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@PreInsert
		void preInsert() {
			Events.names.add( "mutating-pre-insert:" + name );
			name = name + "-pre-insert";
		}

		@PreUpsert
		void preUpsert() {
			Events.names.add( "mutating-pre-upsert:" + name );
			name = name + "-pre-upsert";
		}
	}

	@Entity(name = "Jpa4LifecyclePlainEntity")
	public static class PlainEntity {
		@Id
		private Integer id;
		private String name;

		public PlainEntity() {
		}

		public PlainEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Jpa4LifecycleTypedListenerEntity")
	@EntityListeners( MultiTypeListener.class )
	public static class TypedListenerEntity {
		@Id
		private Integer id;
		private String name;

		public TypedListenerEntity() {
		}

		public TypedListenerEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public static class MultiTypeListener {
		@PostPersist
		void any(Object entity) {
			Events.names.add( "listener-object:" + entity.getClass().getSimpleName() );
		}

		@PostPersist
		void typed(TypedListenerEntity entity) {
			Events.names.add( "listener-typed:" + entity.name );
		}

		@PostPersist
		void plain(PlainEntity entity) {
			Events.names.add( "listener-plain:" + entity.name );
		}
	}
}
