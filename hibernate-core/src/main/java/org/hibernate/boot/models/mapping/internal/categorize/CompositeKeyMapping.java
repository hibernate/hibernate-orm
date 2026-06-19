/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

/// Key mapping whose key type is composite.
///
/// A composite key can be represented by a physical embeddable attribute, as with
/// {@link AggregatedKeyMapping}, or by multiple id attributes that are virtually
/// composed through an id-class, as with {@link NonAggregatedKeyMapping}.
///
/// @since 9.0
/// @author Steve Ebersole
public interface CompositeKeyMapping extends KeyMapping {
}
