/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic.bitset;

import java.io.Serializable;
import java.util.BitSet;

import org.hibernate.SharedSessionContract;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * A BitSet's internal state is mutable, so handle that
 */
public class BitSetMutabilityPlan implements MutabilityPlan<BitSet> {
	/**
	 * Singleton access
	 */
	public static final BitSetMutabilityPlan INSTANCE = new BitSetMutabilityPlan();

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public BitSet deepCopy(BitSet value) {
		return BitSet.valueOf(value.toByteArray());
	}

	@Override
	public Serializable disassemble(BitSet value, SharedSessionContract session) {
		return value.toByteArray();
	}

	@Override
	public BitSet assemble(Serializable cached, SharedSessionContract session) {
		return BitSet.valueOf((byte[]) cached);
	}
}
