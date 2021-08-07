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

	private final Map<CommonSpatialFunction, String> sqls = new HashMap<>();

	// Note that we alias the function invocation so that
	// we can map the return value to the required type
	public NativeSQLTemplates() {
		sqls.put( ST_ASTEXT, "select id, st_astext(geom) from %s" );
		sqls.put( ST_GEOMETRYTYPE, "select id, st_geometrytype(geom) from %s" );
		sqls.put( ST_DIMENSION, "select id, st_dimension(geom) from %s" );
		sqls.put( ST_ENVELOPE, "select id, st_envelope(geom) from %s" );
		sqls.put( ST_SRID, "select id, st_srid(geom) from %s" );
	}

	public Map<CommonSpatialFunction, String> all() {
		return Collections.unmodifiableMap( this.sqls );
	}
}
