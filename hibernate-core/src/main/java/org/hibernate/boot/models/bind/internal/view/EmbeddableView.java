/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.view;

import org.hibernate.boot.models.bind.internal.model.EmbeddableTypeBinding;

/// Stable read view over a finalized embeddable binding.
///
/// @since 9.0
/// @author Steve Ebersole
public record EmbeddableView(EmbeddableTypeBinding binding) implements ManagedTypeView {
}
