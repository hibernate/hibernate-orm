/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.xml.metamodel;

import javax.persistence.EntityManager;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-10968")
public class DisabledPopulationTest extends AbstractMetamodelPopulateTest {
	@Override
	protected String getPopulationMode() {
		return "DISABLED";
	}

	@Test
	public void testJpaMetaModelPopulationDisabled() {
		EntityManager entityManager = getOrCreateEntityManager();
		try {
			// When 'DISABLED' is set, no metamodel data is populated.
			// This means that the metamodel data will be null.
			assertNull( entityManager.getMetamodel() );
		}
		finally {
			entityManager.close();
		}
	}
}
