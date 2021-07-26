/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

public class NativeSqlTemplates {


	public NativeSQLTemplate createNativeAsTextTemplate() {
		return new NativeSQLTemplate( "select id, st_astext(geom) from %s" );
	}

	public NativeSQLTemplate createNativeGeometryTypeTemplate() {
		return new NativeSQLTemplate( "select id, st_geometrytype(geom) from %s" );
	}

	public NativeSQLTemplate createNativeDimensionTemplate() {
		return new NativeSQLTemplate( "select id, st_dimension(geom) from %s" );
	}

}
