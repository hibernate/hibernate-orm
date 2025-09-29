/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.Locale;
import java.util.Map;

import static org.hibernate.cfg.MappingSettings.JPA_METAMODEL_POPULATION;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;

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

	public static JpaMetamodelPopulationSetting determineJpaMetaModelPopulationSetting(Map<String, Object> settings) {
		return parse( getString( JPA_METAMODEL_POPULATION, settings, "ignoreUnsupported" ) );
	}
}
