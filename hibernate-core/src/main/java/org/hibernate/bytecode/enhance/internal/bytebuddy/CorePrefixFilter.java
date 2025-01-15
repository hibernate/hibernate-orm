/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Objects;

/**
 * We differentiate between core classes and application classes during symbol
 * resolution for the purposes of entity enhancement.
 * The discriminator is the prefix of the fully qualified classname, for
 * example it could be package names.
 * The "core classes" don't have to be comprehensively defined: we want a small
 * set of prefixes for which we know with certainty that a)They won't be used
 * in application code (assuming people honour reasonable package name rules)
 * or any code that needs being enhanced. and b) frequently need to be looked up
 * during the enhancement process.
 * A great example is the "jakarta.persistence.Entity" annotation: we'll most likely
 * need to load it when doing any form of introspection on user's code, but we expect
 * the bytecode which represents the annotation to not be enhanced.
 * We then benefit from caching such representations of object types which are frequently
 * loaded; since caching end user code would lead to enhancement problems, it's best
 * to keep the list conservative when in doubt.
 * For example, don't consider all of {@code "org.hibernate."} prefixes as safe, as
 * that would include entities used during our own testsuite and entities defined by Envers.
 *
 */
public final class CorePrefixFilter {

	private final String[] acceptedPrefixes;
	public static final CorePrefixFilter DEFAULT_INSTANCE = new CorePrefixFilter();

	/**
	 * Do not invoke: use DEFAULT_INSTANCE
	 */
	CorePrefixFilter() {
		//By default optimise for jakarta annotations, java util collections, and Hibernate marker interfaces
		this("jakarta.", "java.", "org.hibernate.annotations.", "org.hibernate.bytecode.enhance.spi.", "org.hibernate.engine.spi.");
	}

	public CorePrefixFilter(final String... acceptedPrefixes) {
		this.acceptedPrefixes = Objects.requireNonNull( acceptedPrefixes );
	}

	public boolean isCoreClassName(final String name) {
		for ( String acceptedPrefix : this.acceptedPrefixes ) {
			if ( name.startsWith( acceptedPrefix ) ) {
				return true;
			}
		}
		return false;
	}

}
