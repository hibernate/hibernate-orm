/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.binders;

import java.util.List;

import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;

/// Pending association target binding for a non-primary-key reference.
///
/// Join columns that name target columns outside the identifier need all target
/// properties to be bound before the binder can identify the referenced property.
/// This typed state lets member binding create the association value while a later
/// phase resolves the property reference.
///
/// @since 9.0
/// @author Steve Ebersole
public record AssociationTargetBinding(
		PersistentClass ownerBinding,
		ManyToOne value,
		EntityTypeBinder targetTypeBinder,
		List<String> referencedColumnNames,
		String role) {
}
