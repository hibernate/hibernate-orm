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

package org.hibernate.spatial.dialect.oracle;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Jun 30, 2010
 */

class SDOGeometry {

	private static final String SQL_TYPE_NAME = "MDSYS.SDO_GEOMETRY";
	private SDOGType gtype;
	private int srid;
	private SDOPoint point;
	private ElemInfo info;
	private Ordinates ordinates;


	public SDOGeometry() {

	}

	public static String getTypeName() {
		return SQL_TYPE_NAME;
	}

	static String arrayToString(Object array) {
		if ( array == null || java.lang.reflect.Array.getLength( array ) == 0 ) {
			return "()";
		}
		final int length = java.lang.reflect.Array.getLength( array );
		final StringBuilder stb = new StringBuilder();
		stb.append( "(" ).append( java.lang.reflect.Array.get( array, 0 ) );
		for ( int i = 1; i < length; i++ ) {
			stb.append( "," ).append( java.lang.reflect.Array.get( array, i ) );
		}
		stb.append( ")" );
		return stb.toString();
	}

	/**
	 * This joins an array of SDO_GEOMETRIES to a SDOGeometry of type
	 * COLLECTION
	 *
	 * @param sdoElements The SDO_geometries to join into an SDO Geometry Collection
	 *
	 * @return The SDO Collection Geometry
	 */
	public static SDOGeometry join(SDOGeometry[] sdoElements) {
		final SDOGeometry sdoCollection = new SDOGeometry();
		if ( sdoElements == null || sdoElements.length == 0 ) {
			sdoCollection.setGType( new SDOGType( 2, 0, TypeGeometry.COLLECTION ) );
		}
		else {
			final SDOGeometry firstElement = sdoElements[0];
			final int dim = firstElement.getGType().getDimension();
			final int lrsDim = firstElement.getGType().getLRSDimension();
			sdoCollection.setGType( new SDOGType( dim, lrsDim, TypeGeometry.COLLECTION ) );
			int ordinatesOffset = 1;
			for ( int i = 0; i < sdoElements.length; i++ ) {
				final ElemInfo element = sdoElements[i].getInfo();
				final Double[] ordinates = sdoElements[i].getOrdinates().getOrdinateArray();
				if ( element != null && element.getSize() > 0 ) {
					final int shift = ordinatesOffset - element.getOrdinatesOffset( 0 );
					shiftOrdinateOffset( element, shift );
					sdoCollection.addElement( element );
					sdoCollection.addOrdinates( ordinates );
					ordinatesOffset += ordinates.length;
				}
			}
		}
		return sdoCollection;
	}

	private static void shiftOrdinateOffset(ElemInfo elemInfo, int offset) {
		for ( int i = 0; i < elemInfo.getSize(); i++ ) {
			final int newOffset = elemInfo.getOrdinatesOffset( i ) + offset;
			elemInfo.setOrdinatesOffset( i, newOffset );
		}
	}

	private static SDOGType deriveGTYPE(ElementType elementType,
										SDOGeometry origGeom) {
		switch ( elementType ) {
			case POINT:
			case ORIENTATION:
				return new SDOGType(
						origGeom.getDimension(), origGeom
						.getLRSDimension(), TypeGeometry.POINT
				);
			case POINT_CLUSTER:
				return new SDOGType(
						origGeom.getDimension(), origGeom
						.getLRSDimension(), TypeGeometry.MULTIPOINT
				);
			case LINE_ARC_SEGMENTS:
			case LINE_STRAITH_SEGMENTS:
			case COMPOUND_LINE:
				return new SDOGType(
						origGeom.getDimension(), origGeom
						.getLRSDimension(), TypeGeometry.LINE
				);
			case COMPOUND_EXTERIOR_RING:
			case EXTERIOR_RING_ARC_SEGMENTS:
			case EXTERIOR_RING_CIRCLE:
			case EXTERIOR_RING_RECT:
			case EXTERIOR_RING_STRAIGHT_SEGMENTS:
				return new SDOGType(
						origGeom.getDimension(), origGeom
						.getLRSDimension(), TypeGeometry.POLYGON
				);
			default:
				return null;
		}
	}

	public static SDOGeometry load(Struct struct) {

		Object[] data;
		try {
			data = struct.getAttributes();
		}
		catch ( SQLException e ) {
			throw new RuntimeException( e );
		}

		final SDOGeometry geom = new SDOGeometry();
		geom.setGType( SDOGType.parse( data[0] ) );
		geom.setSRID( data[1] );
		if ( data[2] != null ) {
			geom.setPoint( new SDOPoint( (Struct) data[2] ) );
		}
		geom.setInfo( new ElemInfo( (Array) data[3] ) );
		geom.setOrdinates( new Ordinates( (Array) data[4] ) );

		return geom;
	}

