/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.view;

import org.hibernate.boot.models.mapping.internal.model.IdentifierContribution;

import jakarta.annotation.Nullable;

/// Common read view over entity and mapped-superclass binding facts.
///
/// @since 9.0
/// @author Steve Ebersole
public interface IdentifiableTypeView extends ManagedTypeView {
	@Nullable IdentifierContribution identifierContribution();

	default @Nullable IdentifierContributionView identifierContributionView() {
		final IdentifierContribution contribution = identifierContribution();
		return contribution == null ? null : new IdentifierContributionView( contribution );
	}
}
