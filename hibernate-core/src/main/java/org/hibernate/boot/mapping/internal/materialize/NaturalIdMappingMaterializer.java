/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.context.NaturalIdPropertyHandoff;
import org.hibernate.boot.mapping.internal.view.NaturalIdContributionView;
import org.hibernate.mapping.Property;

/// Materializes the legacy mapping flags for a `@NaturalId` attribute.
///
/// Natural-id handling is represented as internal binding state before the
/// corresponding legacy `Property` is updated.
///
/// @since 9.0
/// @author Steve Ebersole
public class NaturalIdMappingMaterializer {
	public void materializeNaturalId(NaturalIdContributionView contribution, Property property, BindingState state) {
		property.setNaturalIdentifier( true );
		property.resetUpdateable( contribution.mutable() );
		state.addNaturalIdPropertyHandoff( new NaturalIdPropertyHandoff( contribution, property ) );
	}
}
