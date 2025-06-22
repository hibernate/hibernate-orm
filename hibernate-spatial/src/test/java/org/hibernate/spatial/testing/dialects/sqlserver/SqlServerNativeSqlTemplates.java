/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.sqlserver;

import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;

import static org.hibernate.spatial.CommonSpatialFunction.ST_ASBINARY;
import static org.hibernate.spatial.CommonSpatialFunction.ST_ASTEXT;
import static org.hibernate.spatial.CommonSpatialFunction.ST_BOUNDARY;
import static org.hibernate.spatial.CommonSpatialFunction.ST_BUFFER;
import static org.hibernate.spatial.CommonSpatialFunction.ST_CONTAINS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_CONVEXHULL;
import static org.hibernate.spatial.CommonSpatialFunction.ST_CROSSES;
import static org.hibernate.spatial.CommonSpatialFunction.ST_DIFFERENCE;
import static org.hibernate.spatial.CommonSpatialFunction.ST_DIMENSION;
import static org.hibernate.spatial.CommonSpatialFunction.ST_DISJOINT;
import static org.hibernate.spatial.CommonSpatialFunction.ST_DISTANCE;
import static org.hibernate.spatial.CommonSpatialFunction.ST_ENVELOPE;
import static org.hibernate.spatial.CommonSpatialFunction.ST_EQUALS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_GEOMETRYTYPE;
import static org.hibernate.spatial.CommonSpatialFunction.ST_INTERSECTION;
import static org.hibernate.spatial.CommonSpatialFunction.ST_INTERSECTS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_ISEMPTY;
import static org.hibernate.spatial.CommonSpatialFunction.ST_ISSIMPLE;
import static org.hibernate.spatial.CommonSpatialFunction.ST_OVERLAPS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_SRID;
import static org.hibernate.spatial.CommonSpatialFunction.ST_SYMDIFFERENCE;
import static org.hibernate.spatial.CommonSpatialFunction.ST_TOUCHES;
import static org.hibernate.spatial.CommonSpatialFunction.ST_UNION;
import static org.hibernate.spatial.CommonSpatialFunction.ST_WITHIN;

public class SqlServerNativeSqlTemplates extends NativeSQLTemplates {

	public SqlServerNativeSqlTemplates() {
		sqls.put( ST_ASTEXT, "select id, geom.STAsText() as result from %s" );
		sqls.put( ST_GEOMETRYTYPE, "select id, geom.STGeometryType() as result from %s" );
		sqls.put( ST_DIMENSION, "select id, geom.STDimension() as result from %s" );
		sqls.put( ST_ENVELOPE, "select id, geom.STEnvelope() as result from %s" );
		sqls.put( ST_SRID, "select id, geom.STSrid as result from %s" );
		sqls.put( ST_ASBINARY, "select id, geom.STAsBinary() as result from %s" );
		sqls.put( ST_ISEMPTY, "select id, geom.STIsEmpty() as result from %s" );
		sqls.put( ST_ISSIMPLE, "select id, geom.STIsSimple() as result from %s" );
		sqls.put( ST_BOUNDARY, "select id, geom.STBoundary() as result from %s" );
		sqls.put(
				ST_OVERLAPS,
				"select id, geom.STOverlaps(geometry::::STGeomFromText(:filter, 4326)) as result from %s"
		);
		sqls.put(
				ST_INTERSECTS,
				"select id, geom.STIntersects(geometry::::STGeomFromText(:filter, 4326)) as result from %s"
		);
		sqls.put( ST_CROSSES,
				"select id, geom.STCrosses(geometry::::STGeomFromText(:filter, 4326)) as result from %s"
		);
		sqls.put( ST_CONTAINS, "select id, geom.STContains(geometry::::STGeomFromText(:filter, 4326)) as result from %s" );

		sqls.put( ST_DISJOINT, "select id, geom.STDisjoint(geometry::::STGeomFromText(:filter, 4326)) as result from %s" );

		//TODO -- re-enable once the ST_Relate() mess is resolved
//		sqls.put( ST_RELATE, "select id, geom.STRelate(geometry::::STGeomFromText(:filter, 4326), 'FF*FF****') as result from %s" );
		sqls.put( ST_TOUCHES, "select id, geom.STTouches(geometry::::STGeomFromText(:filter, 4326)) as result from %s" );
		sqls.put( ST_WITHIN, "select id, geom.STWithin(geometry::::STGeomFromText(:filter, 4326)) as result from %s" );
		sqls.put( ST_EQUALS, "select id, geom.STEquals(geometry::::STGeomFromText(:filter, 4326)) as result from %s" );
		sqls.put( ST_DISTANCE, "select id, geom.STDistance(geometry::::STGeomFromText(:filter, 4326)) as result from %s" );
		sqls.put( ST_BUFFER, "select id, geom.STBuffer(2.0) as result from %s" );
		sqls.put( ST_CONVEXHULL, "select id, geom.STConvexHull() as result from %s" );
		sqls.put( ST_DIFFERENCE, "select id, geom.STDifference(geometry::::STGeomFromText(:filter, 4326)) as result from %s" );
		sqls.put(
				ST_INTERSECTION,
				"select id, geom.STIntersection(geometry::::STGeomFromText(:filter, 4326)) as result from %s"
		);
		sqls.put(
				ST_SYMDIFFERENCE,
				"select id, geom.STSymDifference(geometry::::STGeomFromText(:filter, 4326)) as result from %s"
		);
		sqls.put( ST_UNION, "select id, geom.STUnion(geometry::::STGeomFromText(:filter, 4326)) as result from %s");
	}
}
