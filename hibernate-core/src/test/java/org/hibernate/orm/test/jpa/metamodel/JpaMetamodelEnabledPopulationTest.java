/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12871")
@Jpa(
		annotatedClasses = {AbstractJpaMetamodelPopulationTest.SimpleAnnotatedEntity.class, AbstractJpaMetamodelPopulationTest.CompositeIdAnnotatedEntity.class},
		xmlMappings = {
				"org/hibernate/jpa/test/metamodel/SimpleEntity.xml",
				"org/hibernate/jpa/test/metamodel/CompositeIdEntity.hbm.xml",
				"org/hibernate/jpa/test/metamodel/CompositeId2Entity.hbm.xml"
		},
		integrationSettings = {@Setting(name = AvailableSettings.JPA_METAMODEL_POPULATION, value = "enabled")}
)
public class JpaMetamodelEnabledPopulationTest extends AbstractJpaMetamodelPopulationTest {
	@Override
	protected String getJpaMetamodelPopulationValue() {
		return "enabled";
	}
}
