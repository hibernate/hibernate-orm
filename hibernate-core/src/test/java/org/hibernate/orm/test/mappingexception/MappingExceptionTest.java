/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mappingexception;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.hibernate.Hibernate;
import org.hibernate.InvalidMappingException;
import org.hibernate.MappingException;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.ConfigHelper;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for various mapping exceptions thrown when mappings are not found or invalid.
 *
 * @author Max Rydahl Andersen
 */
@BaseUnitTest
public class MappingExceptionTest {
	@Test
	public void testNotFound() throws MappingException, MalformedURLException {
		Configuration cfg = new Configuration();

		try {
			cfg.addCacheableFile( "completelybogus.hbm.xml" );
			fail();
		}
		catch (org.hibernate.boot.MappingNotFoundException e) {
			assertEquals( SourceType.FILE, e.getOrigin().getType() );
			assertEquals( "completelybogus.hbm.xml", e.getOrigin().getName() );
		}

		try {
			cfg.addCacheableFile( new File( "completelybogus.hbm.xml" ) );
			fail();
		}
		catch (org.hibernate.boot.MappingNotFoundException e) {
			assertEquals( SourceType.FILE, e.getOrigin().getType() );
			assertEquals( "completelybogus.hbm.xml", e.getOrigin().getName() );
		}

		try {
			cfg.addClass( Hibernate.class ); // TODO: String.class result in npe, because no classloader exists for it
			fail();
		}
		catch (org.hibernate.boot.MappingNotFoundException e) {
			assertEquals( SourceType.RESOURCE, e.getOrigin().getType() );
			assertEquals( "org/hibernate/Hibernate.hbm.xml", e.getOrigin().getName() );
		}

		try {
			cfg.addFile( "completelybogus.hbm.xml" );
			fail();
		}
		catch (org.hibernate.boot.MappingNotFoundException e) {
			assertEquals( SourceType.FILE, e.getOrigin().getType() );
			assertEquals( "completelybogus.hbm.xml", e.getOrigin().getName() );
		}

		try {
			cfg.addFile( new File( "completelybogus.hbm.xml" ) );
			fail();
		}
		catch (org.hibernate.boot.MappingNotFoundException e) {
			assertEquals( SourceType.FILE, e.getOrigin().getType() );
			assertEquals( "completelybogus.hbm.xml", e.getOrigin().getName() );
		}

		try {
			cfg.addInputStream( new ByteArrayInputStream( new byte[0] ) );
			fail();
		}
		catch (org.hibernate.boot.InvalidMappingException e) {
			assertEquals( SourceType.INPUT_STREAM, e.getOrigin().getType() );
			assertNull( null, e.getOrigin().getName() );
		}
		catch (InvalidMappingException inv) {
			assertEquals( "input stream", inv.getType() );
			assertNull( inv.getPath() );
		}

		try {
			cfg.addResource( "nothere" );
			fail();
		}
		catch (org.hibernate.boot.MappingNotFoundException e) {
			assertEquals( SourceType.RESOURCE, e.getOrigin().getType() );
			assertEquals( "nothere", e.getOrigin().getName() );
		}

		try {
			cfg.addURL( new URL( "file://nothere" ) );
			fail();
		}
		catch (org.hibernate.boot.MappingNotFoundException e) {
			assertEquals( SourceType.URL, e.getOrigin().getType() );
			assertEquals( "file://nothere", e.getOrigin().getName() );
		}
		catch (InvalidMappingException inv) {
			assertEquals( "URL", inv.getType() );
			assertEquals( "file://nothere", inv.getPath() );
		}
		catch (org.hibernate.boot.MappingException me) {
			assertEquals( SourceType.URL, me.getOrigin().getType() );
			assertEquals( "file://nothere", me.getOrigin().getName() );
		}
	}

	@Test
	public void testInvalidMapping() throws MappingException, IOException {
		String resourceName = "org/hibernate/orm/test/mappingexception/InvalidMapping.hbm.xml";
		File file = File.createTempFile( "TempInvalidMapping", ".hbm.xml" );
		file.deleteOnExit();
		copy( ConfigHelper.getConfigStream( resourceName ), file );

		Configuration cfg = new Configuration();
		try {
			cfg.addCacheableFile( file.getAbsolutePath() );
			fail();
		}
		catch (InvalidMappingException inv) {
			assertEquals( "file", inv.getType() );
			assertNotNull( inv.getPath() );
			assertTrue( inv.getPath().endsWith( ".hbm.xml" ) );
		}

		try {
			cfg.addCacheableFile( file );
			fail();
		}
		catch (InvalidMappingException inv) {
			assertEquals( "file", inv.getType() );
			assertNotNull( inv.getPath() );
			assertTrue( inv.getPath().endsWith( ".hbm.xml" ) );
		}

		try {
			cfg.addClass( InvalidMapping.class );
			fail();
		}
		catch (InvalidMappingException inv) {
			assertEquals( "resource", inv.getType() );
			assertEquals( "org/hibernate/orm/test/mappingexception/InvalidMapping.hbm.xml", inv.getPath() );
		}

		try {
			cfg.addFile( file.getAbsolutePath() );
			fail();
		}
		catch (InvalidMappingException inv) {
			assertEquals( "file", inv.getType() );
			assertEquals( file.getPath(), inv.getPath() );
		}

		try {
			cfg.addFile( file );
			fail();
		}
		catch (InvalidMappingException inv) {
			assertEquals( "file", inv.getType() );
			assertEquals( file.getPath(), inv.getPath() );
		}

		try {
			cfg.addInputStream( ConfigHelper.getResourceAsStream( resourceName ) );
			fail();
		}
		catch (InvalidMappingException inv) {
			assertEquals( "input stream", inv.getType() );
			assertNull( inv.getPath() );
		}

		try {
			cfg.addResource( resourceName );
			fail();
		}
		catch (InvalidMappingException inv) {
			assertEquals( "resource", inv.getType() );
			assertEquals( resourceName, inv.getPath() );
		}

		try {
			cfg.addURL( ConfigHelper.findAsResource( resourceName ) );
			fail();
		}
		catch (InvalidMappingException inv) {
			assertEquals( "URL", inv.getType() );
			assertTrue( inv.getPath().endsWith( "InvalidMapping.hbm.xml" ) );
		}
	}

	void copy(InputStream in, File dst) throws IOException {
		OutputStream out = new FileOutputStream( dst );

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ( ( len = in.read( buf ) ) > 0 ) {
			out.write( buf, 0, len );
		}
		in.close();
		out.close();
	}
}
