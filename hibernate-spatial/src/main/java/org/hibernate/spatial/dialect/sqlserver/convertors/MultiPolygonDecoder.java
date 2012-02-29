/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.dialect.sqlserver.convertors;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import org.hibernate.spatial.jts.mgeom.MGeometryFactory;

import java.util.List;

class MultiPolygonDecoder extends AbstractGeometryCollectionDecoder<MultiPolygon> {

	public MultiPolygonDecoder(MGeometryFactory factory) {
		super(factory);
	}


	@Override
	protected OpenGisType getOpenGisType() {
		return OpenGisType.MULTIPOLYGON;
	}

	@Override
	protected MultiPolygon createGeometry(List<Geometry> geometries, boolean hasM) {
		Polygon[] polygons = geometries != null ? geometries.toArray(new Polygon[geometries.size()]) : null;
		return getGeometryFactory().createMultiPolygon(polygons);
	}


}
