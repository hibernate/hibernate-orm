/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.detached;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;


/**
 * @author Christian Beikov
 */
@JiraKey(value = "HHH-14387")
@DomainModel(
		annotatedClasses = {
				RemoveUninitializedLazyCollectionTest.Parent.class,
				RemoveUninitializedLazyCollectionTest.Child1.class,
				RemoveUninitializedLazyCollectionTest.Child2.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(
		lazyLoading = true,
		inlineDirtyChecking = true,
		biDirectionalAssociationManagement = true
)
public class RemoveUninitializedLazyCollectionTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		Parent parent = new Parent( 1L, "test" );
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( parent );
					entityManager.persist( new Child2( 1L, "child2", parent ) );
				}
		);
	}

	@Test
	public void testDeleteParentWithBidirOrphanDeleteCollectionBasedOnPropertyRef(SessionFactoryScope scope) {
		EntityManager em = scope.getSessionFactory().createEntityManager();
		try {
			// Lazily initialize the child1 collection
			List<Child1> child1 = em.find( Parent.class, 1L ).getChild1();
			Hibernate.initialize( child1 );

			org.hibernate.testing.orm.transaction.TransactionUtil.inTransaction(
					em,
					entityManager -> {
						Parent parent = new Parent();
						parent.setId( 1L );
						parent.setName( "new name" );
						entityManager.merge( parent );
					}
			);

		}
		finally {
			em.close();
		}
	}

	@Entity(name = "Parent")
	public static class Parent {

		private Long id;

		private String name;

		private List<Child1> child1 = new ArrayList<>();

		private List<Child2> child2 = new ArrayList<>();

		public Parent() {
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
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

		@OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "parent")
		public List<Child1> getChild1() {
			return child1;
		}

		public void setChild1(List<Child1> child1) {
			this.child1 = child1;
		}

		@OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "parent")
		public List<Child2> getChild2() {
			return child2;
		}

		public void setChild2(List<Child2> child2) {
			this.child2 = child2;
		}

	}

	@Entity(name = "Child1")
	public static class Child1 {

		@Id
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn
		private Parent parent;

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
		public String toString() {
			return "Child1 [id=" + id + ", name=" + name + "]";
		}

	}

	@Entity(name = "Child2")
	public static class Child2 {
		@Id
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn
		private Parent parent;

		public Child2() {
		}

		public Child2(Long id, String name, Parent parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
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

	}

}
