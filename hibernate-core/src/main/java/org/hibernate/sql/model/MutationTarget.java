/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * Target of mutations from persistence context events
 *
 * @author Steve Ebersole
 */
public interface MutationTarget<T extends TableMapping> {
	/**
	 * The model role of this target
	 */
	NavigableRole getNavigableRole();

	default String getRolePath() {
		return getNavigableRole().getFullPath();
	}

	/**
	 * The ModelPart describing the mutation target
	 */
	ModelPartContainer getTargetPart();

	/**
	 * Visit each table.
	 *
	 * @apiNote Inverse tables are excluded here - they are not mutable
	 * 		relative to this mapping
	 */
	void forEachMutableTable(Consumer<T> consumer);

	/**
	 * Same as {@link #forEachMutableTable} except that here the tables
	 * are visited in reverse order
	 *
	 * @apiNote Inverse tables are excluded here - they are not mutable
	 * 		relative to this mapping
	 */
	void forEachMutableTableReverse(Consumer<T> consumer);

	/**
	 * The name of the table defining the identifier for this target
	 */
	String getIdentifierTableName();

	/**
	 * The descriptor for the table containing the identifier for the target
	 */
	TableMapping getIdentifierTableMapping();
}
