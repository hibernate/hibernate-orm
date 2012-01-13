/*
 * $Id: TestEventLocator.java 226 2010-06-28 20:58:45Z maesenka $
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.PrecisionModel;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.spatial.mgeom.EventLocator;
import org.hibernate.spatial.mgeom.MCoordinate;
import org.hibernate.spatial.mgeom.MCoordinateSequenceFactory;
import org.hibernate.spatial.mgeom.MGeometryException;
import org.hibernate.spatial.mgeom.MGeometryFactory;
import org.hibernate.spatial.mgeom.MLineString;
import org.hibernate.spatial.mgeom.MultiMLineString;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestEventLocator {

	private PrecisionModel prec = new PrecisionModel( PrecisionModel.FIXED );

	private MGeometryFactory mgeomFactory = new MGeometryFactory(
			MCoordinateSequenceFactory.instance()
	);

	private MultiMLineString incrML;

	@Before
	public void setUp() {

		MCoordinate[] coordinates = new MCoordinate[] {
				MCoordinate.create2dWithMeasure( 0.0, 0.0, 0.0 ),
				MCoordinate.create2dWithMeasure( 1.0, 0.0, 1.0 ),
				MCoordinate.create2dWithMeasure( 2.0, 0.0, 2.0 ),
				MCoordinate.create2dWithMeasure( 3.0, 0.0, 3.0 ),
				MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.0 )
		};
		MLineString line1 = mgeomFactory.createMLineString( coordinates );

		MCoordinate[] coordinates2 = new MCoordinate[] {
				MCoordinate.create2dWithMeasure( 5.0, 0.0, 5.0 ),
				MCoordinate.create2dWithMeasure( 6.0, 0.0, 6.0 ),
				MCoordinate.create2dWithMeasure( 7.0, 0.0, 7.0 ),
		};
		MLineString line2 = mgeomFactory.createMLineString( coordinates2 );

		MCoordinate[] coordinates3 = new MCoordinate[] {
				MCoordinate.create2dWithMeasure( 9.0, 0.0, 9.0 ),
				MCoordinate.create2dWithMeasure( 10.0, 0.0, 10.0 ),
				MCoordinate.create2dWithMeasure( 11.0, 0.0, 11.0 ),
		};
		MLineString line3 = mgeomFactory.createMLineString( coordinates2 );

		incrML = mgeomFactory.createMultiMLineString( new MLineString[] { line1, line2 } );

	}

	@Test
	public void test_event_starts_at_end_of_component() throws MGeometryException {
		MultiMLineString result = EventLocator.getLinearGeometry( incrML, 4.0, 5.5 );
		assertNotNull( result );
		assertEquals( 1, result.getNumGeometries() );
		assertEquals( 2, result.getCoordinates().length );
		Coordinate[] coordinates = result.getCoordinates();
		assertEquals( MCoordinate.create2dWithMeasure( 5.0, 0.0, 5.0 ), (MCoordinate) coordinates[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 5.5, 0.0, 5.5 ), (MCoordinate) coordinates[1] );

	}

	@Test
	public void test_event_ends_at_begin_of_component() throws MGeometryException {
		MultiMLineString result = EventLocator.getLinearGeometry( incrML, 3.0, 5.0 );
		assertNotNull( result );
		assertEquals( 1, result.getNumGeometries() );
		assertEquals( 2, result.getCoordinates().length );
		Coordinate[] coordinates = result.getCoordinates();
		assertEquals( MCoordinate.create2dWithMeasure( 3.0, 0.0, 3.0 ), (MCoordinate) coordinates[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 4.0, 0.0, 4.0 ), (MCoordinate) coordinates[1] );
	}


	@Test
	public void test_event_ends_at_end_of_component() throws MGeometryException {
		MultiMLineString result = EventLocator.getLinearGeometry( incrML, 4.5, 7.0 );
		assertNotNull( result );
		assertEquals( 1, result.getNumGeometries() );
		assertEquals( 3, result.getCoordinates().length );
		Coordinate[] coordinates = result.getCoordinates();
		assertEquals( MCoordinate.create2dWithMeasure( 5.0, 0.0, 5.0 ), (MCoordinate) coordinates[0] );
		assertEquals( MCoordinate.create2dWithMeasure( 6.0, 0.0, 6.0 ), (MCoordinate) coordinates[1] );
		assertEquals( MCoordinate.create2dWithMeasure( 7.0, 0.0, 7.0 ), (MCoordinate) coordinates[2] );
	}

	@Test
	public void test_locator_result_has_same_srid_as_input_mgeometry() throws MGeometryException {
		incrML.setSRID( 123 );
		MultiMLineString result = EventLocator.getLinearGeometry( incrML, 4.5, 7.0 );
		assertEquals( 123, result.getSRID() );
	}


}
