/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.oracle;


import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;

import org.geolatte.geom.AbstractGeometryCollection;
import org.geolatte.geom.ExactPositionEquality;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.GeometryEquality;
import org.geolatte.geom.LinearRing;
import org.geolatte.geom.Polygon;
import org.geolatte.geom.Position;
import org.geolatte.geom.PositionSequence;
import org.geolatte.geom.PositionSequenceBuilders;
import org.geolatte.geom.PositionSequenceEquality;
import org.geolatte.geom.PositionSequencePositionEquality;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.polygon;
import static org.geolatte.geom.builder.DSL.ring;
import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;

/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Oct 22, 2010
 */
public class OracleSDOTestSupport extends TestSupport {

	@Override
	public NativeSQLTemplates templates() {
		return new OracleSDONativeSqlTemplates();
	}

	@Override
	public PredicateRegexes predicateRegexes() {
		return new OraclePredicateRegexes();
	}

	@Override
	public TestData createTestData(TestDataPurpose purpose) {
		return TestData.fromFile( "oracle10g/test-sdo-geometry-data-set-2D.xml", new SDOTestDataReader() );
	}

	public GeomCodec codec() {
		return in -> (Geometry<?>) in;
	}


	public Geometry<?> getFilterGeometry() {
		//ensure the filter geometry has the correct orientation (Counter-clockwise for exterior ring)
		// if not this creates problems for the SQL/MM function, esp. ST_GEOMERY.ST_WITHIN().
		return polygon(
				WGS84,
				ring( g( 0, 0 ), g( 10, 0 ), g( 10, 10 ), g( 0, 10 ), g( 0, 0 ) )
		);
	}

	@Override
	public GeometryEquality getGeometryEquality() {
		return new OraGeometryEquality();
	}

	@SuppressWarnings("rawtypes")
	static class OraGeometryEquality implements GeometryEquality {

		private final ExactPositionEquality pointEquality = new ExactPositionEquality();
		private final PositionSequenceEquality pointSeqEq = new PositionSequencePositionEquality( pointEquality );

		/**
		 * {@inheritDoc}
		 */
		@Override
		public <P extends Position> boolean equals(Geometry<P> first, Geometry<P> second) {
			if ( first == second ) {
				return true;
			}
			if ( first == null || second == null ) {
				return false;
			}
			if ( first.isEmpty() && second.isEmpty() ) {
				return true;
			}
			if ( first.isEmpty() || second.isEmpty() ) {
				return false;
			}
			if ( !first.getCoordinateReferenceSystem().equals( second.getCoordinateReferenceSystem() ) ) {
				return false;
			}
			if ( first.getGeometryType() != second.getGeometryType() ) {
				return false;
			}
			if ( first instanceof AbstractGeometryCollection ) {
				assert ( second instanceof AbstractGeometryCollection );
				return equals( (AbstractGeometryCollection<?, ?>) first, (AbstractGeometryCollection<?, ?>) second );
			}
			if ( first instanceof Polygon ) {
				assert ( second instanceof Polygon );
				return equals( (Polygon<P>) first, (Polygon<P>) second );
			}
			return pointSeqEq.equals( first.getPositions(), second.getPositions() );
		}

		private <P extends Position> boolean equals(Polygon<P> first, Polygon<P> second) {
			if ( first.getNumInteriorRing() != second.getNumInteriorRing() ) {
				return false;
			}
			if ( notEqualRings( first.getExteriorRing(), second.getExteriorRing() ) ) {
				return false;
			}
			for ( int i = 0; i < first.getNumInteriorRing(); i++ ) {
				if ( notEqualRings( first.getInteriorRingN( i ), second.getInteriorRingN( i ) ) ) {
					return false;
				}
			}
			return true;
		}

		private <P extends Position> boolean notEqualRings(LinearRing<P> ring1, LinearRing<P> ring2) {
			var p1 = ring1.getPositions();
			var p2 = ring2.getPositions();
			int shift = determineShift( p1, p2 );
			return !this.pointSeqEq.equals( p1, shiftSeqBy( shift, p2 ) );
		}

		private <P extends Position> PositionSequence<P> shiftSeqBy(int shift, PositionSequence<P> p2) {
			if ( shift == 0 ) {
				return p2;
			}
			int size = p2.size();
			var bldr = PositionSequenceBuilders.fixedSized( size, p2.getPositionClass() );
			for ( int k = shift; k < shift + size; k++ ) {
				var idx = k % size;
				if ( idx == 0 ) {
					continue; //skip first (will be repeated at end)
				}
				bldr.add( p2.getPositionN( idx ) );
			}
			//repeat element that should be first
			bldr.add( p2.getPositionN( shift ) );
			return bldr.toPositionSequence();
		}

		// determines shift, if any. Otherwise return 0;
		private <P extends Position> int determineShift(PositionSequence<P> p1, PositionSequence<P> p2) {
			var startP1 = p1.getPositionN( 0 );
			int shift = 0;
			for ( var p : p2 ) {
				if ( p.equals( startP1 ) ) {
					return shift;
				}
				shift++;
			}
			return 0;
		}

		@SuppressWarnings("unchecked")
		private boolean equals(AbstractGeometryCollection first, AbstractGeometryCollection second) {
			if ( first.getNumGeometries() != second.getNumGeometries() ) {
				return false;
			}
			for ( int i = 0; i < first.getNumGeometries(); i++ ) {
				if ( !equals( first.getGeometryN( i ), second.getGeometryN( i ) ) ) {
					return false;
				}
			}
			return true;
		}


	}

}
