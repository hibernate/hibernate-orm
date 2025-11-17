/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

/**
 * An enum defining the nature (categorization) of a persistent attribute.
 *
 * @see jakarta.persistence.metamodel.Attribute.PersistentAttributeType
 */
public enum AttributeNature {
	BASIC,
	EMBEDDED,
	ANY,
	TO_ONE,
	ELEMENT_COLLECTION,
	MANY_TO_ANY,
	MANY_TO_MANY,
	ONE_TO_MANY
}
