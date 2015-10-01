/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.boot.binding.cacheable;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.internal.CacheableFileXmlSource;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.XmlMappingBinderAccess;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.fail;

/**
 * Originally developed to help diagnose HHH-10131 - the original tests
 * check 4 conditions:<ol>
 *     <li>strict usage where the cached file does exist</li>
 *     <li>strict usage where the cached file does not exist</li>
 *     <li>non-strict usage where the cached file does exist</li>
 *     <li>non-strict usage where the cached file does not exist</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public class CacheableHbmXmlTest {
	private static final Logger log = Logger.getLogger( CacheableHbmXmlTest.class );

	private static final String HBM_RESOURCE_NAME = "org/hibernate/test/boot/binding/cacheable/SimpleEntity.hbm.xml";

	private StandardServiceRegistry ssr;
	private MappingBinder binder;

	private File hbmXmlFile;
	private File hbmXmlBinFile;

	@Before
	public void before() throws Exception {
		ssr = new StandardServiceRegistryBuilder()
				.build();
		binder = new XmlMappingBinderAccess( ssr ).getMappingBinder();

		final URL hbmXmlUrl = getClass().getClassLoader().getResource( HBM_RESOURCE_NAME );
		if ( hbmXmlUrl == null ) {
			throw couldNotFindHbmXmlFile();
		}
		hbmXmlFile = new File( hbmXmlUrl.getFile() );
		if ( ! hbmXmlFile.exists() ) {
			throw couldNotFindHbmXmlFile();
		}
		hbmXmlBinFile = CacheableFileXmlSource.determineCachedFile( hbmXmlFile );
	}

	private Exception couldNotFindHbmXmlFile() {
		throw new IllegalStateException( "Could not locate hbm.xml file by resource lookup" );
	}

	@After
	public void after() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testStrictCaseWhereFileDoesPreviouslyExist() throws FileNotFoundException {
		deleteBinFile();
		createBinFile();
		try {
			new MetadataSources( ssr ).addCacheableFileStrictly( hbmXmlFile ).buildMetadata();
		}
		catch (MappingException e) {
			fail( "addCacheableFileStrictly led to MappingException when bin file existed" );
		}
	}

	@Test
	public void testStrictCaseWhereFileDoesNotPreviouslyExist() throws FileNotFoundException {
		deleteBinFile();
		try {
			new MetadataSources( ssr ).addCacheableFileStrictly( hbmXmlFile ).buildMetadata();
			fail( "addCacheableFileStrictly should be led to MappingException when bin file does not exist" );
		}
		catch (MappingException ignore) {
			// this is the expected result
		}
	}

	@Test
	public void testNonStrictCaseWhereFileDoesPreviouslyExist() {
		deleteBinFile();
		createBinFile();
		new MetadataSources( ssr ).addCacheableFile( hbmXmlFile ).buildMetadata();
	}

	@Test
	public void testNonStrictCaseWhereFileDoesNotPreviouslyExist() {
		deleteBinFile();
		new MetadataSources( ssr ).addCacheableFile( hbmXmlFile ).buildMetadata();
	}

	private void deleteBinFile() {
		// if it exists
		if ( hbmXmlBinFile.exists() ) {
			final boolean success = hbmXmlBinFile.delete();
			if ( !success ) {
				log.warn( "Unable to delete existing cached hbm.xml.bin file", new Exception() );
			}
		}
	}

	private void createBinFile() {
		if ( hbmXmlBinFile.exists() ) {
			log.warn( "Cached hbm.xml.bin file already existed on request to create", new Exception() );
		}
		else {
			CacheableFileXmlSource.createSerFile( hbmXmlFile, binder );
		}
	}
}
