/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

/**
 * Json item types
 */
public enum JsonDocumentItemType {
	/**
	 * Start of a Json Object '{'
	 */
	OBJECT_START,
	/**
	 * end  of a Json Object '}'
	 */
	OBJECT_END,
	/**
	 * Start of a Json array '['
	 */
	ARRAY_START,
	/**
	 * End of a Json array ']'
	 */
	ARRAY_END,
	/**
	 * key of Json attribute
	 */
	VALUE_KEY,
	/**
	 * boolean value within Json object or array
	 */
	BOOLEAN_VALUE,
	/**
	 * null value within Json object or array
	 */
	NULL_VALUE,
	/**
	 * numeric value within Json object or array
	 */
	NUMERIC_VALUE,
	/**
	 * String (quoted) value within Json object or array
	 */
	VALUE
}
