package org.hibernate.orm.test.notfound;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-21205")
@DomainModel(annotatedClasses = SelfReferencingNotFoundFetchGraphAdditionalTest.Node.class)
@SessionFactory
// Clear the test suite's default depth limit to exercise association-key circularity detection.
@ServiceRegistry(settings = @Setting(name = AvailableSettings.MAX_FETCH_DEPTH, value = ""))
class SelfReferencingNotFoundFetchGraphAdditionalTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( "insert into graph_node(id, parent_id) values (2, null)" )
					.executeUpdate();
			session.createNativeMutationQuery( "insert into graph_node(id, parent_id) values (1, 2)" )
					.executeUpdate();
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Node" ).executeUpdate() );
	}

	@ParameterizedTest
	@EnumSource(GraphSemantic.class)
	void testParent(GraphSemantic semantic, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Node root = loadNode( session, semantic );
			assertParentLoaded( root );
			assertNull( root.parent.parent );
		} );
	}

	@ParameterizedTest
	@EnumSource(GraphSemantic.class)
	void testMissingParent(GraphSemantic semantic, SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createNativeMutationQuery(
				"update graph_node set parent_id = 999 where id = 1"
		).executeUpdate() );
		scope.inTransaction( session -> assertNull( loadNode( session, semantic ).parent ) );
	}

	@ParameterizedTest
	@EnumSource(GraphSemantic.class)
	void testMissingGrandparent(GraphSemantic semantic, SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createNativeMutationQuery(
				"update graph_node set parent_id = 999 where id = 2"
		).executeUpdate() );
		scope.inTransaction( session -> {
			final Node root = loadNode( session, semantic );
			assertParentLoaded( root );
			// The graph omits parent.parent, but @NotFound(IGNORE) still requires resolving it.
			assertNull( root.parent.parent );
		} );
	}

	@ParameterizedTest
	@EnumSource(GraphSemantic.class)
	void testGrandparent(GraphSemantic semantic, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( "insert into graph_node(id, parent_id) values (3, null)" )
					.executeUpdate();
			session.createNativeMutationQuery( "update graph_node set parent_id = 3 where id = 2" )
					.executeUpdate();
		} );
		scope.inTransaction( session -> {
			final Node root = loadNode( session, semantic );
			assertParentLoaded( root );
			assertNotNull( root.parent.parent );
			assertTrue( Hibernate.isInitialized( root.parent.parent ) );
			assertEquals( 3L, root.parent.parent.id );
			assertNull( root.parent.parent.parent );
		} );
	}

	@ParameterizedTest
	@EnumSource(GraphSemantic.class)
	void testCycle(GraphSemantic semantic, SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createNativeMutationQuery(
				"update graph_node set parent_id = 1 where id = 2"
		).executeUpdate() );
		scope.inTransaction( session -> {
			final Node root = loadNode( session, semantic );
			assertParentLoaded( root );
			assertSame( root, root.parent.parent );
		} );
	}

	@Test
	void testJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Node root = session.createQuery(
					"from Node n left join fetch n.parent where n.id = :id", Node.class
			).setParameter( "id", 1L ).getSingleResult();
			assertParentLoaded( root );
			assertNull( root.parent.parent );
		} );
	}

	private Node loadNode(Session session, GraphSemantic semantic) {
		final var graph = session.createEntityGraph( Node.class );
		graph.addAttributeNodes( "parent" );
		return session.createQuery( "from Node n where n.id = :id", Node.class )
				.setParameter( "id", 1L )
				.setHint( semantic.getJakartaHintName(), graph )
				.getSingleResult();
	}

	private void assertParentLoaded(Node root) {
		assertNotNull( root );
		assertNotNull( root.parent );
		assertTrue( Hibernate.isInitialized( root.parent ) );
		assertEquals( 2L, root.parent.id );
	}

	@Entity(name = "Node")
	@Table(name = "graph_node")
	public static class Node {
		@Id
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "parent_id")
		Node parent;
	}
}
