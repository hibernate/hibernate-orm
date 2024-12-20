/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

/**
 * Defines a contract for objects that are bindable.
 *
 * @author Chris Cranford
 */
public interface Bindable<T> {
	/**
	 * Builds the specified binded class type.
	 *
	 * @return instance of the bindable class type, never {@code null}
	 */
	T build();
}
