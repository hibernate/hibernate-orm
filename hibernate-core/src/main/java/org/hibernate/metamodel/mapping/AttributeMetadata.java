/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * @author Steve Ebersole
 */
public interface AttributeMetadata {
	PropertyAccess getPropertyAccess();

	MutabilityPlan getMutabilityPlan();

	boolean isNullable();

	boolean isInsertable();

	boolean isUpdatable();

	boolean isSelectable();

	boolean isIncludedInDirtyChecking();

	boolean isIncludedInOptimisticLocking();

	default CascadeStyle getCascadeStyle() {
		// todo (6.0) - implement in each subclass.
		//		For now return a default NONE value for all contributors since this isn't
		//		to be supported as a part of Alpha1.
		return CascadeStyles.NONE;
	}
}
