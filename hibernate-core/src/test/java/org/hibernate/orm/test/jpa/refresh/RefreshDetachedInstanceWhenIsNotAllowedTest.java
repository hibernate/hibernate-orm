/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.refresh;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@Jpa(
		annotatedClasses = {
				TestEntity.class
		}
)
public class RefreshDetachedInstanceWhenIsNotAllowedTest {
	private TestEntity testEntity;

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		testEntity = new TestEntity();
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( testEntity );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from TestEntity" ).executeUpdate();
				}
		);
	}

	@Test
	public void testUnwrappedSessionRefreshDetachedInstance(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final Session session = entityManager.unwrap( Session.class );
					Assertions.assertThrows(
							IllegalArgumentException.class,
							() -> session.refresh( testEntity ),
							"Should have thrown an IllegalArgumentException"
					);
				}
		);
	}

	@Test
	public void testRefreshDetachedInstance(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Assertions.assertThrows(
							IllegalArgumentException.class,
							() -> entityManager.refresh( testEntity ),
							"Should have thrown an IllegalArgumentException"
					);
				}
		);
	}
}
