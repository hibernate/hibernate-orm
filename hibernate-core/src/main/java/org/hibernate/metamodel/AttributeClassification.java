/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
 */
@Incubating
public enum AttributeClassification {
	/**
	 * @see jakarta.persistence.Basic
	 */
	BASIC( PersistentAttributeType.BASIC ),

	/**
	 * @see jakarta.persistence.Embedded
	 */

	EMBEDDED( PersistentAttributeType.EMBEDDED ),

	/**
	 * @see org.hibernate.annotations.Any
	 */
	ANY( null ),

	/**
	 * @see jakarta.persistence.OneToOne
	 */
	ONE_TO_ONE( PersistentAttributeType.ONE_TO_ONE ),

	/**
	 * @see jakarta.persistence.ManyToOne
	 */
	MANY_TO_ONE( PersistentAttributeType.MANY_TO_ONE ),

	/**
	 * @see jakarta.persistence.ElementCollection
	 */
	ELEMENT_COLLECTION( PersistentAttributeType.ELEMENT_COLLECTION ),

	/**
	 * @see jakarta.persistence.OneToMany
	 */
	ONE_TO_MANY( PersistentAttributeType.ONE_TO_MANY ),

	/**
	 * @see jakarta.persistence.ManyToMany
	 */
	MANY_TO_MANY( PersistentAttributeType.MANY_TO_MANY );

	private final PersistentAttributeType jpaClassification;

	AttributeClassification(PersistentAttributeType jpaClassification) {
		this.jpaClassification = jpaClassification;
	}

	/**
	 * The associated {@link PersistentAttributeType}, if one
	 */
	public PersistentAttributeType getJpaClassification() {
		return jpaClassification;
	}
}
