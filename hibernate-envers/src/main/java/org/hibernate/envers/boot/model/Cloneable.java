/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

/**
 * Contract for an object that is cloneable.
 *
 * @author Chris Cranford
 */
public interface Cloneable<T> {
	/**
	 * Creates a new, deep-copied instance of this object
	 * @return a deep-copy clone of the referenced object
	 */
	T deepCopy();
}
