/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.mapping;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				DefaultCascadeTest.Parent.class,
				DefaultCascadeTest.Child.class
		},
		xmlMappings = "org/hibernate/orm/test/jpa/mapping/orm.xml"
)
public class DefaultCascadeTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCascadePersist(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Parent parent = new Parent();
					Child child = new Child();
					child.parent = parent;

					entityManager.persist( child );
				}
		);
	}

	@Entity(name = "Parent")
	@Table(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue
		private Integer id;
	}

	@Entity(name = "Child")
	@Table(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		private Parent parent;
	}

}
