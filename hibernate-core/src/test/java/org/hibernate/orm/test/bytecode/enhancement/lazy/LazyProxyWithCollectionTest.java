/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Beikov
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-14619" )
@DomainModel(
		annotatedClasses = {
				LazyProxyWithCollectionTest.Parent.class,
				LazyProxyWithCollectionTest.Child.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyProxyWithCollectionTest {

	private Long childId;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			Child c = new Child();
			em.persist( c );
			childId = c.getId();
		} );
	}

	@Test
	public void testReference(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			Child child = em.getReference( Child.class, childId );
			Parent parent = new Parent();
			parent.child = child;
			em.persist( parent );
			// Class cast exception occurs during auto-flush
			em.find( Parent.class, parent.getId() );
		} );
	}

	@Test
	public void testLazyCollection(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			Child child = em.find( Child.class, childId );
			Parent parent = new Parent();
			parent.child = child;
			em.persist( parent );
			child.children = new HashSet<>();
			// Class cast exception occurs during auto-flush
			em.find( Parent.class, parent.getId() );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17750" )
	public void testMerge(SessionFactoryScope scope) {
		final Child child = scope.fromTransaction( em -> em.find( Child.class, childId ) );

		final Parent parent = scope.fromTransaction( em -> {
			Parent p = new Parent();
			p.setChild( child );
			return em.merge( p );
		} );

		scope.inTransaction( em -> em.merge( parent ) );

		scope.inTransaction( em -> {
			assertThat( em.find( Parent.class, parent.getId() ).getChild().getId() ).isEqualTo( child.getId() );
		} );
	}

	// --- //

	@Entity
	@Table( name = "PARENT" )
	static class Parent {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		@OneToOne( fetch = FetchType.LAZY )
		Child child;

		public Long getId() {
			return id;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}
	}

	@Entity
	@Table( name = "CHILD" )
	static class Child {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;
		@Version
		Long version;

		String name;

		@OneToMany
		Set<Child> children = new HashSet<>();

		Child() {
			// No-arg constructor necessary for proxy factory
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
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

}
