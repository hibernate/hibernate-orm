/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.uuid;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerationStrategy;

/**
 * Implements a "random" UUID generation strategy as defined by the {@link UUID#randomUUID()} method.
 *
 * @author Steve Ebersole
 */
public class StandardRandomStrategy implements UUIDGenerationStrategy, UuidValueGenerator {
	public static final StandardRandomStrategy INSTANCE = new StandardRandomStrategy();

	/**
	 * A variant 4 (random) strategy
	 */
	@Override
	public int getGeneratedVersion() {
		// a "random" strategy
		return 4;
	}

	/**
	 * Delegates to {@link UUID#randomUUID()}
	 */
	@Override
	public UUID generateUUID(SharedSessionContractImplementor session) {
		return generateUuid( session );
	}

	@Override
	public UUID generateUuid(SharedSessionContractImplementor session) {
		return UUID.randomUUID();
	}
}
