/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.various;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = Antenna.class
)
@SessionFactory
public class GeneratedTest {

	@Test
	public void testGenerated(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Antenna antenna = new Antenna();
					antenna.id = 1;
					session.persist( antenna );
					assertNull( antenna.latitude );
					assertNull( antenna.longitude );
				}
		);
	}
}
