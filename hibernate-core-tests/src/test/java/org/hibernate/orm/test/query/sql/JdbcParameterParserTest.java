package org.hibernate.orm.test.query.sql;

import java.util.stream.Stream;

import org.hibernate.MappingException;
import org.hibernate.query.sql.internal.ParameterParser;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcParameterParserTest {
	static Stream<Arguments> statements() {
		return Stream.of(
				Arguments.of( "select '?'", 0 ),
				Arguments.of( "update item set name=? where id=?", 2 ),
				Arguments.of( "update item set name='isn''t ?' where id=?", 1 ),
				Arguments.of( "update \"table?\" set \"a\"\"?\"=? where id=?", 2 ),
				Arguments.of( "delete from item -- ?\r\n where id=? /* '?' */", 1 ),
				Arguments.of( "delete from item where id=? -- ?", 1 ),
				Arguments.of( "{? = call update_item(?,?,?,?)}", 5 ),
				Arguments.of( "{call delete_item(?,?)}", 2 )
		);
	}

	@ParameterizedTest
	@MethodSource("statements")
	void countsJdbcMarkers(String sql, int expected) {
		assertEquals( expected, ParameterParser.countJdbcParameters( sql ) );
	}

	@ParameterizedTest
	@ValueSource(strings = { "select '?' /*", "select 'unterminated ?", "select \"unterminated ?" })
	void rejectsUnterminatedText(String sql) {
		assertThrows( MappingException.class, () -> ParameterParser.countJdbcParameters( sql ) );
	}
}
