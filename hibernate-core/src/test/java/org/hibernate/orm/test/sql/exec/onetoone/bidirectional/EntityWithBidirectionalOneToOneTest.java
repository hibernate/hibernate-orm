/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.exec.onetoone.bidirectional;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Chris Cranford
 */
@DomainModel(
		annotatedClasses = {
				EntityWithBidirectionalOneToOneTest.Parent.class,
				EntityWithBidirectionalOneToOneTest.Child.class,
				EntityWithBidirectionalOneToOneTest.Child2.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
public class EntityWithBidirectionalOneToOneTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent( 1, "Hibernate ORM" );
			Child child = new Child( 2, parent );
			child.setName( "Acme" );
			Child2 child2 = new Child2( 3, parent );
			child2.setName( "Fab" );
			session.save( parent );
			session.save( child );
			session.save( child2 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete from Child2" ).executeUpdate();
			session.createQuery( "delete from Parent" ).executeUpdate();
			session.createQuery( "delete from Child" ).executeUpdate();
		} );
	}

	@Test
	public void testGetParent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent = session.get( Parent.class, 1 );
			Child child = parent.getOwnedBidirectionalChild();
			assertThat( child, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child ),
					"The child eager OneToOne association is not initialized"
			);
			assertThat( child.getName(), notNullValue() );
			assertThat( child.getParentMappedByChild(), notNullValue() );
			assertThat( child.getParentMappedByChild(), notNullValue() );

			Child2 child2 = parent.getChildMappedByParent1();
			assertThat( child2, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child2 ),
					"The child2 eager OneToOne association is not initialized"
			);
			assertThat( child2.getName(), equalTo( "Fab" ) );
			assertThat( child2.getOwnedBidirectionalParent(), notNullValue() );

		} );
	}

	@Test
	public void testGetParent2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent( 4, "Hibernate OGM" );
			Child child = new Child( 5, parent );
			child.setName( "Acme2" );

			Child2 child2 = new Child2( 6, parent );
			child2.setName( "Fab2" );

			child2.setUnidirectionalParent( parent );

			session.save( parent );
			session.save( child );
			session.save( child2 );
		} );

		scope.inTransaction( session -> {
			final Parent parent = session.get( Parent.class, 4 );
			Child child = parent.getOwnedBidirectionalChild();
			assertThat( child, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child ),
					"The child eager OneToOne association is not initialized"
			);
			assertThat( child.getName(), notNullValue() );
			assertThat( child.getParentMappedByChild(), notNullValue() );

			Child2 child2 = parent.getChildMappedByParent1();
			assertThat( child2, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child2 ),
					"The child2 eager OneToOne association is not initialized"
			);
			assertThat( child2.getName(), equalTo( "Fab2" ) );
			assertThat( child2.getOwnedBidirectionalParent(), notNullValue() );
			assertThat( child2.getOwnedBidirectionalParent().getDescription(), equalTo( "Hibernate OGM" ) );

			Parent parent2 = child2.getUnidirectionalParent();
			assertThat( parent2, notNullValue() );
			assertThat( parent2.getDescription(), equalTo( "Hibernate OGM" ) );
			assertThat( parent2.getOwnedBidirectionalChild(), notNullValue() );

		} );
	}

	@Test
	public void testGetParent3(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			Parent parent = new Parent( 4, "Hibernate Search" );
			Child child = new Child( 5, parent );
			child.setName( "Acme2" );
			Child2 child2 = new Child2( 7, parent );
			child2.setName( "Fab2" );

			Parent parent2 = new Parent( 6, "Hibernate OGM" );
			child2.setUnidirectionalParent( parent2 );

			Child child1 = new Child( 8, parent2 );

			session.save( parent );
			session.save( parent2 );
			session.save( child );
			session.save( child1 );
			session.save( child2 );
		} );

		scope.inTransaction( session -> {
			final Parent parent = session.get( Parent.class, 4 );
			assertThat( parent.getDescription(), equalTo( "Hibernate Search" ) );

			Child child = parent.getOwnedBidirectionalChild();
			assertThat( child, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child ),
					"The child eager OneToOne association is not initialized"
			);
			assertThat( child.getName(), notNullValue() );
			assertThat( child.getParentMappedByChild(), notNullValue() );

			Child2 child2 = parent.getChildMappedByParent1();
			assertThat( child2, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child2 ),
					"The child2 eager OneToOne association is not initialized"
			);
			assertThat( child2.getName(), equalTo( "Fab2" ) );
			assertThat( child2.getOwnedBidirectionalParent(), notNullValue() );
			assertThat( child2.getOwnedBidirectionalParent().getDescription(), equalTo( "Hibernate Search" ) );

			Parent parent2 = child2.getUnidirectionalParent();
			assertThat( parent2, notNullValue() );
			assertThat( parent2.getDescription(), equalTo( "Hibernate OGM" ) );
			assertThat( parent2.getOwnedBidirectionalChild(), notNullValue() );

		} );
	}

	@Test
	public void testGetChild(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Child child = session.get( Child.class, 2 );
			Parent parent = child.getParentMappedByChild();
			assertTrue(
					Hibernate.isInitialized( parent ),
					"The parent eager OneToOne association is not initialized"
			);
			assertThat( parent, notNullValue() );
			assertThat( parent.getDescription(), notNullValue() );
			Child child1 = parent.getOwnedBidirectionalChild();
			assertThat( child1, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child1 ),
					"The child eager OneToOne association is not initialized"
			);
			Child2 child2 = parent.getChildMappedByParent1();
			assertThat( child2, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child2 ),
					"The child2 eager OneToOne association is not initialized"
			);
			assertThat( child2.getOwnedBidirectionalParent(), notNullValue() );
			assertThat( child2.getUnidirectionalParent(), nullValue() );
		} );
	}

	@Test
	public void testHqlSelectParent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Parent parent = session.createQuery(
							"SELECT p FROM Parent p JOIN p.ownedBidirectionalChild WHERE p.id = :id",
							Parent.class
					)
							.setParameter( "id", 1 )
							.getSingleResult();

					assertThat( parent.getOwnedBidirectionalChild(), notNullValue() );
					String name = parent.getOwnedBidirectionalChild().getName();
					assertThat( name, notNullValue() );
				}
		);
	}

	@Test
	@FailureExpected
	public void testHqlSelectChild(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String queryString = "SELECT c FROM Child c JOIN c.parentMappedByChild d WHERE d.id = :id";
					final Child child = session.createQuery( queryString, Child.class )
							.setParameter( "id", 1 )
							.getSingleResult();

					assertThat( child.getParentMappedByChild(), notNullValue() );

					String description = child.getParentMappedByChild().getDescription();
					assertThat( description, notNullValue() );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Integer id;
		private String description;

		@OneToOne
		private Child ownedBidirectionalChild;

		@OneToOne(mappedBy = "ownedBidirectionalParent")
		private Child2 childMappedByParent1;

		Parent() {
		}

		public Parent(Integer id, String description) {
			this.id = id;
			this.description = description;
		}

		Parent(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Child getOwnedBidirectionalChild() {
			return ownedBidirectionalChild;
		}

		public void setOwnedBidirectionalChild(Child ownedBidirectionalChild) {
			this.ownedBidirectionalChild = ownedBidirectionalChild;
		}

		public Child2 getChildMappedByParent1() {
			return childMappedByParent1;
		}

		public void setChildMappedByParent1(Child2 childMappedByParent1) {
			this.childMappedByParent1 = childMappedByParent1;
		}

	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Integer id;
		private String name;

		@OneToOne(mappedBy = "ownedBidirectionalChild")
		private Parent parentMappedByChild;

		Child() {

		}

		Child(Integer id, Parent parentMappedByChild) {
			this.id = id;
			this.parentMappedByChild = parentMappedByChild;
			this.parentMappedByChild.setOwnedBidirectionalChild( this );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParentMappedByChild() {
			return parentMappedByChild;
		}

		public void setParentMappedByChild(Parent parentMappedByChild) {
			this.parentMappedByChild = parentMappedByChild;
		}
	}

	@Entity(name = "Child2")
	@Table(name = "CHILD2")
	public static class Child2 {
		@Id
		private Integer id;

		private String name;

		@OneToOne
		private Parent ownedBidirectionalParent;

		@OneToOne
		private Parent unidirectionalParent;

		Child2() {
		}

		Child2(Integer id, Parent ownedBidirectionalParent) {
			this.id = id;
			this.ownedBidirectionalParent = ownedBidirectionalParent;
			this.ownedBidirectionalParent.setChildMappedByParent1( this );
		}


		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getOwnedBidirectionalParent() {
			return ownedBidirectionalParent;
		}

		public void setOwnedBidirectionalParent(Parent ownedBidirectionalParent) {
			this.ownedBidirectionalParent = ownedBidirectionalParent;
		}

		public Parent getUnidirectionalParent() {
			return unidirectionalParent;
		}

		public void setUnidirectionalParent(Parent parent) {
			this.unidirectionalParent = parent;
		}
	}
}
