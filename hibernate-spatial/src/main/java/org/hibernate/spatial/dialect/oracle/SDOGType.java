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

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Jun 30, 2010
 */
class SDOGType {

	private int dimension = 2;

	private int lrsDimension = 0;

	private TypeGeometry typeGeometry = TypeGeometry.UNKNOWN_GEOMETRY;

	public SDOGType(int dimension, int lrsDimension,
					TypeGeometry typeGeometry) {
		setDimension( dimension );
		setLrsDimension( lrsDimension );
		setTypeGeometry( typeGeometry );
	}

	public int getDimension() {
		return dimension;
	}

	public void setDimension(int dimension) {
		if ( dimension < 2 || dimension > 4 ) {
			throw new IllegalArgumentException(
					"Dimension can only be 2,3 or 4."
			);
		}
		this.dimension = dimension;
	}

	public TypeGeometry getTypeGeometry() {
		return typeGeometry;
	}

	public void setTypeGeometry(TypeGeometry typeGeometry) {

		this.typeGeometry = typeGeometry;
	}

	public int getLRSDimension() {
		if ( this.lrsDimension > 0 ) {
			return this.lrsDimension;
		}
		else if ( this.lrsDimension == 0 && this.dimension == 4 ) {
			return 4;
		}
		return 0;
	}

	public int getZDimension() {
		if ( this.dimension > 2 ) {
			if ( !isLRSGeometry() ) {
				return this.dimension;
			}
			else {
				return ( getLRSDimension() < this.dimension ? 4 : 3 );
			}
		}
		return 0;
	}

	public boolean isLRSGeometry() {
		return ( this.lrsDimension > 0 || ( this.lrsDimension == 0 && this.dimension == 4 ) );
	}

	public void setLrsDimension(int lrsDimension) {
		if ( lrsDimension != 0 && lrsDimension > this.dimension ) {
			throw new IllegalArgumentException(
					"lrsDimension must be 0 or lower or equal to dimenstion."
			);
		}
		this.lrsDimension = lrsDimension;
	}

	public int intValue() {
		int v = this.dimension * 1000;
		v += lrsDimension * 100;
		v += typeGeometry.intValue();
		return v;
	}

	public static SDOGType parse(int v) {
		int dim = v / 1000;
		v -= dim * 1000;
		int lrsDim = v / 100;
		v -= lrsDim * 100;
		TypeGeometry typeGeometry = TypeGeometry.parse( v );
		return new SDOGType( dim, lrsDim, typeGeometry );
	}

	public static SDOGType parse(Object datum) {

		try {
			int v = ( (Number) datum ).intValue();
			return parse( v );
		}
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}

	}

	public String toString() {
		return Integer.toString( this.intValue() );
	}
}