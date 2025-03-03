/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

/**
 * A setting for use in other annotations to define settings for various things.
 */
public @interface Setting {
	/**
	 * The setting name.  Often a constant from {@link org.hibernate.cfg.AvailableSettings}
	 */
	String name();

	/**
	 * The setting value
	 */
	String value();
}
