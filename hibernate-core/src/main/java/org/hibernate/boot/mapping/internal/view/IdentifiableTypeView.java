/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.view;

import org.hibernate.boot.mapping.internal.model.EntityIdentifierBinding;

import jakarta.annotation.Nullable;

/// Common read view over entity and mapped-superclass binding facts.
///
/// @since 9.0
/// @author Steve Ebersole
public interface IdentifiableTypeView extends ManagedTypeView {
	@Nullable EntityIdentifierBinding entityIdentifierBinding();

	default @Nullable EntityIdentifierBindingView entityIdentifierBindingView() {
		final EntityIdentifierBinding binding = entityIdentifierBinding();
		return binding == null ? null : new EntityIdentifierBindingView( binding );
	}
}
