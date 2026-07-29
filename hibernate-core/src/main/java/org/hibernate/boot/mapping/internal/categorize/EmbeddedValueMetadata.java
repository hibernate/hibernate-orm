/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.List;

/// Categorized metadata for an embedded value.
///
/// @since 9.0
/// @author Steve Ebersole
public interface EmbeddedValueMetadata extends ValueMetadata {
	EmbeddableUsageMetadata getEmbeddableUsage();

	/// Categorized applications of polymorphic embeddable subtypes.
	///
	/// Each usage contains the effective members and resolved types for one
	/// concrete subtype at this same embedded site.
	List<EmbeddableUsageMetadata> getSubtypeUsages();
}
