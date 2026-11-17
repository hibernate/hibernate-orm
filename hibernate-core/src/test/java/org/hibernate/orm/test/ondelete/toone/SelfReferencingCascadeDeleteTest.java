package org.hibernate.orm.test.ondelete.toone;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.Hibernate;
import org.hibernate.PropertyValueException;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JiraKey("HHH-21133")
@DomainModel(annotatedClasses = {
		SelfReferencingCascadeDeleteTest.Node.class,
		SelfReferencingCascadeDeleteTest.EmbeddedNode.class,
		SelfReferencingCascadeDeleteTest.NestedNode.class,
		SelfReferencingCascadeDeleteTest.InheritedNode.class
})
@SessionFactory(useCollectingStatementObserver = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCircularCascadeDeleteCheck.class)
@BytecodeEnhanced(runNotEnhancedAsWell = true, testEnhancedClasses = {
		SelfReferencingCascadeDeleteTest.Node.class,
		SelfReferencingCascadeDeleteTest.EmbeddedNode.class,
		SelfReferencingCascadeDeleteTest.NestedNode.class
})
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true)
public class SelfReferencingCascadeDeleteTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void deleteSelfReference(SessionFactoryScope scope) {
		deleteSelfReference( false, scope );
	}

	@Test
	void deleteInitializedReference(SessionFactoryScope scope) {
		deleteSelfReference( true, scope );
	}

	private void deleteSelfReference(boolean reference, SessionFactoryScope scope) {
		persistTree( scope );
		scope.inTransaction( session -> {
			var root = reference ? session.getReference( Node.class, 1 ) : session.find( Node.class, 1 );
			Hibernate.initialize( root );
			scope.getCollectingStatementObserver().clear();
			session.remove( root );
		} );
		assertMutations( scope, 0, 1 );
		assertEmpty( scope, "CascadeNode" );
	}

	@Test
	void deleteEmbeddedSelfReference(SessionFactoryScope scope) {
		deleteEmbeddedSelfReference( false, scope );
	}

	@Test
	void deleteEmbeddedSelfReferenceWithNullableSibling(SessionFactoryScope scope) {
		deleteEmbeddedSelfReference( true, scope );
	}

	private void deleteEmbeddedSelfReference(boolean nullableSibling, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var root = new EmbeddedNode();
			root.id = 1;
			root.link = new Link();
			root.link.parent = root;
			if ( nullableSibling ) {
				root.link.otherParent = root;
			}
			session.persist( root );

			var child = new EmbeddedNode();
			child.id = 2;
			child.link = new Link();
			child.link.parent = root;
			session.persist( child );
		} );
		scope.inTransaction( session -> {
			var root = session.find( EmbeddedNode.class, 1 );
			scope.getCollectingStatementObserver().clear();
			session.remove( root );
		} );
		final boolean nullificationRequired = scope.getSessionFactory().getJdbcServices().getDialect()
				.getForeignKeySupport().requiresSelfReferentialForeignKeyNullification();
		assertMutations( scope, nullableSibling && nullificationRequired ? 1 : 0, 1 );
		assertEmpty( scope, "EmbeddedCascadeNode" );
	}

	@Test
	void deleteNestedEmbeddedSelfReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var root = new NestedNode();
			root.id = 1;
			root.outer = new OuterLink();
			root.outer.inner = new InnerLink();
			root.outer.inner.parent = root;
			session.persist( root );

			var child = new NestedNode();
			child.id = 2;
			child.outer = new OuterLink();
			child.outer.inner = new InnerLink();
			child.outer.inner.parent = root;
			session.persist( child );
		} );
		scope.inTransaction( session -> {
			var root = session.find( NestedNode.class, 1 );
			scope.getCollectingStatementObserver().clear();
			session.remove( root );
		} );
		assertMutations( scope, 0, 1 );
		assertEmpty( scope, "NestedCascadeNode" );
	}

	@Test
	void deleteInheritedSelfReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var root = new InheritedNode();
			root.id = 1;
			root.parent = root;
			root.beforeParent = "before";
			root.afterParent = "after";
			session.persist( root );
		} );
		scope.inTransaction( session -> {
			var root = session.find( InheritedNode.class, 1 );
			scope.getCollectingStatementObserver().clear();
			session.remove( root );
		} );
		assertMutations( scope, 0, 1 );
		assertEmpty( scope, "InheritedCascadeNode" );
	}

	@Test
	void deletingChildDoesNotDeleteItsParent(SessionFactoryScope scope) {
		persistTree( scope );
		scope.inTransaction( session -> {
			var child = session.find( Node.class, 2 );
			scope.getCollectingStatementObserver().clear();
			session.remove( child );
		} );
		assertMutations( scope, 0, 1 );
		scope.inTransaction( session -> {
			assertEquals( 1L, session.createSelectionQuery(
					"select count(*) from CascadeNode", Long.class ).getSingleResult() );
			assertEquals( 1, session.find( Node.class, 1 ).id );
		} );
	}

	@Test
	void nonSelfReferenceToDeletedParentStillFails(SessionFactoryScope scope) {
		persistTree( scope );
		scope.inSession( session -> {
			final var transaction = session.beginTransaction();
			try {
				var root = session.find( Node.class, 1 );
				var child = session.find( Node.class, 2 );
				session.remove( root );
				final var exception = assertThrows( PropertyValueException.class, () -> session.remove( child ) );
				assertEquals( "parent", exception.getPropertyName() );
			}
			finally {
				transaction.rollback();
			}
		} );
	}

	@Test
	void nullAssociationStillFailsOnInsert(SessionFactoryScope scope) {
		final var exception = assertThrows( PropertyValueException.class, () -> scope.inTransaction( session -> {
			var node = new Node();
			node.id = 1;
			session.persist( node );
			session.flush();
		} ) );
		assertEquals( "parent", exception.getPropertyName() );
	}

	@Test
	void transientAssociationStillFailsOnInsert(SessionFactoryScope scope) {
		final var exception = assertThrows( IllegalStateException.class, () -> scope.inTransaction( session -> {
			var node = new Node();
			node.id = 1;
			node.parent = new Node();
			node.parent.id = 2;
			session.persist( node );
			session.flush();
		} ) );
		assertInstanceOf( TransientPropertyValueException.class, exception.getCause() );
	}

	private void persistTree(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var root = new Node();
			root.id = 1;
			root.parent = root;
			session.persist( root );

			var child = new Node();
			child.id = 2;
			child.parent = root;
			session.persist( child );
		} );
	}

	private void assertEmpty(SessionFactoryScope scope, String entityName) {
		scope.inTransaction( session -> assertEquals( 0L, session.createSelectionQuery(
				"select count(*) from " + entityName, Long.class ).getSingleResult() ) );
	}

	private void assertMutations(SessionFactoryScope scope, int updates, int deletes) {
		final var observer = scope.getCollectingStatementObserver();
		observer.assertQueries().hasSize( updates + deletes );
		for ( int i = 0; i < updates; i++ ) {
			observer.assertQuery( i ).startsWith( "update" );
		}
		for ( int i = updates; i < updates + deletes; i++ ) {
			observer.assertQuery( i ).startsWith( "delete" );
		}
	}

	@Entity(name = "CascadeNode")
	public static class Node {
		@Id
		Integer id;
		@ManyToOne(optional = false)
		@OnDelete(action = OnDeleteAction.CASCADE)
		Node parent;
	}

	@Entity(name = "EmbeddedCascadeNode")
	public static class EmbeddedNode {
		@Id
		Integer id;
		@Embedded
		Link link;
	}

	@Embeddable
	public static class Link {
		@ManyToOne(optional = false)
		@OnDelete(action = OnDeleteAction.CASCADE)
		EmbeddedNode parent;

		@ManyToOne
		EmbeddedNode otherParent;
	}

	@Entity(name = "NestedCascadeNode")
	public static class NestedNode {
		@Id
		Integer id;
		@Embedded
		OuterLink outer;
	}

	@Embeddable
	public static class OuterLink {
		@Embedded
		InnerLink inner;
	}

	@Embeddable
	public static class InnerLink {
		@ManyToOne(optional = false)
		@OnDelete(action = OnDeleteAction.CASCADE)
		NestedNode parent;
	}

	@MappedSuperclass
	public static class NodeBase {
		@Id
		Integer id;
		String beforeParent;
		@ManyToOne(optional = false)
		@OnDelete(action = OnDeleteAction.CASCADE)
		InheritedNode parent;
	}

	@Entity(name = "InheritedCascadeNode")
	public static class InheritedNode extends NodeBase {
		String afterParent;
	}
}
