/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.enhance;

import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.HashSet;
import java.util.function.Function;

import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.TransformerException;
import org.hibernate.bytecode.enhance.internal.bytebuddy.CorePrefixFilter;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.BytecodeProviderInitiator;

/// Rewrites client instructions without performing managed-class enhancement.
/// Each defining loader receives its own enhancement context and type cache.
///
/// @since 8.0
/// @author Steve Ebersole
public final class ClientClassTransformer implements ClassTransformer {

	private final Function<ClassLoader, ClientEnhancementContext> contextFactory;
	private WeakReference<Entry> entry = new WeakReference<>( null );

	public ClientClassTransformer(
			Function<ClassLoader, ClientEnhancementContext> contextFactory,
			ClassLoader temporaryLoader) {
		this.contextFactory = contextFactory;
		getEnhancer( temporaryLoader );
	}

	@Override
	public byte[] transform(
			ClassLoader loader,
			String className,
			Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws TransformerException {
		if ( loader == null || CorePrefixFilter.DEFAULT_INSTANCE.isCoreClassName( className.replace( '/', '.' ) ) ) {
			return null;
		}
		try {
			return getEnhancer( loader ).enhanceClient( className, classfileBuffer );
		}
		catch (RuntimeException e) {
			throw new TransformerException( "Error performing client enhancement of " + className, e );
		}
	}

	private synchronized Enhancer getEnhancer(ClassLoader loader) {
		var current = entry.get();
		if ( current == null || current.loader != loader ) {
			final var context = contextFactory.apply( loader );
			final var configuredProvider = context.getBytecodeProvider();
			final var provider = configuredProvider == null
					? BytecodeProviderInitiator.buildDefaultBytecodeProvider() : configuredProvider;
			final var enhancer = provider.getEnhancer( context );
			final var discovered = new HashSet<String>();
			while ( discovered.addAll( context.getCandidates() ) ) {
				for ( String candidate : List.copyOf( discovered ) ) {
					enhancer.discoverTypes( candidate, null );
				}
			}
			current = new Entry( loader, enhancer );
			entry = new WeakReference<>( current );
		}
		return current.enhancer;
	}

	private record Entry(ClassLoader loader, Enhancer enhancer) {
	}
}
