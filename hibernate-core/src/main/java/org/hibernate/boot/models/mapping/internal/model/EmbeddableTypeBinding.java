/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.model;

import jakarta.persistence.AccessType;

import org.hibernate.models.spi.ClassDetails;

/// Binding-model declaration node for an embeddable type.
///
/// @since 9.0
/// @author Steve Ebersole
public class EmbeddableTypeBinding extends ManagedTypeBinding {
	public EmbeddableTypeBinding(ClassDetails classDetails, AccessType accessType) {
		super( classDetails, Kind.EMBEDDABLE, accessType );
	}
}
