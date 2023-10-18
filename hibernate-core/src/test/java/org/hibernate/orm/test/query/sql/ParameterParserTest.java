/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sql;

import org.hibernate.engine.query.ParameterRecognitionException;
import org.hibernate.engine.query.internal.NativeQueryInterpreterStandardImpl;
import org.hibernate.engine.query.spi.ParamLocationRecognizer;
import org.hibernate.query.sql.internal.ParameterParser;
import org.hibernate.query.sql.spi.ParameterRecognizer;

import org.hibernate.testing.TestForIssue;
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
		ParamLocationRecognizer recognizer = createRecognizer();

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
		ParamLocationRecognizer recognizer = createRecognizer();

		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"-- 'This' should not fail the test.\n" + "SELECT column FROM Table WHERE column <> :param",
				recognizer
		);

		recognizer.validate();

		assertTrue(recognizer.getNamedParameterDescriptionMap().containsKey("param"));
	}

	private ParamLocationRecognizer createRecognizer() {
		return new ParamLocationRecognizer( 1 );
	}

	@Test
	public void testContractionInComment() {
		ParamLocationRecognizer recognizer = createRecognizer();

		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"-- This shouldn't fail the test.\n" + "SELECT column FROM Table WHERE column <> :param",
				recognizer
		);

		recognizer.complete();
		recognizer.validate();

		assertTrue( recognizer.getNamedParameterDescriptionMap().containsKey("param"));
	}

	@Test
	public void testDoubleDashInCharLiteral() {
		ParamLocationRecognizer recognizer = createRecognizer();

		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"select coalesce(i.name, '--NONE--') as itname  from Item i where i.intVal=? ",
				recognizer
		);

		recognizer.complete();
		recognizer.validate();

		assertEquals( 1, recognizer.getOrdinalParameterDescriptionMap().size() );
	}

	@Test
	public void testSlashStarInCharLiteral() {
		ParamLocationRecognizer recognizer = createRecognizer();

		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"select coalesce(i.name, '/*NONE') as itname  from Item i where i.intVal=? ",
				recognizer
		);

		recognizer.complete();
		recognizer.validate();

		assertEquals( 1, recognizer.getOrdinalParameterDescriptionMap().size() );
	}

	@Test
	public void testApostropheInOracleAlias() {
		ParamLocationRecognizer recognizer = createRecognizer();

		NATIVE_QUERY_INTERPRETER.recognizeParameters(
				"SELECT column as \"Table's column\" FROM Table WHERE column <> :param",
				recognizer
		);

		recognizer.complete();
		recognizer.validate();

		assertTrue(recognizer.getNamedParameterDescriptionMap().containsKey("param"));
	}
	
    @Test
	@TestForIssue( jiraKey = "HHH-1237")
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
                captured.append(character);
            }

			@Override
			public void complete() {
			}
		};
        ParameterParser.parse("SELECT @a,(@a::=20) FROM tbl_name", recognizer);
		recognizer.complete();
        assertEquals("SELECT @a,(@a:=20) FROM tbl_name", captured.toString());
    }
    
    @Test
    public void testParseNamedParameter() {
        ParamLocationRecognizer recognizer = createRecognizer();
		NATIVE_QUERY_INTERPRETER.recognizeParameters("from Stock s where s.stockCode = :stockCode and s.xyz = :pxyz", recognizer);
		recognizer.complete();
		recognizer.validate();

        assertTrue(recognizer.getNamedParameterDescriptionMap().containsKey("stockCode"));
        assertTrue(recognizer.getNamedParameterDescriptionMap().containsKey("pxyz"));
        assertEquals( 2, recognizer.getNamedParameterDescriptionMap().size() );
    }
    
    @Test
    public void testParseJPAPositionalParameter() {
        ParamLocationRecognizer recognizer = createRecognizer();
		NATIVE_QUERY_INTERPRETER.recognizeParameters("from Stock s where s.stockCode = ?1 and s.xyz = ?1", recognizer);
		recognizer.complete();
		recognizer.validate();

        assertEquals( 1, recognizer.getOrdinalParameterDescriptionMap().size() );
        
        ParameterParser.parse("from Stock s where s.stockCode = ?1 and s.xyz = ?2", recognizer);
		recognizer.complete();
		recognizer.validate();

        assertEquals( 2, recognizer.getOrdinalParameterDescriptionMap().size() );
    }

	@Test
	public void testJdbcParameterScanningEnabled() {
		ParamLocationRecognizer recognizer = createRecognizer();

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
		ParamLocationRecognizer recognizer = createRecognizer();

		// Should recognize the jpa style ordinal parameters
		new NativeQueryInterpreterStandardImpl( true ).recognizeParameters(
				"SELECT column FROM Table WHERE column.id = ?1 and column.name = ?2",
				recognizer
		);
		recognizer.validate();
		assertEquals( 2, recognizer.getOrdinalParameterDescriptionMap().size() );

		recognizer = createRecognizer();
		// Should ignore the '?'
		new NativeQueryInterpreterStandardImpl( true ).recognizeParameters(
				"SELECT column ? FROM Table WHERE column.id = :id",
				recognizer
		);
		recognizer.validate();
		assertTrue(recognizer.getNamedParameterDescriptionMap().containsKey("id"));
		assertEquals( 0, recognizer.getOrdinalParameterDescriptionMap().size() );

	}
}
