/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12871")
public class JpaMetamodelDisabledPopulationTest extends AbstractJpaMetamodelPopulationTest {
	@Override
	protected String getJpaMetamodelPopulationValue() {
		return "disabled";
	}
}
