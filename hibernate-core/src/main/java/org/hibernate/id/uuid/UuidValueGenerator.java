/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.uuid;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Represents a specific algorithm for producing UUID values.
 * <p>
 * Used in conjunction with {@link UuidGenerator} and
 * {@link org.hibernate.annotations.UuidGenerator @UuidGenerator}.
 *
 * @author Steve Ebersole
 */
public interface UuidValueGenerator {
	/**
	 * Generate the UUID value
	 */
	UUID generateUuid(SharedSessionContractImplementor session);
}
