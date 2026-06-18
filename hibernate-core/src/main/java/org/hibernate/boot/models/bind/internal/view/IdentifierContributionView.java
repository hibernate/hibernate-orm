/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.view;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.models.bind.internal.model.IdentifierAttributeBinding;
import org.hibernate.boot.models.bind.internal.model.IdentifierContribution;
import org.hibernate.boot.models.bind.internal.model.IdentifierExtractionKind;
import org.hibernate.models.spi.MemberDetails;

import jakarta.annotation.Nullable;

/// Stable read view over a finalized identifier contribution.
///
/// This is the pilot view from the resolved-model proposal: consumers ask this
/// view for identifier classification and projected column order instead of
/// inferring those facts from duplicated or sorted mapping-model structures.
///
/// @since 9.0
/// @author Steve Ebersole
public record IdentifierContributionView(IdentifierContribution contribution) {
	public List<Attribute> attributes() {
		return contribution.attributes().stream()
				.map( Attribute::new )
				.toList();
	}

	public @Nullable Attribute attribute(String attributeName) {
		final IdentifierAttributeBinding attribute = contribution.getAttribute( attributeName );
		return attribute == null ? null : new Attribute( attribute );
	}

	public Set<String> idAttributeNames() {
		final LinkedHashSet<String> names = new LinkedHashSet<>();
		for ( IdentifierAttributeBinding attribute : contribution.attributes() ) {
			names.add( attribute.attributeName() );
		}
		return names;
	}

	public List<String> identifierSelectableNames() {
		return contribution.attributes().stream()
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
