/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.enhance;

import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import org.hibernate.bytecode.enhance.spi.EnhancementEnvironment;
import org.hibernate.bytecode.enhance.spi.EnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementOptions;
import org.hibernate.bytecode.spi.BytecodeProvider;

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

	private final EnhancementModel model;
	private final BytecodeProvider provider;
	private WeakReference<Entry> entry = new WeakReference<>( null );

	public ClientClassTransformer(EnhancementModel model, BytecodeProvider provider, ClassLoader temporaryLoader) {
		this.model = model;
		this.provider = provider == null ? BytecodeProviderInitiator.buildDefaultBytecodeProvider() : provider;
		getEnhancer(temporaryLoader);
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
			final var session = provider.createEnhancementSession(model, EnhancementEnvironment.forClassLoader(loader));
			for (String candidate : model.getCandidates()) {
				session.discoverTypes(candidate, null);
			}
			final var enhancer = session.createEnhancer(EnhancementOptions.of(false, false, false));
			current = new Entry( loader, enhancer );
			entry = new WeakReference<>( current );
		}
		return current.enhancer;
	}

	private record Entry(ClassLoader loader, Enhancer enhancer) {
	}
}
