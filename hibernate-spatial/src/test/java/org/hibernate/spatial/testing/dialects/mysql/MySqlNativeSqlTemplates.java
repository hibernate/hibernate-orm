/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.mysql;

import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;

import static org.hibernate.spatial.CommonSpatialFunction.ST_BUFFER;
import static org.hibernate.spatial.CommonSpatialFunction.ST_CONTAINS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_CONVEXHULL;
import static org.hibernate.spatial.CommonSpatialFunction.ST_CROSSES;
import static org.hibernate.spatial.CommonSpatialFunction.ST_DIFFERENCE;
import static org.hibernate.spatial.CommonSpatialFunction.ST_DISJOINT;
import static org.hibernate.spatial.CommonSpatialFunction.ST_DISTANCE;
import static org.hibernate.spatial.CommonSpatialFunction.ST_EQUALS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_INTERSECTION;
import static org.hibernate.spatial.CommonSpatialFunction.ST_INTERSECTS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_OVERLAPS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_RELATE;
import static org.hibernate.spatial.CommonSpatialFunction.ST_SYMDIFFERENCE;
import static org.hibernate.spatial.CommonSpatialFunction.ST_TOUCHES;
import static org.hibernate.spatial.CommonSpatialFunction.ST_UNION;
import static org.hibernate.spatial.CommonSpatialFunction.ST_WITHIN;

public class MySqlNativeSqlTemplates extends NativeSQLTemplates {

	public MySqlNativeSqlTemplates() {
		super();
		sqls.put( ST_OVERLAPS, "select id, st_overlaps(geom, st_geomfromtext(:filter, 0)) as result from %s" );
		sqls.put( ST_INTERSECTS, "select id, st_intersects(geom, st_geomfromtext(:filter, 0)) as result from %s" );
		sqls.put( ST_CROSSES, "select id, st_crosses(geom, st_geomfromtext(:filter, 0)) as result from %s" );
		sqls.put( ST_CONTAINS, "select id, st_contains(geom, st_geomfromtext(:filter, 0)) as result from %s" );
		sqls.put( ST_DISJOINT, "select id, st_disjoint(geom, st_geomfromtext(:filter, 0)) as result from %s" );
		sqls.put( ST_RELATE, "select id, st_relate(geom, st_geomfromtext(:filter, 0)) as result from %s" );
		sqls.put( ST_TOUCHES, "select id, st_touches(geom, st_geomfromtext(:filter, 0)) as result from %s" );
		sqls.put( ST_WITHIN, "select id, st_within(geom, st_geomfromtext(:filter, 0)) as result from %s" );
		sqls.put( ST_EQUALS, "select id, st_equals(geom, st_geomfromtext(:filter, 0)) as result from %s" );
		sqls.put( ST_DISTANCE, "select id, st_distance(geom, st_geomfromtext(:filter, 0)) as result from %s" );
		sqls.put( ST_BUFFER, "select id, st_buffer(geom, 2) as result from %s" );
		sqls.put( ST_CONVEXHULL, "select id, st_convexhull(geom) as result from %s" );
		sqls.put( ST_DIFFERENCE, "select id, st_difference(geom, st_geomfromtext(:filter, 0)) as result from %s" );
		sqls.put(
				ST_INTERSECTION,
				"select id, st_intersection(geom, st_geomfromtext(:filter, 0)) as result from %s"
		);
		sqls.put(
				ST_SYMDIFFERENCE,
				"select id, st_symdifference(geom, st_geomfromtext(:filter, 0)) as result from %s"
		);
		sqls.put( ST_UNION, "select id, st_union(geom, st_geomfromtext(:filter, 0)) as result from %s" );
	}
}
