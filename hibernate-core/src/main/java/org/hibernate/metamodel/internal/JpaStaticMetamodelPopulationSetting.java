/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.Locale;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Enumerated setting used to control whether Hibernate looks for and populates
 * JPA static metamodel models of application's domain model.
 *
 * @author Andrea Boriero
 */
public enum JpaStaticMetamodelPopulationSetting {
	/**
	 * Indicates that Hibernate will look for the JPA static metamodel description
	 * of the application domain model and populate it.
	 */
	ENABLED,
	/**
	 * Indicates that Hibernate will not look for the JPA static metamodel description
	 * of the application domain model.
	 */
	DISABLED,
	/**
	 * Works as {@link #ENABLED} but ignores any non-JPA features that would otherwise
	 * result in the population failing.
	 */
	SKIP_UNSUPPORTED;

	public static JpaStaticMetamodelPopulationSetting parse(String setting) {
		return switch ( setting.toLowerCase(Locale.ROOT) ) {
			case "enabled" -> ENABLED;
			case "disabled" -> DISABLED;
			default -> SKIP_UNSUPPORTED;
		};
	}

	public static JpaStaticMetamodelPopulationSetting determineJpaStaticMetaModelPopulationSetting(Map configurationValues) {
		return parse( determineSetting( configurationValues ) );
	}

	private static String determineSetting(Map configurationValues) {
		return ConfigurationHelper.getString(
				AvailableSettings.STATIC_METAMODEL_POPULATION,
				configurationValues,
				"skipUnsupported"
		);
	}
}
