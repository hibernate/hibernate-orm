/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.jts;


import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.PrecisionModel;

//TODO -- verify code, and put into Geolatte.

/**
 * This class provides operations for handling the usage of Circles and arcs in
 * Geometries.
 * <p/>
 *
 * @author Tom Acree
 */
public class Circle {
	private static final double TWO_PI = Math.PI * 2;
	private Coordinate center = new Coordinate( 0.0, 0.0 );
	private double radius;
	// Constructors **********************************************************
	private PrecisionModel precisionModel = new PrecisionModel();

	/**
	 * Creates a circle whose center is at the origin and whose radius is 0.
	 */
	protected Circle() {
	}

	/**
	 * Create a circle with a defined center and radius
	 *
	 * @param center The coordinate representing the center of the circle
	 * @param radius The radius of the circle
	 */
	public Circle(Coordinate center, double radius) {
		this.center = center;
		this.radius = radius;
	}

	/**
	 * Create a circle using the x/y coordinates for the center.
	 *
	 * @param xCenter The x coordinate of the circle's center
	 * @param yCenter The y coordinate of the circle's center
	 * @param radius the radius of the circle
	 */
	public Circle(double xCenter, double yCenter, double radius) {
		this( new Coordinate( xCenter, yCenter ), radius );
	}

	/**
	 * Creates a circle based on bounding box. It is possible for the user of
	 * this class to pass bounds to this method that do not represent a square.
	 * If this is the case, we must force the bounding rectangle to be a square.
	 * To this end, we check the box and set the side of the box to the larger
	 * dimension of the rectangle
	 *
	 * @param xLeft The leftmost x coordinate
	 * @param yUpper The uppermost y coordinate
	 * @param xRight The rightmost x coordinate
	 * @param yLower The lowest y coordinate
	 */
	public Circle(double xLeft, double yUpper, double xRight, double yLower) {
		final double side = Math.min( Math.abs( xRight - xLeft ), Math.abs( yLower - yUpper ) );
		this.center.x = Math.min( xRight, xLeft ) + side / 2;
		this.center.y = Math.min( yUpper, yLower ) + side / 2;
		this.radius = side / 2;
	}

	/**
	 * Three point method of circle construction. All three points must be on
	 * the circumference of the circle.
	 *
	 * @param point1 The first point
	 * @param point2 The second point
	 * @param point3 The third point
	 */
	public Circle(Coordinate point1, Coordinate point2, Coordinate point3) {
		initThreePointCircle( point1, point2, point3 );
	}

	/**
	 * Three point method of circle construction. All three points must be on
	 * the circumference of the circle.
	 *
	 * @param x1 The x coordinate of the first point
	 * @param y1 The y coordinate of the first point
	 * @param x2 The x coordinate of the second point
	 * @param y2 The y coordinate of the second point
	 * @param x3 The x coordinate of the third point
	 * @param y3 The y coordinate of the third point
	 */
	public Circle(double x1, double y1, double x2, double y2, double x3, double y3) {
		this(
				new Coordinate( x1, y1 ), new Coordinate( x2, y2 ), new Coordinate( x3, y3 )
		);
	}

	/**
	 * Given 2 points defining an arc on the circle, interpolates the circle
	 * into a collection of points that provide connected chords that
	 * approximate the arc based on the tolerance value. The tolerance value
	 * specifies the maximum distance between a chord and the circle.
	 *
	 * @param x1 x coordinate of point 1
	 * @param y1 y coordinate of point 1
	 * @param x2 x coordinate of point 2
	 * @param y2 y coordinate of point 2
	 * @param x3 x coordinate of point 3
	 * @param y3 y coordinate of point 3
	 * @param tolerence maximum distance between the center of the chord and the outer
	 * edge of the circle
	 *
	 * @return an ordered list of Coordinates representing a series of chords
	 *         approximating the arc.
	 */
	public static Coordinate[] linearizeArc(double x1, double y1, double x2, double y2, double x3, double y3, double tolerence) {
		final Coordinate p1 = new Coordinate( x1, y1 );
		final Coordinate p2 = new Coordinate( x2, y2 );
		final Coordinate p3 = new Coordinate( x3, y3 );
		return new Circle( p1, p2, p3 ).linearizeArc( p1, p2, p3, tolerence );
	}

