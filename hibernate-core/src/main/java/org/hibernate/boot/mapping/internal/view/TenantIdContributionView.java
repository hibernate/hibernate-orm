/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.view;

import org.hibernate.boot.mapping.internal.model.TenantIdContribution;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.BasicType;

/// Stable read view over a finalized tenant-id contribution.
///
/// The view exposes tenant-id source facts without exposing the materialized
/// `Property`, `BasicValue`, filter, or row-level-security mapping objects as
/// semantic state.
///
/// @since 9.0
/// @author Steve Ebersole
public record TenantIdContributionView(TenantIdContribution contribution) {
	public EntityTypeMetadata owner() {
		return contribution.owner();
	}

	public String attributeName() {
		return contribution.attributeName();
	}

	public MemberDetails member() {
		return contribution.member();
	}

	public BasicType<?> tenantIdType() {
		return contribution.tenantIdType();
	}
}
