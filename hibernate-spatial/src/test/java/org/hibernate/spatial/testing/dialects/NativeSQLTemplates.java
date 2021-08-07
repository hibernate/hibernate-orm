/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.spatial.CommonSpatialFunction;

import static org.hibernate.spatial.CommonSpatialFunction.*;

public class NativeSQLTemplates {

	protected final Map<CommonSpatialFunction, String> sqls = new HashMap<>();

	// Note that we alias the function invocation so that
	// we can map the return value to the required type
	public NativeSQLTemplates() {
		sqls.put( ST_ASTEXT, "select id, st_astext(geom) as result from %s" );
		sqls.put( ST_GEOMETRYTYPE, "select id, st_geometrytype(geom) as result from %s" );
		sqls.put( ST_DIMENSION, "select id, st_dimension(geom) as result from %s" );
		sqls.put( ST_ENVELOPE, "select id, st_envelope(geom) as result from %s" );
		sqls.put( ST_SRID, "select id, st_srid(geom) as result from %s" );
		sqls.put( ST_ASBINARY, "select id, st_asbinary(geom) as result from %s" );
		sqls.put( ST_ISEMPTY, "select id, st_isempty(geom) as result from %s" );
		sqls.put( ST_ISSIMPLE, "select id, st_issimple(geom) as result from %s" );
		sqls.put( ST_BOUNDARY, "select id, st_boundary(geom) as result from %s" );
		sqls.put( ST_OVERLAPS, "select id, st_overlaps(geom, st_geomfromtext(:filter, 4326)) as result from %s" );
		sqls.put( ST_INTERSECTS, "select id, st_intersects(geom, st_geomfromtext(:filter, 4326)) as result from %s" );
		sqls.put( ST_CROSSES, "select id, st_crosses(geom, st_geomfromtext(:filter, 4326)) as result from %s" );
		sqls.put( ST_CONTAINS, "select id, st_contains(geom, st_geomfromtext(:filter, 4326)) as result from %s" );
		sqls.put( ST_DISJOINT, "select id, st_disjoint(geom, st_geomfromtext(:filter, 4326)) as result from %s" );
		sqls.put( ST_RELATE, "select id, st_relate(geom, st_geomfromtext(:filter, 4326)) as result from %s" );
		sqls.put( ST_TOUCHES, "select id, st_touches(geom, st_geomfromtext(:filter, 4326)) as result from %s" );
		sqls.put( ST_WITHIN, "select id, st_within(geom, st_geomfromtext(:filter, 4326)) as result from %s" );
		sqls.put( ST_EQUALS, "select id, st_equals(geom, st_geomfromtext(:filter, 4326)) as result from %s" );
		sqls.put( ST_DISTANCE, "select id, st_distance(geom, st_geomfromtext(:filter, 4326)) as result from %s" );
		sqls.put( ST_BUFFER, "select id, st_buffer(geom, 2) as result from %s" );
		sqls.put( ST_CONVEXHULL, "select id, st_convexhull(geom) as result from %s" );
		sqls.put( ST_DIFFERENCE, "select id, st_difference(geom, st_geomfromtext(:filter, 4326)) as result from %s" );
		sqls.put( ST_INTERSECTION, "select id, st_intersection(geom, st_geomfromtext(:filter, 4326)) as result from %s" );
		sqls.put( ST_SYMDIFFERENCE, "select id, st_symdifference(geom, st_geomfromtext(:filter, 4326)) as result from %s" );
		sqls.put( ST_UNION, "select id, st_union(geom, st_geomfromtext(:filter, 4326)) as result from %s" );

	}

	public Map<CommonSpatialFunction, String> all() {
		return Collections.unmodifiableMap( this.sqls );
	}
}
