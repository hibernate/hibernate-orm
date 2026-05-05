/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.ChangesetEntity;
import org.hibernate.audit.EntityTrackingChangesetListener;
import org.hibernate.audit.ModificationType;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests {@link EntityTrackingChangesetListener}: per-entity-change callbacks.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		EntityTrackingRevisionListenerTest.TrackedEntity.class,
		EntityTrackingRevisionListenerTest.TrackingRevisionInfo.class
})
class EntityTrackingRevisionListenerTest {

	@BeforeEach
	void clearTracker() {
		TrackingListener.changes.clear();
	}

	@Test
	void testEntityChangedCallback(SessionFactoryScope scope) {
		// REV 1: insert
		scope.getSessionFactory().inTransaction( session -> {
			var entity = new TrackedEntity();
			entity.id = 1L;
			entity.name = "Created";
			session.persist( entity );
		} );

		assertEquals( 1, TrackingListener.changes.size() );
		var change = TrackingListener.changes.get( 0 );
		assertEquals( TrackedEntity.class, change.entityClass );
		assertEquals( 1L, change.entityId );
		assertEquals( ModificationType.ADD, change.modificationType );
		assertNotNull( change.revisionEntity );

		TrackingListener.changes.clear();

		// REV 2: update
		scope.getSessionFactory().inTransaction( session ->
				session.find( TrackedEntity.class, 1L ).name = "Updated"
		);

		assertEquals( 1, TrackingListener.changes.size() );
		change = TrackingListener.changes.get( 0 );
		assertEquals( 1L, change.entityId );
		assertEquals( ModificationType.MOD, change.modificationType );

		TrackingListener.changes.clear();

		// REV 3: delete
		scope.getSessionFactory().inTransaction( session ->
				session.remove( session.find( TrackedEntity.class, 1L ) )
		);

		assertEquals( 1, TrackingListener.changes.size() );
		change = TrackingListener.changes.get( 0 );
		assertEquals( 1L, change.entityId );
		assertEquals( ModificationType.DEL, change.modificationType );
	}

	@Test
	void testMultipleEntitiesInOneTransaction(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction( session -> {
			var e1 = new TrackedEntity();
			e1.id = 10L;
			e1.name = "First";
			session.persist( e1 );
			var e2 = new TrackedEntity();
			e2.id = 11L;
			e2.name = "Second";
			session.persist( e2 );
		} );

		assertEquals( 2, TrackingListener.changes.size() );
		// Both should have the same revision entity
		assertEquals( TrackingListener.changes.get( 0 ).revisionEntity,
				TrackingListener.changes.get( 1 ).revisionEntity );
	}

	@Test
	void testRevisionEntityAccessible(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction( session -> {
			var entity = new TrackedEntity();
			entity.id = 20L;
			entity.name = "RevTest";
			session.persist( entity );
		} );

		assertEquals( 1, TrackingListener.changes.size() );
		var revEntity = (TrackingRevisionInfo) TrackingListener.changes.get( 0 ).revisionEntity;
		assertNotNull( revEntity );
		assertEquals( "tracking-user", revEntity.username );
	}

	// ---- Listener ----

	record EntityChange(
			Class<?> entityClass,
			Object entityId,
			ModificationType modificationType,
			Object revisionEntity) {
	}

	public static class TrackingListener implements EntityTrackingChangesetListener {
		static final List<EntityChange> changes = new ArrayList<>();

		@Override
		public void newChangeset(Object changesetEntity) {
			((TrackingRevisionInfo) changesetEntity).username = "tracking-user";
		}

		@Override
		public void entityChanged(
				Class<?> entityClass,
				Object entityId,
				ModificationType modificationType,
				Object revisionEntity) {
			changes.add( new EntityChange(
					entityClass, entityId,
					modificationType, revisionEntity ) );
		}
	}

	// ---- Entities ----

	@ChangesetEntity(listener = TrackingListener.class)
	@Entity(name = "TrackingRevisionInfo")
	@Table(name = "REVINFO")
	static class TrackingRevisionInfo {
		@Id
		@GeneratedValue
		@ChangesetEntity.ChangesetId
		@Column(name = "REV")
		int id;

		@ChangesetEntity.Timestamp
		@Column(name = "REVTSTMP")
		long timestamp;

		@Column(name = "USERNAME")
		String username;
	}

	@Audited
	@Entity(name = "TrackedEntity")
	static class TrackedEntity {
		@Id
		long id;
		String name;
	}
}
