/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import jakarta.persistence.AccessType;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.models.spi.MemberDetails;

/// Binding-model node for one declared or applied persistent attribute.
///
/// Attribute bindings preserve the source member, declaring managed type,
/// effective access strategy, semantic attribute kind, and source role/path used
/// while resolving overrides, identifier participation, association targets, and
/// column/selectable correspondence.
///
/// @since 9.0
/// @author Steve Ebersole
public record AttributeBinding(
		String attributeName,
		ManagedTypeBinding declaringType,
		MemberDetails member,
		AccessType accessType,
		AttributeNature nature,
		String sourceRole) {
}
