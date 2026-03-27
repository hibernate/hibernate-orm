/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;

import java.util.function.Function;

/**
 * @author Steve Ebersole
 */
public interface EntityGraphMutationTarget extends GraphMutationTarget<EntityTableDescriptor> {
	@Override
	EntityMappingType getTargetPart();

	/**
	 * Get all table descriptors.
	 */
	EntityTableDescriptor[] getTableDescriptors();

	EntityTableDescriptor getIdentifierTableDescriptor();

	void addDiscriminatorToInsertGroup(Function<String, TableInsertBuilder> insertGroupBuilder);
	void addSoftDeleteToInsertGroup(Function<String, TableInsertBuilder> insertGroupBuilder);
}
