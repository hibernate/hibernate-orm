package org.hibernate.orm.test.query.sql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.engine.query.ParameterRecognitionException;
import org.hibernate.engine.query.internal.NativeQueryInterpreterStandardImpl;
import org.hibernate.query.sql.internal.ParameterParser;
import org.hibernate.query.sql.internal.ParameterRecognizerImpl;
import org.hibernate.query.sql.spi.ParameterRecognizer;

import org.hibernate.testing.orm.junit.JiraKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.engine.query.internal.NativeQueryInterpreterStandardImpl.NATIVE_QUERY_INTERPRETER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Unit tests of the ParameterParser class
 *
 * @author Steve Ebersole
 */
public class ParameterParserTest {
	static Stream<Arguments> nativeQueryPrefixes() {
		return Stream.of(
				Arguments.of( "select 'isn''t :ignored ?'", "select 'isn''t :ignored ?'" ),
				Arguments.of( "select \"a\"\":ignored?\"", "select \"a\"\":ignored?\"" ),
				Arguments.of( "select '\\'", "select '\\'" ),
				Arguments.of( "select 1 -- :ignored ?\n", "select 1 -- :ignored ?\n" ),
				Arguments.of( "select 1 -- :ignored ?\r", "select 1 -- :ignored ?\r" ),
				Arguments.of( "select 1 -- :ignored ?\r\n", "select 1 -- :ignored ?\r\n" ),
				Arguments.of( "select 1 /* ' :ignored ? */", "select 1 /* ' :ignored ? */" ),
				Arguments.of( "select 1 /* /* :ignored ? */", "select 1 /* /* :ignored ? */" ),
				Arguments.of( "select \\?, \\:literal, \\\\", "select ?, :literal, \\" ),
				Arguments.of( "select value::::text, @a::=20", "select value::text, @a:=20" )
		);
	}

	@ParameterizedTest
	@MethodSource("nativeQueryPrefixes")
	void preservesNativeQueryTextAndParameterPositions(String prefix, String adjustedPrefix) {
		final var sourcePositions = new ArrayList<Integer>();
		final var recognizer = new ParameterRecognizerImpl() {
			@Override
			public void namedParameter(String name, int sourcePosition) {
				sourcePositions.add( sourcePosition );
				super.namedParameter( name, sourcePosition );
			}
		};
		final String sql = prefix + " where id=:id and value=:value";
		ParameterParser.parse( sql, recognizer );
		assertEquals( adjustedPrefix + " where id=? and value=?", recognizer.getAdjustedSqlString() );
		assertEquals( Set.of( "id", "value" ), recognizer.getNamedQueryParameters().keySet() );
		assertEquals( List.of( sql.indexOf( ":id" ), sql.indexOf( ":value" ) ), sourcePositions );
		assertEquals( List.of( adjustedPrefix.length() + 10, adjustedPrefix.length() + 22 ),
				recognizer.getParameterList().stream().map( occurrence -> occurrence.sourcePosition() ).toList() );
	}

	@ParameterizedTest
	@ValueSource(strings = { "'unterminated ?", "\"unterminated ?", "/* unterminated ?" })
	void preservesNativeQueryUnterminatedText(String text) {
		final var recognizer = new ParameterRecognizerImpl();
		final String sql = "select ? " + text;
		ParameterParser.parse( sql, recognizer );
		assertEquals( sql, recognizer.getAdjustedSqlString() );
		assertEquals( 1, recognizer.getParameterList().size() );
	}

	@Test
	void preservesRepeatedNumberedParameters() {
		final var recognizer = new ParameterRecognizerImpl();
		ParameterParser.parse( "select ?1, '?2', ?1, ?2", recognizer );
		assertEquals( "select ?, '?2', ?, ?", recognizer.getAdjustedSqlString() );
		assertEquals( Set.of( 1, 2 ), recognizer.getPositionalQueryParameters().keySet() );
		assertEquals( List.of( 1, 1, 2 ), recognizer.getParameterList().stream()
				.map( occurrence -> occurrence.parameter().getPosition() ).toList() );
	}

	@Test
	void preservesIgnoredJdbcParameters() {
		final var recognizer = new ParameterRecognizerImpl();
		ParameterParser.parse( "select ? where id=:id and value=?1", recognizer, true );
		assertEquals( "select  where id=? and value=?", recognizer.getAdjustedSqlString() );
		assertEquals( Set.of( "id" ), recognizer.getNamedQueryParameters().keySet() );
		assertEquals( Set.of( 1 ), recognizer.getPositionalQueryParameters().keySet() );
		assertEquals( 2, recognizer.getParameterList().size() );
	}

