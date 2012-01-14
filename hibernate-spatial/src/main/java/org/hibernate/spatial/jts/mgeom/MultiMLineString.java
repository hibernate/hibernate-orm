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
package org.hibernate.spatial.jts.mgeom;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;

public class MultiMLineString extends MultiLineString implements MGeometry {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final double mGap; // difference in m between end of one part and

	private boolean monotone = false;

	private boolean strictMonotone = false;

	/**
	 * @param MlineStrings the <code>MLineString</code>s for this
	 * <code>MultiMLineString</code>, or <code>null</code> or an
	 * empty array to create the empty geometry. Elements may be
	 * empty <code>LineString</code>s, but not <code>null</code>s.
	 */
	public MultiMLineString(MLineString[] MlineStrings, double mGap,
							GeometryFactory factory) {
		super( MlineStrings, factory );
		this.mGap = mGap;
		determineMonotone();
	}

	/**
	 * TODO Improve this, and add more unit tests
	 */
	private void determineMonotone() {
		this.monotone = true;
		this.strictMonotone = true;
		if ( this.isEmpty() ) {
			return;
		}
		int mdir = CONSTANT;
		for ( int i = 0; i < this.geometries.length; i++ ) {
			MLineString ml = (MLineString) this.geometries[0];
			if ( !ml.isEmpty() ) {
				mdir = ml.getMeasureDirection();
				break;
			}
		}
		for ( int i = 0; i < this.geometries.length; i++ ) {
			MLineString ml = (MLineString) this.geometries[i];
			if ( ml.isEmpty() ) {
				continue;
			}
			// check whether mlinestrings are all pointing in same direction,
			// and
			// are monotone
			if ( !ml.isMonotone( false )
					|| ( ml.getMeasureDirection() != mdir && !( ml
					.getMeasureDirection() == CONSTANT) ) ) {
				this.monotone = false;
				break;
			}

			if ( !ml.isMonotone( true ) || ( ml.getMeasureDirection() != mdir ) ) {
				this.strictMonotone = false;
				break;
			}

			// check whether the geometry measures do not overlap or
			// are inconsistent with previous parts
			if ( i > 0 ) {
				MLineString mlp = (MLineString) this.geometries[i - 1];
				if ( mdir == INCREASING) {
					if ( mlp.getMaxM() > ml.getMinM() ) {
						monotone = false;
					}
					else if ( mlp.getMaxM() >= ml.getMinM() ) {
						strictMonotone = false;
					}
				}
				else {
					if ( mlp.getMinM() < ml.getMaxM() ) {
						monotone = false;
					}
					else if ( mlp.getMinM() <= ml.getMaxM() ) {
						strictMonotone = false;
					}
				}

			}

		}
		if ( !monotone ) {
			this.strictMonotone = false;
		}

	}

	protected void geometryChangedAction() {
		determineMonotone();
	}

	public String getGeometryType() {
		return "MultiMLineString";
	}

	public double getMGap() {
		return this.mGap;
	}

	public double getMatCoordinate(Coordinate co, double tolerance)
			throws MGeometryException {

		if ( !this.isMonotone( false ) ) {
			throw new MGeometryException(
					MGeometryException.OPERATION_REQUIRES_MONOTONE
			);
		}

		double mval = Double.NaN;
		double dist = Double.POSITIVE_INFINITY;

		com.vividsolutions.jts.geom.Point p = this.getFactory().createPoint( co );

		// find points within tolerance for getMatCoordinate
		for ( int i = 0; i < this.getNumGeometries(); i++ ) {
			MLineString ml = (MLineString) this.getGeometryN( i );
			// go to next MLineString if the input point is beyond tolerance
			if ( ml.distance( p ) > tolerance ) {
				continue;
			}

			MCoordinate mc = ml.getClosestPoint( co, tolerance );
			if ( mc != null ) {
				double d = mc.distance( co );
				if ( d <= tolerance && d < dist ) {
					dist = d;
					mval = mc.m;
				}
			}
		}
		return mval;
	}

	public Object clone() {
		MultiLineString ml = (MultiLineString) super.clone();
		return ml;
	}

	public void measureOnLength(boolean keepBeginMeasure) {
		double startM = 0.0;
		for ( int i = 0; i < this.getNumGeometries(); i++ ) {
			MLineString ml = (MLineString) this.getGeometryN( i );
			if ( i == 0 ) {
				ml.measureOnLength( keepBeginMeasure );
			}
			else {
				ml.measureOnLength( false );
			}
			if ( startM != 0.0 ) {
				ml.shiftMeasure( startM );
			}
			startM += ml.getLength() + mGap;
		}
		this.geometryChanged();
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.spatial.mgeom.MGeometry#getCoordinateAtM(double)
		  */

	public Coordinate getCoordinateAtM(double m) throws MGeometryException {

		if ( !this.isMonotone( false ) ) {
			throw new MGeometryException(
					MGeometryException.OPERATION_REQUIRES_MONOTONE
			);
		}

		Coordinate c = null;
		for ( int i = 0; i < this.getNumGeometries(); i++ ) {
			MGeometry mg = (MGeometry) this.getGeometryN( i );
			c = mg.getCoordinateAtM( m );
			if ( c != null ) {
				return c;
			}
		}
		return null;
	}

	public CoordinateSequence[] getCoordinatesBetween(double begin, double end)
			throws MGeometryException {

		if ( !this.isMonotone( false ) ) {
			throw new MGeometryException(
					MGeometryException.OPERATION_REQUIRES_MONOTONE,
					"Operation requires geometry with monotonic measures"
			);
		}

		if ( this.isEmpty() ) {
			return null;
		}

		java.util.ArrayList<CoordinateSequence> ar = new java.util.ArrayList<CoordinateSequence>();

		for ( int i = 0; i < this.getNumGeometries(); i++ ) {
			MLineString ml = (MLineString) this.getGeometryN( i );
			for ( CoordinateSequence cs : ml.getCoordinatesBetween( begin, end ) ) {
				if ( cs.size() > 0 ) {
					ar.add( cs );
				}
			}
		}
		return ar.toArray( new CoordinateSequence[ar.size()] );
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.spatial.mgeom.MGeometry#getMinM()
		  */

	public double getMinM() {
		double minM = Double.POSITIVE_INFINITY;
		for ( int i = 0; i < this.getNumGeometries(); i++ ) {
			MLineString ml = (MLineString) this.getGeometryN( i );
			double d = ml.getMinM();
			if ( d < minM ) {
				minM = d;
			}
		}
		return minM;
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.spatial.mgeom.MGeometry#getMaxM()
		  */

	public double getMaxM() {
		double maxM = Double.NEGATIVE_INFINITY;
		for ( int i = 0; i < this.getNumGeometries(); i++ ) {
			MLineString ml = (MLineString) this.getGeometryN( i );
			double d = ml.getMaxM();
			if ( d > maxM ) {
				maxM = d;
			}
		}
		return maxM;
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.spatial.mgeom.MGeometry#isMonotone()
		  */

	public boolean isMonotone(boolean strictMonotone) {
		return strictMonotone ? this.strictMonotone : monotone;
	}

	public Geometry asGeometry() {
		return this;
	}
}
