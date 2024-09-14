/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.hbm.cacheable;

import java.io.File;
import java.net.URL;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.internal.CacheableFileXmlSource;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.XmlMappingBinderAccess;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.fail;

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
@ServiceRegistry()
public class CacheableHbmXmlTest {

	private static final String HBM_RESOURCE_NAME = "org/hibernate/orm/test/bootstrap/binding/hbm/cacheable/SimpleEntity.hbm.xml";

	private static MappingBinder binder;
	private static File hbmXmlFile;

	@BeforeAll
	public static void prepareFixtures(ServiceRegistryScope scope) throws Exception {
		binder = new XmlMappingBinderAccess( scope.getRegistry() ).getMappingBinder();

		final URL hbmXmlUrl = CacheableHbmXmlTest.class.getClassLoader().getResource( HBM_RESOURCE_NAME );
		if ( hbmXmlUrl == null ) {
			throw couldNotFindHbmXmlResource();
		}
		hbmXmlFile = new File( hbmXmlUrl.getFile() );
		if ( ! hbmXmlFile.exists() ) {
			throw couldNotFindHbmXmlFile( hbmXmlFile );
		}
	}

	private static Exception couldNotFindHbmXmlResource() {
		throw new IllegalStateException( "Could not locate `" + HBM_RESOURCE_NAME + "` by resource lookup" );
	}

	private static Exception couldNotFindHbmXmlFile(File file) {
		throw new IllegalStateException(
				"File `" + file.getAbsolutePath() + "` resolved from `" + HBM_RESOURCE_NAME + "` resource-lookup does not exist"
		);
	}

	@Test
	public void testStrictlyWithExistingFile(ServiceRegistryScope serviceRegistryScope, @TempDir File binOutputDir) {
		final StandardServiceRegistry ssr = serviceRegistryScope.getRegistry();

		// create the cacheable file so that it exists before we try to build the boot model
		createBinFile( binOutputDir );

		try {
			new MetadataSources( ssr ).addCacheableFileStrictly( hbmXmlFile, binOutputDir ).buildMetadata();
		}
		catch (MappingException e) {
			fail( "addCacheableFileStrictly led to MappingException when bin file existed" );
		}
	}

	@Test
	public void testStrictlyWithNoExistingFile(ServiceRegistryScope serviceRegistryScope, @TempDir File binOutputDir) {
		try {
			final StandardServiceRegistry ssr = serviceRegistryScope.getRegistry();
			new MetadataSources( ssr )
					.addCacheableFileStrictly( hbmXmlFile, binOutputDir )
					.buildMetadata();
			fail( "addCacheableFileStrictly should be led to MappingException when bin file does not exist" );
		}
		catch (MappingException ignore) {
			// this is the expected result
		}
	}

	@Test
	public void testNonStrictlyWithExistingFile(ServiceRegistryScope serviceRegistryScope, @TempDir File binOutputDir) {
		final StandardServiceRegistry ssr = serviceRegistryScope.getRegistry();

		// create the cacheable file so that it exists before we try to build the boot model
		createBinFile( binOutputDir );

		try {
			new MetadataSources( ssr ).addCacheableFile( hbmXmlFile, binOutputDir ).buildMetadata();
		}
		catch (MappingException e) {
			fail( "addCacheableFileStrictly led to MappingException when bin file existed" );
		}
	}

	@Test
	public void testNonStrictlyWithNoExistingFile(ServiceRegistryScope serviceRegistryScope, @TempDir File binOutputDir) {
		final StandardServiceRegistry ssr = serviceRegistryScope.getRegistry();
		new MetadataSources( ssr ).addCacheableFile( hbmXmlFile, binOutputDir ).buildMetadata();
	}

	private void createBinFile(File binOutputDir) {
		final String outputName = hbmXmlFile.getName() + ".bin";
		final File file = new File( binOutputDir, outputName );
		CacheableFileXmlSource.createSerFile( hbmXmlFile, file, binder );
	}
}
