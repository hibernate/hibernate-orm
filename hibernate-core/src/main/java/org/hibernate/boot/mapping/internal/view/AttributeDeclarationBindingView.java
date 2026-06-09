/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.view;

import jakarta.persistence.AccessType;

import org.hibernate.boot.mapping.internal.model.AttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.models.spi.MemberDetails;

/// Stable read view over a finalized attribute declaration binding.
///
/// This view is declaration-oriented.  It describes the attribute member owned
/// by a managed type, not a particular usage/application of that member at an
/// entity or embedded path.
///
/// @since 9.0
/// @author Steve Ebersole
public record AttributeDeclarationBindingView(AttributeDeclarationBinding binding) {
	public String attributeName() {
		return binding.attributeName();
	}

	public ManagedTypeBinding declarationContainer() {
		return binding.declarationContainer();
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
}
