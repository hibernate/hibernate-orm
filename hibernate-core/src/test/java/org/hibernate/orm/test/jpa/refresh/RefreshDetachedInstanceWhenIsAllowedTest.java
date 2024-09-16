/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.refresh;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11188")
@Jpa(
		annotatedClasses = {
				TestEntity.class
		},
		integrationSettings = { @Setting(name = AvailableSettings.ALLOW_REFRESH_DETACHED_ENTITY, value = "true") }
)
public class RefreshDetachedInstanceWhenIsAllowedTest {
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
		scope.inEntityManager(
				entityManager -> {
					final Session session = entityManager.unwrap( Session.class );
					session.refresh( testEntity );
				}
		);
	}

	@Test
	public void testRefreshDetachedInstance(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					entityManager.refresh( testEntity );
				}
		);
	}
}
