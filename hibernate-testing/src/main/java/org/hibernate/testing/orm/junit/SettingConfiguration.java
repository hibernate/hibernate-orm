/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * Allows setting multiple configuration values at once.
 *
 * @author Steve Ebersole
 */
public @interface SettingConfiguration {
	Class<? extends Configurer> configurer();

	interface Configurer {
		default void applySettings(StandardServiceRegistryBuilder registryBuilder) {
			final Map<String, Object> temp = new HashMap<>();
			applySettings( temp );
			registryBuilder.applySettings( temp );
		}

		default void applySettings(Map<String, Object> configValues) {}
	}
}
