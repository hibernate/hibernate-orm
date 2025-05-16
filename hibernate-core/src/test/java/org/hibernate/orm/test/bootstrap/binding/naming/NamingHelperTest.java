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
						"fk_ka01ji8vk10osbgp6ve604cm1kfsso7byjr6s294jaukhv3ajq", "fk_kv2dq7fp00eyv1vq5yc29rvc3yftq2fmyg9iacv99wrn3nn0l6" ),
				Arguments.of(
						StandardCharsets.ISO_8859_1.name(),
						"fk_", "table_name", "other_table_name", List.of( "col1", "col2", "col3" ),
						"fk_ka01ji8vk10osbgp6ve604cm1kfsso7byjr6s294jaukhv3ajq", "fk_kv2dq7fp00eyv1vq5yc29rvc3yftq2fmyg9iacv99wrn3nn0l6" ),
				Arguments.of(
						StandardCharsets.UTF_8.name(),
						"fk_", "café", "le_déjeuner", List.of( "col1", "col2", "col3" ),
						"fk_ih8sokb1hh74aiucascp5pv0dlecescli8httwu7ca8ggbvxx4", "fk_1mlbg6hqesxj797eo16lo82hd491j5ag67833h3i1k7q99wo9b" ),
				Arguments.of(
						StandardCharsets.ISO_8859_1.name(),
						"fk_", "café", "le_déjeuner", List.of( "col1", "col2", "col3" ),
						"fk_i2tnixfnx9ylanksjrn8u41wvg5cdgl8wr7264olc17srxpa95", "fk_h8iedvm0im7uuapsek0b5wsc5goahu7wvjgtc3a5snqi79outg" ),
				Arguments.of(
						StandardCharsets.UTF_8.name(),
						"fk_", "abcdefghijklmnopqrstuvwxyzäöüß", "stuvwxyzäöüß", List.of( "col1" ),
						"fk_eh9134y0qw0bck215ws5kixdw8v41w26nuq76p5p6vdheyvbpk", "fk_megnb1o6em9hrlel3dvyomlmo41my964kfvdudonbumofve1jx" ),
				Arguments.of(
						StandardCharsets.ISO_8859_1.name(),
						"fk_", "abcdefghijklmnopqrstuvwxyzäöüß", "stuvwxyzäöüß", List.of( "col1" )
						, "fk_t8yjwdnsr4el6guwpgnxtlsvcgodr9rtaod8uor849w36552h", "fk_x5u4f3i64gnbca1jxu03q2968mn4b66bb6lbqbtf5apo6ux13" )
		);
	}

}
