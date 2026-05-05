/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.LegacyMutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;

/**
 * Entity-specific mutation target for the legacy (sequential) action queue.
 * <p>
 * Provides entity table information as {@link EntityTableMapping} instances,
 * used by legacy mutation coordinators.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface LegacyEntityMutationTarget extends LegacyMutationTarget<EntityTableMapping> {

	@Override
	EntityMappingType getTargetPart();

	@Override
	EntityTableMapping getIdentifierTableMapping();

	/**
	 * All table mappings for this entity
	 */
	@Internal
	EntityTableMapping[] getTableMappings();

	/**
	 * The ModelPart describing the identifier/key for this target
	 */
	ModelPart getIdentifierDescriptor();

	/**
	 * The physical table name to use when mutating the given selectable
	 */
	String physicalTableNameForMutation(SelectableMapping selectableMapping);

	/**
	 * Add discriminator column to the insert group builder
	 *
	 * @deprecated Used by legacy action queue processes.
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	void addDiscriminatorToInsertGroup(MutationGroupBuilder insertGroupBuilder);

	/**
	 * Add auxiliary columns to the insert group builder
	 */
	void addAuxiliaryToInsertGroup(MutationGroupBuilder insertGroupBuilder);

	/**
	 * The name of the table to use when performing mutations (INSERT,UPDATE,DELETE)
	 * for the given attribute
	 */
	String getAttributeMutationTableName(int i);

	/**
	 * The delegate for executing inserts against the root table for
	 * targets defined using post-insert id generation
	 *
	 * @deprecated use {@link #getInsertDelegate()} instead
	 */
	@Deprecated(forRemoval = true, since = "6.5")
	default InsertGeneratedIdentifierDelegate getIdentityInsertDelegate() {
		final GeneratedValuesMutationDelegate insertDelegate = getInsertDelegate();
		return insertDelegate instanceof InsertGeneratedIdentifierDelegate insertGeneratedIdentifierDelegate
				? insertGeneratedIdentifierDelegate
				: null;
	}

	/**
	 * The delegate for insert-generated values
	 */
	GeneratedValuesMutationDelegate getInsertDelegate();

	/**
	 * The delegate for update-generated values
	 */
	GeneratedValuesMutationDelegate getUpdateDelegate();

	/**
	 * Get the mutation delegate for the given mutation type
	 */
	default GeneratedValuesMutationDelegate getMutationDelegate(MutationType mutationType) {
		return switch (mutationType) {
			case INSERT -> getInsertDelegate();
			case UPDATE -> getUpdateDelegate();
			default -> null;
		};
	}
}
