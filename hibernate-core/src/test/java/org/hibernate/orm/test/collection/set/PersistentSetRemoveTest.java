/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {
		PersistentSetRemoveTest.Parent.class,
		PersistentSetRemoveTest.Child.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19805")
public class PersistentSetRemoveTest {

	@Test
	public void testRemove(SessionFactoryScope scope) {
		Parent p1 = new Parent( 1L, "p1" );
		Child c1 = new Child( 1L, "c1" );
		Child c2 = new Child( 2L, "c2" );
		Child c3 = new Child( 3L, "c3" );
		p1.getChildren().add( c1 );
		c1.setParent( p1 );
		p1.getChildren().add( c2 );
		c2.setParent( p1 );
		p1.getChildren().add( c3 );
		c3.setParent( p1 );

		scope.inTransaction(
				session -> session.persist( p1 )
		);

		scope.inTransaction(
				session -> {
					Child child = session.createQuery( "from Child c where c.name = :name", Child.class )
							.setParameter( "name", "c1" )
							.getSingleResult();

					boolean removed = child.getParent().getChildren().remove( child );
					assertTrue( removed );
				}
		);

		scope.inTransaction(
				session -> {
					Parent parent = session.find( Parent.class, p1.getId() );
					Child childToRemove = parent.getChildren().stream()
							.filter( book -> "c2".equals( book.getName() ) )
							.findFirst()
							.orElseThrow();
					boolean removed = parent.getChildren().remove( childToRemove );
					assertTrue( removed );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Long id;
		private String name;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		private Set<Child> children;

		public Parent() {
			this.children = new HashSet<>();
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
			this.children = new HashSet<>();
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Child> getChildren() {
			return children;
		}

		public void setChildren(Set<Child> children) {
			this.children = children;
		}
	}


	@Entity(name = "Child")
	public static class Child {
		@Id
		private Long id;
		private String name;

		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
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

		@Override
		public final boolean equals(Object o) {
			if ( !(o instanceof Child child) ) {
				return false;
			}

			return Objects.equals( name, child.name );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( name );
		}
	}
}
