/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.actionqueue;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.cfg.FlushSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Clearing an uninitialized inverse collection queues the operation instead of initializing
 * the collection, so the collection has no snapshot. It is still marked dirty, so an update
 * action is scheduled for it, and ordering those actions compared them by pending deletions,
 * which read the missing snapshot.
 */
@JiraKey("HHH-20910")
@Jpa(annotatedClasses = {
		CollectionUpdateOrderingUninitializedTest.Assignment.class,
		CollectionUpdateOrderingUninitializedTest.Replacement.class
	},
	integrationSettings = {@Setting(name = BatchSettings.ORDER_UPDATES, value = "true"),
		@Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy")})
class CollectionUpdateOrderingUninitializedTest {

	@AfterEach
	void dropTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete from Replacement" ).executeUpdate();
			session.createQuery( "delete from Assignment" ).executeUpdate();
		} );
	}

	@Test
	void clearingUninitializedCollectionsOfTheSameRole(EntityManagerFactoryScope scope) {
		createAssignmentsWithOneReplacementEach( scope );

		scope.inTransaction( session -> {
			final Assignment first = session.find( Assignment.class, 1L );
			final Assignment second = session.find( Assignment.class, 2L );
			// clearing without reading queues the operation, so no snapshot is taken
			assertFalse( Hibernate.isInitialized( first.replacedBy ) );
			assertFalse( Hibernate.isInitialized( second.replacedBy ) );
			first.replacedBy.clear();
			second.replacedBy.clear();
			session.flush();
			assertFalse( Hibernate.isInitialized( first.replacedBy ) );
			assertFalse( Hibernate.isInitialized( second.replacedBy ) );
		} );

		// the inverse side owns no column, so clearing it deletes nothing
		scope.inTransaction( session -> assertEquals( 2L, countReplacements( session ) ) );
	}

	@Test
	void clearingInitializedAndUninitializedCollectionsOfTheSameRole(EntityManagerFactoryScope scope) {
		createAssignmentsWithOneReplacementEach( scope );

		scope.inTransaction( session -> {
			final Assignment initialized = session.find( Assignment.class, 1L );
			final Assignment uninitialized = session.find( Assignment.class, 2L );
			// one action whose collection has a snapshot, one whose collection has none
			assertEquals( 1, initialized.replacedBy.size() );
			initialized.replacedBy.clear();
			assertFalse( Hibernate.isInitialized( uninitialized.replacedBy ) );
			uninitialized.replacedBy.clear();
			session.flush();
		} );

		scope.inTransaction( session -> assertEquals( 2L, countReplacements( session ) ) );
	}

	@Test
	void clearingUninitializedCollectionsOnBothSidesOfTheAssociation(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Assignment( 1L ) );
			session.persist( new Assignment( 2L ) );
			session.persist( new Replacement( 1L,
					session.getReference( Assignment.class, 1L ),
					session.getReference( Assignment.class, 2L ) ) );
			session.persist( new Replacement( 2L,
					session.getReference( Assignment.class, 2L ),
					session.getReference( Assignment.class, 1L ) ) );
		} );

		scope.inTransaction( session -> {
			final Assignment first = session.find( Assignment.class, 1L );
			final Assignment second = session.find( Assignment.class, 2L );
			// both roles are cleared from both owners without either being read
			first.replacedBy.clear();
			second.replacedBy.clear();
			first.replacing.clear();
			second.replacing.clear();
			// the owning side is removed explicitly, since the inverse side writes nothing
			session.remove( session.find( Replacement.class, 1L ) );
			session.remove( session.find( Replacement.class, 2L ) );
			session.flush();
		} );

		scope.inTransaction( session -> assertEquals( 0L, countReplacements( session ) ) );
	}

	private static void createAssignmentsWithOneReplacementEach(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Assignment( 1L ) );
			session.persist( new Assignment( 2L ) );
			session.persist( new Replacement( 1L, session.getReference( Assignment.class, 1L ) ) );
			session.persist( new Replacement( 2L, session.getReference( Assignment.class, 2L ) ) );
		} );
	}

	private static long countReplacements(jakarta.persistence.EntityManager session) {
		return session.createQuery( "select count(r) from Replacement r", Long.class ).getSingleResult();
	}

	@Entity(name = "Assignment")
	static class Assignment {
		@Id
		Long id;

		@OneToMany(mappedBy = "oldAssignment", fetch = FetchType.LAZY)
		Set<Replacement> replacedBy = new HashSet<>();

		@OneToMany(mappedBy = "newAssignment", fetch = FetchType.LAZY)
		Set<Replacement> replacing = new HashSet<>();

		Assignment() {
		}

		Assignment(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "Replacement")
	static class Replacement {
		@Id
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		Assignment oldAssignment;

		@ManyToOne(fetch = FetchType.LAZY)
		Assignment newAssignment;

		Replacement() {
		}

		Replacement(Long id, Assignment oldAssignment) {
			this( id, oldAssignment, null );
		}

		Replacement(Long id, Assignment oldAssignment, Assignment newAssignment) {
			this.id = id;
			this.oldAssignment = oldAssignment;
			this.newAssignment = newAssignment;
		}
	}
}
