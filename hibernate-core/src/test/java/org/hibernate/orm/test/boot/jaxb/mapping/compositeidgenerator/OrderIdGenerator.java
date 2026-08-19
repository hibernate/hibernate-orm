/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.compositeidgenerator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class OrderIdGenerator implements IdentifierGenerator {

	private static int next = 1;

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner) {
		final Order.Id id = new Order.Id();
		id.setNumber( next );
		id.setCode( "ORD-" + next++ );
		return id;
	}
}
