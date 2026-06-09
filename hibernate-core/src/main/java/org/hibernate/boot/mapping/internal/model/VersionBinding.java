/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.models.spi.MemberDetails;

/// Binding-layer state for a source-model `@Version` attribute.
///
/// Version attributes are skipped by ordinary attribute binding because the
/// legacy mapping model stores them in a dedicated `RootClass` slot.  This
/// binding records the source and value intent before materialization
/// creates the compatibility `Property`/`BasicValue` shape.
///
/// @since 9.0
/// @author Steve Ebersole
public record VersionBinding(
		EntityTypeMetadata owner,
		String attributeName,
		MemberDetails member,
		BasicValueIntent valueIntent) {
}