	/**
	 * Given 2 points defining an arc on the circle, interpolates the circle
	 * into a collection of points that provide connected chords that
	 * approximate the arc based on the tolerance value. This method uses a
	 * tolerence value of 1/100 of the length of the radius.
	 *
	 * @param x1 x coordinate of point 1
	 * @param y1 y coordinate of point 1
	 * @param x2 x coordinate of point 2
	 * @param y2 y coordinate of point 2
	 * @param x3 x coordinate of point 3
	 * @param y3 y coordinate of point 3
	 *
	 * @return an ordered list of Coordinates representing a series of chords
	 *         approximating the arc.
	 */
	public static Coordinate[] linearizeArc(double x1, double y1, double x2, double y2, double x3, double y3) {
		final Coordinate p1 = new Coordinate( x1, y1 );
		final Coordinate p2 = new Coordinate( x2, y2 );
		final Coordinate p3 = new Coordinate( x3, y3 );
		final Circle c = new Circle( p1, p2, p3 );
		final double tolerence = 0.01 * c.getRadius();
		return c.linearizeArc( p1, p2, p3, tolerence );
	}

	/**
	 * Given a circle defined by the 3 points, creates a linearized
	 * interpolation of the circle starting and ending on the first coordinate.
	 * This method uses a tolerence value of 1/100 of the length of the radius.
	 *
	 * @param x1 x coordinate of point 1
	 * @param y1 y coordinate of point 1
	 * @param x2 x coordinate of point 2
	 * @param y2 y coordinate of point 2
	 * @param x3 x coordinate of point 3
	 * @param y3 y coordinate of point 3
	 *
	 * @return an ordered list of Coordinates representing a series of chords
	 *         approximating the arc.
	 */
	public static Coordinate[] linearizeCircle(double x1, double y1, double x2, double y2, double x3, double y3) {
		final Coordinate p1 = new Coordinate( x1, y1 );
		final Coordinate p2 = new Coordinate( x2, y2 );
		final Coordinate p3 = new Coordinate( x3, y3 );
		final Circle c = new Circle( p1, p2, p3 );
		final double tolerence = 0.01 * c.getRadius();
		return c.linearizeArc( p1, p2, p1, tolerence );
	}

	/**
	 * Returns an angle between 0 and 2*PI. For example, 4*PI would get returned
	 * as 2*PI since they are equivalent.
	 *
	 * @param angle an angle in radians to normalize
	 *
	 * @return an angle between 0 and 2*PI
	 */
	public static double normalizeAngle(double angle) {
		final double maxRadians = 2 * Math.PI;
		if ( angle >= 0 && angle <= maxRadians ) {
			return angle;
		}
		if ( angle < 0 ) {
			return maxRadians - Math.abs( angle );
		}
		else {
			return angle % maxRadians;
		}
	}

	/**
	 * Returns the angle between the angles a1 and a2 in radians. Angle is
	 * calculated in the counterclockwise direction.
	 *
	 * @param a1 first angle
	 * @param a2 second angle
	 *
	 * @return the angle between a1 and a2 in the clockwise direction
	 */
	public static double subtractAngles(double a1, double a2) {
		if ( a1 < a2 ) {
			return a2 - a1;
		}
		else {
			return TWO_PI - Math.abs( a2 - a1 );
		}
	}

	/**
	 * Shifts the center of the circle by delta X and delta Y
	 *
	 * @param deltaX The shift along the X-coordinate axis
	 * @param deltaY The shift along the Y-coordinate axis
	 */
	public void shift(double deltaX, double deltaY) {
		this.center.x = this.center.x + deltaX;
		this.center.y = this.center.y + deltaY;
	}

