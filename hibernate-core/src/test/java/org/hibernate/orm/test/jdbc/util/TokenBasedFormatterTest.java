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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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

	private final Map<String, SqlFixture> dmlFixtures = new HashMap<>(20);

	private static final TokenBasedFormatterImpl formatter = new TokenBasedFormatterImpl();

	@Test
	public void testFixtures() {
		testDMLFixtures();
		testDDLFixtures();
	}

	// Utility method to be able to debug a single fixture (uncomment '@Test')
	@Test
	public void testSelectedDmlFixture() {
		final String id = "";
		if ( !id.isBlank() ) {
			dmlFixtures.get( id ).verify();
		}
	}

	private void testDMLFixtures() {
		for ( SqlFixture fixture : dmlFixtures.values() ) {
			fixture.verify();
		}
	}

	private void testDDLFixtures() {
		// todo
	}

	@Test
	public void testEmptyString() {
		String formatted = formatter.format("");
		assertTrue(formatted.isBlank(), "Empty string should remain empty");
	}

	@Test
	public void testNullString() {
		String formatted = formatter.format(null);
		assertTrue(formatted == null || formatted.isEmpty(), "Null should be handled gracefully");
	}

	@BeforeAll
	public void setup(ServiceRegistryScope registryScope) {
		this.dmlFixtures.putAll( loadFixtures(registryScope, "org/hibernate/orm/test/jdbc/util/dml_sql_fixtures.txt") );
	}

	private Map<String, SqlFixture> loadFixtures(ServiceRegistryScope registryScope, String path) {
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

	private Map<String, SqlFixture> parseFixtures(String input) {
		final Map<String, SqlFixture> testFixtures = new HashMap<>(20);
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
				testFixtures.put( id, new SqlFixture( id, sql, expected ) );
			}
		}

		return testFixtures;
	}

	private record SqlFixture(String id, String sql, String expected) {
		public void verify() {
				String actual = formatter.format( sql );
				System.out.println("=== Testing: " + id + " ===");
				System.out.println("Expected:");
				System.out.println(expected);
				System.out.println("\nActual:");
				System.out.println(actual);
				System.out.println("===");
				assertEquals( expected, actual, "Sql formatting of \"%s\" failed".formatted(id) );
			}
		}

}
