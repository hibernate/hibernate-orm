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
package org.hibernate.spatial.jts.mgeom;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

/**
 * An implementation of the LineString class with the addition that the
 * containing CoordinateSequence can carry measure. Note that this is not a
 * strict requirement of the class, and can interact with non-measure geometries
 * for JTS topological comparisons regardless.
 *
 * @author Karel Maesen
 */
public class MLineString extends LineString implements MGeometry {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private boolean monotone = false;

	private boolean strictMonotone = false;

	public MLineString(CoordinateSequence points, GeometryFactory factory) {
		super( points, factory );
		determineMonotone();
	}

	public Object clone() {
		LineString ls = (LineString) super.clone();
		return new MLineString( ls.getCoordinateSequence(), this.getFactory() );
	}

	/**
	 * Calculates whether the measures in the CoordinateSequence are monotone
	 * and strict monotone. The strict parameter indicates whether the
	 * determination should apply the definition of "strict monotonicity" or
	 * non-strict.
	 */
	private void determineMonotone() {
		this.monotone = true;
		this.strictMonotone = true;
		if ( !this.isEmpty() ) {
			double m[] = this.getMeasures();
			// short circuit if the first value is NaN
			if ( Double.isNaN( m[0] ) ) {
				this.monotone = false;
				this.strictMonotone = false;
			}
			else {
				int result = 0;
				int prevResult = 0;
				for ( int i = 1; i < m.length && this.monotone; i++ ) {
					result = Double.compare( m[i - 1], m[i] );
					this.monotone = !( result * prevResult < 0 || Double
							.isNaN( m[i] ) );
					this.strictMonotone &= this.monotone && result != 0;
					prevResult = result;
				}
			}
		}
		// if not monotone, then certainly not strictly monotone
		assert ( !( this.strictMonotone && !this.monotone ) );
	}

	protected void geometryChangedAction() {
		determineMonotone();
	}

