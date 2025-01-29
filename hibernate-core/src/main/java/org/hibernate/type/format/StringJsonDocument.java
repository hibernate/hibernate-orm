/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.internal.util.collections.StandardStack;

/**
 * base class for JSON document String reader
 * @author Emmanuel Jannetti
 */
public abstract class StringJsonDocument {
	/**
	 * Processing states. This can be (nested)Object or Arrays.
	 * When processing objects, values are stored as [,]"key":"value"[,]. we add separator when adding new key
	 * When processing arrays, values are stored as [,]"value"[,]. we add separator when adding new value
	 */
	enum PROCESSING_STATE {
		NONE,
		STARTING_OBJECT, // object started but no value added
		OBJECT_KEY_NAME, // We are processing an object key name
		OBJECT, // object started, and we've started adding key/value pairs
		ENDING_OBJECT, // we are ending an object
		STARTING_ARRAY,  // array started but no value added
		ENDING_ARRAY,  // we are ending an array
		ARRAY // we are piling array values
	}
	// Stack of current processing states
	protected StandardStack<PROCESSING_STATE> processingStates = new StandardStack<>();



}
