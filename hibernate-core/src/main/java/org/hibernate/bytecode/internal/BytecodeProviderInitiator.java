/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.hibernate.Internal;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
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
	public BytecodeProvider initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final Collection<BytecodeProvider> bytecodeProviders =
				registry.requireService( ClassLoaderService.class )
						.loadJavaServices( BytecodeProvider.class );
		return getBytecodeProvider( bytecodeProviders );
	}

	@Override
	public Class<BytecodeProvider> getServiceInitiated() {
		return BytecodeProvider.class;
	}

	@Internal
	public static BytecodeProvider buildDefaultBytecodeProvider() {
		// Use BytecodeProvider's ClassLoader to ensure we can find the service
		return getBytecodeProvider( ServiceLoader.load(
				BytecodeProvider.class,
				BytecodeProvider.class.getClassLoader()
		) );
	}

	@Internal
	public static BytecodeProvider getBytecodeProvider(Iterable<BytecodeProvider> bytecodeProviders) {
		final Iterator<BytecodeProvider> iterator = bytecodeProviders.iterator();
		if ( !iterator.hasNext() ) {
			// If no BytecodeProvider service is available, default to the "no-op" enhancer
			return new org.hibernate.bytecode.internal.none.BytecodeProviderImpl();
		}

		final BytecodeProvider provider = iterator.next();
		if ( iterator.hasNext() ) {
			throw new IllegalStateException( "Found multiple BytecodeProvider service registrations, cannot determine which one to use" );
		}
		return provider;
	}
}
