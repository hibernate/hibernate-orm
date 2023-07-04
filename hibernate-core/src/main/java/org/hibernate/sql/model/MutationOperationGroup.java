/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Group of {@link MutationOperation} references for a specific
 * logical operation (target + type)
 *
 * @author Steve Ebersole
 */
public interface MutationOperationGroup {

	/**
	 * The type of mutation (at the model-level) represented by this group.
	 */
	MutationType getMutationType();

	/**
	 * The model-part being mutated
	 */
	MutationTarget<?> getMutationTarget();

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
	 * @deprecated Will be removed - use a combination of {@link #getNumberOfOperations()} and {@link #getOperation(int)}
	 * to iterate the list of operations.
	 */
	@Deprecated(forRemoval = true)
	List<MutationOperation> getOperations();

	/**
	 * Visit each operation
	 * @deprecated Will be removed - use a combination of {@link #getNumberOfOperations()} and {@link #getOperation(int)}
	 * to iterate the list of operations.
	 */
	@Deprecated(forRemoval = true)
	void forEachOperation(BiConsumer<Integer, MutationOperation> action);

	/**
	 * Test whether any operations match the condition
	 * @deprecated Will be removed - use a combination of {@link #getNumberOfOperations()} and {@link #getOperation(int)}
	 * to iterate the list of operations.
	 */
	@Deprecated(forRemoval = true)
	boolean hasMatching(BiFunction<Integer, MutationOperation, Boolean> matcher);

}
