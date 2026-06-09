/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.view;

import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;

/// Standard read view over a managed-type binding when no specialized entity,
/// mapped-superclass, or embeddable view is needed.
///
/// @since 9.0
/// @author Steve Ebersole
public record StandardManagedTypeView(ManagedTypeBinding binding) implements ManagedTypeView {
}
