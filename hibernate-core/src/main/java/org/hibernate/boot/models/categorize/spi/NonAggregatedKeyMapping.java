/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.List;

import org.hibernate.models.spi.ClassDetails;

/**
 * CompositeIdMapping which is virtually an embeddable and represented by one-or-more
 * {@linkplain #getIdAttributes id-attributes} identified by one-or-more {@code @Id}
 * annotations.
 * Also defines an {@linkplain #getIdClassType() id-class} which is used for loading.

 * @see jakarta.persistence.Id
 * @see jakarta.persistence.IdClass
 *
 * @author Steve Ebersole
 */
public interface NonAggregatedKeyMapping extends CompositeKeyMapping {
	/**
	 * The attributes making up the composition.
	 */
	List<AttributeMetadata> getIdAttributes();

	/**
	 * Details about the {@linkplain jakarta.persistence.IdClass id-class}.
	 *
	 * @see jakarta.persistence.IdClass
	 */
	ClassDetails getIdClassType();
}
