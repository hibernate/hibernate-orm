/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Enumeration of available graph parser syntax modes.
 *
 */
public enum GraphParserMode {
	/**
	 * Legacy syntax: attribute(SubType: attributes)
	 * This is the legacy syntax.
	 */
	LEGACY( "legacy" ),

	/**
	 * Modern syntax: attribute:SubType(attributes)
	 * This is the preferred new syntax.
	 */
	MODERN( "modern" );

	private final String configValue;

	GraphParserMode(String configValue) {
		this.configValue = configValue;
	}

	public String getConfigValue() {
		return configValue;
	}

	/**
	 * Interpret the configured valueHandlingMode value.
	 * Valid values are either a {@link GraphParserMode} object or its String representation.
	 * For string values, the matching is case insensitive, so you can use either {@code MODERN} or {@code modern}.
	 *
	 * @param graphParserMode configured {@link GraphParserMode} representation
	 *
	 * @return associated {@link GraphParserMode} object
	 */
	public static GraphParserMode interpret(Object graphParserMode) {
		if ( graphParserMode == null ) {
			return LEGACY;
		}
		else if ( graphParserMode instanceof GraphParserMode mode ) {
			return mode;
		}
		else if ( graphParserMode instanceof String string ) {
			for ( GraphParserMode value : values() ) {
				if ( value.name().equalsIgnoreCase( string ) ) {
					return value;
				}
			}
		}
		throw new HibernateException(
				"Unrecognized graph_parser_mode value : " + graphParserMode
						+ ".  Supported values include 'modern' and 'legacy'."
		);
	}
}
