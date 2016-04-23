/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.query.spi.ParameterParser;
import org.hibernate.engine.query.spi.ParameterParser.Recognizer;
import org.junit.Test;

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
            public void jpaPositionalParameter(String name, int position) {
                fail();
            }
            @Override
            public void other(char character) {
                captured.append(character);
            }
	    };
	    ParameterParser.parse("SELECT @a,(@a::=20) FROM tbl_name", recognizer);
	    assertEquals("SELECT @a,(@a:=20) FROM tbl_name", captured.toString());
	}
	
    @Test
    public void testParseNamedParameter() {
        final StringBuilder other = new StringBuilder();
        final List<String> params = new ArrayList<String>();
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
                params.add(name);
            }
            @Override
            public void jpaPositionalParameter(String name, int position) {
                fail();
            }
            @Override
            public void other(char character) {
                other.append(character);
            }
        };
        ParameterParser.parse("from Stock s where s.stockCode = :stockCode and s.xyz = :pxyz", recognizer);
        assertEquals("from Stock s where s.stockCode =  and s.xyz = ", other.toString());
        assertEquals("stockCode", params.get(0));
        assertEquals("pxyz", params.get(1));
    }
    
}
