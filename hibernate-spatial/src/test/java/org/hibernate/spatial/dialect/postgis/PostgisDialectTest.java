package org.hibernate.spatial.dialect.postgis;

import junit.framework.TestCase;
import org.junit.Test;

import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.dialect.postgis.PostgisDialect;

/**
 * Tests support for
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/19/11
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
