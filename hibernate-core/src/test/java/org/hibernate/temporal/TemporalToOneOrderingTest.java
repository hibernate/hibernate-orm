package org.hibernate.temporal;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

import org.hibernate.annotations.Temporal;
import org.hibernate.audit.AuditLog;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-21217")
@SessionFactory
@DomainModel(annotatedClasses = { TemporalToOneOrderingTest.Node.class, TemporalToOneOrderingTest.Peer.class,
		TemporalToOneOrderingTest.VersionedNode.class, TemporalToOneOrderingTest.PersistPeer.class })
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets")
})
class TemporalToOneOrderingTest {
	@Test
	void foreignKeys(SessionFactoryScope scope) {
		assertEquals( 0, scope.getSessionFactory().getMappingMetamodel().getConstraintModel().foreignKeys().size() );
	}

	@Test
	void updateSelfReference(SessionFactoryScope scope) {
		final var before = scope.fromTransaction( session -> {
			final var node = new Node( 1 );
			node.parent = node;
			session.persist( node );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> session.find( Node.class, 1 ).revision = 1 );
		final String hql = "select n.revision from Node n where n.parent.id = 1";
		try (var session = scope.getSessionFactory().withOptions().atChangeset( before ).openSession()) {
			assertEquals( List.of( 0 ), session.createSelectionQuery( hql, Integer.class ).list() );
		}
		scope.inSession( session -> assertEquals( List.of( 1 ), session.createSelectionQuery( hql, Integer.class ).list() ) );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsPrimaryKeyUpdate.class)
	void persistMutualReferences(SessionFactoryScope scope) {
		final var changeset = scope.fromTransaction( session -> {
			final var node = new VersionedNode( 3 );
			final var peer = new PersistPeer();
			peer.id = 4;
			node.peer = peer;
			peer.node = node;
			session.persist( node );
			session.persist( peer );
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		final String hql = "select n.version from VersionedNode n join n.peer p where n.id = 3 and p.id = 4 and p.node.id = n.id";
		final var version = scope.fromSession( session -> session.createSelectionQuery( hql, Integer.class ).getSingleResult() );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			assertEquals( version, session.createSelectionQuery( hql, Integer.class ).getSingleResult() );
		}
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsPrimaryKeyUpdate.class)
	void persistMutualReferencesOfSameType(SessionFactoryScope scope) {
		final var changeset = scope.fromTransaction( session -> {
			final var first = new VersionedNode( 5 );
			final var second = new VersionedNode( 6 );
			first.parent = second;
			second.parent = first;
			session.persist( first );
			session.persist( second );
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		final String hql = "select p.id from VersionedNode n join n.parent p where n.id = 5 and p.parent.id = n.id";
		scope.inSession( session -> assertEquals( List.of( 6 ),
				session.createSelectionQuery( hql, Integer.class ).list() ) );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			assertEquals( List.of( 6 ), session.createSelectionQuery( hql, Integer.class ).list() );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( AuditLog.ALL_CHANGESETS ).openSession()) {
			assertEquals( List.of( 5, 6 ), session.createSelectionQuery(
					"select n.id from VersionedNode n where n.id in (5, 6) order by n.id", Integer.class ).list() );
		}
	}

	@Test
	void updateMutualReferences(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Node( 2 ) );
			final var peer = new Peer();
			peer.id = 2;
			session.persist( peer );
		} );
		final var before = scope.fromTransaction( session -> {
			final var node = session.find( Node.class, 2 );
			final var peer = session.find( Peer.class, 2 );
			node.peer = peer;
			peer.node = node;
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> {
			final var node = session.find( Node.class, 2 );
			node.revision = 1;
			node.peer.revision = 2;
		} );
		final String hql = "select n.revision + p.revision from Node n join n.peer p where n.id = 2 and p.node.id = n.id";
		try (var session = scope.getSessionFactory().withOptions().atChangeset( before ).openSession()) {
			assertEquals( List.of( 0 ), session.createSelectionQuery( hql, Integer.class ).list() );
		}
		scope.inSession( session -> assertEquals( List.of( 3 ), session.createSelectionQuery( hql, Integer.class ).list() ) );
	}

	@Entity(name = "Node")
	@Temporal
	static class Node {
		@Id int id;
		int revision;
		@ManyToOne Node parent;
		@ManyToOne Peer peer;

		Node() {}
		Node(int id) {
			this.id = id;
		}
	}

	@Entity(name = "Peer")
	@Temporal
	static class Peer {
		@Id int id;
		int revision;
		@ManyToOne Node node;
	}

	@Entity(name = "VersionedNode")
	@Temporal
	static class VersionedNode {
		@Id int id;
		@Version int version;
		@ManyToOne VersionedNode parent;
		@ManyToOne PersistPeer peer;

		VersionedNode() {}
		VersionedNode(int id) {
			this.id = id;
		}
	}

	@Entity(name = "PersistPeer")
	@Temporal
	static class PersistPeer {
		@Id int id;
		@ManyToOne VersionedNode node;
	}
}
