/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Coordinates the updating of an entity.
 *
 * @author Steve Ebersole
 * @see #update
 */
public interface UpdateCoordinator extends MutationCoordinator {
	/**
	 * Update a persistent instance.
	 *
	 * @return The {@linkplain GeneratedValues generated values} if any, {@code null} otherwise.
	 */
	@Nullable GeneratedValues update(
			Object entity,
			Object id,
			Object rowId,
			Object[] values,
			Object oldVersion,
			Object[] incomingOldValues,
			int[] dirtyAttributeIndexes,
			boolean hasDirtyCollection,
			SharedSessionContractImplementor session);

	void forceVersionIncrement(
			Object id,
			Object currentVersion,
			Object nextVersion,
			SharedSessionContractImplementor session);

	default void forceVersionIncrement(
			Object id,
			Object currentVersion,
			Object nextVersion,
			boolean batching,
			SharedSessionContractImplementor session) {
		forceVersionIncrement( id, currentVersion, nextVersion, session );
	}
}
