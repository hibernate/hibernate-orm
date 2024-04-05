/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.Hibernate;
import org.hibernate.jpa.SpecHints;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
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

@JiraKey("HHH-15607")
@DomainModel(
		annotatedClasses = {
				IdClassEntityGraphTest.Parent.class,
				IdClassEntityGraphTest.Child.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
public class IdClassEntityGraphTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( 1l, "abc" );
					Child child1 = new Child( parent, LocalDateTime.of( 2002, Month.APRIL, 12, 12, 12 ) );
					Child child2 = new Child( parent, LocalDateTime.of( 2003, Month.APRIL, 12, 12, 12 )  );
					session.persist( child1 );
					session.persist( child2 );
					session.persist( parent );
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
	public void testFetchBasicAttributeAndOneToMany(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = session.createQuery( "SELECT p FROM Parent p WHERE p.id = :id", Parent.class )
							.setParameter( "id", 1L )
							.setHint(
									SpecHints.HINT_SPEC_FETCH_GRAPH,
									session.getEntityGraph( "Parent.descritpionAndChildren" )
							)
							.getSingleResult();

					assertTrue( Hibernate.isPropertyInitialized( parent, "description" ) );
					assertTrue( Hibernate.isInitialized( parent.getChildren() ) );
				}
		);
	}

	@Test
	public void testFetchBasicAttributeOnly(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = session.createQuery( "SELECT p FROM Parent p WHERE p.id = :id", Parent.class )
							.setParameter( "id", 1L )
							.setHint(
									SpecHints.HINT_SPEC_FETCH_GRAPH,
									session.getEntityGraph( "Parent.descriptionOnly" )
							)
							.getSingleResult();

					assertTrue( Hibernate.isPropertyInitialized( parent, "description" ) );
					assertFalse( Hibernate.isInitialized( parent.getChildren() ) );
				}
		);
	}

	@Test
	public void testFetchOneToMany(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = session.createQuery( "SELECT p FROM Parent p WHERE p.id = :id", Parent.class )
							.setParameter( "id", 1L )
							.setHint(
									SpecHints.HINT_SPEC_FETCH_GRAPH,
									session.getEntityGraph( "Parent.childrenOnly" )
							)
							.getSingleResult();

					assertFalse( Hibernate.isPropertyInitialized( parent, "description" ) );
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
		@ManyToOne(fetch = FetchType.LAZY)
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
