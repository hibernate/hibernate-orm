/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cfg;

import java.io.File;

import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests using of cacheable configuration files.
 *
 * @author Steve Ebersole
 */
public class CacheableFileTest extends BaseUnitTestCase {
	public static final String MAPPING = "org/hibernate/test/cfg/Cacheable.hbm.xml";

	private File mappingFile;
	private File mappingBinFile;

	@Before
	public void setUp() throws Exception {
		mappingFile = new File( getClass().getClassLoader().getResource( MAPPING ).toURI() );
		assertTrue( mappingFile.exists() );
		mappingBinFile = new File( mappingFile.getParentFile(), mappingFile.getName() + ".bin" );
		if ( mappingBinFile.exists() ) {
			//noinspection ResultOfMethodCallIgnored
			mappingBinFile.delete();
		}
	}

	@After
	public void tearDown() throws Exception {
		if ( mappingBinFile != null && mappingBinFile.exists() ) {
			// be nice
			//noinspection ResultOfMethodCallIgnored
			mappingBinFile.delete();
		}
		mappingBinFile = null;
		mappingFile = null;
	}

	@Test
	public void testCachedFiles() throws Exception {
		assertFalse( mappingBinFile.exists() );
		// This call should create the cached file
		new Configuration().addCacheableFile( mappingFile );
		assertTrue( mappingBinFile.exists() );

		new Configuration().addCacheableFileStrictly( mappingFile );
	}
}
