/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.integration.geolatte;

import org.hibernate.dialect.Dialect;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.integration.AbstractTestStoreRetrieve;
import org.hibernate.spatial.testing.GeolatteGeometryEquality;
import org.hibernate.spatial.testing.GeometryEquality;
import org.hibernate.spatial.testing.SpatialDialectMatcher;
import org.hibernate.spatial.testing.TestDataElement;

import org.hibernate.testing.Skip;

import org.jboss.logging.Logger;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.WktDecodeException;

/**
 * This testsuite-suite class verifies whether the <code>Geometry</code>s retrieved
 * are equal to the <code>Geometry</code>s stored.
 */
@Skip(condition = SpatialDialectMatcher.class, message = "No Spatial Dialect")
public class TestStoreRetrieveUsingGeolatte extends AbstractTestStoreRetrieve<Geometry, GeomEntity> {

	private static HSMessageLogger LOG = Logger.getMessageLogger(
			HSMessageLogger.class,
			TestStoreRetrieveUsingGeolatte.class.getName()
	);

	protected HSMessageLogger getLogger() {
		return LOG;
	}

	@Override
	protected GeometryEquality getGeometryEquality() {
		return new GeolatteGeometryEquality();
	}

	@Override
	protected Class getGeomEntityClass() {
		return GeomEntity.class;
	}

	@Override
	protected GeomEntity createFrom(
			TestDataElement element, Dialect dialect) throws WktDecodeException {
		return GeomEntity.createFrom( element, dialect );
	}
}
