/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;

import org.hibernate.testing.TestForIssue;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12871")
public class JpaMetamodelEnabledPopulationTest extends AbstractJpaMetamodelPopulationTest {
	@Override
	protected String getJpaMetamodelPopulationValue() {
		return "enabled";
	}
}
