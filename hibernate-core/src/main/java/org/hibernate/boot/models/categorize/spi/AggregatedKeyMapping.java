/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

/**
 * CompositeIdMapping which is physically an embeddable and represented by a single attribute.
 *
 * @see jakarta.persistence.EmbeddedId
 *
 * @author Steve Ebersole
 */
public interface AggregatedKeyMapping extends CompositeKeyMapping, SingleAttributeKeyMapping {
}
