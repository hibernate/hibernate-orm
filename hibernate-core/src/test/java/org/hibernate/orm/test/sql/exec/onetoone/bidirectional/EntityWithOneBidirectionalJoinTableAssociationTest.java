/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.onetoone.bidirectional;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EntityWithOneBidirectionalJoinTableAssociationTest.Parent.class,
				EntityWithOneBidirectionalJoinTableAssociationTest.Child.class,
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
public class EntityWithOneBidirectionalJoinTableAssociationTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( 1, "Hibernate" );
					Child child = new Child( 2, parent );
					child.setName( "Acme" );
					session.persist( parent );
					session.persist( child );
				} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGetParent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Parent parent = session.get( Parent.class, 1 );
					Child child = parent.getChild();
					assertThat( child, CoreMatchers.notNullValue() );
					assertTrue(
							Hibernate.isInitialized( child ),
							"The child eager OneToOne association is not initialized"
					);
					assertThat( child.getName(), equalTo( "Acme" ) );
					assertThat( child.getParent(), CoreMatchers.notNullValue() );
				} );
	}

	@Test
	public void testGetChild(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Child child = session.get( Child.class, 2 );
			Parent parent = child.getParent();
			assertThat( parent, CoreMatchers.notNullValue() );
			assertTrue(
					Hibernate.isInitialized( parent ),
					"The parent eager OneToOne association is not initialized"
			);
			assertThat( parent.getDescription(), CoreMatchers.notNullValue() );

			Child child1 = parent.getChild();
			assertThat( child1, CoreMatchers.notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child1 ),
					"The child eager OneToOne association is not initialized"
			);

		} );
	}

	@Test
	public void testHqlSelectChild(SessionFactoryScope scope) {
		scope.inTransaction(
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

	@Test
	public void testHqlSelectParent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Parent parent = session.createQuery(
							"SELECT p FROM Parent p JOIN p.child WHERE p.id = :id",
							Parent.class
					)
							.setParameter( "id", 1 )
							.getSingleResult();

					Child child = parent.getChild();
					assertThat( child, CoreMatchers.notNullValue() );
					assertTrue(
							Hibernate.isInitialized( child ),
							"the child have to be initialized"
					);
					String name = child.getName();
					assertThat( name, CoreMatchers.notNullValue() );
				}

		);

		scope.inTransaction(
				session -> {
					final Parent parent = session.createQuery(
							"SELECT p FROM Parent p JOIN p.child WHERE p.id = :id",
							Parent.class
					)
							.setParameter( "id", 1 )
							.getSingleResult();

					Child child = parent.getChild();
					assertThat( child, CoreMatchers.notNullValue() );
					assertTrue(
							Hibernate.isInitialized( child ),
							"The child have to be initialized"
					);
					String name = child.getName();
					assertThat( name, CoreMatchers.notNullValue() );
				}

		);
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT")
	public static class Parent {
		private Integer id;

		private String description;
		private Child child;

		Parent() {
		}

		public Parent(Integer id, String description) {
			this.id = id;
			this.description = description;
		}

		@Id
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

		@OneToOne
		@JoinTable(name = "PARENT_CHILD", inverseJoinColumns = @JoinColumn(name = "child_id"), joinColumns = @JoinColumn(name = "parent_id"))
		public Child getChild() {
			return child;
		}

		public void setChild(Child other) {
			this.child = other;
		}

	}

	@Entity(name = "Child")
	@Table(name = "CHILD")
	public static class Child {
		private Integer id;

		private String name;
		private Parent parent;

		Child() {
		}

		Child(Integer id, Parent parent) {
			this.id = id;
			this.parent = parent;
			this.parent.setChild( this );
		}

		@Id
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

		@OneToOne(mappedBy = "child")
		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

}
