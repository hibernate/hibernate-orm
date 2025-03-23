/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.dynamicmodel;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.EntityManager;

import static org.junit.Assert.assertNotNull;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@JiraKey(value = "HHH-8769")
public class DynamicModelTest extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "mappings/dynamicmodel/dynamicModel.hbm.xml" };
	}

	/**
	 * Tests that an EntityManager can be created when using a dynamic model mapping.
	 */
	@Test
	public void testDynamicModelMapping() {
		EntityManager entityManager = getOrCreateEntityManager();
		assertNotNull( "Expected an entity manager to be returned", entityManager );
	}

}
