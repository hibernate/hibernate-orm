/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.SPI;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationType;

import static org.hibernate.SPI.Role.USE;

/**
 * Group of {@link MutationOperation} references for a specific
 * logical operation (target + type)
 *
 * @author Steve Ebersole
 *
 * @deprecated This contract is used only by the legacy action queue. The graph-based
 * action queue decomposes mutations into individual flush operations.
 */
@Deprecated(since = "8.0", forRemoval = true)
@SPI(USE)
public interface MutationOperationGroup {

	/**
	 * The type of mutation (at the model-level) represented by this group.
	 */
	MutationType getMutationType();

	/**
	 * The model-part being mutated
	 */
	org.hibernate.sql.spi.mutation.MutationTarget getMutationTarget();

	/**
	 * Number of operations in this group
	 */
	int getNumberOfOperations();

	/**
	 * Get the singular operation, assuming there is just one.
	 *
	 * Throws an exception if there are more than one.
	 */
	MutationOperation getSingleOperation();

	/**
	 * Gets a specific MutationOperation from the group
	 * @param idx the index, starting from zero.
	 * @return
	 */
	MutationOperation getOperation(int idx);

	/**
	 * Get the operation for a specific table.
	 */
	MutationOperation getOperation(String tableName);

	/**
	 * Attempt to cast to the frequently uses subtype EntityMutationOperationGroup;
	 * returns null if this is not possible.
	 * @return
	 */
	default EntityMutationOperationGroup asEntityMutationOperationGroup() {
		return null;
	}

	/**
	 * @deprecated Will be removed. Use the other methods to visit each operation.
	 */
	@Deprecated(forRemoval = true)
	default <O extends MutationOperation> void forEachOperation(BiConsumer<Integer, O> action) {
		for ( int i = 0; i < getNumberOfOperations(); i++ ) {
			action.accept( i, (O) getOperation( i ) );
		}
	}

	/**
	 * @deprecated Will be removed. Use the other methods to visit each operation.
	 */
	@Deprecated(forRemoval = true)
	default <O extends MutationOperation> boolean hasMatching(BiFunction<Integer, O, Boolean> matcher) {
		for ( int i = 0; i < getNumberOfOperations(); i++ ) {
			if ( matcher.apply( i, (O) getOperation( i ) ) ) {
				return true;
			}
		}
		return false;
	}

}
