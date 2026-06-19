/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import org.hibernate.boot.mapping.internal.view.NaturalIdContributionView;
import org.hibernate.mapping.Property;

/// Materializes the legacy mapping flags for a `@NaturalId` attribute.
///
/// Natural-id handling is intentionally represented as a binding contribution
/// before mutating the legacy `Property`, making this a second proof of the
/// binding-layer replacement shape for built-in and custom attribute binders.
///
/// @since 9.0
/// @author Steve Ebersole
public class NaturalIdMappingMaterializer {
	public void materializeNaturalId(NaturalIdContributionView contribution, Property property) {
		property.setNaturalIdentifier( true );
		property.setUpdatable( contribution.mutable() );
	}
}
