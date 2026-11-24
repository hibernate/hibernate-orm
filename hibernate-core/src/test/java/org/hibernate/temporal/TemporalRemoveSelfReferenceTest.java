package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Temporal;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-21219")
class TemporalRemoveSelfReferenceTest {
	private static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";

	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = Node.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void singleTable(SessionFactoryScope scope) {
		removeParentThenChild( scope );
	}

	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = Node.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void historyTable(SessionFactoryScope scope) {
		removeParentThenChild( scope );
	}

	private void removeParentThenChild(SessionFactoryScope scope) {
		final var before = scope.fromTransaction( session -> {
			final var parent = new Node( 1, null );
			session.persist( parent );
			session.persist( new Node( 2, parent ) );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> {
			final var child = session.find( Node.class, 2 );
			session.remove( child.parent );
			session.remove( child );
		} );

		final String hql = "select count(*) from Node";
		scope.inSession( session -> assertEquals( 0L,
				session.createSelectionQuery( hql, Long.class ).getSingleResult() ) );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( before ).openSession()) {
			assertEquals( 2L, session.createSelectionQuery( hql, Long.class ).getSingleResult() );
			assertEquals( 1, session.createSelectionQuery(
					"select n.parent.id from Node n where n.id = 2", Integer.class ).getSingleResult() );
		}
	}

	@Entity(name = "Node")
	@Temporal
	static class Node {
		@Id
		int id;
		@ManyToOne
		Node parent;

		Node() {
		}

		Node(int id, Node parent) {
			this.id = id;
			this.parent = parent;
		}
	}
}
