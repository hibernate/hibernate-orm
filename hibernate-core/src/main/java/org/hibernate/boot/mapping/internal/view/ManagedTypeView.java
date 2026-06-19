/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.view;

import java.util.List;

import jakarta.persistence.AccessType;

import org.hibernate.boot.mapping.internal.model.AttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.AttributeUsageBinding;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.models.spi.ClassDetails;

/// Stable read view over a finalized managed-type binding.
///
/// @since 9.0
/// @author Steve Ebersole
public interface ManagedTypeView {
	ManagedTypeBinding binding();

	default ClassDetails classDetails() {
		return binding().classDetails();
	}

	default ManagedTypeBinding.Kind kind() {
		return binding().kind();
	}

	default AccessType accessType() {
		return binding().accessType();
	}

	default List<AttributeDeclarationBinding> declaredAttributes() {
		return binding().declaredAttributes();
	}

	default List<AttributeBindingView> declaredAttributeViews() {
		return binding().attributeUsages().stream()
				.map( AttributeBindingView::new )
				.toList();
	}

	default List<AttributeUsageBinding> attributeUsages() {
		return binding().attributeUsages();
	}
}
