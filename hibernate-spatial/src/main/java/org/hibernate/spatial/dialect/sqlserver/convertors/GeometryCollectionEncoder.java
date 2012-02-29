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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

import java.util.List;

/**
 * <code>Encoder</code> for GeometryCollections.
 *
 * @Author Karel Maesen
 */
class GeometryCollectionEncoder<T extends GeometryCollection> extends AbstractEncoder<T> {

	private final OpenGisType openGisType;

	GeometryCollectionEncoder(OpenGisType openGisType) {
		this.openGisType = openGisType;
	}

	public boolean accepts(Geometry geom) {
		return this.openGisType.typeOf(geom);
	}

	@Override
	protected void encode(Geometry geom, int parentShapeIndex, List<Coordinate> coordinates, List<Figure> figures, List<Shape> shapes) {
		if (geom.isEmpty()) {
			shapes.add(new Shape(parentShapeIndex, -1, this.openGisType));
			return;
		}
		int thisShapeIndex = shapes.size();
		Shape thisShape = createShape(parentShapeIndex, figures);
		shapes.add(thisShape);
		for (int i = 0; i < geom.getNumGeometries(); i++) {
			Geometry component = geom.getGeometryN(i);
			encodeComponent(component, thisShapeIndex, coordinates, figures, shapes);
		}
	}

	protected Shape createShape(int parentShapeIndex, List<Figure> figures) {
		Shape thisShape = new Shape(parentShapeIndex, figures.size(), this.openGisType);
		return thisShape;
	}

	protected void encodeComponent(Geometry geom, int thisShapeIndex, List<Coordinate> coordinates, List<Figure> figures, List<Shape> shapes) {
		AbstractEncoder<? extends Geometry> encoder = (AbstractEncoder<? extends Geometry>) Encoders.encoderFor(geom);
		encoder.encode(geom, thisShapeIndex, coordinates, figures, shapes);
	}
}
