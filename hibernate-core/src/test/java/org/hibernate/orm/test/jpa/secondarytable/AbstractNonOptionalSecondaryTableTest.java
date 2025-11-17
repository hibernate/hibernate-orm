/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.secondarytable;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;

@ParameterizedClass
@EnumSource(value = AbstractNonOptionalSecondaryTableTest.JpaComplianceCachingSetting.class)
public abstract class AbstractNonOptionalSecondaryTableTest extends EntityManagerFactoryBasedFunctionalTest {
	public enum JpaComplianceCachingSetting{ DEFAULT, TRUE, FALSE }

	@Parameter
	private JpaComplianceCachingSetting jpaComplianceCachingSetting;

	@Override
	protected void addConfigOptions(Map options) {
		switch ( jpaComplianceCachingSetting ) {
			case DEFAULT:
				// Keep the default (false)
				break;
			case TRUE:
				options.put(
						AvailableSettings.JPA_CACHING_COMPLIANCE,
						Boolean.TRUE
				);
				break;
			case FALSE:
				options.put(
						AvailableSettings.JPA_CACHING_COMPLIANCE,
						Boolean.FALSE
				);
				break;
		}
	}
}
