/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * The tenant attribute and generated tenant components of an entity identifier.
 * Resolves embedded attribute paths once, when the entity mapping is built.
 */
public interface TenantIdMapping {
	/**
	 * The tenant attribute outside the identifier, or {@code null} if there is none.
	 */
	AttributeMapping getAttributeMapping();

	Object getTenantIdFromIdentifier(Object id, SharedSessionContractImplementor session);

	boolean hasUnassignedIdentifierTenant(Object id, SharedSessionContractImplementor session);

	void validateIdentifier(Object id, SharedSessionContractImplementor session);

	void validateAssignedValue(Object entity, Object id, SharedSessionContractImplementor session);

	void initializeIdentifier(Object entity, SharedSessionContractImplementor session);

	void initialize(Object entity, Object[] state, SharedSessionContractImplementor session);
}
