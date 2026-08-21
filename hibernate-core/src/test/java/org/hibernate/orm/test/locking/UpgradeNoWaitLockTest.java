/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.LockMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				UpgradeNoWaitLockTest.Parent.class,
				UpgradeNoWaitLockTest.Child.class
		}
)
@SessionFactory
public class UpgradeNoWaitLockTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	@JiraKey( "HHH-19924" )
	public void testFindIdOfNotPersistedEntity(SessionFactoryScope scope) {
		Child child = new Child( 1L, "And" );
		scope.inTransaction(
				session ->
						session.persist( child )
		);

		scope.inTransaction(
				session -> {
					Child c = session.find( Child.class, 2L, LockMode.UPGRADE_NOWAIT );
					assertThat( c ).isNull();
				}
		);
	}

	@Test
	@JiraKey( "HHH-19924" )
	public void testFindIdOfPersistedEntity(SessionFactoryScope scope) {
		long childId = 1L;
		Child child = new Child( childId, "And" );
		scope.inTransaction(
				session ->
						session.persist( child )
		);

		scope.inTransaction(
				session -> {
					Child c = session.find( Child.class, childId, LockMode.UPGRADE_NOWAIT );
					assertThat( c ).isNotNull();
					assertThat( c.getId() ).isEqualTo( childId );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Long id;

		private String name;

		@OneToMany(fetch = FetchType.EAGER)
		public List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public void add(Child child) {
			children.add( child );
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<Child> getChildren() {
			return children;
		}
	}


	@Entity(name = "Child")
	public static class Child {

		@Id
		private Long id;

		public String name;

		@ManyToOne
		public Parent parent;

		public Child() {
		}

		public Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Parent getParent() {
			return parent;
		}
	}
}
