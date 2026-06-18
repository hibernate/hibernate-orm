/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.views;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.models.bind.internal.binding.IdentifierAttributeBinding;
import org.hibernate.boot.models.bind.internal.binding.IdentifierContribution;
import org.hibernate.mapping.Column;

/// Stable read view over a finalized identifier contribution.
///
/// This is the pilot view from the resolved-model proposal: consumers ask this
/// view for identifier classification and projected column order instead of
/// inferring those facts from duplicated or sorted mapping-model structures.
///
/// @since 9.0
/// @author Steve Ebersole
public record IdentifierContributionView(IdentifierContribution contribution) {
	public Set<String> idAttributeNames() {
		final LinkedHashSet<String> names = new LinkedHashSet<>();
		for ( IdentifierAttributeBinding attribute : contribution.attributes() ) {
			names.add( attribute.attributeName() );
		}
		return names;
	}

	public List<Column> identifierColumns() {
		return contribution.attributes().stream()
				.flatMap( (attribute) -> attribute.columns().stream() )
				.toList();
	}
}
