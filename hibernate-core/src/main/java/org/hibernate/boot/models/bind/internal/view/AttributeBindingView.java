/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.view;

import jakarta.persistence.AccessType;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.models.bind.internal.model.AttributeBinding;
import org.hibernate.boot.models.bind.internal.model.ManagedTypeBinding;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/// Stable read view over a finalized attribute binding.
///
/// The view exposes source and option facts without exposing the materialized
/// `Property`, `Value`, or `Column` objects as semantic state.
///
/// @since 9.0
/// @author Steve Ebersole
public record AttributeBindingView(AttributeBinding binding) {
	public String attributeName() {
		return binding.attributeName();
	}

	public AttributeMetadata attributeMetadata() {
		return binding.attributeMetadata();
	}

	public ManagedTypeBinding ownerType() {
		return binding.ownerType();
	}

	public ManagedTypeBinding declaringType() {
		return binding.declaringType();
	}

	public MemberDetails member() {
		return binding.member();
	}

	public AccessType accessType() {
		return binding.accessType();
	}

	public AttributeNature nature() {
		return binding.nature();
	}

	public String sourceRole() {
		return binding.sourceRole();
	}

	public String attributePath() {
		return binding.attributePath();
	}

	public boolean isNaturalId() {
		return binding.isNaturalId();
	}

	public boolean naturalIdMutable() {
		return binding.naturalIdMutable();
	}

	public String collation() {
		return binding.collation();
	}

	public String lazyGroup() {
		return binding.lazyGroup();
	}

	public boolean optimisticLocked() {
		return binding.optimisticLocked();
	}

	public boolean immutable() {
		return binding.immutable();
	}

	public Class<? extends MutabilityPlan<?>> explicitMutabilityPlanClass() {
		return binding.explicitMutabilityPlanClass();
	}
}
