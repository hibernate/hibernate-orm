package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Temporal;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.cfg.FlushSettings.GRAPH_DEFER_IDENTITY_INSERTS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@JiraKey("HHH-21217")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
class TemporalIdentityOrderingTest {
	private static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";

	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = { Parent.class, Child.class })
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
			@Setting(name = GRAPH_DEFER_IDENTITY_INSERTS, value = "true")
	})
	void deferredParentInsertPrecedesChildBatch(SessionFactoryScope scope) {
		final var previous = scope.fromTransaction( session -> {
			final var parent = new Parent();
			session.persist( parent );
			return parent;
		} );
		final var parent = scope.fromTransaction( session -> {
			session.persist( new Child( 1, session.getReference( Parent.class, previous.id ) ) );
			final var next = new Parent();
			session.persist( next );
			assertNull( next.id );
			session.persist( new Child( 2, next ) );
			return next;
		} );

		assertNotNull( parent.id );
		scope.inSession( session -> assertEquals( parent.id, session.createSelectionQuery(
				"select c.parent.id from Child c where c.id = 2", Long.class ).getSingleResult() ) );
	}

	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = { AssignedNode.class, IdentityNode.class })
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void earlyIdentityInsertCompletesInitialVersion(SessionFactoryScope scope) {
		checkInitialVersion( scope, null );
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 5 })
	@SessionFactory
	@DomainModel(annotatedClasses = { AssignedNode.class, IdentityNode.class })
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void earlyIdentityInsertCompletesInitialHistoryVersion(int batchSize, SessionFactoryScope scope) {
		checkInitialVersion( scope, batchSize );
	}

	private static void checkInitialVersion(SessionFactoryScope scope, Integer batchSize) {
		final var changeset = scope.fromTransaction( session -> {
			session.setJdbcBatchSize( batchSize );
			final var node = new AssignedNode();
			node.id = 1L;
			final var peer = new IdentityNode();
			node.peer = peer;
			peer.peer = node;
			session.persist( node );
			// Obtaining the identity executes the pending assigned-id insert as well.
			session.persist( peer );
			assertNotNull( peer.id );
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );

		final String hql = "select n.id from AssignedNode n join n.peer p where p.peer.id = n.id";
		scope.inSession( session -> assertEquals( 1L, session.createSelectionQuery( hql, Long.class ).getSingleResult() ) );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			assertEquals( 1L, session.createSelectionQuery( hql, Long.class ).getSingleResult() );
		}
	}

	@Entity(name = "Parent")
	@Temporal
	static class Parent {
		@Id @GeneratedValue(strategy = IDENTITY) Long id;
	}

	@Entity(name = "Child")
	static class Child {
		@Id int id;
		@ManyToOne(optional = false) Parent parent;

		Child() {}
		Child(int id, Parent parent) {
			this.id = id;
			this.parent = parent;
		}
	}

	@Entity(name = "AssignedNode")
	@Temporal
	static class AssignedNode {
		@Id long id;
		@ManyToOne IdentityNode peer;
	}

	@Entity(name = "IdentityNode")
	@Temporal
	static class IdentityNode {
		@Id @GeneratedValue(strategy = IDENTITY) Long id;
		@ManyToOne AssignedNode peer;
	}
}
