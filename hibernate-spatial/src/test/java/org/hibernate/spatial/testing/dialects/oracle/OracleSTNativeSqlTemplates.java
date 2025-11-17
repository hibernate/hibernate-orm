/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.oracle;

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
import static org.hibernate.spatial.CommonSpatialFunction.ST_RELATE;
import static org.hibernate.spatial.CommonSpatialFunction.ST_SRID;
import static org.hibernate.spatial.CommonSpatialFunction.ST_SYMDIFFERENCE;
import static org.hibernate.spatial.CommonSpatialFunction.ST_TOUCHES;
import static org.hibernate.spatial.CommonSpatialFunction.ST_UNION;
import static org.hibernate.spatial.CommonSpatialFunction.ST_WITHIN;

public class OracleSTNativeSqlTemplates extends NativeSQLTemplates {
	public OracleSTNativeSqlTemplates() {
		sqls.clear();
		sqls.put( ST_ASTEXT, "select t.ID, t.GEOM.GET_WKT() as result from %s t" );
		sqls.put( ST_GEOMETRYTYPE, "select t.ID, ST_GEOMETRY(t.GEOM).ST_GEOMETRYTYPE() as result from %s t" );
		sqls.put( ST_DIMENSION, "select id, ST_GEOMETRY(t.GEOM).ST_DIMENSION() as result from %s t" );
		sqls.put( ST_ENVELOPE, "select id, ST_GEOMETRY(t.GEOM).ST_ENVELOPE().geom as result from %s t" );
		sqls.put( ST_ASBINARY, "select id, ST_GEOMETRY(t.GEOM).GET_WKB() as result from %s t" );
		sqls.put( ST_SRID, "select t.id, ST_GEOMETRY(t.GEOM).ST_SRID() as result from %s t" );
		sqls.put( ST_ISEMPTY, "select id, ST_GEOMETRY(t.GEOM).ST_ISEMPTY() as result from %s t" );
		sqls.put( ST_ISSIMPLE, "select id, ST_GEOMETRY(t.GEOM).ST_ISSIMPLE() as result from %s t" );
		sqls.put( ST_BOUNDARY, "select id, ST_GEOMETRY(t.GEOM).ST_BOUNDARY().Geom as result from %s t" );
		sqls.put(
				ST_OVERLAPS,
				"select id, ST_GEOMETRY(t.GEOM).ST_OVERLAP(ST_Geometry.FROM_WKT(:filter, 4326)) as result from %s t"
		);
		sqls.put(
				ST_INTERSECTS,
				"select id, ST_GEOMETRY(t.GEOM).st_intersects(ST_Geometry.From_WKT(:filter, 4326)) as result from %s t"
		);
		sqls.put(
				ST_CROSSES,
				"select id, ST_GEOMETRY(t.GEOM).st_crosses(ST_GEOMETRY.FROM_WKT(:filter, 4326)) as result from %s t"
		);
		sqls.put(
				ST_CONTAINS,
				"select id, ST_GEOMETRY(t.GEOM).ST_CONTAINS(ST_GEOMETRY.FROM_WKT(:filter, 4326)) as result from %s t"
		);
		sqls.put(
				ST_DISJOINT,
				"select id, ST_GEOMETRY(t.GEOM).ST_DISJOINT(ST_GEOMETRY.FROM_WKT(:filter, 4326)) as result from %s t"
		);
		sqls.put( ST_RELATE,
				"select id, ST_GEOMETRY(t.GEOM).st_relate(st_geometry.from_wkt(:filter, 4326), 'DETERMINE') as result from %s t" );
		sqls.put(
				ST_TOUCHES,
				"select id, ST_GEOMETRY(t.GEOM).ST_TOUCHES(ST_GEOMETRY.FROM_WKT(:filter, 4326)) as result from %s t"
		);
		sqls.put(
				ST_WITHIN,
				"select id, ST_GEOMETRY(t.GEOM).ST_WITHIN(ST_GEOMETRY.FROM_WKT(:filter, 4326)) as result from %s t"
		);
		sqls.put(
				ST_EQUALS,
				"select id, ST_GEOMETRY(t.GEOM).ST_EQUALS( ST_GEOMETRY.FROM_WKT(:filter, 4326)) as result from %s t"
		);
		sqls.put(
				ST_DISTANCE,
				"select id, ST_GEOMETRY(t.GEOM).st_distance(ST_Geometry.FROM_WKT(:filter, 4326)) as result from %s t"
		);
		sqls.put( ST_BUFFER, "select id,ST_GEOMETRY(t.GEOM).ST_BUFFER( 2 ).Geom as result from %s t" );
		sqls.put( ST_CONVEXHULL, "select id, ST_GEOMETRY(t.GEOM).st_convexhull().Geom as result from %s t" );
		sqls.put(
				ST_DIFFERENCE,
				"select id, ST_GEOMETRY(t.GEOM).st_difference(ST_GEOMETRY.FROM_WKT(:filter, 4326)).Geom as result from %s t"
		);
		sqls.put(
				ST_INTERSECTION,
				"select id, ST_GEOMETRY(t.geom).st_intersection(ST_GEOMETRY.FROM_WKT(:filter, 4326)).Geom as result from %s t"
		);
		sqls.put(
				ST_SYMDIFFERENCE,
				"select id, ST_GEOMETRY(t.GEOM).st_symdifference(ST_GEOMETRY.FROM_WKT(:filter, 4326)).Geom as result from %s t"
		);
		sqls.put(
				ST_UNION,
				"select id, ST_GEOMETRY(t.geom).st_union(ST_GEOMETRY.FROM_WKT(:filter, 4326)).Geom as result from %s t"
		);

	}

}
