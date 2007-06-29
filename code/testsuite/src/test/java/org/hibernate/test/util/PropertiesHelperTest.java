package org.hibernate.test.util;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.junit.UnitTestCase;
import org.hibernate.util.PropertiesHelper;

/**
 * @author Steve Ebersole
 */
public class PropertiesHelperTest extends UnitTestCase {

	private Properties props;

	public PropertiesHelperTest(String string) {
		super( string );
	}

	public static Test suite() {
		return new TestSuite( PropertiesHelperTest.class );
	}

	protected void setUp() throws Exception {
		props = new Properties();

		props.setProperty( "my.nonexistent.prop", "${}" );

		props.setProperty( "my.string.prop", "${test.my.sys.string.prop}" );
		System.setProperty( "test.my.sys.string.prop", "string" );

		props.setProperty( "my.boolean.prop", "${test.my.sys.boolean.prop}" );
		System.setProperty( "test.my.sys.boolean.prop", "true" );

		props.setProperty( "my.int.prop", "${test.my.sys.int.prop}" );
		System.setProperty( "test.my.sys.int.prop", "1" );

		props.setProperty( "my.integer.prop", "${test.my.sys.integer.prop}" );
		System.setProperty( "test.my.sys.integer.prop", "1" );

		props.setProperty( "partial.prop1", "${somedir}/middle/dir/${somefile}" );
		props.setProperty( "partial.prop2", "basedir/${somedir}/myfile.txt" );
		System.setProperty( "somedir", "tmp" );
		System.setProperty( "somefile", "tmp.txt" );

		props.setProperty( "parse.error", "steve" );
	}

	public void testPlaceholderReplacement() {
		PropertiesHelper.resolvePlaceHolders( props );

		String str = PropertiesHelper.getString( "my.nonexistent.prop", props, "did.not.exist" );
		assertEquals( "did.not.exist", str );
		str = PropertiesHelper.getString( "my.nonexistent.prop", props, null );
		assertNull( str );
		str = PropertiesHelper.getString( "my.string.prop", props, "na" );
		assertEquals( "replacement did not occur", "string", str );
		str = PropertiesHelper.getString( "my.string.prop", props, "did.not.exist" );
		assertEquals( "replacement did not occur", "string", str );

		boolean bool = PropertiesHelper.getBoolean( "my.nonexistent.prop", props );
		assertFalse( "non-exists as boolean", bool );
		bool = PropertiesHelper.getBoolean( "my.nonexistent.prop", props, false );
		assertFalse( "non-exists as boolean", bool );
		bool = PropertiesHelper.getBoolean( "my.nonexistent.prop", props, true );
		assertTrue( "non-exists as boolean", bool );
		bool = PropertiesHelper.getBoolean( "my.boolean.prop", props );
		assertTrue( "boolean replacement did not occur", bool );
		bool = PropertiesHelper.getBoolean( "my.boolean.prop", props, false );
		assertTrue( "boolean replacement did not occur", bool );

		int i = PropertiesHelper.getInt( "my.nonexistent.prop", props, -1 );
		assertEquals( -1, i );
		i = PropertiesHelper.getInt( "my.int.prop", props, 100 );
		assertEquals( 1, i );

		Integer I = PropertiesHelper.getInteger( "my.nonexistent.prop", props );
		assertNull( I );
		I = PropertiesHelper.getInteger( "my.integer.prop", props );
		assertEquals( I, new Integer( 1 ) );

		str = props.getProperty( "partial.prop1" );
		assertEquals( "partial replacement (ends)", "tmp/middle/dir/tmp.txt", str );

		str = props.getProperty( "partial.prop2" );
		assertEquals( "partial replacement (midst)", "basedir/tmp/myfile.txt", str );
	}

	public void testParseExceptions() {
		boolean b = PropertiesHelper.getBoolean( "parse.error", props );
		assertFalse( "parse exception case - boolean", b );

		try {
			PropertiesHelper.getInt( "parse.error", props, 20 );
			fail( "parse exception case - int" );
		}
		catch( NumberFormatException expected ) {
		}

		try {
			PropertiesHelper.getInteger( "parse.error", props );
			fail( "parse exception case - Integer" );
		}
		catch( NumberFormatException expected ) {
		}
	}
}
