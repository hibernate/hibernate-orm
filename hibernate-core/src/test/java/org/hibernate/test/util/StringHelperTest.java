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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
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

}
