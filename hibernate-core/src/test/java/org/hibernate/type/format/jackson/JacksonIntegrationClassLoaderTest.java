/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.type.format.FormatMapperCreationContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonIntegrationClassLoaderTest {

	private ClassLoader originalTccl;

	@BeforeEach
	void setUp() {
		originalTccl = Thread.currentThread().getContextClassLoader();
	}

	@AfterEach
	void tearDown() {
		Thread.currentThread().setContextClassLoader( originalTccl );
	}

	@Test
	void loadJackson2Modules_usingClassloadingService() {
		Thread.currentThread().setContextClassLoader( wrapperClassLoader() );

		AtomicBoolean called = new AtomicBoolean( false );
		JacksonIntegration.loadModules( recordingContext( called ) );

		assertTrue( called.get() );
	}

	@Test
	void loadJackson3Modules_usingClassloadingService() {
		Thread.currentThread().setContextClassLoader( wrapperClassLoader() );

		AtomicBoolean called = new AtomicBoolean( false );
		JacksonIntegration.loadJackson3Modules( recordingContext( called ) );

		assertTrue( called.get() );
	}

	/**
	 * A wrapper classloader that delegates to the same parent — different instance,
	 * same class definitions. This simulates a Jakarta EE deployment where the
	 * application classloader is distinct from Hibernate's classloader but shares
	 * the same Jackson jars.
	 */
	private ClassLoader wrapperClassLoader() {
		return new URLClassLoader( new URL[0], JacksonIntegration.class.getClassLoader() );
	}

	private FormatMapperCreationContext recordingContext(AtomicBoolean workWithClassLoaderCalled) {
		ClassLoaderService classLoaderService = classLoaderServiceProxy( workWithClassLoaderCalled );
		BootstrapContext bootstrapContext = bootstrapContextProxy( classLoaderService );
		return () -> bootstrapContext;
	}

	private ClassLoaderService classLoaderServiceProxy(AtomicBoolean workWithClassLoaderCalled) {
		return (ClassLoaderService) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { ClassLoaderService.class },
				(proxy, method, args) -> {
					if ( "workWithClassLoader".equals( method.getName() ) ) {
						workWithClassLoaderCalled.set( true );
						ClassLoaderService.Work<?> work = (ClassLoaderService.Work<?>) args[0];
						return work.doWork( getClass().getClassLoader() );
					}
					throw new UnsupportedOperationException( method.getName() );
				}
		);
	}

	private BootstrapContext bootstrapContextProxy(ClassLoaderService classLoaderService) {
		return (BootstrapContext) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { BootstrapContext.class },
				(proxy, method, args) -> {
					if ( "getClassLoaderService".equals( method.getName() ) ) {
						return classLoaderService;
					}
					throw new UnsupportedOperationException( method.getName() );
				}
		);
	}
}
