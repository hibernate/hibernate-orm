/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Enumerated setting used to control whether Hibernate looks for and populates
 * JPA static metamodel models of application's domain model.
 *
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public enum JpaStaticMetaModelPopulationSetting {
	/**
	 * Indicates that Hibernate will look for the JPA static metamodel description
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
	IGNORE_UNSUPPORTED;

	public static JpaStaticMetaModelPopulationSetting parse(String setting) {
		if ( "enabled".equalsIgnoreCase( setting ) ) {
			return ENABLED;
		}
		else if ( "disabled".equalsIgnoreCase( setting ) ) {
			return DISABLED;
		}
		else {
			return IGNORE_UNSUPPORTED;
		}
	}

	public static JpaStaticMetaModelPopulationSetting determineJpaMetaModelPopulationSetting(Map configurationValues) {
		return JpaStaticMetaModelPopulationSetting.parse( determineSetting( configurationValues ) );
	}

	private static String determineSetting(Map configurationValues) {
		final String setting = ConfigurationHelper.getString(
				AvailableSettings.STATIC_METAMODEL_POPULATION,
				configurationValues,
				null
		);
		if ( setting != null ) {
			return setting;
		}

		final String legacySetting1 = ConfigurationHelper.getString(
				AvailableSettings.JPA_METAMODEL_POPULATION,
				configurationValues,
				null
		);
		if ( legacySetting1 != null ) {
			DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
					AvailableSettings.JPA_METAMODEL_POPULATION,
					AvailableSettings.STATIC_METAMODEL_POPULATION
			);
			return legacySetting1;
		}

		final String legacySetting2 = ConfigurationHelper.getString(
				AvailableSettings.JPA_METAMODEL_GENERATION,
				configurationValues,
				null
		);
		if ( legacySetting2 != null ) {
			DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
					AvailableSettings.JPA_METAMODEL_GENERATION,
					AvailableSettings.STATIC_METAMODEL_POPULATION
			);
			return legacySetting1;
		}

		return null;
	}
}
