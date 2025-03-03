/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

/**
 * Annotation to allow providing settings whose values can be
 * types other than String.
 *
 * @author Steve Ebersole
 */
public @interface SettingProvider {
	/**
	 * The name of the setting whose value is being provided
	 */
	String settingName();

	/**
	 * The value provider
	 */
	Class<? extends Provider<?>> provider();

	/**
	 * Contract for providing a value
	 */
	interface Provider<S> {
		/**
		 * Get the setting value
		 */
		S getSetting();
	}
}
