/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.xml.metamodel;

import java.util.Map;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-10968")
public abstract class AbstractMetamodelPopulateTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected String[] getMappings() {
		return new String[] { "org/hibernate/jpa/test/xml/Person.hbm.xml" };
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.JPA_METAMODEL_POPULATION, getPopulationMode() );
	}

	protected abstract String getPopulationMode();
}
