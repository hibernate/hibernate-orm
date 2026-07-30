/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.List;

import org.hibernate.boot.mapping.spi.EmbeddedValueMetadata;
import org.hibernate.boot.mapping.spi.ValueNature;
import org.hibernate.models.spi.TypeDetails;

/// Standard [EmbeddedValueMetadataImpl] implementation.
///
/// @since 9.0
/// @author Steve Ebersole
public record EmbeddedValueMetadataImpl(
		TypeDetails type,
		EmbeddableUsageMetadataImpl embeddableUsage,
		List<EmbeddableUsageMetadataImpl> subtypeUsages) implements EmbeddedValueMetadata {
	public EmbeddedValueMetadataImpl {
		subtypeUsages = List.copyOf( subtypeUsages );
	}

	public EmbeddedValueMetadataImpl(TypeDetails type, EmbeddableUsageMetadataImpl embeddableUsage) {
		this( type, embeddableUsage, List.of() );
	}

	@Override
	public TypeDetails getType() {
		return type;
	}

	@Override
	public ValueNature getNature() {
		return ValueNature.EMBEDDED;
	}

	@Override
	public EmbeddableUsageMetadataImpl getEmbeddableUsage() {
		return embeddableUsage;
	}

	@Override
	public List<EmbeddableUsageMetadataImpl> getSubtypeUsages() {
		return subtypeUsages;
	}
}
