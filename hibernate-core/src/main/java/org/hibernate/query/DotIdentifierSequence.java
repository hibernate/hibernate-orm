/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import java.util.function.BiFunction;

/**
 * Hibernate often deals with compound names/paths.  This interface defines a standard way of interacting with them
 *
 * @author Steve Ebersole
 */
public interface DotIdentifierSequence {
	DotIdentifierSequence getParent();
	String getLocalName();
	String getFullPath();

	DotIdentifierSequence append(String subPathName);

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
