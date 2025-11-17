/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid.annotation;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.uuid.UuidValueGenerator;

/**
 * @author Steve Ebersole
 */
public class CustomUuidValueGenerator implements UuidValueGenerator {
	private long counter = 0;

	@Override
	public UUID generateUuid(SharedSessionContractImplementor session) {
		final UUID sessionIdentifier = session.getSessionIdentifier();
		return new UUID( sessionIdentifier.getMostSignificantBits(), ++counter );
	}
}
