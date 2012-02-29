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

import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import org.hibernate.spatial.jts.mgeom.MGeometryFactory;
import org.hibernate.spatial.jts.mgeom.MLineString;

class MultiLineStringDecoder extends AbstractGeometryCollectionDecoder<MultiLineString> {

	public MultiLineStringDecoder(MGeometryFactory factory) {
		super( factory );
	}

	@Override
	protected OpenGisType getOpenGisType() {
		return OpenGisType.MULTILINESTRING;
	}


	@Override
	protected MultiLineString createGeometry(List<Geometry> geometries, boolean hasM) {
		if ( hasM ) {
			MLineString[] mlAr = geometries != null ? geometries.toArray( new MLineString[geometries.size()] ) : null;
			return getGeometryFactory().createMultiMLineString( mlAr );
		}
		LineString[] lAr = geometries != null ? geometries.toArray( new LineString[geometries.size()] ) : null;
		return getGeometryFactory().createMultiLineString( lAr );
	}

}
