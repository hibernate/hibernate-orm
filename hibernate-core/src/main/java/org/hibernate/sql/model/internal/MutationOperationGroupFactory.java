/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.internal;

import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.MutationGroup;

public final class MutationOperationGroupFactory {

	public static MutationOperationGroup noOperations(
			final MutationType mutationType,
			final MutationTarget mutationTarget) {
		if ( mutationTarget instanceof EntityMutationTarget entityMutationTarget ) {
			return new EntityMutationOperationGroupStandard(
					mutationType,
					entityMutationTarget
			);
		}
		else {
			return new MutationOperationGroupStandard(
					mutationType,
					mutationTarget
			);
		}
	}

	public static MutationOperationGroup noOperations(final MutationGroup mutationGroup) {
		return noOperations( mutationGroup.getMutationType(), mutationGroup.getMutationTarget() );
	}

	public static MutationOperationGroup singleOperation(
			final MutationType mutationType,
			final MutationTarget mutationTarget,
			final MutationOperation operation) {
		if ( mutationTarget instanceof EntityMutationTarget entityMutationTarget) {
			return new EntityMutationOperationGroupStandard(
					mutationType,
					entityMutationTarget,
					operation
			);
		}
		else {
			return new MutationOperationGroupStandard(
					mutationType,
					mutationTarget,
					operation
			);
		}
	}

	public static MutationOperationGroup singleOperation(final MutationGroup mutationGroup, final MutationOperation operation) {
		return singleOperation( mutationGroup.getMutationType(), mutationGroup.getMutationTarget(), operation );
	}

	public static MutationOperationGroup manyOperations(
			final MutationType mutationType,
			final MutationTarget mutationTarget,
			final MutationOperation[] operations) {
		if ( mutationTarget instanceof EntityMutationTarget entityMutationTarget ) {
			return new EntityMutationOperationGroupStandard( mutationType, entityMutationTarget, operations );
		}
		else {
			return new MutationOperationGroupStandard( mutationType, mutationTarget, operations );
		}
	}
}
