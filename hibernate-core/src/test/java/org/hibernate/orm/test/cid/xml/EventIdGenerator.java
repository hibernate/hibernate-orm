/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cid.xml;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * Simple {@link IdentifierGenerator} that produces a composite {@link EventId}.
 */
public class EventIdGenerator implements IdentifierGenerator {

	private static int next = 1;

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner) {
		final int value = next++;
		return new EventId( 100 + value, value );
	}
}
