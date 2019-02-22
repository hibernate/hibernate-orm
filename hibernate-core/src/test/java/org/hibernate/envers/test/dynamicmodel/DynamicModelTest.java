/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.dynamicmodel;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.junit.Assert.assertNotNull;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-8769")
public class DynamicModelTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	@Override
	protected String[] getMappings() {
		return new String[] { "dynamicmodel/dynamicModel.hbm.xml" };
	}

	/**
	 * Tests that an EntityManager can be created when using a dynamic model mapping.
	 */
	@DynamicTest
	public void testDynamicModelMapping() {
		inTransaction(
				entityManager -> {
					assertNotNull( "Expected an entity manager to be returned", entityManager );
				}
		);
	}

}