	@Test
	public void testFunctionAsNativeQuery() {
		ExtendedParameterRecognizer recognizer = createRecognizer();

		try {
			NATIVE_QUERY_INTERPRETER.recognizeParameters(
					"{? = call abc()}",
					recognizer
			);
			fail( "Expecting exception" );
		}
		catch (UnsupportedOperationException expected) {
		}

		try {
			NATIVE_QUERY_INTERPRETER.recognizeParameters(
					"{?=call abc()}",
					recognizer
			);
			fail( "Expecting exception" );
		}
		catch (UnsupportedOperationException expected) {
		}

		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"{call abc()}",
				recognizer
		);

		recognizer.validate();
	}

	@Test
	public void testQuotedTextInComment() {
		ExtendedParameterRecognizer recognizer = createRecognizer();

		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"-- 'This' should not fail the test.\n" + "SELECT column FROM Table WHERE column <> :param",
				recognizer
		);

		recognizer.validate();

		assertTrue( recognizer.getNamedParameters().contains( "param" ) );
	}

	@Test
	public void testContractionInComment() {
		ExtendedParameterRecognizer recognizer = createRecognizer();

		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"-- This shouldn't fail the test.\n" + "SELECT column FROM Table WHERE column <> :param",
				recognizer
		);

		recognizer.complete();
		recognizer.validate();

		assertTrue( recognizer.getNamedParameters().contains( "param" ) );
	}

	@Test
	public void testDoubleDashInCharLiteral() {
		ExtendedParameterRecognizer recognizer = createRecognizer();

		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"select coalesce(i.name, '--NONE--') as itname  from Item i where i.intVal=? ",
				recognizer
		);

		recognizer.complete();
		recognizer.validate();

		assertEquals( 1, recognizer.getOrdinalParameterCount() );
	}

	@Test
	public void testSlashStarInCharLiteral() {
		ExtendedParameterRecognizer recognizer = createRecognizer();

		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"select coalesce(i.name, '/*NONE') as itname  from Item i where i.intVal=? ",
				recognizer
		);

		recognizer.complete();
		recognizer.validate();

		assertEquals( 1, recognizer.getOrdinalParameterCount() );
	}

	@Test
	public void testApostropheInOracleAlias() {
		ExtendedParameterRecognizer recognizer = createRecognizer();

		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"SELECT column as \"Table's column\" FROM Table WHERE column <> :param",
				recognizer
		);

		recognizer.complete();
		recognizer.validate();

		assertTrue( recognizer.getNamedParameters().contains( "param" ) );
	}

	@Test
	@JiraKey(value = "HHH-1237")
	public void testParseColonCharacterEscaped() {
		final StringBuilder captured = new StringBuilder();
		ParameterRecognizer recognizer = new ParameterRecognizer() {
			@Override
			public void ordinalParameter(int position) {
				fail();
			}

			@Override
			public void namedParameter(String name, int position) {
				fail();
			}

			@Override
			public void jpaPositionalParameter(int name, int position) {
				fail();
			}

			@Override
			public void other(char character) {
				captured.append( character );
			}

			@Override
			public void complete() {
			}
		};
		ParameterParser.parse( "SELECT @a,(@a::=20) FROM tbl_name", recognizer );
		recognizer.complete();
		assertEquals( "SELECT @a,(@a:=20) FROM tbl_name", captured.toString() );
	}

	@Test
	@JiraKey(value = "HHH-17759")
	public void testParseColonCharacterTypeCasting() {
		final StringBuilder captured = new StringBuilder();
		ParameterRecognizer recognizer = new ParameterRecognizer() {
			@Override
			public void ordinalParameter(int position) {
				// don't care
			}

			@Override
			public void namedParameter(String name, int position) {
				// don't care
			}

			@Override
			public void jpaPositionalParameter(int name, int position) {
				// don't care
			}

			@Override
			public void other(char character) {
				captured.append( character );
			}

			@Override
			public void complete() {
			}

		};
		String expectedQuery = "SELECT column_name::text FROM table_name";

		ParameterParser.parse( "SELECT column_name::text FROM table_name", recognizer );
		recognizer.complete();
		assertEquals( expectedQuery, captured.toString() );

		captured.setLength( 0 ); // clear for new test

		ParameterParser.parse( "SELECT column_name::::text FROM table_name", recognizer );
		recognizer.complete();
		assertEquals( expectedQuery, captured.toString() );
	}

	@Test
	public void testParseNamedParameter() {
		ExtendedParameterRecognizer recognizer = createRecognizer();
		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"from Stock s where s.stockCode = :stockCode and s.xyz = :pxyz",
				recognizer
		);
		recognizer.complete();
		recognizer.validate();

		assertTrue( recognizer.getNamedParameters().contains( "stockCode" ) );
		assertTrue( recognizer.getNamedParameters().contains( "pxyz" ) );
		assertEquals( 2, recognizer.getNamedParameters().size() );
	}

	@Test
	public void testParseNamedParameterEndWithSemicolon() {
		ExtendedParameterRecognizer recognizer = createRecognizer();
		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"from Stock s where s.stockCode = :stockCode and s.xyz = :pxyz;",
				recognizer
		);
		recognizer.complete();
		recognizer.validate();

		assertTrue( recognizer.getNamedParameters().contains( "stockCode" ) );
		assertTrue( recognizer.getNamedParameters().contains( "pxyz" ) );
		assertEquals( 2, recognizer.getNamedParameters().size() );
	}

	@Test
	public void testParseJPAPositionalParameter() {
		ExtendedParameterRecognizer recognizer = createRecognizer();
		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"from Stock s where s.stockCode = ?1 and s.xyz = ?1",
				recognizer
		);
		recognizer.complete();
		recognizer.validate();

		assertEquals( 1, recognizer.getJpaPositionalParameterCount() );

		recognizer = createRecognizer();
		ParameterParser.parse( "from Stock s where s.stockCode = ?1 and s.xyz = ?2", recognizer );
		recognizer.complete();
		recognizer.validate();

		assertEquals( 2, recognizer.getJpaPositionalParameterCount() );
	}

	@Test
	public void testJdbcParameterScanningEnabled() {
		ExtendedParameterRecognizer recognizer = createRecognizer();

		assertThrows(
				ParameterRecognitionException.class,
				() -> {
					NATIVE_QUERY_INTERPRETER.recognizeParameters(
							"SELECT column FROM Table WHERE column.id = :param and column.name = ?1",
							recognizer
					);
					recognizer.validate();
				},
				"Mixed parameter strategies - use just one of named, positional or JPA-ordinal strategy"
		);
	}

	@Test
	public void testJdbcParameterScanningDisabled() {
		ExtendedParameterRecognizer recognizer = createRecognizer();

		// Should recognize the jpa style ordinal parameters
		new NativeQueryInterpreterStandardImpl( true ).recognizeParameters(
				"SELECT column FROM Table WHERE column.id = ?1 and column.name = ?2",
				recognizer
		);
		recognizer.validate();
		assertEquals( 2, recognizer.getJpaPositionalParameterCount() );

		recognizer = createRecognizer();
		// Should ignore the '?'
		new NativeQueryInterpreterStandardImpl( true ).recognizeParameters(
				"SELECT column ? FROM Table WHERE column.id = :id",
				recognizer
		);
		recognizer.validate();
		assertTrue( recognizer.getNamedParameters().contains( "id" ) );
		assertEquals( 0, recognizer.getOrdinalParameterCount() );

	}

	private ExtendedParameterRecognizer createRecognizer() {
		return new TestParameterRecognizer();
	}

	private interface ExtendedParameterRecognizer extends org.hibernate.query.sql.spi.ParameterRecognizer {
		void validate();

		int getOrdinalParameterCount();

		int getJpaPositionalParameterCount();

		Set<String> getNamedParameters();
	}

	private final static class TestParameterRecognizer implements ExtendedParameterRecognizer {
		private int ordinalParameterCount = 0;
		private final Set<Integer> jpaPositionalParameters = new HashSet<>( 2 );
		private final Set<String> namedParameters = new HashSet<>( 2 );

		@Override
		public void ordinalParameter(int sourcePosition) {
			ordinalParameterCount++;
		}

		@Override
		public void namedParameter(String name, int sourcePosition) {
			namedParameters.add( name );
		}

		@Override
		public void jpaPositionalParameter(int label, int sourcePosition) {
			jpaPositionalParameters.add( label );
		}

		@Override
		public void other(char character) {
			// Don't care
		}

		@Override
		public void validate() {
			if ( namedParameters.size() > 0 && ( ordinalParameterCount > 0 || jpaPositionalParameters.size() > 0 ) ) {
				throw mixedParamStrategy();
			}

			if ( ordinalParameterCount > 0 && jpaPositionalParameters.size() > 0 ) {
				throw mixedParamStrategy();
			}
		}

		@Override
		public int getOrdinalParameterCount() {
			return ordinalParameterCount;
		}

		@Override
		public int getJpaPositionalParameterCount() {
			return jpaPositionalParameters.size();
		}

		@Override
		public Set<String> getNamedParameters() {
			return namedParameters;
		}

		private ParameterRecognitionException mixedParamStrategy() {
			throw new ParameterRecognitionException(
					"Mixed parameter strategies - use just one of named, positional or JPA-ordinal strategy" );
		}
	}
}
