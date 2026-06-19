/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.extension;

import org.hibernate.boot.mapping.internal.model.AttributeUsageBinding;
import org.hibernate.type.BasicType;

/// Capability-oriented target for an internal attribute binding contribution.
///
/// The target exposes semantic facets instead of compatibility objects.  The
/// current internal implementation may still adapt these facet calls to
/// materialization products, but contributors should express what they mean in
/// terms of attribute options, selectable/value intent, or containing-entity
/// effects.
///
/// @since 9.0
/// @author Steve Ebersole
public interface AttributeBindingTarget {
	/// The concrete attribute usage being contributed to.
	AttributeUsageBinding usage();

	/// Attribute-level option contributions.
	AttributeOptions options();

	/// Selectable/value-oriented contributions.
	SelectableTarget selectables();

	/// Contributions that affect the containing entity in addition to the
	/// attribute itself.
	EntityTarget entity();

	interface AttributeOptions {
		/// Mark this attribute as a natural-id part.
		void naturalId(boolean mutable);
	}

	interface SelectableTarget {
		/// Apply a collation request to this attribute's selectable output.
		void collation(String collation);
	}

	interface EntityTarget {
		/// Mark this attribute as the tenant-id attribute for the containing
		/// entity.
		void tenantId(BasicType<?> tenantIdType);
	}
}
