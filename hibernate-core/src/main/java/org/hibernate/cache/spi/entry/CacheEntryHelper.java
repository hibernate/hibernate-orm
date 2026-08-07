/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.entry;

import java.io.Serializable;

import jakarta.annotation.Nullable;
import org.hibernate.Hibernate;
import org.hibernate.Internal;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.type.Type;

/**
 * Operations for assembly and disassembly of an array of property values.
 */
@Internal
public class CacheEntryHelper {

	/**
	 * Apply the {@link Type#disassemble} operation across a series of values,
	 * optionally resolving {@link LazyPropertyInitializer#UNFETCHED_PROPERTY}
	 * placeholders from the entity when the persister indicates that lazy
	 * properties should be cached.
	 *
	 * @param row The values (may contain UNFETCHED_PROPERTY for lazy fields)
	 * @param types The value types
	 * @param nonCacheable An array indicating which values to exclude from caching
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 * @param persister The entity persister, used to resolve lazy values from the entity;
	 *                  if null, UNFETCHED_PROPERTY placeholders are preserved as-is
	 *
	 * @return The disassembled state
	 */
	static Serializable[] disassemble(
			final Object[] row,
			final Type[] types,
			final boolean[] nonCacheable,
			final SharedSessionContractImplementor session,
			final Object owner,
			@Nullable final EntityPersister persister) {
		final Serializable[] disassembled = new Serializable[types.length];
		for ( int i = 0; i < row.length; i++ ) {
			if ( nonCacheable!=null && nonCacheable[i] ) {
				disassembled[i] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			}
			else if ( isPlaceholder( row[i] ) ) {
				if ( persister != null
						&& persister.isLazyPropertiesCacheable()
						&& row[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
					// The state array contains UNFETCHED_PROPERTY for a lazy field,
					// but lazy properties should be cached. Check if the field is
					// actually initialized on the entity (e.g., the application set
					// the value via constructor or setter during em.merge/persist)
					// and if so, read the actual value for caching. Only do this
					// when the interceptor reports the attribute as loaded — do NOT
					// trigger lazy loading here. (HHH-20773)
					final String propertyName = persister.getAttributeMapping( i ).getAttributeName();
					if ( Hibernate.isPropertyInitialized( owner, propertyName ) ) {
						final Object resolved = persister.getValue( owner, i );
						if ( !isPlaceholder( resolved ) ) {
							// The field is confirmed loaded by the interceptor.
							// Cache the actual value — including null, which is
							// a legitimate column value (HHH-20773).
							disassembled[i] = types[i].disassemble( resolved, session, owner );
						}
						else {
							disassembled[i] = (Serializable) row[i];
						}
					}
					else {
						disassembled[i] = (Serializable) row[i];
					}
				}
				else {
					disassembled[i] = (Serializable) row[i];
				}
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
	static Object[] assemble(
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

	public static Object buildStructuredCacheEntry(
			Object entity,
			Object version,
			Object[] state,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		return persister.getCacheEntryStructure()
				.structure( persister.buildCacheEntry( entity, state, version, session ) );
	}
}
