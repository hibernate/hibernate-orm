/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.mutation.internal;

import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.MutationOperationGroup;

public interface EntityMutationOperationGroup extends MutationOperationGroup {

	/**
	 * The model-part being mutated.
	 * N.B. it returns a widened type compared to the same method in the super interface.
	 */
	@Override
	EntityMutationTarget getMutationTarget();

	@Override
	default EntityMutationOperationGroup asEntityMutationOperationGroup() {
		return this;
	}

}
