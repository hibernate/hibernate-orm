/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import org.hibernate.models.spi.TypeDetails;

/// Categorized metadata for a mapped value.
///
/// Values are intentionally distinct from attributes.  A singular attribute
/// maps one value, while collection elements and indexes/map keys are values
/// without their own persistent Java attribute or backing member.
///
/// This acts as an early categorized form of [org.hibernate.mapping.Value].
///
/// @since 9.0
/// @author Steve Ebersole
public interface ValueMetadata {
	/// The Java type resolved for this application of the value.
	TypeDetails getType();

	/// The semantic shape of the value.
	ValueNature getNature();
}
