// $Id: SQLExceptionConversionTest.java 6847 2005-05-21 15:46:41Z oneovthafew $
package org.hibernate.test.mappingexception;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import org.hibernate.DuplicateMappingException;
import org.hibernate.Hibernate;
import org.hibernate.InvalidMappingException;
import org.hibernate.MappingException;
import org.hibernate.MappingNotFoundException;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test for various mapping exceptions thrown when mappings are not found or invalid.
 *
 * @author Max Rydahl Andersen
 */
public class MappingExceptionTest extends BaseUnitTestCase {
	@Test
	public void testNotFound() throws MappingException, MalformedURLException {
		Configuration cfg = new Configuration();

		try {
			cfg.addCacheableFile( "completelybogus.hbm.xml" );
			fail();
		}
		catch ( MappingNotFoundException e ) {
			assertEquals( e.getType(), "file" );
			assertEquals( e.getPath(), "completelybogus.hbm.xml" );
		}

		try {
			cfg.addCacheableFile( new File( "completelybogus.hbm.xml" ) );
			fail();
		}
		catch ( MappingNotFoundException e ) {
			assertEquals( e.getType(), "file" );
			assertEquals( e.getPath(), "completelybogus.hbm.xml" );
		}

		try {
			cfg.addClass( Hibernate.class ); // TODO: String.class result in npe, because no classloader exists for it
			fail();
		}
		catch ( MappingNotFoundException inv ) {
			assertEquals( inv.getType(), "resource" );
			assertEquals( inv.getPath(), "org/hibernate/Hibernate.hbm.xml" );
		}

		try {
			cfg.addFile( "completelybogus.hbm.xml" );
			fail();
		}
		catch ( MappingNotFoundException e ) {
			assertEquals( e.getType(), "file" );
			assertEquals( e.getPath(), "completelybogus.hbm.xml" );
		}

		try {
			cfg.addFile( new File( "completelybogus.hbm.xml" ) );
			fail();
		}
		catch ( MappingNotFoundException inv ) {
			assertEquals( inv.getType(), "file" );
			assertEquals( inv.getPath(), "completelybogus.hbm.xml" );
		}

		try {
			cfg.addInputStream( new ByteArrayInputStream( new byte[0] ) );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getType(), "input stream" );
			assertEquals( inv.getPath(), null );
		}

		try {
			cfg.addResource( "nothere" );
			fail();
		}
		catch ( MappingNotFoundException inv ) {
			assertEquals( inv.getType(), "resource" );
			assertEquals( inv.getPath(), "nothere" );
		}

		try {
			cfg.addResource( "nothere", getClass().getClassLoader() );
			fail();
		}
		catch ( MappingNotFoundException inv ) {
			assertEquals( inv.getType(), "resource" );
			assertEquals( inv.getPath(), "nothere" );
		}

		try {
			cfg.addURL( new URL( "file://nothere" ) );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getType(), "URL" );
			assertEquals( inv.getPath(), "file://nothere" );
		}
	}

	public void testDuplicateMapping() {
		String resourceName = "org/hibernate/test/mappingexception/User.hbm.xml";
		Configuration cfg = new Configuration();
		cfg.addResource( resourceName );
		cfg.buildMappings();
		try {
			cfg.addResource( resourceName );
			cfg.buildMappings();
			fail();
		}
		catch ( InvalidMappingException e ) {
			assertEquals( e.getType(), "resource" );
			assertEquals( e.getPath(), resourceName );
			assertClassAssignability( DuplicateMappingException.class, e.getCause().getClass() );
		}
	}

	private void assertClassAssignability(Class expected, Class actual) {
		if ( !expected.isAssignableFrom( actual ) ) {
			fail( "Actual class [" + actual.getName() + "] not assignable to expected [" + expected.getName() + "]" );
		}
	}

	public void testInvalidMapping() throws MappingException, IOException {
		String resourceName = "org/hibernate/test/mappingexception/InvalidMapping.hbm.xml";
		File file = File.createTempFile( "TempInvalidMapping", ".hbm.xml" );
		file.deleteOnExit();
		copy( ConfigHelper.getConfigStream( resourceName ), file );

		Configuration cfg = new Configuration();
		try {
			cfg.addCacheableFile( file.getAbsolutePath() );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getType(), "file" );
			assertNotNull( inv.getPath() );
			assertTrue( inv.getPath().endsWith( ".hbm.xml" ) );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			cfg.addCacheableFile( file );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getType(), "file" );
			assertNotNull( inv.getPath() );
			assertTrue( inv.getPath().endsWith( ".hbm.xml" ) );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			cfg.addClass( InvalidMapping.class );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getType(), "resource" );
			assertEquals( inv.getPath(), "org/hibernate/test/mappingexception/InvalidMapping.hbm.xml" );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			cfg.addFile( file.getAbsolutePath() );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getType(), "file" );
			assertEquals( inv.getPath(), file.getPath() );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			cfg.addFile( file );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getType(), "file" );
			assertEquals( inv.getPath(), file.getPath() );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}


		try {
			cfg.addInputStream( ConfigHelper.getResourceAsStream( resourceName ) );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getType(), "input stream" );
			assertEquals( inv.getPath(), null );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			cfg.addResource( resourceName );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getType(), "resource" );
			assertEquals( inv.getPath(), resourceName );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			cfg.addResource( resourceName, getClass().getClassLoader() );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getType(), "resource" );
			assertEquals( inv.getPath(), resourceName );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			cfg.addURL( ConfigHelper.findAsResource( resourceName ) );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getType(), "URL" );
			assertTrue( inv.getPath().endsWith( "InvalidMapping.hbm.xml" ) );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
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
