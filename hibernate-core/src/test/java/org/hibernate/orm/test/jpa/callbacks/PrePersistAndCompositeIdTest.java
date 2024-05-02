package org.hibernate.orm.test.jpa.callbacks;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@Jpa(
		annotatedClasses = {
				PrePersistAndCompositeIdTest.Parent.class,
				PrePersistAndCompositeIdTest.Child.class,
		}
)
@JiraKey("HHH-18032")
public class PrePersistAndCompositeIdTest {

	private static final Long PARENT_ID1 = 1L;
	private static final Long PARENT_ID2 = 2L;
	private static final Long PARENT_ID_2_2 = 3L;


	private static final Long CHILD_ID1 = 3L;
	private static final Long CHILD_ID2 = 4L;
	private static final Long CHILD_ID_2_2 = 5L;

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Child" ).executeUpdate();
					entityManager.createQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	public void testMerge(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Child child = new Child( CHILD_ID1, null, "child" );
					Child merged = entityManager.merge( child );
					Parent parent = new Parent( PARENT_ID1, null, "parent", merged );
					entityManager.merge( parent );
				}
		);

		scope.inTransaction(
				session -> {
					Parent parent = session.find( Parent.class, new CompositeId( PARENT_ID1, PARENT_ID2 ) );
					assertThat( parent ).isNotNull();
					assertThat( parent.getChildren().size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testPersist(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Child child = new Child( CHILD_ID1, null, "child" );
					entityManager.persist( child );
					Parent parent = new Parent( PARENT_ID1, null, "parent", child );
					entityManager.persist( parent );
				}
		);

		scope.inTransaction(
				session -> {
					Parent parent = session.find( Parent.class, new CompositeId( PARENT_ID1, PARENT_ID2 ) );
					assertThat( parent ).isNotNull();
					assertThat( parent.getChildren().size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testMergeAssignBothId(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					Child child = new Child( CHILD_ID1, CHILD_ID_2_2, "child" );
					Child merged = entityManager.merge( child );
					Parent parent = new Parent( PARENT_ID1, PARENT_ID_2_2, "parent", merged );
					entityManager.merge( parent );
				}
		);
		scope.inTransaction(
				session -> {
					Parent parent = session.find( Parent.class, new CompositeId( PARENT_ID1, PARENT_ID_2_2 ) );
					assertThat( parent ).isNotNull();
					assertThat( parent.getChildren().size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testPersistAssignBothId(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Child child = new Child( CHILD_ID1, CHILD_ID_2_2, "child" );
					entityManager.persist( child );
					Parent parent = new Parent( PARENT_ID1, PARENT_ID_2_2, "parent", child );
					entityManager.persist( parent );
				}
		);
		scope.inTransaction(
				session -> {
					Parent parent = session.find( Parent.class, new CompositeId( PARENT_ID1, PARENT_ID_2_2 ) );
					assertThat( parent ).isNotNull();
					assertThat( parent.getChildren().size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "Parent")
	@IdClass(CompositeId.class)
	public static class Parent {
		@Id
		private Long id1;

		@Id
		private Long id2;

		private String name;

		@OneToMany(fetch = FetchType.EAGER, mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		@Fetch(FetchMode.SELECT)
		List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(Long id1, Long id2, String name, Child child) {
			this.id1 = id1;
			this.id2 = id2;
			this.name = name;
			this.children.add( child );
			child.parent = this;
		}

		@PrePersist
		public void prePersist() {
			if ( id2 == null ) {
				id2 = PARENT_ID2;
			}
		}

		public Long getId1() {
			return id1;
		}

		public Long getId2() {
			return id2;
		}

		public String getName() {
			return name;
		}

		public List<Child> getChildren() {
			return children;
		}
	}

	@Entity(name = "Child")
	@IdClass(CompositeId.class)
	public static class Child {
		@Id
		private Long id1;

		@Id
		private Long id2;

		private String name;

		@ManyToOne
		@JoinColumns({
				@JoinColumn(name = "parent_id1", referencedColumnName = "id1"),
				@JoinColumn(name = "parent_id2", referencedColumnName = "id2")
		})
		private Parent parent;

		public Child() {
		}

		public Child(Long id1, Long id2, String name) {
			this.id1 = id1;
			this.id2 = id2;
			this.name = name;
		}

		@PrePersist
		public void prePersist() {
			if ( id2 == null ) {
				id2 = CHILD_ID2;
			}
		}
	}

	public static class CompositeId {
		private Long id1;

		private Long id2;

		public CompositeId() {
		}

		public CompositeId(Long id1, Long id2) {
			this.id1 = id1;
			this.id2 = id2;
		}
	}
}
