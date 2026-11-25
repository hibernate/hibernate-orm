package org.hibernate.orm.test.collection.ordercol;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The order column of an owned one-to-many list whose join column is not
 * updatable is maintained when the list changes.
 */
@DomainModel(annotatedClasses = {
		NonUpdatableJoinColumnOrderColumnTest.Parent.class,
		NonUpdatableJoinColumnOrderColumnTest.Child.class
})
@SessionFactory
@JiraKey("HHH-13399")
class NonUpdatableJoinColumnOrderColumnTest {

	@Entity(name = "Parent")
	@Table(name = "parents")
	static class Parent {
		@Id
		Long id;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinColumn(name = "parent_id", nullable = false, updatable = false)
		@OrderColumn(name = "child_index", nullable = false)
		List<Child> children = new ArrayList<>();
	}

	@Entity(name = "Child")
	@Table(name = "children")
	static class Child {
		@Id
		Long id;
		String name;

		Child() {
		}

		Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	private static void createParent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var parent = new Parent();
			parent.id = 1L;
			parent.children.add( new Child( 1L, "a" ) );
			parent.children.add( new Child( 2L, "b" ) );
			parent.children.add( new Child( 3L, "c" ) );
			session.persist( parent );
		} );
	}

	private static List<String> namesInOrder(SessionFactoryScope scope) {
		return scope.fromTransaction( session -> session.find( Parent.class, 1L ).children.stream()
				.map( child -> child.name ).toList() );
	}

	private static List<Object[]> rows(SessionFactoryScope scope) {
		return scope.fromTransaction( session -> session.createNativeQuery(
				"select id, parent_id, child_index from children order by id", Object[].class ).getResultList() );
	}

	@Test
	void persist(SessionFactoryScope scope) {
		createParent( scope );
		assertThat( namesInOrder( scope ) ).containsExactly( "a", "b", "c" );
	}

	@Test
	void reorder(SessionFactoryScope scope) {
		createParent( scope );
		scope.inTransaction( session -> {
			final var children = session.find( Parent.class, 1L ).children;
			children.add( children.remove( 0 ) );
		} );
		assertThat( namesInOrder( scope ) ).containsExactly( "b", "c", "a" );
	}

	@Test
	void removeFromMiddle(SessionFactoryScope scope) {
		createParent( scope );
		scope.inTransaction( session -> session.find( Parent.class, 1L ).children.remove( 1 ) );
		assertThat( namesInOrder( scope ) ).containsExactly( "a", "c" );
		assertThat( rows( scope ) ).hasSize( 2 );
	}

	@Test
	void insertAtFront(SessionFactoryScope scope) {
		createParent( scope );
		scope.inTransaction( session -> session.find( Parent.class, 1L ).children.add( 0, new Child( 4L, "d" ) ) );
		assertThat( namesInOrder( scope ) ).containsExactly( "d", "a", "b", "c" );
	}

	@Test
	void append(SessionFactoryScope scope) {
		createParent( scope );
		scope.inTransaction( session -> session.find( Parent.class, 1L ).children.add( new Child( 4L, "d" ) ) );
		assertThat( namesInOrder( scope ) ).containsExactly( "a", "b", "c", "d" );
	}

	/**
	 * The join column is not updatable, so a child added to the list of another
	 * parent stays in the list of its own parent, at the same index.
	 */
	@Test
	void addToListOfAnotherParent(SessionFactoryScope scope) {
		createParent( scope );
		scope.inTransaction( session -> {
			final var other = new Parent();
			other.id = 2L;
			session.persist( other );
		} );
		scope.inTransaction( session -> session.find( Parent.class, 2L ).children
				.add( session.find( Child.class, 3L ) ) );
		assertThat( namesInOrder( scope ) ).containsExactly( "a", "b", "c" );
		final List<Child> otherChildren = scope.fromTransaction( session -> session.find( Parent.class, 2L ).children );
		assertThat( otherChildren ).isEmpty();
	}
}
