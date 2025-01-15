/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import net.bytebuddy.dynamic.ClassFileLocator;

/**
 * Allows wrapping another ClassFileLocator to add the ability to define
 * resolution overrides for specific resources.
 * This is useful when enhancing entities and we need to process an
 * input byte array representing the bytecode of the entity, and some
 * external party is providing the byte array explicitly, avoiding for
 * us having to load it from the classloader.
 * We'll still need to load several other symbols from a parent @{@link ClassFileLocator}
 * (typically based on the classloader), but need to return the provided
 * byte array for some selected resources.
 * Any override is scoped to the current thread; this is to avoid
 * interference among threads in systems which perform parallel
 * class enhancement, for example containers requiring "on the fly"
 * transformation of classes as they are being loaded will often
 * perform such operations concurrently.
 */
final class OverridingClassFileLocator implements ClassFileLocator {

	private final ThreadLocal<HashMap<String, Resolution>> registeredResolutions = ThreadLocal.withInitial( () -> new HashMap<>() );
	private final ClassFileLocator parent;

	/**
	 * @param parent the @{@link ClassFileLocator} which will be used to load any resource which wasn't explicitly registered as an override.
	 */
	OverridingClassFileLocator(final ClassFileLocator parent) {
		this.parent = Objects.requireNonNull( parent );
	}

	@Override
	public Resolution locate(final String name) throws IOException {
		final Resolution resolution = getLocalMap().get( name );
		if ( resolution != null ) {
			return resolution;
		}
		else {
			return parent.locate( name );
		}
	}

	private HashMap<String, Resolution> getLocalMap() {
		return registeredResolutions.get();
	}

	@Override
	public void close() {
		registeredResolutions.remove();
	}

	/**
	 * Registers an explicit resolution override
	 *
	 * @param className
	 * @param explicit
	 */
	void put(String className, Resolution.Explicit explicit) {
		getLocalMap().put( className, explicit );
	}

	/**
	 * Removes an explicit resolution override
	 *
	 * @param className
	 */
	void remove(String className) {
		getLocalMap().remove( className );
	}

}
