/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.uuid;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Implements a "random" UUID generation strategy as defined by the {@link UUID#randomUUID()} method.
 *
 * @author Steve Ebersole
 */
public class StandardRandomStrategy implements UuidValueGenerator {
	public static final StandardRandomStrategy INSTANCE = new StandardRandomStrategy();

	@Override
	public UUID generateUuid(SharedSessionContractImplementor session) {
		return UUID.randomUUID();
	}
}
