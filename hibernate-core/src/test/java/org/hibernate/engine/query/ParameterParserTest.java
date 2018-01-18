/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query;

import org.hibernate.engine.query.spi.ParamLocationRecognizer;
import org.hibernate.engine.query.spi.ParameterParser;
import org.hibernate.engine.query.spi.ParameterParser.Recognizer;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests of the ParameterParser class
 *
 * @author Steve Ebersole
 */
public class ParameterParserTest {
	@Test
	public void testEscapeCallRecognition() {
		assertTrue( ParameterParser.startsWithEscapeCallTemplate( "{ ? = call abc(?) }" ) );
		assertFalse( ParameterParser.startsWithEscapeCallTemplate(
				"from User u where u.userName = ? and u.userType = 'call'"
		) );
	}
	@Test
	public void testQuotedTextInComment() {
		ParamLocationRecognizer recognizer = new ParamLocationRecognizer( 0 );

		ParameterParser.parse("-- 'This' should not fail the test.\n"
									  + "SELECT column FROM Table WHERE column <> :param", recognizer);

		assertTrue(recognizer.getNamedParameterDescriptionMap().containsKey("param"));
	}

	@Test
	public void testContractionInComment() {
		ParamLocationRecognizer recognizer = new ParamLocationRecognizer( 0 );

		ParameterParser.parse("-- This shouldn't fail the test.\n" + "SELECT column FROM Table WHERE column <> :param",
							  recognizer);

		assertTrue( recognizer.getNamedParameterDescriptionMap().containsKey("param"));
	}

	@Test
	public void testDoubleDashInCharLiteral() {
		ParamLocationRecognizer recognizer = new ParamLocationRecognizer( 0 );

		ParameterParser.parse("select coalesce(i.name, '--NONE--') as itname  from Item i where i.intVal=? ",recognizer);

		assertEquals( 1, recognizer.getOrdinalParameterDescriptionMap().size() );
	}

	@Test
	public void testSlashStarInCharLiteral() {
		ParamLocationRecognizer recognizer = new ParamLocationRecognizer( 0 );

		ParameterParser.parse("select coalesce(i.name, '/*NONE') as itname  from Item i where i.intVal=? ",recognizer);

		assertEquals( 1, recognizer.getOrdinalParameterDescriptionMap().size() );
	}

	@Test
	public void testApostropheInOracleAlias() {
		ParamLocationRecognizer recognizer = new ParamLocationRecognizer( 0 );

		ParameterParser.parse("SELECT column as \"Table's column\" FROM Table WHERE column <> :param", recognizer);

		assertTrue(recognizer.getNamedParameterDescriptionMap().containsKey("param"));
	}
	
    @Test
	@TestForIssue( jiraKey = "HHH-1237")
    public void testParseColonCharacterEscaped() {
        final StringBuilder captured = new StringBuilder();
        Recognizer recognizer = new Recognizer() {
            @Override
            public void outParameter(int position) {
                fail();
            }

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
        assertEquals("SELECT @a,(@a:=20) FROM tbl_name", captured.toString());
    }
    
    @Test
    public void testParseNamedParameter() {
        ParamLocationRecognizer recognizer = new ParamLocationRecognizer( 0 );
        ParameterParser.parse("from Stock s where s.stockCode = :stockCode and s.xyz = :pxyz", recognizer);
        assertTrue(recognizer.getNamedParameterDescriptionMap().containsKey("stockCode"));
        assertTrue(recognizer.getNamedParameterDescriptionMap().containsKey("pxyz"));
        assertEquals( 2, recognizer.getNamedParameterDescriptionMap().size() );
    }
    
    @Test
    public void testParseJPAPositionalParameter() {
        ParamLocationRecognizer recognizer = new ParamLocationRecognizer( 0 );
        ParameterParser.parse("from Stock s where s.stockCode = ?1 and s.xyz = ?1", recognizer);
        assertEquals( 1, recognizer.getOrdinalParameterDescriptionMap().size() );
        
        ParameterParser.parse("from Stock s where s.stockCode = ?1 and s.xyz = ?2", recognizer);
        assertEquals( 2, recognizer.getOrdinalParameterDescriptionMap().size() );
    }

}
