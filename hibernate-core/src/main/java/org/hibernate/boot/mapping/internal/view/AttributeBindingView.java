/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.view;

import jakarta.persistence.AccessType;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.model.AttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.AttributeUsageBinding;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.model.IdentifiableAttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.model.ValueIntent;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadata;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/// Stable read view over a finalized attribute usage binding.
///
/// The view exposes source and option facts without exposing the materialized
/// `Property`, `Value`, or `Column` objects as semantic state.  Transitional
/// callers that still need categorized attribute metadata read it from the
/// declaration binding.
///
/// @since 9.0
/// @author Steve Ebersole
public record AttributeBindingView(AttributeUsageBinding binding) {
	public AttributeDeclarationBinding declaration() {
		return binding.declaration();
	}

	public AttributeUsageBinding usageBinding() {
		return binding;
	}

	public String attributeName() {
		return binding.attributeName();
	}

	public AttributeMetadata attributeMetadata() {
		final AttributeMetadata attributeMetadata = attributeDeclaration().attributeMetadata();
		if ( attributeMetadata == null ) {
			throw new IllegalStateException(
					"Attribute binding view requires categorized attribute metadata for " + binding.sourceRole()
			);
		}
		return attributeMetadata;
	}

	public ManagedTypeBinding ownerType() {
		return (ManagedTypeBinding) binding.usageContainer();
	}

	public ManagedTypeBinding declaringType() {
		return declaration().declarationContainer();
	}

	public MemberDetails member() {
		return binding.member();
	}

	public AccessType accessType() {
		return declaration().accessType();
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

	public TypeDetails resolvedType() {
		return binding.resolvedType();
	}

	public boolean isNaturalId() {
		return attributeDeclaration().isNaturalId();
	}

	public boolean naturalIdMutable() {
		return attributeDeclaration().naturalIdMutable();
	}

	public String collation() {
		return attributeDeclaration().collation();
	}

	public String lazyGroup() {
		return attributeDeclaration().lazyGroup();
	}

	public boolean optimisticLocked() {
		return attributeDeclaration().optimisticLocked();
	}

	public boolean immutable() {
		return attributeDeclaration().immutable();
	}

	public Class<? extends MutabilityPlan<?>> explicitMutabilityPlanClass() {
		return attributeDeclaration().explicitMutabilityPlanClass();
	}

	public ValueIntent valueIntent() {
		return binding.valueIntent();
	}

	public BasicValueIntent basicValueIntent() {
		return binding.basicValueIntent();
	}

	private IdentifiableAttributeDeclarationBinding attributeDeclaration() {
		if ( binding.declaration() instanceof IdentifiableAttributeDeclarationBinding attributeBinding ) {
			return attributeBinding;
		}
		throw new IllegalStateException(
				"Attribute binding view requires an identifiable attribute declaration for " + binding.sourceRole()
		);
	}
}
