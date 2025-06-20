/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@JiraKey(value = "HHH-13000")
@Jpa(annotatedClasses = {
		PessimisticWriteWithOptionalOuterJoinBreaksRefreshTest.Parent.class,
		PessimisticWriteWithOptionalOuterJoinBreaksRefreshTest.Child.class
})
public class PessimisticWriteWithOptionalOuterJoinBreaksRefreshTest {

	private Child child;

	@BeforeEach
	protected void afterEntityManagerFactoryBuilt(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					child = new Child();
					child.parent = new Parent();
					entityManager.persist( child );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void pessimisticWriteWithOptionalOuterJoinBreaksRefreshTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					child = entityManager.find( Child.class, child.id );
					entityManager.lock( child, LockModeType.PESSIMISTIC_WRITE );
					entityManager.flush();
					entityManager.refresh( child );
				}
		);
	}

	@Test
	public void pessimisticReadWithOptionalOuterJoinBreaksRefreshTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					child = entityManager.find( Child.class, child.id );
					entityManager.lock( child, LockModeType.PESSIMISTIC_READ );
					entityManager.flush();
					entityManager.refresh( child );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@ManyToOne(cascade = { CascadeType.PERSIST })
		@JoinTable(name = "test")
		Parent parent;
	}
}
