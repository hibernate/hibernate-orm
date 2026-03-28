/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.sql.model.TableMapping;

import java.util.BitSet;


/**
 * Represents a Set of TableMapping(s); table mappings are
 * identified by an ordered unique id: the order in which
 * they are updated within the scope of a particular persister.
 * This makes it possible to store a set of them as a bitset,
 * which is typically more efficient than using a {@link java.util.Set}.
 * These table ids come from {@link TableMapping#getRelativePosition}.
 * <p>N.B. Make sure to not store TableMappings from different
 * persisters, as their unique identifiers will overlap:
 * we'll only verify a mismatch if assertions are enabled.</p>
 */
public final class TableDescriptorSet {
	private BitSet bits;

	public void add(final TableDescriptor table) {
		if ( bits == null ) {
			bits = new BitSet();
		}
		bits.set( table.getRelativePosition() );
	}

	public boolean contains(final TableDescriptor tableMapping) {
		return bits != null && bits.get( tableMapping.getRelativePosition() );
	}

	public void remove(final TableDescriptor tableMapping) {
		if ( bits != null ) {
			bits.set( tableMapping.getRelativePosition(), false );
		}
	}

	public boolean isEmpty() {
		return bits == null;
	}

}
