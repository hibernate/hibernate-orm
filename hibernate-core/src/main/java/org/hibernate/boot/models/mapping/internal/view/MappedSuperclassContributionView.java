/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.view;

import java.util.List;

import org.hibernate.boot.models.mapping.internal.model.MappedSuperclassContribution;
import org.hibernate.boot.models.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.models.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.boot.models.mapping.internal.categorize.MappedSuperclassTypeMetadata;

/// Stable read view over a mapped-superclass application.
///
/// @since 9.0
/// @author Steve Ebersole
public record MappedSuperclassContributionView(MappedSuperclassContribution contribution) {
	public MappedSuperclassTypeMetadata declaration() {
		return contribution.declaration();
	}

	public IdentifiableTypeMetadata consumer() {
		return contribution.consumer();
	}

	public EntityTypeMetadata nearestEntityConsumer() {
		return contribution.nearestEntityConsumer();
	}

	public List<String> appliedAttributeNames() {
		return contribution.appliedAttributeNames();
	}
}
