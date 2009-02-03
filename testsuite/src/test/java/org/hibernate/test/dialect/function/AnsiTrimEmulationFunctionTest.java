/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

import java.util.List;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.hibernate.dialect.function.AnsiTrimEmulationFunction;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class AnsiTrimEmulationFunctionTest extends TestCase {
	private static final String trimSource = "a.column";

	public void testBasicSqlServerProcessing() {
		AnsiTrimEmulationFunction function = new AnsiTrimEmulationFunction();

		performBasicSpaceTrimmingTests( function );

		final String expectedTrimPrep = "replace(replace(a.column,' ','${space}$'),'-',' ')";
		final String expectedPostTrimPrefix = "replace(replace(";
		final String expectedPostTrimSuffix = ",' ','-'),'${space}$',' ')";

		// -> trim(LEADING '-' FROM a.column)
		String rendered = function.render( argList( "LEADING", "'-'", "FROM", trimSource ), null );
		String expected = expectedPostTrimPrefix + "ltrim(" + expectedTrimPrep + ")" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );

		// -> trim(TRAILING '-' FROM a.column)
		rendered = function.render( argList( "TRAILING", "'-'", "FROM", trimSource ), null );
		expected = expectedPostTrimPrefix + "rtrim(" + expectedTrimPrep + ")" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );

		// -> trim(BOTH '-' FROM a.column)
		rendered = function.render( argList( "BOTH", "'-'", "FROM", trimSource ), null );
		expected = expectedPostTrimPrefix + "ltrim(rtrim(" + expectedTrimPrep + "))" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );

		// -> trim('-' FROM a.column)
		rendered = function.render( argList( "'-'", "FROM", trimSource ), null );
		expected = expectedPostTrimPrefix + "ltrim(rtrim(" + expectedTrimPrep + "))" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );
	}

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
		String rendered = function.render( argList( "LEADING", "'-'", "FROM", trimSource ), null );
		String expected = expectedPostTrimPrefix + "ltrim(" + expectedTrimPrep + ")" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );

		// -> trim(TRAILING '-' FROM a.column)
		rendered = function.render( argList( "TRAILING", "'-'", "FROM", trimSource ), null );
		expected = expectedPostTrimPrefix + "rtrim(" + expectedTrimPrep + ")" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );

		// -> trim(BOTH '-' FROM a.column)
		rendered = function.render( argList( "BOTH", "'-'", "FROM", trimSource ), null );
		expected = expectedPostTrimPrefix + "ltrim(rtrim(" + expectedTrimPrep + "))" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );

		// -> trim('-' FROM a.column)
		rendered = function.render( argList( "'-'", "FROM", trimSource ), null );
		expected = expectedPostTrimPrefix + "ltrim(rtrim(" + expectedTrimPrep + "))" + expectedPostTrimSuffix;
		assertEquals( expected, rendered );
	}

	private void performBasicSpaceTrimmingTests(AnsiTrimEmulationFunction function) {
		// -> trim(a.column)
		String rendered = function.render( argList( trimSource ), null );
		assertEquals( "ltrim(rtrim(a.column))", rendered );

		// -> trim(FROM a.column)
		rendered = function.render( argList( "FROM", trimSource ), null );
		assertEquals( "ltrim(rtrim(a.column))", rendered );

		// -> trim(BOTH FROM a.column)
		rendered = function.render( argList( "BOTH", "FROM", trimSource ), null );
		assertEquals( "ltrim(rtrim(a.column))", rendered );

		// -> trim(BOTH ' ' FROM a.column)
		rendered = function.render( argList( "BOTH", "' '", "FROM", trimSource ), null );
		assertEquals( "ltrim(rtrim(a.column))", rendered );

		// -> trim(LEADING FROM a.column)
		rendered = function.render( argList( "LEADING", "FROM", trimSource ), null );
		assertEquals( "ltrim(a.column)", rendered );

		// -> trim(LEADING ' ' FROM a.column)
		rendered = function.render( argList( "LEADING", "' '", "FROM", trimSource ), null );
		assertEquals( "ltrim(a.column)", rendered );

		// -> trim(TRAILING FROM a.column)
		rendered = function.render( argList( "TRAILING", "FROM", trimSource ), null );
		assertEquals( "rtrim(a.column)", rendered );

		// -> trim(TRAILING ' ' FROM a.column)
		rendered = function.render( argList( "TRAILING", "' '", "FROM", trimSource ), null );
		assertEquals( "rtrim(a.column)", rendered );
	}

	private List argList(String arg) {
		ArrayList args = new ArrayList();
		args.add( arg );
		return args;
	}

	private List argList(String arg1, String arg2) {
		ArrayList args = new ArrayList();
		args.add( arg1 );
		args.add( arg2 );
		return args;
	}

	private List argList(String arg1, String arg2, String arg3) {
		ArrayList args = new ArrayList();
		args.add( arg1 );
		args.add( arg2 );
		args.add( arg3 );
		return args;
	}

	private List argList(String arg1, String arg2, String arg3, String arg4) {
		ArrayList args = new ArrayList();
		args.add( arg1 );
		args.add( arg2 );
		args.add( arg3 );
		args.add( arg4 );
		return args;
	}

}
