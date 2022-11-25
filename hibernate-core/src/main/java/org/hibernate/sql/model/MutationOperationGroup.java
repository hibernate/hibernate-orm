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
	<O extends MutationOperation> O getSingleOperation();

	<O extends MutationOperation> List<O> getOperations();

	/**
	 * Get the operation for a specific table.
	 */
	<O extends MutationOperation> O getOperation(String tableName);

	/**
	 * Visit each operation
	 */
	<O extends MutationOperation> void forEachOperation(BiConsumer<Integer, O> action);

	/**
	 * Test whether any operations match the condition
	 */
	<O extends MutationOperation> boolean hasMatching(BiFunction<Integer, O, Boolean> matcher);
}
