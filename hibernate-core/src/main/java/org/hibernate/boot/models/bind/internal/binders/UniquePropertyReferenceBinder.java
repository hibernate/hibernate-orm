/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.ToOne;

/// Centralizes the collector side-channel for associations that reference a unique property.
///
/// Non-primary-key association references still need to inform the metadata
/// collector about the referenced unique property so `org.hibernate.mapping`
/// can create property-ref constraints.  Keeping that call here makes the
/// remaining collector dependency visible and gives target-resolution code one
/// place to migrate if the mapping model grows a typed property-reference API.
///
/// @since 9.0
/// @author Steve Ebersole
class UniquePropertyReferenceBinder {
	static void bindUniquePropertyReference(
			BindingState bindingState,
			ToOne value,
			String referencedPropertyName) {
		value.setReferencedPropertyName( referencedPropertyName );
		value.setReferenceToPrimaryKey( false );
		bindingState.addUniquePropertyReference( value.getReferencedEntityName(), referencedPropertyName );
	}
}
