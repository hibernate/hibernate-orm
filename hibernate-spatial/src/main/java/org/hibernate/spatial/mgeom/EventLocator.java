/*
 * $Id:$
 *
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2007-2010 Geovise BVBA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */
package org.hibernate.spatial.mgeom;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class EventLocator {

	/**
	 * Returns the point on the specified MGeometry where its measure equals the specified position.
	 *
	 * @return a Point Geometry
	 *
	 * @throws MGeometryException
	 */
	public static Point getPointGeometry(MGeometry lrs, double position)
			throws MGeometryException {
		if ( lrs == null ) {
			throw new MGeometryException( "Non-null MGeometry parameter is required." );
		}
		Coordinate c = lrs.getCoordinateAtM( position );
		Point pnt = lrs.getFactory().createPoint( c );
		copySRID( lrs.asGeometry(), pnt );
		return pnt;
	}

	public static MultiMLineString getLinearGeometry(MGeometry lrs,
													 double begin, double end) throws MGeometryException {

		if ( lrs == null ) {
			throw new MGeometryException( "Non-null MGeometry parameter is required." );
		}
		MGeometryFactory factory = (MGeometryFactory) lrs.getFactory();
		CoordinateSequence[] cs = lrs.getCoordinatesBetween( begin, end );
		List<MLineString> linestrings = new ArrayList<MLineString>( cs.length );
		for ( int i = 0; i < cs.length; i++ ) {
			MLineString ml;
			if ( cs[i].size() >= 2 ) {
				ml = factory.createMLineString( cs[i] );
				linestrings.add( ml );
			}
		}
		MultiMLineString result = factory.createMultiMLineString( linestrings.toArray( new MLineString[linestrings.size()] ) );
		copySRID( lrs.asGeometry(), result.asGeometry() );
		return result;
	}

	public static void copySRID(Geometry source, Geometry target) {
		target.setSRID( source.getSRID() );
	}

}
