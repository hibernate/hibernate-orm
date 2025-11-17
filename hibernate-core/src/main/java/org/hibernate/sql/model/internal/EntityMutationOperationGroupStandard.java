/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.internal;

import java.util.Locale;

import org.hibernate.sql.model.EntityMutationOperationGroup;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationType;

public class EntityMutationOperationGroupStandard implements EntityMutationOperationGroup {

	private static final MutationOperation[] EMPTY = new MutationOperation[0];

	private final MutationType mutationType;
	private final EntityMutationTarget mutationTarget;
	private final MutationOperation[] operations;

	/**
	 * Intentionally package private: use {@link MutationOperationGroupFactory}.
	 * Constructor for when there are no operations.
	 * @param mutationType
	 * @param mutationTarget
	 */
	EntityMutationOperationGroupStandard(MutationType mutationType, EntityMutationTarget mutationTarget) {
		this( mutationType, mutationTarget, EMPTY );
	}

	/**
	 * Intentionally package private: use {@link MutationOperationGroupFactory}.
	 * Constructor for when there's a single operation.
	 * @param mutationType
	 * @param mutationTarget
	 * @param operation
	 */
	EntityMutationOperationGroupStandard(MutationType mutationType, EntityMutationTarget mutationTarget, MutationOperation operation) {
		this( mutationType, mutationTarget, new MutationOperation[]{ operation } );
	}

	/**
	 * Intentionally package private: use {@link MutationOperationGroupFactory}.
	 * Constructor for when there's multiple operations.
	 * @param mutationType
	 * @param mutationTarget
	 * @param operations
	 */
	EntityMutationOperationGroupStandard(MutationType mutationType, EntityMutationTarget mutationTarget, MutationOperation[] operations) {
		this.mutationType = mutationType;
		this.mutationTarget = mutationTarget;
		this.operations = operations;
	}

	@Override
	public MutationType getMutationType() {
		return mutationType;
	}

	@Override
	public EntityMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public int getNumberOfOperations() {
		return operations.length;
	}

	@Override
	public MutationOperation getSingleOperation() {
		if ( operations.length == 1 ) {
			return operations[0];
		}
		else {
			throw new IllegalStateException(
					String.format(
							Locale.ROOT,
							"Group contains multiple table mutations - %s : %s ",
							getMutationType().name(),
							getMutationTarget().getNavigableRole()
					)
			);
		}
	}

	@Override
	public MutationOperation getOperation(int idx) {
		return operations[idx];
	}

	@Override
	public MutationOperation getOperation(final String tableName) {
		for ( int i = 0; i < operations.length; i++ ) {
			final MutationOperation operation = operations[i];
			if ( operation.getTableDetails().getTableName().equals( tableName ) ) {
				return operation;
			}
		}
		return null;
	}

}
