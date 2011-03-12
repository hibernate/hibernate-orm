//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
 *
 */
package org.hibernate.test.lob;

import junit.framework.Test;

import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.dialect.Dialect;

/**
 * Tests eager materialization and mutation of data mapped by
 * {@link org.hibernate.type.MaterializedBlobType}.
 *
 * @author Gail Badner
 */
public class MaterializedBlobTest extends LongByteArrayTest {

	public MaterializedBlobTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "lob/MaterializedBlobMappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( MaterializedBlobTest.class );
	}

	@Override
	public void testBoundedLongByteArrayAccess() {
		super.testBoundedLongByteArrayAccess();
	}

	@Override
	public void testSaving() {
		super.testSaving();
	}

	public boolean appliesTo(Dialect dialect) {
		if ( ! dialect.supportsExpectedLobUsagePattern() ) {
			reportSkip( "database/driver does not support expected LOB usage pattern", "LOB support" );
			return false;
		}
		return true;
	}
}