/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;

/**
 * Describes the mutability aspects of a Java type.  The term mutability refers to the fact that generally speaking
 * the aspects described by this contract are defined by whether the Java type's internal state is mutable or not.
 *
 * @author Steve Ebersole
 */
public interface MutabilityPlan<T> extends Serializable {
	/**
	 * Can the internal state of instances of <tt>T</tt> be changed?
	 *
	 * @return True if the internal state can be changed; false otherwise.
	 */
	public boolean isMutable();

	/**
	 * Return a deep copy of the value.
	 *
	 * @param value The value to deep copy
	 *
	 * @return The deep copy.
	 */
	public T deepCopy(T value);

	/**
	 * Return a "disassembled" representation of the value.  This is used to push values onto the
	 * second level cache.  Compliment to {@link #assemble}
	 *
	 * @param value The value to disassemble
	 *
	 * @return The disassembled value.
	 *
	 * @see #assemble
	 */
	public Serializable disassemble(T value);

	/**
	 * Assemble a previously {@linkplain #disassemble disassembled} value.  This is used when pulling values from the
	 * second level cache.  Compliment to {@link #disassemble}
	 *
	 * @param cached The disassembled state
	 *
	 * @return The re-assembled value.
	 *
	 * @see #disassemble
	 */
	public T assemble(Serializable cached);
}
