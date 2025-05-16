/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.naming;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.NamingHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class NamingHelperTest {

	@ParameterizedTest
	@MethodSource("args")
	void smoke(String charset, String prefix, String tableName, String referencedTableName, List<String> columnNames, String expectedFkName, String expectedConstraintName) {
		assertThat( NamingHelper.withCharset( charset )
				.generateHashedFkName(
						prefix,
						Identifier.toIdentifier( tableName ),
						Identifier.toIdentifier( referencedTableName ),
						columnNames.stream().map( Identifier::toIdentifier )
								.collect( Collectors.toUnmodifiableList() ) ) )
				.isEqualTo( expectedFkName );

		assertThat( NamingHelper.withCharset( charset )
				.generateHashedFkName(
						prefix,
						Identifier.toIdentifier( tableName ),
						Identifier.toIdentifier( referencedTableName ),
						columnNames.stream().map( Identifier::toIdentifier )
								.toArray( Identifier[]::new ) ) )
				.isEqualTo( expectedFkName );

		assertThat( NamingHelper.withCharset( charset )
				.generateHashedConstraintName(
						prefix,
						Identifier.toIdentifier( tableName ),
						columnNames.stream().map( Identifier::toIdentifier )
								.collect( Collectors.toUnmodifiableList() ) ) )
				.isEqualTo( expectedConstraintName );

		assertThat( NamingHelper.withCharset( charset )
				.generateHashedConstraintName(
						prefix,
						Identifier.toIdentifier( tableName ),
						columnNames.stream().map( Identifier::toIdentifier )
								.toArray( Identifier[]::new ) ) )
				.isEqualTo( expectedConstraintName );
	}

	@Test
	void testHashWithAlgorithm_md5_utf8() throws Exception {
		NamingHelper helper = NamingHelper.withCharset(StandardCharsets.UTF_8.name());
		String hash = helper.hashWithAlgorithm("table_name", "MD5");
		assertNotNull(hash);
		assertFalse(hash.isEmpty());
		// MD5 hash of "table_name" in base 35 should be deterministic
		assertEquals("8q6ok4ne4ufel54crtitkq7ir", hash);
	}

	@Test
	void testHashWithAlgorithm_sha256_utf8() throws Exception {
		NamingHelper helper = NamingHelper.withCharset(StandardCharsets.UTF_8.name());
		String hash = helper.hashWithAlgorithm("table_name", "SHA-256");
		assertNotNull(hash);
		assertFalse(hash.isEmpty());
		// SHA-256 hash of "table_name" in base 35 should be deterministic
		assertEquals("nie2bx5e7mderevrnl4gkuhtmy45nwfvst7dv6cx3pb3yy9ul1", hash);
	}

	@Test
	void testHashWithAlgorithm_md5_iso88591() throws Exception {
		NamingHelper helper = NamingHelper.withCharset(StandardCharsets.ISO_8859_1.name());
		String hash = helper.hashWithAlgorithm("café", "MD5");
		assertNotNull(hash);
		assertFalse(hash.isEmpty());
		assertEquals("hgll69c0qdhsjikniholqfcj4", hash);
	}

	@Test
	void testHashWithAlgorithm_invalidAlgorithm() {
		NamingHelper helper = NamingHelper.withCharset(StandardCharsets.UTF_8.name());
		assertThrows(NoSuchAlgorithmException.class, () -> {
			helper.hashWithAlgorithm("table_name", "NOPE");
		});
	}

	@Test
	void testHashWithAlgorithm_invalidCharset() {
		NamingHelper helper = NamingHelper.withCharset("NOPE-CHARSET");
		assertThrows(UnsupportedEncodingException.class, () -> {
			helper.hashWithAlgorithm("table_name", "MD5");
		});
	}

	private static Stream<Arguments> args() {
		// String charset, String prefix, String tableName, String referencedTableName,
		// List<String> columnNames, String expectedFkName, String expectedConstraintName
		return Stream.of(
				Arguments.of(
						StandardCharsets.UTF_8.name(),
						"fk_", "table_name", "other_table_name", List.of( "col1", "col2", "col3" ),
						"fk_f4u43ook9b825fxbm3exb18q6", "fk_1o8k3sa4q2a2wb596v4htt8qf" ),
				Arguments.of(
						StandardCharsets.ISO_8859_1.name(),
						"fk_", "table_name", "other_table_name", List.of( "col1", "col2", "col3" ),
						"fk_f4u43ook9b825fxbm3exb18q6", "fk_1o8k3sa4q2a2wb596v4htt8qf" ),
				Arguments.of(
						StandardCharsets.UTF_8.name(),
						"fk_", "café", "le_déjeuner", List.of( "col1", "col2", "col3" ),
						"fk_jdvsrk14lxab6a829ok160vyj", "fk_h34kugb2bguwmcn1g5h1q3snf" ),
				Arguments.of(
						StandardCharsets.ISO_8859_1.name(),
						"fk_", "café", "le_déjeuner", List.of( "col1", "col2", "col3" ),
						"fk_g1py0mkjd1tu46tr8c2e1vm2l", "fk_1pitt5gtytwpy6ea02o7l5men" ),
				Arguments.of(
						StandardCharsets.UTF_8.name(),
						"fk_", "abcdefghijklmnopqrstuvwxyzäöüß", "stuvwxyzäöüß", List.of( "col1" ),
						"fk_q11mlivmrc3sdfnncd2hwkpqp", "fk_gm8xsqu7ayucv5w5w2gj2dfly" ),
				Arguments.of(
						StandardCharsets.ISO_8859_1.name(),
						"fk_", "abcdefghijklmnopqrstuvwxyzäöüß", "stuvwxyzäöüß", List.of( "col1" )
						, "fk_fua9hgc6dn6eno8hlqt58j72o", "fk_3iig3yrgsf5bjlbdo05d7mp2" )
		);
	}

}
