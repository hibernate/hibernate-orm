/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

/**
 * Settings for configuring graph parser behavior.
 *
 */
public interface GraphParserSettings {

	/**
	 * Setting to control which graph parser syntax to use.
	 * <p>
	 * Valid values are:
	 * <ul>
	 *     <li>{@code "legacy"} - Uses the legacy syntax: attribute(SubType: attributes)
	 *         This generates deprecation warnings when the old syntax is detected.
	 *     <li>{@code "modern"} - Uses the new syntax: attribute:SubType(attributes)
	 *         This is the preferred syntax going forward.
	 * </ul>
	 * <p>
	 * @settingDefault {@code "legacy"} for backward compatibility
	 * @since 7.0
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	String GRAPH_PARSER_MODE = "hibernate.graph_parser_mode";

}
