/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.Locale;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * @author Steve Ebersole
 */
public enum JpaMetamodelPopulationSetting {
	ENABLED,
	DISABLED,
	IGNORE_UNSUPPORTED;

	public static JpaMetamodelPopulationSetting parse(String setting) {
		return switch ( setting.toLowerCase(Locale.ROOT) ) {
			case "enabled" -> ENABLED;
			case "disabled" -> DISABLED;
			default -> IGNORE_UNSUPPORTED;
		};
	}

	public static JpaMetamodelPopulationSetting determineJpaMetaModelPopulationSetting(Map configurationValues) {
		String setting = ConfigurationHelper.getString(
				AvailableSettings.JPA_METAMODEL_POPULATION,
				configurationValues,
				"ignoreUnsupported"
		);
		return JpaMetamodelPopulationSetting.parse( setting );
	}
}
