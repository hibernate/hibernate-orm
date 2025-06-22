/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.inheritance;

import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

@ServiceRegistry
public class JoinedSubclassesWithSameFieldNamesButDifferentTypesTest {

	@Test
	@JiraKey(value = "HHH-15369")
	public void testNoExceptionIsThrown(ServiceRegistryScope scope) {
		try (final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) new MetadataSources( scope.getRegistry() )
				.addResource( "org/hibernate/orm/test/hbm/inheritance/AnimalReport.hbm.xml" )
				.buildMetadata()
				.buildSessionFactory()) {
		}
	}
}
