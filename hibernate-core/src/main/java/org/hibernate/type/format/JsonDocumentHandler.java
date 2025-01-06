/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;
/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * JSON document handler.
 * Used to parse JSON documents. Implementors of this will define
 * proper callback implementations.
 *
 * @author Emmanuel Jannetti
 */

public interface JsonDocumentHandler {
	/**
	 * Callback to be called when the start of an JSON object is encountered.
	 */
	void startObject();

	/**
	 * Callback to be called when the end of an JSON object is encountered.
	 */
	void endObject();

	/**
	 * Callback to be called when the start of an array is encountered.
	 */
	void startArray();

	/**
	 * Callback to be called when the end of an array is encountered.
	 */
	void endArray();

	/**
	 * Callback to be called when the key of JSON attribute is encountered.
	 */
	void onObjectKey(String key);

	/**
	 * Callback to be called when null value is encountered.
	 */
	void onNullValue();

	/**
	 * Callback to be called when boolean value is encountered.
	 */
	void onBooleanValue(boolean value);

	/**
	 * Callback to be called when string value is encountered.
	 */
	void onStringValue(String value);


}
