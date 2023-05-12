/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model;

/**
 * Registry of all {@link ClassDetails} references
 *
 * @author Steve Ebersole
 */
public interface ClassDetailsRegistry {
	/**
	 * Find the managed-class with the given {@code name}, if there is one.
	 * Returns {@code null} if there are none registered with that name.
	 *
	 * @see #resolveClassDetails
	 */
	ClassDetails findClassDetails(String name);

	/**
	 * Form of {@link #findClassDetails} throwing an exception if no registration is found
	 *
	 * @see #resolveClassDetails
	 *
	 * @throws UnknownManagedClassException If no registration is found with the given {@code name}
	 */
	ClassDetails getClassDetails(String name);

	/**
	 * Resolves a managed-class by name.  If there is currently no such registration,
	 * one is created and registered.
	 */
	ClassDetails resolveClassDetails(String name);
}
