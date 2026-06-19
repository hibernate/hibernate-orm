/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

/// Standard binding-model usage of a persistent attribute declaration.
///
/// A usage binds a declaration into one concrete usage container.  The
/// declaration container and usage container may be the same managed type for a
/// direct attribute, or different for inherited, mapped-superclass, embedded,
/// collection-element, and map-key contexts.
///
/// @since 9.0
/// @author Steve Ebersole
public record StandardAttributeUsageBinding(
		AttributeDeclarationBinding declaration,
		AttributeUsageContainer usageContainer,
		MemberDetails member,
		TypeDetails resolvedType,
		String sourceRole,
		String attributePath,
		AttributeNature nature,
		ValueIntent valueIntent) implements AttributeUsageBinding {
	@Override
	public String attributeName() {
		return declaration.attributeName();
	}
}
