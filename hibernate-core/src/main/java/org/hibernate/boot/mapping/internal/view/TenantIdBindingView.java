/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.view;

import org.hibernate.boot.mapping.internal.model.TenantIdBinding;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.BasicType;

/// Stable read view over a finalized tenant-id binding.
///
/// The view exposes tenant-id source facts without exposing the materialized
/// `Property`, `BasicValue`, filter, or row-level-security mapping objects as
/// semantic state.
///
/// @since 9.0
/// @author Steve Ebersole
public record TenantIdBindingView(TenantIdBinding binding) {
	public EntityTypeMetadata owner() {
		return binding.owner();
	}

	public String attributeName() {
		return binding.attributeName();
	}

	public MemberDetails member() {
		return binding.member();
	}

	public BasicValueIntent valueIntent() {
		return binding.valueIntent();
	}

	public BasicType<?> tenantIdType() {
		return binding.tenantIdType();
	}
}
