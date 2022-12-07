/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi.entry;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.type.Type;

import java.io.Serializable;

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
	public static Object[] disassemble(
			final Object[] row,
			final Type[] types,
			final boolean[] nonCacheable,
			final SharedSessionContractImplementor session,
			final Object owner) {
		Object[] disassembled = new Object[row.length];
		for ( int i = 0; i < row.length; i++ ) {
			if ( nonCacheable!=null && nonCacheable[i] ) {
				disassembled[i] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			}
			else if ( row[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY 
					| row[i] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
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
			final Object[] row,
			final Type[] types,
			final SharedSessionContractImplementor session,
			final Object owner) {
		Object[] assembled = new Object[row.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( row[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY 
					|| row[i] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				assembled[i] = row[i];
			}
			else {
				assembled[i] = types[i].assemble( row[i], session, owner );
			}
		}
		return assembled;
	}

//	public static Object[] assemble(
//			final Object[] row,
//			final Type[] types,
//			final SharedSessionContractImplementor session,
//			final Object owner) {
//		Object[] assembled = new Object[row.length];
//		for ( int i = 0; i < types.length; i++ ) {
//			if ( row[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY || row[i] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
//				assembled[i] = row[i];
//			}
//			else {
//				assembled[i] = types[i].assemble( (Serializable) row[i], session, owner );
//			}
//		}
//		return assembled;
//	}

}
