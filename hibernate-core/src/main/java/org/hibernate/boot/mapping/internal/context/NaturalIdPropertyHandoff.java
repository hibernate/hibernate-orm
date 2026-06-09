/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import org.hibernate.boot.mapping.internal.view.NaturalIdContributionView;
import org.hibernate.mapping.Property;

/// Links a semantic natural-id contribution to its materialized compatibility
/// property.
///
/// @since 9.0
/// @author Steve Ebersole
public record NaturalIdPropertyHandoff(
		NaturalIdContributionView contribution,
		Property property) {
}
