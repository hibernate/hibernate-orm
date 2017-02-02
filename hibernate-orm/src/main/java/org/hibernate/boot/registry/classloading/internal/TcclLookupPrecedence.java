/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.classloading.internal;

/**
 * Defines when the lookup in the current thread context {@link ClassLoader} should be
 * done according to the other ones.
 * 
 * @author Cédric Tabin
 */
public enum TcclLookupPrecedence {
	/**
	 * The current thread context {@link ClassLoader} will never be used during
	 * the class lookup.
	 */
	NEVER,

	/**
	 * The class lookup will be done in the thread context {@link ClassLoader} prior
	 * to the other {@code ClassLoader}s.
	 */
	BEFORE,

	/**
	 * The class lookup will be done in the thread context {@link ClassLoader} if
	 * the former hasn't been found in the other {@code ClassLoader}s.
	 * This is the default value.
	 */
	AFTER
}
