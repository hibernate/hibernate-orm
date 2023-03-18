package org.hibernate.orm.test.bytecode.enhancement.merge;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static jakarta.persistence.CascadeType.MERGE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue( jiraKey = "HHH-16322")
public class MergeUnsavedEntitiesTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Parent.class,
				Child.class
		};
	}

	@AfterEach
	public void tearDown() {
		inTransaction(
				session -> {
					session.createMutationQuery( "delete from Child" ).executeUpdate();
					session.createMutationQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	public void testMerge() {
		inTransaction(
				session -> {
					Parent parent = new Parent( 1l, 2l );
					parent = session.merge( parent );
					Child child = new Child( 2l, "first child" );
					child = session.merge( child );
					parent.addChild( child );
					parent.getId();
				}
		);

		inTransaction(
				session -> {
					Parent parent = session.find( Parent.class, 1l );
					assertThat( parent.getChildren().size() ).isEqualTo( 1 );
				}
		);

		inTransaction(
				session -> {
					Parent parent = session.find( Parent.class, 1l );
					session.merge( parent );
				}
		);

		inTransaction(
				session -> {
					Parent parent = session.find( Parent.class, 1l );
					parent.setChildren( new ArrayList<>() );
					session.merge( parent );
				}
		);

		inTransaction(
				session -> {
					Parent parent = session.find( Parent.class, 1l );
					assertThat( parent.getChildren().size() ).isEqualTo( 0 );
				}
		);
	}

	@Entity(name = "parent")
	@Table(name = "parent")
	public static class Parent {
		@Id
		private Long id;

		private Long version;

		@OneToMany(mappedBy = "parent", cascade = { MERGE }, orphanRemoval = true, fetch = FetchType.LAZY)
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(Long id, Long version) {
			this.id = id;
			this.version = version;
		}

		public Long getId() {
			return this.id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}

		public void addChild(Child child) {
			children.add( child );
			child.setParent( this );
		}

		public void removeChild(Child child) {
			children.remove( child );
			child.setParent( null );
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}
	}

	@Entity
	@Table(name = "child")
	public static class Child {

		@Id
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Parent parent;

		public Child() {
		}

		public Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return this.id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

	}

}
