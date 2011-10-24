/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.testing.junit4.BaseUnitTestCase;

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

		Configuration cfg = new Configuration().addCacheableFileStrictly( mappingFile );
		SerializationHelper.clone( cfg );
	}
}
