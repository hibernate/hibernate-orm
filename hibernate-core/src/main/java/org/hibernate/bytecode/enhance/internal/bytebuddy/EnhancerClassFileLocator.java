/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		final var enhancementState = cacheProvider.getEnhancementState();
		return enhancementState != null && enhancementState.getClassName().equals( name )
				? enhancementState.getClassFileResolution()
				: delegate.locate( name );
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}

}
