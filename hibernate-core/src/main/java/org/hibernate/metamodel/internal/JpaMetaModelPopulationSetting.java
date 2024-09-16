/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * @author Steve Ebersole
 */
public enum JpaMetaModelPopulationSetting {
	ENABLED,
	DISABLED,
	IGNORE_UNSUPPORTED;

	public static JpaMetaModelPopulationSetting parse(String setting) {
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

	public static JpaMetaModelPopulationSetting determineJpaMetaModelPopulationSetting(Map configurationValues) {
		String setting = ConfigurationHelper.getString(
				AvailableSettings.JPA_METAMODEL_POPULATION,
				configurationValues
		);
		return JpaMetaModelPopulationSetting.parse( setting );
	}
}
