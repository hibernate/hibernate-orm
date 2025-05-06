/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.sequences;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.annotations.id.sequences.entities.Location;
import org.hibernate.orm.test.annotations.id.sequences.entities.Tower;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
@DomainModel(annotatedClasses = Tower.class)
@SessionFactory
public class IdClassTest {

	@Test
	public void testIdClassInSuperclass(SessionFactoryScope scope) {
		Tower tower = new Tower();
		tower.latitude = 10.3;
		tower.longitude = 45.4;

		scope.inTransaction(
				session -> {
					session.persist( tower );
					session.flush();
					session.clear();
					Location loc = new Location();
					loc.latitude = tower.latitude;
					loc.longitude = tower.longitude;
					assertNotNull( session.get( Tower.class, loc ) );
				}
		);
	}
}
