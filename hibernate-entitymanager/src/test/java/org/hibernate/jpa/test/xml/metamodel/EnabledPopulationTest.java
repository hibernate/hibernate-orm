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

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-10968")
public class EnabledPopulationTest extends AbstractMetamodelPopulateTest {
	@Override
	protected String getPopulationMode() {
		return "ENABLED";
	}

	@Test
	public void testJpaMetaModelPopulationEnabled() {
		EntityManager entityManager = getOrCreateEntityManager();
		try {
			// When 'ENABLED' is set, the metamodel data is populated, including map-mode entities.
			// This means that the mapped entity will be included in the managed types.
			assertEquals( 1, entityManager.getMetamodel().getManagedTypes().size() );
		}
		finally {
			entityManager.close();
		}
	}
}
