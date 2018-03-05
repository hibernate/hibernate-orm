/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.postgis;

import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;

import org.junit.Test;
import junit.framework.TestCase;

/**
 * Tests support for
 *
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 1/19/11
 */
public class PostgisDialectTest extends TestCase {

	SpatialDialect dialect = new PostgisDialect();

	@Test
	public void testSupports() throws Exception {
		for ( SpatialFunction sf : SpatialFunction.values() ) {
			assertTrue( "Dialect doesn't support " + sf, dialect.supports( sf ) );
		}
	}
}
