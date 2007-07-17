package org.hibernate.test.cfg;

import java.io.File;

import org.hibernate.cfg.Configuration;
import org.hibernate.junit.UnitTestCase;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class CacheableFileTest extends UnitTestCase {

	public static final String MAPPING = "org/hibernate/test/cfg/Cacheable.hbm.xml";

	private File mappingFile;

	public CacheableFileTest(String string) {
		super( string );
	}

	protected void setUp() throws Exception {
		super.setUp();
		mappingFile = new File( getClass().getClassLoader().getResource( MAPPING ).getFile() );
		assertTrue( mappingFile.exists() );
		File cached = new File( mappingFile.getParentFile(), mappingFile.getName() + ".bin" );
		if ( cached.exists() ) {
			cached.delete();
		}
	}

	protected void tearDown() throws Exception {
		mappingFile = null;
		super.tearDown();
	}

	public void testCachedFiles() {
		Configuration cfg = new Configuration();
		cfg.addCacheableFile( mappingFile );
		Configuration cfg2 = new Configuration();
		cfg2.addCacheableFile( mappingFile );
	}
}
