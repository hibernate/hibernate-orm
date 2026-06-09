/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal;

import java.util.Map;
import java.util.ServiceLoader;

import jakarta.annotation.Nonnull;
import org.hibernate.Internal;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.bytecode.internal.none.BytecodeProviderImpl;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class BytecodeProviderInitiator implements StandardServiceInitiator<BytecodeProvider> {

	/**
	 * @deprecated Register a {@link BytecodeProvider} through Java {@linkplain java.util.ServiceLoader services}.
	 */
	@Deprecated( forRemoval = true, since = "6.2" )
	public static final String BYTECODE_PROVIDER_NAME_BYTEBUDDY = "bytebuddy";

	/**
	 * Singleton access
	 */
	public static final StandardServiceInitiator<BytecodeProvider> INSTANCE = new BytecodeProviderInitiator();

	@Override
	@Nonnull
	public BytecodeProvider initiateService(@Nonnull Map<String, Object> configurationValues, @Nonnull ServiceRegistryImplementor registry) {
		final var bytecodeProviders =
				registry.requireService( ClassLoaderService.class )
						.loadJavaServices( BytecodeProvider.class );
		return getBytecodeProvider( bytecodeProviders );
	}

	@Override
	@Nonnull
	public Class<BytecodeProvider> getServiceInitiated() {
		return BytecodeProvider.class;
	}

	@Internal
	@Nonnull
	public static BytecodeProvider buildDefaultBytecodeProvider() {
		// Use BytecodeProvider's ClassLoader to ensure we can find the service
		return getBytecodeProvider( ServiceLoader.load(
				BytecodeProvider.class,
				BytecodeProvider.class.getClassLoader()
		) );
	}

	@Internal
	@Nonnull
	public static BytecodeProvider getBytecodeProvider(@Nonnull Iterable<BytecodeProvider> bytecodeProviders) {
		final var iterator = bytecodeProviders.iterator();
		if ( !iterator.hasNext() ) {
			// If no BytecodeProvider service is available,
			// default to the "no-op" enhancer
			return new BytecodeProviderImpl();
		}
		else {
			final var provider = iterator.next();
			if ( iterator.hasNext() ) {
				throw new IllegalStateException(
						"Found multiple BytecodeProvider service registrations, cannot determine which one to use" );
			}
			return provider;
		}
	}
}