	/**
	 * @param co input coordinate in the neighbourhood of the MLineString
	 * @param tolerance max. distance that co may be from this MLineString
	 *
	 * @return an MCoordinate on this MLineString with appropriate M-value
	 */
	public MCoordinate getClosestPoint(Coordinate co, double tolerance)
			throws MGeometryException {
		if ( !this.isMonotone( false ) ) {
			throw new MGeometryException(
					MGeometryException.OPERATION_REQUIRES_MONOTONE
			);
		}

		if ( !this.isEmpty() ) {
			LineSegment seg = new LineSegment();
			Coordinate[] coAr = this.getCoordinates();
			seg.p0 = coAr[0];
			double d = 0.0;
			double projfact = 0.0;
			double minDist = Double.POSITIVE_INFINITY;
			MCoordinate mincp = null;
			for ( int i = 1; i < coAr.length; i++ ) {
				seg.p1 = coAr[i];
				Coordinate cp = seg.closestPoint( co );
				d = cp.distance( co );
				if ( d <= tolerance && d <= minDist ) {
					MCoordinate testcp = new MCoordinate( cp );
					projfact = seg.projectionFactor( cp );
					testcp.m = ( (MCoordinate) coAr[i - 1] ).m
							+ projfact
							* ( ( (MCoordinate) coAr[i] ).m - ( (MCoordinate) coAr[i - 1] ).m );
					if ( d < minDist || testcp.m < mincp.m ) {
						mincp = testcp;
						minDist = d;
					}
				}
				seg.p0 = seg.p1;
			}
			if ( minDist > tolerance ) {
				return null;
			}
			else {
				return mincp;
			}
		}
		else {
			return null;
		}
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernatespatial.mgeom.MGeometry#getCoordinateAtM(double)
		  */

	public Coordinate getCoordinateAtM(double m) throws MGeometryException {
		if ( !this.isMonotone( false ) ) {
			throw new MGeometryException(
					MGeometryException.OPERATION_REQUIRES_MONOTONE
			);
		}
		if ( this.isEmpty() ) {
			return null;
		}
		else {
			double mval[] = this.getMeasures();
			double lb = getMinM();
			double up = getMaxM();

			if ( m < lb || m > up ) {
				return null;
			}
			else {
				// determine linesegment that contains m;
				for ( int i = 1; i < mval.length; i++ ) {
					if ( ( mval[i - 1] <= m && m <= mval[i] )
							|| ( mval[i] <= m && m <= mval[i - 1] ) ) {
						MCoordinate p0 = (MCoordinate) this
								.getCoordinateN( i - 1 );
						MCoordinate p1 = (MCoordinate) this.getCoordinateN( i );
						// r indicates how far in this segment the M-values lies
						double r = ( m - mval[i - 1] ) / ( mval[i] - mval[i - 1] );
						double dx = r * ( p1.x - p0.x );
						double dy = r * ( p1.y - p0.y );
						double dz = r * ( p1.z - p0.z );
						MCoordinate nc = new MCoordinate(
								p0.x + dx, p0.y + dy,
								p0.z + dz, m
						);
						return nc;
					}
				}
			}
		}
		return null;
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see com.vividsolutions.jts.geom.Geometry#getGeometryType()
		  */

	public String getGeometryType() {
		return "MLineString";
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see com.vividsolutions.jts.geom.Geometry#getMatCoordinate(com.vividsolutions.jts.geom.Coordinate,
		  *      double)
		  */

	public double getMatCoordinate(Coordinate c, double tolerance)
			throws MGeometryException {
		MCoordinate mco = this.getClosestPoint( c, tolerance );
		if ( mco == null ) {
			return Double.NaN;
		}
		else {
			return ( mco.m );
		}
	}

	/**
	 * get the measure of the specified coordinate
	 *
	 * @param n index of the coordinate
	 *
	 * @return The measure of the coordinate. If the coordinate does not exists
	 *         it returns Double.NaN
	 */
	public double getMatN(int n) {
		return ( (MCoordinate) ( this.getCoordinates()[n] ) ).m;
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.spatial.mgeom.MGeometry##MGeometry#getMaxM()
		  */

	public double getMaxM() {
		if ( this.isEmpty() ) {
			return Double.NaN;
		}
		else {
			double[] measures = this.getMeasures();

			if ( this.getMeasureDirection() == INCREASING ) {
				return measures[measures.length - 1];
			}
			else if ( this.getMeasureDirection() == DECREASING
					|| this.getMeasureDirection() == CONSTANT ) {
				return measures[0];
			}
			else {
				double ma = Double.NEGATIVE_INFINITY;
				for ( int i = 0; i < measures.length; i++ ) {
					if ( ma < measures[i] ) {
						ma = measures[i];
					}
				}
				return ma;
			}
		}
	}


	/**
	 * Copies the coordinates of the specified array that fall between fromM and toM to a CoordinateSubSequence.
	 * <p/>
	 * The CoordinateSubSequence also contains the array indices of the first and last coordinate in firstIndex, resp.
	 * lastIndex. If there are no coordinates between fromM and toM, then firstIndex will contain -1, and lastIndex
	 * will point to the coordinate that is close to fromM or toM.
	 * <p/>
	 * This function expects that fromM is less than or equal to toM, and that the coordinates in the array are
	 * sorted monotonic w.r.t. to their m-values.
	 *
	 * @param mcoordinates
	 * @param fromM
	 * @param toM
	 * @param direction INCREASING or DECREASING
	 *
	 * @return a CoordinateSubSequence containing the coordinates between fromM and toM
	 */
	private CoordinateSubSequence copyCoordinatesBetween(MCoordinate[] mcoordinates, double fromM, double toM, int direction) {
		CoordinateSubSequence sseq = new CoordinateSubSequence();
		sseq.firstIndex = -1;
		sseq.lastIndex = -1;
		for ( int i = 0; i < mcoordinates.length; i++ ) {
			double m = mcoordinates[i].m;

			if ( m >= fromM && m <= toM ) {
				sseq.vertices.add( mcoordinates[i] );
				if ( sseq.firstIndex == -1 ) {
					sseq.firstIndex = i;
				}
			}
			if ( direction == INCREASING ) {
				if ( m > toM ) {
					break;
				}
				sseq.lastIndex = i;
			}
			else {
				if ( m < fromM ) {
					break;
				}
				sseq.lastIndex = i;
			}

		}
		return sseq;
	}

	/**
	 * Interpolates a coordinate between mco1, mco2, based on the measured value m
	 */
	private MCoordinate interpolate(MCoordinate mco1, MCoordinate mco2, double m) {
		if ( mco1.m > mco2.m ) {
			MCoordinate h = mco1;
			mco1 = mco2;
			mco2 = h;
		}

		if ( m < mco1.m || m > mco2.m ) {
			throw new IllegalArgumentException( "Internal Error: m-value not in interval mco1.m/mco2.m" );
		}

		double r = ( m - mco1.m ) / ( mco2.m - mco1.m );
		MCoordinate interpolated = new MCoordinate(
				mco1.x + r * ( mco2.x - mco1.x ),
				mco1.y + r * ( mco2.y - mco1.y ),
				mco1.z + r * ( mco2.z - mco1.z ),
				m
		);
		this.getPrecisionModel().makePrecise( interpolated );
		return interpolated;
	}


	public CoordinateSequence[] getCoordinatesBetween(double fromM, double toM) throws MGeometryException {
		if ( !this.isMonotone( false ) ) {
			throw new MGeometryException(
					MGeometryException.OPERATION_REQUIRES_MONOTONE,
					"Operation requires geometry with monotonic measures"
			);
		}

		if ( fromM > toM ) {
			return getCoordinatesBetween( toM, fromM );
		}

		MCoordinateSequence mc;
		if ( !isOverlapping( fromM, toM ) ) {
			mc = new MCoordinateSequence( new MCoordinate[] { } );
		}
		else {
			MCoordinate[] mcoordinates = (MCoordinate[]) this.getCoordinates();
			CoordinateSubSequence subsequence = copyCoordinatesBetween(
					mcoordinates,
					fromM,
					toM,
					this.getMeasureDirection()
			);
			addInterpolatedEndPoints( fromM, toM, mcoordinates, subsequence );
			MCoordinate[] ra = subsequence.vertices.toArray( new MCoordinate[subsequence.vertices.size()] );
			mc = new MCoordinateSequence( ra );
		}
		return new MCoordinateSequence[] { mc };
	}

	private boolean isOverlapping(double fromM, double toM) {
		if ( this.isEmpty() ) {
			return false;
		}
		//WARNING: this assumes a monotonic increasing or decreasing measures
		MCoordinate beginCo = (MCoordinate) this.getCoordinateN( 0 );
		MCoordinate endCo = (MCoordinate) this.getCoordinateN( this.getNumPoints() - 1 );
		return !( Math.min( fromM, toM ) > Math.max( beginCo.m, endCo.m ) ||
				Math.max( fromM, toM ) < Math.min( beginCo.m, endCo.m ) );
	}

	private void addInterpolatedEndPoints(double fromM, double toM, MCoordinate[] mcoordinates, CoordinateSubSequence subsequence) {

		boolean increasing = this.getMeasureDirection() == INCREASING;
		double fM, lM;
		if ( increasing ) {
			fM = fromM;
			lM = toM;
		}
		else {
			fM = toM;
			lM = fromM;
		}

		if ( subsequence.firstIndex == -1 ) {
			MCoordinate fi = interpolate(
					mcoordinates[subsequence.lastIndex],
					mcoordinates[subsequence.lastIndex + 1],
					fM
			);
			subsequence.vertices.add( fi );
			MCoordinate li = interpolate(
					mcoordinates[subsequence.lastIndex],
					mcoordinates[subsequence.lastIndex + 1],
					lM
			);
			subsequence.vertices.add( li );
		}
		else {
			//interpolate a first vertex if necessary
			if ( subsequence.firstIndex > 0 && (
					increasing && mcoordinates[subsequence.firstIndex].m > fromM ||
							!increasing && mcoordinates[subsequence.firstIndex].m < toM
			) ) {
				MCoordinate fi = interpolate(
						mcoordinates[subsequence.firstIndex - 1],
						mcoordinates[subsequence.firstIndex],
						fM
				);
				subsequence.vertices.add( 0, fi );
			}
			//interpolate a last vertex if necessary
			if ( subsequence.lastIndex < ( mcoordinates.length - 1 ) && (
					increasing && mcoordinates[subsequence.lastIndex].m < toM ||
							!increasing && mcoordinates[subsequence.lastIndex].m > fromM ) ) {
				MCoordinate li = interpolate(
						mcoordinates[subsequence.lastIndex],
						mcoordinates[subsequence.lastIndex + 1],
						lM
				);
				subsequence.vertices.add( li );
			}
		}
	}

	private MCoordinate[] inverse(MCoordinate[] mcoordinates) {
		for ( int i = 0; i < mcoordinates.length / 2; i++ ) {
			MCoordinate h = mcoordinates[i];
			mcoordinates[i] = mcoordinates[mcoordinates.length - 1 - i];
			mcoordinates[mcoordinates.length - 1 - i] = h;
		}
		return mcoordinates;
	}


	/**
	 * determine the direction of the measures w.r.t. the direction of the line
	 *
	 * @return MGeometry.NON_MONOTONE<BR>
	 *         MGeometry.INCREASING<BR>
	 *         MGeometry.DECREASING<BR>
	 *         MGeometry.CONSTANT
	 */
	public int getMeasureDirection() {
		if ( !this.monotone ) {
			return NON_MONOTONE;
		}
		MCoordinate c1 = (MCoordinate) this.getCoordinateN( 0 );
		MCoordinate c2 = (MCoordinate) this
				.getCoordinateN( this.getNumPoints() - 1 );

		if ( c1.m < c2.m ) {
			return INCREASING;
		}
		else if ( c1.m > c2.m ) {
			return DECREASING;
		}
		else {
			return CONSTANT;
		}
	}

	/**
	 * @return the array with measure-values of the vertices
	 */
	public double[] getMeasures() {
		// return the measures of all vertices
		if ( !this.isEmpty() ) {
			Coordinate[] co = this.getCoordinates();
			double[] a = new double[co.length];
			for ( int i = 0; i < co.length; i++ ) {
				a[i] = ( (MCoordinate) co[i] ).m;
			}
			return a;
		}
		else {
			return null;
		}
	}

	public double getMinM() {

		if ( this.isEmpty() ) {
			return Double.NaN;
		}
		else {
			double[] a = this.getMeasures();
			if ( this.getMeasureDirection() == INCREASING ) {
				return a[0];
			}
			else if ( this.getMeasureDirection() == DECREASING
					|| this.getMeasureDirection() == CONSTANT ) {
				return a[a.length - 1];
			}
			else {

				double ma = Double.POSITIVE_INFINITY;
				for ( int i = 0; i < a.length; i++ ) {
					if ( ma > a[i] ) {
						ma = a[i];
					}
				}
				return ma;
			}
		}
	}

	/**
	 * Assigns the first coordinate in the CoordinateSequence to the
	 * <code>beginMeasure</code> and the last coordinate in the
	 * CoordinateSequence to the <code>endMeasure</code>. Measure values for
	 * intermediate coordinates are then interpolated proportionally based on
	 * their 2d offset of the overall 2d length of the LineString.
	 * <p/>
	 * If the beginMeasure and endMeasure values are equal it is assumed that
	 * all intermediate coordinates shall be the same value.
	 *
	 * @param beginMeasure Measure value for first coordinate
	 * @param endMeasure Measure value for last coordinate
	 */
	public void interpolate(double beginMeasure, double endMeasure) {
		if ( this.isEmpty() ) {
			return;
		}
		// interpolate with first vertex = beginMeasure; last vertex =
		// endMeasure
		Coordinate[] coordinates = this.getCoordinates();
		double length = this.getLength();
		double mLength = endMeasure - beginMeasure;
		double d = 0;
		boolean continuous = DoubleComparator.equals( beginMeasure, endMeasure );
		double m = beginMeasure;
		MCoordinate prevCoord = MCoordinate.convertCoordinate( coordinates[0] );
		prevCoord.m = m;
		MCoordinate curCoord;
		for ( int i = 1; i < coordinates.length; i++ ) {
			curCoord = MCoordinate.convertCoordinate( coordinates[i] );
			if ( continuous ) {
				curCoord.m = beginMeasure;
			}
			else {
				d += curCoord.distance( prevCoord );
				m = beginMeasure + ( d / length ) * mLength;
				curCoord.m = m;
				prevCoord = curCoord;
			}
		}
		this.geometryChanged();
		assert ( this.isMonotone( false ) ) : "interpolate function should always leave MGeometry monotone";
	}

	/**
	 * Returns the measure length of the segment. This method assumes that the
	 * length of the LineString is defined by the absolute value of (last
	 * coordinate - first coordinate) in the CoordinateSequence. If either
	 * measure is not defined or the CoordinateSequence contains no coordinates,
	 * then Double.NaN is returned. If there is only 1 element in the
	 * CoordinateSequence, then 0 is returned.
	 *
	 * @return The measure length of the LineString
	 */
	public double getMLength() {
		if ( getCoordinateSequence().size() == 0 ) {
			return Double.NaN;
		}
		if ( getCoordinateSequence().size() == 1 ) {
			return 0.0D;
		}
		else {
			int lastIndex = getCoordinateSequence().size() - 1;
			double begin = getCoordinateSequence().getOrdinate(
					0,
					CoordinateSequence.M
			);
			double end = getCoordinateSequence().getOrdinate(
					lastIndex,
					CoordinateSequence.M
			);
			return ( Double.isNaN( begin ) || Double.isNaN( end ) ) ? Double.NaN
					: Math.abs( end - begin );
		}
	}

	/**
	 * Indicates whether the MLineString has monotone increasing or decreasing
	 * M-values
	 *
	 * @return <code>true if MLineString is empty or M-values are increasing (NaN) values, false otherwise</code>
	 */
	public boolean isMonotone(boolean strict) {
		return strict ? this.strictMonotone : this.monotone;
	}

	public Geometry asGeometry() {
		return this;
	}

	// TODO get clear on function and implications of normalize
	// public void normalize(){
	//
	// }

	public void measureOnLength(boolean keepBeginMeasure) {

		Coordinate[] co = this.getCoordinates();
		if ( !this.isEmpty() ) {
			double d = 0.0;
			MCoordinate pco = (MCoordinate) co[0];
			if ( !keepBeginMeasure || Double.isNaN( pco.m ) ) {
				pco.m = 0.0d;
			}
			MCoordinate mco;
			for ( int i = 1; i < co.length; i++ ) {
				mco = (MCoordinate) co[i];
				d += mco.distance( pco );
				mco.m = d;
				pco = mco;
			}
			this.geometryChanged();
		}
	}

	/**
	 * This method reverses the measures assigned to the Coordinates in the
	 * CoordinateSequence without modifying the positional (x,y,z) values.
	 */
	public void reverseMeasures() {
		if ( !this.isEmpty() ) {
			double m[] = this.getMeasures();
			MCoordinate[] coar = (MCoordinate[]) this.getCoordinates();
			double nv;
			for ( int i = 0; i < m.length; i++ ) {
				nv = m[m.length - 1 - i];
				coar[i].m = nv;
			}
			this.geometryChanged();
		}
	}

	public void setMeasureAtIndex(int index, double m) {
		getCoordinateSequence().setOrdinate( index, CoordinateSequence.M, m );
		this.geometryChanged();
	}

	/**
	 * Shift all measures by the amount parameter. A negative amount shall
	 * subtract the amount from the measure. Note that this can make for
	 * negative measures.
	 *
	 * @param amount the positive or negative amount by which to shift the measures
	 * in the CoordinateSequence.
	 */
	public void shiftMeasure(double amount) {
		Coordinate[] coordinates = this.getCoordinates();
		MCoordinate mco;
		if ( !this.isEmpty() ) {
			for ( int i = 0; i < coordinates.length; i++ ) {
				mco = (MCoordinate) coordinates[i];
				mco.m = mco.m + amount;
			}
		}
		this.geometryChanged();
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see java.lang.Object#toString()
		  */

	public String toString() {
		Coordinate[] ar = this.getCoordinates();
		StringBuffer buf = new StringBuffer( ar.length * 17 * 3 );
		for ( int i = 0; i < ar.length; i++ ) {
			buf.append( ar[i].x );
			buf.append( " " );
			buf.append( ar[i].y );
			buf.append( " " );
			buf.append( ( (MCoordinate) ar[i] ).m );
			buf.append( "\n" );
		}
		return buf.toString();
	}

	public MLineString unionM(MLineString l) throws MGeometryException {

		if ( !this.monotone || !l.monotone ) {
			throw new MGeometryException(
					MGeometryException.OPERATION_REQUIRES_MONOTONE
			);
		}
		Coordinate[] linecoar = l.getCoordinates();
		if ( l.getMeasureDirection() == DECREASING ) {
			CoordinateArrays.reverse( linecoar );
		}
		Coordinate[] thiscoar = this.getCoordinates();
		if ( this.getMeasureDirection() == DECREASING ) {
			CoordinateArrays.reverse( thiscoar );
		}

		// either the last coordinate in thiscoar equals the first in linecoar;
		// or the last in linecoar equals the first in thiscoar;
		MCoordinate lasttco = (MCoordinate) thiscoar[thiscoar.length - 1];
		MCoordinate firsttco = (MCoordinate) thiscoar[0];
		MCoordinate lastlco = (MCoordinate) linecoar[linecoar.length - 1];
		MCoordinate firstlco = (MCoordinate) linecoar[0];

		MCoordinate[] newcoar = new MCoordinate[thiscoar.length
				+ linecoar.length - 1];
		if ( lasttco.equals2D( firstlco )
				&& DoubleComparator.equals( lasttco.m, firstlco.m ) ) {
			System.arraycopy( thiscoar, 0, newcoar, 0, thiscoar.length );
			System.arraycopy(
					linecoar, 1, newcoar, thiscoar.length,
					linecoar.length - 1
			);
		}
		else if ( lastlco.equals2D( firsttco )
				&& DoubleComparator.equals( lastlco.m, firsttco.m ) ) {
			System.arraycopy( linecoar, 0, newcoar, 0, linecoar.length );
			System.arraycopy(
					thiscoar, 1, newcoar, linecoar.length,
					thiscoar.length - 1
			);
		}
		else {
			throw new MGeometryException(
					MGeometryException.UNIONM_ON_DISJOINT_MLINESTRINGS
			);
		}

		CoordinateSequence mcs = this.getFactory()
				.getCoordinateSequenceFactory().create( newcoar );
		MLineString returnmlinestring = new MLineString( mcs, this.getFactory() );
		assert ( returnmlinestring.isMonotone( false ) ) : "new unionM-ed MLineString is not monotone";
		return returnmlinestring;
	}

	static class CoordinateSubSequence {
		private int firstIndex;
		private int lastIndex;
		private List<MCoordinate> vertices = new ArrayList<MCoordinate>();
	}
}
