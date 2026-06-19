/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.model;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.models.mapping.internal.sources.ComponentSource;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.AssociationOverride;

/// Source-level intent for a to-one-valued component member.
///
/// This intent records the source facts needed to delegate to to-one
/// materialization while keeping the binding model free of compatibility
/// association and foreign-key objects.
///
/// @since 9.0
/// @author Steve Ebersole
public record ToOneValueIntent(
		TypeDetails memberType,
		String path,
		String fullPath,
		AssociationOverride associationOverride) implements ValueIntent {
	@Override
	public AttributeNature nature() {
		return AttributeNature.TO_ONE;
	}

	public static ToOneValueIntent fromComponentMember(
			ComponentSource source,
			ComponentSource.ComponentMember member) {
		return new ToOneValueIntent(
				member.type(),
				member.path(),
				member.fullPath(),
				source.associationOverride( member.path() )
		);
	}
}
