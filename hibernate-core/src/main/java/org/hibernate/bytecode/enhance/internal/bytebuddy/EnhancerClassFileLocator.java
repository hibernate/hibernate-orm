/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import net.bytebuddy.dynamic.ClassFileLocator;

import java.io.IOException;

/**
 * A delegating ClassFileLocator that allows overriding the resolution for the class that is currently being enhanced.
 */
final class EnhancerClassFileLocator implements ClassFileLocator {

	private final EnhancerCacheProvider cacheProvider;
	private final ClassFileLocator delegate;

	public EnhancerClassFileLocator(EnhancerCacheProvider cacheProvider, ClassFileLocator delegate) {
		this.cacheProvider = cacheProvider;
		this.delegate = delegate;
	}

	@Override
	public Resolution locate(final String name) throws IOException {
		final EnhancerCacheProvider.EnhancementState enhancementState = cacheProvider.getEnhancementState();
		if ( enhancementState != null && enhancementState.getClassName().equals( name ) ) {
			return enhancementState.getClassFileResolution();
		}
		return delegate.locate( name );
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}

}
