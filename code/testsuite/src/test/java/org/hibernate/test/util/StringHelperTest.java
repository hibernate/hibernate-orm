package org.hibernate.test.util;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.junit.UnitTestCase;
import org.hibernate.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class StringHelperTest extends UnitTestCase {

	public StringHelperTest(String string) {
		super( string );
	}

	public static Test suite() {
		return new TestSuite( StringHelperTest.class );
	}

	public void testAliasGeneration() {
		assertSimpleAlias( "xyz", "xyz_" );
		assertSimpleAlias( "_xyz", "xyz_" );
		assertSimpleAlias( "!xyz", "xyz_" );
		assertSimpleAlias( "abcdefghijklmnopqrstuvwxyz", "abcdefghij_" );
	}

	private void assertSimpleAlias(String source, String expected) {
		assertEquals( expected, StringHelper.generateAlias( source ) );
	}
}
