/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * A ternary boolean enum for describing the mutability aspects of an
 * attribute as a natural id.
 *
 * @author Steve Ebersole
 */
public enum NaturalIdMutability {
	/**
	 * The attribute is part of a mutable natural id
	 */
	MUTABLE,
	/**
	 * The attribute is part of a immutable natural id
	 */
	IMMUTABLE,
	/**
	 * The attribute is not part of any kind of natural id.
	 */
	NOT_NATURAL_ID
}
