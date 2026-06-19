/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.view;

import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.model.VersionBinding;
import org.hibernate.models.spi.MemberDetails;

/// Stable read view over a finalized version binding.
///
/// The view exposes version source facts and value intent without exposing the
/// materialized `Property` or `BasicValue` as semantic state.
///
/// @since 9.0
/// @author Steve Ebersole
public record VersionBindingView(VersionBinding binding) {
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
}
