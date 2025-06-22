/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel;

import org.hibernate.Incubating;

import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;

/**
 * An extended set of {@link PersistentAttributeType} adding {@link #ANY}
 *
 * @since 6.0
 *
 * @see PersistentAttributeType
 */
@Incubating
public enum AttributeClassification {
	/**
	 * @see jakarta.persistence.Basic
	 */
	BASIC,

	/**
	 * @see jakarta.persistence.Embedded
	 */

	EMBEDDED,

	/**
	 * @see org.hibernate.annotations.Any
	 */
	ANY,

	/**
	 * @see jakarta.persistence.OneToOne
	 */
	ONE_TO_ONE,

	/**
	 * @see jakarta.persistence.ManyToOne
	 */
	MANY_TO_ONE,

	/**
	 * @see jakarta.persistence.ElementCollection
	 */
	ELEMENT_COLLECTION,

	/**
	 * @see jakarta.persistence.OneToMany
	 */
	ONE_TO_MANY,

	/**
	 * @see jakarta.persistence.ManyToMany
	 */
	MANY_TO_MANY;

	/**
	 * The associated {@link PersistentAttributeType}, if one
	 */
	public PersistentAttributeType getJpaClassification() {
		return switch ( this ) {
			case BASIC -> PersistentAttributeType.BASIC;
			case EMBEDDED -> PersistentAttributeType.EMBEDDED;
			case ONE_TO_ONE -> PersistentAttributeType.ONE_TO_ONE;
			case MANY_TO_ONE -> PersistentAttributeType.MANY_TO_ONE;
			case ELEMENT_COLLECTION -> PersistentAttributeType.ELEMENT_COLLECTION;
			case ONE_TO_MANY -> PersistentAttributeType.ONE_TO_MANY;
			case MANY_TO_MANY -> PersistentAttributeType.MANY_TO_MANY;
			case ANY -> null;
		};
	}
}
