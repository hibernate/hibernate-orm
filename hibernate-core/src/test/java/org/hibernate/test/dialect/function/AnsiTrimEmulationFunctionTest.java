/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.dialect.function;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.hibernate.dialect.function.AnsiTrimEmulationFunction;

import static org.junit.Assert.assertEquals;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class AnsiTrimEmulationFunctionTest  {
	private static final String trimSource = "a.column";
    @Test
	public void testBasicSqlServerProcessing() {
		AnsiTrimEmulationFunction function = new AnsiTrimEmulationFunction();

		performBasicSpaceTrimmingTests( function );

		final String expectedTrimPrep = "replace(replace(a.column,' ','${space}$'),'-',' ')";
		final String expectedPostTrimPrefix = "replace(replace(";
		final String expectedPostTrimSuffix = ",' ','-'),'${space}$',' ')";

		// -> trim(LEADING '-' FROM a.column)
		String rendered = function.render( null, argList( "LEADING", "'-'", "FROM", trimSource ), null );
		String expected = expectedPostTrimPrefix + "ltrim(" + expectedTrimPrep + ")" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );

		// -> trim(TRAILING '-' FROM a.column)
		rendered = function.render( null, argList( "TRAILING", "'-'", "FROM", trimSource ), null );
		expected = expectedPostTrimPrefix + "rtrim(" + expectedTrimPrep + ")" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );

		// -> trim(BOTH '-' FROM a.column)
		rendered = function.render( null, argList( "BOTH", "'-'", "FROM", trimSource ), null );
		expected = expectedPostTrimPrefix + "ltrim(rtrim(" + expectedTrimPrep + "))" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );

		// -> trim('-' FROM a.column)
		rendered = function.render( null, argList( "'-'", "FROM", trimSource ), null );
		expected = expectedPostTrimPrefix + "ltrim(rtrim(" + expectedTrimPrep + "))" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );
	}
    @Test
	public void testBasicSybaseProcessing() {
		AnsiTrimEmulationFunction function = new AnsiTrimEmulationFunction(
				AnsiTrimEmulationFunction.LTRIM,
				AnsiTrimEmulationFunction.RTRIM,
				"str_replace"
		);

		performBasicSpaceTrimmingTests( function );

		final String expectedTrimPrep = "str_replace(str_replace(a.column,' ','${space}$'),'-',' ')";
		final String expectedPostTrimPrefix = "str_replace(str_replace(";
		final String expectedPostTrimSuffix = ",' ','-'),'${space}$',' ')";

		// -> trim(LEADING '-' FROM a.column)
		String rendered = function.render( null, argList( "LEADING", "'-'", "FROM", trimSource ), null );
		String expected = expectedPostTrimPrefix + "ltrim(" + expectedTrimPrep + ")" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );

		// -> trim(TRAILING '-' FROM a.column)
		rendered = function.render( null, argList( "TRAILING", "'-'", "FROM", trimSource ), null );
		expected = expectedPostTrimPrefix + "rtrim(" + expectedTrimPrep + ")" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );

		// -> trim(BOTH '-' FROM a.column)
		rendered = function.render( null, argList( "BOTH", "'-'", "FROM", trimSource ), null );
		expected = expectedPostTrimPrefix + "ltrim(rtrim(" + expectedTrimPrep + "))" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );

		// -> trim('-' FROM a.column)
		rendered = function.render( null, argList( "'-'", "FROM", trimSource ), null );
		expected = expectedPostTrimPrefix + "ltrim(rtrim(" + expectedTrimPrep + "))" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );
	}

	private void performBasicSpaceTrimmingTests(AnsiTrimEmulationFunction function) {
		// -> trim(a.column)
		String rendered = function.render( null, argList( trimSource ), null );
		assertEquals( "ltrim(rtrim(a.column))", rendered );

		// -> trim(FROM a.column)
		rendered = function.render( null, argList( "FROM", trimSource ), null );
		assertEquals( "ltrim(rtrim(a.column))", rendered );

		// -> trim(BOTH FROM a.column)
		rendered = function.render( null, argList( "BOTH", "FROM", trimSource ), null );
		assertEquals( "ltrim(rtrim(a.column))", rendered );

		// -> trim(BOTH ' ' FROM a.column)
		rendered = function.render( null, argList( "BOTH", "' '", "FROM", trimSource ), null );
		assertEquals( "ltrim(rtrim(a.column))", rendered );

		// -> trim(LEADING FROM a.column)
		rendered = function.render( null, argList( "LEADING", "FROM", trimSource ), null );
		assertEquals( "ltrim(a.column)", rendered );

		// -> trim(LEADING ' ' FROM a.column)
		rendered = function.render( null, argList( "LEADING", "' '", "FROM", trimSource ), null );
		assertEquals( "ltrim(a.column)", rendered );

		// -> trim(TRAILING FROM a.column)
		rendered = function.render( null, argList( "TRAILING", "FROM", trimSource ), null );
		assertEquals( "rtrim(a.column)", rendered );

		// -> trim(TRAILING ' ' FROM a.column)
		rendered = function.render( null, argList( "TRAILING", "' '", "FROM", trimSource ), null );
		assertEquals( "rtrim(a.column)", rendered );
	}

	private List argList(String... args) {
		return Arrays.asList( args );
	}

}
