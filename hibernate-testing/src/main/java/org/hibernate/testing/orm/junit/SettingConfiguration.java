/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * Allows setting multiple configuration values at once.
 *
 * @author Steve Ebersole
 */
public @interface SettingConfiguration {
	Class<? extends Configurer> configurer();

	interface Configurer {
		void applySettings(StandardServiceRegistryBuilder registryBuilder);
	}
}
