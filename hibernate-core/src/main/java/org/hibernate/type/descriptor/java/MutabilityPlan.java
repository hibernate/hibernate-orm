/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;
import org.hibernate.SPI;

import jakarta.annotation.Nullable;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Describes the mutability semantics of a Java type.
///
/// Mutable values must be [#deepCopy(Object) copied] for dirty-checking
/// snapshots and carefully [#disassemble(Object, SharedSessionContract)
/// disassembled] for second-level caching. Immutable values may normally
/// return themselves from `deepCopy()`, but may still need a custom cache
/// representation when they are not serializable or retain entities or
/// heavyweight resources.
///
/// Supply a reusable plan from [JavaType#getMutabilityPlan()]. A plan must not
/// retain a session passed to `disassemble()` or `assemble()`.
///
/// @param <T> the planned Java value type
///
/// @see org.hibernate.annotations.Mutability
/// @see org.hibernate.annotations.CollectionIdMutability#value()
/// @see org.hibernate.annotations.MapKeyMutability#value()
/// @see org.hibernate.annotations.Mutability#value()
/// @see org.hibernate.mapping.BasicValue#setExplicitMutabilityPlanAccess(java.util.function.Function)
/// @see JavaType#getMutabilityPlan()
///
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface MutabilityPlan<T> extends Serializable {
	/**
	 * Can the internal state of instances of {@code T} be changed?
	 *
	 * @return True if the internal state can be changed; false otherwise.
	 */
	boolean isMutable();

	/**
	 * Return a deep copy of the value.
	 *
	 * @param value The value to deep copy
	 *
	 * @return The deep copy.
	 */
	@Nullable T deepCopy(@Nullable T value);

	/**
	 * Return a disassembled representation of the value.
	 * <p>
	 * Called before storing a value in the second-level cache.
	 * <p>
	 * Complementary to {@link #assemble}.
	 *
	 * @see #assemble
	 */
	@Nullable Serializable disassemble(@Nullable T value, SharedSessionContract session);

	/**
	 * Assemble a previously {@linkplain #disassemble disassembled} value.
	 * <p>
	 * Called after reading a value from the second level cache.
	 * <p>
	 * Complementary to {@link #disassemble}.
	 *
	 * @see #disassemble
	 */
	@Nullable T assemble(@Nullable Serializable cached, SharedSessionContract session);
}
