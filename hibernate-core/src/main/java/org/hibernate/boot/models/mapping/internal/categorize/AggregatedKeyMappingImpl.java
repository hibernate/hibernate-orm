/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;


/// Standard AggregatedKeyMapping impl
///
/// @since 9.0
/// @author Steve Ebersole
public record AggregatedKeyMappingImpl(AttributeMetadata attribute) implements AggregatedKeyMapping {
	@Override
	public AttributeMetadata getAttribute() {
		return attribute;
	}
}
