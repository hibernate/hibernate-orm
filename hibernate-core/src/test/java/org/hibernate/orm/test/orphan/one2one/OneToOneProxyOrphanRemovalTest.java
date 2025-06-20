/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A test that shows orphan-removal is triggered when an entity has a lazy one-to-one
 * mapping with property-based annotations and the getter method unwraps the proxy
 * inline during invocation leading to constraint violation due to attempted removal
 * of the associated entity.
 * <p>
 * This test case documents old behavior so that it can be preserved but allowing
 * us to also maintain the fix for {@code HHH-9663}.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11965")
@Jpa(
		annotatedClasses = { OneToOneProxyOrphanRemovalTest.Child.class, OneToOneProxyOrphanRemovalTest.Parent.class }
)
public class OneToOneProxyOrphanRemovalTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope){
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUnproxyOneToOneWithCascade(EntityManagerFactoryScope scope) {
		Integer pId = scope.fromTransaction(
				entityManager -> {
					Parent p = new Parent();
					p.setChild( new Child() );
					entityManager.persist( p );
					return p.getId();
				} );

		// This lambda fails because during flush the cascade of operations determine that the entity state
		// maintains the unwrapped proxy (from the getter) does not match the value maintained in the persistence
		// context (which is the proxy).
		//
		// This results in a comparison that deems the values different and allows the orphan-removal to proceed,
		// leading to a constraint violation because the 'Child' entity continues to be referentially linked to
		// the 'Parent' entity.
		//
		// In short, no cascade of orphan-removal should be invoked for this scenario, thus avoiding the raised
		// constraint violation exception.
		scope.inTransaction(
				entityManager ->
						assertNotNull( entityManager.find( Parent.class, pId ) )
		);
	}

	@Entity(name = "Child")
	public static class Child {
		private Integer id;

		private String name;

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Parent")
	public static class Parent {
		private Integer id;

		private String name;

		private Child child;

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
		public Child getChild() {
			return (Child) Hibernate.unproxy( child );
		}

		public void setChild(Child child) {
			this.child = child;
		}
	}

}
