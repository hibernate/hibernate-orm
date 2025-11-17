/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

/**
 * Contract that defines an attribute that can participate in a key.
 *
 * @author Chris Cranford
 */
public interface Keyable {
	/**
	 * Get whether the attribute is already participatig in a key.
	 * @return true if the attribute is in the key; false otherwise
	 */
	boolean isKey();

	/**
	 * Set whether this attribute should or shouldn't participate as a key attribute.
	 * @param key specifies if the attribute is part of the key
	 */
	void setKey(boolean key);
}
