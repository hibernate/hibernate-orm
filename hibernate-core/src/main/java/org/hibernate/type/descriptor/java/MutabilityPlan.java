/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;

/**
 * Describes the mutability aspects of a given Java type.
 * <p>
 * The term "mutability" refers to the fact that, generally speaking, the
 * aspects described by this contract are determined by whether the Java
 * type's internal state is mutable or immutable. For example, for an
 * immutable Java class, {@link #deepCopy(Object)} may simply return its
 * argument.
 *
 * @author Steve Ebersole
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
	T deepCopy(T value);

	/**
	 * Return a disassembled representation of the value.
	 *
	 * Called before storing a value in the second-level cache.
	 *
	 * Complementary to {@link #assemble}.
	 *
	 * @see #assemble
	 */
	Serializable disassemble(T value, SharedSessionContract session);

	/**
	 * Assemble a previously {@linkplain #disassemble disassembled} value.
	 *
	 * Called after reading a value from the second level cache.
	 *
	 * Complementary to {@link #disassemble}.
	 *
	 * @see #disassemble
	 */
	T assemble(Serializable cached, SharedSessionContract session);
}
