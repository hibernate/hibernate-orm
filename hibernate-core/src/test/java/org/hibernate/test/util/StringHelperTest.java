/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import java.util.BitSet;
import java.util.Iterator;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Steve Ebersole
 * @author Scott Buchanan
 */
public class StringHelperTest extends BaseUnitTestCase {

	private static final String BASE_PACKAGE = "org.hibernate";
	private static final String STRING_HELPER_FQN = "org.hibernate.internal.util.StringHelper";
	private static final String STRING_HELPER_NAME = StringHelper.unqualify( STRING_HELPER_FQN );

	@Test
	public void testNameCollapsing() {
		assertNull( StringHelper.collapse( null ) );
		assertEquals( STRING_HELPER_NAME, StringHelper.collapse( STRING_HELPER_NAME ) );
		assertEquals( "o.h.i.u.StringHelper", StringHelper.collapse( STRING_HELPER_FQN ) );
	}

	@Test
	public void testPartialNameUnqualification() {
		assertNull( StringHelper.partiallyUnqualify( null, BASE_PACKAGE ) );
		assertEquals( STRING_HELPER_NAME, StringHelper.partiallyUnqualify( STRING_HELPER_NAME, BASE_PACKAGE ) );
		assertEquals( "internal.util.StringHelper", StringHelper.partiallyUnqualify( STRING_HELPER_FQN, BASE_PACKAGE ) );
	}

	@Test
	public void testIsBlank() {
		assertFalse( StringHelper.isBlank( "A" ) );
		assertFalse( StringHelper.isBlank( " a" ) );
		assertFalse( StringHelper.isBlank( "a " ) );
		assertFalse( StringHelper.isBlank( "a\t" ) );
		assertTrue( StringHelper.isBlank( "\t\n" ) );
		assertTrue( StringHelper.isBlank( null ) );
		assertTrue( StringHelper.isBlank( "" ) );
		assertTrue( StringHelper.isBlank( "     " ) );
	}

	@Test
	public void testBasePackageCollapsing() {
		assertNull( StringHelper.collapseQualifierBase( null, BASE_PACKAGE ) );
		assertEquals( STRING_HELPER_NAME, StringHelper.collapseQualifierBase( STRING_HELPER_NAME, BASE_PACKAGE ) );
		assertEquals( "o.h.internal.util.StringHelper", StringHelper.collapseQualifierBase( STRING_HELPER_FQN, BASE_PACKAGE ) );
	}

	@Test
	public void testFindIdentifierWord() {
		assertEquals( StringHelper.indexOfIdentifierWord( "", "word" ), -1 );
		assertEquals( StringHelper.indexOfIdentifierWord( null, "word" ), -1 );
		assertEquals( StringHelper.indexOfIdentifierWord( "sentence", null ), -1 );
		assertEquals( StringHelper.indexOfIdentifierWord( "where name=?13 and description=?1", "?1" ), 31 );
		assertEquals( StringHelper.indexOfIdentifierWord( "where name=?13 and description=?1 and category_id=?4", "?1" ), 31 );
		assertEquals( StringHelper.indexOfIdentifierWord( "?1", "?1" ), 0 );
		assertEquals( StringHelper.indexOfIdentifierWord( "no identifier here", "?1" ), -1 );
		assertEquals( StringHelper.indexOfIdentifierWord( "some text ?", "?" ), 10 );
	}

	private static H2Dialect DIALECT = new H2Dialect();
	@Test
	public void testArrayUnquoting() {
		assertNull( StringHelper.unquote( (String[]) null, DIALECT ) );
		//This to verify that the string array isn't being copied unnecessarily:
		unchanged( new String [0] );
		unchanged( new String[] { "a" } );
		unchanged( new String[] { "a", "b" } );
		helperEquals( new String[] { "a", "b", "c" }, new String[] { "a", "b", "`c`" } );
		helperEquals( new String[] { "a", "b", "c" }, new String[] { "a", "\"b\"", "c" } );
	}

	private static void unchanged(String[] input) {
		final String[] output = StringHelper.unquote( input, DIALECT );
		assertTrue( input == output );
	}

	private static void helperEquals(String[] expectation, String[] input) {
		final String[] output = StringHelper.unquote( input, DIALECT );
		assertTrue( Arrays.equals( expectation, output ) );
	}

	@Test
	public void testIsQuotedWithDialect() {
		Assert.assertFalse( StringHelper.isQuoted( "a", DIALECT ) );
		Assert.assertTrue( StringHelper.isQuoted( "`a`", DIALECT ) );

		//This dialect has a different "open" than "close" quoting symbol:
		final SQLServerDialect sqlServerDialect = new SQLServerDialect();
		Assert.assertTrue( StringHelper.isQuoted( "[a]", sqlServerDialect ) );
		Assert.assertFalse( StringHelper.isQuoted( "`a]", sqlServerDialect ) );
		Assert.assertFalse( StringHelper.isQuoted( "[a`", sqlServerDialect ) );
		Assert.assertFalse( StringHelper.isQuoted( "\"a`", sqlServerDialect ) );
		Assert.assertFalse( StringHelper.isQuoted( "`a\"", sqlServerDialect ) );
		Assert.assertFalse( StringHelper.isQuoted( "a", sqlServerDialect ) );
	}

	@Test
	public void replaceRepeatingPlaceholdersWithoutStackOverflow() {
		String ordinalParameters = generateOrdinalParameters( 3, 19999 );
		String result = StringHelper.replace(
				"select * from books where category in (?1) and id in(" + ordinalParameters + ") and parent_category in (?1) and id in(" + ordinalParameters + ")",
				"?1", "?1, ?2", true, true );
		assertEquals( "select * from books where category in (?1, ?2) and id in(" + ordinalParameters + ") and parent_category in (?1, ?2) and id in(" + ordinalParameters + ")", result );
	}

