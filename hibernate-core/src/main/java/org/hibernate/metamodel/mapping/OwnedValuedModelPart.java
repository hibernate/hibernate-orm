/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

/**
 * Marker interface for valued model parts that have a declaring/owner type.
 */
public interface OwnedValuedModelPart extends ValuedModelPart {
	MappingType getDeclaringType();
}
