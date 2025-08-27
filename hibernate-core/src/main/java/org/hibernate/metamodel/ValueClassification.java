/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel;

/**
 * Any "value mapping" (id, version, attribute, collection element, etc.)
 * belongs to one of several broad categories. This enumeration is quite
 * similar to {@link jakarta.persistence.metamodel.Type.PersistenceType}.
 *
 * @author Steve Ebersole
 *
 * @see jakarta.persistence.metamodel.Type.PersistenceType
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
