/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration.criteria;

import java.util.stream.Stream;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.spatial.HibernateSpatialConfigurationSettings;
import org.hibernate.spatial.testing.IsSupportedBySpatial;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;

import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Test SpatialCriteria queries using geometric parameter binding
 *
 * @author Marco Belladelli
 */
@RequiresDialectFeature(feature = IsSupportedBySpatial.class)
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "BIND"),
		@Setting(name = HibernateSpatialConfigurationSettings.ORACLE_OGC_STRICT, value = "true")
})
@SessionFactory
public class SpatialCriteriaBindingModeTest extends SpatialCriteriaTest {
	@Override
	public Stream<PredicateRegexes.PredicateRegex> getTestRegexes() {
		return  super.predicateRegexes.bindingModeRegexes();
	}
}
