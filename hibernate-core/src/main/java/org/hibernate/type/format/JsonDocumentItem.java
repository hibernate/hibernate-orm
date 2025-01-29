/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

public class JsonDocumentItem {
	public static final JsonDocumentItem OBJECT_START_ITEM = new JsonDocumentItem(JsonDocumentItemType.OBJECT_START,
			Boolean.TRUE );
	public static final JsonDocumentItem OBJECT_END_ITEM = new JsonDocumentItem(JsonDocumentItemType.OBJECT_END,
			Boolean.TRUE );
	public static final JsonDocumentItem ARRAY_START_ITEM = new JsonDocumentItem(JsonDocumentItemType.ARRAY_START,
			Boolean.TRUE );
	public static final JsonDocumentItem ARRAY_END_ITEM = new JsonDocumentItem(JsonDocumentItemType.ARRAY_END,
			Boolean.TRUE );
	public static final JsonDocumentItem KEY_NAME_ITEM = new JsonDocumentItem(JsonDocumentItemType.VALUE_KEY,
			Boolean.TRUE );

	public static final JsonDocumentItem TRUE_VALUE_ITEM = new JsonDocumentItem(JsonDocumentItemType.BOOLEAN_VALUE,Boolean.TRUE);
	public static final JsonDocumentItem FALSE_VALUE_ITEM = new JsonDocumentItem(JsonDocumentItemType.BOOLEAN_VALUE,Boolean.FALSE);
	public static final JsonDocumentItem NULL_VALUE_ITEM = new JsonDocumentItem(JsonDocumentItemType.NULL_VALUE,null);



	private JsonDocumentItemType type;
	private Object value;


	public Object getValue() {
		return value;
	}

	public JsonDocumentItem(JsonDocumentItemType type, Object value) {
		this(type);
		this.value = value;
	}
	public JsonDocumentItem(JsonDocumentItemType type) {
		this.type = type;
		this.value = null;
	}

	public JsonDocumentItemType getType() {
		return type;
	}

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
}
