/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12871")
public class JpaMetamodelEnabledPopulationTest extends AbstractJpaMetamodelPopulationTest {
	@Override
	protected String getJpaMetamodelPopulationValue() {
		return "enabled";
	}
}
