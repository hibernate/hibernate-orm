/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import jakarta.annotation.Nullable;
import jakarta.persistence.AccessType;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeVariableScope;

/// Categorized declaration of an embeddable type.
///
/// An embeddable declaration is intentionally distinct from an
/// [EmbeddableUsageMetadata application] of the type. In particular, access
/// type and generic member types may vary between applications of the same
/// embeddable declaration.
///
/// @since 9.0
/// @author Steve Ebersole
public interface EmbeddableTypeMetadata {
	/// The Java type which declares the embeddable.
	ClassDetails getClassDetails();

	/// Access explicitly declared on the embeddable type, or `null` when an
	/// application inherits access from its containing managed type.
	@Nullable
	AccessType getExplicitAccessType();

	/// Resolve one application of this embeddable declaration.
	EmbeddableUsageMetadata resolveUsage(
			MemberDetails sourceMember,
			TypeVariableScope typeVariableScope,
			AccessType inheritedAccessType,
			CategorizationContext context);
}
