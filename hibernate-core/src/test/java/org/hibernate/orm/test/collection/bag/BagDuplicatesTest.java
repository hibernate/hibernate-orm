package org.hibernate.orm.test.collection.bag;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentBag;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using its built-in unit test framework.
 * Although ORMStandaloneTestCase is perfectly acceptable as a reproducer, usage of this class is much preferred.
 * Since we nearly always include a regression test with bug fixes, providing your reproducer using this method
 * simplifies the process.
 * <p>
 * What's even better?  Fork hibernate-orm itself, add your test case directly to a module's unit tests, then
 * submit it as a PR!
 */
@DomainModel(
		annotatedClasses = {
				BagDuplicatesTest.Parent.class,
				BagDuplicatesTest.Child.class
		}
)
@SessionFactory
public class BagDuplicatesTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	@JiraKey("HHH-4171")
	void testPersistChildAddedToUninitializedBag(boolean flushBeforeInitialization, SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction( session -> {
			var parent = new Parent();
			session.persist( parent );
			return parent.getId();
		} );

		scope.inTransaction( session -> {
			var parent = session.find( Parent.class, parentId );
			assertFalse( Hibernate.isInitialized( parent.getChildren() ) );
			var child = new Child();
			child.setName( "new child" );
			parent.addChild( child );
			assertFalse( Hibernate.isInitialized( parent.getChildren() ) );
			assertTrue( assertInstanceOf( PersistentBag.class, parent.getChildren() ).hasQueuedOperations() );

			session.persist( child );
			if ( flushBeforeInitialization ) {
				session.flush();
			}
			assertFalse( Hibernate.isInitialized( parent.getChildren() ) );

			Hibernate.initialize( parent.getChildren() );
			assertEquals( 1, parent.getChildren().size() );
			assertSame( child, parent.getChildren().get( 0 ) );
		} );

		scope.inTransaction( session -> {
			var parent = session.find( Parent.class, parentId );
			assertEquals( 1, parent.getChildren().size() );
			assertEquals( "new child", parent.getChildren().get( 0 ).getName() );
		} );
	}

	@Test
	public void HHH10385Test(SessionFactoryScope scope) {

		Long parentId = scope.fromTransaction(
				session -> {
					Parent parent = new Parent();
					session.persist( parent );
					session.flush();
					return parent.getId();
				}
		);


		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, parentId );
					Child child1 = new Child();
					child1.setName( "child1" );
					child1.setParent( parent );
					parent.addChild( child1 );
					parent = (Parent) session.merge( parent );
					session.flush();
				}
		);


		scope.inTransaction(
				session -> {

					Parent parent = session.get( Parent.class, parentId );
					assertEquals( 1, parent.getChildren().size() );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", orphanRemoval = true)
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void addChild(Child child) {
			children.add( child );
			child.setParent( this );
		}

		public void removeChild(Child child) {
			children.remove( child );
			child.setParent( null );
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private String name;

		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		@Override
		public String toString() {
			return "Child{" +
					"id=" + id +
					", name='" + name + '\'' +
					'}';
		}
	}

}