	/**
	 * Moves the circle to a new center
	 *
	 * @param x The x coordinate of the new center
	 * @param y The y coordinate of the new center
	 */
	public void move(double x, double y) {
		this.center.x = x;
		this.center.y = y;
	}

	/**
	 * Defines the circle based on three points. All three points must be on on
	 * the circumference of the circle, and hence, the 3 points cannot be have
	 * any pair equal, and cannot form a line. Therefore, each point given is
	 * one radius measure from the circle's center.
	 *
	 * @param p1 A point on the desired circle
	 * @param p2 A point on the desired circle
	 * @param p3 A point on the desired circle
	 */
	private void initThreePointCircle(Coordinate p1, Coordinate p2, Coordinate p3) {
		double a13, b13, c13;
		double a23, b23, c23;
		double x, y, rad;

		// begin pre-calculations for linear system reduction
		a13 = 2 * ( p1.x - p3.x );
		b13 = 2 * ( p1.y - p3.y );
		c13 = ( p1.y * p1.y - p3.y * p3.y ) + ( p1.x * p1.x - p3.x * p3.x );
		a23 = 2 * ( p2.x - p3.x );
		b23 = 2 * ( p2.y - p3.y );
		c23 = ( p2.y * p2.y - p3.y * p3.y ) + ( p2.x * p2.x - p3.x * p3.x );
		// testsuite-suite to be certain we have three distinct points passed
		final double smallNumber = 0.01;
		if ( ( Math.abs( a13 ) < smallNumber && Math.abs( b13 ) < smallNumber )
				|| ( Math.abs( a13 ) < smallNumber && Math.abs( b13 ) < smallNumber ) ) {
			// // points too close so set to default circle
			x = 0;
			y = 0;
			rad = 0;
		}
		else {
			// everything is acceptable do the y calculation
			y = ( a13 * c23 - a23 * c13 ) / ( a13 * b23 - a23 * b13 );
			// x calculation
			// choose best formula for calculation
			if ( Math.abs( a13 ) > Math.abs( a23 ) ) {
				x = ( c13 - b13 * y ) / a13;
			}
			else {
				x = ( c23 - b23 * y ) / a23;
			}
			// radius calculation
			rad = Math.sqrt( ( x - p1.x ) * ( x - p1.x ) + ( y - p1.y ) * ( y - p1.y ) );
		}
		this.center.x = x;
		this.center.y = y;
		this.radius = rad;
	}

	public Coordinate getCenter() {
		return this.center;
	}

	public double getRadius() {
		return this.radius;
	}

	/**
	 * Given 2 points defining an arc on the circle, interpolates the circle
	 * into a collection of points that provide connected chords that
	 * approximate the arc based on the tolerance value. The tolerance value
	 * specifies the maximum distance between a chord and the circle.
	 *
	 * @param p1 begin coordinate of the arc
	 * @param p2 any other point on the arc
	 * @param p3 end coordinate of the arc
	 * @param tolerence maximum distance between the center of the chord and the outer
	 * edge of the circle
	 *
	 * @return an ordered list of Coordinates representing a series of chords
	 *         approximating the arc.
	 */
	public Coordinate[] linearizeArc(Coordinate p1, Coordinate p2, Coordinate p3, double tolerence) {
		final Arc arc = createArc( p1, p2, p3 );
		final List<Coordinate> result = linearizeInternal( null, arc, tolerence );
		return result.toArray( new Coordinate[result.size()] );
	}

