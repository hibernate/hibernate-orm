/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.various;

import org.hibernate.annotations.OptimisticLock;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(annotatedClasses = { OneToOneOptimisticLockTest.Parent.class, OneToOneOptimisticLockTest.Child.class })
@SessionFactory
@JiraKey( "HHH-15440" )
public class OneToOneOptimisticLockTest {

	public final static Integer PARENT_ID = 1;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( PARENT_ID );
					session.persist( parent );
				}
		);
	}

	@Test
	public void testUpdateChildDoesNotIncrementParentVersion(SessionFactoryScope scope) {
		Integer version = scope.fromTransaction(
				session -> {
					Parent parent = session.get( Parent.class, PARENT_ID );
					Integer vers = parent.getVersion();

					Child child = new Child( 2 );
					parent.addChild( child );

					session.persist( child );
					return vers;
				}
		);

		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, PARENT_ID );
					assertThat( parent.getVersion() ).isEqualTo( version );
				}
		);
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT_TABLE")
	public static class Parent {

		@Id
		Integer id;

		public Parent(Integer id) {
			this.id = id;
		}

		public Parent() {
		}

		@OptimisticLock(excluded = true)
		@OneToOne(mappedBy = "parent")
		Child child;

		@Version
		@Column(name = "VERSION_COLUMN")
		Integer version;

		public void addChild(Child child) {
			this.child = child;
			child.parent = this;
		}

		public Integer getVersion() {
			return version;
		}
	}

	@Entity(name = "Child")
	@Table(name = "CHILD_TABLE")
	public static class Child {

		@Id
		Integer id;

		@OneToOne
		Parent parent;

		public Child() {
		}

		public Child(Integer id) {
			this.id = id;
		}
	}
}
