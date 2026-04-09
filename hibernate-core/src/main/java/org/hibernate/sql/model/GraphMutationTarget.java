/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model;

import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * Mutation target contract for the graph-based action queue.
 * <p>
 * Exposes table information as {@link TableDescriptor} instances, used by
 * graph-based decomposers for planning mutation operations.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface GraphMutationTarget<TD extends TableDescriptor> {
	/**
	 * The model role of this target
	 */
	NavigableRole getNavigableRole();

	/**
	 * The string representation of the role path
	 */
	String getRolePath();

	/**
	 * The ModelPart describing the mutation target
	 */
	ModelPartContainer getTargetPart();

	/**
	 * Visit each mutable (non-inverse) table descriptor.
	 *
	 * @apiNote Inverse tables are excluded here - they are not mutable
	 * 		relative to this mapping
	 */
	void forEachMutableTableDescriptor(Consumer<TD> consumer);

	/**
	 * Visit each mutable (non-inverse) table descriptor, but in reverse (delete) order.
	 *
	 * @apiNote Inverse tables are excluded here - they are not mutable
	 * 		relative to this mapping
	 */
	void forEachMutableTableDescriptorReverse(Consumer<TD> consumer);

	/**
	 * The name of the table defining the identifier for this target
	 */
	String getIdentifierTableName();

	/**
	 * The table descriptor for the table containing the identifier
	 */
	TD getIdentifierTableDescriptor();
}
