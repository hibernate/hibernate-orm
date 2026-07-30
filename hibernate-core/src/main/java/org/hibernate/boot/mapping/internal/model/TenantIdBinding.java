/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadataImpl;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.type.BasicType;

/// Binding-layer state for a source-model `@TenantId` attribute.
///
/// The binding owns the semantic tenant-id source facts; materialization
/// later turns it into the compatibility outputs needed by filters and
/// row-level-security support.
///
/// @since 9.0
/// @author Steve Ebersole
public record TenantIdBinding(
		EntityTypeMetadataImpl owner,
		String attributeName,
		MemberDetails member,
		TypeDetails resolvedType,
		BasicValueIntent valueIntent,
		BasicType<?> tenantIdType) {
}
