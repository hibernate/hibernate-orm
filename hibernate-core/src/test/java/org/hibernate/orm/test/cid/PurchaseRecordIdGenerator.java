/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cid;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * Simple {@link IdentifierGenerator} implementation for testing composite-id.
 *
 * @author Jacob Robertson
 */
public class PurchaseRecordIdGenerator implements IdentifierGenerator {

	private static int nextPurchaseNumber = 2;
	private static int nextPurchaseSequence = 3;

	@Override
	public Object generate(SharedSessionContractImplementor s, Object obj) {
		return new PurchaseRecord.Id(
				nextPurchaseNumber++,
				String.valueOf(nextPurchaseSequence++));
	}

}
