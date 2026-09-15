/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;

import java.util.stream.Stream;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.sql.JdbcParameterCounter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcParameterCounterTest {
	static Stream<Arguments> statements() {
		final var h2 = new H2Dialect();
		final var pg = new PostgreSQLDialect();
		return Stream.of(
				Arguments.of( "update item set name=? where id=?", h2, 2 ),
				Arguments.of( "update item set name='isn''t ?' where id=?", h2, 1 ),
				Arguments.of( "update \"table?\" set \"a\"\"?\"=? where id=?", h2, 2 ),
				Arguments.of( "delete from item -- ?\r\n where id=? /* '?' */", h2, 1 ),
				Arguments.of( "delete /* outer /* inner ? */ ? */ from item where id=?", pg, 1 ),
				Arguments.of( "update item set name=$tag$it's ?$tag$ where id=?", pg, 1 ),
				Arguments.of( "update item set name=$$it's ?$$ where id=?", h2, 1 ),
				Arguments.of( "update item set name=E'isn\\'t ?' where id=?", pg, 1 ),
				Arguments.of( "update item set name='\\' where id=?", pg, 1 ),
				Arguments.of( "delete from item where data ?? 'key' and id=?", pg, 1 ),
				Arguments.of( "delete from item where data ??| array[?] and id=?", pg, 2 ),
				Arguments.of( "update item set name=q'[isn't ?]' where id=?", new OracleDialect(), 1 ),
				Arguments.of( "update item set name=q'!isn't ?!' where id=?", new OracleDialect(), 1 ),
				Arguments.of( "update [table?] set [a]]?]=? where id=?", new SQLServerDialect(), 2 ),
				Arguments.of( "update `table?` set `a``?`=? where id=?", new MySQLDialect(), 2 ),
				Arguments.of( "update item set name='isn\\'t ?' where id=? # ?", new MySQLDialect(), 1 ),
				Arguments.of( "update item set amount=amount--? where id=?", new MySQLDialect(), 2 ),
				Arguments.of( "{? = call update_item(?,?,?,?)}", h2, 5 ),
				Arguments.of( "{call delete_item(?,?)}", h2, 2 )
		);
	}

	@ParameterizedTest
	@MethodSource("statements")
	void countsJdbcMarkers(String sql, Dialect dialect, int expected) {
		assertEquals( expected, JdbcParameterCounter.count( sql, dialect ) );
	}

	@ParameterizedTest
	@ValueSource(strings = { "select '?' /*", "select 'unterminated ?", "select $tag$unterminated ?", "select q'[unterminated ?" })
	void rejectsUnterminatedText(String sql) {
		assertThrows( MappingException.class, () -> JdbcParameterCounter.count( sql, new H2Dialect() ) );
	}
}
