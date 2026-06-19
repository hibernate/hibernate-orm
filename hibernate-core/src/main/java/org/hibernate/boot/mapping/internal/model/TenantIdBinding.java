/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.BasicType;

/// Binding-layer state for a source-model `@TenantId` attribute.
///
/// This is an internal proof for a possible binding-layer replacement shape for
/// `org.hibernate.binder.AttributeBinder`.  The proof is intentionally
/// attribute-level rather than basic-value-shaped: `@TenantId` happens to
/// materialize a basic property today, but the extension concept being tested is
/// "an annotation contributes structured attribute semantics before
/// compatibility objects are materialized".
///
/// The binding owns the semantic tenant-id source facts; materialization
/// later turns it into the compatibility outputs needed by filters and
/// row-level-security support.
///
/// @since 9.0
/// @author Steve Ebersole
public record TenantIdBinding(
		EntityTypeMetadata owner,
		String attributeName,
		MemberDetails member,
		BasicValueIntent valueIntent,
		BasicType<?> tenantIdType) {
}
