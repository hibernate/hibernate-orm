/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Highly abstract concept of the index of an "indexed persistent collection".
 * More concretely (and generally more usefully) categorized as either:<ul>
 *     <li>{@link PluralAttributeSequentialIndexSource} - for list/array indexes</li>
 *     <li>{@link PluralAttributeMapKeySource} - for map keys</li>
 * </ul>
 *
 */
public interface PluralAttributeIndexSource {
	PluralAttributeIndexNature getNature();

	/**
	 * Obtain information about the Hibernate index type ({@link org.hibernate.type.Type})
	 * for this plural attribute index.
	 *
	 * @return The Hibernate type information
	 */
	HibernateTypeSource getTypeInformation();

	String getXmlNodeName();
}
