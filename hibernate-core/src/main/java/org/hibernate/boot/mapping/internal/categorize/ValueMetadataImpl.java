/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import org.hibernate.models.spi.TypeDetails;

/// Standard metadata for a value without a categorized embeddable usage.
///
/// @since 9.0
/// @author Steve Ebersole
public record ValueMetadataImpl(
		TypeDetails type,
		ValueNature nature) implements ValueMetadata {
	@Override
	public TypeDetails getType() {
		return type;
	}

	@Override
	public ValueNature getNature() {
		return nature;
	}
}
