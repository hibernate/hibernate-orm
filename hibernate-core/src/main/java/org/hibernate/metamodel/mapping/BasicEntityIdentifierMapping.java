/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

/**
 * Mapping for a simple, single-column identifier
 *
 * @author Steve Ebersole
 */
public interface BasicEntityIdentifierMapping extends SingleAttributeIdentifierMapping, BasicValuedModelPart {
	@Override
	default int getFetchableKey() {
		return -1;
	}

	@Override
	boolean isNullable();

	@Override
	boolean isInsertable();

}
