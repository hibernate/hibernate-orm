/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.util;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.jdbc.internal.TokenBasedFormatterImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for the ANTLR-based token formatter
 *
 * @author Jan Schatteman
 */
@ServiceRegistry
public class TokenBasedFormatterTest {

	private static final String FIXTURE_SEP = "@@@@ FIXTURE @@@@";
	private static final String EXPECTED_SEP = "=== EXPECTED ===";
	private static final String SQL_SEP = "=== SQL ===";
	private static final String ID_SEP = "=== ID ===";

	private static final TokenBasedFormatterImpl formatter = new TokenBasedFormatterImpl();

	@Test
	public void testFixtures(ServiceRegistryScope registryScope) {
		testDMLFixtures( registryScope );
		testDDLFixtures( registryScope );
	}

	private void testDMLFixtures(ServiceRegistryScope registryScope) {
		for ( SqlFixture fixture : loadFixtures(registryScope, "org/hibernate/orm/test/jdbc/util/dml_sql_fixtures.txt") ) {
			fixture.verify();
		}
	}

	private void testDDLFixtures(ServiceRegistryScope registryScope) {
		// todo
	}

	@Test
	public void testEmptyString() {
		String formatted = formatter.format("");
		assertTrue(formatted.isEmpty() || formatted.isBlank(), "Empty string should remain empty");
	}

	@Test
	public void testNullString() {
		String formatted = formatter.format(null);
		assertTrue(formatted == null || formatted.isEmpty(), "Null should be handled gracefully");
	}

	private List<SqlFixture> loadFixtures(ServiceRegistryScope registryScope, String path) {
		final StandardServiceRegistry serviceRegistry = registryScope.getRegistry();
		final ClassLoaderService classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		String input = null;
		try (InputStream is = classLoaderService.locateResourceStream( path )) {
			input = new String( is.readAllBytes(), StandardCharsets.UTF_8 );
		}
		catch (IOException e) {
			fail("Sql fixtures file could not be loaded", e);
		}

		return parseFixtures( input );
	}

	private List<SqlFixture> parseFixtures(String input) {
		final List<SqlFixture> testFixtures = new ArrayList<>(20);
		String[] fixtures = input.split( FIXTURE_SEP );
		// Ignore the header above the first fixture
		for ( int i = 1; i < fixtures.length; i++ ) {
			final String[] fixtureContents = fixtures[i].split(EXPECTED_SEP);
			final String expected = fixtureContents[1].trim();
			final String[] idsql = fixtureContents[0].split(SQL_SEP);
			final String sql = idsql[1].trim();
			// remove ID_SEP and its newline
			final String id = idsql[0].substring(ID_SEP.length() + 1).trim();

			if ( !(id.isBlank() || sql.isBlank() || expected.isBlank()) ) {
				testFixtures.add( new SqlFixture( id, sql, expected ) );
			}
		}

		return testFixtures;
	}

	private record SqlFixture(String id, String sql, String expected) {
		public void verify() {
				assertEquals( expected, formatter.format( sql ), "Sql formatting of \"%s\" failed".formatted(id) );
			}
		}

}
