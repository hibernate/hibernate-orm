/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Enumerated setting used to control whether Hibernate looks for and populates
 * JPA static metamodel models of application's domain model.
 *
 * @author Andrea Boriero
 */
public enum JpaStaticMetaModelPopulationSetting {
	/**
	 * ENABLED indicates that Hibernate will look for the JPA static metamodel description
	 * of the application domain model and populate it.
	 */
	ENABLED,
	/**
	 * DISABLED indicates that Hibernate will not look for the JPA static metamodel description
	 * of the application domain model.
	 */
	DISABLED,
	/**
	 * SKIP_UNSUPPORTED works as ENABLED but ignores any non-JPA features that would otherwise
	 * result in the population failing.
	 */
	SKIP_UNSUPPORTED;

	public static JpaStaticMetaModelPopulationSetting parse(String setting) {
		if ( "enabled".equalsIgnoreCase( setting ) ) {
			return ENABLED;
		}
		else if ( "disabled".equalsIgnoreCase( setting ) ) {
			return DISABLED;
		}
		else {
			return SKIP_UNSUPPORTED;
		}
	}

	public static JpaStaticMetaModelPopulationSetting determineJpaStaticMetaModelPopulationSetting(Map configurationValues) {
		return parse( determineSetting( configurationValues ) );
	}

	private static String determineSetting(Map configurationValues) {
		return ConfigurationHelper.getString(
				AvailableSettings.STATIC_METAMODEL_POPULATION,
				configurationValues
		);
	}
}
