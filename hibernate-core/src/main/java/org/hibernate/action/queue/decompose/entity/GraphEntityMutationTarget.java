/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import java.util.function.Function;

import org.hibernate.Incubating;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.exec.JdbcValueBindings;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.model.GraphMutationTarget;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;

/**
 * Entity-specific mutation target for the graph-based action queue.
 * <p>
 * Provides entity table information as {@link EntityTableDescriptor} instances,
 * used by graph-based action decomposers for planning mutation execution.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface GraphEntityMutationTarget extends GraphMutationTarget<EntityTableDescriptor> {

	@Override
	EntityMappingType getTargetPart();

	@Override
	EntityTableDescriptor getIdentifierTableDescriptor();

	/**
	 * All table descriptors for this entity
	 */
	EntityTableDescriptor[] getTableDescriptors();

	/**
	 * The ModelPart describing the identifier/key for this target
	 */
	ModelPart getIdentifierDescriptor();

	/**
	 * The decomposer for INSERT actions
	 */
	EntityActionDecomposer<AbstractEntityInsertAction> getInsertDecomposer();

	/**
	 * The decomposer for UPDATE actions
	 */
	EntityActionDecomposer<EntityUpdateAction> getUpdateDecomposer();

	/**
	 * The decomposer for DELETE actions
	 */
	EntityActionDecomposer<EntityDeleteAction> getDeleteDecomposer();

	/**
	 * Add discriminator column to the insert operation builder
	 */
	default void addDiscriminatorToInsertGroup(Function<String, TableInsertBuilder> insertGroupBuilder) {
	}

	/**
	 * Bind discriminator value for insert
	 */
	default void bindDiscriminatorForInsert(JdbcValueBindings jdbcValueBindings) {
	}

	/**
	 * Add soft-delete column to the insert operation builder
	 */
	void addSoftDeleteToInsertGroup(Function<String, TableInsertBuilder> insertGroupBuilder);
}
