/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * The tenant attribute and generated tenant components of an entity identifier.
 * Resolves embedded attribute paths once, when the entity mapping is built.
 */
public interface TenantIdMapping {
	/**
	 * The tenant attribute outside the identifier, or {@code null} if there is none.
	 */
	@Nullable
	AttributeMapping getAttributeMapping();

	@Nullable
	Object getTenantIdFromIdentifier(@Nullable Object id, @Nonnull SharedSessionContractImplementor session);

	boolean hasUnassignedIdentifierTenant(@Nullable Object id, @Nonnull SharedSessionContractImplementor session);

	void validateIdentifier(@Nullable Object id, @Nonnull SharedSessionContractImplementor session);

	void validateAssignedValue(@Nonnull Object entity, @Nullable Object id, @Nonnull SharedSessionContractImplementor session);

	void initializeIdentifier(@Nonnull Object entity, @Nonnull SharedSessionContractImplementor session);

	void initialize(@Nonnull Object entity, @Nonnull Object[] state, @Nonnull SharedSessionContractImplementor session);
}
