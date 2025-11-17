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

public class OracleSDONativeSqlTemplates extends NativeSQLTemplates {
	public OracleSDONativeSqlTemplates() {
		sqls.clear();
		sqls.put( ST_ASTEXT, "select t.ID, t.GEOM.GET_WKT() as result from %s t" );
		sqls.put( ST_GEOMETRYTYPE, "select t.ID, ST_Geometry(t.GEOM).st_geometrytype() as result from %s t" );
		sqls.put( ST_DIMENSION, "select id, t.GEOM.Get_Dims() as result from %s t" );
		sqls.put( ST_ENVELOPE, "select id, SDO_GEOM.SDO_MBR(t.geom) as result from %s t" );
		sqls.put( ST_SRID, "select id, t.geom.sdo_srid as result from %s t" );
		sqls.put( ST_ASBINARY, "select id, t.geom.Get_WKB() as result from %s t" );
		sqls.put( ST_ISEMPTY, "select id, ST_Geometry(t.geom).st_isempty() as result from %s t" );
		sqls.put( ST_ISSIMPLE, "select id, ST_Geometry(t.geom).st_issimple() as result from %s t" );
		sqls.put( ST_BOUNDARY, "select id, ST_Geometry(t.geom).st_boundary().geom as result from %s t" );
		sqls.put(
				ST_OVERLAPS,
				"select t.id, SDO_GEOM.relate( t.geom, 'CONTAINS', ST_GEOMETRY.FROM_WKT(:filter, 4326).Geom, 0.005) as result from %s T"
		);
		sqls.put(
				ST_INTERSECTS,
				"select id, case SDO_GEOM.relate(t.geom, 'OVERLAPBDYDISJOINT + OVERLAPBDYINTERSECT', ST_GEOMETRY.FROM_WKT(:filter, 4326).Geom, 0.005) when 'FALSE' THEN 0 ELSE 1 END as result from %s T"
		);
		sqls.put(
				ST_CROSSES,
				"select id, ST_GEOMETRY(t.geom).st_crosses(ST_GEOMETRY.FROM_WKT(:filter, 4326)) as result from %s t"
		);
		sqls.put(
				ST_CONTAINS,
				"select id,  CASE SDO_GEOM.relate(t.geom, 'CONTAINS', ST_GEOMETRY.FROM_WKT(:filter, 4326).Geom, 0.005) when 'FALSE' THEN 0 ELSE 1 END as result from %s T"
		);
		sqls.put(
				ST_DISJOINT,
				"select id,  CASE SDO_GEOM.relate(t.geom, 'DISJOINT', ST_GEOMETRY.FROM_WKT(:filter, 4326).Geom, 0.005) when 'FALSE' THEN 0 ELSE 1 END as result from %s T"
		);
		sqls.put(
				ST_RELATE,
				"select id, ST_GEOMETRY(t.geom).st_relate(ST_GEOMETRY.FROM_WKT(:filter, 4326), 'DETERMINE') as result from %s t"
		);
		sqls.put(
				ST_TOUCHES,
				"select id,  CASE SDO_GEOM.relate(t.geom, 'TOUCH', ST_GEOMETRY.FROM_WKT(:filter, 4326).Geom, 0.005) when 'FALSE' THEN 0 ELSE 1 END as result from %s T"
		);
		sqls.put(
				ST_WITHIN,
				"select id,  CASE SDO_GEOM.relate(t.geom, 'COVERS+CONTAINS', ST_GEOMETRY.FROM_WKT(:filter, 4326).Geom, 0.005) when 'FALSE' THEN 0 ELSE 1 END as result from %s T"
		);

		sqls.put(
				ST_EQUALS,
				"select id,  CASE SDO_GEOM.relate(t.geom, 'EQUAL', ST_GEOMETRY.FROM_WKT(:filter, 4326).Geom, 0.005) when 'FALSE' THEN 0 ELSE 1 END as result from %s T"
		);
		sqls.put(
				ST_DISTANCE,
				"select id, SDO_GEOM.SDO_DISTANCE(t.geom, ST_GEOMETRY.FROM_WKT(:filter, 4326).Geom) as result from %s t"
		);
		sqls.put( ST_BUFFER, "select id, SDO_GEOM.SDO_BUFFER(t.geom, 2) as result from %s t" );
		sqls.put( ST_CONVEXHULL, "select id, SDO_GEOM.SDO_CONVEXHULL(t.geom) as result from %s t" );
		sqls.put(
				ST_DIFFERENCE,
				"select id, SDO_GEOM.SDO_DIFFERENCE(t.geom, ST_GEOMETRY.FROM_WKT(:filter, 4326).Geom) as result from %s t"
		);
		sqls.put(
				ST_INTERSECTION,
				"select id, SDO_GEOM.SDO_INTERSECTION(t.geom, ST_GEOMETRY.FROM_WKT(:filter, 4326).Geom) as result from %s t"
		);
		sqls.put(
				ST_SYMDIFFERENCE,
				"select id, SDO_GEOM.SDO_XOR(t.geom, ST_GEOMETRY.FROM_WKT(:filter, 4326).Geom) as result from %s t"
		);
		sqls.put(
				ST_UNION,
				"select id, SDO_GEOM.SDO_UNION(t.geom, ST_GEOMETRY.FROM_WKT(:filter, 4326).Geom) as result from %s t"
		);
	}
}
