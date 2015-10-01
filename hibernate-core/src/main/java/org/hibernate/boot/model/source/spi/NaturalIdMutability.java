/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
