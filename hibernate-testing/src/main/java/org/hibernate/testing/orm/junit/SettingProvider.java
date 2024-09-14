/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
