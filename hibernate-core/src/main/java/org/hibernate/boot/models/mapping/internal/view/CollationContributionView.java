/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.view;

import org.hibernate.boot.models.mapping.internal.model.CollationContribution;
import org.hibernate.boot.models.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.models.spi.MemberDetails;

/// Stable read view over a finalized collation contribution.
///
/// The view exposes source facts and the requested collation without exposing
/// the materialized `Property`, `Value`, or `Column` objects as semantic state.
///
/// @since 9.0
/// @author Steve Ebersole
public record CollationContributionView(CollationContribution contribution) {
	public IdentifiableTypeMetadata owner() {
		return contribution.owner();
	}

	public String attributePath() {
		return contribution.attributePath();
	}

	public MemberDetails member() {
		return contribution.member();
	}

	public String collation() {
		return contribution.collation();
	}
}
