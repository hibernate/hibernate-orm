/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.view;

import org.hibernate.boot.models.mapping.internal.model.NaturalIdContribution;
import org.hibernate.boot.models.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.models.spi.MemberDetails;

/// Stable read view over a finalized natural-id contribution.
///
/// The view exposes natural-id source facts without exposing the materialized
/// `Property` as semantic state.
///
/// @since 9.0
/// @author Steve Ebersole
public record NaturalIdContributionView(NaturalIdContribution contribution) {
	public IdentifiableTypeMetadata owner() {
		return contribution.owner();
	}

	public String attributeName() {
		return contribution.attributeName();
	}

	public MemberDetails member() {
		return contribution.member();
	}

	public boolean mutable() {
		return contribution.mutable();
	}
}
