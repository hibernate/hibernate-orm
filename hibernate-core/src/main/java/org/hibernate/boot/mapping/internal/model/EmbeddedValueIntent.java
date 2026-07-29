/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import jakarta.annotation.Nullable;

import org.hibernate.boot.mapping.internal.categorize.EmbeddedValueMetadata;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.models.spi.TypeDetails;

/// Source-level intent for an embedded-valued component member.
///
/// The intent captures the path and type facts needed to apply a nested
/// embeddable contribution.  It intentionally does not retain the materialized
/// compatibility object produced by the materialization phase.
///
/// @since 9.0
/// @author Steve Ebersole
public record EmbeddedValueIntent(
		TypeDetails memberType,
		String path,
		String fullPath,
		@Nullable EmbeddedValueMetadata valueMetadata) implements ValueIntent {
	public EmbeddedValueIntent(TypeDetails memberType, String path, String fullPath) {
		this( memberType, path, fullPath, null );
	}

	@Override
	public AttributeNature nature() {
		return AttributeNature.EMBEDDED;
	}

	public static EmbeddedValueIntent fromComponentMember(ComponentSource.ComponentMember member) {
		return new EmbeddedValueIntent(
				member.type(),
				member.path(),
				member.fullPath()
		);
	}

	public static EmbeddedValueIntent fromAttribute(TypeDetails memberType, String attributeName, String sourceRole) {
		return new EmbeddedValueIntent(
				memberType,
				attributeName,
				sourceRole
		);
	}

	public static EmbeddedValueIntent fromAttribute(
			EmbeddedValueMetadata valueMetadata,
			String attributeName,
			String sourceRole) {
		return new EmbeddedValueIntent(
				valueMetadata.getType(),
				attributeName,
				sourceRole,
				valueMetadata
		);
	}

	public static EmbeddedValueIntent fromAttribute(
			@Nullable EmbeddedValueMetadata valueMetadata,
			TypeDetails memberType,
			String attributeName,
			String sourceRole) {
		if ( valueMetadata == null ) {
			return fromAttribute( memberType, attributeName, sourceRole );
		}
		return new EmbeddedValueIntent( memberType, attributeName, sourceRole, valueMetadata );
	}
}
