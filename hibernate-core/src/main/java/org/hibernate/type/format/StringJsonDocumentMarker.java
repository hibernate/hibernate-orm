/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

/**
 * Enum class for JSON object markers.
 */
public enum StringJsonDocumentMarker {
	ARRAY_END(']'),
	ARRAY_START('['),
	OBJECT_END('}'),
	OBJECT_START('{'),
	SEPARATOR(','),
	QUOTE('"'),
	KEY_VALUE_SEPARATOR(':'),
	OTHER();

	private final char val;
	StringJsonDocumentMarker(char val) {
		this.val = val;
	}
	StringJsonDocumentMarker() {
		this.val = 0;
	}
	public char getMarkerCharacter() {
		return this.val;
	}

	public static StringJsonDocumentMarker markerOf(char ch) {
		switch (ch) {
			case ']':
				return ARRAY_END;
			case '[':
				return ARRAY_START;
			case '}':
				return OBJECT_END;
			case '{':
				return OBJECT_START;
			case ',':
				return SEPARATOR;
			case '"':
				return QUOTE;
			case ':':
				return KEY_VALUE_SEPARATOR;
			default:
				return OTHER;
		}
	}
}
