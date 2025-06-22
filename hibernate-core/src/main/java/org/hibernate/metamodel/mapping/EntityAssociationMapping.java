/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import java.util.Set;

import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;

/**
 * Commonality between `many-to-one`, `one-to-one` and `any`, as well as entity-valued collection elements and map-keys
 *
 * @author Steve Ebersole
 */
public interface EntityAssociationMapping extends ModelPart, Association, TableGroupJoinProducer {
	@Override
	default String getFetchableName() {
		return getPartName();
	}

	EntityMappingType getAssociatedEntityMappingType();

	Set<String> getTargetKeyPropertyNames();

	/**
	 * The model sub-part relative to the associated entity type that is the target
	 * of this association's foreign-key
	 */
	ModelPart getKeyTargetMatchPart();

	boolean isReferenceToPrimaryKey();

	boolean isFkOptimizationAllowed();

	@Override
	default boolean incrementFetchDepth(){
		return true;
	}
}
