/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model;

import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;

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

	default GeneratedValuesMutationDelegate getMutationDelegate() {
		return getMutationTarget().getMutationDelegate( getMutationType() );
	}
}
