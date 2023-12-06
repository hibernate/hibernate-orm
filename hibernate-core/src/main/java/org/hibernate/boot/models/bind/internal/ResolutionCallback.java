/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.internal;

/**
 * Provides callback notification when an object of interest is
 * fully resolved and all of its state available.
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface ResolutionCallback<T> {
	/**
	 * Callback to use the fully resolved {@code resolvedThing}
	 *
	 * @param resolvedThing The resolved object of interest
	 *
	 * @return {@code true} if processing was successful; {@code false} otherwise
	 */
	boolean handleResolution(T resolvedThing);
}
