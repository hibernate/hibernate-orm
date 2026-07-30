/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import org.hibernate.boot.mapping.internal.categorize.AbstractIdentifiableTypeMetadata;
import org.hibernate.models.spi.MemberDetails;

/// Binding-layer contribution for a source-model `@Collate` attribute.
///
/// The contribution records the attribute path because collation can be applied
/// to attributes nested inside embeddables.  Materialization currently applies
/// the collation to the compatibility selectable outputs for the attribute
/// value.
///
/// @since 9.0
/// @author Steve Ebersole
public record CollationContribution(
		AbstractIdentifiableTypeMetadata owner,
		String attributePath,
		MemberDetails member,
		String collation) {
}