	public ElemInfo getInfo() {
		return info;
	}

	public void setInfo(ElemInfo info) {
		this.info = info;
	}

	public SDOGType getGType() {
		return gtype;
	}

	public void setGType(SDOGType gtype) {
		this.gtype = gtype;
	}

	public Ordinates getOrdinates() {
		return ordinates;
	}

	public void setOrdinates(Ordinates ordinates) {
		this.ordinates = ordinates;
	}

	public SDOPoint getPoint() {
		return point;
	}

	public void setPoint(SDOPoint point) {
		this.point = point;
	}

	public int getSRID() {
		return srid;
	}

	public void setSRID(int srid) {
		this.srid = srid;
	}

	private void setSRID(Object datum) {
		if ( datum == null ) {
			this.srid = 0;
			return;
		}
		try {
			this.srid = ( (Number) datum ).intValue();
		}
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	public boolean isLRSGeometry() {
		return gtype.isLRSGeometry();
	}

	public int getDimension() {
		return gtype.getDimension();
	}

	public int getLRSDimension() {
		return gtype.getLRSDimension();
	}

	public int getZDimension() {
		return gtype.getZDimension();
	}

	/**
	 * Gets the number of elements or compound elements.
	 * <p/>
	 * Subelements of a compound element are not counted.
	 *
	 * @return the number of elements
	 */
	public int getNumElements() {
		int cnt = 0;
		int i = 0;
		while ( i < info.getSize() ) {
			if ( info.getElementType( i ).isCompound() ) {
				final int numCompounds = info.getNumCompounds( i );
				i += 1 + numCompounds;
			}
			else {
				i++;
			}
			cnt++;
		}
		return cnt;
	}

	public String toString() {
		final StringBuilder stb = new StringBuilder();
		stb.append( "(" ).append( gtype ).append( "," ).append( srid ).append( "," )
				.append( point ).append( "," ).append( info ).append( "," ).append(
				ordinates
		).append( ")" );
		return stb.toString();
	}

	public void addOrdinates(Double[] newOrdinates) {
		if ( this.ordinates == null ) {
			this.ordinates = new Ordinates( newOrdinates );
		}
		else {
			this.ordinates.addOrdinates( newOrdinates );
		}
	}

	public void addElement(ElemInfo element) {
		if ( this.info == null ) {
			this.info = element;
		}
		else {
			this.info.addElement( element );
		}
	}

	/**
	 * If this SDOGeometry is a COLLECTION, this method returns an array of
	 * the SDO_GEOMETRIES that make up the collection. If not a Collection,
	 * an array containing this SDOGeometry is returned.
	 *
	 * @return collection elements as individual SDO_GEOMETRIES
	 */
	public SDOGeometry[] getElementGeometries() {
		if ( getGType().getTypeGeometry() == TypeGeometry.COLLECTION ) {
			final List<SDOGeometry> elements = new ArrayList<SDOGeometry>();
			int i = 0;
			while ( i < this.getNumElements() ) {
				final ElementType et = this.getInfo().getElementType( i );
				int next = i + 1;
				// if the element is an exterior ring, or a compound
				// element, then this geometry spans multiple elements.
				if ( et.isExteriorRing() ) {
					// then next element is the
					// first non-interior ring
					while ( next < this.getNumElements() ) {
						if ( !this.getInfo().getElementType( next )
								.isInteriorRing() ) {
							break;
						}
						next++;
					}
				}
				else if ( et.isCompound() ) {
					next = i + this.getInfo().getNumCompounds( i ) + 1;
				}
				final SDOGeometry elemGeom = new SDOGeometry();
				final SDOGType elemGtype = deriveGTYPE( this.getInfo().getElementType( i ), this );
				elemGeom.setGType( elemGtype );
				elemGeom.setSRID( this.getSRID() );
				final ElemInfo elemInfo = new ElemInfo( this.getInfo().getElement( i ) );
				shiftOrdinateOffset( elemInfo, -elemInfo.getOrdinatesOffset( 0 ) + 1 );
				elemGeom.setInfo( elemInfo );
				final int startPosition = this.getInfo().getOrdinatesOffset( i );
				Ordinates elemOrdinates = null;
				if ( next < this.getNumElements() ) {
					final int endPosition = this.getInfo().getOrdinatesOffset( next );
					elemOrdinates = new Ordinates(
							this.getOrdinates()
									.getOrdinatesArray( startPosition, endPosition )
					);
				}
				else {
					elemOrdinates = new Ordinates(
							this.getOrdinates()
									.getOrdinatesArray( startPosition )
					);
				}
				elemGeom.setOrdinates( elemOrdinates );
				elements.add( elemGeom );
				i = next;
			}
			return elements.toArray( new SDOGeometry[elements.size()] );
		}
		else {
			return new SDOGeometry[] { this };
		}
	}

}
