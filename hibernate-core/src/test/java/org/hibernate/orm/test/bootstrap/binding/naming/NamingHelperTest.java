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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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
