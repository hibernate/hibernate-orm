/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.testing.spi.DialectContractProfile;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Self-tests the published harness against representative Core Dialects.
///
/// @author Steve Ebersole
class DialectTestKitSelfTest {
	@TestFactory
	DynamicContainer h2Contracts() {
		return DialectTestKit.contractTests( profile( "H2", H2Dialect::new ) );
	}

	@TestFactory
	DynamicContainer postgreSqlContracts() {
		return DialectTestKit.contractTests( profile( "PostgreSQL", PostgreSQLDialect::new ) );
	}

	@TestFactory
	DynamicContainer sybaseContracts() {
		return DialectTestKit.contractTests( profile( "Sybase ASE", SybaseASEDialect::new ) );
	}

	@Test
	void contextRejectsUseAfterClose() {
		final DialectTestContext context = DialectTestKit.openContext( profile( "H2", H2Dialect::new ) );
		context.close();
		assertThrows( IllegalStateException.class, context::getDialect );
	}

	@Test
	void contextRejectsUseFromAnotherThread() throws InterruptedException {
		try ( DialectTestContext context = DialectTestKit.openContext( profile( "H2", H2Dialect::new ) ) ) {
			final AtomicReference<Throwable> failure = new AtomicReference<>();
			final Thread thread = new Thread( () -> {
				try {
					context.getDialect();
				}
				catch (Throwable throwable) {
					failure.set( throwable );
				}
			} );
			thread.start();
			thread.join();
			assertTrue( failure.get() instanceof IllegalStateException );
		}
	}

	@Test
	void contextRejectsConnectionSettings() {
		assertThrows(
				IllegalArgumentException.class,
				() -> DialectTestKit.openContext( profile(
						"H2",
						H2Dialect::new,
						Map.of( "jakarta.persistence.jdbc.url", "jdbc:h2:mem:test" )
				) )
		);
	}

	@Test
	void contextRejectsSchemaExecutionSettings() {
		assertThrows(
				IllegalArgumentException.class,
				() -> DialectTestKit.openContext( profile(
						"H2",
						H2Dialect::new,
						Map.of( "hibernate.hbm2ddl.auto", "create" )
				) )
		);
	}

	private static DialectContractProfile profile(String name, DialectFactory factory) {
		return profile( name, factory, Map.of() );
	}

	private static DialectContractProfile profile(
			String name,
			DialectFactory factory,
			Map<String, Object> settings) {
		final DatabaseVersion expectedVersion = factory.create().getVersion();
		return new DialectContractProfile() {
			@Override
			public String name() {
				return name;
			}

			@Override
			public Dialect createDialect() {
				return factory.create();
			}

			@Override
			public DatabaseVersion expectedDatabaseVersion() {
				return expectedVersion;
			}

			@Override
			public Map<String, Object> settings() {
				return settings;
			}
		};
	}

	@FunctionalInterface
	private interface DialectFactory {
		Dialect create();
	}
}
