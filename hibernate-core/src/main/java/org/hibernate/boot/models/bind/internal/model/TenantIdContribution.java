/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.BasicType;

/// Binding-layer contribution for a source-model `@TenantId` attribute.
///
/// This is an internal proof for a possible binding-layer replacement shape for
/// `org.hibernate.binder.AttributeBinder`.  The proof is intentionally
/// attribute-level rather than `BasicValue`-level: `@TenantId` happens to
/// materialize a basic property today, but the extension concept being tested is
/// "an annotation contributes structured attribute semantics before
/// `org.hibernate.mapping` objects exist".
///
/// The contribution owns the semantic tenant-id source facts; materialization
/// later turns it into the legacy property, filter, and row-level-security
/// mapping objects.
///
/// @since 9.0
/// @author Steve Ebersole
public record TenantIdContribution(
		EntityTypeMetadata owner,
		String attributeName,
		MemberDetails member,
		BasicType<?> tenantIdType) {
}
