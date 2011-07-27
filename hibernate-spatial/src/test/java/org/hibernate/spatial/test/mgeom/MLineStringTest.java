/**
 * $Id: MLineStringTest.java 296 2011-03-05 11:50:41Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the 
 * hibernate ORM solution for geographic data. 
 *
 * Copyright © 2007 Geovise BVBA
 * Copyright © 2007 K.U. Leuven LRD, Spatial Applications Division, Belgium
 *
 * This work was partially supported by the European Commission, 
 * under the 6th Framework Programme, contract IST-2-004688-STP.
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
package org.hibernate.spatial.test.mgeom;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceComparator;
import com.vividsolutions.jts.geom.PrecisionModel;
import junit.framework.TestCase;

import org.hibernate.spatial.mgeom.DoubleComparator;
import org.hibernate.spatial.mgeom.MCoordinate;
import org.hibernate.spatial.mgeom.MCoordinateSequenceFactory;
import org.hibernate.spatial.mgeom.MGeometry;
import org.hibernate.spatial.mgeom.MGeometryException;
import org.hibernate.spatial.mgeom.MGeometryFactory;
import org.hibernate.spatial.mgeom.MLineString;

/**
 * @author Karel Maesen
 */
public class MLineStringTest extends TestCase {

	private PrecisionModel prec = new PrecisionModel( PrecisionModel.FIXED );

	private MGeometryFactory mgeomFactory = new MGeometryFactory(
			MCoordinateSequenceFactory.instance()
	);

	private MLineString controlledLine;

	private MLineString arbitraryLine;

	private MLineString nullLine;

	private MLineString ringLine;

	public static void main(String[] args) {
		junit.textui.TestRunner.run( MLineStringTest.class );
	}

	/*
		  * @see TestCase#setUp()
		  */

	protected void setUp() throws Exception {
		super.setUp();
		MCoordinate mc0 = MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 );
		MCoordinate mc1 = MCoordinate.create2dWithMeasure( 1.0, 0.0, 0.0 );
		MCoordinate mc2 = MCoordinate.create2dWithMeasure( 1.0, 1.0, 0.0 );
		MCoordinate mc3 = MCoordinate.create2dWithMeasure( 2.0, 1.0, 0.0 );

		MCoordinate[] mcoar = new MCoordinate[] { mc0, mc1, mc2, mc3 };
		controlledLine = mgeomFactory.createMLineString( mcoar );

