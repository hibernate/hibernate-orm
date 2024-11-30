/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel;

/**
 * At the end of the day, any "value mapping" (id, version, attribute, collection element, etc.)
 * can be one of a few classifications.  This defines an enumeration of those classifications.
 *
 * @author Steve Ebersole
 */
public enum ValueClassification {
	/**
	 * The mapped value is a basic value (String, Date, etc).
	 */
	BASIC,
	/**
	 * ANY is a Hibernate-specific concept.  It is essentially a "reverse discrimination".
	 * Here, the association itself defines a discriminator to the associated entity,
	 * sort of the reverse of discriminator-inheritance.  Here the various associated types
	 * can be unrelated in terms of mapped inheritance.
	 */
	ANY,
	/**
	 * An {@link jakarta.persistence.Embeddable} value
	 */
	EMBEDDABLE,
	/**
	 * Reference to an entity
	 */
	ENTITY
}
