package org.hibernate.orm.test.jpa.callbacks.jpa4;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.PreInsert;

import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lifecycle callback tests shared by the annotation and the {@code orm.xml} variants,
 * each of which supplies its own domain model.
 */
public abstract class AbstractJpa4LifecycleCallbackTests {

	protected abstract NamedEntity newLifecycleEntity(Integer id, String name);

	protected abstract Class<? extends NamedEntity> lifecycleEntityClass();

	protected abstract NamedEntity newMutatingEntity(Integer id, String name);

	protected abstract Class<? extends NamedEntity> mutatingEntityClass();

	protected abstract NamedEntity newPlainEntity(Integer id, String name);

	protected abstract NamedEntity newListenedEntity(Integer id, String name);

	protected abstract Class<? extends NamedEntity> listenedEntityClass();

	protected abstract NamedEntity newSuperclassEntity(Integer id, String name);

	protected abstract Class<? extends NamedEntity> superclassEntityClass();

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
		scope.inTransaction( session -> session.persist( newLifecycleEntity( 1, "created" ) ) );

		assertThat( Events.names ).containsExactly(
				"pre-persist:created",
				"pre-insert:created",
				"post-persist:created",
				"post-insert:created"
		);

		Events.reset();

		scope.inTransaction( session -> {
			final NamedEntity entity = session.get( lifecycleEntityClass(), 1 );
			entity.setName( "deleted" );
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
		scope.inTransaction( session -> session.persist( newLifecycleEntity( 1, "original" ) ) );
		Events.reset();

		scope.inTransaction( session -> session.merge( newLifecycleEntity( 1, "merged" ) ) );

		assertThat( Events.names ).containsExactly( "pre-merge:merged" );
		scope.inTransaction( session -> {
			final NamedEntity entity = session.get( lifecycleEntityClass(), 1 );
			assertThat( entity.getName() ).isEqualTo( "merged-pre-merge" );
		} );
	}

	@Test
	void preInsertMutationIsInserted(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( newMutatingEntity( 1, "created" ) ) );

		assertThat( Events.names ).containsExactly( "mutating-pre-insert:created" );
		scope.inTransaction( session -> {
			final NamedEntity entity = session.get( mutatingEntityClass(), 1 );
			assertThat( entity.getName() ).isEqualTo( "created-pre-insert" );
		} );
	}

	@Test
	void entityAgentUpsertCallbacksFire(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> session.upsert( newLifecycleEntity( 1, "upserted" ) ) );

		assertThat( Events.names ).containsExactly(
				"pre-upsert:upserted",
				"post-upsert:upserted"
		);
	}

	@Test
	void preUpsertMutationIsUpserted(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> session.upsert( newMutatingEntity( 1, "upserted" ) ) );

		assertThat( Events.names ).containsExactly( "mutating-pre-upsert:upserted" );
		scope.inTransaction( session -> {
			final NamedEntity entity = session.get( mutatingEntityClass(), 1 );
			assertThat( entity.getName() ).isEqualTo( "upserted-pre-upsert" );
		} );
	}

	@Test
	void runtimeListenerCanTargetEntitySupertype(SessionFactoryScope scope) {
		final var registration = scope.getSessionFactory()
				.addListener( Object.class, PreInsert.class, Events::runtimePreInsert );

		scope.inTransaction( session -> {
			session.persist( newPlainEntity( 1, "plain" ) );
			session.persist( newLifecycleEntity( 2, "lifecycle" ) );
		} );

		assertThat( Events.names ).contains(
				"runtime-pre-insert:PlainEntity",
				"runtime-pre-insert:LifecycleEntity"
		);

		registration.cancel();
		Events.reset();

		scope.inTransaction( session -> session.persist( newPlainEntity( 3, "plain-after-cancel" ) ) );

		assertThat( Events.names ).isEmpty();
	}

	@Test
	void entityListenerCallbacksFire(SessionFactoryScope scope) {
		assertCallbacksFire( scope, "listener", this::newListenedEntity, listenedEntityClass() );
	}

	@Test
	void mappedSuperclassCallbacksFire(SessionFactoryScope scope) {
		assertCallbacksFire( scope, "superclass", this::newSuperclassEntity, superclassEntityClass() );
	}

	private static void assertCallbacksFire(
			final SessionFactoryScope scope,
			final String prefix,
			final EntityFactory factory,
			final Class<? extends NamedEntity> entityClass) {
		scope.inTransaction( session -> session.persist( factory.create( 1, "created" ) ) );

		assertThat( Events.names ).containsExactly(
				prefix + "-pre-persist:created",
				prefix + "-pre-insert:created",
				prefix + "-post-persist:created",
				prefix + "-post-insert:created"
		);

		Events.reset();

		scope.inTransaction( session -> session.merge( factory.create( 1, "merged" ) ) );

		assertThat( Events.names ).containsExactly( prefix + "-pre-merge:merged" );

		Events.reset();

		scope.inStatelessTransaction( session -> session.upsert( factory.create( 1, "upserted" ) ) );

		assertThat( Events.names ).containsExactly(
				prefix + "-pre-upsert:upserted",
				prefix + "-post-upsert:upserted"
		);

		Events.reset();

		scope.inTransaction( session -> {
			final NamedEntity entity = session.get( entityClass, 1 );
			entity.setName( "deleted" );
			session.remove( entity );
		} );

		assertThat( Events.names ).containsExactly(
				prefix + "-pre-remove:deleted",
				prefix + "-pre-delete:deleted",
				prefix + "-post-remove:deleted",
				prefix + "-post-delete:deleted"
		);
	}

	@FunctionalInterface
	interface EntityFactory {
		NamedEntity create(Integer id, String name);
	}

	public interface NamedEntity {
		String getName();

		void setName(String name);
	}

	public static class Events {
		static final List<String> names = new ArrayList<>();

		static void reset() {
			names.clear();
		}

		static void record(String event, Object entity) {
			names.add( event + ":" + ( (NamedEntity) entity ).getName() );
		}

		static void runtimePreInsert(Object entity) {
			names.add( "runtime-pre-insert:" + entity.getClass().getSimpleName() );
		}
	}
}
