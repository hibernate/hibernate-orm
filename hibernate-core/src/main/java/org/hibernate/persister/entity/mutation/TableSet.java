/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import java.util.Arrays;
import java.util.BitSet;

import org.hibernate.sql.model.TableMapping;

/**
 * Represents a Set of TableMapping(s); table mappings are
 * identified by an ordered unique id: the order in which
 * they are updated within the scope of a particular persister.
 * This makes it possible to store a set of them as a bitset,
 * which is typically more efficient than using a {@link java.util.Set}.
 * These table ids come from {@link org.hibernate.sql.model.TableMapping#getRelativePosition}.
 * <p>N.B. Make sure to not store TableMappings from different
 * persisters, as their unique identifiers will overlap:
 * we'll only verify a mismatch if assertions are enabled.</p>
 */
public final class TableSet {

	private BitSet bits;
	private Object[] checks; //Meant for assertions only

	public void add(final TableMapping tableMapping) {
		if ( bits == null ) {
			bits = new BitSet();
		}
		assert addForChecks( tableMapping );
		bits.set( tableMapping.getRelativePosition() );
	}

	public boolean isEmpty() {
		return bits == null;
	}

	public boolean contains(final TableMapping tableMapping) {
		assert matchRead( tableMapping );
		return bits != null && bits.get( tableMapping.getRelativePosition() );
	}

	//Meant for assertions only
	private boolean matchRead(final TableMapping tableMapping) {
		if ( bits != null ) {
			final int index = tableMapping.getRelativePosition();
			if ( bits.get( index ) ) {
				return checks[index] == tableMapping;
			}
		}
		return true; //to make the assertion happy
	}

	//Meant for assertions only
	private boolean addForChecks(final TableMapping tableMapping) {
		final int position = tableMapping.getRelativePosition();
		ensureCapacity( position );
		if ( checks[position] != null ) {
			//pre-existing in the set: verify it's the same one.
			if ( checks[position] != tableMapping ) {
				return false;//fail the assertion
			}
		}
		checks[position] = tableMapping;
		return true; //to make the assertion happy
	}

	//Meant for assertions only
	private void ensureCapacity(final int position) {
		final int increments = 3; //Needs to be at least 1.
		if ( checks == null ) {
			checks = new Object[position + increments];
		}
		else if ( checks.length <= position ) {
			checks = Arrays.copyOf( checks, position + increments );
		}
	}

}