	private List<Coordinate> linearizeInternal(List<Coordinate> coordinates, Arc arc, double tolerence) {
		if ( coordinates == null ) {
			coordinates = new ArrayList<Coordinate>();
		}
		final double arcHt = arc.getArcHeight();
		if ( Double.compare( arcHt, tolerence ) <= 0 ) {
			final int lastIndex = coordinates.size() - 1;
			final Coordinate lastCoord = lastIndex >= 0 ? coordinates.get( lastIndex ) : null;
			if ( lastCoord == null || !arc.getP1().equals2D( lastCoord ) ) {
				coordinates.add( arc.getP1() );
				coordinates.add( arc.getP2() );
			}
			else {
				coordinates.add( arc.getP2() );
			}

		}
		else {
			// otherwise, split
			final Arc[] splits = arc.split();
			linearizeInternal( coordinates, splits[0], tolerence );
			linearizeInternal( coordinates, splits[1], tolerence );
		}
		return coordinates;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		final Circle circle = (Circle) o;

		if ( Double.compare( circle.radius, this.radius ) != 0 ) {
			return false;
		}
		if ( this.center != null ? !this.center.equals2D( circle.center )
				: circle.center != null ) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = center.hashCode();
		temp = radius != +0.0d ? Double.doubleToLongBits( radius ) : 0L;
		result = 31 * result + (int) ( temp ^ ( temp >>> 32 ) );
		return result;
	}

	@Override
	public String toString() {
		return "Circle with Radius = " + this.radius
				+ " and a center at the coordinates (" + this.center.x + ", "
				+ this.center.y + ")";
	}

	/**
	 * Returns the angle of the point from the center and the horizontal line
	 * from the center.
	 *
	 * @param p a point in space
	 *
	 * @return The angle of the point from the center of the circle
	 */
	public double getAngle(Coordinate p) {
		final double dx = p.x - this.center.x;
		final double dy = p.y - this.center.y;
		double angle;

		if ( dx == 0.0 ) {
			if ( dy == 0.0 ) {
				angle = 0.0;
			}
			else if ( dy > 0.0 ) {
				angle = Math.PI / 2.0;
			}
			else {
				angle = ( Math.PI * 3.0 ) / 2.0;
			}
		}
		else if ( dy == 0.0 ) {
			if ( dx > 0.0 ) {
				angle = 0.0;
			}
			else {
				angle = Math.PI;
			}
		}
		else {
			if ( dx < 0.0 ) {
				angle = Math.atan( dy / dx ) + Math.PI;
			}
			else if ( dy < 0.0 ) {
				angle = Math.atan( dy / dx ) + ( 2 * Math.PI );
			}
			else {
				angle = Math.atan( dy / dx );
			}
		}
		return angle;
	}

	/**
	 * Returns the coordinate on the circle at the specified angle
	 *
	 * @param angle The angle
	 *
	 * @return Coordinate
	 */
	public Coordinate getPoint(final double angle) {
		double x = Math.cos( angle ) * this.radius;
		x = x + this.center.x;
		x = this.precisionModel.makePrecise( x );

		double y = Math.sin( angle ) * this.radius;
		y = y + this.center.y;
		y = this.precisionModel.makePrecise( y );
		return new Coordinate( x, y );
	}

	/**
	 * Returns the distance the point is from the center of the circle
	 *
	 * @param p A point in space
	 *
	 * @return The distance the point is from the center of the circle
	 */
	public double distanceFromCenter(Coordinate p) {
		return Math.abs( this.center.distance( p ) );
	}

	/**
	 * Creates an arc through the specified points
	 *
	 * @param p1 The first point
	 * @param p2 The second point
	 * @param p3 The third point
	 *
	 * @return The {@code Arc} through the three points
	 */
	public Arc createArc(Coordinate p1, Coordinate p2, Coordinate p3) {
		return new Arc( p1, p2, p3 );
	}

	/**
	 * An arc, or circle segment
	 */
	public class Arc {
		private Coordinate p1, p2;
		private double arcAngle;
		private double p1Angle;
		private double p2Angle;
		private boolean clockwise;

