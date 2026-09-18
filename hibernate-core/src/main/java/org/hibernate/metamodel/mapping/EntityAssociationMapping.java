/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

import java.util.Set;

import org.hibernate.sql.ast.spi.query.from.TableGroupJoinProducer;

/**
 * Commonality between `many-to-one`, `one-to-one` and `any`, as well as entity-valued collection elements and map-keys
 *
 * @author Steve Ebersole
 */
public interface EntityAssociationMapping extends ModelPart, Association, TableGroupJoinProducer {
	@Nonnull
	@Override
	default String getFetchableName() {
		return org.hibernate.internal.util.NullnessUtil.castNonNull( getPartName() );
	}

	@Nonnull
	EntityMappingType getAssociatedEntityMappingType();

	@Nonnull
	Set<String> getTargetKeyPropertyNames();

	/**
	 * The model sub-part relative to the associated entity type that is the target
	 * of this association's foreign-key
	 */
	@Nonnull
	ModelPart getKeyTargetMatchPart();

	boolean isReferenceToPrimaryKey();

	boolean isFkOptimizationAllowed();

	@Override
	default boolean incrementFetchDepth(){
		return true;
	}
}
