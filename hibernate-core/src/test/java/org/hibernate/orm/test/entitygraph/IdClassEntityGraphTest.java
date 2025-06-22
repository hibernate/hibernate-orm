/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import org.hibernate.Hibernate;
import org.hibernate.jpa.SpecHints;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = {
				IdClassEntityGraphTest.Parent.class,
				IdClassEntityGraphTest.Child.class
		}
)
@JiraKey( value = "HHH-15607")
public class IdClassEntityGraphTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Parent parent = new Parent( 1l, "abc" );
					Child child1 = new Child( parent, LocalDateTime.of( 2002, Month.APRIL, 12, 12, 12 ) );
					Child child2 = new Child( parent, LocalDateTime.of( 2003, Month.APRIL, 12, 12, 12 )  );
					entityManager.persist( child1 );
					entityManager.persist( child2 );
					entityManager.persist( parent );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testFetchBasicAttributeAndOneToMany(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Parent parent = entityManager.createQuery( "SELECT p FROM Parent p WHERE p.id = :id", Parent.class )
							.setParameter( "id", 1L )
							.setHint(
									SpecHints.HINT_SPEC_FETCH_GRAPH,
									entityManager.getEntityGraph( "Parent.descritpionAndChildren" )
							)
							.getSingleResult();

					assertTrue( Hibernate.isPropertyInitialized( parent, "description" ) );
					assertTrue( Hibernate.isInitialized( parent.getChildren() ) );
				}
		);
	}

	@Test
	public void testFetchBasicAttributeOnly(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Parent parent = entityManager.createQuery( "SELECT p FROM Parent p WHERE p.id = :id", Parent.class )
							.setParameter( "id", 1L )
							.setHint(
									SpecHints.HINT_SPEC_FETCH_GRAPH,
									entityManager.getEntityGraph( "Parent.descriptionOnly" )
							)
							.getSingleResult();

					assertTrue( Hibernate.isPropertyInitialized( parent, "description" ) );
					assertFalse( Hibernate.isInitialized( parent.getChildren() ) );
				}
		);
	}

	@Test
	public void testFetchOneToMany(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Parent parent = entityManager.createQuery( "SELECT p FROM Parent p WHERE p.id = :id", Parent.class )
							.setParameter( "id", 1L )
							.setHint(
									SpecHints.HINT_SPEC_FETCH_GRAPH,
									entityManager.getEntityGraph( "Parent.childrenOnly" )
							)
							.getSingleResult();

					assertTrue( Hibernate.isPropertyInitialized( parent, "description" ) );
					assertTrue( Hibernate.isInitialized( parent.getChildren() ) );
				}
		);
	}

	@Entity(name = "Parent")
	@Table(name = "Parent")
	@NamedEntityGraph(
			name = "Parent.descriptionOnly",
			attributeNodes = {
					@NamedAttributeNode("description"),
			}
	)
	@NamedEntityGraph(
			name = "Parent.childrenOnly",
			attributeNodes = {
					@NamedAttributeNode("children"),
			}
	)
	@NamedEntityGraph(
			name = "Parent.descritpionAndChildren",
			attributeNodes = {
					@NamedAttributeNode("description"),
					@NamedAttributeNode("children"),
			}
	)
	public static class Parent {

		@Id
		private Long id;

		@Basic(fetch = FetchType.LAZY)
		private String description;

		@OrderBy("createdAt DESC")
		@OneToMany(mappedBy = "parent")
		private List<Child> children;

		public Parent() {
		}

		public Parent(Long id, String description) {
			this.id = id;
			this.description = description;
		}

		public Long getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}

		void addChild(Child child) {
			if ( children == null ) {
				children = new ArrayList<>();
			}
			children.add( child );
		}
	}

	@Entity(name = "Child")
	@Table(name = "Child")
	@IdClass(Child.PK.class)
	public static class Child {

		@Id
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@JoinColumn(name = "parent_id")
		private Parent parent;

		@Id
		@Column(name = "createdAt")
		private LocalDateTime createdAt;

		public Child() {
		}

		public Child(Parent parent, LocalDateTime createdAt) {
			this.createdAt = createdAt;
			this.parent = parent;
			parent.addChild( this );
		}

		public Parent getParent() {
			return parent;
		}

		public LocalDateTime getCreatedAt() {
			return createdAt;
		}

		public static class PK implements Serializable {
			private Long parent;
			private LocalDateTime createdAt;

			public Long getParent() {
				return parent;
			}

			public void setParent(Long parent) {
				this.parent = parent;
			}

			public LocalDateTime getCreatedAt() {
				return createdAt;
			}

			public void setCreatedAt(LocalDateTime createdAt) {
				this.createdAt = createdAt;
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}
				PK pk = (PK) o;
				return Objects.equals( parent, pk.parent ) && Objects.equals( createdAt, pk.createdAt );
			}

			@Override
			public int hashCode() {
				return Objects.hash( parent, createdAt );
			}
		}
	}
}
