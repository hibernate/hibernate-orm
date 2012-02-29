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
import org.hibernate.spatial.jts.mgeom.MGeometryFactory;

abstract class AbstractDecoder<G extends Geometry> implements Decoder<G> {

	private final MGeometryFactory geometryFactory;

	public AbstractDecoder(MGeometryFactory factory) {
		this.geometryFactory = factory;
	}

	public G decode(SqlServerGeometry nativeGeom) {
		if (!accepts(nativeGeom)) {
			throw new IllegalArgumentException(getClass().getSimpleName() + " received object of type " + nativeGeom.openGisType());
		}
		if (nativeGeom.isEmpty()) {
			G nullGeom = createNullGeometry();
			setSrid(nativeGeom, nullGeom);
			return nullGeom;
		}
		G result = createGeometry(nativeGeom);
		setSrid(nativeGeom, result);
		return result;
	}

	public boolean accepts(OpenGisType type) {
		return type == getOpenGisType();
	}

	public boolean accepts(SqlServerGeometry nativeGeom) {
		return accepts(nativeGeom.openGisType());
	}

	protected abstract OpenGisType getOpenGisType();

	protected abstract G createNullGeometry();

	protected abstract G createGeometry(SqlServerGeometry nativeGeom);

	protected abstract G createGeometry(SqlServerGeometry nativeGeom, int shapeIndex);

	protected MGeometryFactory getGeometryFactory() {
		return this.geometryFactory;
	}

	protected void setSrid(SqlServerGeometry sqlServerGeom, G result) {
		if (sqlServerGeom.getSrid() != null) {
			result.setSRID(sqlServerGeom.getSrid());
		}
	}


}
