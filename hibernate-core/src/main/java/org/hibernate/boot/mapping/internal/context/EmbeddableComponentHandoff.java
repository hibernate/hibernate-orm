/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import org.hibernate.boot.mapping.internal.view.EmbeddableContributionView;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;

/// Links one applied embeddable contribution to the legacy component materialized
/// for compatibility consumers.
///
/// @since 9.0
/// @author Steve Ebersole
public record EmbeddableComponentHandoff(
		EmbeddableContributionView contribution,
		PersistentClass owner,
		Component component) {
	public String pathPrefix() {
		return contribution.pathPrefix();
	}
}
