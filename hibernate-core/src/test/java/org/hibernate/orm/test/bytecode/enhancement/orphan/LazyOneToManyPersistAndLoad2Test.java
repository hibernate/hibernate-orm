package org.hibernate.orm.test.bytecode.enhancement.orphan;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestForIssue(jiraKey = "HHH-16334")
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({
		EnhancerTestContext.class, // supports laziness and dirty-checking
		NoDirtyCheckEnhancementContext.class, // supports laziness; does not support dirty-checking,
		DefaultEnhancementContext.class
})
public class LazyOneToManyPersistAndLoad2Test extends BaseCoreFunctionalTestCase {

	public static final String CHILD_NAME = "Luigi";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Parent.class,
				Child.class
		};
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createMutationQuery( "delete from Child" ).executeUpdate();
					session.createMutationQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	public void testEmptyCollectionPersistQueryJoinFetch() {
		inTransaction(
				session -> {
					Parent p = new Parent( 1l );
					session.persist( p );
				}
		);

		inTransaction(
				session -> {
					Parent p = session.getReference( Parent.class, 1l );

					List<Parent> parents = session.createQuery(
							"select p from Parent p left join fetch p.children",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					List<Child> children = p.getChildren();
					assertThat( children ).isEqualTo( parents.get( 0 ).getChildren() );

					assertTrue( Hibernate.isInitialized( children ) );
					assertThat( children.size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testEmptyCollectionPersistQuery() {
		inTransaction(
				session -> {
					Parent p = new Parent( 1l );
					session.persist( p );
				}
		);

		inTransaction(
				session -> {
					Parent p = session.getReference( Parent.class, 1l );

					List<Parent> parents = session.createQuery(
							"select p from Parent p ",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					List<Child> children = p.getChildren();
					assertThat( children ).isEqualTo( parents.get( 0 ).getChildren() );

					assertFalse( Hibernate.isInitialized( children ) );
					assertThat( children.size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testCollectionPersistQueryJoinFetch() {
		inTransaction(
				session -> {
					Parent p = new Parent( 1l );
					Child c = new Child( CHILD_NAME );
					p.addChild( c );
					session.persist( c );
					session.persist( p );
				}
		);

		inTransaction(
				session -> {
					Parent p = session.getReference( Parent.class, 1l );

					List<Parent> parents = session.createQuery(
							"select p from Parent p join fetch p.children",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					List<Child> children = p.getChildren();
					assertThat( children ).isEqualTo( parents.get( 0 ).getChildren() );

					assertTrue( Hibernate.isInitialized( children ) );
					assertThat( children.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testCollectionPersistQuery() {
		inTransaction(
				session -> {
					Parent p = new Parent( 1l );
					Child c = new Child( CHILD_NAME );
					p.addChild( c );
					session.persist( c );
					session.persist( p );
				}
		);

		inTransaction(
				session -> {
					Parent p = session.getReference( Parent.class, 1l );

					List<Parent> parents = session.createQuery(
							"select p from Parent p",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					List<Child> children = p.getChildren();
					assertThat( children ).isEqualTo( parents.get( 0 ).getChildren() );
					assertFalse( Hibernate.isInitialized( children ) );

					assertThat( children.size() ).isEqualTo( 1 );

					Child child = children.get( 0 );
					assertThat( child.getName() ).isEqualTo( CHILD_NAME );
				}
		);
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Child(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Child> children;

		public Parent() {
		}

		public Parent(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void addChild(Child c) {
			if ( children == null ) {
				children = new ArrayList<>();
			}
			children.add( c );
			c.setParent( this );
		}

	}

}
