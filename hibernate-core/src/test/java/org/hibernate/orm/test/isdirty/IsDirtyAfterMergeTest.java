package org.hibernate.orm.test.isdirty;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Merging a detached instance which was not modified leaves the session clean,
 * when the entity has an unowned collection.
 */
@DomainModel(annotatedClasses = IsDirtyAfterMergeTest.Node.class)
@SessionFactory
@JiraKey("HHH-11310")
class IsDirtyAfterMergeTest {
	private static final long PARENT = 1;
	private static final long LEAF = 2;

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Node parent = new Node( PARENT, null );
			session.persist( parent );
			session.persist( new Node( LEAF, parent ) );
			session.persist( new Node( 3, parent ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@CsvSource({
			PARENT + ", false",
			PARENT + ", true",
			LEAF + ", false",
			LEAF + ", true"
	})
	void mergeOfUnmodifiedDetachedInstance(long id, boolean initializeChildren, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Node node = session.find( Node.class, id );
			if ( initializeChildren ) {
				Hibernate.initialize( node.children );
			}
			session.detach( node );
			assertFalse( session.isDirty() );
			session.merge( node );
			assertFalse( session.isDirty() );
		} );
	}

	@Entity(name = "Node")
	@Table(name = "dirty_merge_node")
	static class Node {
		@Id
		Long id;
		@ManyToOne
		Node parent;
		@OneToMany(mappedBy = "parent")
		Set<Node> children = new HashSet<>();

		Node() {
		}

		Node(long id, Node parent) {
			this.id = id;
			this.parent = parent;
		}
	}
}
