/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.view;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.model.IdentifierAttributeBinding;
import org.hibernate.boot.mapping.internal.model.EntityIdentifierBinding;
import org.hibernate.boot.mapping.internal.model.IdentifierExtractionKind;
import org.hibernate.models.spi.MemberDetails;

import jakarta.annotation.Nullable;

/// Stable read view over a finalized identifier binding.
///
/// This is the pilot view from the resolved-model proposal: consumers ask this
/// view for identifier classification and projected column order instead of
/// inferring those facts from duplicated or sorted mapping-model structures.
///
/// @since 9.0
/// @author Steve Ebersole
public record EntityIdentifierBindingView(EntityIdentifierBinding binding) {
	public EntityTypeMetadata owner() {
		return binding.owner();
	}

	public List<Attribute> attributes() {
		return binding.attributes().stream()
				.map( Attribute::new )
				.toList();
	}

	public @Nullable Attribute attribute(String attributeName) {
		final IdentifierAttributeBinding attribute = binding.getAttribute( attributeName );
		return attribute == null ? null : new Attribute( attribute );
	}

	public Set<String> idAttributeNames() {
		final LinkedHashSet<String> names = new LinkedHashSet<>();
		for ( IdentifierAttributeBinding attribute : binding.attributes() ) {
			names.add( attribute.attributeName() );
		}
		return names;
	}

	public List<String> identifierSelectableNames() {
		return binding.attributes().stream()
				.flatMap( (attribute) -> attribute.selectableNames().stream() )
				.toList();
	}

	public record Attribute(IdentifierAttributeBinding binding) {
		public String attributeName() {
			return binding.attributeName();
		}

		public IdentifierExtractionKind extractionKind() {
			return binding.extractionKind();
		}

		public MemberDetails virtualMember() {
			return binding.virtualMember();
		}

		public @Nullable MemberDetails idRepresentationMember() {
			return binding.idRepresentationMember();
		}

		public List<String> selectableNames() {
			return binding.selectableNames();
		}
	}
}
