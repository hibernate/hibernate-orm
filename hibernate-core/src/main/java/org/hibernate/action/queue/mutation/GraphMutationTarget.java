/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.model.domain.NavigableRole;

import java.util.function.Consumer;

/// Target of mutations from persistence context events.
///
/// @author Steve Ebersole
@Incubating
public interface GraphMutationTarget<T extends TableDescriptor> {
	/**
	 * The ModelPart describing the mutation target
	 */
	ModelPartContainer getTargetPart();

	/**
	 * The model role of this target
	 */
	NavigableRole getNavigableRole();

	default String getRolePath() {
		return getNavigableRole().getFullPath();
	}

	/**
	 * Visit each mutable (non-inverse) table.
	 */
	void forEachMutableTableDescriptor(Consumer<T> consumer);

	/**
	 * Visit each mutable (non-inverse) table, but in reverse (delete) order.
	 */
	void forEachMutableTableDescriptorReverse(Consumer<T> consumer);
}
