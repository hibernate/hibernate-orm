/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
