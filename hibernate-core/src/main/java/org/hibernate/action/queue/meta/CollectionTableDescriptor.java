/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.meta;

import org.hibernate.sql.model.TableMapping.MutationDetails;

/**
 * @author Steve Ebersole
 */
public record CollectionTableDescriptor(
		String normalizedName,
		String physicalName,
		boolean isJoinTable,
		boolean isInverse,
		boolean cascadeDeleteEnabled,
		MutationDetails insertDetails,
		MutationDetails updateDetails,
		MutationDetails deleteDetails,
		MutationDetails deleteAllDetails,
		TableKeyDescriptor keyDescriptor) implements TableDescriptor {
	@Override
	public boolean isOptional() {
		return false;
	}
}
