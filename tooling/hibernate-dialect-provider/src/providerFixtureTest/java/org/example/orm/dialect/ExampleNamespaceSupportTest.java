/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;

import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies nondefault namespace and schema-metadata contracts in the provider fixture.
///
/// @author Steve Ebersole
public class ExampleNamespaceSupportTest {
	@Test
	void suppliesProviderOwnedPluralNamespaceCommands() {
		final var dialect = new ExampleDialect();
		final var support = dialect.getNamespaceSupport();
		assertTrue( support.canCreateCatalog() );
		assertTrue( support.canCreateSchema() );
		assertArrayEquals(
				new String[] { "create fixture catalog orm", "initialize fixture catalog orm" },
				support.getCreateCatalogCommands( "orm" )
		);
		assertArrayEquals(
				new String[] { "create fixture schema if not exists orm" },
				support.getCreateSchemaCommands( "orm" )
		);
	}

	@Test
	void independentlySuppliesQualificationSeparatorAndSchemaResolution() throws Exception {
		final var dialect = new ExampleDialect();
		assertEquals( NameQualifierSupport.BOTH, dialect.getNameQualifierSupport() );
		assertEquals( "::", dialect.getCatalogSeparator() );

		final Connection connection = (Connection) Proxy.newProxyInstance(
				Connection.class.getClassLoader(),
				new Class<?>[] { Connection.class },
				(proxy, method, arguments) -> method.getName().equals( "getSchema" ) ? "provider" : null
		);
		assertEquals(
				"fixture_provider",
				dialect.getSchemaNameResolver().resolveSchemaName( connection, dialect )
		);
	}

	@Test
	void appendsProviderTableTypesWithoutChangingExistingEntries() {
		final var dialect = new ExampleDialect();
		final var physical = new ArrayList<>( java.util.List.of( "configured", "configured" ) );
		dialect.augmentPhysicalTableTypes( physical );
		assertEquals(
				java.util.List.of( "configured", "configured", "FIXTURE TABLE" ),
				physical
		);

		final var recognized = new ArrayList<>( java.util.List.of( "TABLE", "fixture view" ) );
		dialect.augmentRecognizedTableTypes( recognized );
		assertEquals(
				java.util.List.of( "TABLE", "fixture view", "FIXTURE VIEW" ),
				recognized
		);
	}
}
