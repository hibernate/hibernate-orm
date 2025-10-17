/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.various;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for the @OptimisticLock annotation.
 *
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = Conductor.class
)
@SessionFactory
public class OptimisticLockAnnotationTest {

	@Test
	public void testOptimisticLockExcludeOnNameProperty(SessionFactoryScope scope) {


		scope.inTransaction(
				session -> {
					Conductor c = new Conductor();
					c.setName( "Bob" );
					session.persist( c );
					session.flush();

					session.clear();

					c = session.find( Conductor.class, c.getId() );
					Long version = c.getVersion();
					c.setName( "Don" );
					session.flush();

					session.clear();

					c = session.find( Conductor.class, c.getId() );
					assertEquals( version, c.getVersion() );
				}
		);
	}
}
