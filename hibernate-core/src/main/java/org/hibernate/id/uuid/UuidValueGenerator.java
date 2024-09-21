/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.uuid;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Represents a specific algorithm for producing UUID values.  Used in
 * conjunction with {@linkplain UuidGenerator} and
 *
 * @author Steve Ebersole
 */
public interface UuidValueGenerator {
	/**
	 * Generate the UUID value
	 */
	UUID generateUuid(SharedSessionContractImplementor session);
}
