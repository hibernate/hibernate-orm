/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spi;

import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A compound name.
 * <p>
 * Hibernate often deals with compound names/paths.
 * This interface defines a standard way of interacting with them.
 *
 * @author Steve Ebersole
 */
public interface DotIdentifierSequence {
	/**
	 * The parent sequence part.
	 * <p>
	 * Given the sequence {@code a.b.c}, returns the sequence
	 * {@code a.b}.
	 */
	@Nullable DotIdentifierSequence getParent();

	/**
	 * The name of this leaf sequence part.
	 * <p>
	 * Given the sequence {@code a.b.c}, returns the string
	 * {@code "c"}.
	 */
	String getLocalName();

	/**
	 * The full sequence text.
	 * <p>
	 * Given the sequence {@code a.b.c}, returns the string
	 * {@code "a.b.c"}.
	 *
	 * @implNote This method may dynamically build the returned
	 *           string and should be avoided for critical paths
	 *           (comparisons,for example).
	 */
	String getFullPath();

	/**
	 * Append a new part to the end of this sequence, returning
	 * the new representation.
	 * <p>
	 * Given the sequence {@code a.b.c}, appending {@code d}
	 * results in the new sequence {@code a.b.c.d}.
	 */
	DotIdentifierSequence append(String subPathName);

	default DotIdentifierSequence[] getParts() {
		final List<DotIdentifierSequence> list = new ArrayList<>();
		parts( list );
		return list.toArray(new DotIdentifierSequence[0]);
	}

	private void parts(List<DotIdentifierSequence> list) {
		DotIdentifierSequence parent = getParent();
		if ( parent != null ) {
			parent.parts( list );
		}
		list.add( this );
	}

	/**
	 * Is this sequence node the root of the sequence?
	 * <p>
	 * Same as checking the nullness of {@link #getParent()}.
	 */
	default boolean isRoot() {
		return getParent() == null;
	}

}
