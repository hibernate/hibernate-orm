/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.view;

import org.hibernate.boot.models.bind.internal.model.EntityTypeBinding;
import org.hibernate.boot.models.bind.internal.model.IdentifierContribution;

import jakarta.annotation.Nullable;

/// Stable read view over a finalized entity binding.
///
/// @since 9.0
/// @author Steve Ebersole
public record EntityView(
		EntityTypeBinding binding,
		@Nullable IdentifierContribution identifierContribution) implements IdentifiableTypeView {
}
