/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Describes the mutability aspects of a given Java type.
 * <p>
 * Mutable values require special handling that is not necessary for
 * immutable values:
 * <ul>
 * <li>a mutable value must be {@linkplain #deepCopy(Object) cloned} when
 *     taking a "snapshot" of the state of an entity for dirty-checking,
 *     and
 * <li>a mutable value requires more careful handling when the entity is
 *     {@linkplain #disassemble(Object, SharedSessionContract) disassembled}
 *     for storage in destructured form in the second-level cache.
 * </ul>
 * <p>
 * Neither is a requirement for correctness when dealing with an immutable
 * object. But there might be other reasons why an immutable object requires
 * custom implementations of {@link #disassemble} and {@link #assemble}.
 * <p>
 * For example:
 * <ul>
 * <li>if the object is not serializable, we might convert it to a serializable
 *     format,
 * <li>if the object hold a reference to an entity, we must replace that
 *     reference with an identifier, or
 * <li>if the object hold a reference to some heavyweight resource, we must
 *     release it.
 * </ul>
 * <p>
 * For an immutable type, it's not usually necessary to do anything special
 * in {@link #deepCopy}. The method can simply return its argument.
 *
 * @apiNote The term "mutability" refers to the fact that, in general,
 *          the aspects of the Java type described by this contract are
 *          determined by whether the Java type has mutable internal
 *          state.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.annotations.Mutability
 */
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
