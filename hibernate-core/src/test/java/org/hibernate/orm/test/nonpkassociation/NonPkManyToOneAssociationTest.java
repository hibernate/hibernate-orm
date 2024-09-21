/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.nonpkassociation;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author pholvs
 */
@DomainModel(
		annotatedClasses = {
				NonPkManyToOneAssociationTest.Parent.class,
				NonPkManyToOneAssociationTest.Child.class,
		}
)
@SessionFactory
public class NonPkManyToOneAssociationTest {
	private Parent parent;

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					parent = new Parent( 99999L );
					s.persist( parent );

					Child c = new Child( parent );
					parent.getChildren().add( c );
					c.setParent( parent );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Child" ).executeUpdate();
					session.createQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}


	@Test
	public void testHqlWithFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Parent parent = s.find( Parent.class, this.parent.getId() );
					assertEquals( 1, parent.getChildren().size() );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent implements Serializable {

		@Id
		@GeneratedValue
		private Long id;

		private Long collectionKey;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		private Set<Child> children = new HashSet<>();

		public Parent(Long collectionKey) {
			setCollectionKey( collectionKey );
		}

		Parent() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getCollectionKey() {
			return collectionKey;
		}

		public void setCollectionKey(Long collectionKey) {
			this.collectionKey = collectionKey;
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
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn(name = "parentVal", referencedColumnName = "collectionKey")
		private Parent parent;

		public Child(Parent parent) {
			setParent( parent );
		}

		Child() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