		mc0 = MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 );
		mc1 = MCoordinate.create2dWithMeasure( 1.0, 0.0, 0.0 );
		mc2 = MCoordinate.create2dWithMeasure( 1.0, 1.0, 0.0 );
		mc3 = MCoordinate.create2dWithMeasure( 0.0, 1.0, 0.0 );
		MCoordinate mc4 = MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 );

		mcoar = new MCoordinate[] { mc0, mc1, mc2, mc3, mc4 };
		ringLine = mgeomFactory.createMLineString( mcoar );

		int l = (int) Math.round( Math.random() * 250.0 );
		l = Math.max( 2, l );
		System.out.println( "Size of arbitraryline ==" + l );
		mcoar = new MCoordinate[l];
		for ( int i = 0; i < mcoar.length; i++ ) {
			double x = Math.random() * 100000.0;
			double y = Math.random() * 100000.0;
			double z = Double.NaN; // JTS doesn't support operations on the
			// z-coordinate value
			double m = Math.random() * 100000.0;
			mcoar[i] = new MCoordinate( x, y, z, m );
		}
		arbitraryLine = mgeomFactory.createMLineString( mcoar );

		mcoar = new MCoordinate[0];
		nullLine = mgeomFactory.createMLineString( mcoar );
	}

	/*
		  * @see TestCase#tearDown()
		  */

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Constructor for MLineStringTest.
	 *
	 * @param name
	 */
	public MLineStringTest(String name) {
		super( name );
	}

	//
	// public void testMLineString() {

	// }

	/*
		  * Class under testsuite-suite for Object clone()
		  */

	public void testClone() {
		MLineString mltest = (MLineString) arbitraryLine.clone();

		Coordinate[] testco = mltest.getCoordinates();
		Coordinate[] arco = arbitraryLine.getCoordinates();
		assertEquals( testco.length, arco.length );
		for ( int i = 0; i < arco.length; i++ ) {
			// clones must have equal, but not identical coordinates
			assertEquals( arco[i], testco[i] );
			assertNotSame( arco[i], testco[i] );
		}

		mltest = (MLineString) nullLine.clone();
		assertEquals( mltest.isEmpty(), nullLine.isEmpty() );
		assertTrue( mltest.isEmpty() );

	}

	public void testGetClosestPoint() {

		try {
			if ( !arbitraryLine.isMonotone( false ) ) {
				Coordinate mc = arbitraryLine.getClosestPoint(
						new Coordinate(
								1.0, 2.0
						), 0.1
				);
				assertTrue( false ); // should never evaluate this
			}
		}
		catch ( Exception e ) {
			assertTrue( ( (MGeometryException) e ).getType() == MGeometryException.OPERATION_REQUIRES_MONOTONE );
		}

		try {
			// check reaction on null string
			MCoordinate mc = nullLine.getClosestPoint(
					new Coordinate( 0.0, 1.0 ),
					1.0
			);
			assertNull( mc );

			// must return the very same coordinate if the coordinate is a
			// coordinate of the line
			arbitraryLine.measureOnLength( false );
			int selp = (int) ( arbitraryLine.getNumPoints() / 2 );
			MCoordinate mcexp = (MCoordinate) arbitraryLine
					.getCoordinateN( selp );
			MCoordinate mctest = arbitraryLine.getClosestPoint( mcexp, 1 );
			assertEquals( mcexp, mctest );

			// must not return a point that is beyond the tolerance
			mctest = controlledLine.getClosestPoint(
					new Coordinate( 20.0, 20, 0 ), 1.0
			);
			assertNull( mctest );

			// check for cases of circular MGeometry: lowest measure should be
			// return.
			ringLine.measureOnLength( false );
			assertTrue( ringLine.isRing() );
			assertTrue( ringLine.isMonotone( false ) );
			assertTrue( ringLine.getMeasureDirection() == MGeometry.INCREASING );
			MCoordinate expCo = MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 );
			MCoordinate testCo = ringLine.getClosestPoint( expCo, 0.1 );
			assertTrue( DoubleComparator.equals( testCo.m, expCo.m ) );
			ringLine.reverseMeasures();
			testCo = ringLine.getClosestPoint( expCo, 0.1 );
			assertTrue( DoubleComparator.equals( testCo.m, expCo.m ) );
			ringLine.measureOnLength( false );
			int n = ringLine.getNumPoints() - 1;
			ringLine.setMeasureAtIndex( n, 100.0 );
			ringLine.setMeasureAtIndex( 0, 0.0 );
			testCo = ringLine.getClosestPoint( expCo, 0.001 );
			assertTrue( DoubleComparator.equals( testCo.m, 0.0 ) );

			// get two neighbouring points along the arbitraryline
			arbitraryLine.measureOnLength( false );
			int elem1Indx = (int) ( Math.random() * ( arbitraryLine
					.getNumPoints() - 1 ) );
			int elem2Indx = 0;
			if ( elem1Indx == arbitraryLine.getNumPoints() - 1 ) {
				elem2Indx = elem1Indx - 1;
			}
			else {
				elem2Indx = elem1Indx + 1;
			}
			// testsuite-suite whether a coordinate between these two returns exactly
			MCoordinate mco1 = (MCoordinate) arbitraryLine
					.getCoordinateN( elem1Indx );
			MCoordinate mco2 = (MCoordinate) arbitraryLine
					.getCoordinateN( elem2Indx );
			double d = mco1.distance( mco2 );
			double offset = Math.random();
			mcexp = MCoordinate.create2dWithMeasure(
					mco1.x + offset
							* ( mco2.x - mco1.x ), mco1.y + offset * ( mco2.y - mco1.y ),
					0.0
			);
			mctest = arbitraryLine.getClosestPoint( mcexp, d );
			mcexp.m = mco1.m + offset * ( mco2.m - mco1.m );
			assertEquals( mcexp.x, mctest.x, 0.001 );
			assertEquals( mcexp.y, mctest.y, 0.001 );
			assertEquals( mcexp.z, mctest.z, 0.001 );
			double delta = Math.random();

			MCoordinate mcin = MCoordinate.create2dWithMeasure(
					mco1.x + offset
							* ( mco2.x - mco1.x ) + delta, mco1.y + offset
					* ( mco2.y - mco1.y ) + delta, 0.0
			);

			// returned point is on the line
			mctest = arbitraryLine.getClosestPoint( mcin, d );
			assertEquals( mcin.x, mctest.x, delta * Math.sqrt( 2 ) );
			assertEquals( mcin.y, mctest.y, delta * Math.sqrt( 2 ) );
		}
		catch ( Exception e ) {
			e.printStackTrace();
			assertTrue( false ); // should never reach this point
		}

	}

	public void testGetCoordinateAtM() {
		// what if null string
		try {
			Coordinate mc = nullLine.getCoordinateAtM( 2 );
			assertNull( mc );

			// get two neighbouring points along the arbitraryline
			arbitraryLine.measureOnLength( false );
			int elem1Indx = (int) ( Math.random() * ( arbitraryLine
					.getNumPoints() - 1 ) );
			int elem2Indx = 0;
			if ( elem1Indx == arbitraryLine.getNumPoints() - 1 ) {
				elem2Indx = elem1Indx - 1;
			}
			else {
				elem2Indx = elem1Indx + 1;
			}

			// if m is value of a coordinate, the returned coordinate should
			// equal that coordinate

			MCoordinate mco1 = (MCoordinate) arbitraryLine
					.getCoordinateN( elem1Indx );
			MCoordinate mcotest = (MCoordinate) arbitraryLine
					.getCoordinateAtM( mco1.m );
			assertNotSame( mco1, mcotest );
			assertEquals( mco1.x, mcotest.x, Math.ulp( 100 * mco1.x ) );
			assertEquals( mco1.y, mcotest.y, Math.ulp( 100 * mco1.y ) );
			assertEquals( mco1.m, mcotest.m, Math.ulp( 100 * mco1.m ) );

			MCoordinate mco2 = (MCoordinate) arbitraryLine
					.getCoordinateN( elem2Indx );
			double offset = Math.random();
			double newM = mco1.m + offset * ( mco2.m - mco1.m );
			MCoordinate mcexp = new MCoordinate(
					mco1.x + offset
							* ( mco2.x - mco1.x ), mco1.y + offset * ( mco2.y - mco1.y ),
					Double.NaN, mco1.m + offset * ( mco2.m - mco1.m )
			);
			MCoordinate mctest = (MCoordinate) arbitraryLine
					.getCoordinateAtM( newM );
			assertEquals( mcexp.x, mctest.x, 0.0001 );
			assertEquals( mcexp.y, mctest.y, 0.0001 );
			assertEquals( mcexp.m, mctest.m, 0.0001 );

		}
		catch ( Exception e ) {
			System.err.println( e );
		}
	}

	/*
		  * Class under testsuite-suite for String getGeometryType()
		  */

	public void testGetGeometryType() {
		assertEquals( "MLineString", arbitraryLine.getGeometryType() );
	}

	public void testGetMatCoordinate() {
		try {
			// what in case of the null string
			assertTrue(
					Double.isNaN(
							nullLine.getMatCoordinate(
									new Coordinate(
											1.0, 1.0
									), 1.0
							)
					)
			);

			// get two neighbouring points along the arbitraryline
			arbitraryLine.measureOnLength( false );
			int elem1Indx = (int) ( Math.random() * ( arbitraryLine
					.getNumPoints() - 1 ) );
			int elem2Indx = 0;
			if ( elem1Indx == arbitraryLine.getNumPoints() - 1 ) {
				elem2Indx = elem1Indx - 1;
			}
			else {
				elem2Indx = elem1Indx + 1;
			}

			// if a coordinate of the geometry is passed, it should return
			// exactly that m-value
			MCoordinate mco1 = (MCoordinate) arbitraryLine
					.getCoordinateN( elem1Indx );
			double m = arbitraryLine.getMatCoordinate( mco1, 0.00001 );
			assertEquals(
					mco1.m, m, DoubleComparator
					.defaultNumericalPrecision()
			);

			// check for a coordinate between mco1 and mco2 (neighbouring
			// coordinates)
			MCoordinate mco2 = (MCoordinate) arbitraryLine
					.getCoordinateN( elem2Indx );
			double offset = Math.random();
			double expectedM = mco1.m + offset * ( mco2.m - mco1.m );
			Coordinate mctest = new Coordinate(
					mco1.x + offset
							* ( mco2.x - mco1.x ), mco1.y + offset * ( mco2.y - mco1.y )
			);

			double testM = arbitraryLine.getMatCoordinate( mctest, offset );
			assertEquals(
					expectedM, testM, DoubleComparator
					.defaultNumericalPrecision()
			);
		}
		catch ( Exception e ) {
			e.printStackTrace();
			assertTrue( false );// should never reach here
		}
	}

	public void testGetMatN() {
		// Implement getMatN().
	}

	public void testGetMaxM() {
		// Implement getMaxM().
	}

	public void testGetCoordinatesBetween() {

		try {
			// what if the null value is passed
			CoordinateSequence[] cs = nullLine.getCoordinatesBetween( 0.0, 5.0 );
			assertTrue( "cs.length = " + cs.length + ". Should be 1", cs.length == 1 );
			assertEquals( cs[0].size(), 0 );

			arbitraryLine.measureOnLength( false );
			// what if from/to is outside of the range of values
			double maxM = arbitraryLine.getMaxM();
			cs = arbitraryLine.getCoordinatesBetween( maxM + 1.0, maxM + 10.0 );

			// check for several ascending M-values
			int minIdx = (int) ( Math.random() * ( arbitraryLine.getNumPoints() - 1 ) );
			int maxIdx = Math.min(
					( arbitraryLine.getNumPoints() - 1 ),
					minIdx + 10
			);
			double minM = ( (MCoordinate) arbitraryLine.getCoordinateN( minIdx ) ).m;
			maxM = ( (MCoordinate) arbitraryLine.getCoordinateN( maxIdx ) ).m;
			cs = arbitraryLine.getCoordinatesBetween( minM, maxM );
			assertNotNull( cs );
			assertTrue( cs.length > 0 );
			Coordinate[] coar = cs[0].toCoordinateArray();
			int j = 0;
			for ( int i = minIdx; i <= maxIdx; i++ ) {
				assertEquals(
						(MCoordinate) arbitraryLine.getCoordinateN( i ),
						coar[j]
				);
				j++;
			}

			minM = Math.max( 0.0, minM - Math.random() * 10 );
			cs = arbitraryLine.getCoordinatesBetween( minM, maxM );
			coar = cs[0].toCoordinateArray();
			MCoordinate mctest = (MCoordinate) coar[0];
			MCoordinate mcexp = (MCoordinate) arbitraryLine
					.getCoordinateAtM( minM );
			assertEquals( mcexp, mctest );
			assertEquals(
					mctest.m, minM, DoubleComparator
					.defaultNumericalPrecision()
			);

			maxM = Math.min(
					arbitraryLine.getLength(), maxM + Math.random()
					* 10
			);
			cs = arbitraryLine.getCoordinatesBetween( minM, maxM );
			coar = cs[0].toCoordinateArray();
			mctest = (MCoordinate) coar[coar.length - 1];
			mcexp = (MCoordinate) arbitraryLine.getCoordinateAtM( maxM );
			assertEquals( mcexp.x, mctest.x, Math.ulp( mcexp.x ) * 100 );
			assertEquals( mcexp.y, mctest.y, Math.ulp( mcexp.y ) * 100 );
			assertEquals(
					mctest.m, maxM, DoubleComparator
					.defaultNumericalPrecision()
			);

		}
		catch ( Exception e ) {
			e.printStackTrace();
			assertTrue( false );// should never reach here

		}
	}

	public void testGetMeasureDirection() {
		assertTrue( nullLine.isMonotone( false ) );

		assertTrue(
				arbitraryLine.isMonotone( false )
						|| ( !arbitraryLine.isMonotone( false ) && arbitraryLine
						.getMeasureDirection() == MGeometry.NON_MONOTONE )
		);
		arbitraryLine.measureOnLength( false );
		assertEquals( MGeometry.INCREASING, arbitraryLine.getMeasureDirection() );

		arbitraryLine.reverseMeasures();
		assertEquals( MGeometry.DECREASING, arbitraryLine.getMeasureDirection() );

		for ( int i = 0; i < arbitraryLine.getNumPoints(); i++ ) {
			arbitraryLine.setMeasureAtIndex( i, 0.0 );
		}
		assertEquals( MGeometry.CONSTANT, arbitraryLine.getMeasureDirection() );
	}

	public void testGetMeasures() {

	}

	public void testGetMinM() {

	}

	public void testInterpolate() {
		MCoordinate mc0NaN = MCoordinate.create2d( 0.0, 0.0 );
		MCoordinate mc0 = MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 );
		MCoordinate mc2NaN = MCoordinate.create2d( 2.0, 0.0 );
		MCoordinate mc5NaN = MCoordinate.create2d( 5.0, 0.0 );
		MCoordinate mc10NaN = MCoordinate.create2d( 10.0, 0.0 );
		MCoordinate mc10 = MCoordinate.create2dWithMeasure( 10.0, 0.0, 10.0 );

		// Internal coordinate measures are not defined, outer measures are
		// 0-10, total 2d length is 10
		MLineString line = mgeomFactory.createMLineString(
				new MCoordinate[] {
						mc0, mc2NaN, mc5NaN, mc10
				}
		);
		MLineString lineBeginNaN = mgeomFactory
				.createMLineString(
						new MCoordinate[] {
								mc0NaN, mc2NaN, mc5NaN,
								mc10
						}
				);
		MLineString lineEndNaN = mgeomFactory
				.createMLineString(
						new MCoordinate[] {
								mc0, mc2NaN, mc5NaN,
								mc10NaN
						}
				);

		assertTrue( DoubleComparator.equals( line.getLength(), 10 ) );
		assertTrue( DoubleComparator.equals( lineBeginNaN.getLength(), 10 ) );
		assertTrue( DoubleComparator.equals( lineEndNaN.getLength(), 10 ) );

		line.interpolate( mc0.m, mc10.m );
		lineBeginNaN.interpolate( mc0.m, mc10.m );
		lineEndNaN.interpolate( mc0.m, mc10.m );

		assertTrue( line.isMonotone( false ) );
		assertTrue( line.isMonotone( true ) );
		assertTrue( lineBeginNaN.isMonotone( false ) );
		assertTrue( lineBeginNaN.isMonotone( true ) );
		assertTrue( lineEndNaN.isMonotone( false ) );
		assertTrue( lineEndNaN.isMonotone( true ) );

		double[] expectedM = new double[] { mc0.m, 2.0, 5.0, mc10.m };
		for ( int i = 0; i < expectedM.length; i++ ) {
			double actualMLine = line.getCoordinateSequence().getOrdinate(
					i,
					CoordinateSequence.M
			);
			double actualBeginNaN = lineBeginNaN.getCoordinateSequence()
					.getOrdinate( i, CoordinateSequence.M );
			double actualEndNaN = lineEndNaN.getCoordinateSequence()
					.getOrdinate( i, CoordinateSequence.M );
			assertTrue( DoubleComparator.equals( expectedM[i], actualMLine ) );
			assertTrue( DoubleComparator.equals( expectedM[i], actualBeginNaN ) );
			assertTrue( DoubleComparator.equals( expectedM[i], actualEndNaN ) );
		}

		// Test Continuous case by interpolating with begin and end measures
		// equal
		double continuousMeasure = 0.0D;
		line.interpolate( continuousMeasure, continuousMeasure );
		double[] measures = line.getMeasures();
		for ( int i = 0; i < measures.length; i++ ) {
			assertTrue( DoubleComparator.equals( measures[i], continuousMeasure ) );
		}
	}

	public void testIsMonotone() {
		MCoordinate mc0NaN = MCoordinate.create2d( 1.0, 0.0 );
		MCoordinate mc0 = MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 );
		MCoordinate mc1NaN = MCoordinate.create2d( 1.0, 0.0 );
		MCoordinate mc1 = MCoordinate.create2dWithMeasure( 1.0, 0.0, 1.0 );
		MCoordinate mc2NaN = MCoordinate.create2d( 2.0, 0.0 );
		MCoordinate mc2 = MCoordinate.create2dWithMeasure( 2.0, 0.0, 2.0 );
		MCoordinate mc3NaN = MCoordinate.create2d( 3.0, 0.0 );
		MCoordinate mc3 = MCoordinate.create2dWithMeasure( 3.0, 0.0, 3.0 );

		MLineString emptyLine = mgeomFactory
				.createMLineString( new MCoordinate[] { } );
		MLineString orderedLine = mgeomFactory
				.createMLineString( new MCoordinate[] { mc0, mc1, mc2, mc3 } );
		MLineString unorderedLine = mgeomFactory
				.createMLineString( new MCoordinate[] { mc0, mc2, mc1, mc3 } );
		MLineString constantLine = mgeomFactory
				.createMLineString( new MCoordinate[] { mc2, mc2, mc2, mc2 } );
		MLineString reverseOrderedLine = mgeomFactory
				.createMLineString( new MCoordinate[] { mc3, mc2, mc1, mc0 } );
		MLineString reverseUnOrderedLine = mgeomFactory
				.createMLineString( new MCoordinate[] { mc3, mc1, mc2, mc0 } );
		MLineString dupCoordLine = mgeomFactory
				.createMLineString( new MCoordinate[] { mc0, mc1, mc1, mc2 } );
		MLineString reverseDupCoordLine = mgeomFactory
				.createMLineString( new MCoordinate[] { mc2, mc1, mc1, mc0 } );

		assertTrue( emptyLine.isMonotone( false ) );
		assertTrue( emptyLine.isMonotone( true ) );

		assertTrue( orderedLine.isMonotone( false ) );
		assertTrue( orderedLine.isMonotone( true ) );
		// testsuite-suite reversing the ordered line
		orderedLine.reverseMeasures();
		assertTrue( orderedLine.isMonotone( false ) );
		assertTrue( orderedLine.isMonotone( true ) );
		// testsuite-suite shifting
		orderedLine.shiftMeasure( 1.0 );
		assertTrue( orderedLine.isMonotone( false ) );
		assertTrue( orderedLine.isMonotone( true ) );
		orderedLine.shiftMeasure( -1.0 );
		assertTrue( orderedLine.isMonotone( false ) );
		assertTrue( orderedLine.isMonotone( true ) );

		assertFalse( unorderedLine.isMonotone( false ) );
		assertFalse( unorderedLine.isMonotone( true ) );

		assertTrue( constantLine.isMonotone( false ) );
		assertFalse( constantLine.isMonotone( true ) );
		// testsuite-suite shifting
		constantLine.shiftMeasure( 1.0 );
		assertTrue( constantLine.isMonotone( false ) );
		assertFalse( constantLine.isMonotone( true ) );
		constantLine.shiftMeasure( -1.0 );
		assertTrue( constantLine.isMonotone( false ) );
		assertFalse( constantLine.isMonotone( true ) );

		assertTrue( reverseOrderedLine.isMonotone( false ) );
		assertTrue( reverseOrderedLine.isMonotone( true ) );
		// testsuite-suite reversing the line
		reverseOrderedLine.reverseMeasures();
		assertTrue( reverseOrderedLine.isMonotone( false ) );
		assertTrue( reverseOrderedLine.isMonotone( true ) );
		// testsuite-suite shifting
		reverseOrderedLine.shiftMeasure( 1.0 );
		assertTrue( reverseOrderedLine.isMonotone( false ) );
		assertTrue( reverseOrderedLine.isMonotone( true ) );
		reverseOrderedLine.shiftMeasure( -1.0 );
		assertTrue( reverseOrderedLine.isMonotone( false ) );
		assertTrue( reverseOrderedLine.isMonotone( true ) );

		assertFalse( reverseUnOrderedLine.isMonotone( false ) );
		assertFalse( reverseUnOrderedLine.isMonotone( true ) );

		assertTrue( dupCoordLine.isMonotone( false ) );
		assertFalse( dupCoordLine.isMonotone( true ) );
		// testsuite-suite shifting
		dupCoordLine.shiftMeasure( 1.0 );
		assertTrue( dupCoordLine.isMonotone( false ) );
		assertFalse( dupCoordLine.isMonotone( true ) );
		dupCoordLine.shiftMeasure( -1.0 );
		assertTrue( dupCoordLine.isMonotone( false ) );
		assertFalse( dupCoordLine.isMonotone( true ) );

		assertTrue( reverseDupCoordLine.isMonotone( false ) );
		assertFalse( reverseDupCoordLine.isMonotone( true ) );
		// testsuite-suite shifting
		reverseDupCoordLine.shiftMeasure( 1.0 );
		assertTrue( reverseDupCoordLine.isMonotone( false ) );
		assertFalse( reverseDupCoordLine.isMonotone( true ) );
		reverseDupCoordLine.shiftMeasure( -1.0 );
		assertTrue( reverseDupCoordLine.isMonotone( false ) );
		assertFalse( reverseDupCoordLine.isMonotone( true ) );

		assertEquals( orderedLine.getMeasureDirection(), MGeometry.INCREASING );
		assertEquals(
				unorderedLine.getMeasureDirection(),
				MGeometry.NON_MONOTONE
		);
		assertEquals(
				reverseOrderedLine.getMeasureDirection(),
				MGeometry.DECREASING
		);
		assertEquals( dupCoordLine.getMeasureDirection(), MGeometry.INCREASING );
		assertEquals(
				reverseDupCoordLine.getMeasureDirection(),
				MGeometry.DECREASING
		);

		// Test scenario where there are NaN middle measures
		MLineString internalNaNLine = mgeomFactory
				.createMLineString( new MCoordinate[] { mc0, mc1NaN, mc2NaN, mc3 } );
		MLineString beginNaNLine = mgeomFactory
				.createMLineString( new MCoordinate[] { mc0NaN, mc2, mc3 } );
		MLineString endNaNLine = mgeomFactory
				.createMLineString( new MCoordinate[] { mc0, mc2, mc3NaN } );
		MLineString beginEndNaNLine = mgeomFactory
				.createMLineString( new MCoordinate[] { mc0NaN, mc2, mc3NaN } );

		assertFalse( internalNaNLine.isMonotone( false ) );
		assertFalse( internalNaNLine.isMonotone( true ) );
		internalNaNLine.measureOnLength( false );
		assertTrue( internalNaNLine.isMonotone( false ) );
		assertTrue( internalNaNLine.isMonotone( true ) );

		assertFalse( beginNaNLine.isMonotone( false ) );
		assertFalse( beginNaNLine.isMonotone( true ) );
		beginNaNLine.measureOnLength( false );
		assertTrue( beginNaNLine.isMonotone( false ) );
		assertTrue( beginNaNLine.isMonotone( true ) );

		assertFalse( endNaNLine.isMonotone( false ) );
		assertFalse( endNaNLine.isMonotone( true ) );
		endNaNLine.measureOnLength( false );
		assertTrue( endNaNLine.isMonotone( false ) );
		assertTrue( endNaNLine.isMonotone( true ) );

		assertFalse( beginEndNaNLine.isMonotone( false ) );
		assertFalse( beginEndNaNLine.isMonotone( true ) );
		beginEndNaNLine.measureOnLength( false );
		assertTrue( beginEndNaNLine.isMonotone( false ) );
		assertTrue( beginEndNaNLine.isMonotone( true ) );
	}

	public void testGetCoordinatesBetweenNonStrict() {
		try {
			CoordinateSequenceComparator coordCompare = new CoordinateSequenceComparator();
			MCoordinate mc0 = MCoordinate.create2dWithMeasure( 0.0, 0.0, 0 );
			MCoordinate mc1 = MCoordinate.create2dWithMeasure( 0.0, 1.0, 1 );
			MCoordinate mc2_1 = MCoordinate.create2dWithMeasure( 0.0, 2.0, 1 );
			MCoordinate mc2 = MCoordinate.create2dWithMeasure( 0.0, 2.0, 2 );
			MCoordinate mc3 = MCoordinate.create2dWithMeasure( 0.0, 3.0, 3 );
			MCoordinate mc4 = MCoordinate.create2dWithMeasure( 0.0, 4.0, 4 );

			// Test non-strict sequence where all coordinate x,y positions are
			// unique, but contains a
			// duplicate measure. The measure sequence in this testsuite-suite s
			// [0,1,1,3,4]
			MLineString nonStrictPointLine = mgeomFactory
					.createMLineString(
							new MCoordinate[] {
									mc0, mc1, mc2_1,
									mc3, mc4
							}
					);
			CoordinateSequence[] nonStrictSeq = nonStrictPointLine
					.getCoordinatesBetween( mc0.m, mc2_1.m );
			assertNotNull( nonStrictSeq );

			nonStrictSeq = nonStrictPointLine.getCoordinatesBetween(
					mc0.m,
					mc4.m
			);
			assertNotNull( nonStrictSeq );

			nonStrictSeq = nonStrictPointLine.getCoordinatesBetween(
					mc1.m,
					mc4.m
			);
			assertNotNull( nonStrictSeq );

			nonStrictSeq = nonStrictPointLine
					.getCoordinatesBetween( 1.1D, mc4.m );
			assertNotNull( nonStrictSeq );

		}
		catch ( MGeometryException e ) {
			e.printStackTrace();
		}
	}

	public void testmeasureOnLength() {
		arbitraryLine.measureOnLength( false );
		double maxM = arbitraryLine.getMaxM();
		double minM = arbitraryLine.getMinM();
		assertEquals(
				maxM, arbitraryLine.getLength(), DoubleComparator
				.defaultNumericalPrecision()
		);
		assertEquals( minM, 0.0d, DoubleComparator.defaultNumericalPrecision() );
		MCoordinate mco = (MCoordinate) arbitraryLine
				.getCoordinateN( arbitraryLine.getNumPoints() - 1 );
		assertEquals( mco.m, maxM, DoubleComparator.defaultNumericalPrecision() );
		mco = (MCoordinate) arbitraryLine.getCoordinateN( 0 );
		assertEquals( mco.m, minM, DoubleComparator.defaultNumericalPrecision() );
	}

	public void testReverseMeasures() {

		nullLine.reverseMeasures();

		arbitraryLine.measureOnLength( false );
		arbitraryLine.reverseMeasures();
		assertTrue( arbitraryLine.getMeasureDirection() == MGeometry.DECREASING );
		double mlast = arbitraryLine.getMatN( arbitraryLine.getNumPoints() - 1 );
		arbitraryLine.reverseMeasures();
		assertTrue( arbitraryLine.getMeasureDirection() == MGeometry.INCREASING );
		double mfirst = arbitraryLine.getMatN( 0 );
		assertEquals(
				mlast, mfirst, DoubleComparator
				.defaultNumericalPrecision()
		);

	}

	public void testSetMatN() {
		// TODO Implement setMeasureAtIndex().
	}

	public void testShiftMBy() {
		// TODO Implement shiftMeasure().
	}

	/*
		  * Class under testsuite-suite for String toString()
		  */

	public void testToString() {
		// TODO Implement toString().
	}

	public void testUnionM() {
		// TODO Implement unionM().
	}

	public void testVerifyMonotone() {
		// TODO Implement verifyMonotone().
	}

}
