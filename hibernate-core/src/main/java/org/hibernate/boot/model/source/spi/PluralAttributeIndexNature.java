/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes the nature of plural attribute indexes in terms of relational implications.
 *
 * @author Steve Ebersole
 */
public enum PluralAttributeIndexNature {
	/**
	 * A sequential array/list index
	 */
	SEQUENTIAL,
	/**
	 * The collection indexes are basic, simple values.
	 */
	BASIC,
	/**
	 * The map key is an aggregated composite
	 */
	AGGREGATE,
	/**
	 * The map key is an association identified by a column(s) on the collection table.
	 */
	MANY_TO_MANY,
	/**
	 * The map key is represented by a Hibernate ANY mapping
	 */
	MANY_TO_ANY
}
