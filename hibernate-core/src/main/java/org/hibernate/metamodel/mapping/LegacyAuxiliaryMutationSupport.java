/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.Incubating;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;

/// Optional legacy mutation support for an [AuxiliaryMapping].
///
/// This contract isolates auxiliary mapping participation in legacy mutation
/// group construction from the mapping/query-facing [AuxiliaryMapping] surface.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public interface LegacyAuxiliaryMutationSupport {
	/// Adds auxiliary columns to the legacy entity insert mutation group.
	void addToInsertGroup(MutationGroupBuilder insertGroupBuilder, EntityPersister persister);
}