		private Arc(Coordinate p1, Coordinate midPt, Coordinate p2) {
			this.p1 = p1;
			this.p2 = p2;
			this.p1Angle = getAngle( p1 );
			// See if this arc covers the whole circle
			if ( p1.equals2D( p2 ) ) {
				this.p2Angle = TWO_PI + this.p1Angle;
				this.arcAngle = TWO_PI;
			}
			else {
				this.p2Angle = getAngle( p2 );
				final double midPtAngle = getAngle( midPt );

				// determine the direction
				final double ccDegrees = Circle.subtractAngles( this.p1Angle, midPtAngle ) + Circle.subtractAngles(
						midPtAngle,
						this.p2Angle
				);

				if ( ccDegrees < TWO_PI ) {
					this.clockwise = false;
					this.arcAngle = ccDegrees;
				}
				else {
					this.clockwise = true;
					this.arcAngle = TWO_PI - ccDegrees;
				}
			}
		}

		private Arc(Coordinate p1, Coordinate p2, boolean isClockwise) {
			this.p1 = p1;
			this.p2 = p2;
			this.clockwise = isClockwise;
			this.p1Angle = getAngle( p1 );
			if ( p1.equals2D( p2 ) ) {
				this.p2Angle = TWO_PI + this.p1Angle;
			}
			else {
				this.p2Angle = getAngle( p2 );
			}
			determineArcAngle();
		}

		private void determineArcAngle() {
			double diff;
			if ( this.p1.equals2D( this.p2 ) ) {
				diff = TWO_PI;
			}
			else if ( this.clockwise ) {
				diff = this.p1Angle - this.p2Angle;
			}
			else {
				diff = this.p2Angle - this.p1Angle;
			}
			this.arcAngle = Circle.normalizeAngle( diff );
		}

		/**
		 * given a an arc defined from p1 to p2 existing on this circle, returns
		 * the height of the arc. This height is defined as the distance from
		 * the center of a chord defined by (p1, p2) and the outer edge of the
		 * circle.
		 *
		 * @return the arc height
		 */
		public double getArcHeight() {
			final Coordinate chordCenterPt = this.getChordCenterPoint();
			final double dist = distanceFromCenter( chordCenterPt );
			if ( this.arcAngle > Math.PI ) {
				return Circle.this.radius + dist;
			}
			else {
				return Circle.this.radius - dist;
			}
		}

		/**
		 * Returns the center of this {@code Arc}
		 *
		 * @return the center of this {@code Arc}
		 */
		public Coordinate getChordCenterPoint() {
			final double centerX = this.p1.x + ( this.p2.x - this.p1.x ) / 2;
			final double centerY = this.p1.y + ( this.p2.y - this.p1.y ) / 2;
			return new Coordinate( centerX, centerY );
		}

		/**
		 * Splits this {@code Arc} at the mid point
		 *
		 * @return an array of two {@code Arc}s
		 */
		public Arc[] split() {
			final int directionFactor = isClockwise() ? -1 : 1;
			final double angleOffset = directionFactor * ( this.arcAngle / 2 );

			final double midAngle = this.p1Angle + angleOffset;
			final Coordinate newMidPoint = getPoint( midAngle );

			final Arc arc1 = new Arc( this.p1, newMidPoint, isClockwise() );
			final Arc arc2 = new Arc( newMidPoint, this.p2, isClockwise() );
			return new Arc[] { arc1, arc2 };
		}

		public Coordinate getP1() {
			return this.p1;
		}

		public Coordinate getP2() {
			return this.p2;
		}

		public double getArcAngle() {
			return this.arcAngle;
		}

		public double getArcAngleDegrees() {
			return Math.toDegrees( this.arcAngle );
		}

		public double getP1Angle() {
			return this.p1Angle;
		}

		public double getP2Angle() {
			return this.p2Angle;
		}

		public boolean isClockwise() {
			return this.clockwise;
		}

		@Override
		public String toString() {
			return "P1: " + this.p1 + " P2: " + this.p2 + " clockwise: " + this.clockwise;
		}
	}

}
