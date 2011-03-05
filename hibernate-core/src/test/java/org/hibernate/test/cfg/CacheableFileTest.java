/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.cfg;

import java.io.File;

import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.testing.junit.UnitTestCase;

/**
 * Tests using of cacheable configuration files.
 *
 * @author Steve Ebersole
 */
public class CacheableFileTest extends UnitTestCase {
	public static final String MAPPING = "org/hibernate/test/cfg/Cacheable.hbm.xml";

	private File mappingFile;
	private File mappingBinFile;

	public CacheableFileTest(String string) {
		super( string );
	}

	protected void setUp() throws Exception {
		super.setUp();
		mappingFile = new File( getClass().getClassLoader().getResource( MAPPING ).toURI() );
		assertTrue( mappingFile.exists() );
		mappingBinFile = new File( mappingFile.getParentFile(), mappingFile.getName() + ".bin" );
		if ( mappingBinFile.exists() ) {
			//noinspection ResultOfMethodCallIgnored
			mappingBinFile.delete();
		}
	}

	protected void tearDown() throws Exception {
		if ( mappingBinFile != null && mappingBinFile.exists() ) {
			// be nice
			//noinspection ResultOfMethodCallIgnored
			mappingBinFile.delete();
		}
		mappingBinFile = null;
		mappingFile = null;
		super.tearDown();
	}

	public void testCachedFiles() throws Exception {
		assertFalse( mappingBinFile.exists() );
		// This call should create the cached file
		new Configuration().addCacheableFile( mappingFile );
		assertTrue( mappingBinFile.exists() );

		Configuration cfg = new Configuration().addCacheableFileStrictly( mappingFile );
		SerializationHelper.clone( cfg );
	}
}
