/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import java.util.List;

/// Categorized description of an embedded value and its usage-specific
/// embeddable applications.
///
/// The primary usage describes the declared embeddable type. Subtype usages
/// describe additional concrete embeddable types of a polymorphic embeddable.
///
/// @since 9.0
/// @author Steve Ebersole
public interface EmbeddedValueMetadata extends ValueMetadata {
	/// The primary embeddable usage.
	EmbeddableUsageMetadata getEmbeddableUsage();

	/// Concrete subtype usages for a polymorphic embeddable, or an empty list
	/// when the embeddable is not polymorphic.
	List<? extends EmbeddableUsageMetadata> getSubtypeUsages();
}
