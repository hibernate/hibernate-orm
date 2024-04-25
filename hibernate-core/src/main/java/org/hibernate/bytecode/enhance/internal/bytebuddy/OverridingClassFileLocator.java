/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.bytebuddy.dynamic.ClassFileLocator;

/**
 * Allows wrapping another ClassFileLocator to add the ability to define
 * resolution overrides for specific resources.
 */
public final class OverridingClassFileLocator implements ClassFileLocator {

	private final ConcurrentHashMap<String, Resolution> registeredResolutions = new ConcurrentHashMap<>();
	private final ClassFileLocator parent;

	public OverridingClassFileLocator(final ClassFileLocator parent) {
		this.parent = Objects.requireNonNull( parent );
	}

	@Override
	public Resolution locate(final String name) throws IOException {
		final Resolution resolution = registeredResolutions.get( name );
		if ( resolution != null ) {
			return resolution;
		}
		else {
			return parent.locate( name );
		}
	}

	@Override
	public void close() throws IOException {
		//Nothing to do: we're not responsible for parent
	}

	void put(String className, Resolution.Explicit explicit) {
		registeredResolutions.put( className, explicit );
	}

	void remove(String className) {
		registeredResolutions.remove( className );
	}

}
