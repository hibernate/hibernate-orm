/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.Incubating;

/**
 * A basic plural type. Represents a type, that is mapped to a single column instead of multiple rows.
 * This is used for array or collection types, that are backed by e.g. SQL array or JSON/XML DDL types.
 *
 * @see BasicCollectionType
 * @see BasicArrayType
 */
@Incubating
public interface BasicPluralType<C, E> extends BasicType<C> {
	/**
	 * Get element type
	 */
	BasicType<E> getElementType();

}
