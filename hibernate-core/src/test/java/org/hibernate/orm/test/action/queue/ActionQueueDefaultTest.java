/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.action.queue.spi.QueueType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies the default action queue selection.
///
/// @author Steve Ebersole
@Jpa(annotatedClasses = ActionQueueDefaultTest.EntityWithId.class)
public class ActionQueueDefaultTest {
	@Test
	public void graphQueueIsTheDefault(EntityManagerFactoryScope scope) {
		final var sessionFactory = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );

		assertEquals( QueueType.GRAPH, sessionFactory.getActionQueueFactory().getConfiguredQueueType() );
	}

	@Entity(name = "EntityWithId")
	public static class EntityWithId {
		@Id
		private Integer id;
	}
}
