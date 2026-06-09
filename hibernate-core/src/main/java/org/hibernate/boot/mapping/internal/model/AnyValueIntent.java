/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.sources.AnySource;

/// Source-level intent for an any-valued attribute usage.
///
/// `@Any` is physically materialized as discriminator and key values, but those
/// two pieces represent one association-valued usage.  The [AnySource] keeps the
/// discriminator, key, cascade, optionality, and optional association-table facts
/// together until compatibility `org.hibernate.mapping.Any` materialization.
///
/// @since 9.0
/// @author Steve Ebersole
public record AnyValueIntent(AnySource source) implements ValueIntent {
	@Override
	public AttributeNature nature() {
		return AttributeNature.ANY;
	}
}
