// $Id: SQLExceptionConversionTest.java 6847 2005-05-21 15:46:41Z oneovthafew $
package org.hibernate.test.mappingexception;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.hibernate.DuplicateMappingException;
import org.hibernate.Hibernate;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.jaxb.spi.SourceType;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.source.InvalidMappingException;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MappingNotFoundException;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * Test for various mapping exceptions thrown when mappings are not found or invalid.
 *
 * @author Max Rydahl Andersen
 * @author Brett Meyer
 */
public class MappingExceptionTest extends BaseUnitTestCase {
	@Test
	public void testNotFound() throws MappingException, MalformedURLException {
		MetadataSources sources = new MetadataSources( new StandardServiceRegistryBuilder().build() );

		try {
			sources.addCacheableFile( "completelybogus.hbm.xml" );
			fail();
		}
		catch ( MappingNotFoundException e ) {
			assertEquals( e.getOrigin().getType(), SourceType.FILE );
			assertTrue( e.getOrigin().getName().contains( "completelybogus.hbm.xml" ) );
		}

		try {
			sources.addCacheableFile( new File( "completelybogus.hbm.xml" ) );
			fail();
		}
		catch ( MappingNotFoundException e ) {
			assertEquals( e.getOrigin().getType(), SourceType.FILE );
			assertTrue( e.getOrigin().getName().contains( "completelybogus.hbm.xml" ) );
		}

		try {
			sources.addClass( Hibernate.class ); // TODO: String.class result in npe, because no classloader exists for it
			fail();
		}
		catch ( MappingNotFoundException e ) {
			assertEquals( e.getOrigin().getType(), SourceType.RESOURCE );
			assertTrue( e.getOrigin().getName().contains( "org/hibernate/Hibernate.hbm.xml" ) );
		}

		try {
			sources.addFile( "completelybogus.hbm.xml" );
			fail();
		}
		catch ( MappingNotFoundException e ) {
			assertEquals( e.getOrigin().getType(), SourceType.FILE );
			assertTrue( e.getOrigin().getName().contains( "completelybogus.hbm.xml" ) );
		}

		try {
			sources.addFile( new File( "completelybogus.hbm.xml" ) );
			fail();
		}
		catch ( MappingNotFoundException e ) {
			assertEquals( e.getOrigin().getType(), SourceType.FILE );
			assertTrue( e.getOrigin().getName().contains( "completelybogus.hbm.xml" ) );
		}

		try {
			sources.addInputStream( new ByteArrayInputStream( new byte[0] ) );
			fail();
		}
		catch ( InvalidMappingException e ) {
			assertEquals( e.getOrigin().getType(), SourceType.INPUT_STREAM );
			assertEquals( e.getOrigin().getName(), MetadataSources.UNKNOWN_FILE_PATH );
		}

		try {
			sources.addResource( "nothere" );
			fail();
		}
		catch ( MappingNotFoundException e ) {
			assertEquals( e.getOrigin().getType(), SourceType.RESOURCE );
			assertTrue( e.getOrigin().getName().contains( "nothere" ) );
		}

		try {
			sources.addResource( "nothere" );
			fail();
		}
		catch ( MappingNotFoundException e ) {
			assertEquals( e.getOrigin().getType(), SourceType.RESOURCE );
			assertTrue( e.getOrigin().getName().contains( "nothere" ) );
		}

		try {
			sources.addURL( new URL( "file://nothere" ) );
			fail();
		}
		catch ( MappingNotFoundException e ) {
			assertEquals( e.getOrigin().getType(), SourceType.URL );
			assertTrue( e.getOrigin().getName().contains( "file://nothere" ) );
		}
	}

	@Test
	public void testDuplicateMapping() {
		String resourceName = "org/hibernate/test/mappingexception/User.hbm.xml";
		MetadataSources sources = new MetadataSources( new StandardServiceRegistryBuilder().build() );
		sources.addResource( resourceName );
		sources.buildMetadata();
		try {
			sources.addResource( resourceName );
			sources.buildMetadata();
			fail();
		}
		catch ( DuplicateMappingException e ) {
			assertEquals( e.getType(), DuplicateMappingException.Type.ENTITY.name() );
			assertNotNull( e.getName() );
		}
	}

	private void assertClassAssignability(Class expected, Class actual) {
		if ( !expected.isAssignableFrom( actual ) ) {
			fail( "Actual class [" + actual.getName() + "] not assignable to expected [" + expected.getName() + "]" );
		}
	}

	@Test
	public void testInvalidMapping() throws MappingException, IOException {
		String resourceName = "org/hibernate/test/mappingexception/InvalidMapping.hbm.xml";
		File file = File.createTempFile( "TempInvalidMapping", ".hbm.xml" );
		file.deleteOnExit();
		copy( ConfigHelper.getConfigStream( resourceName ), file );

		MetadataSources sources = new MetadataSources( new StandardServiceRegistryBuilder().build() );
		try {
			sources.addCacheableFile( file.getAbsolutePath() );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getOrigin().getType(), SourceType.FILE );
			assertNotNull( inv.getOrigin().getName() );
			assertTrue( inv.getOrigin().getName().endsWith( ".hbm.xml" ) );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			sources.addCacheableFile( file );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getOrigin().getType(), SourceType.FILE );
			assertNotNull( inv.getOrigin().getName() );
			assertTrue( inv.getOrigin().getName().endsWith( ".hbm.xml" ) );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			sources.addClass( InvalidMapping.class );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getOrigin().getType(), SourceType.RESOURCE );
			assertNotNull( inv.getOrigin().getName() );
			assertTrue( inv.getOrigin().getName().contains( "org/hibernate/test/mappingexception/InvalidMapping.hbm.xml" ) );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			sources.addFile( file.getAbsolutePath() );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getOrigin().getType(), SourceType.FILE );
			assertNotNull( inv.getOrigin().getName() );
			assertEquals( inv.getOrigin().getName(), file.getPath() );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			sources.addFile( file );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getOrigin().getType(), SourceType.FILE );
			assertNotNull( inv.getOrigin().getName() );
			assertEquals( inv.getOrigin().getName(), file.getPath() );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}


		try {
			sources.addInputStream( ConfigHelper.getResourceAsStream( resourceName ) );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getOrigin().getType(), SourceType.INPUT_STREAM );
			assertEquals( inv.getOrigin().getName(), MetadataSources.UNKNOWN_FILE_PATH );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			sources.addResource( resourceName );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getOrigin().getType(), SourceType.RESOURCE );
			assertNotNull( inv.getOrigin().getName() );
			assertEquals( inv.getOrigin().getName(), resourceName );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			sources.addResource( resourceName );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getOrigin().getType(), SourceType.RESOURCE );
			assertNotNull( inv.getOrigin().getName() );
			assertEquals( inv.getOrigin().getName(), resourceName );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}

		try {
			sources.addURL( ConfigHelper.findAsResource( resourceName ) );
			fail();
		}
		catch ( InvalidMappingException inv ) {
			assertEquals( inv.getOrigin().getType(), SourceType.URL );
			assertNotNull( inv.getOrigin().getName() );
			assertTrue( inv.getOrigin().getName().endsWith( "InvalidMapping.hbm.xml" ) );
			assertTrue( !( inv.getCause() instanceof MappingNotFoundException ) );
		}
	}

	private void copy(InputStream in, File dst) throws IOException {
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
