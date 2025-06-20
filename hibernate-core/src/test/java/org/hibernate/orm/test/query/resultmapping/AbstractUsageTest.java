/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = SimpleEntityWithNamedMappings.class )
@SessionFactory
public abstract class AbstractUsageTest {
	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new SimpleEntityWithNamedMappings( 1, "test", "notes" ) );
				}
		);
	}

	@AfterEach
	public void cleanUpData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
