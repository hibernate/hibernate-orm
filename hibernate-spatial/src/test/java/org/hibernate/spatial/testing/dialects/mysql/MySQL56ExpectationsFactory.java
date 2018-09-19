/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing.dialects.mysql;

import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.NativeSQLStatement;

import org.locationtech.jts.geom.Geometry;

/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 10/9/13
 */
public class MySQL56ExpectationsFactory extends MySQLExpectationsFactory {


	public MySQL56ExpectationsFactory(DataSourceUtils dataSourceUtils) {
		super( dataSourceUtils );
	}

	@Override
	protected NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Touches(t.geom, GeomFromText(?, 4326)) from geomtest t where ST_Touches(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_overlaps(t.geom, GeomFromText(?, 4326)) from geomtest t where ST_Overlaps(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Intersects(t.geom, GeomFromText(?, 4326)) from geomtest t where ST_Intersects(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeWithinStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Within(t.geom, GeomFromText(?, 4326)) from geomtest t where ST_Within(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Equals(t.geom, GeomFromText(?, 4326)) from geomtest t where ST_Equals(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Crosses(t.geom, GeomFromText(?, 4326)) from geomtest t where ST_Crosses(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Contains(t.geom, GeomFromText(?, 4326)) from geomtest t where ST_Contains(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Disjoint(t.geom, GeomFromText(?, 4326)) from geomtest t where ST_Disjoint(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

}