	private String generateOrdinalParameters(int startPosition, int endPosition) {
		StringBuilder builder = new StringBuilder();
		for ( int i = startPosition; i <= endPosition; i++ ) {
			builder.append( '?' ).append( i );
			if ( i < endPosition ) {
				builder.append( ", " );
			}
		}
		return builder.toString();
	}

  @Test
  public void testCountUnquotedReturnsOne() {
		assertEquals(1, StringHelper.countUnquoted("1", '1'));
		assertEquals(0, StringHelper.countUnquoted("1", '\u0000'));
		assertEquals(0, StringHelper.countUnquoted("\'", '\u0000'));
		assertEquals(0, StringHelper.countUnquoted("a\'b\'c", '\u0000'));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCountUnquotedIllegalArgumentException() {
		StringHelper.countUnquoted("\'", '\'');
		// Method is not expected to return due to exception thrown
	}

	@Test
	public void testFirstIndexOfCharReturnsNegativeOneIfNotFound() {
		assertEquals(-1, StringHelper.firstIndexOfChar("a\'b\'c", new BitSet(), 4));
	}

	@Test
	public void testGenerateAliasNonDigit() {
		assertEquals(",_", StringHelper.generateAlias(","));
		assertEquals("2x_", StringHelper.generateAlias("2"));
		assertEquals("w_", StringHelper.generateAlias("W\u802f/"));
	}

	@Test
	public void testGetFirstNonWhitespaceCharacter() {
		assertEquals('\'', StringHelper.getFirstNonWhitespaceCharacter("\'"));
	}

	@Test
	public void testGetLastNonWhitespaceCharacter() {
		assertEquals('o', StringHelper.getLastNonWhitespaceCharacter("foo"));
	}

	@Test
	public void testIsEmptyOrWhiteSpaceReturnsFalse() {
		assertFalse(StringHelper.isEmptyOrWhiteSpace("BAZ"));
	}

	@Test
	public void testIsQuotedReturnsFalse1() {
		assertFalse(StringHelper.isQuoted("a/b/c"));
		assertFalse(StringHelper.isQuoted("\"````"));
		assertTrue(StringHelper.isQuoted("\"\"\"\"\""));
	}

	@Test
	public void testLastIndexOfLetterReturnsTwo() {
		assertEquals(2, StringHelper.lastIndexOfLetter("foo"));
		assertEquals(0, StringHelper.lastIndexOfLetter("a\'b\'c"));
		assertEquals(-1, StringHelper.lastIndexOfLetter("\'"));
	}

	@Test
	public void testLocateUnquotedReturnsEmptyArray() {
		assertArrayEquals(new int[]{}, StringHelper.locateUnquoted("\'", '%'));
		assertArrayEquals(new int[]{0}, StringHelper.locateUnquoted(",", ','));
		assertArrayEquals(new int[]{}, StringHelper.locateUnquoted("a\'b\'c", '\u0000'));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLocateUnquotedThrowsIllegalArgumentException() {
		StringHelper.locateUnquoted("a,b,c", '\'');
		// Method is not expected to return due to exception thrown
	}

	@Test
	public void testPartiallyUnqualifyNoBaseReturnsSameString() {
		assertEquals("a\'b\'c", StringHelper.partiallyUnqualify("a\'b\'c", "3"));
		assertEquals("34", StringHelper.partiallyUnqualify("1234", "1"));
	}

	@Test(expected = StringIndexOutOfBoundsException.class)
	public void testPartiallyUnqualifyThrowsStringIndexOutOfBoundsException() {
		StringHelper.partiallyUnqualify("a\'b\'c", "a\'b\'c");
		// Method is not expected to return due to exception thrown
	}

	@Test
	public void testQuote() {
		assertEquals("`3`", StringHelper.quote("3"));
	}

	@Test
	public void testReplaceOnce() {
		assertEquals("a,b,c", StringHelper.replaceOnce("3", "3", "a,b,c"));
		assertEquals("a\'b\'c", StringHelper.replaceOnce("a\'b\'c", "1234", "a,b,c"));
	}

	@Test
	public void testJoinWithQualifiersAndSuffix() {
		assertEquals("a.foobca.barbca.bazb", StringHelper.joinWithQualifierAndSuffix(new String[]{"foo", "bar", "baz"}, "a", "b", "c"));
		assertEquals("", StringHelper.joinWithQualifierAndSuffix(new String[0], "foo", "bar", "BAZ"));
	}

	@Test
	public void testJoin() {
		Iterator<?> objects = ((ArrayList<?>) new ArrayList<String>() {
			{
				add("foo");
				add("bar");
				add("baz");
			}
		}).iterator();
		assertEquals("fooabarabaz", StringHelper.join("a", objects));
	}

	@Test
	public void testAdd() {
		assertEquals(new String[]{"fooa", "barb", "bazc"}, StringHelper.add(new String[]{"foo", "bar", "baz"}, "", new String[]{"a", "b", "c"}));
		assertEquals(new String[]{"fooa"}, StringHelper.add(new String[]{""}, "foo", new String[]{"a", "b", "c"}));
	}

	@Test
	public void testRepeat() {
		assertEquals("", StringHelper.repeat("foo", 0));
		assertEquals("bar", StringHelper.repeat("bar", 1));
	}

	@Test
	public void testSplitTrimmingTokens() {
		assertEquals(new String[]{"foo", "-", "bar", "-", "baz"}, StringHelper.splitTrimmingTokens("-", "foo-bar-baz", true));
		assertEquals(new String[]{"foo", "bar", "baz"}, StringHelper.splitTrimmingTokens("-", "foo-bar-baz", false));
	}
}
