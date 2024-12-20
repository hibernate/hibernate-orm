/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;

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
