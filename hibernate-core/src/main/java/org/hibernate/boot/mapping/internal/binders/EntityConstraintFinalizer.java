/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;

/// Finalizes physical constraints for an entity after value and key binding.
///
/// This replaces the new-pipeline use of an anonymous metadata-collector second
/// pass for `PersistentClass#createConstraints`.  The work still happens late,
/// but it is now named as entity binding finalization.
///
/// @since 9.0
/// @author Steve Ebersole
final class EntityConstraintFinalizer {
	private EntityConstraintFinalizer() {
	}

	static void finalizeConstraints(PersistentClass entityBinding, MetadataBuildingContext context) {
		entityBinding.createConstraints( context );
	}
}
