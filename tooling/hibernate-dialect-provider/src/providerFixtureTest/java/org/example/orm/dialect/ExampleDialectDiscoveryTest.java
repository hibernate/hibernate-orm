/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.lang.reflect.Proxy;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies provider-owned Dialect selector and resolver discovery.
///
/// @author Steve Ebersole
public class ExampleDialectDiscoveryTest {
	@Test
	void discoversAndInvokesBothProviderServices() {
		try ( var registry = new BootstrapServiceRegistryBuilder()
				.applyClassLoader( ExampleDialectDiscoveryTest.class.getClassLoader() )
				.build() ) {
			final ClassLoaderService classLoaderService = registry.requireService( ClassLoaderService.class );
			final DialectSelector selector = classLoaderService.loadJavaServices( DialectSelector.class )
					.stream()
					.filter( ExampleDialectSelector.class::isInstance )
					.findFirst()
					.orElseThrow();
			final DialectResolver resolver = classLoaderService.loadJavaServices( DialectResolver.class )
					.stream()
					.filter( ExampleDialectResolver.class::isInstance )
					.findFirst()
					.orElseThrow();

			assertEquals( ExampleDialect.class, selector.resolve( "Example" ) );
			assertNull( selector.resolve( "Unknown" ) );
			assertInstanceOf( ExampleDialect.class, resolver.resolveDialect( info( "ExampleDB" ) ) );
			assertNull( resolver.resolveDialect( info( "UnknownDB" ) ) );
		}
	}

	private static DialectResolutionInfo info(String databaseName) {
		return (DialectResolutionInfo) Proxy.newProxyInstance(
				DialectResolutionInfo.class.getClassLoader(),
				new Class<?>[] { DialectResolutionInfo.class },
				(proxy, method, arguments) -> switch ( method.getName() ) {
					case "getDatabaseName" -> databaseName;
					case "getDatabaseMajorVersion", "getDatabaseMinorVersion", "getDatabaseMicroVersion" -> 1;
					default -> null;
				}
		);
	}
}
