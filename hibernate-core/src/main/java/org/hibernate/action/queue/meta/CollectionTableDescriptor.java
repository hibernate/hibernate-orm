/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.meta;

import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.sql.model.TableMapping.MutationDetails;

import java.io.Serializable;

/// TableDescriptor for collection tables.
///
/// @author Steve Ebersole
public record CollectionTableDescriptor(
		String name,
		NavigableRole navigableRole,
		boolean isJoinTable,
		boolean isInverse,
		boolean isSelfReferential,
		boolean cascadeDeleteEnabled,
		MutationDetails insertDetails,
		MutationDetails updateDetails,
		MutationDetails deleteDetails,
		MutationDetails deleteAllDetails,
		TableKeyDescriptor keyDescriptor) implements TableDescriptor, Serializable {
	@Override
	public boolean isOptional() {
		return false;
	}

	@Override
	public int getRelativePosition() {
		return 0;
	}
}
