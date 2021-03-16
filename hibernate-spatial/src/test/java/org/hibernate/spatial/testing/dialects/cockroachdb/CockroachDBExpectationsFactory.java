/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.cockroachdb;

import org.hibernate.spatial.dialect.postgis.PGGeometryTypeDescriptor;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.dialects.postgis.PostgisExpectationsFactory;

import org.geolatte.geom.jts.JTS;
import org.locationtech.jts.geom.Geometry;

public class CockroachDBExpectationsFactory extends PostgisExpectationsFactory {

	public CockroachDBExpectationsFactory(DataSourceUtils utils) {
		super( utils );
	}

	@Override
	protected Geometry decode(Object object) {
		org.geolatte.geom.Geometry<?> geometry = PGGeometryTypeDescriptor.INSTANCE_WKB_2.toGeometry( object );
		return JTS.to( geometry );
	}
}
