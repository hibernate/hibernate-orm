/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.entry;

import java.io.Serializable;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.type.Type;

/**
 * Operations for assembly and disassembly of an array of property values.
 */
class CacheEntryHelper {

	/**
	 * Apply the {@link Type#disassemble} operation across a series of values.
	 *
	 * @param row The values
	 * @param types The value types
	 * @param nonCacheable An array indicating which values to include in the disassembled state
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 *
	 * @return The disassembled state
	 */
	public static Serializable[] disassemble(
			final Object[] row,
			final Type[] types,
			final boolean[] nonCacheable,
			final SharedSessionContractImplementor session,
			final Object owner) {
		final Serializable[] disassembled = new Serializable[types.length];
		for ( int i = 0; i < row.length; i++ ) {
			if ( nonCacheable!=null && nonCacheable[i] ) {
				disassembled[i] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			}
			else if ( isPlaceholder( row[i] ) ) {
				disassembled[i] = (Serializable) row[i];
			}
			else {
				disassembled[i] = types[i].disassemble( row[i], session, owner );
			}
		}
		return disassembled;
	}

	/**
	 * Apply the {@link Type#assemble} operation across a series of values.
	 *
	 * @param row The values
	 * @param types The value types
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 * @return The assembled state
	 */
	public static Object[] assemble(
			final Serializable[] row,
			final Type[] types,
			final SharedSessionContractImplementor session,
			final Object owner) {
		final Object[] assembled = new Object[row.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( isPlaceholder( row[i] ) ) {
				assembled[i] = row[i];
			}
			else {
				assembled[i] = types[i].assemble( row[i], session, owner );
			}
		}
		return assembled;
	}

	private static boolean isPlaceholder(Object value) {
		return value == LazyPropertyInitializer.UNFETCHED_PROPERTY
			|| value == PropertyAccessStrategyBackRefImpl.UNKNOWN;
	}

}
