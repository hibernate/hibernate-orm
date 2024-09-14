/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
