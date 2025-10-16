/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.dynamicmodel;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@JiraKey(value = "HHH-8769")
@EnversTest
@Jpa(xmlMappings = "mappings/dynamicmodel/dynamicModel.hbm.xml")
public class DynamicModelTest {

	/**
	 * Tests that an EntityManager can be created when using a dynamic model mapping.
	 */
	@Test
	public void testDynamicModelMapping(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertNotNull( em, "Expected an entity manager to be returned" );
		} );
	}
}
