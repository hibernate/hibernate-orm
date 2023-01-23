/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties.processor;

/**
 * Lists the config parameters that can be passed to this annotation processor via {@code -A.....}.
 */
public final class Configuration {
	private Configuration() {
	}

	private static final String HIBERNATE_ORM_CPCAP_PREFIX = "org.hibernate.orm.cpcap.";

	/**
	 * Use to define a base URL for Hibernate ORM Javadoc. As we are getting parts of Javadoc links in it should
	 * be adjusted to point to somewhere where the docs actually live.
	 */
	public static final String JAVADOC_LINK = HIBERNATE_ORM_CPCAP_PREFIX + "javadoc.link";
	/**
	 * Use to define a pattern for classes to be ignored by this collector. We can have some {@code *Settings} classes
	 * in {@code impl} packages. And we don't need to collect properties from those.
	 */
	public static final String IGNORE_PATTERN = HIBERNATE_ORM_CPCAP_PREFIX + "ignore.pattern";
	/**
	 * Use to define a pattern for property key values that should be ignored. By default, we will ignore keys that end
	 * with a dot {@code '.'}.
	 */
	public static final String IGNORE_KEY_VALUE_PATTERN = HIBERNATE_ORM_CPCAP_PREFIX + "ignore.key.value.pattern";
	/**
	 * Used to group properties in sections and as a title of that section.
	 */
	public static final String MODULE_TITLE = HIBERNATE_ORM_CPCAP_PREFIX + "module.title";
	/**
	 * Used to group properties in sections and as a title of that section.
	 */
	public static final String MODULE_LINK_ANCHOR = HIBERNATE_ORM_CPCAP_PREFIX + "module.link.anchor";
}
