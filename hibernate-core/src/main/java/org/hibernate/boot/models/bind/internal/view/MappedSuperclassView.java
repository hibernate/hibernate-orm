/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.view;

import org.hibernate.boot.models.bind.internal.model.IdentifierContribution;
import org.hibernate.boot.models.bind.internal.model.MappedSuperclassTypeBinding;

import jakarta.annotation.Nullable;

/// Stable read view over a finalized mapped-superclass binding.
///
/// @since 9.0
/// @author Steve Ebersole
public record MappedSuperclassView(
		MappedSuperclassTypeBinding binding,
		@Nullable IdentifierContribution identifierContribution) implements IdentifiableTypeView {
}
