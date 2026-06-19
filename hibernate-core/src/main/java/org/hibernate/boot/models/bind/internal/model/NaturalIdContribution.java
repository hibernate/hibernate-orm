/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.spi.MemberDetails;

/// Binding-layer contribution for a source-model `@NaturalId` attribute.
///
/// This is the second small proof for a possible binding-layer replacement
/// shape for `org.hibernate.binder.AttributeBinder`.  Unlike `@TenantId`,
/// natural-id binding is mostly compatibility decoration today, but it is still
/// attribute-level semantic state: the annotation may appear on an inherited
/// identifiable-type attribute, and the resulting natural-id contract is
/// consumed later by entity mapping and runtime layers.
///
/// The contribution records only source/binding facts.  Materialization applies
/// those facts to compatibility outputs until natural-id handling can be
/// consumed directly from binding/model views.
///
/// @since 9.0
/// @author Steve Ebersole
public record NaturalIdContribution(
		IdentifiableTypeMetadata owner,
		String attributeName,
		MemberDetails member,
		boolean mutable) {
}
