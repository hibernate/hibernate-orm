/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.xml;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@Jpa(
		xmlMappings = "org/hibernate/orm/test/jpa/callbacks/xml/MyEntity.orm.xml"
)
public class EntityListenerViaXmlTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-9771")
	public void testUsage(EntityManagerFactoryScope scope) {
		JournalingListener.reset();

		scope.inTransaction(
				entityManager -> entityManager.persist( new MyEntity( 1, "steve" ) )
		);

		assertEquals( 1, JournalingListener.getPrePersistCount() );
	}
}
