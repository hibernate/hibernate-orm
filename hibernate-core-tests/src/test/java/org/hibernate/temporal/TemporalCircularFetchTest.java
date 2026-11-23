package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Temporal;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@DomainModel(annotatedClasses = { TemporalCircularFetchTest.Node.class, TemporalCircularFetchTest.Peer.class })
class TemporalCircularFetchTest {
	private static final String SUPPLIER = "org.hibernate.temporal.TemporalCircularFetchTest$ChangesetIds";

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsPrimaryKeyUpdate.class)
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void singleTable(SessionFactoryScope scope) {
		findHistoricalCycle( scope );
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void historyTable(SessionFactoryScope scope) {
		findHistoricalCycle( scope );
	}

	private void findHistoricalCycle(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var node = new Node();
			node.id = 1;
			session.persist( node );
			final var peer = new Peer();
			peer.id = 2;
			peer.name = "before";
			peer.node = node;
			session.persist( peer );
		} );
		final var changeset = scope.fromTransaction( session -> {
			session.find( Node.class, 1 ).peer = session.find( Peer.class, 2 );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> session.find( Peer.class, 2 ).name = "after" );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			final var node = session.find( Node.class, 1 );
			assertEquals( "before", node.peer.name );
			assertSame( node, node.peer.node );
			assertSame( node.peer, session.find( Peer.class, 2 ) );
			assertSame( node, session.find( Node.class, 1 ) );
		}
		scope.inSession( session -> {
			final var node = session.find( Node.class, 1 );
			assertEquals( "after", node.peer.name );
			assertSame( node, node.peer.node );
		} );
	}

	@Entity(name = "Node")
	@Temporal
	static class Node {
		@Id int id;
		@ManyToOne Peer peer;
	}

	@Entity(name = "Peer")
	@Temporal
	static class Peer {
		@Id int id;
		String name;
		@ManyToOne Node node;
	}
}
