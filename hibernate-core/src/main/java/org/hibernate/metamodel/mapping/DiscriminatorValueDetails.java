/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

/**
 * Details for a particular discriminator value.
 *
 * @apiNote For {@linkplain jakarta.persistence.InheritanceType#JOINED joined} and
 * {@linkplain jakarta.persistence.InheritanceType#TABLE_PER_CLASS union} inheritance,
 * the discriminator also effectively indicates a specific table.  That table can be
 * found via {@linkplain EntityMappingType#getMappedTableDetails()} for the
 * {@linkplain #getIndicatedEntity() indicated entity}
 *
 * @see jakarta.persistence.DiscriminatorValue
 */
public interface DiscriminatorValueDetails {
	/**
	 * The discriminator value
	 */
	Object getValue();

	/**
	 * The name of the concrete entity-type mapped to this {@linkplain #getValue() discriminator value}
	 */
	default String getIndicatedEntityName() {
		return getIndicatedEntity().getEntityName();
	}

	/**
	 * Form of {@link #getIndicatedEntityName()} returning the matched {@link EntityMappingType}
	 */
	EntityMappingType getIndicatedEntity();
}
