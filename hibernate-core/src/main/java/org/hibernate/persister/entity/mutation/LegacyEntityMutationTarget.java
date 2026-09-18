/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.LegacyMutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.ast.spi.model.builder.MutationGroupBuilder;

/**
 * Entity-specific mutation target for the legacy (sequential) action queue.
 * <p>
 * Provides entity table information as {@link EntityTableMapping} instances,
 * used by legacy mutation coordinators.
 *
 * @author Steve Ebersole
 */
@Incubating(since = "6.2")
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface LegacyEntityMutationTarget extends LegacyMutationTarget<EntityTableMapping> {

	@Nonnull
	@Override
	EntityMappingType getTargetPart();

	@Nonnull
	@Override
	EntityTableMapping getIdentifierTableMapping();

	/**
	 * All table mappings for this entity
	 */
	@Nonnull
	@Internal
	EntityTableMapping[] getTableMappings();

	/**
	 * The ModelPart describing the identifier/key for this target
	 */
	@Nonnull
	ModelPart getIdentifierDescriptor();

	/**
	 * The physical table name to use when mutating the given selectable
	 */
	@Nonnull
	String physicalTableNameForMutation(@Nonnull SelectableMapping selectableMapping);

	/**
	 * Add discriminator column to the insert group builder
	 *
	 * @deprecated Used by legacy action queue processes.
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	void addDiscriminatorToInsertGroup(@Nonnull MutationGroupBuilder insertGroupBuilder);

	/**
	 * Add auxiliary columns to the insert group builder
	 */
	void addAuxiliaryToInsertGroup(@Nonnull MutationGroupBuilder insertGroupBuilder);

	/**
	 * The name of the table to use when performing mutations (INSERT,UPDATE,DELETE)
	 * for the given attribute
	 */
	@Nonnull
	String getAttributeMutationTableName(int i);

	/**
	 * The delegate for executing inserts against the root table for
	 * targets defined using post-insert id generation
	 *
	 * @deprecated use {@link #getInsertDelegate()} instead
	 */
	@Nullable
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
	@Nullable
	GeneratedValuesMutationDelegate getInsertDelegate();

	/**
	 * The delegate for update-generated values
	 */
	@Nullable
	GeneratedValuesMutationDelegate getUpdateDelegate();

	/**
	 * Get the mutation delegate for the given mutation type
	 */
	@Nullable
	default GeneratedValuesMutationDelegate getMutationDelegate(@Nonnull MutationType mutationType) {
		return switch (mutationType) {
			case INSERT -> getInsertDelegate();
			case UPDATE -> getUpdateDelegate();
			default -> null;
		};
	}
}
