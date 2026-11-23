package org.hibernate.orm.test.ondelete.toone;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

import org.hibernate.PropertyValueException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

@JiraKey("HHH-21133")
@DomainModel(annotatedClasses = {
		SelfReferencingCascadeDeleteNullificationTest.NullableNode.class,
		SelfReferencingCascadeDeleteNullificationTest.NullableEmbeddedNode.class,
		SelfReferencingCascadeDeleteNullificationTest.RestrictiveNode.class,
		SelfReferencingCascadeDeleteNullificationTest.NonNullableNode.class,
		SelfReferencingCascadeDeleteNullificationTest.JoinedBase.class,
		SelfReferencingCascadeDeleteNullificationTest.JoinedChild.class
})
@SessionFactory(useCollectingStatementObserver = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.HasSelfReferentialForeignKeyBugCheck.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCircularCascadeDeleteCheck.class)
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true)
public class SelfReferencingCascadeDeleteNullificationTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void deleteWithNullableCascadingSelfReference(SessionFactoryScope scope) {
		deleteWithNullableCascadingSelfReferences( false, scope );
	}

	@Test
	void deleteWithTwoNullableCascadingSelfReferences(SessionFactoryScope scope) {
		deleteWithNullableCascadingSelfReferences( true, scope );
	}

	private void deleteWithNullableCascadingSelfReferences(boolean twoReferences, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var root = new NullableNode();
			root.id = 1;
			root.firstParent = root;
			root.secondParent = twoReferences ? root : null;
			session.persist( root );

			var child = new NullableNode();
			child.id = 2;
			child.firstParent = root;
			child.secondParent = twoReferences ? root : null;
			session.persist( child );
		} );
		scope.inTransaction( session -> {
			var root = session.find( NullableNode.class, 1 );
			session.remove( root );
		} );
		assertEmpty( scope, "NullableCascadeNode" );
	}

	@Test
	void deleteWithNullableEmbeddedCascade(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var root = new NullableEmbeddedNode();
			root.id = 1;
			root.link = new NullableLink();
			root.link.parent = root;
			session.persist( root );
		} );
		scope.inTransaction( session -> {
			var root = session.find( NullableEmbeddedNode.class, 1 );
			session.remove( root );
		} );
		assertEmpty( scope, "NullableEmbeddedCascadeNode" );
	}

	@Test
	void nullableRestrictiveSelfReferenceStillNeedsNullification(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var root = new RestrictiveNode();
			root.id = 1;
			root.parent = root;
			session.persist( root );
		} );
		scope.inTransaction( session -> {
			var root = session.find( RestrictiveNode.class, 1 );
			scope.getCollectingStatementObserver().clear();
			session.remove( root );
		} );
		assertNullificationThenDelete( scope, 1 );
		assertEmpty( scope, "RestrictiveNode" );
	}

	@Test
	void nonNullableSelfReferenceWithoutCascadeStillFails(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var root = new NonNullableNode();
			root.id = 1;
			root.parent = root;
			session.persist( root );
		} );
		final var exception = assertThrows( PropertyValueException.class, () -> scope.inTransaction( session -> {
			var root = session.find( NonNullableNode.class, 1 );
			scope.getCollectingStatementObserver().clear();
			session.remove( root );
		} ) );
		assertEquals( "parent", exception.getPropertyName() );
		scope.getCollectingStatementObserver().assertQueries().isEmpty();
	}

	@Test
	void joinedSelfReferencePreservesExistingDeletionBehavior(SessionFactoryScope scope) {
		// On these dialects, the existing nullification also prevents the subclass deletion
		// from cascading to the root row before Hibernate deletes it.
		scope.inTransaction( session -> {
			var node = new JoinedChild();
			node.id = 1;
			session.persist( node );
			session.flush();
			node.setSelfReference( node );
		} );
		scope.inTransaction( session -> {
			var node = session.find( JoinedChild.class, 1 );
			assertEquals( 1, node.selfReference.id );
			scope.getCollectingStatementObserver().clear();
			session.remove( node );
		} );
		assertNullificationThenDelete( scope, 2 );
		assertEmpty( scope, "JoinedCascadeBase" );
		assertEmpty( scope, "JoinedCascadeChild" );
	}

	private void assertNullificationThenDelete(SessionFactoryScope scope, int deletes) {
		final var observer = scope.getCollectingStatementObserver();
		observer.assertQueries().hasSize( 1 + deletes );
		observer.assertQuery( 0 ).startsWith( "update" );
		for ( int i = 1; i <= deletes; i++ ) {
			observer.assertQuery( i ).startsWith( "delete" );
		}
	}

	private void assertEmpty(SessionFactoryScope scope, String entityName) {
		scope.inTransaction( session -> assertEquals( 0L, session.createSelectionQuery(
				"select count(*) from " + entityName, Long.class ).getSingleResult() ) );
	}

	@Entity(name = "NullableCascadeNode")
	public static class NullableNode {
		@Id
		Integer id;
		@ManyToOne
		@OnDelete(action = OnDeleteAction.CASCADE)
		NullableNode firstParent;
		@ManyToOne
		@OnDelete(action = OnDeleteAction.CASCADE)
		NullableNode secondParent;
	}

	@Entity(name = "NullableEmbeddedCascadeNode")
	public static class NullableEmbeddedNode {
		@Id
		Integer id;
		@Embedded
		NullableLink link;
	}

	@Embeddable
	public static class NullableLink {
		@ManyToOne
		@OnDelete(action = OnDeleteAction.CASCADE)
		NullableEmbeddedNode parent;
	}

	@Entity(name = "RestrictiveNode")
	public static class RestrictiveNode {
		@Id
		Integer id;
		@ManyToOne
		RestrictiveNode parent;
	}

	@Entity(name = "NonNullableNode")
	public static class NonNullableNode {
		@Id
		Integer id;
		@ManyToOne(optional = false)
		NonNullableNode parent;
	}

	@Entity(name = "JoinedCascadeBase")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class JoinedBase {
		@Id
		Integer id;
		@ManyToOne
		@OnDelete(action = OnDeleteAction.CASCADE)
		JoinedChild selfReference;

		void setSelfReference(JoinedChild selfReference) {
			this.selfReference = selfReference;
		}
	}

	@Entity(name = "JoinedCascadeChild")
	public static class JoinedChild extends JoinedBase {
		String description;
	}
}
