/*
 * $Id: TestMLineStringGetCoordinatesBetween.java 225 2010-06-25 21:59:05Z maesenka $
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
import com.vividsolutions.jts.geom.PrecisionModel;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class TestMLineStringGetCoordinatesBetween {
	MLineString incrLine;
	MLineString decLine;
	MLineString emptyLine;
	MLineString nonMonotoneLine;
	MLineString partiallyConstantIncreasing;
	MLineString partiallyConstantDecreasing;


	private PrecisionModel prec = new PrecisionModel( PrecisionModel.FIXED );

	private MGeometryFactory mgeomFactory = new MGeometryFactory(
			MCoordinateSequenceFactory.instance()
	);

	@Before
	public void setUp() {

		MCoordinate[] coordinates = new MCoordinate[] {
				MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ),
				MCoordinate.create2dWithMeasure( 1.0, 0.0, 1.0 ),
				MCoordinate.create2dWithMeasure( 2.0, 0.0, 2.0 ),
				MCoordinate.create2dWithMeasure( 3.0, 0.0, 3.0 ),
				MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.0 )
		};
		incrLine = mgeomFactory.createMLineString( coordinates );

		coordinates = new MCoordinate[] {
				MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.0 ),
				MCoordinate.create2dWithMeasure( 3.0, 0.0, 3.0 ),
				MCoordinate.create2dWithMeasure( 2.0, 0.0, 2.0 ),
				MCoordinate.create2dWithMeasure( 1.0, 0.0, 1.0 ),
				MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 )
		};
		decLine = mgeomFactory.createMLineString( coordinates );

		coordinates = new MCoordinate[] {
				MCoordinate.create2dWithMeasure( 0.0, 0.0, 1.0 ),
				MCoordinate.create2dWithMeasure( 1.0, 0.0, 3.0 ),
				MCoordinate.create2dWithMeasure( 2.0, 0.0, 2.0 ),
				MCoordinate.create2dWithMeasure( 3.0, 0.0, 5.0 ),
				MCoordinate.create2dWithMeasure( 4.0, 0.0, 1.5 )
		};
		nonMonotoneLine = mgeomFactory.createMLineString( coordinates );

		coordinates = new MCoordinate[] {
				MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ),
				MCoordinate.create2dWithMeasure( 1.0, 0.0, 1.0 ),
				MCoordinate.create2dWithMeasure( 2.0, 0.0, 2.0 ),
				MCoordinate.create2dWithMeasure( 3.0, 0.0, 2.0 ),
				MCoordinate.create2dWithMeasure( 4.0, 0.0, 3.0 ),
				MCoordinate.create2dWithMeasure( 5.0, 0.0, 4.0 )
		};
		partiallyConstantIncreasing = mgeomFactory.createMLineString( coordinates );

		coordinates = new MCoordinate[] {
				MCoordinate.create2dWithMeasure( 5.0, 0.0, 4.0 ),
				MCoordinate.create2dWithMeasure( 4.0, 0.0, 3.0 ),
				MCoordinate.create2dWithMeasure( 3.0, 0.0, 2.0 ),
				MCoordinate.create2dWithMeasure( 2.0, 0.0, 2.0 ),
				MCoordinate.create2dWithMeasure( 1.0, 0.0, 1.0 ),
				MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 )
		};
		partiallyConstantDecreasing = mgeomFactory.createMLineString( coordinates );


	}

	@Test
	public void test_measure_inside_monotone_increasing() throws MGeometryException {
		CoordinateSequence[] result;
		result = incrLine.getCoordinatesBetween( 0.5, 3.5 );
		assertEquals( 1, result.length );
		Coordinate[] crds = result[0].toCoordinateArray();
		assertEquals( 5, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 3.5, 0.0, 3.5 ), crds[crds.length - 1] );

		result = incrLine.getCoordinatesBetween( 1.0, 3.0 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 3, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 1.0, 0.0, 1.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 2.0, 0.0, 2.0 ), crds[1] );
		assertEquals( MCoordinate.create2dWithMeasure( 3.0, 0.0, 3.0 ), crds[2] );

		result = incrLine.getCoordinatesBetween( 0.0, 4.0 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 5, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 1.0, 0.0, 1.0 ), crds[1] );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.0 ), crds[4] );

		result = incrLine.getCoordinatesBetween( 0.5, 1.5 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 3, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 1.5, 0.0, 1.5 ), crds[crds.length - 1] );

		result = incrLine.getCoordinatesBetween( 3.5, 4.0 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 3.5, 0.0, 3.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.5 ), crds[crds.length - 1] );

		result = incrLine.getCoordinatesBetween( 3.5, 3.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 3.5, 0.0, 3.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 3.7, 0.0, 3.7 ), crds[1] );

		result = incrLine.getCoordinatesBetween( 0.5, 0.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.7, 0.0, 0.7 ), crds[1] );

		result = incrLine.getCoordinatesBetween( -0.5, 0.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.7, 0.0, 0.7 ), crds[1] );

		result = incrLine.getCoordinatesBetween( 3.5, 4.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 3.5, 0.0, 3.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.0 ), crds[1] );
	}


	@Test
	public void test_measure_inside_partially_constant_increasing() throws MGeometryException {
		CoordinateSequence[] result;
		result = partiallyConstantIncreasing.getCoordinatesBetween( 0.5, 2.5 );
		assertEquals( 1, result.length );
		Coordinate[] crds = result[0].toCoordinateArray();
		assertEquals( 5, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 3.5, 0.0, 2.5 ), crds[crds.length - 1] );

		result = partiallyConstantIncreasing.getCoordinatesBetween( 1.0, 3.0 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 4, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 1.0, 0.0, 1.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 2.0, 0.0, 2.0 ), crds[1] );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 3.0 ), crds[3] );

		result = partiallyConstantIncreasing.getCoordinatesBetween( 0.0, 4.0 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 6, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 1.0, 0.0, 1.0 ), crds[1] );
		assertEquals( MCoordinate.create2dWithMeasure( 5.0, 0.0, 4.0 ), crds[5] );

		result = partiallyConstantIncreasing.getCoordinatesBetween( 0.5, 1.5 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 3, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 1.5, 0.0, 1.5 ), crds[crds.length - 1] );

		result = partiallyConstantIncreasing.getCoordinatesBetween( 1.5, 2.5 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 4, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 1.5, 0.0, 1.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 3.5, 0.0, 2.5 ), crds[crds.length - 1] );

		result = partiallyConstantIncreasing.getCoordinatesBetween( 3.5, 4.0 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 4.5, 0.0, 3.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 5.0, 0.0, 4.0 ), crds[crds.length - 1] );

		result = partiallyConstantIncreasing.getCoordinatesBetween( 3.5, 3.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 4.5, 0.0, 3.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 4.7, 0.0, 3.7 ), crds[1] );

		result = partiallyConstantIncreasing.getCoordinatesBetween( 0.5, 0.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.7, 0.0, 0.7 ), crds[1] );

		result = partiallyConstantIncreasing.getCoordinatesBetween( -0.5, 0.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.7, 0.0, 0.7 ), crds[1] );

		result = partiallyConstantIncreasing.getCoordinatesBetween( 3.5, 4.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 4.5, 0.0, 3.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 5.0, 0.0, 4.0 ), crds[1] );
	}

	@Test
	public void test_measures_monotone_decreasing() throws MGeometryException {
		CoordinateSequence[] result;
		result = decLine.getCoordinatesBetween( 0.5, 3.5 );
		assertEquals( 1, result.length );
		Coordinate[] crds = result[0].toCoordinateArray();
		assertEquals( 5, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 3.5, 0.0, 3.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[crds.length - 1] );

		result = decLine.getCoordinatesBetween( 1.0, 3.0 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 3, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 3.0, 0.0, 3.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 2.0, 0.0, 2.0 ), crds[1] );
		assertEquals( MCoordinate.create2dWithMeasure( 1.0, 0.0, 1.0 ), crds[2] );

		result = decLine.getCoordinatesBetween( 0.0, 4.0 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 5, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 3.0, 0.0, 3.0 ), crds[1] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ), crds[4] );

		result = decLine.getCoordinatesBetween( 0.5, 1.5 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 3, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 1.5, 0.0, 1.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[crds.length - 1] );

		result = decLine.getCoordinatesBetween( 3.5, 4.0 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 3.5, 0.0, 3.5 ), crds[crds.length - 1] );

		result = decLine.getCoordinatesBetween( 3.5, 3.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 3.7, 0.0, 3.7 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 3.5, 0.0, 3.5 ), crds[1] );

		result = decLine.getCoordinatesBetween( 0.5, 0.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.7, 0.0, 0.7 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[1] );

		result = decLine.getCoordinatesBetween( -0.5, 0.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.7, 0.0, 0.7 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ), crds[1] );

		result = decLine.getCoordinatesBetween( 3.5, 4.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 3.5, 0.0, 3.5 ), crds[1] );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.0 ), crds[0] );
	}


	@Test
	public void test_measures_partially_constant_decreasing() throws MGeometryException {
		CoordinateSequence[] result;
		result = partiallyConstantDecreasing.getCoordinatesBetween( 0.5, 3.5 );
		assertEquals( 1, result.length );
		Coordinate[] crds = result[0].toCoordinateArray();
		assertEquals( 6, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 4.5, 0.0, 3.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[crds.length - 1] );

		result = partiallyConstantDecreasing.getCoordinatesBetween( 1.0, 3.0 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 4, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 3.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 3.0, 0.0, 2.0 ), crds[1] );
		assertEquals( MCoordinate.create2dWithMeasure( 2.0, 0.0, 2.0 ), crds[2] );
		assertEquals( MCoordinate.create2dWithMeasure( 1.0, 0.0, 1.0 ), crds[3] );

		result = partiallyConstantDecreasing.getCoordinatesBetween( 0.0, 4.0 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 6, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 5.0, 0.0, 4.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 3.0 ), crds[1] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ), crds[5] );

		result = partiallyConstantDecreasing.getCoordinatesBetween( 0.5, 1.5 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 3, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 1.5, 0.0, 1.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[crds.length - 1] );

		result = partiallyConstantDecreasing.getCoordinatesBetween( 1.5, 2.5 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 4, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 3.5, 0.0, 2.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 3.0, 0.0, 2.0 ), crds[1] );
		assertEquals( MCoordinate.create2dWithMeasure( 2.0, 0.0, 2.0 ), crds[2] );
		assertEquals( MCoordinate.create2dWithMeasure( 1.5, 0.0, 1.5 ), crds[3] );

		result = partiallyConstantDecreasing.getCoordinatesBetween( 3.5, 4.0 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 5.0, 0.0, 4.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 4.5, 0.0, 3.5 ), crds[crds.length - 1] );

		result = partiallyConstantDecreasing.getCoordinatesBetween( 3.5, 3.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 4.7, 0.0, 3.7 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 4.5, 0.0, 3.5 ), crds[1] );

		result = partiallyConstantDecreasing.getCoordinatesBetween( 0.5, 0.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.7, 0.0, 0.7 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[1] );

		result = partiallyConstantDecreasing.getCoordinatesBetween( -0.5, 0.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.7, 0.0, 0.7 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ), crds[1] );

		result = partiallyConstantDecreasing.getCoordinatesBetween( 3.5, 4.7 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 2, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 4.5, 0.0, 3.5 ), crds[1] );
		assertEquals( MCoordinate.create2dWithMeasure( 5.0, 0.0, 4.0 ), crds[0] );
	}

	@Test
	public void test_measure_outside_monotone_increasing() throws MGeometryException {
		CoordinateSequence[] result;
		result = incrLine.getCoordinatesBetween( -1.5, -0.5 );
		assertEquals( 1, result.length );
		assertEquals( 0, result[0].size() );

		result = incrLine.getCoordinatesBetween( 10.0, 20.0 );
		assertEquals( 1, result.length );
		assertEquals( 0, result[0].size() );
	}

	@Test
	public void test_measure_outside_monotone_decreasing() throws MGeometryException {
		CoordinateSequence[] result;
		result = decLine.getCoordinatesBetween( -1.5, -0.5 );
		assertEquals( 1, result.length );
		assertEquals( 0, result[0].size() );

		result = decLine.getCoordinatesBetween( 10.0, 20.0 );
		assertEquals( 1, result.length );
		assertEquals( 0, result[0].size() );
	}

	@Test
	public void test_measure_overlap_monotone_increasing() throws MGeometryException {
		CoordinateSequence[] result;
		result = incrLine.getCoordinatesBetween( -0.5, 5 );
		assertEquals( 1, result.length );
		Coordinate[] crds = result[0].toCoordinateArray();
		assertEquals( 5, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.0 ), crds[crds.length - 1] );

		result = incrLine.getCoordinatesBetween( 0.5, 5 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 5, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.0 ), crds[crds.length - 1] );

		result = incrLine.getCoordinatesBetween( -1.0, 2.5 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 4, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 2.5, 0.0, 2.5 ), crds[crds.length - 1] );

		result = incrLine.getCoordinatesBetween( 4.0, 5.5 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 1, crds.length );

	}

	@Test
	public void test_measure_overlap_monotone_decreasing() throws MGeometryException {
		CoordinateSequence[] result;
		result = decLine.getCoordinatesBetween( -0.5, 5 );
		assertEquals( 1, result.length );
		Coordinate[] crds = result[0].toCoordinateArray();
		assertEquals( 5, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ), crds[crds.length - 1] );

		result = decLine.getCoordinatesBetween( 0.5, 5 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 5, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.0 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[crds.length - 1] );

		result = decLine.getCoordinatesBetween( -1.0, 2.5 );
		assertEquals( 1, result.length );
		crds = result[0].toCoordinateArray();
		assertEquals( 4, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 2.5, 0.0, 2.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ), crds[crds.length - 1] );
	}

	@Test
	public void test_measure_inverse_monotone_increasing() throws MGeometryException {
		CoordinateSequence[] result;
		result = incrLine.getCoordinatesBetween( 3.5, 0.5 );
		assertEquals( 1, result.length );
		Coordinate[] crds = result[0].toCoordinateArray();
		assertEquals( 5, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 3.5, 0.0, 3.5 ), crds[crds.length - 1] );
	}

	@Test
	public void test_measure_inverse_monotone_decreasing() throws MGeometryException {
		CoordinateSequence[] result;
		result = decLine.getCoordinatesBetween( 3.5, 0.5 );
		assertEquals( 1, result.length );
		Coordinate[] crds = result[0].toCoordinateArray();
		assertEquals( 5, crds.length );
		assertEquals( MCoordinate.create2dWithMeasure( 3.5, 0.0, 3.5 ), crds[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 0.5, 0.0, 0.5 ), crds[crds.length - 1] );
	}

	@Test
	public void test_fail_on_non_monotone() throws MGeometryException {
		try {
			nonMonotoneLine.getCoordinatesBetween( 0.5, 10.0 );
			fail( "Needs to throw an IllegalArgumentException on non-monotone linestrings." );
		}
		catch ( MGeometryException e ) {
			assertEquals( e.getType(), MGeometryException.OPERATION_REQUIRES_MONOTONE );
		}
	}

}
