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

import static org.junit.Assert.assertTrue;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-10968")
public class IgnoreUnsupportedPopulationTest extends AbstractMetamodelPopulateTest {
	@Override
	protected String getPopulationMode() {
		return "ignoreUnsupported";
	}

	@Test
	public void testJpaMetaModelPopulationSettingUnsupported() {
		EntityManager entityManager = getOrCreateEntityManager();
		try {
			// When 'ignoreUnsupported' is set, all map-mode entities are ignored.
			// This means that the managed types collection is empty.
			assertTrue( entityManager.getMetamodel().getManagedTypes().isEmpty() );
		}
		finally {
			entityManager.close();
		}
	}
}
