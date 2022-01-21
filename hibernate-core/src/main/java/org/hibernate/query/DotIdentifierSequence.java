/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import java.util.function.BiFunction;

/**
 * Hibernate often deals with compound names/paths.  This interface defines a
 * standard way of interacting with them
 *
 * @author Steve Ebersole
 */
public interface DotIdentifierSequence {
	/**
	 * The parent sequence part.  E.g., given the sequence `a.b.c`,
	 * this returns `a.b`
	 */
	DotIdentifierSequence getParent();

	/**
	 * The name of this sequence part.  E.g., given the sequence `a.b.c`,
	 * this returns `c`
	 */
	String getLocalName();

	/**
	 * The full sequence text.  E.g., given the sequence `a.b.c`,
	 * this returns `a.b.c`
	 */
	String getFullPath();

	/**
	 * Add a new part to the end of this sequence, returning the new
	 * representation.  E.g., given the sequence `a.b.c` and appending `d`
	 * would return a new sequence `a.b.c.d`
	 */
	DotIdentifierSequence append(String subPathName);

	/**
	 * Is this sequence node the root of the sequence.  Same as checking
	 * the nullness of {@link #getParent()}
	 */
	default boolean isRoot() {
		return getParent() == null;
	}

	default <T> T resolve(T base, BiFunction<T, String, T> baseResolver, BiFunction<T, String, T> resolver) {
		final T result;
		if ( getParent() == null ) {
			result = baseResolver.apply( base, getLocalName() );
		}
		else {
			result = resolver.apply( getParent().resolve( base, baseResolver, resolver ), getLocalName() );
		}
		return result;
	}
}
