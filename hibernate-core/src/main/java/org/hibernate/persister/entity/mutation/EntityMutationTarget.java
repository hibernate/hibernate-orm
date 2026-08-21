/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;

/**
 * Anything that can be the target of {@linkplain MutationExecutor mutations}
 *
 * @author Steve Ebersole
 */
@Incubating
public interface EntityMutationTarget extends MutationTarget<EntityTableMapping> {

	@Override
	EntityMappingType getTargetPart();

	@Override
	EntityTableMapping getIdentifierTableMapping();

	@Internal
	EntityTableMapping[] getTableMappings();

	/**
	 * The ModelPart describing the identifier/key for this target
	 */
	ModelPart getIdentifierDescriptor();

	String physicalTableNameForMutation(SelectableMapping selectableMapping);

	void addDiscriminatorToInsertGroup(MutationGroupBuilder insertGroupBuilder);

	void addSoftDeleteToInsertGroup(MutationGroupBuilder insertGroupBuilder);

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
	@Deprecated( forRemoval = true, since = "6.5" )
	default InsertGeneratedIdentifierDelegate getIdentityInsertDelegate() {
		final GeneratedValuesMutationDelegate insertDelegate = getInsertDelegate();
		return insertDelegate instanceof InsertGeneratedIdentifierDelegate insertGeneratedIdentifierDelegate
				? insertGeneratedIdentifierDelegate
				: null;
	}

	GeneratedValuesMutationDelegate getInsertDelegate();

	GeneratedValuesMutationDelegate getUpdateDelegate();

	default GeneratedValuesMutationDelegate getMutationDelegate(MutationType mutationType) {
		return switch ( mutationType ) {
			case INSERT -> getInsertDelegate();
			case UPDATE -> getUpdateDelegate();
			default -> null;
		};
	}
}
