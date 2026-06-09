/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

/// Composite key mapping physically represented by one embeddable attribute.
///
/// @see jakarta.persistence.EmbeddedId
///
/// @since 9.0
/// @author Steve Ebersole
public interface AggregatedKeyMapping extends CompositeKeyMapping, SingleAttributeKeyMapping {
}
