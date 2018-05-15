/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.crud.onetoone.bidirectional;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Chris Cranford
 */
public class EntityWithBidirectionalOneToOneTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Parent.class );
		metadataSources.addAnnotatedClass( Child.class );
		metadataSources.addAnnotatedClass( Child2.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@BeforeEach
	public void setUp() {
		sessionFactoryScope().inTransaction( session -> {
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
	public void tearDown() {
		sessionFactoryScope().inTransaction( session -> {
			session.createQuery( "delete from Child" ).executeUpdate();
			session.createQuery( "delete from Child2" ).executeUpdate();
			session.createQuery( "delete from Parent" ).executeUpdate();
		} );
	}

	@Test
	public void testGetParent() {
		sessionFactoryScope().inTransaction( session -> {
			final Parent parent = session.get( Parent.class, 1 );
			Child child = parent.getChild();
			assertThat( child, CoreMatchers.notNullValue() );
			assertTrue(
					"The child eager OneToOne association is not initialized",
					Hibernate.isInitialized( child )
			);
			assertThat( child.getName(), CoreMatchers.notNullValue() );
			assertThat( child.getParent(), CoreMatchers.notNullValue() );
			assertThat( child.getParent(), CoreMatchers.notNullValue() );

			Child2 child2 = parent.getChild2();
			assertThat( child2, CoreMatchers.notNullValue() );
			assertTrue(
					"The child2 eager OneToOne association is not initialized",
					Hibernate.isInitialized( child2 )
			);
			assertThat( child2.getName(), equalTo( "Fab" ) );
			assertThat( child2.getParent(), CoreMatchers.notNullValue() );

		} );
	}

	@Test
	public void testGetParent2() {
		sessionFactoryScope().inTransaction( session ->{
			Parent parent = new Parent( 4, "Hibernate OGM" );
			Child child = new Child( 5, parent );
			child.setName( "Acme2" );

			Child2 child2 = new Child2( 6, parent );
			child2.setName( "Fab2" );

			child2.setParent2( parent );

			session.save( parent );
			session.save( child );
			session.save( child2 );
		} );

		sessionFactoryScope().inTransaction( session -> {
			final Parent parent = session.get( Parent.class, 4 );
			Child child = parent.getChild();
			assertThat( child, CoreMatchers.notNullValue() );
			assertTrue(
					"The child eager OneToOne association is not initialized",
					Hibernate.isInitialized( child )
			);
			assertThat( child.getName(), CoreMatchers.notNullValue() );
			assertThat( child.getParent(), CoreMatchers.notNullValue() );

			Child2 child2 = parent.getChild2();
			assertThat( child2, CoreMatchers.notNullValue() );
			assertTrue(
					"The child2 eager OneToOne association is not initialized",
					Hibernate.isInitialized( child2 )
			);
			assertThat( child2.getName(), equalTo( "Fab2" ) );
			assertThat( child2.getParent(), CoreMatchers.notNullValue() );
			assertThat( child2.getParent().getDescription(), equalTo( "Hibernate OGM" ) );

			Parent parent2 = child2.getParent2();
			assertThat( parent2, CoreMatchers.notNullValue() );
			assertThat( parent2.getDescription(), equalTo( "Hibernate OGM" ) );
			assertThat( parent2.getChild(), CoreMatchers.notNullValue() );

		} );
	}

	@Test
	public void testGetParent3() {
		sessionFactoryScope().inTransaction( session ->{

			Parent parent = new Parent( 4, "Hibernate Search" );
			Child child = new Child( 5, parent );
			child.setName( "Acme2" );
			Child2 child2 = new Child2( 7, parent );
			child2.setName( "Fab2" );

			Parent parent2 = new Parent( 6, "Hibernate OGM" );
			child2.setParent2( parent2 );

			Child child1 = new Child( 8, parent2 );

			session.save( parent );
			session.save( parent2 );
			session.save( child );
			session.save( child1 );
			session.save( child2 );
		} );

		sessionFactoryScope().inTransaction( session -> {
			final Parent parent = session.get( Parent.class, 4 );
			assertThat( parent.getDescription(), equalTo( "Hibernate Search" ) );

			Child child = parent.getChild();
			assertThat( child, CoreMatchers.notNullValue() );
			assertTrue(
					"The child eager OneToOne association is not initialized",
					Hibernate.isInitialized( child )
			);
			assertThat( child.getName(), CoreMatchers.notNullValue() );
			assertThat( child.getParent(), CoreMatchers.notNullValue() );

			Child2 child2 = parent.getChild2();
			assertThat( child2, CoreMatchers.notNullValue() );
			assertTrue(
					"The child2 eager OneToOne association is not initialized",
					Hibernate.isInitialized( child2 )
			);
			assertThat( child2.getName(), equalTo( "Fab2" ) );
			assertThat( child2.getParent(), CoreMatchers.notNullValue() );
			assertThat( child2.getParent().getDescription(), equalTo( "Hibernate Search" ) );

			Parent parent2 = child2.getParent2();
			assertThat( parent2, CoreMatchers.notNullValue() );
			assertThat( parent2.getDescription(), equalTo( "Hibernate OGM" ) );
			assertThat( parent2.getChild(), CoreMatchers.notNullValue() );

		} );
	}

	@Test
	public void testGetChild() {
		sessionFactoryScope().inTransaction( session -> {
			final Child child = session.get( Child.class, 2 );
			Parent parent = child.getParent();
			assertTrue(
					"The parent eager OneToOne association is not initialized",
					Hibernate.isInitialized( parent )
			);
			assertThat( parent, CoreMatchers.notNullValue() );
			assertThat( parent.getDescription(), CoreMatchers.notNullValue() );
			Child child1 = parent.getChild();
			assertThat( child1, CoreMatchers.notNullValue() );
			assertTrue(
					"The child eager OneToOne association is not initialized",
					Hibernate.isInitialized( child1 )
			);
			Child2 child2 = parent.getChild2();
			assertThat( child2, CoreMatchers.notNullValue() );
			assertTrue(
					"The child2 eager OneToOne association is not initialized",
					Hibernate.isInitialized( child2 )
			);
			assertThat( child2.getParent(), CoreMatchers.notNullValue() );
		} );
	}

	@Test
	public void testHqlSelectParent() {
		sessionFactoryScope().inTransaction(
				session -> {
					final Parent parent = session.createQuery(
							"SELECT p FROM Parent p JOIN p.child WHERE p.id = :id",
							Parent.class
					)
							.setParameter( "id", 1 )
							.getSingleResult();

					assertThat( parent.getChild(), CoreMatchers.notNullValue() );
					String name = parent.getChild().getName();
					assertThat( name, CoreMatchers.notNullValue() );
				}
		);
	}

	@Test
	public void testHqlSelectChild() {
		sessionFactoryScope().inTransaction(
				session -> {
					final String queryString = "SELECT c FROM Child c JOIN c.parent d WHERE d.id = :id";
					final Child child = session.createQuery( queryString, Child.class )
							.setParameter( "id", 1 )
							.getSingleResult();

					assertThat( child.getParent(), CoreMatchers.notNullValue() );

					String description = child.getParent().getDescription();
					assertThat( description, CoreMatchers.notNullValue() );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Integer id;
		private String description;
		@OneToOne(mappedBy = "parent")
		private Child child;
		@OneToOne(mappedBy = "parent")
		private Child2 child2;

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

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}

		public Child2 getChild2() {
			return child2;
		}

		public void setChild2(Child2 child2) {
			this.child2 = child2;
		}

	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Integer id;
		private String name;
		@OneToOne
		private Parent parent;

		Child() {

		}

		Child(Integer id, Parent parent) {
			this.id = id;
			this.parent = parent;
			this.parent.setChild( this );
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

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "Child2")
	@Table(name = "CHILD2")
	public static class Child2 {
		@Id
		private Integer id;

		private String name;

		@OneToOne
		private Parent parent;

		@OneToOne
		private Parent parent2;

		Child2() {
		}

		Child2(Integer id, Parent parent) {
			this.id = id;
			this.parent = parent;
			this.parent.setChild2( this );
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

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public Parent getParent2() {
			return parent2;
		}

		public void setParent2(Parent parent) {
			this.parent2 = parent;
		}
	}
}
