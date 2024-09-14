/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial;

import org.geolatte.geom.Geometry;

public interface GeomCodec {

	/**
	 * Decode value returned by JDBC Driver for Geometry as Geolatte Geometry
	 * @param in value returned by JDBC Driver
	 * @return the decoded Geoemtry
	 */
	Geometry<?> toGeometry(Object in);


}
