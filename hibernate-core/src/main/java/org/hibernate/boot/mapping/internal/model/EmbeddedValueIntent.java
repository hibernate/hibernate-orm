/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

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
		String fullPath) implements ValueIntent {
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
}
