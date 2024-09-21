/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
