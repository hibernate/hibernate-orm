/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import org.hibernate.HibernateException;
import org.hibernate.sql.results.graph.FetchOptions;

/**
 * Details about the discriminator for an embeddable hierarchy.
 *
 * @author Marco Belladelli
 * @see EmbeddableMappingType#getDiscriminatorMapping()
 */
public interface EmbeddableDiscriminatorMapping extends DiscriminatorMapping, FetchOptions {
	/**
	 * Retrieve the relational discriminator value corresponding to the provided embeddable class name.
	 *
	 * @throws HibernateException if the embeddable class name is not handled by this discriminator
	 */
	@Nullable
	default Object getDiscriminatorValue(@Nonnull String embeddableClassName) {
		return getValueConverter().getDetailsForEntityName( embeddableClassName ).getValue();
	}
}
