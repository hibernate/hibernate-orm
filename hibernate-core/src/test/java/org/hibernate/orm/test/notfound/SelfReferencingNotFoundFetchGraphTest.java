package org.hibernate.orm.test.notfound;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The four scenarios from the reproducer attached to HHH-21205. */
@JiraKey("HHH-21205")
@DomainModel(annotatedClasses = SelfReferencingNotFoundFetchGraphTest.Node.class)
@SessionFactory
// Clear the test suite's depth limit to match the standalone reproducer's configuration.
@ServiceRegistry(settings = @Setting(name = AvailableSettings.MAX_FETCH_DEPTH, value = ""))
class SelfReferencingNotFoundFetchGraphTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		assertTrue( SelfReferencingNotFoundFetchGraphTest.class.desiredAssertionStatus() );
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( "insert into nodes(id, parent_id) values (2, null)" )
					.executeUpdate();
			session.createNativeMutationQuery( "insert into nodes(id, parent_id) values (1, 2)" )
					.executeUpdate();
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "update Node set parent = null" ).executeUpdate();
			session.createMutationQuery( "delete from Node" ).executeUpdate();
		} );
	}

	@Test
	void fetchGraphMustLoadSelfReferencingParent(SessionFactoryScope scope) {
		loadAndCheck( scope, GraphSemantic.FETCH );
	}

	@Test
	void loadGraphLoadsSelfReferencingParent(SessionFactoryScope scope) {
		loadAndCheck( scope, GraphSemantic.LOAD );
	}

	@Test
	void leftJoinFetchLoadsSelfReferencingParent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Node root = session.createQuery( """
					select n from Node n
					left join fetch n.parent
					where n.id = :id
					""", Node.class )
					.setParameter( "id", 1L )
					.getSingleResult();
			assertParentLoaded( root );
		} );
	}

	@Test
	void missingParentIsIgnored(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( "insert into nodes(id, parent_id) values (3, 999)" )
					.executeUpdate();
			Node node = session.find( Node.class, 3L );
			assertNotNull( node );
			assertNull( node.parent );
		} );
	}

	private void loadAndCheck(SessionFactoryScope scope, GraphSemantic semantic) {
		scope.inTransaction( session -> {
			var graph = session.createEntityGraph( Node.class );
			graph.addAttributeNodes( "parent" );

			Node root = session.createQuery( "from Node n where n.id = :id", Node.class )
					.setParameter( "id", 1L )
					.setHint( semantic.getJakartaHintName(), graph )
					.getSingleResult();
			assertParentLoaded( root );
		} );
	}

	private void assertParentLoaded(Node root) {
		assertNotNull( root.parent );
		assertTrue( Hibernate.isInitialized( root.parent ) );
		assertEquals( 2L, root.parent.id );
		assertNull( root.parent.parent );
	}

	@Entity(name = "Node")
	@Table(name = "nodes")
	public static class Node {
		@Id
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "parent_id")
		Node parent;
	}
}
