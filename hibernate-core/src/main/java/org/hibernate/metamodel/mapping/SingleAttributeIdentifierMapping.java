/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.generator.Generator;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * @author Steve Ebersole
 */
public interface SingleAttributeIdentifierMapping extends EntityIdentifierMapping, PropertyBasedMapping,
		SingularAttributeMapping, AttributeMetadata {
	/**
	 * Access to the identifier attribute's PropertyAccess
	 */
	PropertyAccess getPropertyAccess();

	String getAttributeName();

	@Override
	default String getPartName() {
		return ID_ROLE_NAME;
	}

	@Override
	default Generator getGenerator() {
		return null;
	}

	@Override
	default int getStateArrayPosition() {
		return -1;
	}

	@Override
	default AttributeMetadata getAttributeMetadata() {
		return this;
	}

	@Override
	default ManagedMappingType getDeclaringType() {
		return findContainingEntityMapping();
	}

	@Override
	default boolean isSelectable() {
		return true;
	}

	@Override
	default boolean isNullable() {
		return false;
	}

	@Override
	default boolean isInsertable() {
		return true;
	}

	@Override
	default boolean isUpdatable() {
		return false;
	}

	@Override
	default boolean isIncludedInDirtyChecking() {
		return false;
	}

	@Override
	default boolean isIncludedInOptimisticLocking() {
		return true;
	}

	@Override
	default MutabilityPlan getMutabilityPlan() {
		return ImmutableMutabilityPlan.instance();
	}
}
