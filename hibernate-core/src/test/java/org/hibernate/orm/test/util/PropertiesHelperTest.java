/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.util;

import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class PropertiesHelperTest {
	private Properties props;

	@BeforeEach
	public void setUp() {
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

	@Test
	public void testPlaceholderReplacement() {
		ConfigurationHelper.resolvePlaceHolders( props );

		String str = ConfigurationHelper.getString( "my.nonexistent.prop", props, "did.not.exist" );
		assertEquals( "did.not.exist", str );
		str = ConfigurationHelper.getString( "my.nonexistent.prop", props );
		assertNull( str );
		str = ConfigurationHelper.getString( "my.string.prop", props, "na" );
		assertEquals( "string", str, "replacement did not occur" );
		str = ConfigurationHelper.getString( "my.string.prop", props, "did.not.exist" );
		assertEquals( "string", str, "replacement did not occur" );

		boolean bool = ConfigurationHelper.getBoolean( "my.nonexistent.prop", props );
		assertFalse( bool, "non-exists as boolean" );
		bool = ConfigurationHelper.getBoolean( "my.nonexistent.prop", props, false );
		assertFalse( bool, "non-exists as boolean" );
		bool = ConfigurationHelper.getBoolean( "my.nonexistent.prop", props, true );
		assertTrue( bool, "non-exists as boolean" );
		bool = ConfigurationHelper.getBoolean( "my.boolean.prop", props );
		assertTrue( bool, "boolean replacement did not occur" );
		bool = ConfigurationHelper.getBoolean( "my.boolean.prop", props, false );
		assertTrue( bool, "boolean replacement did not occur" );

		int i = ConfigurationHelper.getInt( "my.nonexistent.prop", props, -1 );
		assertEquals( -1, i );
		i = ConfigurationHelper.getInt( "my.int.prop", props, 100 );
		assertEquals( 1, i );

		Integer I = ConfigurationHelper.getInteger( "my.nonexistent.prop", props );
		assertNull( I );
		I = ConfigurationHelper.getInteger( "my.integer.prop", props );
		assertEquals( Integer.valueOf( 1 ), I );

		str = props.getProperty( "partial.prop1" );
		assertEquals( "tmp/middle/dir/tmp.txt", str, "partial replacement (ends)" );

		str = props.getProperty( "partial.prop2" );
		assertEquals( "basedir/tmp/myfile.txt", str, "partial replacement (midst)" );
	}

	@Test
	public void testParseExceptions() {
		boolean b = ConfigurationHelper.getBoolean( "parse.error", props );
		assertFalse( b, "parse exception case - boolean" );

		try {
			ConfigurationHelper.getInt( "parse.error", props, 20 );
			fail( "parse exception case - int" );
		}
		catch( NumberFormatException expected ) {
		}

		try {
			ConfigurationHelper.getInteger( "parse.error", props );
			fail( "parse exception case - Integer" );
		}
		catch( NumberFormatException expected ) {
		}
	}
}
