/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.spi.MemberDetails;

/// Binding-layer contribution for a source-model `@Collate` attribute.
///
/// This contribution is the third proof for a possible binding-layer
/// replacement shape for `org.hibernate.binder.AttributeBinder`.  It exercises
/// value/selectable decoration rather than entity-wide side effects
/// (`@TenantId`) or simple attribute flags (`@NaturalId`).
///
/// The contribution records the attribute path because collation can be applied
/// to attributes nested inside embeddables.  Materialization currently applies
/// the collation to the legacy mapping columns for the attribute value.
///
/// @since 9.0
/// @author Steve Ebersole
public record CollationContribution(
		IdentifiableTypeMetadata owner,
		String attributePath,
		MemberDetails member,
		String collation) {
}
