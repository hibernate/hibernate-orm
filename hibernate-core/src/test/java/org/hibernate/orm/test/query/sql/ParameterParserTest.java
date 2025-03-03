/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sql;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.query.ParameterRecognitionException;
import org.hibernate.engine.query.internal.NativeQueryInterpreterStandardImpl;
import org.hibernate.query.sql.internal.ParameterParser;
import org.hibernate.query.sql.spi.ParameterRecognizer;

import org.hibernate.testing.orm.junit.JiraKey;

import org.junit.jupiter.api.Test;

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
